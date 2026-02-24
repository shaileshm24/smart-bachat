package com.ametsa.smartbachat.dto.budget;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBudgetRequest {

    @Positive(message = "Total income must be positive")
    private Double totalIncome;

    private List<BudgetCategoryRequest> categories;

    private String status;
    private String notes;
}

