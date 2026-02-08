package com.ametsa.smartbachat.security;

import java.util.List;
import java.util.UUID;

/**
 * User principal containing authenticated user information.
 */
public class UserPrincipal {

    private final UUID userId;
    private final UUID profileId;
    private final String email;
    private final List<String> roles;
    private final String token;

    public UserPrincipal(UUID userId, UUID profileId, String email, List<String> roles) {
        this(userId, profileId, email, roles, null);
    }

    public UserPrincipal(UUID userId, UUID profileId, String email, List<String> roles, String token) {
        this.userId = userId;
        this.profileId = profileId;
        this.email = email;
        this.roles = roles;
        this.token = token;
    }

    public UUID getUserId() { return userId; }
    public UUID getProfileId() { return profileId; }
    public String getEmail() { return email; }
    public List<String> getRoles() { return roles; }
    public String getToken() { return token; }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    @Override
    public String toString() {
        return "UserPrincipal{userId=" + userId + ", profileId=" + profileId + ", email='" + email + "'}";
    }
}

