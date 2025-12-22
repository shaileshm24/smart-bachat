package com.ametsa.smartbachat.dto.setu;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Webhook payload from Setu AA for consent and session events.
 */
public class SetuWebhookPayload {

    @JsonProperty("type")
    private String type; // CONSENT_STATUS_UPDATE, SESSION_STATUS_UPDATE

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("consentId")
    private String consentId;

    @JsonProperty("consentHandle")
    private String consentHandle;

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("notificationId")
    private String notificationId;

    // For consent approved events
    @JsonProperty("accounts")
    private List<LinkedAccount> accounts;

    public static class LinkedAccount {
        @JsonProperty("fiType")
        private String fiType;

        @JsonProperty("fipId")
        private String fipId;

        @JsonProperty("maskedAccNumber")
        private String maskedAccNumber;

        @JsonProperty("linkRefNumber")
        private String linkRefNumber;

        public String getFiType() { return fiType; }
        public void setFiType(String fiType) { this.fiType = fiType; }
        public String getFipId() { return fipId; }
        public void setFipId(String fipId) { this.fipId = fipId; }
        public String getMaskedAccNumber() { return maskedAccNumber; }
        public void setMaskedAccNumber(String maskedAccNumber) { this.maskedAccNumber = maskedAccNumber; }
        public String getLinkRefNumber() { return linkRefNumber; }
        public void setLinkRefNumber(String linkRefNumber) { this.linkRefNumber = linkRefNumber; }
    }

    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public String getConsentId() { return consentId; }
    public void setConsentId(String consentId) { this.consentId = consentId; }
    public String getConsentHandle() { return consentHandle; }
    public void setConsentHandle(String consentHandle) { this.consentHandle = consentHandle; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    public List<LinkedAccount> getAccounts() { return accounts; }
    public void setAccounts(List<LinkedAccount> accounts) { this.accounts = accounts; }
}

