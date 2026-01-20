package com.ametsa.smartbachat.uam.service;

import com.ametsa.smartbachat.uam.config.EmailConfig;
import com.ametsa.smartbachat.uam.entity.User;
import com.ametsa.smartbachat.uam.entity.VerificationToken;
import com.ametsa.smartbachat.uam.repository.UserRepository;
import com.ametsa.smartbachat.uam.repository.VerificationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for email verification.
 */
@Service
public class VerificationService {

    private static final Logger log = LoggerFactory.getLogger(VerificationService.class);

    private final VerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final EmailConfig emailConfig;

    public VerificationService(
            VerificationTokenRepository tokenRepository,
            UserRepository userRepository,
            EmailService emailService,
            EmailConfig emailConfig) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.emailConfig = emailConfig;
    }

    /**
     * Create and send email verification token.
     */
    @Transactional
    public void sendVerificationEmail(User user) {
        // Delete any existing verification tokens for this user
        tokenRepository.deleteByUserIdAndTokenType(user.getId(), VerificationToken.TYPE_EMAIL_VERIFICATION);

        // Create new token
        VerificationToken token = new VerificationToken(
                user,
                VerificationToken.TYPE_EMAIL_VERIFICATION,
                emailConfig.getVerificationTokenExpirationMinutes()
        );
        tokenRepository.save(token);

        // Send email
        emailService.sendVerificationEmail(user, token.getToken());
        log.info("Verification email sent to user: {}", user.getId());
    }

    /**
     * Verify email with token.
     */
    @Transactional
    public void verifyEmail(String token) {
        VerificationToken verificationToken = tokenRepository.findByTokenAndTokenType(
                token, VerificationToken.TYPE_EMAIL_VERIFICATION)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));

        if (!verificationToken.isValid()) {
            throw new RuntimeException("Verification token has expired or already been used");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        user.setStatus("ACTIVE");
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        verificationToken.markAsUsed();
        tokenRepository.save(verificationToken);

        // Send welcome email
        emailService.sendWelcomeEmail(user);

        log.info("Email verified for user: {}", user.getId());
    }

    /**
     * Resend verification email.
     */
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getEmailVerified()) {
            throw new RuntimeException("Email is already verified");
        }

        sendVerificationEmail(user);
    }

    /**
     * Check if user has a valid verification token.
     */
    public boolean hasValidVerificationToken(UUID userId) {
        return tokenRepository.findValidTokenByUserAndType(
                userId, VerificationToken.TYPE_EMAIL_VERIFICATION, Instant.now())
                .isPresent();
    }
}

