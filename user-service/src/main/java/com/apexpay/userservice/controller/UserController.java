package com.apexpay.userservice.controller;

import com.apexpay.userservice.dto.request.LoginRequest;
import com.apexpay.userservice.dto.request.RegisterRequest;
import com.apexpay.userservice.dto.response.LoginResponse;
import com.apexpay.userservice.dto.response.RegisterResponse;
import com.apexpay.userservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class UserController {
    private final UserService userService;

    public UserController(
            UserService userService
    ) {
        this.userService = userService;
    }

    public ResponseEntity<RegisterResponse> register(RegisterRequest request) {
        RegisterResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    public ResponseEntity<LoginResponse> login(LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }
}
