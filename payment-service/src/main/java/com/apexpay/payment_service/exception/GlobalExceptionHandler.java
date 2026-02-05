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
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
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
        FeignErrorDetails errorDetails = extractErrorDetails(ex);
        ErrorCode errorCode = errorDetails.errorCode().orElseGet(() -> mapFeignStatusToErrorCode(ex.status()));

        return buildResponse(errorCode, errorDetails.message(), request);
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
     * Handles JSON parsing errors (e.g., invalid UUID format, wrong data types in request body).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<@NonNull ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                               HttpServletRequest request) {
        String errorMessage = extractReadableErrorMessage(ex);
        log.warn("Request body parsing failed: {}", errorMessage);
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED.getCode(),
                "Invalid Request Body", errorMessage, request);
    }

    /**
     * Extracts a user-friendly error message from HttpMessageNotReadableException.
     */
    private String extractReadableErrorMessage(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();

        // Handle Jackson InvalidFormatException (e.g., invalid UUID)
        if (cause instanceof com.fasterxml.jackson.databind.exc.InvalidFormatException invalidFormat) {
            String fieldName = invalidFormat.getPath().isEmpty() ? "field"
                    : invalidFormat.getPath().get(invalidFormat.getPath().size() - 1).getFieldName();
            Class<?> targetType = invalidFormat.getTargetType();
            Object value = invalidFormat.getValue();

            if (targetType == java.util.UUID.class) {
                return String.format("Invalid UUID format for '%s': '%s' is not a valid UUID", fieldName, value);
            }
            return String.format("Invalid value for '%s': cannot convert '%s' to %s",
                    fieldName, value, targetType.getSimpleName());
        }

        // Handle tools.jackson (Jackson 3.x) InvalidFormatException
        if (cause != null && cause.getClass().getName().contains("InvalidFormatException")) {
            String message = cause.getMessage();
            if (message != null && message.contains("UUID")) {
                return "Invalid UUID format in request body. Please provide a valid UUID (e.g., '550e8400-e29b-41d4-a716-446655440000')";
            }
            return "Invalid format in request body: " + message;
        }

        // Fallback to generic message
        String message = ex.getMessage();
        if (message != null && message.contains("UUID")) {
            return "Invalid UUID format in request body. Please provide a valid UUID";
        }
        return "Malformed JSON request body";
    }

    /**
     * Fallback handler for all other exceptions.
     * Also checks if the exception wraps a FeignException to handle wallet service errors.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<@NonNull ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        // Check if this exception wraps a FeignException (common with transaction proxies)
        FeignException feignException = extractFeignException(ex);
        if (feignException != null) {
            log.warn("Found wrapped FeignException, delegating to FeignException handler");
            return handleFeignException(feignException, request);
        }

        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildResponse(ErrorCode.INTERNAL_ERROR, ErrorMessages.UNEXPECTED_ERROR_OCCURRED, request);
    }

    /**
     * Extracts a FeignException from the exception chain if present.
     */
    private FeignException extractFeignException(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof FeignException feignEx) {
                return feignEx;
            }
            current = current.getCause();
        }
        return null;
    }

    /**
     * Extracts error details (message and error code) from FeignException response body.
     * Parses the standardized ErrorResponse format from other ApexPay services.
     *
     * @param ex the FeignException containing the error response
     * @return FeignErrorDetails containing the extracted message and optional ErrorCode
     */
    private FeignErrorDetails extractErrorDetails(FeignException ex) {
        try {
            if (ex.responseBody().isPresent()) {
                String body = StandardCharsets.UTF_8.decode(ex.responseBody().get().duplicate()).toString();
                log.debug("Feign error response body: {}", body);

                JsonNode jsonNode = objectMapper.readTree(body);

                // Extract message from standardized ErrorResponse format
                String message = extractMessageFromJson(jsonNode);

                // Try to map the code to our ErrorCode enum
                Optional<ErrorCode> errorCode = extractErrorCodeFromJson(jsonNode);

                if (message != null) {
                    return new FeignErrorDetails(message, errorCode);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to parse Feign error response: {}", e.getMessage());
        }

        // Fallback to status-based messages
        String fallbackMessage = switch (ex.status()) {
            case 400 -> "Bad request to wallet service";
            case 403 -> "Insufficient balance or access denied";
            case 404 -> "Wallet not found";
            case 409 -> "Wallet operation conflict";
            case 503 -> "Wallet service unavailable";
            default -> "Wallet service error";
        };
        return new FeignErrorDetails(fallbackMessage, Optional.empty());
    }

    /**
     * Extracts the error message from JSON response.
     */
    private String extractMessageFromJson(JsonNode jsonNode) {
        // Try "message" field first (our standard ErrorResponse format)
        if (jsonNode.has("message") && !jsonNode.get("message").isNull()) {
            return jsonNode.get("message").asText();
        }
        // Fallback to "error" field
        if (jsonNode.has("error") && !jsonNode.get("error").isNull()) {
            return jsonNode.get("error").asText();
        }
        return null;
    }

    /**
     * Extracts and maps the error code from JSON response to ErrorCode enum.
     */
    private Optional<ErrorCode> extractErrorCodeFromJson(JsonNode jsonNode) {
        if (!jsonNode.has("code") || jsonNode.get("code").isNull()) {
            return Optional.empty();
        }

        int code = jsonNode.get("code").asInt();
        return mapCodeToErrorCode(code);
    }

    /**
     * Maps numeric error codes from other services to ErrorCode enum.
     */
    private Optional<ErrorCode> mapCodeToErrorCode(int code) {
        for (ErrorCode errorCode : ErrorCode.values()) {
            if (errorCode.getCode() == code) {
                return Optional.of(errorCode);
            }
        }
        return Optional.empty();
    }

    /**
     * Maps Feign HTTP status codes to appropriate ErrorCodes.
     * Used as fallback when error code cannot be extracted from response body.
     */
    private ErrorCode mapFeignStatusToErrorCode(int status) {
        return switch (status) {
            case 400 -> ErrorCode.VALIDATION_FAILED;
            case 403 -> ErrorCode.INSUFFICIENT_BALANCE;
            case 404 -> ErrorCode.WALLET_NOT_FOUND;
            case 409 -> ErrorCode.CONCURRENT_MODIFICATION;
            case 503 -> ErrorCode.SERVICE_UNAVAILABLE;
            default -> ErrorCode.PAYMENT_PROCESSING_ERROR;
        };
    }

    /**
     * Record to hold extracted error details from Feign response.
     */
    private record FeignErrorDetails(String message, Optional<ErrorCode> errorCode) {}

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
