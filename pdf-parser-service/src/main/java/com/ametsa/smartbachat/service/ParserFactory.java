package com.ametsa.smartbachat.service;

import com.ametsa.smartbachat.util.PdfParserStrategy;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Lightweight registry for {@link PdfParserStrategy} implementations.
 *
 * <p>New bank parsers simply implement the SPI and are auto-registered via
 * Spring; no code changes are required here beyond adding the new bean.</p>
 */
@Component
public class ParserFactory {

    private final Map<String, PdfParserStrategy> byBankCode = new HashMap<>();
    private final PdfParserStrategy fallbackParser;

    public ParserFactory(Collection<PdfParserStrategy> strategies) {
        PdfParserStrategy fallback = null;
        for (PdfParserStrategy s : strategies) {
            String code = s.getBankCode();
            if (code != null) {
                byBankCode.put(code.toUpperCase(Locale.ROOT), s);
            }
            if (fallback == null && "GENERIC".equalsIgnoreCase(code)) {
                fallback = s;
            }
        }
        this.fallbackParser = fallback;
    }

    public PdfParserStrategy getParser(String bank) {
        if (bank == null || bank.isBlank()) {
            return fallbackOrAny();
        }
        PdfParserStrategy byCode = byBankCode.get(bank.toUpperCase(Locale.ROOT));
        if (byCode != null) return byCode;
        return fallbackOrAny();
    }

    private PdfParserStrategy fallbackOrAny() {
        if (fallbackParser != null) return fallbackParser;
        // As a last resort, return any registered parser
        return byBankCode.values().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No PdfParserStrategy beans registered"));
    }
}
