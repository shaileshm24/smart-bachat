package com.ametsa.smartbachat.service;

import com.ametsa.smartbachat.dto.setu.Transaction;
import com.ametsa.smartbachat.entity.TransactionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
class BankTransactionMapperTest {

    @Mock
    private TransactionCategorizationService categorizationService;

    private BankTransactionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new BankTransactionMapper(categorizationService);
        // Mock categorization to set category
        doAnswer(inv -> {
            TransactionEntity txn = inv.getArgument(0);
            txn.setCategory("OTHER");
            return null;
        }).when(categorizationService).categorizeAndUpdate(any(TransactionEntity.class));
    }

    @Nested
    @DisplayName("Basic Mapping Tests")
    class BasicMappingTests {

        @Test
        void shouldMapCreditTransaction() {
            Transaction aaTxn = createTransaction("TXN001", "CREDIT", "5000.00",
                    "2025-01-15T10:30:00Z", "SALARY CREDIT", "10000.00");
            UUID bankAccountId = UUID.randomUUID();
            UUID profileId = UUID.randomUUID();

            TransactionEntity entity = mapper.mapFromAA(aaTxn, bankAccountId, profileId);

            assertNotNull(entity);
            assertEquals("TXN001", entity.getBankTxnId());
            assertEquals("CREDIT", entity.getDirection());
            assertEquals(500000L, entity.getAmount()); // 5000.00 * 100
            assertEquals(1000000L, entity.getBalance()); // 10000.00 * 100
            assertEquals(LocalDate.of(2025, 1, 15), entity.getTxnDate());
            assertEquals("SALARY CREDIT", entity.getDescription());
            assertEquals(bankAccountId, entity.getBankAccountId());
            assertEquals(profileId, entity.getProfileId());
            assertEquals("API", entity.getSourceType());
        }

        @Test
        void shouldMapDebitTransaction() {
            Transaction aaTxn = createTransaction("TXN002", "DEBIT", "1500.50",
                    "2025-01-16T14:00:00Z", "SWIGGY ORDER", "8500.00");
            UUID bankAccountId = UUID.randomUUID();
            UUID profileId = UUID.randomUUID();

            TransactionEntity entity = mapper.mapFromAA(aaTxn, bankAccountId, profileId);

            assertEquals("DEBIT", entity.getDirection());
            assertEquals(150050L, entity.getAmount()); // 1500.50 * 100
            assertEquals(850000L, entity.getBalance());
        }

        @Test
        void shouldSetSourceTypeAsAPI() {
            Transaction aaTxn = createTransaction("TXN003", "CREDIT", "100.00",
                    "2025-01-17T09:00:00Z", "TEST", "100.00");

            TransactionEntity entity = mapper.mapFromAA(aaTxn, UUID.randomUUID(), UUID.randomUUID());

            assertEquals("API", entity.getSourceType());
        }

        @Test
        void shouldGenerateDedupeKey() {
            Transaction aaTxn = createTransaction("TXN004", "CREDIT", "200.00",
                    "2025-01-18T11:00:00Z", "TEST", "200.00");
            UUID bankAccountId = UUID.randomUUID();

            TransactionEntity entity = mapper.mapFromAA(aaTxn, bankAccountId, UUID.randomUUID());

            assertNotNull(entity.getDedupeKey());
            assertTrue(entity.getDedupeKey().contains(bankAccountId.toString()));
        }
    }

    @Nested
    @DisplayName("Amount Parsing Tests")
    class AmountParsingTests {

        @Test
        void shouldParseWholeNumber() {
            Transaction aaTxn = createTransaction("TXN", "CREDIT", "1000",
                    "2025-01-15T10:00:00Z", "TEST", "1000");

            TransactionEntity entity = mapper.mapFromAA(aaTxn, UUID.randomUUID(), UUID.randomUUID());

            assertEquals(100000L, entity.getAmount());
        }

        @Test
        void shouldParseDecimalAmount() {
            Transaction aaTxn = createTransaction("TXN", "CREDIT", "1234.56",
                    "2025-01-15T10:00:00Z", "TEST", "1234.56");

            TransactionEntity entity = mapper.mapFromAA(aaTxn, UUID.randomUUID(), UUID.randomUUID());

            assertEquals(123456L, entity.getAmount());
        }

        @Test
        void shouldHandleNullAmount() {
            Transaction aaTxn = createTransaction("TXN", "CREDIT", null,
                    "2025-01-15T10:00:00Z", "TEST", "100.00");

            TransactionEntity entity = mapper.mapFromAA(aaTxn, UUID.randomUUID(), UUID.randomUUID());

            assertEquals(0L, entity.getAmount());
        }
    }

    @Nested
    @DisplayName("Date Parsing Tests")
    class DateParsingTests {

        @Test
        void shouldParseISODateTime() {
            Transaction aaTxn = createTransaction("TXN", "CREDIT", "100.00",
                    "2025-06-20T15:30:45Z", "TEST", "100.00");

            TransactionEntity entity = mapper.mapFromAA(aaTxn, UUID.randomUUID(), UUID.randomUUID());

            assertEquals(LocalDate.of(2025, 6, 20), entity.getTxnDate());
        }

        @Test
        void shouldHandleNullDate() {
            Transaction aaTxn = createTransaction("TXN", "CREDIT", "100.00",
                    null, "TEST", "100.00");

            TransactionEntity entity = mapper.mapFromAA(aaTxn, UUID.randomUUID(), UUID.randomUUID());

            assertNull(entity.getTxnDate());
        }
    }

    @Nested
    @DisplayName("Reference and Metadata Tests")
    class ReferenceTests {

        @Test
        void shouldMapMode() {
            Transaction aaTxn = createTransaction("TXN", "CREDIT", "100.00",
                    "2025-01-15T10:00:00Z", "TEST", "100.00");
            aaTxn.setMode("UPI");

            TransactionEntity entity = mapper.mapFromAA(aaTxn, UUID.randomUUID(), UUID.randomUUID());

            assertEquals("UPI", entity.getTxnType());
        }

        @Test
        void shouldMapBankTxnId() {
            Transaction aaTxn = createTransaction("TXN123", "CREDIT", "100.00",
                    "2025-01-15T10:00:00Z", "TEST", "100.00");

            TransactionEntity entity = mapper.mapFromAA(aaTxn, UUID.randomUUID(), UUID.randomUUID());

            assertEquals("TXN123", entity.getBankTxnId());
        }
    }

    private Transaction createTransaction(String txnId, String type, String amount,
            String transactionTimestamp, String narration, String currentBalance) {
        Transaction txn = new Transaction();
        txn.setTxnId(txnId);
        txn.setType(type);
        txn.setAmount(amount);
        txn.setTransactionTimestamp(transactionTimestamp);
        txn.setNarration(narration);
        txn.setCurrentBalance(currentBalance);
        return txn;
    }
}