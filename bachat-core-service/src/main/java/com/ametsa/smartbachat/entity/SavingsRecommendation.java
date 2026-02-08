package com.ametsa.smartbachat.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Stores AI-generated savings recommendations.
 * Cached to avoid repeated AI calls for the same analysis.
 */
@Entity
@Table(name = "savings_recommendations", indexes = {
        @Index(name = "idx_recommendation_profile", columnList = "profile_id"),
        @Index(name = "idx_recommendation_goal", columnList = "goal_id"),
        @Index(name = "idx_recommendation_type", columnList = "recommendation_type")
})
public class SavingsRecommendation {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    // Optional - null for general recommendations
    @Column(name = "goal_id")
    private UUID goalId;

    // MONTHLY_SAVING, GOAL_ADJUSTMENT, SPENDING_CUT, GOAL_PRIORITY, DEADLINE_WARNING
    @Column(name = "recommendation_type", nullable = false)
    private String recommendationType;

    @Column(name = "message", nullable = false, columnDefinition = "text")
    private String message;

    // Suggested amount in paisa (if applicable)
    @Column(name = "suggested_amount")
    private Long suggestedAmount;

    // AI confidence score (0.0 to 1.0)
    @Column(name = "confidence_score", precision = 3, scale = 2)
    private BigDecimal confidenceScore;

    // Recommendation is valid until this date
    @Column(name = "valid_until")
    private LocalDate validUntil;

    // User dismissed this recommendation
    @Column(name = "is_dismissed")
    private Boolean isDismissed;

    // User accepted/acted on this recommendation
    @Column(name = "is_accepted")
    private Boolean isAccepted;

    // Category of spending to cut (for SPENDING_CUT type)
    @Column(name = "category")
    private String category;

    // Potential monthly savings in paisa
    @Column(name = "potential_savings")
    private Long potentialSavings;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public SavingsRecommendation() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.isDismissed = false;
        this.isAccepted = false;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getProfileId() { return profileId; }
    public void setProfileId(UUID profileId) { this.profileId = profileId; }

    public UUID getGoalId() { return goalId; }
    public void setGoalId(UUID goalId) { this.goalId = goalId; }

    public String getRecommendationType() { return recommendationType; }
    public void setRecommendationType(String recommendationType) { this.recommendationType = recommendationType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getSuggestedAmount() { return suggestedAmount; }
    public void setSuggestedAmount(Long suggestedAmount) { this.suggestedAmount = suggestedAmount; }

    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }

    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }

    public Boolean getIsDismissed() { return isDismissed; }
    public void setIsDismissed(Boolean isDismissed) { this.isDismissed = isDismissed; }

    public Boolean getIsAccepted() { return isAccepted; }
    public void setIsAccepted(Boolean isAccepted) { this.isAccepted = isAccepted; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Long getPotentialSavings() { return potentialSavings; }
    public void setPotentialSavings(Long potentialSavings) { this.potentialSavings = potentialSavings; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    // Helper methods
    public boolean isValid() {
        return validUntil == null || !LocalDate.now().isAfter(validUntil);
    }

    public boolean isActionable() {
        return !isDismissed && !isAccepted && isValid();
    }
}

