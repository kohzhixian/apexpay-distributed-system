package com.apexpay.wallet_service.exceptions;

import com.apexpay.common.dto.ErrorResponse;
import com.apexpay.common.exception.BusinessException;
import com.apexpay.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Global exception handler for wallet service.
 * Converts exceptions to standardized error responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles all BusinessException with ErrorCode.
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
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED.getCode(), "Validation Failed",
                errorMessage, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<@NonNull ErrorResponse> handleValidation(ConstraintViolationException ex, HttpServletRequest request) {

        String errorMessage = ex.getConstraintViolations()
                .stream()
                .map(violation -> {
                    String field = extractFieldName(violation.getPropertyPath());
                    return field + ": " + violation.getMessage();
                })
                .collect(Collectors.joining(", "));

        log.warn("Validation failed: {}", errorMessage);
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED.getCode(), "Validation Failed",
                errorMessage, request);
    }

    private String extractFieldName(Path propertyPath) {
        String fullPath = propertyPath.toString();
        // Extract just the field name from path like "methodName.paramName.fieldName"
        int lastDot = fullPath.lastIndexOf(".");
        return lastDot > 0 ? fullPath.substring(lastDot + 1) : fullPath;
    }

    /**
     * Fallback handler for all other exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<@NonNull ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildResponse(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred.", request);
    }

    /**
     * Helper to build response using ErrorCode.
     */
    private ResponseEntity<@NonNull ErrorResponse> buildResponse(ErrorCode errorCode, String message,
                                                                 HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                errorCode.getHttpStatus().value(),
                errorCode.getCode(),
                errorCode.getHttpStatus().getReasonPhrase(),
                message,
                request.getRequestURI());
        return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
    }

    /**
     * Helper to build response using explicit HttpStatus.
     */
    private ResponseEntity<@NonNull ErrorResponse> buildResponse(HttpStatus status, int code, String error,
                                                                 String message, HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                status.value(), code, error, message, request.getRequestURI());
        return new ResponseEntity<>(errorResponse, status);
    }
}
