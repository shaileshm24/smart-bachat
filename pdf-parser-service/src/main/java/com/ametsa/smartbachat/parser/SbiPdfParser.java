package com.ametsa.smartbachat.parser;

import com.ametsa.smartbachat.entity.TransactionEntity;
import com.ametsa.smartbachat.util.PdfParserStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for SBI bank statements.
 *
 * <p>Assumes a classic ledger layout where each transaction row contains:</n+ * <pre>
 *   Date  ...description...  [Debit]  [Credit]  Balance
 * </pre>
 *
 * The implementation is defensive and falls back to narration-based
 * inference if separate Debit / Credit columns are not clearly present.
 */
@Component
public class SbiPdfParser implements PdfParserStrategy {

    private static final Logger log = LoggerFactory.getLogger(SbiPdfParser.class);

    // Matches dates at the start of a transaction row.
    // Supports formats like:
    //   08-10-2011, 08/10/2011, 8-10-11
    //   8 Oct 2011, 08 Sep 2011
    private static final Pattern DATE_AT_START =
            Pattern.compile(
                    "^(\\d{1,2}(?:[-/]\\d{1,2}[-/]\\d{2,4}|\\s+[A-Za-z]{3,9}\\s+\\d{2,4}))\\s+(.+)$"
            );

    // Generic monetary amount matcher: 123.45 or 1,23,456.78
    private static final Pattern AMOUNT_PATTERN =
            Pattern.compile("([0-9][0-9,]*\\.[0-9]{2})");

    // Generic pattern used for opening balance detection in extractOpeningBalance
    private static final Pattern BAL_AS_ON =
            Pattern.compile("(?i)balance\\s+as\\s+on\\s+\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}");

    @Override
    public String getBankCode() {
        return "SBI";
    }

    @Override
    public List<TransactionEntity> parse(String pageText, Long openingBalancePaisa) {

        if (pageText == null || pageText.isBlank()) return List.of();

        String[] lines = pageText.split("\\r?\\n");

        // Detect whether this page has an explicit "Debit Credit Balance" header.
        boolean hasAmountHeader = false;
        for (String raw : lines) {
            String lower = raw.trim().toLowerCase();
            if (!lower.isEmpty() && lower.contains("debit") && lower.contains("credit") && lower.contains("balance")) {
                hasAmountHeader = true;
                break;
            }
        }

        List<ParsedRow> rows;
        if (hasAmountHeader) {
            rows = parseWithHeaderGrouping(lines);
        } else {
            rows = parseWithDateGrouping(lines);
        }

        log.info("[SBI] Page parsed into {} transaction rows", rows.size());

        List<TransactionEntity> out = new ArrayList<>();
        for (ParsedRow r : rows) {
            out.add(r.entity);
        }
        return out;
    }

    /**
     * Fallback grouping when we don't see an explicit "Debit Credit Balance" header.
     * Uses date-at-start detection to decide when a new logical row begins.
     */
    private List<ParsedRow> parseWithDateGrouping(String[] lines) {
        List<ParsedRow> rows = new ArrayList<>();
        String currentRow = null;

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;

            String lower = line.toLowerCase();
            if (lower.startsWith("date") || lower.contains("transaction details")) {
                continue;
            }

            Matcher m = DATE_AT_START.matcher(line);
            if (m.find()) {
                if (currentRow != null) {
                    ParsedRow r = parseRowInternal(currentRow);
                    if (r != null) rows.add(r);
                }
                currentRow = line;
            } else if (currentRow != null) {
                currentRow += " " + line;
            }
        }

