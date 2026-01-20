package com.ametsa.smartbachat.uam.controller;

import com.ametsa.smartbachat.uam.BaseIntegrationTest;
import com.ametsa.smartbachat.uam.dto.LoginRequest;
import com.ametsa.smartbachat.uam.dto.RegisterRequest;
import com.ametsa.smartbachat.uam.entity.User;
import com.ametsa.smartbachat.uam.entity.VerificationToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class Register {

        @Test
        @DisplayName("Should register new user successfully")
        void shouldRegisterNewUser() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("newuser@example.com");
            request.setPassword("Password123!");
            request.setFirstName("John");
            request.setLastName("Doe");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.userId").isNotEmpty())
                    .andExpect(jsonPath("$.email").value("newuser@example.com"))
                    .andExpect(jsonPath("$.displayName").value("John Doe"))
                    .andExpect(jsonPath("$.roles", hasItem("ROLE_USER")));
        }

        @Test
        @DisplayName("Should return error for duplicate email")
        void shouldReturnErrorForDuplicateEmail() throws Exception {
            // First registration
            RegisterRequest request = new RegisterRequest();
            request.setEmail("duplicate@example.com");
            request.setPassword("Password123!");
            request.setFirstName("First");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isOk());

            // Second registration with same email
            request.setFirstName("Second");
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Email already registered"));
        }

        @Test
        @DisplayName("Should return error for invalid email format")
        void shouldReturnErrorForInvalidEmail() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("invalid-email");
            request.setPassword("Password123!");
            request.setFirstName("John");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void shouldLoginSuccessfully() throws Exception {
            // Create user first
            createTestUser("login@example.com", "Password123!");

            LoginRequest request = new LoginRequest();
            request.setUsername("login@example.com");
            request.setPassword("Password123!");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.email").value("login@example.com"));
        }

        @Test
        @DisplayName("Should return error for invalid password")
        void shouldReturnErrorForInvalidPassword() throws Exception {
            createTestUser("wrongpass@example.com", "Password123!");

            LoginRequest request = new LoginRequest();
            request.setUsername("wrongpass@example.com");
            request.setPassword("WrongPassword!");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid credentials"));
        }

        @Test
        @DisplayName("Should return error for non-existent user")
        void shouldReturnErrorForNonExistentUser() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setUsername("nonexistent@example.com");
            request.setPassword("Password123!");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid credentials"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class RefreshToken {

        @Test
        @DisplayName("Should refresh token successfully")
        void shouldRefreshTokenSuccessfully() throws Exception {
            // Register and get tokens
            RegisterRequest registerRequest = new RegisterRequest();
            registerRequest.setEmail("refresh@example.com");
            registerRequest.setPassword("Password123!");
            registerRequest.setFirstName("Refresh");

            String response = mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(registerRequest)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            String refreshToken = objectMapper.readTree(response).get("refreshToken").asText();

            // Refresh token
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty());
        }
    }

    private void createTestUser(String email, String password) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setStatus("ACTIVE");
        user.setEmailVerified(true);
        userRepository.save(user);
    }
}

