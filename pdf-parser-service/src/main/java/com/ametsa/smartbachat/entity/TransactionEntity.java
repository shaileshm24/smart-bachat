package com.ametsa.smartbachat.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transaction_statement", columnList = "statement_id"),
        @Index(name = "idx_transaction_bank_account", columnList = "bank_account_id"),
        @Index(name = "idx_transaction_profile", columnList = "profile_id"),
        @Index(name = "idx_transaction_date", columnList = "txn_date"),
        @Index(name = "idx_transaction_category", columnList = "category")
})
public class TransactionEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "statement_id")
    private UUID statementId;

    // Reference to connected bank account (for API-sourced transactions)
    @Column(name = "bank_account_id")
    private UUID bankAccountId;

    @Column(name = "profile_id")
    private UUID profileId;

    @Column(name = "txn_date")
    private LocalDate txnDate;

    // Precise timestamp from bank (API provides this, PDF may not)
    @Column(name = "txn_timestamp")
    private LocalDateTime txnTimestamp;

    @Column(name = "amount")
    private Long amount; // paisa

    // Explicit direction of money movement for this transaction.
    // Values typically: "DEBIT" (money out) or "CREDIT" (money in).
    @Column(name = "direction")
    private String direction;

    @Column(name = "currency")
    private String currency;

    // Transaction mode: UPI, NEFT, IMPS, RTGS, CASH, CARD, etc.
    @Column(name = "txn_type")
    private String txnType;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "merchant")
    private String merchant;

    @Column(name = "withdrawal_amount")
    private Long withdrawalAmount;

    @Column(name = "deposit_amount")
    private Long depositAmount;

    @Column(name = "balance")
    private Long balance;

    @Column(name = "dedupe_key")
    private String dedupeKey;

    @Column(name = "raw_text", columnDefinition = "text")
    private String rawText;

    @Column(name = "created_at")
    private Instant createdAt;

    // ========== NEW FIELDS FOR BANK API INTEGRATION ==========

    // Source of transaction data: "PDF" or "API"
    @Column(name = "source_type")
    private String sourceType;

    // Bank's unique transaction reference/ID
    @Column(name = "bank_txn_id")
    private String bankTxnId;

    // UPI reference number (if applicable)
    @Column(name = "upi_ref")
    private String upiRef;

    // Counterparty details (who you paid or received from)
    @Column(name = "counterparty_name")
    private String counterpartyName;

    @Column(name = "counterparty_account")
    private String counterpartyAccount;

    @Column(name = "counterparty_ifsc")
    private String counterpartyIfsc;

    // Category for expense tracking (FOOD, TRANSPORT, UTILITIES, etc.)
    @Column(name = "category")
    private String category;

    // Sub-category for more granular tracking
    @Column(name = "sub_category")
    private String subCategory;

    // User-defined tags (comma-separated or JSON array)
    @Column(name = "tags", columnDefinition = "text")
    private String tags;

    // User notes/memo
    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    // Is this a recurring transaction? (subscription, EMI, etc.)
    @Column(name = "is_recurring")
    private Boolean isRecurring;

    // Recurring transaction group ID (links related recurring txns)
    @Column(name = "recurring_group_id")
    private UUID recurringGroupId;

    // Is this transaction excluded from analytics/budgets?
    @Column(name = "is_excluded")
    private Boolean isExcluded;

    // Location info (if available from bank)
    @Column(name = "location")
    private String location;

    // Cheque number (if applicable)
    @Column(name = "cheque_number")
    private String chequeNumber;

    // Value date (different from transaction date for some banks)
    @Column(name = "value_date")
    private LocalDate valueDate;

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


    public Long getWithdrawalAmount() { return withdrawalAmount; }
    public void setWithdrawalAmount(Long withdrawalAmount) { this.withdrawalAmount = withdrawalAmount; }
    public Long getDepositAmount() { return depositAmount; }
    public void setDepositAmount(Long depositAmount) { this.depositAmount = depositAmount; }
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

    // New field getters and setters
    public UUID getBankAccountId() { return bankAccountId; }
    public void setBankAccountId(UUID bankAccountId) { this.bankAccountId = bankAccountId; }
    public LocalDateTime getTxnTimestamp() { return txnTimestamp; }
    public void setTxnTimestamp(LocalDateTime txnTimestamp) { this.txnTimestamp = txnTimestamp; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getBankTxnId() { return bankTxnId; }
    public void setBankTxnId(String bankTxnId) { this.bankTxnId = bankTxnId; }
    public String getUpiRef() { return upiRef; }
    public void setUpiRef(String upiRef) { this.upiRef = upiRef; }
    public String getCounterpartyName() { return counterpartyName; }
    public void setCounterpartyName(String counterpartyName) { this.counterpartyName = counterpartyName; }
    public String getCounterpartyAccount() { return counterpartyAccount; }
    public void setCounterpartyAccount(String counterpartyAccount) { this.counterpartyAccount = counterpartyAccount; }
    public String getCounterpartyIfsc() { return counterpartyIfsc; }
    public void setCounterpartyIfsc(String counterpartyIfsc) { this.counterpartyIfsc = counterpartyIfsc; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Boolean getIsRecurring() { return isRecurring; }
    public void setIsRecurring(Boolean isRecurring) { this.isRecurring = isRecurring; }
    public UUID getRecurringGroupId() { return recurringGroupId; }
    public void setRecurringGroupId(UUID recurringGroupId) { this.recurringGroupId = recurringGroupId; }
    public Boolean getIsExcluded() { return isExcluded; }
    public void setIsExcluded(Boolean isExcluded) { this.isExcluded = isExcluded; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getChequeNumber() { return chequeNumber; }
    public void setChequeNumber(String chequeNumber) { this.chequeNumber = chequeNumber; }
    public LocalDate getValueDate() { return valueDate; }
    public void setValueDate(LocalDate valueDate) { this.valueDate = valueDate; }
}
