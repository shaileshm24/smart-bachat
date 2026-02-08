package com.ametsa.smartbachat.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Utility class for accessing security context information.
 */
@Component
public class SecurityUtils {

    /**
     * Get the current authenticated user principal.
     */
    public Optional<UserPrincipal> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return Optional.of((UserPrincipal) authentication.getPrincipal());
        }
        return Optional.empty();
    }

    /**
     * Get the current authenticated user's ID.
     */
    public Optional<UUID> getCurrentUserId() {
        return getCurrentUser().map(UserPrincipal::getUserId);
    }

    /**
     * Get the current authenticated user's profile ID.
     */
    public Optional<UUID> getCurrentProfileId() {
        return getCurrentUser().map(UserPrincipal::getProfileId);
    }

    /**
     * Get the current user ID or throw an exception if not authenticated.
     */
    public UUID requireCurrentUserId() {
        return getCurrentUserId()
                .orElseThrow(() -> new SecurityException("User not authenticated"));
    }

    /**
     * Get the current profile ID or throw an exception if not available.
     */
    public UUID requireCurrentProfileId() {
        return getCurrentProfileId()
                .orElseThrow(() -> new SecurityException("Profile not available"));
    }

    /**
     * Get the current JWT token for service-to-service calls.
     */
    public String getCurrentToken() {
        return getCurrentUser().map(UserPrincipal::getToken).orElse(null);
    }
}

