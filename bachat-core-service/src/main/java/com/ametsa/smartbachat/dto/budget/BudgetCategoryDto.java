package com.ametsa.smartbachat.dto.budget;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetCategoryDto {

    private UUID id;
    private String category;
    private Double budgetLimit;      // in rupees
    private Double percentage;
    private Double spentAmount;      // in rupees
    private Double remainingAmount;  // in rupees
    private Double spentPercent;
    private Boolean isOverBudget;
    private String icon;
    private String color;
}

