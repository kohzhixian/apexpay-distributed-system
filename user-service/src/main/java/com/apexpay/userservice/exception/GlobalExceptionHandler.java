package com.apexpay.userservice.exception;

import com.apexpay.userservice.helper.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles all BusinessException with ErrorCode.
     * This is the primary handler for application-specific errors.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<@NonNull ErrorResponse> handleBusinessException(BusinessException ex,
            HttpServletRequest request) {
        log.error("Business error [{}]: {}", ex.getErrorCode().getCode(), ex.getMessage());
        return buildResponse(ex.getErrorCode(), ex.getMessage(), request);
    }

    /**
     * Handles Spring's @Valid annotation validation failures.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<@NonNull ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation failed: {}", errorMessage);
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED.getCode(),
                "Validation Failed", errorMessage, request);
    }

    /**
     * Handles Spring Security's BadCredentialsException (thrown by
     * AuthenticationManager).
     * Returns generic "Invalid credentials" to prevent user enumeration attacks.
     */
    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<@NonNull ErrorResponse> handleSpringBadCredentials(
            org.springframework.security.authentication.BadCredentialsException ex,
            HttpServletRequest request) {
        log.warn("Spring Security authentication failed: {}", ex.getMessage());
        return buildResponse(ErrorCode.INVALID_CREDENTIALS, "Invalid credentials", request);
    }

    /**
     * Handles Spring Security's UsernameNotFoundException.
     * Returns generic "Invalid credentials" to prevent user enumeration attacks.
     */
    @ExceptionHandler(org.springframework.security.core.userdetails.UsernameNotFoundException.class)
    public ResponseEntity<@NonNull ErrorResponse> handleUsernameNotFound(
            org.springframework.security.core.userdetails.UsernameNotFoundException ex,
            HttpServletRequest request) {
        log.warn("User not found: {}", ex.getMessage());
        return buildResponse(ErrorCode.INVALID_CREDENTIALS, "Invalid credentials", request);
    }

    /**
     * Handles Spring Security's AccessDeniedException.
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<@NonNull ErrorResponse> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex,
            HttpServletRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildResponse(ErrorCode.ACCESS_DENIED, ex.getMessage(), request);
    }

    /**
     * Fallback handler for all other exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<@NonNull ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildResponse(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred", request);
    }

    /** Helper to build response using ErrorCode */
    private ResponseEntity<@NonNull ErrorResponse> buildResponse(ErrorCode errorCode, String message,
            HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(
                errorCode.getHttpStatus().value(),
                errorCode.getCode(),
                errorCode.getHttpStatus().getReasonPhrase(),
                message,
                request.getRequestURI());
        return new ResponseEntity<>(response, errorCode.getHttpStatus());
    }

    /**
     * Helper to build response using explicit HttpStatus (for Spring exceptions)
     */
    private ResponseEntity<@NonNull ErrorResponse> buildResponse(HttpStatus status, int code, String error,
            String message,
            HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(status.value(), code, error, message, request.getRequestURI());
        return new ResponseEntity<>(response, status);
    }
}
