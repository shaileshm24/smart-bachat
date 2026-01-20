package com.ametsa.smartbachat.uam.controller;

import com.ametsa.smartbachat.uam.BaseIntegrationTest;
import com.ametsa.smartbachat.uam.dto.LoginRequest;
import com.ametsa.smartbachat.uam.dto.RegisterRequest;
import com.ametsa.smartbachat.uam.dto.UserDto;
import com.ametsa.smartbachat.uam.entity.Role;
import com.ametsa.smartbachat.uam.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("UserController Integration Tests")
class UserControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String accessToken;
    private String adminAccessToken;

    @BeforeEach
    void setUp() throws Exception {
        // Create regular user and get token
        accessToken = registerAndGetToken("user@example.com", "Password123!", "Regular", "User");

        // Create admin user
        registerAndGetToken("admin@example.com", "Password123!", "Admin", "User");

        // Assign admin role
        User adminUser = userRepository.findByEmail("admin@example.com").orElseThrow();
        Role adminRole = roleRepository.findByName(Role.ROLE_ADMIN).orElseThrow();
        adminUser.addRole(adminRole);
        userRepository.save(adminUser);

        // Login again to get token with admin role
        adminAccessToken = loginAndGetToken("admin@example.com", "Password123!");
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername(email);
        request.setPassword(password);

        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private String registerAndGetToken(String email, String password, String firstName, String lastName) throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword(password);
        request.setFirstName(firstName);
        request.setLastName(lastName);

        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("accessToken").asText();
    }

    @Nested
    @DisplayName("GET /api/v1/users/me")
    class GetCurrentUser {

        @Test
        @DisplayName("Should get current user details")
        void shouldGetCurrentUser() throws Exception {
            mockMvc.perform(get("/api/v1/users/me")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("user@example.com"))
                    .andExpect(jsonPath("$.firstName").value("Regular"))
                    .andExpect(jsonPath("$.lastName").value("User"))
                    .andExpect(jsonPath("$.roles", hasItem("ROLE_USER")));
        }

        @Test
        @DisplayName("Should return 401 without token")
        void shouldReturn401WithoutToken() throws Exception {
            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 401 with invalid token")
        void shouldReturn401WithInvalidToken() throws Exception {
            mockMvc.perform(get("/api/v1/users/me")
                            .header("Authorization", "Bearer invalid-token"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/me")
    class UpdateCurrentUser {

        @Test
        @DisplayName("Should update current user")
        void shouldUpdateCurrentUser() throws Exception {
            UserDto updateRequest = new UserDto();
            updateRequest.setFirstName("Updated");
            updateRequest.setLastName("Name");

            mockMvc.perform(put("/api/v1/users/me")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Updated"))
                    .andExpect(jsonPath("$.lastName").value("Name"));
        }

        @Test
        @DisplayName("Should update display name")
        void shouldUpdateDisplayName() throws Exception {
            UserDto updateRequest = new UserDto();
            updateRequest.setDisplayName("My Display Name");

            mockMvc.perform(put("/api/v1/users/me")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.displayName").value("My Display Name"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users (Admin)")
    class GetAllUsers {

        @Test
        @DisplayName("Should get all users as admin")
        void shouldGetAllUsersAsAdmin() throws Exception {
            mockMvc.perform(get("/api/v1/users")
                            .header("Authorization", "Bearer " + adminAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                    .andExpect(jsonPath("$[*].email", hasItems("user@example.com", "admin@example.com")));
        }

        @Test
        @DisplayName("Should return 403 for non-admin user")
        void shouldReturn403ForNonAdmin() throws Exception {
            mockMvc.perform(get("/api/v1/users")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/{id}/status (Admin)")
    class UpdateUserStatus {

        @Test
        @DisplayName("Should update user status as admin")
        void shouldUpdateUserStatusAsAdmin() throws Exception {
            User user = userRepository.findByEmail("user@example.com").orElseThrow();

            mockMvc.perform(put("/api/v1/users/" + user.getId() + "/status")
                            .header("Authorization", "Bearer " + adminAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\": \"SUSPENDED\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUSPENDED"));
        }
    }
}

