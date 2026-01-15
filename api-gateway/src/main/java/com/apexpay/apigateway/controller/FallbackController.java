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

    @RequestMapping("/user-fallback")
    public ResponseEntity<@NonNull String> userServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                "The User Service is currently taking too long to respond or is down.");
    }
}
