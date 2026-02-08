package com.ametsa.smartbachat.parser;

import com.ametsa.smartbachat.entity.TransactionEntity;
import com.ametsa.smartbachat.util.PdfParserStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HDFC Bank PDF parser
 *
 * Flow:
 * 1. Extract Opening Balance (if present)
 * 2. Parse rows without direction
 * 3. Apply balance-delta logic (first row uses opening balance)
 */
@Component
public class HdfcPdfParser implements PdfParserStrategy {

    private static final Logger log = LoggerFactory.getLogger(HdfcPdfParser.class);

    private static final Pattern DATE_AT_START =
            Pattern.compile("^(\\d{1,2}/\\d{1,2}/\\d{2,4})\\s+(.+)$");

    // Match monetary amounts like 407.00 or 142,451.80 anywhere in the row.
    // The first (second-last) match is treated as the transaction amount,
    // the last match is treated as the running balance.
    private static final Pattern AMOUNT_PATTERN =
            Pattern.compile("([0-9][0-9,]*\\.[0-9]{2})");

    private static final Pattern OPENING_BAL_PATTERN =
            Pattern.compile("(?i)(opening\\s+bal(?:ance)?|balance\\s+b/f)[^0-9]*([0-9,]+\\.[0-9]{2})");

    @Override
    public String getBankCode() {
        return "HDFC";
    }

    @Override
    public boolean requiresFullDocumentText() {
        return true;
    }

    @Override
    public Long extractOpeningBalance(String documentText) {
        Matcher m = OPENING_BAL_PATTERN.matcher(documentText);
        if (m.find()) {
            long bal = parseAmountToPaisa(m.group(2));
            return bal;
        }
        return null;
    }

