package com.apexpay.payment_service.exception;

import com.apexpay.common.constants.ErrorMessages;
import com.apexpay.common.dto.ErrorResponse;
import com.apexpay.common.exception.BusinessException;
import com.apexpay.common.exception.ErrorCode;
import com.apexpay.payment_service.client.provider.exception.PaymentProviderException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
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

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Global exception handler for payment service.
 * Converts exceptions to standardized error responses, including
 * FeignException from wallet service calls.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

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
     * Handles PaymentProviderException from external payment providers.
     */
    @ExceptionHandler(PaymentProviderException.class)
    public ResponseEntity<@NonNull ErrorResponse> handlePaymentProviderException(PaymentProviderException ex,
                                                                                  HttpServletRequest request) {
        log.error("Payment provider error [{}]: {}", ex.getProviderName(), ex.getMessage());

        ErrorCode errorCode = ex.isRetryable()
                ? ErrorCode.PAYMENT_PROVIDER_UNAVAILABLE
                : ErrorCode.PAYMENT_CHARGE_FAILED;

        return buildResponse(errorCode, ex.getMessage(), request);
    }

    /**
     * Handles FeignException from inter-service communication (e.g., wallet service).
     * Extracts error details from the response body when available.
     */
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<@NonNull ErrorResponse> handleFeignException(FeignException ex,
                                                                       HttpServletRequest request) {
        log.error("Feign client error: status={}, message={}", ex.status(), ex.getMessage());

        // Try to extract error details from response body
        String errorMessage = extractErrorMessage(ex);
        ErrorCode errorCode = mapFeignStatusToErrorCode(ex.status());

        return buildResponse(errorCode, errorMessage, request);
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
                ErrorMessages.VALIDATION_FAILED, errorMessage, request);
    }

    /**
     * Handles constraint violation exceptions.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<@NonNull ErrorResponse> handleValidation(ConstraintViolationException ex,
                                                                   HttpServletRequest request) {
        String errorMessage = ex.getConstraintViolations()
                .stream()
                .map(violation -> {
                    String field = extractFieldName(violation.getPropertyPath());
                    return field + ": " + violation.getMessage();
                })
                .collect(Collectors.joining(", "));

        log.warn("Validation failed: {}", errorMessage);
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED.getCode(),
                ErrorMessages.VALIDATION_FAILED, errorMessage, request);
    }

    /**
     * Handles type mismatch exceptions (e.g., invalid UUID format in path variables).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<@NonNull ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                                     HttpServletRequest request) {
        String errorMessage = String.format(ErrorMessages.INVALID_PARAMETER_FORMAT,
                ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "valid format");
        log.warn("Type mismatch: {}", errorMessage);
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED.getCode(),
                "Invalid Parameter Format", errorMessage, request);
    }

    /**
     * Fallback handler for all other exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<@NonNull ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildResponse(ErrorCode.INTERNAL_ERROR, ErrorMessages.UNEXPECTED_ERROR_OCCURRED, request);
    }

    /**
     * Extracts error message from FeignException response body.
     * Falls back to exception message if parsing fails.
     */
    private String extractErrorMessage(FeignException ex) {
        try {
            if (ex.responseBody().isPresent()) {
                String body = StandardCharsets.UTF_8.decode(ex.responseBody().get()).toString();
                JsonNode jsonNode = objectMapper.readTree(body);

                // Try common error response fields
                if (jsonNode.has("message")) {
                    return jsonNode.get("message").asText();
                }
                if (jsonNode.has("error")) {
                    return jsonNode.get("error").asText();
                }
                return body;
            }
        } catch (Exception e) {
            log.debug("Failed to parse Feign error response: {}", e.getMessage());
        }

        return switch (ex.status()) {
            case 400 -> "Bad request to wallet service";
            case 403 -> "Insufficient balance or access denied";
            case 404 -> "Wallet not found";
            case 503 -> "Wallet service unavailable";
            default -> "Wallet service error: " + ex.getMessage();
        };
    }

    /**
     * Maps Feign HTTP status codes to appropriate ErrorCodes.
     */
    private ErrorCode mapFeignStatusToErrorCode(int status) {
        return switch (status) {
            case 400 -> ErrorCode.VALIDATION_FAILED;
            case 403 -> ErrorCode.INSUFFICIENT_BALANCE;
            case 404 -> ErrorCode.WALLET_NOT_FOUND;
            case 503 -> ErrorCode.SERVICE_UNAVAILABLE;
            default -> ErrorCode.PAYMENT_PROCESSING_ERROR;
        };
    }

    private String extractFieldName(Path propertyPath) {
        String fullPath = propertyPath.toString();
        int lastDot = fullPath.lastIndexOf(".");
        return lastDot > 0 ? fullPath.substring(lastDot + 1) : fullPath;
    }

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

    private ResponseEntity<@NonNull ErrorResponse> buildResponse(HttpStatus status, int code, String error,
                                                                 String message, HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                status.value(), code, error, message, request.getRequestURI());
        return new ResponseEntity<>(errorResponse, status);
    }
}
