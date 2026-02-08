package com.ametsa.smartbachat.dto.goals;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for goals dashboard summary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalsSummaryResponse {

    private Integer totalGoals;
    private Integer activeGoals;
    private Integer completedGoals;
    private Double totalTargetAmount;      // in rupees
    private Double totalSavedAmount;       // in rupees
    private Double overallProgressPercent;
    private Double totalSuggestedMonthlySaving;  // in rupees
    private Integer goalsOnTrack;
    private Integer goalsBehindSchedule;
    private List<GoalResponse> goals;
    private List<RecommendationResponse> recommendations;
}

