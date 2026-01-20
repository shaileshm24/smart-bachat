package com.ametsa.smartbachat.uam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.email")
public class EmailConfig {

    private String fromAddress;
    private String fromName;
    private String baseUrl;
    private Long verificationTokenExpirationMinutes = 1440L; // 24 hours
    private Long passwordResetTokenExpirationMinutes = 60L; // 1 hour

    public String getFromAddress() { return fromAddress; }
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }
    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public Long getVerificationTokenExpirationMinutes() { return verificationTokenExpirationMinutes; }
    public void setVerificationTokenExpirationMinutes(Long verificationTokenExpirationMinutes) { 
        this.verificationTokenExpirationMinutes = verificationTokenExpirationMinutes; 
    }
    public Long getPasswordResetTokenExpirationMinutes() { return passwordResetTokenExpirationMinutes; }
    public void setPasswordResetTokenExpirationMinutes(Long passwordResetTokenExpirationMinutes) { 
        this.passwordResetTokenExpirationMinutes = passwordResetTokenExpirationMinutes; 
    }
}

