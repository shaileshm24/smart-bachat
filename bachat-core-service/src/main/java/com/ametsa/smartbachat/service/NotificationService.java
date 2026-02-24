package com.ametsa.smartbachat.service;

import com.ametsa.smartbachat.dto.notifications.*;
import com.ametsa.smartbachat.entity.Notification;
import com.ametsa.smartbachat.repository.NotificationRepository;
import com.ametsa.smartbachat.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final SecurityUtils securityUtils;

    public NotificationService(NotificationRepository notificationRepository, SecurityUtils securityUtils) {
        this.notificationRepository = notificationRepository;
        this.securityUtils = securityUtils;
    }

    /**
     * Get all notifications for the user.
     */
    public List<NotificationResponse> getAllNotifications() {
        UUID profileId = securityUtils.requireCurrentProfileId();
        return notificationRepository.findByProfileIdOrderByCreatedAtDesc(profileId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get unread notifications.
     */
    public List<NotificationResponse> getUnreadNotifications() {
        UUID profileId = securityUtils.requireCurrentProfileId();
        return notificationRepository.findByProfileIdAndIsReadFalseOrderByCreatedAtDesc(profileId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get notification summary with unread count.
     */
    public NotificationSummary getSummary() {
        UUID profileId = securityUtils.requireCurrentProfileId();
        List<Notification> recent = notificationRepository.findRecentByProfileId(profileId, 10);
        long unreadCount = notificationRepository.countByProfileIdAndIsReadFalse(profileId);

        return NotificationSummary.builder()
                .totalCount(recent.size())
                .unreadCount((int) unreadCount)
                .recentNotifications(recent.stream().map(this::mapToResponse).collect(Collectors.toList()))
                .build();
    }

    /**
     * Mark a notification as read.
     */
    @Transactional
    public NotificationResponse markAsRead(UUID notificationId) {
        UUID profileId = securityUtils.requireCurrentProfileId();
        Notification notification = notificationRepository.findByIdAndProfileId(notificationId, profileId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        notification.setIsRead(true);
        notification.setReadAt(Instant.now());
        notification = notificationRepository.save(notification);

        log.info("Marked notification as read: {}", notificationId);
        return mapToResponse(notification);
    }

    /**
     * Mark all notifications as read.
     */
    @Transactional
    public int markAllAsRead() {
        UUID profileId = securityUtils.requireCurrentProfileId();
        int count = notificationRepository.markAllAsRead(profileId, Instant.now());
        log.info("Marked {} notifications as read for profile: {}", count, profileId);
        return count;
    }

    /**
     * Delete a notification.
     */
    @Transactional
    public void deleteNotification(UUID notificationId) {
        UUID profileId = securityUtils.requireCurrentProfileId();
        Notification notification = notificationRepository.findByIdAndProfileId(notificationId, profileId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notificationRepository.delete(notification);
        log.info("Deleted notification: {}", notificationId);
    }

    /**
     * Create a notification (internal use).
     */
    @Transactional
    public Notification createNotification(UUID profileId, String type, String title, String message,
            String priority, String referenceType, UUID referenceId, String icon) {
        Notification notification = new Notification();
        notification.setProfileId(profileId);
        notification.setNotificationType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setPriority(priority != null ? priority : "MEDIUM");
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        notification.setIcon(icon != null ? icon : getDefaultIcon(type));

        notification = notificationRepository.save(notification);
        log.info("Created notification: {} for profile: {}", type, profileId);
        return notification;
    }

    // Helper methods
    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .notificationType(notification.getNotificationType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .priority(notification.getPriority())
                .referenceType(notification.getReferenceType())
                .referenceId(notification.getReferenceId())
                .icon(notification.getIcon())
                .actionUrl(notification.getActionUrl())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .timeAgo(getTimeAgo(notification.getCreatedAt()))
                .build();
    }

    private String getTimeAgo(Instant createdAt) {
        if (createdAt == null) return "";
        Duration duration = Duration.between(createdAt, Instant.now());
        long minutes = duration.toMinutes();
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " min ago";
        long hours = duration.toHours();
        if (hours < 24) return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        long days = duration.toDays();
        if (days < 7) return days + " day" + (days > 1 ? "s" : "") + " ago";
        return days / 7 + " week" + (days / 7 > 1 ? "s" : "") + " ago";
    }

    private String getDefaultIcon(String type) {
        return switch (type) {
            case "BILL_DUE" -> "receipt";
            case "GOAL_MILESTONE", "GOAL_ACHIEVED" -> "flag";
            case "BUDGET_EXCEEDED" -> "warning";
            case "SYSTEM" -> "info";
            default -> "notifications";
        };
    }
}

