package com.ametsa.smartbachat.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Savings goal progress summary for dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingsGoalSummary {

    /**
     * Total target amount across all active goals (in rupees).
     */
    private Double totalTarget;

    /**
     * Total saved amount across all active goals (in rupees).
     */
    private Double totalSaved;

    /**
     * Overall progress percentage (0-100).
     */
    private Double progressPercent;

    /**
     * Suggested monthly saving to stay on track (in rupees).
     */
    private Double suggestedMonthlySaving;

    /**
     * Number of active goals.
     */
    private Integer activeGoals;

    /**
     * Number of goals on track.
     */
    private Integer goalsOnTrack;

    /**
     * Number of goals behind schedule.
     */
    private Integer goalsBehindSchedule;
}

