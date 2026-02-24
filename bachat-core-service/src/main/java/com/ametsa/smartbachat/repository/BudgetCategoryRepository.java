package com.ametsa.smartbachat.repository;

import com.ametsa.smartbachat.entity.BudgetCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetCategoryRepository extends JpaRepository<BudgetCategory, UUID> {

    /**
     * Find all categories for a budget.
     */
    List<BudgetCategory> findByBudgetIdOrderByCategoryAsc(UUID budgetId);

    /**
     * Find a specific category in a budget.
     */
    Optional<BudgetCategory> findByBudgetIdAndCategory(UUID budgetId, String category);

    /**
     * Delete all categories for a budget.
     */
    @Modifying
    @Query("DELETE FROM BudgetCategory bc WHERE bc.budgetId = :budgetId")
    void deleteByBudgetId(@Param("budgetId") UUID budgetId);

    /**
     * Get total budget limit for a budget.
     */
    @Query("SELECT COALESCE(SUM(bc.budgetLimit), 0) FROM BudgetCategory bc WHERE bc.budgetId = :budgetId")
    Long sumBudgetLimitByBudgetId(@Param("budgetId") UUID budgetId);

    /**
     * Get total spent for a budget.
     */
    @Query("SELECT COALESCE(SUM(bc.spentAmount), 0) FROM BudgetCategory bc WHERE bc.budgetId = :budgetId")
    Long sumSpentAmountByBudgetId(@Param("budgetId") UUID budgetId);

    /**
     * Find categories that are over budget.
     */
    @Query("SELECT bc FROM BudgetCategory bc WHERE bc.budgetId = :budgetId AND bc.spentAmount > bc.budgetLimit")
    List<BudgetCategory> findOverBudgetCategories(@Param("budgetId") UUID budgetId);
}

