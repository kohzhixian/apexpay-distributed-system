package com.apexpay.wallet_service.exceptions;

import com.apexpay.common.constants.ErrorMessages;
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
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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
     *
     * @param ex      the business exception containing error code and message
     * @param request the HTTP request that triggered the exception
     * @return error response with appropriate HTTP status
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<@NonNull ErrorResponse> handleBusinessException(BusinessException ex,
                                                                          HttpServletRequest request) {
        log.error("Business error [{}]: {}", ex.getErrorCode().getCode(), ex.getMessage());
        return buildResponse(ex.getErrorCode(), ex.getMessage(), request);
    }

    /**
     * Handles Spring's @Valid annotation validation failures.
     *
     * @param ex      the validation exception with field errors
     * @param request the HTTP request that triggered the exception
     * @return error response with validation error details
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
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED.getCode(), ErrorMessages.VALIDATION_FAILED,
                errorMessage, request);
    }

    /**
     * Handles Bean Validation constraint violations (e.g., @NotNull on path variables).
     *
     * @param ex      the constraint violation exception
     * @param request the HTTP request that triggered the exception
     * @return error response with validation error details
     */
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
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED.getCode(), ErrorMessages.VALIDATION_FAILED,
                errorMessage, request);
    }

    /**
     * Handles type mismatch exceptions (e.g., invalid UUID format in path variables).
     *
     * @param ex      the type mismatch exception with parameter details
     * @param request the HTTP request that triggered the exception
     * @return error response with parameter format error details
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<@NonNull ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                                      HttpServletRequest request) {
        String errorMessage = String.format(ErrorMessages.INVALID_PARAMETER_FORMAT,
                ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "valid format");
        log.warn("Type mismatch: {}", errorMessage);
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED.getCode(), "Invalid Parameter Format",
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
     *
     * @param ex      the unexpected exception
     * @param request the HTTP request that triggered the exception
     * @return generic internal server error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<@NonNull ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildResponse(ErrorCode.INTERNAL_ERROR, ErrorMessages.UNEXPECTED_ERROR_OCCURRED, request);
    }

    /**
     * Builds an error response using an ErrorCode enum.
     *
     * @param errorCode the error code containing HTTP status and code
     * @param message   the error message to include
     * @param request   the HTTP request for extracting the request URI
     * @return ResponseEntity with the constructed ErrorResponse
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
     * Builds an error response using explicit HTTP status and error details.
     *
     * @param status  the HTTP status code
     * @param code    the application error code
     * @param error   the error type/name
     * @param message the error message to include
     * @param request the HTTP request for extracting the request URI
     * @return ResponseEntity with the constructed ErrorResponse
     */
    private ResponseEntity<@NonNull ErrorResponse> buildResponse(HttpStatus status, int code, String error,
                                                                 String message, HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                status.value(), code, error, message, request.getRequestURI());
        return new ResponseEntity<>(errorResponse, status);
    }
}
