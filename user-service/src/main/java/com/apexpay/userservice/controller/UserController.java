package com.apexpay.userservice.controller;

import com.apexpay.userservice.dto.request.LoginRequest;
import com.apexpay.userservice.dto.request.RegisterRequest;
import com.apexpay.userservice.dto.response.LoginResponse;
import com.apexpay.userservice.dto.response.RegisterResponse;
import com.apexpay.userservice.service.UserService;
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
     * Sets an HTTP-only access token cookie upon successful registration.
     *
     * @param request  the registration details (email, username, password)
     * @param response the HTTP response for setting cookies
     * @return registration confirmation with HTTP 201 status
     */
    @PostMapping("/register")
    public ResponseEntity<@NonNull RegisterResponse> register(@RequestBody @Valid RegisterRequest request, HttpServletResponse response) {
        RegisterResponse registerResponse = userService.register(request, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(registerResponse);
    }

    /**
     * Authenticates a user and initiates a session.
     * Sets an HTTP-only access token cookie upon successful login.
     *
     * @param request  the login credentials (email, password)
     * @param response the HTTP response for setting cookies
     * @return login confirmation with HTTP 200 status
     */
    @PostMapping("/login")
    public ResponseEntity<@NonNull LoginResponse> login(@RequestBody @Valid LoginRequest request, HttpServletResponse response) {
        LoginResponse loginResponse = userService.login(request, response);
        return ResponseEntity.ok(loginResponse);
    }
}
