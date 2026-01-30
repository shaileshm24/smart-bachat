package com.ametsa.smartbachat.dto.setu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO from Setu AA consent creation.
 * Based on Setu API v2: POST /v2/consents
 */
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

    // Nested class for consent details
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConsentDetail {
        @JsonProperty("dataRange")
        private DataRange dataRange;

        @JsonProperty("consentStart")
        private String consentStart;

        @JsonProperty("consentExpiry")
        private String consentExpiry;

        public DataRange getDataRange() { return dataRange; }
        public void setDataRange(DataRange dataRange) { this.dataRange = dataRange; }
        public String getConsentStart() { return consentStart; }
        public void setConsentStart(String consentStart) { this.consentStart = consentStart; }
        public String getConsentExpiry() { return consentExpiry; }
        public void setConsentExpiry(String consentExpiry) { this.consentExpiry = consentExpiry; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataRange {
        @JsonProperty("from")
        private String from;

        @JsonProperty("to")
        private String to;

        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public ConsentDetail getDetail() { return detail; }
    public void setDetail(ConsentDetail detail) { this.detail = detail; }
}

