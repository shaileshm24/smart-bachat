package com.ametsa.smartbachat.uam.service;

import com.ametsa.smartbachat.uam.dto.AuthResponse;
import com.ametsa.smartbachat.uam.dto.LoginRequest;
import com.ametsa.smartbachat.uam.dto.RegisterRequest;
import com.ametsa.smartbachat.uam.entity.Profile;
import com.ametsa.smartbachat.uam.entity.Role;
import com.ametsa.smartbachat.uam.entity.User;
import com.ametsa.smartbachat.uam.repository.ProfileRepository;
import com.ametsa.smartbachat.uam.repository.RoleRepository;
import com.ametsa.smartbachat.uam.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.stream.Collectors;

@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 30;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final VerificationService verificationService;

    public AuthenticationService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            ProfileRepository profileRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            VerificationService verificationService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.verificationService = verificationService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        return register(request, false);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, boolean requireEmailVerification) {
        log.info("Registering new user: {}", request.getEmail());

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // Check if mobile already exists
        if (request.getMobileNumber() != null &&
            userRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new RuntimeException("Mobile number already registered");
        }

        // Create user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setMobileNumber(request.getMobileNumber());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setDisplayName(request.getFirstName());

        if (!requireEmailVerification) {
            user.setStatus("PENDING_VERIFICATION");
            user.setEmailVerified(false);
        } else {
            user.setStatus("ACTIVE");
            user.setEmailVerified(true);
        }

        // Assign default role
        Role userRole = roleRepository.findByName(Role.ROLE_USER)
                .orElseGet(() -> createDefaultRole());
        user.addRole(userRole);

        user = userRepository.save(user);

        // Create profile
        Profile profile = new Profile();
        profile.setUser(user);
        profile = profileRepository.save(profile);
        user.setProfile(profile);

        // Send verification email if required
        if (!requireEmailVerification) {
            verificationService.sendVerificationEmail(user);
            log.info("User registered, verification email sent: {}", user.getId());
        } else {
            log.info("User registered successfully: {} with profile: {}", user.getId(), profile.getId());
        }

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress) {
        log.info("Login attempt for: {}", request.getUsername());

        // Find user by email or mobile
        User user = userRepository.findByEmailOrMobileNumber(
                request.getUsername(), request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        // Check if account is locked
        if (user.isLocked()) {
            throw new RuntimeException("Account is locked. Try again later.");
        }

        // Check if account is active
        if (!user.isActive()) {
            throw new RuntimeException("Account is not active");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new RuntimeException("Invalid credentials");
        }

        // Reset failed attempts and update login info
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        user.setLastLoginIp(ipAddress);
        userRepository.save(user);

        log.info("User logged in successfully: {}", user.getId());

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtService.validateToken(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        var userId = jwtService.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isActive()) {
            throw new RuntimeException("Account is not active");
        }

        return generateAuthResponse(user);
    }

    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(Instant.now().plusSeconds(LOCK_DURATION_MINUTES * 60));
            log.warn("Account locked due to too many failed attempts: {}", user.getEmail());
        }

        userRepository.save(user);
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new AuthResponse(
                accessToken, refreshToken, jwtService.getExpirationMs(),
                user.getId(),
                user.getProfile() != null ? user.getProfile().getId() : null,
                user.getEmail(), user.getFullName(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toList())
        );
    }

    private Role createDefaultRole() {
        Role role = new Role(Role.ROLE_USER, "User", "Default user role");
        role.setIsSystemRole(true);
        return roleRepository.save(role);
    }
}