        if (currentRow != null) {
            ParsedRow r = parseRowInternal(currentRow);
            if (r != null) rows.add(r);
        }
        return rows;
    }

    /**
     * SBI layout (as seen in the sample PDF) presents a column header row like
     * "Debit Credit Balance" and then splits dates / narration over multiple
     * physical lines. Here we treat a transaction row as the group of lines
     * from the first content line after the header up to the line that
     * contains at least two monetary amounts (txn amount + balance).
     */
    private List<ParsedRow> parseWithHeaderGrouping(String[] lines) {
        List<ParsedRow> rows = new ArrayList<>();
        boolean inTable = false;
        String currentRow = null;

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;

            String lower = line.toLowerCase();

            if (!inTable) {
                // Wait until we see the debit/credit/balance header, then start
                if (lower.contains("debit") && lower.contains("credit") && lower.contains("balance")) {
                    inTable = true;
                    currentRow = null; // start fresh from the next line
                }
                continue;
            }

            // Skip any repeated column titles inside the table body
            if (lower.startsWith("txn date") ||
                    (lower.startsWith("date") && lower.contains("value")) ||
                    lower.contains("description ref no./cheque") ||
                    lower.contains("ref no./cheque")) {
                continue;
            }

            // Accumulate logical row text
            if (currentRow == null) {
                currentRow = line;
            } else {
                currentRow += " " + line;
            }

            // The last physical line of a transaction row contains the
            // monetary columns (at least txn amount + balance).
            int amountCount = 0;
            Matcher numMatcher = AMOUNT_PATTERN.matcher(line);
            while (numMatcher.find()) {
                amountCount++;
            }

            if (amountCount >= 2) {
                ParsedRow r = parseRowInternal(currentRow);
                if (r != null) rows.add(r);
                currentRow = null;
            }
        }

        // We intentionally do not flush a trailing row without amounts:
        // ledger rows should always end with at least txn amount + balance.
        return rows;
    }

    private ParsedRow parseRowInternal(String row) {

        Matcher m = DATE_AT_START.matcher(row);
        if (!m.find()) return null;

        String dateStr = m.group(1);
        String rest = trimAfterFooter(m.group(2));
        String lower = rest.toLowerCase();

        // Ignore opening balance / carry-forward rows
        if (lower.contains("balance b/f") || lower.contains("opening bal")) {
            return null;
        }

        List<AmountMatch> amounts = new ArrayList<>();
        Matcher numMatcher = AMOUNT_PATTERN.matcher(rest);
        while (numMatcher.find()) {
            amounts.add(new AmountMatch(numMatcher.group(1), numMatcher.start()));
        }

        if (amounts.size() < 2) return null; // need at least txn + balance

        long balancePaisa = parseAmountToPaisa(amounts.get(amounts.size() - 1).value);

        long debitPaisa = 0L;
        long creditPaisa = 0L;
        long txnAmountPaisa;
        int descEndIdx;

        if (amounts.size() >= 3) {
            // Heuristic: [...desc...] Debit Credit Balance
            AmountMatch debitMatch = amounts.get(amounts.size() - 3);
            AmountMatch creditMatch = amounts.get(amounts.size() - 2);
            debitPaisa = parseAmountToPaisa(debitMatch.value);
            creditPaisa = parseAmountToPaisa(creditMatch.value);
            txnAmountPaisa = (debitPaisa > 0) ? debitPaisa : creditPaisa;
            descEndIdx = Math.min(debitMatch.startIndex, creditMatch.startIndex);
        } else {
            // Only one amount before balance – rely on narration to infer direction
            AmountMatch amt = amounts.get(0);
            txnAmountPaisa = parseAmountToPaisa(amt.value);
            descEndIdx = amt.startIndex;
        }

        if (descEndIdx <= 0 || descEndIdx > rest.length()) {
            descEndIdx = rest.length();
        }

        String description = rest.substring(0, descEndIdx).trim();

        TransactionEntity e = new TransactionEntity();

        try {
            e.setTxnDate(LocalDate.parse(normalizeDate(dateStr)));
        } catch (Exception ex) {
            // leave date null if parsing fails
        }

        e.setBalance(balancePaisa);
        e.setCurrency("INR");
        e.setDescription(description);
        e.setRawText(row.trim());

        // Direction and debit/credit mapping
        if (debitPaisa > 0 || creditPaisa > 0) {
            if (debitPaisa > 0 && creditPaisa == 0) {
                e.setDirection("DEBIT");
                e.setWithdrawalAmount(debitPaisa);
                e.setDepositAmount(0L);
                e.setAmount(debitPaisa);
            } else if (creditPaisa > 0 && debitPaisa == 0) {
                e.setDirection("CREDIT");
                e.setDepositAmount(creditPaisa);
                e.setWithdrawalAmount(0L);
                e.setAmount(creditPaisa);
            } else {
                // Both populated (unexpected) – fall back to narration
                applyNarrationInference(e, lower, txnAmountPaisa);
            }
        } else {
            // No explicit debit/credit split – narration-based
            applyNarrationInference(e, lower, txnAmountPaisa);
        }

        String txnType = inferTxnType(lower);
        if (txnType != null) e.setTxnType(txnType);

        String merchant = extractMerchant(description, txnType);
        if (merchant != null) e.setMerchant(merchant);

        return new ParsedRow(e, txnAmountPaisa, balancePaisa, lower);
    }

    private void applyNarrationInference(TransactionEntity e, String lower, long txnAmountPaisa) {
        boolean debit = isLikelyDebit(lower);
        if (debit) {
            e.setDirection("DEBIT");
            e.setWithdrawalAmount(txnAmountPaisa);
            e.setDepositAmount(0L);
        } else {
            e.setDirection("CREDIT");
            e.setDepositAmount(txnAmountPaisa);
            e.setWithdrawalAmount(0L);
        }
        e.setAmount(txnAmountPaisa);
    }

    // ------------------------------------------------------------------
    // HELPERS
    // ------------------------------------------------------------------

    private boolean isLikelyDebit(String t) {
        return t.contains(" atm") || t.contains(" atw") || t.contains(" nwd")
                || t.contains(" upi") || t.contains(" imps")
                || t.contains(" neft") || t.contains(" rtgs")
                || t.contains(" pos") || t.contains(" debit")
                || t.contains(" chq") || t.contains(" cheque");
    }

    private String inferTxnType(String t) {
        if (t.contains("upi")) return "UPI";
        if (t.contains("imps")) return "IMPS";
        if (t.contains("neft")) return "NEFT";
        if (t.contains("rtgs")) return "RTGS";
        if (t.contains("pos")) return "POS";
        if (t.contains("atm") || t.contains("atw") || t.contains("nwd")) return "ATM";
        if (t.contains("salary")) return "SALARY";
        if (t.contains("interest")) return "INTEREST";
        if (t.contains("charge") || t.contains("fee")) return "CHARGE";
        if (t.contains("refund") || t.contains("reversal")) return "REFUND";
        return null;
    }

    private String extractMerchant(String desc, String txnType) {
        if (desc == null) return null;
        if ("ATM".equalsIgnoreCase(txnType)) return "ATM CASH";

        String[] parts = desc.split("\\s+");
        int start = Math.max(0, parts.length - 3);
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < parts.length; i++) {
            sb.append(parts[i]).append(" ");
        }
        return sb.toString().trim();
    }

    private String normalizeDate(String d) {
	        if (d == null) return null;
	        String trimmed = d.trim();
	        if (trimmed.isEmpty()) return d;

	        // Collapse multiple spaces to a single space to make parsing stable
	        trimmed = trimmed.replaceAll("\\s+", " ");

	        // 1) Numeric formats like 08-10-2011, 8/10/11, 08/10/2011
	        String numeric = trimmed.replace('/', '-');
	        String[] parts = numeric.split("-");
	        if (parts.length == 3
	                && parts[0].matches("\\d{1,2}")
	                && parts[1].matches("\\d{1,2}")
	                && parts[2].matches("\\d{2,4}")) {
	            String dd = parts[0].length() == 1 ? "0" + parts[0] : parts[0];
	            String mm = parts[1].length() == 1 ? "0" + parts[1] : parts[1];
	            String yy = parts[2];
	            if (yy.length() == 2) {
	                // Interpret 2-digit years as 2000+YY for modern statements
	                yy = "20" + yy;
	            }
	            return yy + "-" + mm + "-" + dd;
	        }

	        // 2) Textual month formats like "8 Oct 2011" or "08 Sep 2011"
	        DateTimeFormatter textYear4 = new DateTimeFormatterBuilder()
	                .parseCaseInsensitive()
	                .appendPattern("d MMM uuuu")
	                .toFormatter(Locale.ENGLISH);
	        DateTimeFormatter textYear2 = new DateTimeFormatterBuilder()
	                .parseCaseInsensitive()
	                .appendPattern("d MMM ")
	                .appendValueReduced(ChronoField.YEAR, 2, 2, 2000)
	                .toFormatter(Locale.ENGLISH);
	        for (DateTimeFormatter fmt : new DateTimeFormatter[]{textYear4, textYear2}) {
	            try {
	                LocalDate dt = LocalDate.parse(trimmed, fmt);
	                return dt.toString(); // ISO_LOCAL_DATE (yyyy-MM-dd)
	            } catch (DateTimeParseException ignore) {
	                // try next format
	            }
	        }

	        // Fallback – let caller attempt parsing original string (will likely fail)
	        return d;
    }

    private long parseAmountToPaisa(String s) {
        try {
            return Math.round(Double.parseDouble(s.replace(",", "")) * 100);
        } catch (Exception e) {
            return 0L;
        }
    }

    private String trimAfterFooter(String row) {
        String[] FOOTER_MARKERS = {
                "Page ", "Statement of account", "Account Number", "Branch :", "Address :"
        };

        int cutIndex = row.length();
        for (String marker : FOOTER_MARKERS) {
            int idx = row.indexOf(marker);
            if (idx > 0) {
                cutIndex = Math.min(cutIndex, idx);
            }
        }
        return row.substring(0, cutIndex).trim();
    }

    // ------------------------------------------------------------------
    // INTERNAL DTOs
    // ------------------------------------------------------------------

    private static class ParsedRow {
        final TransactionEntity entity;
        final long txnAmountPaisa;
        final long balancePaisa;
        final String lowerText;

        ParsedRow(TransactionEntity e, long amt, long bal, String text) {
            this.entity = e;
            this.txnAmountPaisa = amt;
            this.balancePaisa = bal;
            this.lowerText = text;
        }
    }

    private static class AmountMatch {
        final String value;
        final int startIndex;

        AmountMatch(String v, int i) {
            value = v;
            startIndex = i;
        }
    }

    @Override
    public Long extractOpeningBalance(String documentText) {
        if (documentText == null || documentText.isBlank()) {
            return null;
        }

        Pattern BALANCE_AS_ON = Pattern.compile(
                "(?i)balance\\s+as\\s+on\\s+" +
                        "(?:\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{1,2}\\s+[a-z]{3,9}\\s+\\d{4})" +
                        "[^0-9]*([0-9,]+\\.[0-9]{2})"
        );

        Matcher m = BALANCE_AS_ON.matcher(documentText);
        if (m.find()) {
            String balance = m.group(1); // 5,274.00
//        }
//        Matcher m = BAL_AS_ON.matcher(documentText);
//        if (m.find()) {
            // BAL_AS_ON has a single capturing group for the numeric amount
            long bal = parseAmountToPaisa(balance);
            log.info("[SBI] Extracted opening balance from 'Balance as on ...': {}", bal);
            return bal;
        }

        log.info("[SBI] Could not find 'Balance as on ...' opening balance in document text");
        return null;
    }
}
