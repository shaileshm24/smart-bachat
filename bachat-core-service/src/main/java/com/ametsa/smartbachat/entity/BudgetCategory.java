package com.ametsa.smartbachat.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Individual category allocation within a user's budget.
 * Tracks budget limit and actual spending per category.
 */
@Entity
@Table(name = "budget_categories", indexes = {
        @Index(name = "idx_budget_category_budget", columnList = "budget_id"),
        @Index(name = "idx_budget_category_name", columnList = "category")
})
public class BudgetCategory {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "budget_id", nullable = false)
    private UUID budgetId;

    // Category name: FOOD, TRANSPORT, UTILITIES, ENTERTAINMENT, etc.
    @Column(name = "category", nullable = false)
    private String category;

    // Budget limit for this category in paisa
    @Column(name = "budget_limit", nullable = false)
    private Long budgetLimit;

    // Percentage of total budget (for display)
    @Column(name = "percentage")
    private Double percentage;

    // Actual spent amount in paisa (updated from transactions)
    @Column(name = "spent_amount")
    private Long spentAmount;

    // Icon identifier for UI
    @Column(name = "icon")
    private String icon;

    // Color code for UI
    @Column(name = "color")
    private String color;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public BudgetCategory() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.spentAmount = 0L;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getBudgetId() { return budgetId; }
    public void setBudgetId(UUID budgetId) { this.budgetId = budgetId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Long getBudgetLimit() { return budgetLimit; }
    public void setBudgetLimit(Long budgetLimit) { this.budgetLimit = budgetLimit; }

    public Double getPercentage() { return percentage; }
    public void setPercentage(Double percentage) { this.percentage = percentage; }

    public Long getSpentAmount() { return spentAmount; }
    public void setSpentAmount(Long spentAmount) { this.spentAmount = spentAmount; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    // Helper methods
    public Long getRemainingAmount() {
        return (budgetLimit != null ? budgetLimit : 0L) - (spentAmount != null ? spentAmount : 0L);
    }

    public double getSpentPercent() {
        if (budgetLimit == null || budgetLimit == 0) return 0.0;
        return (spentAmount != null ? spentAmount : 0L) * 100.0 / budgetLimit;
    }

    public boolean isOverBudget() {
        return spentAmount != null && budgetLimit != null && spentAmount > budgetLimit;
    }
}

