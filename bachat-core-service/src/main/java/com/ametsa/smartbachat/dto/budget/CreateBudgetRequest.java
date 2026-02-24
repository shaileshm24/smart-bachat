package com.ametsa.smartbachat.dto.budget;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBudgetRequest {

    // Budget month in YYYY-MM format
    @NotBlank(message = "Budget month is required")
    private String budgetMonth;

    // Optional: Use a template
    private UUID templateId;

    // Total monthly income in rupees
    @NotNull(message = "Total income is required")
    @Positive(message = "Total income must be positive")
    private Double totalIncome;

    // Category allocations (required if no template)
    private List<BudgetCategoryRequest> categories;

    private String notes;
}

