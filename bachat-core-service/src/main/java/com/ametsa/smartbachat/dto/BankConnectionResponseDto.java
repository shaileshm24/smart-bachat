package com.ametsa.smartbachat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response DTO for bank connection initiation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankConnectionResponseDto {

    private UUID bankAccountId;
    private String consentId;
    private String redirectUrl; // URL to redirect user for consent approval
    private String status;
    private String message;
}

