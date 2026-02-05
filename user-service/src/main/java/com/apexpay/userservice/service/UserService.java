package com.apexpay.userservice.service;

import com.apexpay.common.exception.BusinessException;
import com.apexpay.common.exception.ErrorCode;
import com.apexpay.userservice.dto.response.GetUserDetailsResponse;
import com.apexpay.userservice.entity.Users;
import com.apexpay.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for user profile operations.
 * <p>
 * Handles retrieval of user details and profile information.
 * </p>
 */
@Service
public class UserService {
    private final UserRepository userRepository;

    /**
     * Constructs a new UserService with the required repository.
     *
     * @param userRepository the repository for user operations
     */
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Retrieves user details by email address.
     *
     * @param email the email of the user to retrieve
     * @return response containing user ID and username
     * @throws BusinessException if user not found
     */
    @Transactional
    public GetUserDetailsResponse getUserDetails(String email) {
        Users existingUser = userRepository.findByEmail(email);

        if (existingUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "User not found.");
        }

        return new GetUserDetailsResponse(existingUser.getId(), existingUser.getUsername());
    }
}
