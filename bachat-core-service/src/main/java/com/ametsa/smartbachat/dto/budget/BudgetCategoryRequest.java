package com.ametsa.smartbachat.dto.budget;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetCategoryRequest {

    @NotBlank(message = "Category is required")
    private String category;

    // Either percentage or amount must be provided
    private Double percentage;

    // Budget limit in rupees (calculated from percentage if not provided)
    @Positive(message = "Budget limit must be positive")
    private Double budgetLimit;

    private String icon;
    private String color;
}

