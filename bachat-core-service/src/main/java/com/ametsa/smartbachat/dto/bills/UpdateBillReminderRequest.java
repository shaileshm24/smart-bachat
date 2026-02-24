package com.ametsa.smartbachat.dto.bills;

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
public class UpdateBillReminderRequest {

    private String name;
    private String category;

    @Positive(message = "Amount must be positive")
    private Double amount;

    private String frequency;
    private Integer customDays;
    private LocalDate nextDueDate;
    private Integer reminderDaysBefore;
    private Boolean isActive;
    private Boolean isAutoPay;
    private String payee;
    private String accountReference;
    private String icon;
    private String color;
    private String notes;
}

