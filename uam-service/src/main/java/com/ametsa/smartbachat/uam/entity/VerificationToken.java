package com.ametsa.smartbachat.uam.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Token entity for email verification and password reset.
 */
@Entity
@Table(name = "verification_tokens", indexes = {
        @Index(name = "idx_verification_token", columnList = "token", unique = true),
        @Index(name = "idx_verification_user", columnList = "user_id"),
        @Index(name = "idx_verification_type", columnList = "token_type")
})
public class VerificationToken {

    public static final String TYPE_EMAIL_VERIFICATION = "EMAIL_VERIFICATION";
    public static final String TYPE_PASSWORD_RESET = "PASSWORD_RESET";

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "token", unique = true, nullable = false)
    private String token;

    @Column(name = "token_type", nullable = false)
    private String tokenType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public VerificationToken() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
    }

    public VerificationToken(User user, String tokenType, long expirationMinutes) {
        this();
        this.user = user;
        this.tokenType = tokenType;
        this.token = UUID.randomUUID().toString();
        this.expiresAt = Instant.now().plusSeconds(expirationMinutes * 60);
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    // Helper methods
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isValid() {
        return !isExpired() && !isUsed();
    }

    public void markAsUsed() {
        this.usedAt = Instant.now();
    }
}

