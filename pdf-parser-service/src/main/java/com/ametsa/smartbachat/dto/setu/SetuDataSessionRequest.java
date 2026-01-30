package com.ametsa.smartbachat.dto.setu;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for creating a data session with Setu AA.
 * Based on Setu API v2: POST /v2/sessions
 */
public class SetuDataSessionRequest {

    @JsonProperty("consentId")
    private String consentId;

    @JsonProperty("dataRange")
    private DataRange dataRange;

    @JsonProperty("format")
    private String format; // json or xml

    public static class DataRange {
        @JsonProperty("from")
        private String from; // ISO date

        @JsonProperty("to")
        private String to;

        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }
    }

    // Getters and Setters
    public String getConsentId() { return consentId; }
    public void setConsentId(String consentId) { this.consentId = consentId; }
    public DataRange getDataRange() { return dataRange; }
    public void setDataRange(DataRange dataRange) { this.dataRange = dataRange; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
}

