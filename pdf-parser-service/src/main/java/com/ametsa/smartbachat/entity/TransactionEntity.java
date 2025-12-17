package com.ametsa.smartbachat.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transaction_statement", columnList = "statement_id")
})
public class TransactionEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "statement_id")
    private UUID statementId;

    @Column(name = "profile_id")
    private UUID profileId;

    @Column(name = "txn_date")
    private LocalDate txnDate;

    @Column(name = "amount")
    private Long amount; // paisa

	    // Explicit direction of money movement for this transaction.
	    // Values typically: "DEBIT" (money out) or "CREDIT" (money in).
	    @Column(name = "direction")
	    private String direction;

    @Column(name = "currency")
    private String currency;

    @Column(name = "txn_type")
    private String txnType;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "merchant")
    private String merchant;

    @Column(name = "balance")
    private Long balance;

    @Column(name = "dedupe_key")
    private String dedupeKey;

    @Column(name = "raw_text", columnDefinition = "text")
    private String rawText;

    @Column(name = "created_at")
    private Instant createdAt;

    public TransactionEntity() {}

    // getters & setters omitted for brevity
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getStatementId() { return statementId; }
    public void setStatementId(UUID statementId) { this.statementId = statementId; }
    public UUID getProfileId() { return profileId; }
    public void setProfileId(UUID profileId) { this.profileId = profileId; }
    public LocalDate getTxnDate() { return txnDate; }
    public void setTxnDate(LocalDate txnDate) { this.txnDate = txnDate; }
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }

	    public String getDirection() { return direction; }
	    public void setDirection(String direction) { this.direction = direction; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getTxnType() { return txnType; }
    public void setTxnType(String txnType) { this.txnType = txnType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getMerchant() { return merchant; }
    public void setMerchant(String merchant) { this.merchant = merchant; }
    public Long getBalance() { return balance; }
    public void setBalance(Long balance) { this.balance = balance; }
    public String getDedupeKey() { return dedupeKey; }
    public void setDedupeKey(String dedupeKey) { this.dedupeKey = dedupeKey; }
    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
