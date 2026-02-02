package com.apexpay.apigateway.controller;

import com.apexpay.apigateway.constants.FallbackMessages;
import com.apexpay.apigateway.dto.FallbackResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fallback controller for circuit breaker responses.
 * Provides user-friendly error messages when downstream services
 * are unavailable or exceed response time thresholds.
 */
@RestController
public class FallbackController {

    /**
     * Fallback endpoint for User Service circuit breaker.
     *
     * @return 503 Service Unavailable response with user-friendly message
     */
    @RequestMapping("/user-fallback")
    public ResponseEntity<FallbackResponse> userServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new FallbackResponse(FallbackMessages.USER_SERVICE_UNAVAILABLE));
    }

    /**
     * Fallback endpoint for Wallet Service circuit breaker.
     *
     * @return 503 Service Unavailable response with user-friendly message
     */
    @RequestMapping("/wallet-fallback")
    public ResponseEntity<FallbackResponse> walletServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new FallbackResponse(FallbackMessages.WALLET_SERVICE_UNAVAILABLE));
    }

    /**
     * Fallback endpoint for Payment Service circuit breaker.
     *
     * @return 503 Service Unavailable response with user-friendly message
     */
    @RequestMapping("/payment-fallback")
    public ResponseEntity<FallbackResponse> paymentServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new FallbackResponse(FallbackMessages.PAYMENT_SERVICE_UNAVAILABLE));
    }
}
