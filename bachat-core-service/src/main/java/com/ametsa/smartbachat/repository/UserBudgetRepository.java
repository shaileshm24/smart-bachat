package com.ametsa.smartbachat.repository;

import com.ametsa.smartbachat.entity.UserBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserBudgetRepository extends JpaRepository<UserBudget, UUID> {

    /**
     * Find budget by profile and month.
     */
    Optional<UserBudget> findByProfileIdAndBudgetMonth(UUID profileId, String budgetMonth);

    /**
     * Find all budgets for a profile.
     */
    List<UserBudget> findByProfileIdOrderByBudgetMonthDesc(UUID profileId);

    /**
     * Find active budgets for a profile.
     */
    List<UserBudget> findByProfileIdAndStatusOrderByBudgetMonthDesc(UUID profileId, String status);

    /**
     * Find budget by ID and profile (for security).
     */
    Optional<UserBudget> findByIdAndProfileId(UUID id, UUID profileId);

    /**
     * Check if budget exists for a month.
     */
    boolean existsByProfileIdAndBudgetMonth(UUID profileId, String budgetMonth);

    /**
     * Get recent budgets (last N months).
     */
    @Query("SELECT b FROM UserBudget b WHERE b.profileId = :profileId ORDER BY b.budgetMonth DESC LIMIT :limit")
    List<UserBudget> findRecentBudgets(@Param("profileId") UUID profileId, @Param("limit") int limit);
}

