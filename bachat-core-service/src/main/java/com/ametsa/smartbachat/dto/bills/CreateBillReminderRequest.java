package com.ametsa.smartbachat.dto.bills;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBillReminderRequest {

    @NotBlank(message = "Bill name is required")
    private String name;

    // Category: RENT, UTILITIES, SUBSCRIPTION, INSURANCE, EMI, CREDIT_CARD, OTHER
    private String category;

    // Amount in rupees
    @Positive(message = "Amount must be positive")
    private Double amount;

    // Frequency: WEEKLY, MONTHLY, QUARTERLY, YEARLY, CUSTOM
    @NotBlank(message = "Frequency is required")
    private String frequency;

    // For CUSTOM frequency
    private Integer customDays;

    @NotNull(message = "Due date is required")
    private LocalDate nextDueDate;

    // Days before due date to send reminder (default: 3)
    private Integer reminderDaysBefore;

    private Boolean isAutoPay;

    private String payee;
    private String accountReference;
    private String icon;
    private String color;
    private String notes;
}

