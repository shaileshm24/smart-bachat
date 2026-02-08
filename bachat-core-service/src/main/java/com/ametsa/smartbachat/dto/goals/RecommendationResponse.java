package com.ametsa.smartbachat.dto.goals;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for AI-generated savings recommendations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponse {

    private UUID id;
    private UUID goalId;
    private String goalName;  // for context
    private String recommendationType;  // MONTHLY_SAVING, GOAL_ADJUSTMENT, SPENDING_CUT, etc.
    private String message;
    private Double suggestedAmount;  // in rupees
    private Double confidenceScore;
    private LocalDate validUntil;
    private String category;  // for SPENDING_CUT type
    private Double potentialSavings;  // in rupees
    private String actionType;  // INCREASE_SAVING, REDUCE_SPENDING, ADJUST_DEADLINE, etc.
    private Instant createdAt;
}

