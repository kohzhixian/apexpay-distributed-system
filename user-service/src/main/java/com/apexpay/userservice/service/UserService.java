package com.apexpay.userservice.service;

import com.apexpay.userservice.dto.request.LoginRequest;
import com.apexpay.userservice.dto.request.RegisterRequest;
import com.apexpay.userservice.dto.response.LoginResponse;
import com.apexpay.userservice.dto.response.RegisterResponse;
import com.apexpay.userservice.entity.UserPrincipal;
import com.apexpay.userservice.entity.Users;
import com.apexpay.userservice.repository.UserRepository;
import com.apexpay.userservice.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service handling user authentication operations including registration and login.
 * Manages JWT token generation and HTTP-only cookie storage for secure authentication.
 */
@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;

    public UserService(
            UserRepository userRepository,
            AuthenticationManager authManager,
            JwtService jwtService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user account.
     * Validates uniqueness of username and email, creates the user,
     * and sets an HTTP-only access token cookie.
     *
     * @param request  the registration details
     * @param response the HTTP response for setting cookies
     * @return registration confirmation response
     * @throws ResponseStatusException if username or email already exists
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request, HttpServletResponse response) {
        // Username - usually public/visible, safe to be specific
        if (checkIfUsernameExist(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken.");
        }

        // Email - more sensitive, can use generic message OR specific with rate limiting
        if (checkIfUserEmailExist(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered.");
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        Users newUser = Users.builder()
                .username(request.username())
                .hashedPassword(encodedPassword)
                .email(request.email())
                .build();

        userRepository.save(newUser);

        // generate access token
        String accessToken = jwtService.generateToken(newUser);

        // store access token into HTTP-only cookie
        storeAccessTokenIntoHeader(accessToken, response);

        // generate refresh token
        return new RegisterResponse("Registration was completed successfully.");
    }

    /**
     * Authenticates a user with email and password.
     * On success, generates and stores an HTTP-only access token cookie.
     *
     * @param request  the login credentials
     * @param response the HTTP response for setting cookies
     * @return login confirmation response
     * @throws ResponseStatusException if authentication fails
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request, HttpServletResponse response) {
        try {
            Authentication authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));

            if (authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserPrincipal(
                    Users user
            )) {
                // generate access token
                String accessToken = jwtService.generateToken(user);
                storeAccessTokenIntoHeader(accessToken, response);
                // generate refresh token
                return new LoginResponse("Login Success.");
            }

        } catch (AuthenticationException e) {
            logger.error("Authentication failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication failed.");

    }

    private void storeAccessTokenIntoHeader(String accessToken, HttpServletResponse response) {
        ResponseCookie accessTokenCookie = ResponseCookie.from("access_token", accessToken)
                .httpOnly(true) // prevents XSS
                .secure(false) // only sent over HTTPs (using false for localhost)
                .path("/") // available for all routes
                .maxAge(900) // 15 minutes (match JWT expiration)
                .sameSite("Strict") // essential for csrf protection
                .build();

        // Add cookies to response
        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
    }

    private boolean checkIfUserEmailExist(String email) {
        Users existingUser = userRepository.findByEmail(email);
        return existingUser != null;
    }

    private boolean checkIfUsernameExist(String username) {
        Users existingUser = userRepository.findByUsername(username);
        return existingUser != null;
    }
}
