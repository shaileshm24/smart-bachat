package com.ametsa.smartbachat.dto.bills;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillReminderResponse {

    private UUID id;
    private String name;
    private String category;
    private Double amount;  // in rupees
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

    // Computed fields
    private Integer daysUntilDue;
    private Boolean isOverdue;
    private String status;  // UPCOMING, DUE_SOON, OVERDUE, PAID

    private LocalDate lastPaidDate;
    private Double lastPaidAmount;  // in rupees

    private Instant createdAt;
    private Instant updatedAt;
}

