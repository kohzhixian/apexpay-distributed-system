package com.apexpay.userservice.service;

import com.apexpay.userservice.entity.UserPrincipal;
import com.apexpay.userservice.entity.Users;
import com.apexpay.userservice.repository.UserRepository;
import lombok.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Custom UserDetailsService implementation for Spring Security.
 * Loads user details by email for authentication purposes.
 */
@Service
public class MyUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public MyUserDetailsService(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    /**
     * Loads user details by email address for authentication.
     *
     * @param email the user's email address (used as username)
     * @return UserPrincipal containing the user's security information
     * @throws UsernameNotFoundException if no user found with the given email
     */
    @Override
    @NonNull
    public UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        Users user = userRepository.findByEmail(email);

        if (user == null) {
            throw new UsernameNotFoundException("User not found.");
        }
        return new UserPrincipal(user);
    }
}
