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

/**
 * REST controller for user profile operations.
 * <p>
 * Provides endpoints for retrieving user details.
 * All endpoints require authentication via the X-USER-EMAIL header.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/user")
public class UserController {
    private final UserService userService;

    /**
     * Constructs a new UserController with the required service.
     *
     * @param userService the user service for business logic
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Retrieves the authenticated user's profile details.
     *
     * @param email the authenticated user's email from the X-USER-EMAIL header
     * @return 200 OK with user ID and username
     */
    @GetMapping("/me")
    public ResponseEntity<@NonNull GetUserDetailsResponse> getUserDetails(@RequestHeader(HttpHeaders.X_USER_EMAIL) String email) {
        GetUserDetailsResponse response = userService.getUserDetails(email);
        return ResponseEntity.ok(response);
    }
}
