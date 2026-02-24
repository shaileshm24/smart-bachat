package com.ametsa.smartbachat.dto.notifications;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private UUID id;
    private String notificationType;
    private String title;
    private String message;
    private String priority;
    private String referenceType;
    private UUID referenceId;
    private String icon;
    private String actionUrl;
    private Boolean isRead;
    private Instant readAt;
    private Instant createdAt;

    // Computed field for relative time
    private String timeAgo;
}

