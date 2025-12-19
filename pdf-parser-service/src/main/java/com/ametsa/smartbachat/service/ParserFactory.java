package com.ametsa.smartbachat.service;

import com.ametsa.smartbachat.util.GenericPdfParser;
import com.ametsa.smartbachat.parser.HdfcPdfParser;
import com.ametsa.smartbachat.util.PdfParserStrategy;
import org.springframework.stereotype.Component;

@Component
public class ParserFactory {

    private final GenericPdfParser genericPdfParser;
	    private final HdfcPdfParser hdfcPdfParser;

	    public ParserFactory(GenericPdfParser genericPdfParser, HdfcPdfParser hdfcPdfParser) {
	        this.genericPdfParser = genericPdfParser;
	        this.hdfcPdfParser = hdfcPdfParser;
	    }

    public PdfParserStrategy getParser(String bank) {
        if (bank == null) return genericPdfParser;
        String b = bank.toLowerCase();
        if (b.contains("hdfc")) {
	            return hdfcPdfParser;
        } else if (b.contains("icici")) {
            return genericPdfParser;
        } else {
            return genericPdfParser;
        }
    }
}
