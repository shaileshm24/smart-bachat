package com.ametsa.smartbachat.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Month's financial forecast for dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForecastSummary {

    /**
     * Projected expense for current month (in rupees).
     */
    @JsonProperty("projected_expense")
    private Double projectedExpense;

    /**
     * Projected income for current month (in rupees).
     */
    @JsonProperty("projected_income")
    private Double projectedIncome;

    /**
     * Projected savings for current month (in rupees).
     */
    @JsonProperty("projected_savings")
    private Double projectedSavings;

    /**
     * Trend compared to last month: UP, DOWN, STABLE.
     */
    private String trend;

    /**
     * Percentage change from last month.
     */
    @JsonProperty("change_percent")
    private Double changePercent;

    /**
     * Average monthly expense (based on historical data).
     */
    @JsonProperty("avg_monthly_expense")
    private Double avgMonthlyExpense;

    /**
     * Average monthly income (based on historical data).
     */
    @JsonProperty("avg_monthly_income")
    private Double avgMonthlyIncome;

    /**
     * Current savings rate percentage.
     */
    @JsonProperty("savings_rate")
    private Double savingsRate;

    /**
     * Confidence score of the forecast (0-1).
     */
    @JsonProperty("confidence_score")
    private Double confidenceScore;

    /**
     * Method used for forecasting.
     */
    @JsonProperty("forecast_method")
    private String forecastMethod;

    /**
     * AI-generated insights about the forecast.
     */
    private List<String> insights;
}

