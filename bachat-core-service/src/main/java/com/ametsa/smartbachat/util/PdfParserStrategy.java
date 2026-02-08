package com.ametsa.smartbachat.util;

import com.ametsa.smartbachat.entity.TransactionEntity;

import java.util.List;

/**
 * Generic strategy SPI for parsing bank statement PDFs.
 *
 * <p>This is the "framework" contract that bank-specific parsers implement.
 * {@link com.ametsa.smartbachat.service.ParserWorker} takes care of loading
 * PDFs with PDFBox and will either pass full-document text or per-page text
 * depending on {@link #requiresFullDocumentText()}.</p>
 */
public interface PdfParserStrategy {

    /**
     * Bank identifier that this parser handles, e.g. "HDFC", "ICICI", "SBI".
     * <p>
     * The value should match what {@link com.ametsa.smartbachat.util.BankDetectorUtil}
     * returns so that {@link com.ametsa.smartbachat.service.ParserFactory} can
     * route correctly.
     */
    String getBankCode();

    /**
     * Parse the given text (either a single page or the full document) into
     * a list of {@link TransactionEntity}.
     *
     * @param text                  page or full-document text, depending on the
     *                              value of {@link #requiresFullDocumentText()}.
     * @param openingBalancePaisa   optional opening balance in paisa if the
     *                              parser uses balance-delta logic; may be {@code null}.
     */
    List<TransactionEntity> parse(String text, Long openingBalancePaisa);

    /**
     * Whether this parser expects to receive the full document text in a
     * single call to {@link #parse(String, Long)}.
     * <p>
     * If {@code false} (default), the framework will call {@code parse(..)}
     * once per page with that page's text.
     */
    default boolean requiresFullDocumentText() {
        return false;
    }

    /**
     * Extract the opening balance (in paisa) from the document text, if the
     * bank format exposes it and the parser wants to use it.
     * <p>
     * The default implementation returns {@code null}, meaning the parser
     * does not rely on an opening balance.
     */
    default Long extractOpeningBalance(String documentText) {
        return null;
    }
}
