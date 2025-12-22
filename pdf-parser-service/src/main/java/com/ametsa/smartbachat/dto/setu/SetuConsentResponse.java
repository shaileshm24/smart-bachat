package com.ametsa.smartbachat.dto.setu;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO from Setu AA consent creation.
 * Based on Setu API v2: POST /v2/consents
 */
public class SetuConsentResponse {

    @JsonProperty("id")
    private String id; // Consent ID

    @JsonProperty("url")
    private String url; // Redirect URL for user consent

    @JsonProperty("status")
    private String status; // PENDING, ACTIVE, REJECTED, REVOKED, EXPIRED

    @JsonProperty("traceId")
    private String traceId;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
}

