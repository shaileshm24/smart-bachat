package com.ametsa.smartbachat.repository;

import com.ametsa.smartbachat.entity.GoalContribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalContributionRepository extends JpaRepository<GoalContribution, UUID> {

    /**
     * Find all contributions for a goal ordered by date.
     */
    List<GoalContribution> findByGoalIdOrderByContributionDateDesc(UUID goalId);

    /**
     * Find contributions for a goal within a date range.
     */
    List<GoalContribution> findByGoalIdAndContributionDateBetweenOrderByContributionDateDesc(
            UUID goalId, LocalDate startDate, LocalDate endDate);

    /**
     * Find contributions by source type.
     */
    List<GoalContribution> findByGoalIdAndSourceOrderByContributionDateDesc(UUID goalId, String source);

    /**
     * Get total contributions for a goal.
     */
    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM GoalContribution c WHERE c.goalId = :goalId")
    Long sumAmountByGoalId(@Param("goalId") UUID goalId);

    /**
     * Get total contributions for a goal in a date range.
     */
    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM GoalContribution c " +
           "WHERE c.goalId = :goalId AND c.contributionDate BETWEEN :startDate AND :endDate")
    Long sumAmountByGoalIdAndDateRange(
            @Param("goalId") UUID goalId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Count contributions for a goal.
     */
    long countByGoalId(UUID goalId);

    /**
     * Find recent contributions (last N).
     */
    List<GoalContribution> findTop5ByGoalIdOrderByContributionDateDesc(UUID goalId);

    /**
     * Check if a transaction is already linked to a contribution.
     */
    boolean existsByTransactionId(UUID transactionId);

    /**
     * Find contribution by transaction ID.
     */
    Optional<GoalContribution> findByTransactionId(UUID transactionId);

    /**
     * Delete all contributions for a goal.
     */
    void deleteByGoalId(UUID goalId);

    /**
     * Get monthly contribution totals for a goal.
     */
    @Query("SELECT FUNCTION('DATE_TRUNC', 'month', c.contributionDate) as month, SUM(c.amount) " +
           "FROM GoalContribution c WHERE c.goalId = :goalId " +
           "GROUP BY FUNCTION('DATE_TRUNC', 'month', c.contributionDate) " +
           "ORDER BY month DESC")
    List<Object[]> getMonthlyContributionTotals(@Param("goalId") UUID goalId);
}

