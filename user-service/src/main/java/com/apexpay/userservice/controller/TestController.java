package com.apexpay.userservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test controller for verifying service connectivity.
 * Provides a simple health check endpoint for authenticated users.
 */
@RestController
@RequestMapping("/api/v1/test")
public class TestController {

    /**
     * Simple test endpoint to verify the service is running and authentication works.
     *
     * @return success message confirming the test passed
     */
    @GetMapping
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("testing success.");
    }
}
