package com.ametsa.smartbachat.uam.service;

import com.ametsa.smartbachat.uam.config.EmailConfig;
import com.ametsa.smartbachat.uam.entity.User;
import com.ametsa.smartbachat.uam.entity.VerificationToken;
import com.ametsa.smartbachat.uam.repository.UserRepository;
import com.ametsa.smartbachat.uam.repository.VerificationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service for password reset functionality.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final VerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final EmailConfig emailConfig;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(
            VerificationTokenRepository tokenRepository,
            UserRepository userRepository,
            EmailService emailService,
            EmailConfig emailConfig,
            PasswordEncoder passwordEncoder) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.emailConfig = emailConfig;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Initiate password reset by sending email with reset token.
     */
    @Transactional
    public void initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        
        // Don't reveal if user exists or not for security
        if (user == null) {
            log.warn("Password reset requested for non-existent email: {}", email);
            return;
        }

        // Delete any existing password reset tokens for this user
        tokenRepository.deleteByUserIdAndTokenType(user.getId(), VerificationToken.TYPE_PASSWORD_RESET);

        // Create new token
        VerificationToken token = new VerificationToken(
                user,
                VerificationToken.TYPE_PASSWORD_RESET,
                emailConfig.getPasswordResetTokenExpirationMinutes()
        );
        tokenRepository.save(token);

        // Send email
        emailService.sendPasswordResetEmail(user, token.getToken());
        log.info("Password reset email sent to user: {}", user.getId());
    }

    /**
     * Validate password reset token.
     */
    public boolean validateResetToken(String token) {
        return tokenRepository.findByTokenAndTokenType(token, VerificationToken.TYPE_PASSWORD_RESET)
                .map(VerificationToken::isValid)
                .orElse(false);
    }

    /**
     * Reset password using token.
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        VerificationToken resetToken = tokenRepository.findByTokenAndTokenType(
                token, VerificationToken.TYPE_PASSWORD_RESET)
                .orElseThrow(() -> new RuntimeException("Invalid reset token"));

        if (!resetToken.isValid()) {
            throw new RuntimeException("Reset token has expired or already been used");
        }

        // Validate password strength
        validatePasswordStrength(newPassword);

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        resetToken.markAsUsed();
        tokenRepository.save(resetToken);

        // Send notification
        emailService.sendPasswordChangedNotification(user);

        log.info("Password reset completed for user: {}", user.getId());
    }

    /**
     * Change password for authenticated user.
     */
    @Transactional
    public void changePassword(User user, String currentPassword, String newPassword) {
        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Validate new password
        validatePasswordStrength(newPassword);

        // Check that new password is different
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new RuntimeException("New password must be different from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        // Send notification
        emailService.sendPasswordChangedNotification(user);

        log.info("Password changed for user: {}", user.getId());
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters long");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new RuntimeException("Password must contain at least one uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new RuntimeException("Password must contain at least one lowercase letter");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new RuntimeException("Password must contain at least one digit");
        }
    }
}

