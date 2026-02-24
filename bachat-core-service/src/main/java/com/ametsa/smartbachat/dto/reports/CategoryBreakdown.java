package com.ametsa.smartbachat.dto.reports;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryBreakdown {

    private String category;
    private Double amount;  // in rupees
    private Integer transactionCount;
    private Double percentOfTotal;
    private Double percentOfIncome;
    private String icon;
    private String color;
}

