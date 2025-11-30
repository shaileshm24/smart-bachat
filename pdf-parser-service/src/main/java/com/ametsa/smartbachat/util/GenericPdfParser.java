package com.ametsa.smartbachat.util;

import com.ametsa.smartbachat.entity.TransactionEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Very basic generic parser: scans lines and tries to match common patterns like:
 *  dd-mm-yyyy  description  debit  credit  balance
 *
 * This is a naive parser for PoC. Replace with proper table extraction & templates in prod.
 */
@Component
public class GenericPdfParser implements PdfParserStrategy {

    private static final Pattern TXN_LINE = Pattern.compile("(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})\\s+(.+?)\\s+(-?\\d+[.,]?\\d*)\\s*$");

    @Override
    public List<TransactionEntity> parse(String pageText) {
        List<TransactionEntity> out = new ArrayList<>();
        if (pageText == null) return out;
        String[] lines = pageText.split("\\r?\\n");
        for (String line : lines) {
            Matcher m = TXN_LINE.matcher(line.trim());
            if (m.find()) {
                TransactionEntity e = new TransactionEntity();
                try {
                    String dateS = m.group(1);
                    LocalDate date = LocalDate.parse(normalizeDate(dateS));
                    e.setTxnDate(date);
                } catch (Exception ex) {
                    e.setTxnDate(null);
                }
                String desc = m.group(2);
                e.setDescription(desc);
                String amtS = m.group(3).replace(",", "").replace("(", "-").replace(")", "");
                try {
                    long paisa = Math.round(Double.parseDouble(amtS) * 100);
                    e.setAmount(paisa);
                } catch (Exception ex) {
                    e.setAmount(0L);
                }
                e.setRawText(line.trim());
                out.add(e);
            }
        }
        return out;
    }

    private String normalizeDate(String d) {
        // naive normalization: dd-mm-yyyy or dd/mm/yyyy -> yyyy-mm-dd
        String[] parts = d.replace("/", "-").split("-");
        if (parts.length != 3) return d;
        String dd = parts[0].length() == 1 ? "0" + parts[0] : parts[0];
        String mm = parts[1].length() == 1 ? "0" + parts[1] : parts[1];
        String yy = parts[2];
        if (yy.length() == 2) yy = "20" + yy;
        return yy + "-" + mm + "-" + dd;
    }
}
