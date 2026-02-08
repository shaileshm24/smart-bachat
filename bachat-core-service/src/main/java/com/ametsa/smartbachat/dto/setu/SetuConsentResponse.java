package com.ametsa.smartbachat.dto.setu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO from Setu AA consent creation.
 * Based on Setu API v2: POST /v2/consents
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SetuConsentResponse {

    @JsonProperty("id")
    private String id; // Consent ID

    @JsonProperty("url")
    private String url; // Redirect URL for user consent

    @JsonProperty("status")
    private String status; // PENDING, ACTIVE, REJECTED, REVOKED, EXPIRED

    @JsonProperty("traceId")
    private String traceId;

    @JsonProperty("detail")
    private ConsentDetail detail;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConsentDetail {
        @JsonProperty("dataRange")
        private DataRange dataRange;

        @JsonProperty("consentStart")
        private String consentStart;

        @JsonProperty("consentExpiry")
        private String consentExpiry;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataRange {
        @JsonProperty("from")
        private String from;

        @JsonProperty("to")
        private String to;
    }
}

