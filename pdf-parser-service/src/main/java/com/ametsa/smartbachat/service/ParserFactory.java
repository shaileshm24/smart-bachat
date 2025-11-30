package com.ametsa.smartbachat.service;


import com.ametsa.smartbachat.util.GenericPdfParser;
import com.ametsa.smartbachat.util.PdfParserStrategy;
import org.springframework.stereotype.Component;

@Component
public class ParserFactory {

    private final GenericPdfParser genericPdfParser;

    public ParserFactory(GenericPdfParser genericPdfParser) {
        this.genericPdfParser = genericPdfParser;
    }

    public PdfParserStrategy getParser(String bank) {
        if (bank == null) return genericPdfParser;
        String b = bank.toLowerCase();
        if (b.contains("hdfc")) {
            // return HDFC parser when implemented
            return genericPdfParser;
        } else if (b.contains("icici")) {
            return genericPdfParser;
        } else {
            return genericPdfParser;
        }
    }
}
