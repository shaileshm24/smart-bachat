package com.ametsa.smartbachat.uam.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Permission entity for fine-grained access control.
 */
@Entity
@Table(name = "permissions", indexes = {
        @Index(name = "idx_permissions_name", columnList = "name", unique = true),
        @Index(name = "idx_permissions_resource", columnList = "resource")
})
public class Permission {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "name", unique = true, nullable = false)
    private String name;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "description")
    private String description;

    // Resource this permission applies to (e.g., "transactions", "bank_accounts")
    @Column(name = "resource")
    private String resource;

    // Action (e.g., "read", "write", "delete", "admin")
    @Column(name = "action")
    private String action;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Permission() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
    }

    public Permission(String name, String resource, String action, String description) {
        this();
        this.name = name;
        this.resource = resource;
        this.action = action;
        this.description = description;
        this.displayName = resource + ":" + action;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    // Predefined permissions
    public static final String TRANSACTIONS_READ = "transactions:read";
    public static final String TRANSACTIONS_WRITE = "transactions:write";
    public static final String TRANSACTIONS_DELETE = "transactions:delete";
    public static final String BANK_ACCOUNTS_READ = "bank_accounts:read";
    public static final String BANK_ACCOUNTS_WRITE = "bank_accounts:write";
    public static final String BANK_ACCOUNTS_DELETE = "bank_accounts:delete";
    public static final String USERS_READ = "users:read";
    public static final String USERS_WRITE = "users:write";
    public static final String USERS_DELETE = "users:delete";
    public static final String ADMIN_ALL = "admin:all";
}

