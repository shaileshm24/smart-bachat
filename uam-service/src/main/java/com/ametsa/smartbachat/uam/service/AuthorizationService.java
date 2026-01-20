package com.ametsa.smartbachat.uam.service;

import com.ametsa.smartbachat.uam.entity.Permission;
import com.ametsa.smartbachat.uam.entity.Role;
import com.ametsa.smartbachat.uam.entity.User;
import com.ametsa.smartbachat.uam.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for role-based access control.
 */
@Service
public class AuthorizationService {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationService.class);

    private final UserRepository userRepository;

    public AuthorizationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Check if user has a specific role.
     */
    public boolean hasRole(UUID userId, String roleName) {
        return userRepository.findById(userId)
                .map(user -> user.getRoles().stream()
                        .anyMatch(role -> role.getName().equals(roleName)))
                .orElse(false);
    }

    /**
     * Check if user has a specific permission.
     */
    public boolean hasPermission(UUID userId, String permissionName) {
        return userRepository.findById(userId)
                .map(user -> user.getRoles().stream()
                        .flatMap(role -> role.getPermissions().stream())
                        .anyMatch(perm -> perm.getName().equals(permissionName)))
                .orElse(false);
    }

    /**
     * Check if user has any of the specified roles.
     */
    public boolean hasAnyRole(UUID userId, String... roleNames) {
        Set<String> requiredRoles = Set.of(roleNames);
        return userRepository.findById(userId)
                .map(user -> user.getRoles().stream()
                        .anyMatch(role -> requiredRoles.contains(role.getName())))
                .orElse(false);
    }

    /**
     * Check if user has all of the specified roles.
     */
    public boolean hasAllRoles(UUID userId, String... roleNames) {
        Set<String> requiredRoles = Set.of(roleNames);
        return userRepository.findById(userId)
                .map(user -> {
                    Set<String> userRoles = user.getRoles().stream()
                            .map(Role::getName)
                            .collect(Collectors.toSet());
                    return userRoles.containsAll(requiredRoles);
                })
                .orElse(false);
    }

    /**
     * Check if user has any of the specified permissions.
     */
    public boolean hasAnyPermission(UUID userId, String... permissionNames) {
        Set<String> requiredPerms = Set.of(permissionNames);
        return userRepository.findById(userId)
                .map(user -> user.getRoles().stream()
                        .flatMap(role -> role.getPermissions().stream())
                        .anyMatch(perm -> requiredPerms.contains(perm.getName())))
                .orElse(false);
    }

    /**
     * Get all permissions for a user.
     */
    public Set<String> getUserPermissions(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> user.getRoles().stream()
                        .flatMap(role -> role.getPermissions().stream())
                        .map(Permission::getName)
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    /**
     * Get all roles for a user.
     */
    public Set<String> getUserRoles(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    /**
     * Check if user is admin.
     */
    public boolean isAdmin(UUID userId) {
        return hasRole(userId, Role.ROLE_ADMIN);
    }

    /**
     * Check if user can access a resource.
     */
    public boolean canAccess(UUID userId, String resource, String action) {
        String permission = resource + ":" + action;
        
        // Admin has access to everything
        if (isAdmin(userId)) {
            return true;
        }
        
        return hasPermission(userId, permission) || 
               hasPermission(userId, Permission.ADMIN_ALL);
    }
}

