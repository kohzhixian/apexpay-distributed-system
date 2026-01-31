package com.apexpay.apigateway.controller;

import lombok.NonNull;
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
    public ResponseEntity<@NonNull String> userServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                "The User Service is currently taking too long to respond or is down.");
    }

    /**
     * Fallback endpoint for Wallet Service circuit breaker.
     *
     * @return 503 Service Unavailable response with user-friendly message
     */
    @RequestMapping("/wallet-fallback")
    public ResponseEntity<@NonNull String> walletServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("The Wallet Service is currently taking too long to respond or is down.");
    }

    /**
     * Fallback endpoint for Payment Service circuit breaker.
     *
     * @return 503 Service Unavailable response with user-friendly message
     */
    @RequestMapping("/payment-fallback")
    public ResponseEntity<@NonNull String> paymentServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("The Payment Service is currently taking too long to respond or is down.");
    }
}
