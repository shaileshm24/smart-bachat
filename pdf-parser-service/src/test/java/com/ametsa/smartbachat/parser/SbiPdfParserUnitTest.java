package com.ametsa.smartbachat.parser;

import com.ametsa.smartbachat.entity.TransactionEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SbiPdfParserUnitTest {

    private final SbiPdfParser parser = new SbiPdfParser();

    @Test
    void parsesTypicalRowWithDebitCreditBalance() {
        // This single line mimics the SBI layout after grouping multiple
        // physical lines into one logical row: date, value date, narration,
        // then two amounts (txn amount + balance) at the end.
        String row = "8 Oct 2011 8 Oct 2011 BY TRANSFER-NEFT HDFC00002400523F11281000 054SANTHOSH KUMAR G- TRANSFER FROM 3197726044305 50,000.00 55,274.00";

        List<TransactionEntity> txns = parser.parse(row, null);

        assertEquals(1, txns.size());
        TransactionEntity e = txns.get(0);
        // 50,000.00 transaction amount, balance becomes 55,274.00
        assertEquals(5_000_000L, e.getAmount());
        assertEquals(5_527_400L, e.getBalance());
        // "BY TRANSFER-NEFT" in SBI statements is a credit into the account
        assertEquals("CREDIT", e.getDirection());
    }

    // ==================== extractOpeningBalance tests ====================

    @Test
    void extractOpeningBalance_withInrPrefix_parsesCorrectly() {
        // Format: "Balance as on 19 DEC 2025 INR 26240.00"
        String documentText = """
            Account Name                Mr. Shailesh Nivas Mali
            Address                     4th Floor, Flat No.33 Saptrishi Avenue
            Balance as on               19 DEC 2025  INR 26240.00
            Search for                  19 AUG 2025 to 19 DEC 2025
            """;

        Long balance = parser.extractOpeningBalance(documentText);

        assertNotNull(balance, "Opening balance should be extracted");
        assertEquals(2624000L, balance, "Balance should be 26240.00 in paisa");
    }

    @Test
    void extractOpeningBalance_withInrPrefix_largeAmount() {
        // Format with larger amount and commas
        String documentText = """
            Balance as on               15 NOV 2025  INR 1,50,000.50
            """;

        Long balance = parser.extractOpeningBalance(documentText);

        assertNotNull(balance, "Opening balance should be extracted");
        assertEquals(15000050L, balance, "Balance should be 1,50,000.50 in paisa");
    }

    @Test
    void extractOpeningBalance_withoutInrPrefix_slashDateFormat() {
        // Format: "Balance as on 19/12/2025 26240.00"
        String documentText = """
            Balance as on 19/12/2025 26240.00
            """;

        Long balance = parser.extractOpeningBalance(documentText);

        assertNotNull(balance, "Opening balance should be extracted");
        assertEquals(2624000L, balance, "Balance should be 26240.00 in paisa");
    }

    @Test
    void extractOpeningBalance_withoutInrPrefix_dashDateFormat() {
        // Format: "Balance as on 19-12-2025 5,274.00"
        String documentText = """
            Balance as on 19-12-2025 5,274.00
            """;

        Long balance = parser.extractOpeningBalance(documentText);

        assertNotNull(balance, "Opening balance should be extracted");
        assertEquals(527400L, balance, "Balance should be 5,274.00 in paisa");
    }

    @Test
    void extractOpeningBalance_nullInput_returnsNull() {
        assertNull(parser.extractOpeningBalance(null));
    }

    @Test
    void extractOpeningBalance_emptyInput_returnsNull() {
        assertNull(parser.extractOpeningBalance(""));
        assertNull(parser.extractOpeningBalance("   "));
    }

    @Test
    void extractOpeningBalance_noBalanceAsOn_returnsNull() {
        String documentText = """
            Account Name                Mr. Shailesh Nivas Mali
            Account Number              43627995007
            """;

        assertNull(parser.extractOpeningBalance(documentText));
    }

    @Test
    void extractOpeningBalance_splitAcrossLines_parsesCorrectly() {
        // This simulates the actual PDF extraction where text is split across lines
        // due to PDF layout not being preserved
        String documentText = """
            415002863
            19 DEC 2025  INR 26240.00
            Mr. Shailesh Nivas Mali
            Drawing Power
            22 Dec 2025
            IFS Code
            Savings A/cAccount Description
            Balance as on
            Search for
            Account Name
            """;

        Long balance = parser.extractOpeningBalance(documentText);

        assertNotNull(balance, "Opening balance should be extracted even when split across lines");
        assertEquals(2624000L, balance, "Balance should be 26240.00 in paisa");
    }

    @Test
    void extractOpeningBalance_splitAcrossLines_withCommas() {
        // Test with larger amount containing commas
        String documentText = """
            15 NOV 2025  INR 1,50,000.50
            Balance as on
            Some other text
            """;

        Long balance = parser.extractOpeningBalance(documentText);

        assertNotNull(balance, "Opening balance should be extracted");
        assertEquals(15000050L, balance, "Balance should be 1,50,000.50 in paisa");
    }
}
