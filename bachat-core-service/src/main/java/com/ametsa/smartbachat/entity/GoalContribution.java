package com.ametsa.smartbachat.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents a contribution towards a savings goal.
 * Can be manual entry or auto-detected from transactions.
 */
@Entity
@Table(name = "goal_contributions", indexes = {
        @Index(name = "idx_contribution_goal", columnList = "goal_id"),
        @Index(name = "idx_contribution_date", columnList = "contribution_date"),
        @Index(name = "idx_contribution_transaction", columnList = "transaction_id")
})
public class GoalContribution {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "goal_id", nullable = false)
    private UUID goalId;

    // Amount in paisa
    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "contribution_date", nullable = false)
    private LocalDate contributionDate;

    // MANUAL, AUTO_DETECTED, TRANSFER, RECURRING
    @Column(name = "source", nullable = false)
    private String source;

    // Optional link to a transaction (for auto-detected contributions)
    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public GoalContribution() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.contributionDate = LocalDate.now();
        this.source = "MANUAL";
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getGoalId() { return goalId; }
    public void setGoalId(UUID goalId) { this.goalId = goalId; }

    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }

    public LocalDate getContributionDate() { return contributionDate; }
    public void setContributionDate(LocalDate contributionDate) { this.contributionDate = contributionDate; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

