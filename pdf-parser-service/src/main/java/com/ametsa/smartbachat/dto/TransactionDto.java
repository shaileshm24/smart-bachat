package com.ametsa.smartbachat.dto;

import java.time.LocalDate;
import java.util.UUID;

public class TransactionDto {

    private UUID id;
    private UUID statementId;
    private UUID profileId;
    private LocalDate txnDate;
    // Amount and balance are exposed in rupees (e.g. 199.99),
    // even though we store minor units (paisa) internally.
    private Double amount;
    // Explicit indicator so clients don't have to infer from sign
    // Values: "DEBIT" or "CREDIT" (null/absent for zero amount)
    private String direction;
    private String currency;
    private String txnType;
    private String description;
    private String merchant;
    private Double balance;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getStatementId() { return statementId; }
    public void setStatementId(UUID statementId) { this.statementId = statementId; }

    public UUID getProfileId() { return profileId; }
    public void setProfileId(UUID profileId) { this.profileId = profileId; }

    public LocalDate getTxnDate() { return txnDate; }
    public void setTxnDate(LocalDate txnDate) { this.txnDate = txnDate; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

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

    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }
}
