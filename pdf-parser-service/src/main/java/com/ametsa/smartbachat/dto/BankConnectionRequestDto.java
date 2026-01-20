package com.ametsa.smartbachat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request DTO for initiating bank account connection.
 */
public class BankConnectionRequestDto {

    @NotNull
    private UUID profileId;

    @NotBlank
    private String mobileNumber; // User's mobile number for AA VUA

    // Optional: Specific date range for data fetch
    private String dataFromDate; // ISO date: 2023-01-01
    private String dataToDate;

    // Optional: Consent duration in months (default: 12)
    private Integer consentDurationMonths;

    // Getters and Setters
    public UUID getProfileId() { return profileId; }
    public void setProfileId(UUID profileId) { this.profileId = profileId; }
    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }
    public String getDataFromDate() { return dataFromDate; }
    public void setDataFromDate(String dataFromDate) { this.dataFromDate = dataFromDate; }
    public String getDataToDate() { return dataToDate; }
    public void setDataToDate(String dataToDate) { this.dataToDate = dataToDate; }
    public Integer getConsentDurationMonths() { return consentDurationMonths; }
    public void setConsentDurationMonths(Integer consentDurationMonths) { this.consentDurationMonths = consentDurationMonths; }
}

