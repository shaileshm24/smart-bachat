package com.ametsa.smartbachat.uam;

import com.ametsa.smartbachat.uam.entity.Profile;
import com.ametsa.smartbachat.uam.entity.Role;
import com.ametsa.smartbachat.uam.entity.User;

import java.time.Instant;
import java.util.UUID;

/**
 * Utility class for creating test data.
 */
public class TestUtils {

    public static User createTestUser() {
        return createTestUser("test@example.com", "Test", "User");
    }

    public static User createTestUser(String email, String firstName, String lastName) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setDisplayName(firstName);
        user.setPasswordHash("$2a$10$hashedpassword");
        user.setStatus("ACTIVE");
        user.setEmailVerified(true);
        user.setMobileVerified(false);
        user.setFailedLoginAttempts(0);
        user.setCreatedAt(Instant.now());
        return user;
    }

    public static User createTestUserWithRole(String email, Role role) {
        User user = createTestUser(email, "Test", "User");
        user.addRole(role);
        return user;
    }

    public static User createTestUserWithProfile(String email) {
        User user = createTestUser(email, "Test", "User");
        Profile profile = createTestProfile(user);
        user.setProfile(profile);
        return user;
    }

    public static Role createUserRole() {
        Role role = new Role(Role.ROLE_USER, "User", "Default user role");
        role.setId(UUID.randomUUID());
        role.setIsSystemRole(true);
        return role;
    }

    public static Role createAdminRole() {
        Role role = new Role(Role.ROLE_ADMIN, "Admin", "Administrator role");
        role.setId(UUID.randomUUID());
        role.setIsSystemRole(true);
        return role;
    }

    public static Profile createTestProfile(User user) {
        Profile profile = new Profile();
        profile.setId(UUID.randomUUID());
        profile.setUser(user);
        profile.setCreatedAt(Instant.now());
        return profile;
    }

    public static User createLockedUser() {
        User user = createTestUser("locked@example.com", "Locked", "User");
        user.setLockedUntil(Instant.now().plusSeconds(3600)); // Locked for 1 hour
        user.setFailedLoginAttempts(5);
        return user;
    }

    public static User createInactiveUser() {
        User user = createTestUser("inactive@example.com", "Inactive", "User");
        user.setStatus("INACTIVE");
        return user;
    }

    public static User createPendingVerificationUser() {
        User user = createTestUser("pending@example.com", "Pending", "User");
        user.setStatus("PENDING_VERIFICATION");
        user.setEmailVerified(false);
        return user;
    }
}

