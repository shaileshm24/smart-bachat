package com.ametsa.smartbachat.dto.setu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Transaction data from Setu AA FI response.
 * Represents a single bank transaction.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    @JsonProperty("counterparty")
    private Counterparty counterparty;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Counterparty {
        @JsonProperty("name")
        private String name;

        @JsonProperty("accountNumber")
        private String accountNumber;

        @JsonProperty("ifsc")
        private String ifsc;
    }
}

