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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetService Unit Tests")
class PasswordResetServiceTest {

    @Mock private VerificationTokenRepository tokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    @Mock private EmailConfig emailConfig;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = TestUtils.createTestUser();
    }

    @Nested
    @DisplayName("Initiate Password Reset")
    class InitiatePasswordReset {

        @Test
        @DisplayName("Should send password reset email for existing user")
        void shouldSendPasswordResetEmail() {
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(emailConfig.getPasswordResetTokenExpirationMinutes()).thenReturn(60L);
            when(tokenRepository.save(any(VerificationToken.class))).thenAnswer(i -> i.getArgument(0));

            passwordResetService.initiatePasswordReset("test@example.com");

            verify(tokenRepository).deleteByUserIdAndTokenType(testUser.getId(), VerificationToken.TYPE_PASSWORD_RESET);
            verify(tokenRepository).save(any(VerificationToken.class));
            verify(emailService).sendPasswordResetEmail(eq(testUser), anyString());
        }

        @Test
        @DisplayName("Should not reveal if user does not exist")
        void shouldNotRevealIfUserDoesNotExist() {
            when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

            // Should not throw exception
            passwordResetService.initiatePasswordReset("nonexistent@example.com");

            verify(tokenRepository, never()).save(any());
            verify(emailService, never()).sendPasswordResetEmail(any(), anyString());
        }
    }

    @Nested
    @DisplayName("Validate Reset Token")
    class ValidateResetToken {

        @Test
        @DisplayName("Should return true for valid token")
        void shouldReturnTrueForValidToken() {
            VerificationToken token = new VerificationToken(testUser, VerificationToken.TYPE_PASSWORD_RESET, 60);
            when(tokenRepository.findByTokenAndTokenType(anyString(), eq(VerificationToken.TYPE_PASSWORD_RESET)))
                    .thenReturn(Optional.of(token));

            boolean result = passwordResetService.validateResetToken("valid-token");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false for non-existent token")
        void shouldReturnFalseForNonExistentToken() {
            when(tokenRepository.findByTokenAndTokenType(anyString(), eq(VerificationToken.TYPE_PASSWORD_RESET)))
                    .thenReturn(Optional.empty());

            boolean result = passwordResetService.validateResetToken("invalid-token");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for expired token")
        void shouldReturnFalseForExpiredToken() {
            VerificationToken token = new VerificationToken(testUser, VerificationToken.TYPE_PASSWORD_RESET, -1);
            when(tokenRepository.findByTokenAndTokenType(anyString(), eq(VerificationToken.TYPE_PASSWORD_RESET)))
                    .thenReturn(Optional.of(token));

            boolean result = passwordResetService.validateResetToken("expired-token");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Reset Password")
    class ResetPassword {

        @Test
        @DisplayName("Should reset password successfully")
        void shouldResetPasswordSuccessfully() {
            VerificationToken token = new VerificationToken(testUser, VerificationToken.TYPE_PASSWORD_RESET, 60);
            when(tokenRepository.findByTokenAndTokenType(anyString(), eq(VerificationToken.TYPE_PASSWORD_RESET)))
                    .thenReturn(Optional.of(token));
            when(passwordEncoder.encode("NewPassword123!")).thenReturn("encodedNewPassword");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
            when(tokenRepository.save(any(VerificationToken.class))).thenAnswer(i -> i.getArgument(0));

            passwordResetService.resetPassword("valid-token", "NewPassword123!");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getPasswordHash()).isEqualTo("encodedNewPassword");
            assertThat(savedUser.getFailedLoginAttempts()).isEqualTo(0);
            assertThat(savedUser.getLockedUntil()).isNull();
            verify(emailService).sendPasswordChangedNotification(testUser);
        }

        @Test
        @DisplayName("Should throw exception for invalid token")
        void shouldThrowExceptionForInvalidToken() {
            when(tokenRepository.findByTokenAndTokenType(anyString(), eq(VerificationToken.TYPE_PASSWORD_RESET)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> passwordResetService.resetPassword("invalid", "NewPassword123!"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Invalid reset token");
        }
    }
}

