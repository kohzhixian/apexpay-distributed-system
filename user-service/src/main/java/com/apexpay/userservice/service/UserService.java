package com.apexpay.userservice.service;

import com.apexpay.userservice.dto.request.LoginRequest;
import com.apexpay.userservice.dto.request.RegisterRequest;
import com.apexpay.userservice.dto.response.LoginResponse;
import com.apexpay.userservice.dto.response.RegisterResponse;
import com.apexpay.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;

    public UserService(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    public RegisterResponse register(RegisterRequest request) {
        if (request.email() == null || request.password() == null) {
            logger.error("invalid input");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input.");
        }
        return new RegisterResponse("", "", "");
    }

    public LoginResponse login(LoginRequest request) {
        return new LoginResponse("", "", "");
    }
}
