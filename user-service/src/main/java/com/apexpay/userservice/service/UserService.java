package com.apexpay.userservice.service;

import com.apexpay.common.exception.BusinessException;
import com.apexpay.common.exception.ErrorCode;
import com.apexpay.userservice.dto.response.GetUserDetailsResponse;
import com.apexpay.userservice.entity.Users;
import com.apexpay.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public GetUserDetailsResponse getUserDetails(String email) {
        Users existingUser = userRepository.findByEmail(email);

        if (existingUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "User not found.");
        }

        return new GetUserDetailsResponse(existingUser.getId(), existingUser.getUsername());
    }
}
