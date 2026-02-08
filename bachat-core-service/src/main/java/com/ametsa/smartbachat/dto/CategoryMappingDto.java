package com.ametsa.smartbachat.dto;

import com.ametsa.smartbachat.entity.PatternType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for user category mapping.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryMappingDto {

    private UUID id;
    private PatternType patternType;
    private String patternValue;
    private String displayName;
    private String category;
    private String subCategory;
    private Boolean isRecurring;
    private Integer matchCount;
    private Instant lastMatchedAt;
    private Instant createdAt;
}

