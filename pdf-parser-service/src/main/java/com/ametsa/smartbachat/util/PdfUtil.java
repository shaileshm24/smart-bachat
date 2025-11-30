package com.ametsa.smartbachat.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;

public class PdfUtil {

    public static String extractTextFromPages(PDDocument doc, int startPage, int endPage) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(startPage);
        stripper.setEndPage(endPage);
        return stripper.getText(doc);
    }
}
