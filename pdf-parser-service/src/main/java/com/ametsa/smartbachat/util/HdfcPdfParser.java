package com.ametsa.smartbachat.util;

import com.ametsa.smartbachat.entity.TransactionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for HDFC Bank statements.
 *
 * Designed for the tabular layout with columns:
 * Date | Narration | Chq./Ref.No. | Value Dt | Withdrawal Amt. | Deposit Amt. | Closing Balance
 *
 * Strategy:
 *  - Treat each line starting with a date (dd/MM/yy) as the beginning of a transaction row.
 *  - Append subsequent non-date lines to handle multi-line narration.
 *  - Within the row, detect monetary values near the end of the line:
 *      * Last amount  -> closing balance
 *      * Previous one -> transaction amount (or withdrawal/deposit when both columns are present)
 *  - Amount sign is inferred from debit/credit columns when present, or from narration keywords.
 */
@Component
public class HdfcPdfParser implements PdfParserStrategy {

    private static final Pattern DATE_AT_START = Pattern.compile("^(\\d{1,2}/\\d{1,2}/\\d{2,4})\\s+(.+)$");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("([0-9]{1,3}(?:,[0-9]{3})*\\.[0-9]{2})");
    private static final Logger log = LoggerFactory.getLogger(HdfcPdfParser.class);

    @Override
    public List<TransactionEntity> parse(String pageText) {
        List<TransactionEntity> out = new ArrayList<>();
        if (pageText == null) {
            return out;
        }

        log.info("CURRENT ROW {}", pageText);
        String[] lines = pageText.split("\\r?\\n");
        String currentRow = null;

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            // Skip header row(s)
            if (line.startsWith("Date ") || line.startsWith("DATE ")) {
                continue;
            }

            Matcher dateMatcher = DATE_AT_START.matcher(line);
            if (dateMatcher.find()) {
                // New transaction row starting
                if (currentRow != null) {
                    TransactionEntity prev = parseRow(currentRow);
                    if (prev != null) {
                        out.add(prev);
                    }
                }
                currentRow = line;
            } else if (currentRow != null) {
                // Continuation of narration for previous row
                currentRow = currentRow + " " + line;
            }
        }

        if (currentRow != null) {
            TransactionEntity last = parseRow(currentRow);
            if (last != null) {
                out.add(last);
            }
        }


