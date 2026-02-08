package com.ametsa.smartbachat.dto.setu;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating a consent with Setu AA.
 * Based on Setu API v2: POST /v2/consents
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetuConsentRequest {

    @JsonProperty("consentDuration")
    private ConsentDuration consentDuration;

    @JsonProperty("vua")
    private String vua; // Virtual User Address (mobile@aa-handle)

    @JsonProperty("dataRange")
    private DataRange dataRange;

    @JsonProperty("context")
    private List<Context> context;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsentDuration {
        private String unit; // MONTH, YEAR, DAY
        private int value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataRange {
        private String from; // ISO date: 2023-01-01T00:00:00.000Z
        private String to;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Context {
        private String key;
        private String value;
    }
}

