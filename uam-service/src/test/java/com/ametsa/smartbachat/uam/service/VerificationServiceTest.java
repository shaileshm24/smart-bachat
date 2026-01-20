package com.ametsa.smartbachat.uam.service;

import com.ametsa.smartbachat.uam.TestUtils;
import com.ametsa.smartbachat.uam.config.EmailConfig;
import com.ametsa.smartbachat.uam.entity.User;
import com.ametsa.smartbachat.uam.entity.VerificationToken;
import com.ametsa.smartbachat.uam.repository.UserRepository;
import com.ametsa.smartbachat.uam.repository.VerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VerificationService Unit Tests")
class VerificationServiceTest {

    @Mock private VerificationTokenRepository tokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    @Mock private EmailConfig emailConfig;

    @InjectMocks
    private VerificationService verificationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = TestUtils.createPendingVerificationUser();
    }

    @Nested
    @DisplayName("Send Verification Email")
    class SendVerificationEmail {

        @Test
        @DisplayName("Should send verification email")
        void shouldSendVerificationEmail() {
            when(emailConfig.getVerificationTokenExpirationMinutes()).thenReturn(1440L);
            when(tokenRepository.save(any(VerificationToken.class))).thenAnswer(i -> i.getArgument(0));

            verificationService.sendVerificationEmail(testUser);

            verify(tokenRepository).deleteByUserIdAndTokenType(
                    testUser.getId(), VerificationToken.TYPE_EMAIL_VERIFICATION);
            verify(tokenRepository).save(any(VerificationToken.class));
            verify(emailService).sendVerificationEmail(eq(testUser), anyString());
        }

        @Test
        @DisplayName("Should delete existing tokens before creating new one")
        void shouldDeleteExistingTokens() {
            when(emailConfig.getVerificationTokenExpirationMinutes()).thenReturn(1440L);
            when(tokenRepository.save(any(VerificationToken.class))).thenAnswer(i -> i.getArgument(0));

            verificationService.sendVerificationEmail(testUser);

            verify(tokenRepository).deleteByUserIdAndTokenType(
                    testUser.getId(), VerificationToken.TYPE_EMAIL_VERIFICATION);
        }
    }

    @Nested
    @DisplayName("Verify Email")
    class VerifyEmail {

        @Test
        @DisplayName("Should verify email successfully")
        void shouldVerifyEmailSuccessfully() {
            VerificationToken token = new VerificationToken(
                    testUser, VerificationToken.TYPE_EMAIL_VERIFICATION, 1440);
            when(tokenRepository.findByTokenAndTokenType(anyString(), eq(VerificationToken.TYPE_EMAIL_VERIFICATION)))
                    .thenReturn(Optional.of(token));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
            when(tokenRepository.save(any(VerificationToken.class))).thenAnswer(i -> i.getArgument(0));

            verificationService.verifyEmail("valid-token");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getEmailVerified()).isTrue();
            assertThat(savedUser.getStatus()).isEqualTo("ACTIVE");
            verify(emailService).sendWelcomeEmail(testUser);
        }

        @Test
        @DisplayName("Should throw exception for invalid token")
        void shouldThrowExceptionForInvalidToken() {
            when(tokenRepository.findByTokenAndTokenType(anyString(), eq(VerificationToken.TYPE_EMAIL_VERIFICATION)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> verificationService.verifyEmail("invalid-token"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Invalid verification token");
        }

        @Test
        @DisplayName("Should throw exception for expired token")
        void shouldThrowExceptionForExpiredToken() {
            VerificationToken token = new VerificationToken(
                    testUser, VerificationToken.TYPE_EMAIL_VERIFICATION, -1);
            when(tokenRepository.findByTokenAndTokenType(anyString(), eq(VerificationToken.TYPE_EMAIL_VERIFICATION)))
                    .thenReturn(Optional.of(token));

            assertThatThrownBy(() -> verificationService.verifyEmail("expired-token"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Verification token has expired or already been used");
        }

        @Test
        @DisplayName("Should throw exception for already used token")
        void shouldThrowExceptionForUsedToken() {
            VerificationToken token = new VerificationToken(
                    testUser, VerificationToken.TYPE_EMAIL_VERIFICATION, 1440);
            token.markAsUsed();
            when(tokenRepository.findByTokenAndTokenType(anyString(), eq(VerificationToken.TYPE_EMAIL_VERIFICATION)))
                    .thenReturn(Optional.of(token));

            assertThatThrownBy(() -> verificationService.verifyEmail("used-token"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Verification token has expired or already been used");
        }
    }

    @Nested
    @DisplayName("Resend Verification Email")
    class ResendVerificationEmail {

        @Test
        @DisplayName("Should resend verification email")
        void shouldResendVerificationEmail() {
            when(userRepository.findByEmail("pending@example.com")).thenReturn(Optional.of(testUser));
            when(emailConfig.getVerificationTokenExpirationMinutes()).thenReturn(1440L);
            when(tokenRepository.save(any(VerificationToken.class))).thenAnswer(i -> i.getArgument(0));

            verificationService.resendVerificationEmail("pending@example.com");

            verify(emailService).sendVerificationEmail(eq(testUser), anyString());
        }

        @Test
        @DisplayName("Should throw exception for non-existent user")
        void shouldThrowExceptionForNonExistentUser() {
            when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> verificationService.resendVerificationEmail("nonexistent@example.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not found");
        }

        @Test
        @DisplayName("Should throw exception if email already verified")
        void shouldThrowExceptionIfAlreadyVerified() {
            User verifiedUser = TestUtils.createTestUser();
            verifiedUser.setEmailVerified(true);
            when(userRepository.findByEmail("verified@example.com")).thenReturn(Optional.of(verifiedUser));

            assertThatThrownBy(() -> verificationService.resendVerificationEmail("verified@example.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Email is already verified");
        }
    }

    @Nested
    @DisplayName("Has Valid Verification Token")
    class HasValidVerificationToken {

        @Test
        @DisplayName("Should return true when valid token exists")
        void shouldReturnTrueWhenValidTokenExists() {
            VerificationToken token = new VerificationToken(
                    testUser, VerificationToken.TYPE_EMAIL_VERIFICATION, 1440);
            when(tokenRepository.findValidTokenByUserAndType(
                    eq(testUser.getId()), eq(VerificationToken.TYPE_EMAIL_VERIFICATION), any(Instant.class)))
                    .thenReturn(Optional.of(token));

            boolean result = verificationService.hasValidVerificationToken(testUser.getId());

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when no valid token exists")
        void shouldReturnFalseWhenNoValidTokenExists() {
            when(tokenRepository.findValidTokenByUserAndType(
                    eq(testUser.getId()), eq(VerificationToken.TYPE_EMAIL_VERIFICATION), any(Instant.class)))
                    .thenReturn(Optional.empty());

            boolean result = verificationService.hasValidVerificationToken(testUser.getId());

            assertThat(result).isFalse();
        }
    }
}

