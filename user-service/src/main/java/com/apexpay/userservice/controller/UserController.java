package com.apexpay.userservice.controller;

import com.apexpay.common.constants.HttpHeaders;
import com.apexpay.userservice.dto.response.GetUserDetailsResponse;
import com.apexpay.userservice.service.UserService;
import lombok.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<@NonNull GetUserDetailsResponse> getUserDetails(@RequestHeader(HttpHeaders.X_USER_EMAIL) String email) {
        GetUserDetailsResponse response = userService.getUserDetails(email);
        return ResponseEntity.ok(response);
    }
}
