package com.ametsa.smartbachat.dto.setu;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response DTO for fetched financial data from Setu AA.
 * Based on Setu API v2: GET /v2/sessions/:sessionId
 */
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class SetuFIDataResponse {

    @JsonProperty("id")
    private String sessionId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("fips")
    private List<FIPData> fips;

    @JsonProperty("traceId")
    private String traceId;

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class FIPData {
        // Setu API returns "fipID" (uppercase ID)
        @JsonProperty("fipID")
        private String fipId;

        @JsonProperty("accounts")
        private List<AccountData> accounts;

        public String getFipId() { return fipId; }
        public void setFipId(String fipId) { this.fipId = fipId; }
        public List<AccountData> getAccounts() { return accounts; }
        public void setAccounts(List<AccountData> accounts) { this.accounts = accounts; }
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class AccountData {
        @JsonProperty("maskedAccNumber")
        private String maskedAccNumber;

        @JsonProperty("linkRefNumber")
        private String linkRefNumber;

        @JsonProperty("fiType")
        private String fiType;

        @JsonProperty("FIstatus")
        private String fiStatus;

        @JsonProperty("data")
        private FinancialData data;

        public String getMaskedAccNumber() { return maskedAccNumber; }
        public void setMaskedAccNumber(String maskedAccNumber) { this.maskedAccNumber = maskedAccNumber; }
        public String getLinkRefNumber() { return linkRefNumber; }
        public void setLinkRefNumber(String linkRefNumber) { this.linkRefNumber = linkRefNumber; }
        public String getFiType() { return fiType; }
        public void setFiType(String fiType) { this.fiType = fiType; }
        public String getFiStatus() { return fiStatus; }
        public void setFiStatus(String fiStatus) { this.fiStatus = fiStatus; }
        public FinancialData getData() { return data; }
        public void setData(FinancialData data) { this.data = data; }
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
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

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class AccountInfo {
        @JsonProperty("linkedAccRef")
        private String linkedAccRef;

        @JsonProperty("maskedAccNumber")
        private String maskedAccNumber;

        @JsonProperty("type")
        private String type; // SAVINGS, CURRENT, deposit

        @JsonProperty("version")
        private String version;

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

        @JsonProperty("profile")
        private AccountProfile profile;

        @JsonProperty("summary")
        private AccountSummary summary;

        @JsonProperty("transactions")
        private TransactionList transactions;

        public String getLinkedAccRef() { return linkedAccRef; }
        public void setLinkedAccRef(String linkedAccRef) { this.linkedAccRef = linkedAccRef; }
        public String getMaskedAccNumber() { return maskedAccNumber; }
        public void setMaskedAccNumber(String maskedAccNumber) { this.maskedAccNumber = maskedAccNumber; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
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
        public AccountProfile getProfile() { return profile; }
        public void setProfile(AccountProfile profile) { this.profile = profile; }
        public AccountSummary getSummary() { return summary; }
        public void setSummary(AccountSummary summary) { this.summary = summary; }
        public TransactionList getTransactions() { return transactions; }
        public void setTransactions(TransactionList transactions) { this.transactions = transactions; }
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class AccountProfile {
        @JsonProperty("holders")
        private HoldersInfo holders;

        public HoldersInfo getHolders() { return holders; }
        public void setHolders(HoldersInfo holders) { this.holders = holders; }
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class HoldersInfo {
        @JsonProperty("type")
        private String type; // SINGLE, JOINT

        @JsonProperty("holder")
        private List<HolderDetail> holder;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public List<HolderDetail> getHolder() { return holder; }
        public void setHolder(List<HolderDetail> holder) { this.holder = holder; }
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class HolderDetail {
        @JsonProperty("name")
        private String name;

        @JsonProperty("dob")
        private String dob;

        @JsonProperty("mobile")
        private String mobile;

        @JsonProperty("email")
        private String email;

        @JsonProperty("pan")
        private String pan;

        @JsonProperty("address")
        private String address;

        @JsonProperty("nominee")
        private String nominee;

        @JsonProperty("ckycCompliance")
        private String ckycCompliance;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDob() { return dob; }
        public void setDob(String dob) { this.dob = dob; }
        public String getMobile() { return mobile; }
        public void setMobile(String mobile) { this.mobile = mobile; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPan() { return pan; }
        public void setPan(String pan) { this.pan = pan; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getNominee() { return nominee; }
        public void setNominee(String nominee) { this.nominee = nominee; }
        public String getCkycCompliance() { return ckycCompliance; }
        public void setCkycCompliance(String ckycCompliance) { this.ckycCompliance = ckycCompliance; }
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class AccountSummary {
        @JsonProperty("currentBalance")
        private String currentBalance;

        @JsonProperty("currency")
        private String currency;

        @JsonProperty("ifscCode")
        private String ifscCode;

        @JsonProperty("branch")
        private String branch;

        @JsonProperty("type")
        private String type;

        public String getCurrentBalance() { return currentBalance; }
        public void setCurrentBalance(String currentBalance) { this.currentBalance = currentBalance; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getIfscCode() { return ifscCode; }
        public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }
        public String getBranch() { return branch; }
        public void setBranch(String branch) { this.branch = branch; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class HolderInfo {
        @JsonProperty("name")
        private String name;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
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

