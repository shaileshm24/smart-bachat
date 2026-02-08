package com.ametsa.smartbachat.dto.goals;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for a goal contribution.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContributionResponse {

    private UUID id;
    private UUID goalId;
    private Double amount;  // in rupees
    private LocalDate contributionDate;
    private String source;
    private UUID transactionId;
    private String notes;
    private Instant createdAt;
}

