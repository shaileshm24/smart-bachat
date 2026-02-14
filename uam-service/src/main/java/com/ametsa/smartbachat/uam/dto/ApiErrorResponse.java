package com.ametsa.smartbachat.uam.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Standardized API error response for consistent error handling across all endpoints.
 * Frontend can rely on this structure for all error responses.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private String code;
    private int status;
    private String message;
    private String detail;
    private String path;
    private Instant timestamp;
    private List<FieldError> errors;

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

    public ApiErrorResponse() {}

    public ApiErrorResponse(String code, int status, String message, String detail, String path, Instant timestamp) {
        this.code = code;
        this.status = status;
        this.message = message;
        this.detail = detail;
        this.path = path;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public List<FieldError> getErrors() { return errors; }
    public void setErrors(List<FieldError> errors) { this.errors = errors; }

    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;

        public FieldError() {}
        public FieldError(String field, String message, Object rejectedValue) {
            this.field = field;
            this.message = message;
            this.rejectedValue = rejectedValue;
        }

        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Object getRejectedValue() { return rejectedValue; }
        public void setRejectedValue(Object rejectedValue) { this.rejectedValue = rejectedValue; }
    }

    // Factory methods
    public static ApiErrorResponse tokenExpired(String path) {
        return new ApiErrorResponse(CODE_TOKEN_EXPIRED, 401, 
            "Your session has expired. Please log in again.", null, path, Instant.now());
    }

    public static ApiErrorResponse tokenInvalid(String path, String detail) {
        return new ApiErrorResponse(CODE_TOKEN_INVALID, 401, 
            "Invalid authentication token.", detail, path, Instant.now());
    }

    public static ApiErrorResponse unauthorized(String path) {
        return new ApiErrorResponse(CODE_UNAUTHORIZED, 401, 
            "Authentication required. Please log in.", null, path, Instant.now());
    }

    public static ApiErrorResponse forbidden(String path) {
        return new ApiErrorResponse(CODE_FORBIDDEN, 403, 
            "You don't have permission to access this resource.", null, path, Instant.now());
    }

    public static ApiErrorResponse badRequest(String path, String message) {
        return new ApiErrorResponse(CODE_BAD_REQUEST, 400, message, null, path, Instant.now());
    }

    public static ApiErrorResponse internalError(String path) {
        return new ApiErrorResponse(CODE_INTERNAL_ERROR, 500, 
            "An unexpected error occurred. Please try again later.", null, path, Instant.now());
    }
}

