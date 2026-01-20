package com.ametsa.smartbachat.dto.setu;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Request DTO for creating a consent with Setu AA.
 * Based on Setu API v2: POST /v2/consents
 */
public class SetuConsentRequest {

    @JsonProperty("consentDuration")
    private ConsentDuration consentDuration;

    @JsonProperty("vua")
    private String vua; // Virtual User Address (mobile@aa-handle)

    @JsonProperty("dataRange")
    private DataRange dataRange;

    @JsonProperty("context")
    private List<Context> context;

    // Nested classes
    public static class ConsentDuration {
        private String unit; // MONTH, YEAR, DAY
        private int value;

        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
    }

    public static class DataRange {
        private String from; // ISO date: 2023-01-01T00:00:00.000Z
        private String to;

        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }
    }

    public static class Context {
        private String key;
        private String value;

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    // Getters and Setters
    public ConsentDuration getConsentDuration() { return consentDuration; }
    public void setConsentDuration(ConsentDuration consentDuration) { this.consentDuration = consentDuration; }
    public String getVua() { return vua; }
    public void setVua(String vua) { this.vua = vua; }
    public DataRange getDataRange() { return dataRange; }
    public void setDataRange(DataRange dataRange) { this.dataRange = dataRange; }
    public List<Context> getContext() { return context; }
    public void setContext(List<Context> context) { this.context = context; }
}

