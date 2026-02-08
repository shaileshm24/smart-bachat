package com.ametsa.smartbachat.dto.setu;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a data session with Setu AA.
 * Based on Setu API v2: POST /v2/sessions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetuDataSessionRequest {

    @JsonProperty("consentId")
    private String consentId;

    @JsonProperty("dataRange")
    private DataRange dataRange;

    @JsonProperty("format")
    private String format; // json or xml

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataRange {
        @JsonProperty("from")
        private String from; // ISO date

        @JsonProperty("to")
        private String to;
    }
}

