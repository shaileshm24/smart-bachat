package com.ametsa.smartbachat.uam.controller;

import com.ametsa.smartbachat.uam.dto.AuthResponse;
import com.ametsa.smartbachat.uam.dto.LoginRequest;
import com.ametsa.smartbachat.uam.dto.PasswordResetRequest;
import com.ametsa.smartbachat.uam.dto.RegisterRequest;
import com.ametsa.smartbachat.uam.service.AuthenticationService;
import com.ametsa.smartbachat.uam.service.PasswordResetService;
import com.ametsa.smartbachat.uam.service.VerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationService authService;
    private final VerificationService verificationService;
    private final PasswordResetService passwordResetService;

    public AuthController(
            AuthenticationService authService,
            VerificationService verificationService,
            PasswordResetService passwordResetService) {
        this.authService = authService;
        this.verificationService = verificationService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            String ipAddress = getClientIp(httpRequest);
            AuthResponse response = authService.login(request, ipAddress);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "FAILED",
                        "message", "Refresh token is required"
                ));
            }
            AuthResponse response = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // For stateless JWT, logout is handled client-side by removing the token
        // In production, you might want to blacklist the token
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Logged out successfully"
        ));
    }

    // Email Verification Endpoints

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            if (token == null || token.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "FAILED",
                        "message", "Token is required"
                ));
            }
            verificationService.verifyEmail(token);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Email verified successfully"
            ));
        } catch (Exception e) {
            log.error("Email verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "FAILED",
                        "message", "Email is required"
                ));
            }
            verificationService.resendVerificationEmail(email);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Verification email sent"
            ));
        } catch (Exception e) {
            log.error("Resend verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    // Password Reset Endpoints

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "FAILED",
                        "message", "Email is required"
                ));
            }
            passwordResetService.initiatePasswordReset(email);
            // Always return success to not reveal if email exists
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "If the email exists, a password reset link has been sent"
            ));
        } catch (Exception e) {
            log.error("Forgot password failed: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "If the email exists, a password reset link has been sent"
            ));
        }
    }

    @GetMapping("/validate-reset-token")
    public ResponseEntity<?> validateResetToken(@RequestParam String token) {
        boolean valid = passwordResetService.validateResetToken(token);
        return ResponseEntity.ok(Map.of(
                "valid", valid
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        try {
            passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Password reset successfully"
            ));
        } catch (Exception e) {
            log.error("Password reset failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

