package com.ametsa.smartbachat.dto.notifications;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSummary {

    private Integer totalCount;
    private Integer unreadCount;
    private List<NotificationResponse> recentNotifications;
}

