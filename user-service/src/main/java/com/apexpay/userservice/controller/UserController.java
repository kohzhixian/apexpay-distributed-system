package com.apexpay.userservice.controller;

import com.apexpay.userservice.dto.request.LoginRequest;
import com.apexpay.userservice.dto.request.RegisterRequest;
import com.apexpay.userservice.dto.response.LoginResponse;
import com.apexpay.userservice.dto.response.RegisterResponse;
import com.apexpay.userservice.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller handling user authentication endpoints.
 * Provides registration and login functionality for the ApexPay user service.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class UserController {
    private final UserService userService;

    public UserController(
            UserService userService
    ) {
        this.userService = userService;
    }

    /**
     * Registers a new user account.
     * Sets HTTP-only access and refresh token cookies upon successful registration.
     *
     * @param registerRequest the registration details (email, username, password)
     * @param response        the HTTP response for setting cookies
     * @param request         the HTTP request for extracting client IP
     * @return registration confirmation with HTTP 201 status
     */
    @PostMapping("/register")
    public ResponseEntity<@NonNull RegisterResponse> register(@RequestBody @Valid RegisterRequest registerRequest, HttpServletResponse response, HttpServletRequest request) {
        RegisterResponse registerResponse = userService.register(registerRequest, response, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(registerResponse);
    }

    /**
     * Authenticates a user and initiates a session.
     * Sets HTTP-only access and refresh token cookies upon successful login.
     *
     * @param loginRequest the login credentials (email, password)
     * @param response     the HTTP response for setting cookies
     * @param request      the HTTP request for extracting client IP
     * @return login confirmation with HTTP 200 status
     */
    @PostMapping("/login")
    public ResponseEntity<@NonNull LoginResponse> login(@RequestBody @Valid LoginRequest loginRequest, HttpServletResponse response, HttpServletRequest request) {
        LoginResponse loginResponse = userService.login(loginRequest, response, request);
        return ResponseEntity.ok(loginResponse);
    }
}
