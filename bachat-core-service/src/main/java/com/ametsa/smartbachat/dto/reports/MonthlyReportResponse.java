package com.ametsa.smartbachat.dto.reports;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyReportResponse {

    private String month;  // YYYY-MM format
    private String monthName;  // e.g., "February 2026"

    // Income summary
    private Double totalIncome;
    private Integer incomeTransactionCount;
    private List<CategoryBreakdown> incomeByCategory;

    // Expense summary
    private Double totalExpenses;
    private Integer expenseTransactionCount;
    private List<CategoryBreakdown> expensesByCategory;

    // Savings
    private Double netSavings;
    private Double savingsRate;  // percentage

    // Comparison with previous month
    private Double previousMonthExpenses;
    private Double expenseChangePercent;
    private String expenseTrend;  // UP, DOWN, STABLE

    private Double previousMonthIncome;
    private Double incomeChangePercent;
    private String incomeTrend;

    // Top transactions
    private List<TopTransaction> topExpenses;
    private List<TopTransaction> topIncomes;

    // Goals progress this month
    private Double goalContributions;
    private Integer goalsOnTrack;
    private Integer goalsAtRisk;

    // Bills paid this month
    private Integer billsPaid;
    private Double billsAmount;

    // Budget status (if budget exists for this month)
    private Boolean hasBudget;
    private Double budgetUtilization;  // percentage
    private Integer categoriesOverBudget;

    // Insights (basic rule-based)
    private List<String> insights;

    private Instant generatedAt;
}

