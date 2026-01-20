package com.ametsa.smartbachat.uam.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Role entity for RBAC.
 */
@Entity
@Table(name = "roles", indexes = {
        @Index(name = "idx_roles_name", columnList = "name", unique = true)
})
public class Role {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "name", unique = true, nullable = false)
    private String name;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "description")
    private String description;

    // System roles cannot be deleted
    @Column(name = "is_system_role")
    private Boolean isSystemRole;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // Many-to-many with permissions
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    public Role() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.isSystemRole = false;
    }

    public Role(String name, String displayName, String description) {
        this();
        this.name = name;
        this.displayName = displayName;
        this.description = description;
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
    public Boolean getIsSystemRole() { return isSystemRole; }
    public void setIsSystemRole(Boolean isSystemRole) { this.isSystemRole = isSystemRole; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Set<Permission> getPermissions() { return permissions; }
    public void setPermissions(Set<Permission> permissions) { this.permissions = permissions; }

    // Helper methods
    public void addPermission(Permission permission) { this.permissions.add(permission); }
    public void removePermission(Permission permission) { this.permissions.remove(permission); }
    public boolean hasPermission(String permissionName) {
        return permissions.stream().anyMatch(p -> p.getName().equals(permissionName));
    }

    // Predefined role names
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_PREMIUM = "ROLE_PREMIUM";
    public static final String ROLE_SUPPORT = "ROLE_SUPPORT";
}

