package com.ametsa.smartbachat.exception;

import com.ametsa.smartbachat.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

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
                .map(error -> ApiErrorResponse.FieldError.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .rejectedValue(error.getRejectedValue())
                        .build())
                .collect(Collectors.toList());

        ApiErrorResponse response = ApiErrorResponse.builder()
                .code(ApiErrorResponse.CODE_VALIDATION_ERROR)
                .status(HttpStatus.BAD_REQUEST.value())
                .message("Validation failed")
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .errors(fieldErrors)
                .build();

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
        
        ApiErrorResponse response = ApiErrorResponse.notFound(
                request.getRequestURI(),
                ex.getMessage() != null ? ex.getMessage() : "Resource not found");

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

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {
        
        ApiErrorResponse response = ApiErrorResponse.badRequest(
                request.getRequestURI(),
                ex.getMessage());

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(SetuApiException.class)
    public ResponseEntity<ApiErrorResponse> handleSetuApiException(
            SetuApiException ex, HttpServletRequest request) {
        
        log.error("Setu API error: {}", ex.getMessage(), ex);
        
        ApiErrorResponse response = ApiErrorResponse.builder()
                .code(ApiErrorResponse.CODE_SERVICE_UNAVAILABLE)
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .message("Bank aggregator service error")
                .detail(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleAllOtherExceptions(
            Exception ex, HttpServletRequest request) {
        
        log.error("Unexpected error for request {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        
        ApiErrorResponse response = ApiErrorResponse.internalError(request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

