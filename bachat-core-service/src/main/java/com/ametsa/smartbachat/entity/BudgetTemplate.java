package com.ametsa.smartbachat.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Predefined budget templates that users can choose from.
 * Examples: 50/30/20 rule, Zero-based budget, etc.
 */
@Entity
@Table(name = "budget_templates", indexes = {
        @Index(name = "idx_budget_template_type", columnList = "template_type"),
        @Index(name = "idx_budget_template_active", columnList = "is_active")
})
public class BudgetTemplate {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    // RULE_50_30_20, ZERO_BASED, ENVELOPE, CUSTOM
    @Column(name = "template_type", nullable = false)
    private String templateType;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    // JSON array of category allocations: [{"category": "ESSENTIALS", "percentage": 50}, ...]
    @Column(name = "category_allocations", columnDefinition = "text")
    private String categoryAllocations;

    // Is this a system template (cannot be deleted by users)
    @Column(name = "is_system")
    private Boolean isSystem;

    @Column(name = "is_active")
    private Boolean isActive;

    // Icon identifier for UI
    @Column(name = "icon")
    private String icon;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public BudgetTemplate() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.isActive = true;
        this.isSystem = false;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTemplateType() { return templateType; }
    public void setTemplateType(String templateType) { this.templateType = templateType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategoryAllocations() { return categoryAllocations; }
    public void setCategoryAllocations(String categoryAllocations) { this.categoryAllocations = categoryAllocations; }

    public Boolean getIsSystem() { return isSystem; }
    public void setIsSystem(Boolean isSystem) { this.isSystem = isSystem; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