        return out;
    }

    private TransactionEntity parseRow(String row) {
        Matcher m = DATE_AT_START.matcher(row);
        if (!m.find()) {
            return null;
        }

        log.info("ROW Details {}", row);
        String dateStr = m.group(1);
        String rest = m.group(2);

        // Ignore opening-balance / brought-forward style rows
        String lowerRest = rest.toLowerCase();
        if (lowerRest.contains("balance b/f") || lowerRest.contains("bal b/f") || lowerRest.contains("opening bal")) {
            return null;
        }

        // Find all monetary amounts on the row
        List<AmountMatch> amounts = new ArrayList<>();
        Matcher numMatcher = AMOUNT_PATTERN.matcher(rest);
        while (numMatcher.find()) {
            amounts.add(new AmountMatch(numMatcher.group(1), numMatcher.start()));
        }

	        // Need at least: transaction amount + closing balance
	        if (amounts.size() < 2) {
	            return null;
	        }

        // Last amount is closing balance
        AmountMatch balanceMatch = amounts.get(amounts.size() - 1);
        long balancePaisa = parseAmountToPaisa(balanceMatch.value);
        // In the HDFC PDFs we've seen, closing balance is never 0.
        // If we parsed 0 here, it usually means we mis-parsed the row,
        // so skip this row rather than storing an incorrect 0 balance.
        if (balancePaisa == 0L) {
            return null;
        }

	        long amountPaisa;
	        int txnAmountIndexStart;
	        String direction = null;

	        if (amounts.size() >= 3) {
	            // Likely: withdrawal, deposit, closing balance
	            AmountMatch withdrawMatch = amounts.get(amounts.size() - 3);
	            AmountMatch depositMatch = amounts.get(amounts.size() - 2);

	            long withdrawPaisa = parseAmountToPaisa(withdrawMatch.value);
	            long depositPaisa = parseAmountToPaisa(depositMatch.value);

	            if (withdrawPaisa > 0 && depositPaisa == 0) {
	                // Money went out
	                amountPaisa = withdrawPaisa; // keep absolute; direction indicates DR
	                direction = "DEBIT";
	                txnAmountIndexStart = withdrawMatch.startIndex;
	            } else if (depositPaisa > 0 && withdrawPaisa == 0) {
	                // Money came in
	                amountPaisa = depositPaisa;
	                direction = "CREDIT";
	                txnAmountIndexStart = depositMatch.startIndex;
	            } else if (depositPaisa > 0) {
	                // Prefer treating as credit if both non-zero (very rare)
	                amountPaisa = depositPaisa;
	                direction = "CREDIT";
	                txnAmountIndexStart = depositMatch.startIndex;
	            } else {
	                // Fallback: treat as debit
	                amountPaisa = withdrawPaisa;
	                direction = "DEBIT";
	                txnAmountIndexStart = withdrawMatch.startIndex;
	            }
	        } else {
	            // Only transaction amount + balance present. Keep absolute and
	            // infer direction from narration heuristics.
	            AmountMatch amtMatch = amounts.get(0);
	            long absPaisa = parseAmountToPaisa(amtMatch.value);
	            boolean debit = isLikelyDebit(lowerRest);
	            amountPaisa = absPaisa;
	            direction = debit ? "DEBIT" : "CREDIT";
	            txnAmountIndexStart = amtMatch.startIndex;
	        }

        int descEnd = Math.min(txnAmountIndexStart, rest.length());
        String description = rest.substring(0, descEnd).trim();

        TransactionEntity e = new TransactionEntity();
        try {
            LocalDate date = LocalDate.parse(normalizeDate(dateStr));
            e.setTxnDate(date);
        } catch (Exception ignored) {
            // keep null txnDate if parsing fails
        }

        // Basic enrichment for downstream categorization
        String descLower = description.toLowerCase();
        String txnType = inferTxnType(descLower);
        String merchant = extractMerchant(description, txnType);

        e.setDescription(description);
        if (txnType != null && !txnType.isEmpty()) {
            e.setTxnType(txnType);
        }
        if (merchant != null && !merchant.isEmpty()) {
            e.setMerchant(merchant);
        }
	        // HDFC statements are INR; set default
	        e.setCurrency("INR");
	        // Store absolute amount (paisa) and semantic direction separately
	        e.setAmount(amountPaisa);
	        e.setDirection(direction);
	        e.setBalance(balancePaisa);
        e.setRawText(row.trim());
        return e;
    }

    private boolean isLikelyDebit(String text) {
	        String t = text.toLowerCase();

	        // If the narration clearly indicates a refund / reversal / cashback or
	        // other typical credit keywords, do NOT treat it as a debit even if it
	        // contains UPI / IMPS / NEFT etc.
	        if (t.contains("refund") || t.contains("reversal") || t.contains(" cashb") ||
	                t.contains(" reward") || t.contains(" interest") ||
	                t.contains(" salary") || t.contains(" sal ")) {
	            return false;
	        }

	        // Channels that are typically money-out for a customer
	        boolean hasUpi = t.contains(" upi") || t.startsWith("upi") ||
	                t.contains("/upi") || t.contains("upi/") || t.contains("upi-");
	        boolean hasImps = t.contains(" imps") || t.startsWith("imps") ||
	                t.contains("/imps") || t.contains("imps-");
	        boolean hasNeft = t.contains(" neft") || t.startsWith("neft") ||
	                t.contains("/neft") || t.contains("neft-");
	        boolean hasRtgs = t.contains(" rtgs") || t.startsWith("rtgs") ||
	                t.contains("/rtgs") || t.contains("rtgs-");
	        boolean hasPos = t.contains(" pos") || t.startsWith("pos");
	        boolean hasAtm = t.contains(" atm") || t.contains(" atw");

	        return hasAtm || hasUpi || hasImps || hasNeft || hasRtgs || hasPos ||
	                t.contains(" billpay") ||
	                t.contains(" chq") || t.contains(" cheque") ||
	                t.contains(" debit") || t.contains(" dr ") || t.endsWith(" dr");
    }

    /**
     * Infer a coarse transaction type/mode for categorization rules.
     */
    private String inferTxnType(String text) {
        String t = text.toLowerCase();
        if (t.contains(" upi") || t.startsWith("upi") || t.contains("/upi")) return "UPI";
        if (t.contains(" imps") || t.startsWith("imps")) return "IMPS";
        if (t.contains(" neft") || t.startsWith("neft")) return "NEFT";
        if (t.contains(" rtgs") || t.startsWith("rtgs")) return "RTGS";
        if (t.contains(" pos") || t.startsWith("pos")) return "POS";
        if (t.contains(" atm") || t.contains(" atw")) return "ATM";
        if (t.contains(" salary") || t.contains(" sal ")) return "SALARY";
        if (t.contains(" chq") || t.contains(" cheque")) return "CHEQUE";
        if (t.contains(" charge") || t.contains(" chg") || t.contains(" fee")) return "CHARGE";
        if (t.contains(" cashback") || t.contains(" reward") || t.contains(" reversal") || t.contains(" refund")) return "REFUND";
        if (t.contains(" interest")) return "INTEREST";
        return null;
    }

    /**
     * Try to pull out a merchant / counterparty name from the narration.
     * This is heuristic but gives enough signal for category engines.
     */
    private String extractMerchant(String description, String txnType) {
        if (description == null) return null;
        String desc = description.trim();
        if (desc.isEmpty()) return null;

        String upper = desc.toUpperCase();

        // If POS, merchant name typically follows POS and (optionally) a numeric ID
        if ("POS".equalsIgnoreCase(txnType) || upper.contains(" POS")) {
            int idx = upper.indexOf("POS");
            if (idx >= 0 && idx + 3 < desc.length()) {
                String tail = desc.substring(idx + 3).trim();
                String[] parts = tail.split("\\s+");
                StringBuilder m = new StringBuilder();
                for (String p : parts) {
                    // skip obvious numeric-only tokens (terminal IDs)
                    if (p.matches("[0-9./-]+")) continue;
                    if (m.length() > 0) m.append(' ');
                    m.append(p);
                    if (m.length() > 30) break; // keep it short
                }
                String res = m.toString().trim();
                if (!res.isEmpty()) return res;
            }
        }

        // UPI / IMPS / NEFT: payee/merchant is often towards the end
        if ("UPI".equalsIgnoreCase(txnType) || "IMPS".equalsIgnoreCase(txnType) ||
                "NEFT".equalsIgnoreCase(txnType) || "RTGS".equalsIgnoreCase(txnType)) {
            String[] tokens = desc.split("\\s+");
            if (tokens.length >= 2) {
                String last = tokens[tokens.length - 1];
                String secondLast = tokens[tokens.length - 2];
                String candidate = (secondLast + " " + last).replaceAll("[*/-]", " ").trim();
                candidate = candidate.replaceAll("\\s+", " ");
                if (!candidate.isEmpty() && !candidate.matches("[0-9.]+")) {
                    return candidate;
                }
            }
        }

        // ATM: just label as ATM CASH so it can be mapped to a cash withdrawal category
        if ("ATM".equalsIgnoreCase(txnType)) {
            return "ATM CASH";
        }

        // Fallback: take a short suffix of the description as merchant-ish text
        String[] tokens = desc.split("\\s+");
        if (tokens.length == 0) return null;
        int start = Math.max(0, tokens.length - 3);
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < tokens.length; i++) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(tokens[i]);
        }
        String fallback = sb.toString().trim();
        if (fallback.isEmpty() || fallback.matches("[0-9.]+")) return null;
        return fallback;
    }

    private String normalizeDate(String d) {
        // Normalize dd/MM/yy or dd/MM/yyyy -> yyyy-MM-dd
        String[] parts = d.replace("-", "/").split("/");
        if (parts.length != 3) return d;
        String dd = parts[0].length() == 1 ? "0" + parts[0] : parts[0];
        String mm = parts[1].length() == 1 ? "0" + parts[1] : parts[1];
        String yy = parts[2];
        if (yy.length() == 2) {
            yy = "20" + yy;
        }
        return yy + "-" + mm + "-" + dd;
    }

    private long parseAmountToPaisa(String s) {
        String cleaned = s.replace(",", "").replace("(", "-").replace(")", "").trim();
        if (cleaned.isEmpty() || "-".equals(cleaned)) {
            return 0L;
        }
        try {
            double v = Double.parseDouble(cleaned);
            return Math.round(v * 100);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static class AmountMatch {
        final String value;
        final int startIndex;

        AmountMatch(String value, int startIndex) {
            this.value = value;
            this.startIndex = startIndex;
        }
    }
}
