package com.ametsa.smartbachat.dto;

import com.ametsa.smartbachat.entity.PatternType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing an uncategorized recurring pattern that the user can map.
 * These are suggestions based on transaction analysis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UncategorizedPatternDto {

    private PatternType patternType;
    private String patternValue;
    private String suggestedDisplayName;
    private int transactionCount;
    private double totalAmount;
    private String sampleDescription;
    private String suggestedCategory;
}

