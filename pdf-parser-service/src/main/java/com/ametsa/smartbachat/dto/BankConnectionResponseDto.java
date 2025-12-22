package com.ametsa.smartbachat.dto;

import java.util.UUID;

/**
 * Response DTO for bank connection initiation.
 */
public class BankConnectionResponseDto {

    private UUID bankAccountId;
    private String consentId;
    private String redirectUrl; // URL to redirect user for consent approval
    private String status;
    private String message;

    public BankConnectionResponseDto() {}

    public BankConnectionResponseDto(UUID bankAccountId, String consentId, String redirectUrl, String status) {
        this.bankAccountId = bankAccountId;
        this.consentId = consentId;
        this.redirectUrl = redirectUrl;
        this.status = status;
    }

    // Getters and Setters
    public UUID getBankAccountId() { return bankAccountId; }
    public void setBankAccountId(UUID bankAccountId) { this.bankAccountId = bankAccountId; }
    public String getConsentId() { return consentId; }
    public void setConsentId(String consentId) { this.consentId = consentId; }
    public String getRedirectUrl() { return redirectUrl; }
    public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

