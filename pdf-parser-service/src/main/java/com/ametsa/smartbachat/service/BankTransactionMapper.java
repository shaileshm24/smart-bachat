package com.ametsa.smartbachat.service;

import com.ametsa.smartbachat.dto.setu.Transaction;
import com.ametsa.smartbachat.entity.TransactionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/**
 * Maps transactions from Setu AA response to TransactionEntity.
 */
@Component
public class BankTransactionMapper {

    private static final Logger log = LoggerFactory.getLogger(BankTransactionMapper.class);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    /**
     * Map a Setu AA transaction to TransactionEntity.
     * 
     * @param aaTxn The transaction from Setu AA
     * @param bankAccountId The bank account ID
     * @param profileId The user's profile ID
     * @return Mapped TransactionEntity
     */
    public TransactionEntity mapFromAA(Transaction aaTxn, UUID bankAccountId, UUID profileId) {
        TransactionEntity entity = new TransactionEntity();
        
        entity.setId(UUID.randomUUID());
        entity.setBankAccountId(bankAccountId);
        entity.setProfileId(profileId);
        entity.setSourceType("API");
        
        // Transaction ID from bank
        entity.setBankTxnId(aaTxn.getTxnId());
        
        // Parse timestamp
        if (aaTxn.getTransactionTimestamp() != null) {
            try {
                LocalDateTime timestamp = LocalDateTime.parse(aaTxn.getTransactionTimestamp(), ISO_FORMATTER);
                entity.setTxnTimestamp(timestamp);
                entity.setTxnDate(timestamp.toLocalDate());
            } catch (DateTimeParseException e) {
                log.warn("Failed to parse timestamp: {}", aaTxn.getTransactionTimestamp());
                entity.setTxnDate(LocalDate.now());
            }
        }
        
        // Parse value date
        if (aaTxn.getValueDate() != null) {
            try {
                entity.setValueDate(LocalDate.parse(aaTxn.getValueDate().substring(0, 10)));
            } catch (Exception e) {
                log.warn("Failed to parse value date: {}", aaTxn.getValueDate());
            }
        }
        
        // Amount in paisa
        entity.setAmount(parseAmountToPaisa(aaTxn.getAmount()));
        
        // Direction
        entity.setDirection(aaTxn.getType()); // DEBIT or CREDIT
        
        // Set withdrawal/deposit amounts
        if ("DEBIT".equalsIgnoreCase(aaTxn.getType())) {
            entity.setWithdrawalAmount(entity.getAmount());
            entity.setDepositAmount(0L);
        } else {
            entity.setDepositAmount(entity.getAmount());
            entity.setWithdrawalAmount(0L);
        }
        
        // Balance
        entity.setBalance(parseAmountToPaisa(aaTxn.getCurrentBalance()));
        
        // Transaction mode (UPI, NEFT, IMPS, etc.)
        entity.setTxnType(aaTxn.getMode());
        
        // Description/Narration
        entity.setDescription(aaTxn.getNarration());
        
        // Reference (could be UPI ref)
        if (aaTxn.getReference() != null) {
            entity.setUpiRef(aaTxn.getReference());
        }
        
        // Counterparty details
        if (aaTxn.getCounterparty() != null) {
            entity.setCounterpartyName(aaTxn.getCounterparty().getName());
            entity.setCounterpartyAccount(aaTxn.getCounterparty().getAccountNumber());
            entity.setCounterpartyIfsc(aaTxn.getCounterparty().getIfsc());
            
            // Use counterparty name as merchant if available
            if (aaTxn.getCounterparty().getName() != null) {
                entity.setMerchant(aaTxn.getCounterparty().getName());
            }
        }
        
        // Currency (default INR)
        entity.setCurrency("INR");
        
        // Generate dedupe key
        entity.setDedupeKey(generateDedupeKey(aaTxn, bankAccountId));
        
        // Timestamps
        entity.setCreatedAt(Instant.now());
        
        return entity;
    }

    /**
     * Parse amount string to paisa (Long).
     * Handles formats like "1234.56" or "1234"
     */
    private Long parseAmountToPaisa(String amountStr) {
        if (amountStr == null || amountStr.isEmpty()) {
            return 0L;
        }
        try {
            BigDecimal amount = new BigDecimal(amountStr.replaceAll("[^0-9.-]", ""));
            return amount.multiply(BigDecimal.valueOf(100)).longValue();
        } catch (NumberFormatException e) {
            log.warn("Failed to parse amount: {}", amountStr);
            return 0L;
        }
    }

    /**
     * Generate a unique dedupe key for the transaction.
     */
    private String generateDedupeKey(Transaction aaTxn, UUID bankAccountId) {
        StringBuilder key = new StringBuilder();
        key.append(bankAccountId.toString());
        key.append("_");
        key.append(aaTxn.getTxnId() != null ? aaTxn.getTxnId() : "");
        key.append("_");
        key.append(aaTxn.getTransactionTimestamp() != null ? aaTxn.getTransactionTimestamp() : "");
        key.append("_");
        key.append(aaTxn.getAmount() != null ? aaTxn.getAmount() : "");
        return key.toString();
    }
}

