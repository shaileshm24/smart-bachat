package com.ametsa.smartbachat.dto.setu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Transaction data from Setu AA FI response.
 * Represents a single bank transaction.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Transaction {

    @JsonProperty("txnId")
    private String txnId;

    @JsonProperty("type")
    private String type; // DEBIT or CREDIT

    @JsonProperty("mode")
    private String mode; // UPI, NEFT, IMPS, RTGS, CASH, etc.

    @JsonProperty("amount")
    private String amount;

    @JsonProperty("currentBalance")
    private String currentBalance;

    @JsonProperty("transactionTimestamp")
    private String transactionTimestamp; // ISO format

    @JsonProperty("valueDate")
    private String valueDate;

    @JsonProperty("narration")
    private String narration;

    @JsonProperty("reference")
    private String reference;

    // Counterparty details (may not always be present)
    @JsonProperty("counterparty")
    private Counterparty counterparty;

    public static class Counterparty {
        @JsonProperty("name")
        private String name;

        @JsonProperty("accountNumber")
        private String accountNumber;

        @JsonProperty("ifsc")
        private String ifsc;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
        public String getIfsc() { return ifsc; }
        public void setIfsc(String ifsc) { this.ifsc = ifsc; }
    }

    // Getters and Setters
    public String getTxnId() { return txnId; }
    public void setTxnId(String txnId) { this.txnId = txnId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }
    public String getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(String currentBalance) { this.currentBalance = currentBalance; }
    public String getTransactionTimestamp() { return transactionTimestamp; }
    public void setTransactionTimestamp(String transactionTimestamp) { this.transactionTimestamp = transactionTimestamp; }
    public String getValueDate() { return valueDate; }
    public void setValueDate(String valueDate) { this.valueDate = valueDate; }
    public String getNarration() { return narration; }
    public void setNarration(String narration) { this.narration = narration; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public Counterparty getCounterparty() { return counterparty; }
    public void setCounterparty(Counterparty counterparty) { this.counterparty = counterparty; }
}

