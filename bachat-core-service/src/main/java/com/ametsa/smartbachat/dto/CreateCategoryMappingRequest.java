package com.ametsa.smartbachat.dto;

import com.ametsa.smartbachat.entity.PatternType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a category mapping.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCategoryMappingRequest {

    @NotNull(message = "Pattern type is required")
    private PatternType patternType;

    @NotBlank(message = "Pattern value is required")
    private String patternValue;

    private String displayName;

    @NotBlank(message = "Category is required")
    private String category;

    private String subCategory;

    private Boolean isRecurring;
}

