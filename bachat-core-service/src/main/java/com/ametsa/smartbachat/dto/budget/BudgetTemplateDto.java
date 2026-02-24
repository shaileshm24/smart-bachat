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
public class BudgetTemplateDto {

    private UUID id;
    private String name;
    private String templateType;
    private String description;
    private List<CategoryAllocationDto> categoryAllocations;
    private Boolean isSystem;
    private String icon;
    private Instant createdAt;
}

