package com.ametsa.smartbachat.dto.goals;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for a savings goal with progress information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalResponse {

    private UUID id;
    private String name;
    private String goalType;
    private Double targetAmount;      // in rupees
    private Double currentAmount;     // in rupees
    private Double remainingAmount;   // in rupees
    private Double progressPercent;
    private LocalDate deadline;
    private String priority;
    private String status;
    private String icon;
    private String color;
    private String notes;
    private LocalDate projectedCompletionDate;
    private Double suggestedMonthlySaving;  // in rupees
    private Integer daysRemaining;
    private Boolean isOnTrack;
    private List<ContributionResponse> recentContributions;
    private Instant createdAt;
    private Instant updatedAt;
}

