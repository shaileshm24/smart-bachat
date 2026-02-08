package com.ametsa.smartbachat.repository;

import com.ametsa.smartbachat.entity.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, UUID> {

    /**
     * Find all goals for a profile ordered by priority and creation date.
     */
    List<SavingsGoal> findByProfileIdOrderByCreatedAtDesc(UUID profileId);

    /**
     * Find active goals for a profile.
     */
    List<SavingsGoal> findByProfileIdAndStatusOrderByCreatedAtDesc(UUID profileId, String status);

    /**
     * Find goals by type for a profile.
     */
    List<SavingsGoal> findByProfileIdAndGoalTypeOrderByCreatedAtDesc(UUID profileId, String goalType);

    /**
     * Find goals with upcoming deadlines.
     */
    List<SavingsGoal> findByProfileIdAndStatusAndDeadlineBetweenOrderByDeadlineAsc(
            UUID profileId, String status, LocalDate startDate, LocalDate endDate);

    /**
     * Find goals by priority.
     */
    List<SavingsGoal> findByProfileIdAndStatusAndPriorityOrderByDeadlineAsc(
            UUID profileId, String status, String priority);

    /**
     * Count active goals for a profile.
     */
    long countByProfileIdAndStatus(UUID profileId, String status);

    /**
     * Get total target amount for all active goals.
     */
    @Query("SELECT COALESCE(SUM(g.targetAmount), 0) FROM SavingsGoal g " +
           "WHERE g.profileId = :profileId AND g.status = 'ACTIVE'")
    Long sumTargetAmountByProfileId(@Param("profileId") UUID profileId);

    /**
     * Get total current amount saved across all active goals.
     */
    @Query("SELECT COALESCE(SUM(g.currentAmount), 0) FROM SavingsGoal g " +
           "WHERE g.profileId = :profileId AND g.status = 'ACTIVE'")
    Long sumCurrentAmountByProfileId(@Param("profileId") UUID profileId);

    /**
     * Find goals that are close to completion (>= 80% progress).
     */
    @Query("SELECT g FROM SavingsGoal g " +
           "WHERE g.profileId = :profileId AND g.status = 'ACTIVE' " +
           "AND (g.currentAmount * 1.0 / g.targetAmount) >= 0.8 " +
           "ORDER BY (g.currentAmount * 1.0 / g.targetAmount) DESC")
    List<SavingsGoal> findNearlyCompletedGoals(@Param("profileId") UUID profileId);

    /**
     * Find goals that are behind schedule.
     */
    @Query("SELECT g FROM SavingsGoal g " +
           "WHERE g.profileId = :profileId AND g.status = 'ACTIVE' " +
           "AND g.deadline IS NOT NULL AND g.deadline < :checkDate " +
           "AND g.currentAmount < g.targetAmount")
    List<SavingsGoal> findOverdueGoals(@Param("profileId") UUID profileId, @Param("checkDate") LocalDate checkDate);

    /**
     * Find a specific goal by ID and profile (for security).
     */
    Optional<SavingsGoal> findByIdAndProfileId(UUID id, UUID profileId);
}

