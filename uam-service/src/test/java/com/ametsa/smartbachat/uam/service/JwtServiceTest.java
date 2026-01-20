package com.ametsa.smartbachat.uam.service;

import com.ametsa.smartbachat.uam.TestUtils;
import com.ametsa.smartbachat.uam.config.JwtConfig;
import com.ametsa.smartbachat.uam.entity.Role;
import com.ametsa.smartbachat.uam.entity.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private JwtConfig jwtConfig;

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig();
        jwtConfig.setSecret("test-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm");
        jwtConfig.setExpirationMs(3600000L); // 1 hour
        jwtConfig.setRefreshExpirationMs(86400000L); // 24 hours
        jwtConfig.setIssuer("smart-bachat-test");
        
        jwtService = new JwtService(jwtConfig);
    }

    @Nested
    @DisplayName("Access Token Generation")
    class AccessTokenGeneration {

        @Test
        @DisplayName("Should generate valid access token for user")
        void shouldGenerateValidAccessToken() {
            User user = TestUtils.createTestUserWithProfile("test@example.com");
            Role role = TestUtils.createUserRole();
            user.addRole(role);

            String token = jwtService.generateAccessToken(user);

            assertThat(token).isNotNull().isNotEmpty();
            assertThat(jwtService.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("Should include user claims in access token")
        void shouldIncludeUserClaimsInAccessToken() {
            User user = TestUtils.createTestUserWithProfile("claims@example.com");
            Role role = TestUtils.createUserRole();
            user.addRole(role);

            String token = jwtService.generateAccessToken(user);
            Claims claims = jwtService.getClaimsFromToken(token);

            assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
            assertThat(claims.get("email")).isEqualTo("claims@example.com");
            assertThat(claims.get("profileId")).isEqualTo(user.getProfile().getId().toString());
            assertThat(claims.get("roles")).isInstanceOf(List.class);
        }

        @Test
        @DisplayName("Should not be a refresh token")
        void accessTokenShouldNotBeRefreshToken() {
            User user = TestUtils.createTestUser();

            String token = jwtService.generateAccessToken(user);

            assertThat(jwtService.isRefreshToken(token)).isFalse();
        }
    }

    @Nested
    @DisplayName("Refresh Token Generation")
    class RefreshTokenGeneration {

        @Test
        @DisplayName("Should generate valid refresh token")
        void shouldGenerateValidRefreshToken() {
            User user = TestUtils.createTestUser();

            String token = jwtService.generateRefreshToken(user);

            assertThat(token).isNotNull().isNotEmpty();
            assertThat(jwtService.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("Should be identified as refresh token")
        void shouldBeIdentifiedAsRefreshToken() {
            User user = TestUtils.createTestUser();

            String token = jwtService.generateRefreshToken(user);

            assertThat(jwtService.isRefreshToken(token)).isTrue();
        }

        @Test
        @DisplayName("Should contain user ID in subject")
        void shouldContainUserIdInSubject() {
            User user = TestUtils.createTestUser();

            String token = jwtService.generateRefreshToken(user);
            UUID userId = jwtService.getUserIdFromToken(token);

            assertThat(userId).isEqualTo(user.getId());
        }
    }

    @Nested
    @DisplayName("Token Validation")
    class TokenValidation {

        @Test
        @DisplayName("Should validate correct token")
        void shouldValidateCorrectToken() {
            User user = TestUtils.createTestUser();
            String token = jwtService.generateAccessToken(user);

            assertThat(jwtService.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("Should reject invalid token")
        void shouldRejectInvalidToken() {
            assertThat(jwtService.validateToken("invalid.token.here")).isFalse();
        }

        @Test
        @DisplayName("Should reject null token")
        void shouldRejectNullToken() {
            assertThat(jwtService.validateToken(null)).isFalse();
        }

        @Test
        @DisplayName("Should reject empty token")
        void shouldRejectEmptyToken() {
            assertThat(jwtService.validateToken("")).isFalse();
        }

        @Test
        @DisplayName("Should reject tampered token")
        void shouldRejectTamperedToken() {
            User user = TestUtils.createTestUser();
            String token = jwtService.generateAccessToken(user);
            String tamperedToken = token.substring(0, token.length() - 5) + "xxxxx";

            assertThat(jwtService.validateToken(tamperedToken)).isFalse();
        }
    }

    @Nested
    @DisplayName("User ID Extraction")
    class UserIdExtraction {

        @Test
        @DisplayName("Should extract user ID from access token")
        void shouldExtractUserIdFromAccessToken() {
            User user = TestUtils.createTestUser();
            String token = jwtService.generateAccessToken(user);

            UUID extractedId = jwtService.getUserIdFromToken(token);

            assertThat(extractedId).isEqualTo(user.getId());
        }

        @Test
        @DisplayName("Should extract user ID from refresh token")
        void shouldExtractUserIdFromRefreshToken() {
            User user = TestUtils.createTestUser();
            String token = jwtService.generateRefreshToken(user);

            UUID extractedId = jwtService.getUserIdFromToken(token);

            assertThat(extractedId).isEqualTo(user.getId());
        }
    }

    @Test
    @DisplayName("Should return correct expiration time")
    void shouldReturnCorrectExpirationTime() {
        assertThat(jwtService.getExpirationMs()).isEqualTo(3600000L);
    }
}

