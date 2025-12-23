package com.ametsa.smartbachat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Setu Account Aggregator integration.
 */
@Configuration
@ConfigurationProperties(prefix = "setu")
public class SetuConfig {

    private String baseUrl;
    private String clientId;
    private String clientSecret;
    private String productInstanceId;
    private String webhookSecret;
    private String redirectUrl; // Where to redirect after consent
    private int consentDurationMonths = 12;
    private int dataFetchMonths = 12; // How many months of data to fetch

    // FI Types to request (comma-separated)
    private String fiTypes = "DEPOSIT";

    // VUA suffix for mobile number (e.g., "@setu-aa")
    private String vuaSuffix = "@setu-aa";

    // Getters and Setters
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    public String getProductInstanceId() { return productInstanceId; }
    public void setProductInstanceId(String productInstanceId) { this.productInstanceId = productInstanceId; }
    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
    public String getRedirectUrl() { return redirectUrl; }
    public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }
    public int getConsentDurationMonths() { return consentDurationMonths; }
    public void setConsentDurationMonths(int consentDurationMonths) { this.consentDurationMonths = consentDurationMonths; }
    public int getDataFetchMonths() { return dataFetchMonths; }
    public void setDataFetchMonths(int dataFetchMonths) { this.dataFetchMonths = dataFetchMonths; }
    public String getFiTypes() { return fiTypes; }
    public void setFiTypes(String fiTypes) { this.fiTypes = fiTypes; }
    public String getVuaSuffix() { return vuaSuffix; }
    public void setVuaSuffix(String vuaSuffix) { this.vuaSuffix = vuaSuffix; }
}

