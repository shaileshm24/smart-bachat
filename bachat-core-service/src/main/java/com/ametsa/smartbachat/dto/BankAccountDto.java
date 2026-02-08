package com.ametsa.smartbachat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for bank account information exposed to clients.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountDto {

    private UUID id;
    private UUID profileId;
    private String bankName;
    private String maskedAccountNumber;
    private String accountType;
    private String accountHolderName;
    private String branch;
    private String ifsc;
    private String consentStatus;
    private Instant consentExpiresAt;
    private Instant lastSyncedAt;
    private Double currentBalance; // In rupees
    private String currency;
    private String nickname;
    private Boolean isPrimary;
    private Boolean isActive;
}

