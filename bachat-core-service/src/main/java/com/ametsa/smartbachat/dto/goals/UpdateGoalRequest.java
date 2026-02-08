package com.ametsa.smartbachat.dto.goals;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for updating an existing savings goal.
 * All fields are optional - only provided fields will be updated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGoalRequest {

    private String name;

    private String goalType;

    @Positive(message = "Target amount must be positive")
    private Double targetAmount;  // in rupees

    private LocalDate deadline;

    private String priority;

    private String status;  // ACTIVE, PAUSED, COMPLETED, CANCELLED

    private String icon;

    private String color;

    private String notes;
}

