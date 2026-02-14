package com.ametsa.smartbachat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Standardized API error response for consistent error handling across all endpoints.
 * Frontend can rely on this structure for all error responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    /**
     * Error code for programmatic handling (e.g., "TOKEN_EXPIRED", "VALIDATION_ERROR")
     */
    private String code;

    /**
     * HTTP status code
     */
    private int status;

    /**
     * Human-readable error message
     */
    private String message;

    /**
     * Detailed error description (optional)
     */
    private String detail;

    /**
     * Request path that caused the error
     */
    private String path;

    /**
     * Timestamp when the error occurred
     */
    private Instant timestamp;

    /**
     * List of field-level validation errors (for validation failures)
     */
    private List<FieldError> errors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
    }

    // Common error codes
    public static final String CODE_TOKEN_EXPIRED = "TOKEN_EXPIRED";
    public static final String CODE_TOKEN_INVALID = "TOKEN_INVALID";
    public static final String CODE_TOKEN_MISSING = "TOKEN_MISSING";
    public static final String CODE_UNAUTHORIZED = "UNAUTHORIZED";
    public static final String CODE_FORBIDDEN = "FORBIDDEN";
    public static final String CODE_NOT_FOUND = "NOT_FOUND";
    public static final String CODE_VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String CODE_BAD_REQUEST = "BAD_REQUEST";
    public static final String CODE_INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String CODE_SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";

    // Factory methods for common errors
    public static ApiErrorResponse tokenExpired(String path) {
        return ApiErrorResponse.builder()
                .code(CODE_TOKEN_EXPIRED)
                .status(401)
                .message("Your session has expired. Please log in again.")
                .path(path)
                .timestamp(Instant.now())
                .build();
    }

    public static ApiErrorResponse tokenInvalid(String path, String detail) {
        return ApiErrorResponse.builder()
                .code(CODE_TOKEN_INVALID)
                .status(401)
                .message("Invalid authentication token.")
                .detail(detail)
                .path(path)
                .timestamp(Instant.now())
                .build();
    }

    public static ApiErrorResponse unauthorized(String path) {
        return ApiErrorResponse.builder()
                .code(CODE_UNAUTHORIZED)
                .status(401)
                .message("Authentication required. Please log in.")
                .path(path)
                .timestamp(Instant.now())
                .build();
    }

    public static ApiErrorResponse forbidden(String path) {
        return ApiErrorResponse.builder()
                .code(CODE_FORBIDDEN)
                .status(403)
                .message("You don't have permission to access this resource.")
                .path(path)
                .timestamp(Instant.now())
                .build();
    }

    public static ApiErrorResponse notFound(String path, String message) {
        return ApiErrorResponse.builder()
                .code(CODE_NOT_FOUND)
                .status(404)
                .message(message)
                .path(path)
                .timestamp(Instant.now())
                .build();
    }

    public static ApiErrorResponse badRequest(String path, String message) {
        return ApiErrorResponse.builder()
                .code(CODE_BAD_REQUEST)
                .status(400)
                .message(message)
                .path(path)
                .timestamp(Instant.now())
                .build();
    }

    public static ApiErrorResponse internalError(String path) {
        return ApiErrorResponse.builder()
                .code(CODE_INTERNAL_ERROR)
                .status(500)
                .message("An unexpected error occurred. Please try again later.")
                .path(path)
                .timestamp(Instant.now())
                .build();
    }
}

