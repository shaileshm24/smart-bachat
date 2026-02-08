package com.ametsa.smartbachat.dto.goals;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for creating a new savings goal.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGoalRequest {

    @NotBlank(message = "Goal name is required")
    private String name;

    @NotBlank(message = "Goal type is required")
    private String goalType;  // TRAVEL, GADGET, EMERGENCY, HOME, VEHICLE, EDUCATION, WEDDING, CUSTOM

    @NotNull(message = "Target amount is required")
    @Positive(message = "Target amount must be positive")
    private Double targetAmount;  // in rupees (will be converted to paisa)

    private LocalDate deadline;

    private String priority;  // HIGH, MEDIUM, LOW

    private String icon;

    private String color;

    private String notes;
}

