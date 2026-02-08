package com.ametsa.smartbachat.dto.goals;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for adding a contribution to a goal.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContributionRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;  // in rupees

    private LocalDate contributionDate;  // defaults to today if not provided

    private String source;  // MANUAL, TRANSFER (defaults to MANUAL)

    private UUID transactionId;  // optional link to a transaction

    private String notes;
}

