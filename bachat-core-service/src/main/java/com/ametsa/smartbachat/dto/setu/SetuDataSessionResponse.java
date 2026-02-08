package com.ametsa.smartbachat.dto.setu;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO from Setu AA data session creation.
 * Based on Setu API v2: POST /v2/sessions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetuDataSessionResponse {

    @JsonProperty("id")
    private String id; // Session ID

    @JsonProperty("status")
    private String status; // PENDING, ACTIVE, COMPLETED, EXPIRED, FAILED

    @JsonProperty("format")
    private String format;

    @JsonProperty("traceId")
    private String traceId;
}

