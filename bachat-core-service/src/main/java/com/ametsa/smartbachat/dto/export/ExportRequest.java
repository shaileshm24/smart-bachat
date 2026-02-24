package com.ametsa.smartbachat.dto.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportRequest {

    // Date range
    private LocalDate startDate;
    private LocalDate endDate;

    // Filter by categories (optional)
    private List<String> categories;

    // Filter by direction: CREDIT, DEBIT, or null for both
    private String direction;

    // Export format: CSV (default), JSON
    private String format;

    // What to export: TRANSACTIONS, BUDGETS, GOALS, BILLS
    private String exportType;
}

