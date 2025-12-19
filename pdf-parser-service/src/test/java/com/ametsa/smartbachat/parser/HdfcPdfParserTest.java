package com.ametsa.smartbachat.parser;

import com.ametsa.smartbachat.entity.TransactionEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HdfcPdfParserTest {

    private final HdfcPdfParser parser = new HdfcPdfParser();

    @Test
    void parsesCreditInterestCapitalisedRow() {
        String row = "01/01/24 CREDIT INTEREST CAPITALISED 000000000000000 31/12/23 407.00 142,451.80";

        List<TransactionEntity> txns = parser.parse(row, 14204480L); // 142,044.80 opening bal

        assertEquals(1, txns.size(), "Expected exactly one transaction parsed");
        TransactionEntity e = txns.get(0);

        // In our balance-delta model, we store the absolute txn amount and infer
        // direction from balance difference. The entity's raw `amount` field is
        // set from `txnAmountPaisa` during row parsing.
        assertEquals(40700L, e.getAmount());
        assertEquals(14245180L, e.getBalance());
        assertEquals("CREDIT", e.getDirection());
    }
}
