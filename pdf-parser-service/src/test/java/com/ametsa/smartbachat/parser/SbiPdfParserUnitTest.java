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
}
