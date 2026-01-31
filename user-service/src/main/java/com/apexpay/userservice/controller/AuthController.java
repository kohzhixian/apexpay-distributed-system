package com.apexpay.userservice.controller;

import com.apexpay.common.constants.HttpHeaders;
import com.apexpay.userservice.dto.request.LoginRequest;
import com.apexpay.userservice.dto.request.RegisterRequest;
import com.apexpay.userservice.dto.response.LoginResponse;
import com.apexpay.userservice.dto.response.LogoutResponse;
import com.apexpay.userservice.dto.response.RefreshResponse;
import com.apexpay.userservice.dto.response.RegisterResponse;
import com.apexpay.userservice.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller handling user authentication endpoints.
 * Provides registration and login functionality for the ApexPay user service.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(
            AuthService authService) {
        this.authService = authService;
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
    public ResponseEntity<@NonNull RegisterResponse> register(@RequestBody @Valid RegisterRequest registerRequest,
                                                              HttpServletResponse response, HttpServletRequest request) {
        RegisterResponse registerResponse = authService.register(registerRequest, response, request);
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
    public ResponseEntity<@NonNull LoginResponse> login(@RequestBody @Valid LoginRequest loginRequest,
                                                        HttpServletResponse response, HttpServletRequest request) {
        LoginResponse loginResponse = authService.login(loginRequest, response, request);
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<@NonNull RefreshResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        RefreshResponse refreshResponse = authService.refresh(request, response);
        return ResponseEntity.ok(refreshResponse);
    }

    /**
     * Logs out the current user by revoking all refresh tokens and clearing cookies.
     *
     * @param userId   the authenticated user's ID from gateway
     * @param response the HTTP response for clearing cookies
     * @return logout confirmation with HTTP 200 status
     */
    @PostMapping("/logout")
    public ResponseEntity<@NonNull LogoutResponse> logout(@RequestHeader(HttpHeaders.X_USER_ID) String userId,
                                                          HttpServletResponse response) {
        LogoutResponse logoutResponse = authService.logout(userId, response);
        return ResponseEntity.ok(logoutResponse);
    }
}
