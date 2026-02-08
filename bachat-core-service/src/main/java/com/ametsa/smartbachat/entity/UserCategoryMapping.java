package com.ametsa.smartbachat.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * User-defined category mappings for transactions.
 * Allows users to categorize recurring transactions to specific counterparties
 * (e.g., UPI payments to mobile numbers, specific merchants).
 * 
 * When a transaction matches a user's mapping, it takes precedence over
 * the default keyword-based categorization.
 */
@Entity
@Table(name = "user_category_mappings", indexes = {
        @Index(name = "idx_ucm_profile", columnList = "profile_id"),
        @Index(name = "idx_ucm_pattern", columnList = "pattern_value"),
        @Index(name = "idx_ucm_category", columnList = "category")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_ucm_profile_pattern", columnNames = {"profile_id", "pattern_type", "pattern_value"})
})
public class UserCategoryMapping {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    /**
     * Type of pattern to match:
     * - COUNTERPARTY_NAME: Match counterparty name (exact or contains)
     * - UPI_ID: Match UPI ID (e.g., mobile@upi, name@bank)
     * - MOBILE_NUMBER: Match mobile number in UPI transactions
     * - DESCRIPTION_KEYWORD: Match keyword in description
     * - MERCHANT: Match merchant name
     */
    @Column(name = "pattern_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private PatternType patternType;

    /**
     * The actual pattern value to match.
     * For MOBILE_NUMBER: "9876543210"
     * For UPI_ID: "name@okaxis"
     * For COUNTERPARTY_NAME: "John Doe"
     * For MERCHANT: "Local Grocery Store"
     */
    @Column(name = "pattern_value", nullable = false)
    private String patternValue;

    /**
     * Optional display name for this mapping (user-friendly label).
     * E.g., "Mom's UPI", "Landlord Rent", "Gym Membership"
     */
    @Column(name = "display_name")
    private String displayName;

    /**
     * Category to assign when pattern matches.
     */
    @Column(name = "category", nullable = false)
    private String category;

    /**
     * Optional sub-category.
     */
    @Column(name = "sub_category")
    private String subCategory;

    /**
     * Whether this is a recurring/subscription payment.
     */
    @Column(name = "is_recurring")
    private Boolean isRecurring;

    /**
     * Number of times this mapping has been applied.
     */
    @Column(name = "match_count")
    private Integer matchCount = 0;

    /**
     * Last time this mapping was applied.
     */
    @Column(name = "last_matched_at")
    private Instant lastMatchedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public UserCategoryMapping() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.matchCount = 0;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getProfileId() { return profileId; }
    public void setProfileId(UUID profileId) { this.profileId = profileId; }

    public PatternType getPatternType() { return patternType; }
    public void setPatternType(PatternType patternType) { this.patternType = patternType; }

    public String getPatternValue() { return patternValue; }
    public void setPatternValue(String patternValue) { this.patternValue = patternValue; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }

    public Boolean getIsRecurring() { return isRecurring; }
    public void setIsRecurring(Boolean isRecurring) { this.isRecurring = isRecurring; }

    public Integer getMatchCount() { return matchCount; }
    public void setMatchCount(Integer matchCount) { this.matchCount = matchCount; }

    public Instant getLastMatchedAt() { return lastMatchedAt; }
    public void setLastMatchedAt(Instant lastMatchedAt) { this.lastMatchedAt = lastMatchedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public void incrementMatchCount() {
        this.matchCount = (this.matchCount == null ? 0 : this.matchCount) + 1;
        this.lastMatchedAt = Instant.now();
    }
}

