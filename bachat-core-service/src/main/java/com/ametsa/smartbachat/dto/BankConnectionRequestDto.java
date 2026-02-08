package com.ametsa.smartbachat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for initiating bank account connection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}

