package com.ametsa.smartbachat.dto.dashboard;

import com.ametsa.smartbachat.dto.TransactionDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Main response DTO for the dashboard API.
 * Aggregates all dashboard sections in a single response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    /**
     * Total savings across all active goals (in rupees).
     */
    private Double totalSavings;

    /**
     * Motivational nudge message for the user.
     */
    private NudgeMessage nudge;

    /**
     * Balance summary across all connected bank accounts.
     */
    private BalanceSummary balance;

    /**
     * Savings goal progress summary.
     */
    private SavingsGoalSummary savingsGoal;

    /**
     * Gamification data (streaks, badges, challenges).
     * Will be null/empty until gamification is implemented.
     */
    private GamificationSummary gamification;

    /**
     * Month's financial forecast.
     */
    private ForecastSummary forecast;

    /**
     * Active alerts and reminders.
     * Will be empty until alerts system is implemented.
     */
    private List<AlertItem> alerts;

    /**
     * Recent transactions (last 5).
     */
    private List<TransactionDto> recentTransactions;

    /**
     * Timestamp when this dashboard data was generated.
     */
    private Instant generatedAt;
}

