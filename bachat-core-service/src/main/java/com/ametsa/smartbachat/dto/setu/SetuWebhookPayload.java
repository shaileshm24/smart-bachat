package com.ametsa.smartbachat.dto.setu;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Webhook payload from Setu AA for consent and session events.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    @JsonProperty("accounts")
    private List<LinkedAccount> accounts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkedAccount {
        @JsonProperty("fiType")
        private String fiType;

        @JsonProperty("fipId")
        private String fipId;

        @JsonProperty("maskedAccNumber")
        private String maskedAccNumber;

        @JsonProperty("linkRefNumber")
        private String linkRefNumber;
    }
}

