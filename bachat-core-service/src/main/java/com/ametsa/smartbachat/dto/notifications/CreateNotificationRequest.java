package com.ametsa.smartbachat.dto.notifications;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationRequest {

    @NotBlank(message = "Notification type is required")
    private String notificationType;

    @NotBlank(message = "Title is required")
    private String title;

    private String message;
    private String priority;
    private String referenceType;
    private UUID referenceId;
    private String icon;
    private String actionUrl;
}

