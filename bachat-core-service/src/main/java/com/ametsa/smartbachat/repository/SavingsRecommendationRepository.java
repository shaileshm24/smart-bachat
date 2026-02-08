package com.ametsa.smartbachat.repository;

import com.ametsa.smartbachat.entity.SavingsRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SavingsRecommendationRepository extends JpaRepository<SavingsRecommendation, UUID> {

    /**
     * Find active (non-dismissed, valid) recommendations for a profile.
     */
    @Query("SELECT r FROM SavingsRecommendation r " +
           "WHERE r.profileId = :profileId " +
           "AND r.isDismissed = false " +
           "AND (r.validUntil IS NULL OR r.validUntil >= :today) " +
           "ORDER BY r.createdAt DESC")
    List<SavingsRecommendation> findActiveRecommendations(
            @Param("profileId") UUID profileId,
            @Param("today") LocalDate today);

    /**
     * Find recommendations for a specific goal.
     */
    List<SavingsRecommendation> findByGoalIdAndIsDismissedFalseOrderByCreatedAtDesc(UUID goalId);

    /**
     * Find recommendations by type.
     */
    List<SavingsRecommendation> findByProfileIdAndRecommendationTypeAndIsDismissedFalseOrderByCreatedAtDesc(
            UUID profileId, String recommendationType);

    /**
     * Find the latest recommendation of a type for a profile.
     */
    @Query("SELECT r FROM SavingsRecommendation r " +
           "WHERE r.profileId = :profileId AND r.recommendationType = :type " +
           "ORDER BY r.createdAt DESC LIMIT 1")
    SavingsRecommendation findLatestByProfileIdAndType(
            @Param("profileId") UUID profileId,
            @Param("type") String type);

    /**
     * Dismiss all recommendations for a goal.
     */
    @Modifying
    @Query("UPDATE SavingsRecommendation r SET r.isDismissed = true " +
           "WHERE r.goalId = :goalId")
    void dismissAllForGoal(@Param("goalId") UUID goalId);

    /**
     * Delete expired recommendations.
     */
    @Modifying
    @Query("DELETE FROM SavingsRecommendation r " +
           "WHERE r.validUntil IS NOT NULL AND r.validUntil < :today")
    void deleteExpiredRecommendations(@Param("today") LocalDate today);

    /**
     * Count active recommendations for a profile.
     */
    @Query("SELECT COUNT(r) FROM SavingsRecommendation r " +
           "WHERE r.profileId = :profileId " +
           "AND r.isDismissed = false " +
           "AND (r.validUntil IS NULL OR r.validUntil >= :today)")
    long countActiveRecommendations(
            @Param("profileId") UUID profileId,
            @Param("today") LocalDate today);
}

