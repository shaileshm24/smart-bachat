package com.ametsa.smartbachat.dto.setu;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO from Setu AA data session creation.
 * Based on Setu API v2: POST /v2/sessions
 */
public class SetuDataSessionResponse {

    @JsonProperty("id")
    private String id; // Session ID

    @JsonProperty("status")
    private String status; // PENDING, ACTIVE, COMPLETED, EXPIRED, FAILED

    @JsonProperty("format")
    private String format;

    @JsonProperty("traceId")
    private String traceId;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
}

