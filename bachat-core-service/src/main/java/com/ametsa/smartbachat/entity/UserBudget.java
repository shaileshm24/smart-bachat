package com.ametsa.smartbachat.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

/**
 * User's active budget for a specific month.
 * Links to a template and tracks budget vs actual spending.
 */
@Entity
@Table(name = "user_budgets", indexes = {
        @Index(name = "idx_user_budget_profile", columnList = "profile_id"),
        @Index(name = "idx_user_budget_month", columnList = "budget_month"),
        @Index(name = "idx_user_budget_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_budget_profile_month", columnNames = {"profile_id", "budget_month"})
})
public class UserBudget {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    // Reference to the template used (optional - can be custom)
    @Column(name = "template_id")
    private UUID templateId;

    // Budget month in YYYY-MM format
    @Column(name = "budget_month", nullable = false)
    private String budgetMonth;

    // Total monthly income in paisa
    @Column(name = "total_income")
    private Long totalIncome;

    // Total budget amount in paisa
    @Column(name = "total_budget")
    private Long totalBudget;

    // Total spent so far in paisa
    @Column(name = "total_spent")
    private Long totalSpent;

    // ACTIVE, COMPLETED, ARCHIVED
    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public UserBudget() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.status = "ACTIVE";
        this.totalSpent = 0L;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getProfileId() { return profileId; }
    public void setProfileId(UUID profileId) { this.profileId = profileId; }

    public UUID getTemplateId() { return templateId; }
    public void setTemplateId(UUID templateId) { this.templateId = templateId; }

    public String getBudgetMonth() { return budgetMonth; }
    public void setBudgetMonth(String budgetMonth) { this.budgetMonth = budgetMonth; }

    public Long getTotalIncome() { return totalIncome; }
    public void setTotalIncome(Long totalIncome) { this.totalIncome = totalIncome; }

    public Long getTotalBudget() { return totalBudget; }
    public void setTotalBudget(Long totalBudget) { this.totalBudget = totalBudget; }

    public Long getTotalSpent() { return totalSpent; }
    public void setTotalSpent(Long totalSpent) { this.totalSpent = totalSpent; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    // Helper methods
    public Long getRemainingBudget() {
        return (totalBudget != null ? totalBudget : 0L) - (totalSpent != null ? totalSpent : 0L);
    }

    public double getSpentPercent() {
        if (totalBudget == null || totalBudget == 0) return 0.0;
        return (totalSpent != null ? totalSpent : 0L) * 100.0 / totalBudget;
    }
}

