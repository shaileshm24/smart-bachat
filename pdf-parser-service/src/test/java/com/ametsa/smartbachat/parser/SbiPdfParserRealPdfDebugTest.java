package com.ametsa.smartbachat.parser;

import com.ametsa.smartbachat.util.PdfUtil;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Debug helper test to inspect the text layout of the real SBI sample PDF
 * located on the developer machine. It does not assert anything so it will
 * not fail CI runs even if the file is absent.
 */
public class SbiPdfParserRealPdfDebugTest {

    private static final String SAMPLE_PDF_PATH =
            "/Users/shaileshmali/Downloads/bank_statement/92508471-Sbi-Statement.pdf";

    @Test
    void dumpFirstPageOfRealSbiPdf() throws Exception {
        File f = new File(SAMPLE_PDF_PATH);
        if (!f.exists()) {
            System.out.println("[SBI DEBUG] Sample PDF not found at " + SAMPLE_PDF_PATH + ", skipping");
            return;
        }

        try (PDDocument doc = Loader.loadPDF(f)) {
            String firstPage = PdfUtil.extractTextFromPages(doc, 1, 1);
            writeDebugFile(firstPage);
        }
    }

	    /**
	     * Convenience debug helper: run the real SBI parser against each page of
	     * the sample PDF and log how many logical rows we detect. This is purely
	     * for local investigation and is a no-op on CI if the sample file is
	     * absent.
	     */
	    @Test
	    void parseRealSbiPdfAllPages() throws Exception {
	        File f = new File(SAMPLE_PDF_PATH);
	        if (!f.exists()) {
	            System.out.println("[SBI DEBUG] Sample PDF not found at " + SAMPLE_PDF_PATH + ", skipping");
	            return;
	        }

	        SbiPdfParser parser = new SbiPdfParser();
	        try (PDDocument doc = Loader.loadPDF(f)) {
	            int total = doc.getNumberOfPages();
	            long grandTotal = 0;
	            for (int i = 1; i <= total; i++) {
	                String pageText = PdfUtil.extractTextFromPages(doc, i, i);
	                int count = parser.parse(pageText, null).size();
	                grandTotal += count;
	                System.out.println("[SBI DEBUG] Page " + i + " parsed into " + count + " rows");
	            }
	            System.out.println("[SBI DEBUG] Grand total parsed rows across all pages: " + grandTotal);
	        }
	    }

    private void writeDebugFile(String firstPage) throws IOException {
        File out = new File("build/sbi-debug-firstpage.txt");
        out.getParentFile().mkdirs();
        try (FileWriter fw = new FileWriter(out)) {
            String[] lines = firstPage.split("\\r?\\n");
            for (int i = 0; i < lines.length; i++) {
                fw.write(String.format("%03d: %s%n", i + 1, lines[i]));
            }
        }
        System.out.println("[SBI DEBUG] Wrote first page text to " + out.getAbsolutePath());
    }
}
