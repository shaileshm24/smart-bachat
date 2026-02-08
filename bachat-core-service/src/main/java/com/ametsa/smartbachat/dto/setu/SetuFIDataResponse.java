package com.ametsa.smartbachat.dto.setu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for fetched financial data from Setu AA.
 * Based on Setu API v2: GET /v2/sessions/:sessionId
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SetuFIDataResponse {

    @JsonProperty("id")
    private String sessionId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("fips")
    private List<FIPData> fips;

    @JsonProperty("traceId")
    private String traceId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FIPData {
        @JsonProperty("fipID")
        private String fipId;

        @JsonProperty("accounts")
        private List<AccountData> accounts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
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
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FinancialData {
        @JsonProperty("account")
        private AccountInfo account;

        @JsonProperty("transactions")
        private TransactionList transactions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
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
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AccountProfile {
        @JsonProperty("holders")
        private HoldersInfo holders;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HoldersInfo {
        @JsonProperty("type")
        private String type; // SINGLE, JOINT

        @JsonProperty("holder")
        private List<HolderDetail> holder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
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
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
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
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HolderInfo {
        @JsonProperty("name")
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TransactionList {
        @JsonProperty("transaction")
        private List<Transaction> transaction;
    }
}

