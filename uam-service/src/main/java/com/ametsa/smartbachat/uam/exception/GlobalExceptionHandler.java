package com.ametsa.smartbachat.uam.exception;

import com.ametsa.smartbachat.uam.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Global exception handler for consistent error responses across all controllers.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        List<ApiErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ApiErrorResponse.FieldError(
                        error.getField(),
                        error.getDefaultMessage(),
                        error.getRejectedValue()))
                .collect(Collectors.toList());

        ApiErrorResponse response = new ApiErrorResponse(
                ApiErrorResponse.CODE_VALIDATION_ERROR,
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                null,
                request.getRequestURI(),
                Instant.now());
        response.setErrors(fieldErrors);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParams(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        
        ApiErrorResponse response = ApiErrorResponse.badRequest(
                request.getRequestURI(),
                String.format("Missing required parameter: %s", ex.getParameterName()));

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        
        ApiErrorResponse response = ApiErrorResponse.badRequest(
                request.getRequestURI(),
                String.format("Invalid value for parameter '%s': %s", ex.getName(), ex.getValue()));

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            NoSuchElementException ex, HttpServletRequest request) {
        
        ApiErrorResponse response = new ApiErrorResponse(
                ApiErrorResponse.CODE_NOT_FOUND,
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage() != null ? ex.getMessage() : "Resource not found",
                null,
                request.getRequestURI(),
                Instant.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        
        ApiErrorResponse response = ApiErrorResponse.badRequest(
                request.getRequestURI(),
                ex.getMessage());

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        
        ApiErrorResponse response = new ApiErrorResponse(
                ApiErrorResponse.CODE_UNAUTHORIZED,
                HttpStatus.UNAUTHORIZED.value(),
                "Invalid credentials",
                null,
                request.getRequestURI(),
                Instant.now());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        
        ApiErrorResponse response = ApiErrorResponse.forbidden(request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        
        ApiErrorResponse response = ApiErrorResponse.unauthorized(request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {
        
        // Check for common auth-related messages
        String message = ex.getMessage();
        if (message != null && (message.contains("Invalid credentials") || 
                message.contains("Account is locked") || 
                message.contains("Account is not active"))) {
            ApiErrorResponse response = ApiErrorResponse.badRequest(request.getRequestURI(), message);
            return ResponseEntity.badRequest().body(response);
        }
        
        log.error("Runtime error for request {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        ApiErrorResponse response = ApiErrorResponse.internalError(request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleAllOtherExceptions(
            Exception ex, HttpServletRequest request) {
        
        log.error("Unexpected error for request {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        
        ApiErrorResponse response = ApiErrorResponse.internalError(request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

