package com.ametsa.smartbachat.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents a user's savings goal.
 * Users can create goals for travel, gadgets, emergency funds, etc.
 */
@Entity
@Table(name = "savings_goals", indexes = {
        @Index(name = "idx_savings_goal_profile", columnList = "profile_id"),
        @Index(name = "idx_savings_goal_status", columnList = "status"),
        @Index(name = "idx_savings_goal_type", columnList = "goal_type")
})
public class SavingsGoal {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    @Column(name = "name", nullable = false)
    private String name;

    // TRAVEL, GADGET, EMERGENCY, HOME, VEHICLE, EDUCATION, WEDDING, CUSTOM
    @Column(name = "goal_type", nullable = false)
    private String goalType;

    // Target amount in paisa
    @Column(name = "target_amount", nullable = false)
    private Long targetAmount;

    // Current saved amount in paisa
    @Column(name = "current_amount", nullable = false)
    private Long currentAmount;

    @Column(name = "deadline")
    private LocalDate deadline;

    // HIGH, MEDIUM, LOW
    @Column(name = "priority")
    private String priority;

    // ACTIVE, PAUSED, COMPLETED, CANCELLED
    @Column(name = "status", nullable = false)
    private String status;

    // Icon identifier for UI
    @Column(name = "icon")
    private String icon;

    // Color code for UI
    @Column(name = "color")
    private String color;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    // AI-suggested monthly saving amount in paisa
    @Column(name = "suggested_monthly_saving")
    private Long suggestedMonthlySaving;

    // Projected completion date based on current saving rate
    @Column(name = "projected_completion_date")
    private LocalDate projectedCompletionDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public SavingsGoal() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.currentAmount = 0L;
        this.status = "ACTIVE";
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getProfileId() { return profileId; }
    public void setProfileId(UUID profileId) { this.profileId = profileId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGoalType() { return goalType; }
    public void setGoalType(String goalType) { this.goalType = goalType; }

    public Long getTargetAmount() { return targetAmount; }
    public void setTargetAmount(Long targetAmount) { this.targetAmount = targetAmount; }

    public Long getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(Long currentAmount) { this.currentAmount = currentAmount; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Long getSuggestedMonthlySaving() { return suggestedMonthlySaving; }
    public void setSuggestedMonthlySaving(Long suggestedMonthlySaving) { this.suggestedMonthlySaving = suggestedMonthlySaving; }

    public LocalDate getProjectedCompletionDate() { return projectedCompletionDate; }
    public void setProjectedCompletionDate(LocalDate projectedCompletionDate) { this.projectedCompletionDate = projectedCompletionDate; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    // Helper methods
    public double getProgressPercent() {
        if (targetAmount == null || targetAmount == 0) return 0.0;
        return (currentAmount * 100.0) / targetAmount;
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(status) || (currentAmount != null && currentAmount >= targetAmount);
    }

    public Long getRemainingAmount() {
        return targetAmount - (currentAmount != null ? currentAmount : 0L);
    }
}

