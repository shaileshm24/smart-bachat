package com.ametsa.smartbachat.dto.setu;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response DTO for fetched financial data from Setu AA.
 * Based on Setu API v2: GET /v2/sessions/:sessionId
 */
public class SetuFIDataResponse {

    @JsonProperty("id")
    private String sessionId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("fips")
    private List<FIPData> fips;

    @JsonProperty("traceId")
    private String traceId;

    public static class FIPData {
        @JsonProperty("fipId")
        private String fipId;

        @JsonProperty("accounts")
        private List<AccountData> accounts;

        public String getFipId() { return fipId; }
        public void setFipId(String fipId) { this.fipId = fipId; }
        public List<AccountData> getAccounts() { return accounts; }
        public void setAccounts(List<AccountData> accounts) { this.accounts = accounts; }
    }

    public static class AccountData {
        @JsonProperty("maskedAccNumber")
        private String maskedAccNumber;

        @JsonProperty("linkRefNumber")
        private String linkRefNumber;

        @JsonProperty("fiType")
        private String fiType;

        @JsonProperty("data")
        private FinancialData data;

        public String getMaskedAccNumber() { return maskedAccNumber; }
        public void setMaskedAccNumber(String maskedAccNumber) { this.maskedAccNumber = maskedAccNumber; }
        public String getLinkRefNumber() { return linkRefNumber; }
        public void setLinkRefNumber(String linkRefNumber) { this.linkRefNumber = linkRefNumber; }
        public String getFiType() { return fiType; }
        public void setFiType(String fiType) { this.fiType = fiType; }
        public FinancialData getData() { return data; }
        public void setData(FinancialData data) { this.data = data; }
    }

    public static class FinancialData {
        @JsonProperty("account")
        private AccountInfo account;

        @JsonProperty("transactions")
        private TransactionList transactions;

        public AccountInfo getAccount() { return account; }
        public void setAccount(AccountInfo account) { this.account = account; }
        public TransactionList getTransactions() { return transactions; }
        public void setTransactions(TransactionList transactions) { this.transactions = transactions; }
    }

    public static class AccountInfo {
        @JsonProperty("type")
        private String type; // SAVINGS, CURRENT

        @JsonProperty("branch")
        private String branch;

        @JsonProperty("ifsc")
        private String ifsc;

        @JsonProperty("currentBalance")
        private String currentBalance;

        @JsonProperty("currency")
        private String currency;

        @JsonProperty("holder")
        private HolderInfo holder;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getBranch() { return branch; }
        public void setBranch(String branch) { this.branch = branch; }
        public String getIfsc() { return ifsc; }
        public void setIfsc(String ifsc) { this.ifsc = ifsc; }
        public String getCurrentBalance() { return currentBalance; }
        public void setCurrentBalance(String currentBalance) { this.currentBalance = currentBalance; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public HolderInfo getHolder() { return holder; }
        public void setHolder(HolderInfo holder) { this.holder = holder; }
    }

    public static class HolderInfo {
        @JsonProperty("name")
        private String name;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class TransactionList {
        @JsonProperty("transaction")
        private List<Transaction> transaction;

        public List<Transaction> getTransaction() { return transaction; }
        public void setTransaction(List<Transaction> transaction) { this.transaction = transaction; }
    }

    // Getters and Setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<FIPData> getFips() { return fips; }
    public void setFips(List<FIPData> fips) { this.fips = fips; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
}

