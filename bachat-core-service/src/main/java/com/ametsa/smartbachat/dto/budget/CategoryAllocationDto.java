package com.ametsa.smartbachat.dto.budget;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryAllocationDto {

    private String category;
    private Double percentage;
    private String icon;
    private String color;
}

