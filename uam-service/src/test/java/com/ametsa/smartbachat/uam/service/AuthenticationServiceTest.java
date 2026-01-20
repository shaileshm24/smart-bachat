package com.ametsa.smartbachat.uam.service;

import com.ametsa.smartbachat.uam.TestUtils;
import com.ametsa.smartbachat.uam.dto.AuthResponse;
import com.ametsa.smartbachat.uam.dto.LoginRequest;
import com.ametsa.smartbachat.uam.dto.RegisterRequest;
import com.ametsa.smartbachat.uam.entity.Profile;
import com.ametsa.smartbachat.uam.entity.Role;
import com.ametsa.smartbachat.uam.entity.User;
import com.ametsa.smartbachat.uam.repository.ProfileRepository;
import com.ametsa.smartbachat.uam.repository.RoleRepository;
import com.ametsa.smartbachat.uam.repository.UserRepository;
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

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService Unit Tests")
class AuthenticationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private ProfileRepository profileRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private VerificationService verificationService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private Role userRole;

    @BeforeEach
    void setUp() {
        userRole = TestUtils.createUserRole();
    }

    @Nested
    @DisplayName("User Registration")
    class UserRegistration {

        @Test
        @DisplayName("Should register new user successfully")
        void shouldRegisterNewUserSuccessfully() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("new@example.com");
            request.setPassword("Password123!");
            request.setFirstName("John");
            request.setLastName("Doe");

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(roleRepository.findByName(Role.ROLE_USER)).thenReturn(Optional.of(userRole));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
            when(profileRepository.save(any(Profile.class))).thenAnswer(i -> i.getArgument(0));
            when(jwtService.generateAccessToken(any(User.class))).thenReturn("accessToken");
            when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refreshToken");
            when(jwtService.getExpirationMs()).thenReturn(3600000L);

            AuthResponse response = authenticationService.register(request);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("accessToken");
            assertThat(response.getRefreshToken()).isEqualTo("refreshToken");
            verify(userRepository).save(any(User.class));
            verify(profileRepository).save(any(Profile.class));
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("existing@example.com");
            request.setPassword("Password123!");

            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authenticationService.register(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Email already registered");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when mobile already exists")
        void shouldThrowExceptionWhenMobileExists() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("new@example.com");
            request.setPassword("Password123!");
            request.setMobileNumber("+919876543210");

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByMobileNumber("+919876543210")).thenReturn(true);

            assertThatThrownBy(() -> authenticationService.register(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Mobile number already registered");
        }

        @Test
        @DisplayName("Should assign default USER role")
        void shouldAssignDefaultUserRole() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("new@example.com");
            request.setPassword("Password123!");
            request.setFirstName("John");

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(roleRepository.findByName(Role.ROLE_USER)).thenReturn(Optional.of(userRole));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
            when(profileRepository.save(any(Profile.class))).thenAnswer(i -> i.getArgument(0));
            when(jwtService.generateAccessToken(any())).thenReturn("token");
            when(jwtService.generateRefreshToken(any())).thenReturn("refresh");
            when(jwtService.getExpirationMs()).thenReturn(3600000L);

            authenticationService.register(request);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository, atLeastOnce()).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getRoles()).contains(userRole);
        }
    }

    @Nested
    @DisplayName("User Login")
    class UserLogin {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void shouldLoginSuccessfully() {
            User user = TestUtils.createTestUserWithProfile("login@example.com");
            user.addRole(userRole);
            LoginRequest request = new LoginRequest();
            request.setUsername("login@example.com");
            request.setPassword("correctPassword");

            when(userRepository.findByEmailOrMobileNumber(anyString(), anyString()))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.matches("correctPassword", user.getPasswordHash()))
                    .thenReturn(true);
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
            when(jwtService.generateAccessToken(any())).thenReturn("accessToken");
            when(jwtService.generateRefreshToken(any())).thenReturn("refreshToken");
            when(jwtService.getExpirationMs()).thenReturn(3600000L);

            AuthResponse response = authenticationService.login(request, "127.0.0.1");

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("accessToken");
        }

        @Test
        @DisplayName("Should throw exception for invalid credentials")
        void shouldThrowExceptionForInvalidCredentials() {
            User user = TestUtils.createTestUser();
            LoginRequest request = new LoginRequest();
            request.setUsername("test@example.com");
            request.setPassword("wrongPassword");

            when(userRepository.findByEmailOrMobileNumber(anyString(), anyString()))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrongPassword", user.getPasswordHash()))
                    .thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            assertThatThrownBy(() -> authenticationService.login(request, "127.0.0.1"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Invalid credentials");
        }

        @Test
        @DisplayName("Should throw exception for non-existent user")
        void shouldThrowExceptionForNonExistentUser() {
            LoginRequest request = new LoginRequest();
            request.setUsername("nonexistent@example.com");
            request.setPassword("password");

            when(userRepository.findByEmailOrMobileNumber(anyString(), anyString()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authenticationService.login(request, "127.0.0.1"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Invalid credentials");
        }

        @Test
        @DisplayName("Should throw exception for locked account")
        void shouldThrowExceptionForLockedAccount() {
            User user = TestUtils.createLockedUser();
            LoginRequest request = new LoginRequest();
            request.setUsername("locked@example.com");
            request.setPassword("password");

            when(userRepository.findByEmailOrMobileNumber(anyString(), anyString()))
                    .thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authenticationService.login(request, "127.0.0.1"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Account is locked. Try again later.");
        }

        @Test
        @DisplayName("Should throw exception for inactive account")
        void shouldThrowExceptionForInactiveAccount() {
            User user = TestUtils.createInactiveUser();
            LoginRequest request = new LoginRequest();
            request.setUsername("inactive@example.com");
            request.setPassword("password");

            when(userRepository.findByEmailOrMobileNumber(anyString(), anyString()))
                    .thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authenticationService.login(request, "127.0.0.1"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Account is not active");
        }

        @Test
        @DisplayName("Should update last login info on successful login")
        void shouldUpdateLastLoginInfo() {
            User user = TestUtils.createTestUserWithProfile("login@example.com");
            user.addRole(userRole);
            LoginRequest request = new LoginRequest();
            request.setUsername("login@example.com");
            request.setPassword("correctPassword");

            when(userRepository.findByEmailOrMobileNumber(anyString(), anyString()))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
            when(jwtService.generateAccessToken(any())).thenReturn("token");
            when(jwtService.generateRefreshToken(any())).thenReturn("refresh");
            when(jwtService.getExpirationMs()).thenReturn(3600000L);

            authenticationService.login(request, "192.168.1.1");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getLastLoginIp()).isEqualTo("192.168.1.1");
            assertThat(savedUser.getLastLoginAt()).isNotNull();
            assertThat(savedUser.getFailedLoginAttempts()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Token Refresh")
    class TokenRefresh {

        @Test
        @DisplayName("Should refresh token successfully")
        void shouldRefreshTokenSuccessfully() {
            User user = TestUtils.createTestUserWithProfile("refresh@example.com");
            user.addRole(userRole);

            when(jwtService.validateToken("validRefreshToken")).thenReturn(true);
            when(jwtService.isRefreshToken("validRefreshToken")).thenReturn(true);
            when(jwtService.getUserIdFromToken("validRefreshToken")).thenReturn(user.getId());
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(jwtService.generateAccessToken(any())).thenReturn("newAccessToken");
            when(jwtService.generateRefreshToken(any())).thenReturn("newRefreshToken");
            when(jwtService.getExpirationMs()).thenReturn(3600000L);

            AuthResponse response = authenticationService.refreshToken("validRefreshToken");

            assertThat(response.getAccessToken()).isEqualTo("newAccessToken");
            assertThat(response.getRefreshToken()).isEqualTo("newRefreshToken");
        }

        @Test
        @DisplayName("Should throw exception for invalid refresh token")
        void shouldThrowExceptionForInvalidRefreshToken() {
            when(jwtService.validateToken("invalidToken")).thenReturn(false);

            assertThatThrownBy(() -> authenticationService.refreshToken("invalidToken"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Invalid refresh token");
        }

        @Test
        @DisplayName("Should throw exception when using access token as refresh")
        void shouldThrowExceptionWhenUsingAccessTokenAsRefresh() {
            when(jwtService.validateToken("accessToken")).thenReturn(true);
            when(jwtService.isRefreshToken("accessToken")).thenReturn(false);

            assertThatThrownBy(() -> authenticationService.refreshToken("accessToken"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Invalid refresh token");
        }
    }
}

