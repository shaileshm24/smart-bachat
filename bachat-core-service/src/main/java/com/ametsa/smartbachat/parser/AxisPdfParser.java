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
 * Parser for Axis Bank statements.
 *
 * Uses the same general ledger-style parsing approach as {@link SbiPdfParser}:
 *
 *   Date  ...description...  [Debit]  [Credit]  Balance
 */
@Component
public class AxisPdfParser implements PdfParserStrategy {

    private static final Logger log = LoggerFactory.getLogger(AxisPdfParser.class);

    private static final Pattern DATE_AT_START =
            Pattern.compile("^(\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4})\\s+(.+)$");

    private static final Pattern AMOUNT_PATTERN =
            Pattern.compile("([0-9][0-9,]*\\.[0-9]{2})");

    @Override
    public String getBankCode() {
        return "AXIS";
    }

    @Override
    public List<TransactionEntity> parse(String pageText, Long openingBalancePaisa) {

        if (pageText == null || pageText.isBlank()) return List.of();

        List<ParsedRow> rows = new ArrayList<>();
        String[] lines = pageText.split("\\r?\\n");
        String currentRow = null;

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;

            String lower = line.toLowerCase();
            if (lower.startsWith("date") || lower.contains("particulars")) {
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

        List<TransactionEntity> out = new ArrayList<>();
        for (ParsedRow r : rows) {
            out.add(r.entity);
        }
        return out;
    }

    private ParsedRow parseRowInternal(String row) {

        Matcher m = DATE_AT_START.matcher(row);
        if (!m.find()) return null;

        String dateStr = m.group(1);
        String rest = trimAfterFooter(m.group(2));
        String lower = rest.toLowerCase();

        if (lower.contains("balance b/f") || lower.contains("opening bal")) {
            return null;
        }

        List<AmountMatch> amounts = new ArrayList<>();
        Matcher numMatcher = AMOUNT_PATTERN.matcher(rest);
        while (numMatcher.find()) {
            amounts.add(new AmountMatch(numMatcher.group(1), numMatcher.start()));
        }

        if (amounts.size() < 2) return null;

        long balancePaisa = parseAmountToPaisa(amounts.get(amounts.size() - 1).value);

        long debitPaisa = 0L;
        long creditPaisa = 0L;
        long txnAmountPaisa;
        int descEndIdx;

        if (amounts.size() >= 3) {
            AmountMatch debitMatch = amounts.get(amounts.size() - 3);
            AmountMatch creditMatch = amounts.get(amounts.size() - 2);
            debitPaisa = parseAmountToPaisa(debitMatch.value);
            creditPaisa = parseAmountToPaisa(creditMatch.value);
            txnAmountPaisa = (debitPaisa > 0) ? debitPaisa : creditPaisa;
            descEndIdx = Math.min(debitMatch.startIndex, creditMatch.startIndex);
        } else {
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
            // ignore, keep null
        }

        e.setBalance(balancePaisa);
        e.setCurrency("INR");
        e.setDescription(description);
        e.setRawText(row.trim());

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
                applyNarrationInference(e, lower, txnAmountPaisa);
            }
        } else {
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
        String[] parts = d.replace('/', '-').split("-");
        if (parts.length != 3) return d;
        String dd = parts[0].length() == 1 ? "0" + parts[0] : parts[0];
        String mm = parts[1].length() == 1 ? "0" + parts[1] : parts[1];
        String yy = parts[2];
        if (yy.length() == 2) yy = "20" + yy;
        return yy + "-" + mm + "-" + dd;
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
}
