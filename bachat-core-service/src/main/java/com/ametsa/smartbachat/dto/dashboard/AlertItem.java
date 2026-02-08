package com.ametsa.smartbachat.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Alert/reminder item for dashboard.
 * Placeholder structure for Phase 3 implementation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertItem {

    /**
     * Unique identifier.
     */
    private String id;

    /**
     * Alert type: EMI_DUE, BILL_REMINDER, GOAL_REMINDER, 
     * CONSENT_EXPIRING, LOW_BALANCE, UNUSUAL_SPENDING, FESTIVAL_ALERT.
     */
    private String type;

    /**
     * Alert title.
     */
    private String title;

    /**
     * Detailed message.
     */
    private String message;

    /**
     * Due date if applicable.
     */
    private LocalDate dueDate;

    /**
     * Amount if applicable (in rupees).
     */
    private Double amount;

    /**
     * Priority: HIGH, MEDIUM, LOW.
     */
    private String priority;

    /**
     * Whether the alert has been read/dismissed.
     */
    private Boolean isRead;

    /**
     * Action to take when clicked.
     */
    private String actionType;

    /**
     * Related entity ID (goal ID, account ID, etc.).
     */
    private String relatedEntityId;
}

