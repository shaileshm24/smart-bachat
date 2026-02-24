package com.ametsa.smartbachat.dto.budget;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetResponse {

    private UUID id;
    private String budgetMonth;
    private UUID templateId;
    private String templateName;

    // Amounts in rupees
    private Double totalIncome;
    private Double totalBudget;
    private Double totalSpent;
    private Double remainingBudget;
    private Double spentPercent;

    private String status;
    private String notes;

    private List<BudgetCategoryDto> categories;

    // Summary stats
    private Integer categoriesOnTrack;
    private Integer categoriesOverBudget;

    private Instant createdAt;
    private Instant updatedAt;
}

