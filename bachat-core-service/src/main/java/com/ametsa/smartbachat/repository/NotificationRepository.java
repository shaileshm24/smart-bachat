package com.ametsa.smartbachat.repository;

import com.ametsa.smartbachat.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Find all notifications for a profile, ordered by creation date.
     */
    List<Notification> findByProfileIdOrderByCreatedAtDesc(UUID profileId);

    /**
     * Find unread notifications for a profile.
     */
    List<Notification> findByProfileIdAndIsReadFalseOrderByCreatedAtDesc(UUID profileId);

    /**
     * Find notification by ID and profile (for security).
     */
    Optional<Notification> findByIdAndProfileId(UUID id, UUID profileId);

    /**
     * Count unread notifications.
     */
    long countByProfileIdAndIsReadFalse(UUID profileId);

    /**
     * Find notifications by type.
     */
    List<Notification> findByProfileIdAndNotificationTypeOrderByCreatedAtDesc(UUID profileId, String type);

    /**
     * Mark all notifications as read for a profile.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.profileId = :profileId AND n.isRead = false")
    int markAllAsRead(@Param("profileId") UUID profileId, @Param("readAt") Instant readAt);

    /**
     * Delete old notifications (cleanup).
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.profileId = :profileId AND n.createdAt < :before")
    int deleteOldNotifications(@Param("profileId") UUID profileId, @Param("before") Instant before);

    /**
     * Find recent notifications (limited).
     */
    @Query("SELECT n FROM Notification n WHERE n.profileId = :profileId ORDER BY n.createdAt DESC LIMIT :limit")
    List<Notification> findRecentByProfileId(@Param("profileId") UUID profileId, @Param("limit") int limit);
}