    @Override
    public List<TransactionEntity> parse(String pageText, Long openingBalancePaisa) {

        if (pageText == null || pageText.isBlank()) return List.of();

        List<ParsedRow> rows = new ArrayList<>();

        String[] lines = pageText.split("\\r?\\n");
        String currentRow = null;

        // If we don't have an opening balance, just return rows parsed from
        // narration (direction inferred purely from text).
        if (openingBalancePaisa == null || openingBalancePaisa <= 0) {
            for (String raw : lines) {
                String line = raw.trim();
                if (line.isEmpty()) continue;
                if (line.startsWith("Date ") || line.startsWith("DATE ")) continue;

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
            // No balance-delta step; narration-only inference will be used.
            return rows.stream().map(r -> r.entity).toList();
        }

        // With a valid opening balance, use balance-delta to derive
        // DEBIT/CREDIT and withdrawal/deposit amounts.
        if (openingBalancePaisa != null && openingBalancePaisa > 0) {
            log.info("Opening balance in parser {}", openingBalancePaisa);
            for (String raw : lines) {
                String line = raw.trim();
                if (line.isEmpty()) continue;
                if (line.startsWith("Date ") || line.startsWith("DATE ")) continue;

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

            // 🔥 STEP 2: Apply balance-delta logic
            long prevBalance = openingBalancePaisa;

            for (ParsedRow curr : rows) {

                TransactionEntity e = curr.entity;
                long delta = curr.balancePaisa - prevBalance;

                if (delta < 0) {
                    e.setDirection("DEBIT");
                    e.setWithdrawalAmount(Math.abs(delta));
                    e.setDepositAmount(0L);
                } else if (delta > 0) {
                    e.setDirection("CREDIT");
                    e.setDepositAmount(delta);
                    e.setWithdrawalAmount(0L);
                } else {
                    applyNarrationInference(curr);
                }

                prevBalance = curr.balancePaisa;
            }

        }
        return rows.stream().map(r -> r.entity).toList();
    }

	    // ------------------------------------------------------------------
	    // INTERNAL ROW PARSER (TEXT ONLY)
	    // ------------------------------------------------------------------

    private ParsedRow parseRowInternal(String row) {

        Matcher m = DATE_AT_START.matcher(row);
        if (!m.find()) return null;

//        log.info("ROW Details {}", row);

        String dateStr = m.group(1);
        String rest = m.group(2);

// 🔥 IMPORTANT: remove footer noise
        rest = trimAfterFooter(rest);

        String lower = rest.toLowerCase();


        if (lower.contains("balance b/f") || lower.contains("opening bal")) {
            return null;
        }

        List<AmountMatch> amounts = new ArrayList<>();
        Matcher numMatcher = AMOUNT_PATTERN.matcher(rest);
        while (numMatcher.find()) {
            amounts.add(new AmountMatch(numMatcher.group(1), numMatcher.start()));
        }

        if (amounts.isEmpty()) return null;

        long balancePaisa;
        long txnAmountPaisa;
        int descEnd;

        if (amounts.size() == 1) {
            // Only one amount present in the row – treat it as the
            // transaction amount, and rely on narration (no balance delta).
            txnAmountPaisa = parseAmountToPaisa(amounts.get(0).value);
            balancePaisa = 0L;
            descEnd = amounts.get(0).startIndex;
        } else {
            // Standard case: last amount is balance, second-last is txn amount
            balancePaisa = parseAmountToPaisa(amounts.get(amounts.size() - 1).value);
            txnAmountPaisa = parseAmountToPaisa(amounts.get(amounts.size() - 2).value);
            descEnd = amounts.get(amounts.size() - 2).startIndex;
        }
        String description = rest.substring(0, descEnd).trim();

        TransactionEntity e = new TransactionEntity();

        try {
            e.setTxnDate(LocalDate.parse(normalizeDate(dateStr)));
        } catch (Exception ignored) {}

        e.setBalance(balancePaisa);
        e.setCurrency("INR");
        e.setDescription(description);
        e.setRawText(row.trim());

        // Store the absolute transaction amount on the entity so that
        // downstream logic (DTO mapping, summaries) has direct access to it.
        e.setAmount(txnAmountPaisa);

        String txnType = inferTxnType(description.toLowerCase());
        if (txnType != null) e.setTxnType(txnType);

        String merchant = extractMerchant(description, txnType);
        if (merchant != null) e.setMerchant(merchant);

        return new ParsedRow(e, txnAmountPaisa, balancePaisa, lower);
    }


    // ------------------------------------------------------------------
    // FALLBACK NARRATION LOGIC
    // ------------------------------------------------------------------

    private void applyNarrationInference(ParsedRow row) {
        TransactionEntity e = row.entity;
        boolean debit = isLikelyDebit(row.lowerText);

        if (debit) {
            e.setDirection("DEBIT");
            e.setWithdrawalAmount(row.txnAmountPaisa);
            e.setDepositAmount(0L);
        } else {
            e.setDirection("CREDIT");
            e.setDepositAmount(row.txnAmountPaisa);
            e.setWithdrawalAmount(0L);
        }
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
        String[] p = d.split("/");
        if (p.length != 3) return d;
        String yyyy = p[2].length() == 2 ? "20" + p[2] : p[2];
        return yyyy + "-" + p[1] + "-" + p[0];
    }

    private long parseAmountToPaisa(String s) {
        try {
            return Math.round(Double.parseDouble(s.replace(",", "")) * 100);
        } catch (Exception e) {
            return 0L;
        }
    }

    private String buildRawText(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null) continue;
            String t = p.trim();
            if (t.isEmpty()) continue;
            if (!sb.isEmpty()) {
                sb.append(" | ");
            }
            sb.append(t);
        }
        return sb.toString();
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
    private String trimAfterFooter(String row) {
        String[] FOOTER_MARKERS = {
                "Page No", "Statement of account",
                // Removed generic "MR " marker because it appears in
                // legitimate payee/merchant names (e.g. "UPI-MR ...",
                // "RAJ MEDICALMR ...") and was truncating real rows.
                "Account Branch", "Address :",
                "OD Limit", "Currency :", "Registered Office"
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

}



//package com.ametsa.smartbachat.parser;
//
//import com.ametsa.smartbachat.entity.TransactionEntity;
//import com.ametsa.smartbachat.util.PdfParserStrategy;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDate;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
///**
// * HDFC Bank PDF parser.
// *
// * Handles cases where:
// * - Withdrawal OR Deposit column is missing (PDF omits empty column)
// * - Balance is always the last amount
// * - Transaction amount is second-last
// */
//@Component
//public class HdfcPdfParser implements PdfParserStrategy {
//
//    private static final Logger log = LoggerFactory.getLogger(HdfcPdfParser.class);
//
//    private static final Pattern DATE_AT_START =
//            Pattern.compile("^(\\d{1,2}/\\d{1,2}/\\d{2,4})\\s+(.+)$");
//
//    private static final Pattern AMOUNT_PATTERN =
//            Pattern.compile("([0-9]{1,3}(?:,[0-9]{3})*\\.[0-9]{2})");
//
//    @Override
//    public List<TransactionEntity> parse(String pageText) {
//
//        List<ParsedRow> rows = new ArrayList<>();
//        if (pageText == null || pageText.isBlank()) return List.of();
//
//        String[] lines = pageText.split("\\r?\\n");
//        String currentRow = null;
//
//        for (String raw : lines) {
//            String line = raw.trim();
//            if (line.isEmpty()) continue;
//            if (line.startsWith("Date ") || line.startsWith("DATE ")) continue;
//
//            Matcher m = DATE_AT_START.matcher(line);
//            if (m.find()) {
//                if (currentRow != null) {
//                    ParsedRow r = parseRowInternal(currentRow);
//                    if (r != null) rows.add(r);
//                }
//                currentRow = line;
//            } else if (currentRow != null) {
//                currentRow += " " + line;
//            }
//        }
//
//        if (currentRow != null) {
//            ParsedRow r = parseRowInternal(currentRow);
//            if (r != null) rows.add(r);
//        }
//
//        // 🔥 SECOND PASS — balance delta logic
//        for (int i = 0; i < rows.size(); i++) {
//
//            ParsedRow curr = rows.get(i);
//            TransactionEntity e = curr.entity;
//
//            if (i == 0) {
//                // first row → fallback to narration
//                applyNarrationInference(curr);
//                continue;
//            }
//
//            ParsedRow prev = rows.get(i - 1);
//
//            long delta = curr.balancePaisa - prev.balancePaisa;
//
//            if (delta < 0) {
//                e.setDirection("DEBIT");
//                e.setWithdrawalAmount(Math.abs(delta));
//                e.setDepositAmount(0L);
//            } else if (delta > 0) {
//                e.setDirection("CREDIT");
//                e.setDepositAmount(delta);
//                e.setWithdrawalAmount(0L);
//            } else {
//                applyNarrationInference(curr);
//            }
//        }
//
//        return rows.stream().map(r -> r.entity).toList();
//    }
//
//    // ---------- INTERNAL ROW PARSER (NO DIRECTION HERE) ----------
//
//    private ParsedRow parseRowInternal(String row) {
//
//        Matcher m = DATE_AT_START.matcher(row);
//        if (!m.find()) return null;
//
//        log.info("ROW Details {}", row);
//
//        String dateStr = m.group(1);
//        String rest = m.group(2);
//        String lower = rest.toLowerCase();
//
//        if (lower.contains("balance b/f") || lower.contains("opening bal")) {
//            return null;
//        }
//
//        List<AmountMatch> amounts = new ArrayList<>();
//        Matcher numMatcher = AMOUNT_PATTERN.matcher(rest);
//        while (numMatcher.find()) {
//            amounts.add(new AmountMatch(numMatcher.group(1), numMatcher.start()));
//        }
//
//        if (amounts.size() < 2) return null;
//
//        long balancePaisa = parseAmountToPaisa(amounts.get(amounts.size() - 1).value);
//        long txnAmountPaisa = parseAmountToPaisa(amounts.get(amounts.size() - 2).value);
//
//        int descEnd = amounts.get(amounts.size() - 2).startIndex;
//        String description = rest.substring(0, descEnd).trim();
//
//        TransactionEntity e = new TransactionEntity();
//        try {
//            e.setTxnDate(LocalDate.parse(normalizeDate(dateStr)));
//        } catch (Exception ignored) {}
//
//        e.setBalance(balancePaisa);
//        e.setCurrency("INR");
//        e.setDescription(description);
//        e.setRawText(row.trim());
//
//        String txnType = inferTxnType(description.toLowerCase());
//        if (txnType != null) e.setTxnType(txnType);
//
//        String merchant = extractMerchant(description, txnType);
//        if (merchant != null) e.setMerchant(merchant);
//
//        return new ParsedRow(e, txnAmountPaisa, balancePaisa, lower);
//    }
//
//    // ---------- FALLBACK (ONLY WHEN DELTA FAILS) ----------
//
//    private void applyNarrationInference(ParsedRow row) {
//        TransactionEntity e = row.entity;
//        boolean debit = isLikelyDebit(row.lowerText);
//
//        if (debit) {
//            e.setDirection("DEBIT");
//            e.setWithdrawalAmount(row.txnAmountPaisa);
//            e.setDepositAmount(0L);
//        } else {
//            e.setDirection("CREDIT");
//            e.setDepositAmount(row.txnAmountPaisa);
//            e.setWithdrawalAmount(0L);
//        }
//    }
//
//    // ---------- HELPERS ----------
//
//    private boolean isLikelyDebit(String t) {
//        return t.contains(" atm") || t.contains(" atw") ||
//                t.contains(" upi") || t.contains(" imps") ||
//                t.contains(" neft") || t.contains(" rtgs") ||
//                t.contains(" pos") || t.contains(" debit") ||
//                t.contains(" chq") || t.contains(" cheque");
//    }
//
//    private String inferTxnType(String t) {
//        if (t.contains("upi")) return "UPI";
//        if (t.contains("imps")) return "IMPS";
//        if (t.contains("neft")) return "NEFT";
//        if (t.contains("rtgs")) return "RTGS";
//        if (t.contains("pos")) return "POS";
//        if (t.contains("atm") || t.contains("atw")) return "ATM";
//        if (t.contains("salary")) return "SALARY";
//        if (t.contains("interest")) return "INTEREST";
//        if (t.contains("charge") || t.contains("fee")) return "CHARGE";
//        if (t.contains("refund") || t.contains("reversal")) return "REFUND";
//        return null;
//    }
//
//    private String extractMerchant(String desc, String txnType) {
//        if (desc == null) return null;
//        String[] parts = desc.split("\\s+");
//        if (parts.length == 0) return null;
//        if ("ATM".equalsIgnoreCase(txnType)) return "ATM CASH";
//
//        int start = Math.max(0, parts.length - 3);
//        StringBuilder sb = new StringBuilder();
//        for (int i = start; i < parts.length; i++) {
//            sb.append(parts[i]).append(" ");
//        }
//        return sb.toString().trim();
//    }
//
//    private String normalizeDate(String d) {
//        String[] p = d.split("/");
//        if (p.length != 3) return d;
//        String yyyy = p[2].length() == 2 ? "20" + p[2] : p[2];
//        return yyyy + "-" + p[1] + "-" + p[0];
//    }
//
//    private long parseAmountToPaisa(String s) {
//        try {
//            return Math.round(Double.parseDouble(s.replace(",", "")) * 100);
//        } catch (Exception e) {
//            return 0L;
//        }
//    }
//
//    // ---------- INTERNAL DTO ----------
//
//    private static class ParsedRow {
//        final TransactionEntity entity;
//        final long txnAmountPaisa;
//        final long balancePaisa;
//        final String lowerText;
//
//        ParsedRow(TransactionEntity e, long amt, long bal, String text) {
//            this.entity = e;
//            this.txnAmountPaisa = amt;
//            this.balancePaisa = bal;
//            this.lowerText = text;
//        }
//    }
//
//    private static class AmountMatch {
//        final String value;
//        final int startIndex;
//        AmountMatch(String v, int i) {
//            value = v;
//            startIndex = i;
//        }
//    }
//}
