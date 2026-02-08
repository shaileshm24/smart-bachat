package com.ametsa.smartbachat.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Motivational nudge message for the dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NudgeMessage {

    /**
     * Short title for the nudge (e.g., "Great job! 🎉").
     */
    private String title;

    /**
     * Detailed message (e.g., "You saved 15% more than last month!").
     */
    private String message;

    /**
     * Type of nudge: POSITIVE, NEUTRAL, WARNING, ACTION_REQUIRED.
     */
    private String type;

    /**
     * Optional action type if user should take action.
     */
    private String actionType;

    /**
     * Optional category related to the nudge.
     */
    private String category;
}

