package com.apexpay.userservice.service;

import com.apexpay.userservice.dto.RefreshTokenObj;
import com.apexpay.userservice.dto.request.LoginRequest;
import com.apexpay.userservice.dto.request.RegisterRequest;
import com.apexpay.userservice.dto.response.LoginResponse;
import com.apexpay.userservice.dto.response.RegisterResponse;
import com.apexpay.userservice.entity.RefreshTokens;
import com.apexpay.userservice.entity.UserPrincipal;
import com.apexpay.userservice.entity.Users;
import com.apexpay.userservice.repository.RefreshtokenRepository;
import com.apexpay.userservice.repository.UserRepository;
import com.apexpay.userservice.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Service handling user authentication operations including registration and login.
 * Manages JWT access tokens and refresh tokens with HTTP-only cookie storage for secure authentication.
 * Implements refresh token rotation pattern for enhanced security.
 */
@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final RefreshtokenRepository refreshtokenRepository;
    private final long jwtTimeout;
    private final long refreshTokenTimeout;
    private final boolean cookieSecureValue;

    public UserService(
            UserRepository userRepository,
            AuthenticationManager authManager,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            RefreshtokenRepository refreshtokenRepository,
            @Value("${apexpay.jwt-timeout}")  long jwtTimeout,
            @Value("${apexpay.refresh-token-timeout}") long refreshTokenTimeout,
            @Value("${apexpay.cookie-secure-value}") boolean cookieSecureValue) {
        this.userRepository = userRepository;
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.refreshtokenRepository = refreshtokenRepository;
        this.jwtTimeout = jwtTimeout;
        this.refreshTokenTimeout = refreshTokenTimeout;
        this.cookieSecureValue = cookieSecureValue;
    }

    /**
     * Registers a new user account.
     * Validates uniqueness of username and email, creates the user,
     * and sets HTTP-only access and refresh token cookies.
     *
     * @param registerRquest the registration details
     * @param response       the HTTP response for setting cookies
     * @param request        the HTTP request for extracting client IP
     * @return registration confirmation response
     * @throws ResponseStatusException if username or email already exists
     */
    @Transactional
    public RegisterResponse register(RegisterRequest registerRquest, HttpServletResponse response, HttpServletRequest request) {
        // Username - usually public/visible, safe to be specific
        if (checkIfUsernameExist(registerRquest.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken.");
        }

        // Email - more sensitive, can use generic message OR specific with rate limiting
        if (checkIfUserEmailExist(registerRquest.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered.");
        }

        String encodedPassword = passwordEncoder.encode(registerRquest.password());

        Users newUser = Users.builder()
                .username(registerRquest.username())
                .hashedPassword(encodedPassword)
                .email(registerRquest.email())
                .build();

        userRepository.save(newUser);
        generateAndStoreTokens(newUser, request, response);
        return new RegisterResponse("Registration was completed successfully.");
    }

    /**
     * Authenticates a user with email and password.
     * On success, generates and stores HTTP-only access and refresh token cookies.
     *
     * @param loginRequest the login credentials
     * @param response     the HTTP response for setting cookies
     * @param request      the HTTP request for extracting client IP
     * @return login confirmation response
     * @throws ResponseStatusException if authentication fails
     */
    @Transactional
    public LoginResponse login(LoginRequest loginRequest, HttpServletResponse response, HttpServletRequest request) {
        try {
            Authentication authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password()));

            if (authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserPrincipal(
                    Users user
            )) {
                generateAndStoreTokens(user, request, response);
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
                .secure(cookieSecureValue) // only sent over HTTPs (using false for localhost)
                .path("/") // available for all routes
                .maxAge(jwtTimeout / 1000) // convert to seconds
                .sameSite("Strict") // essential for csrf protection
                .build();

        // Add cookies to response
        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
    }


    private void storeRefreshTokenIntoHeader(String refreshToken, HttpServletResponse response){
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(cookieSecureValue)
                .path("/refresh")
                .maxAge(refreshTokenTimeout / 1000) // convert to seconds
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
    }

    private boolean checkIfUserEmailExist(String email) {
        Users existingUser = userRepository.findByEmail(email);
        return existingUser != null;
    }

    private boolean checkIfUsernameExist(String username) {
        Users existingUser = userRepository.findByUsername(username);
        return existingUser != null;
    }

    /**
     * Creates a new refresh token entity with hashed token for database storage.
     *
     * @param request the HTTP request for extracting client IP address
     * @param user    the user to associate with the refresh token
     * @return object containing both the entity (hashed) and raw token (for cookie)
     */
    private RefreshTokenObj generateRefreshToken(HttpServletRequest request, Users user) {
        String refreshToken = UUID.randomUUID().toString();
        String hashedRefreshToken = passwordEncoder.encode(refreshToken);
        String ipAddress = request.getRemoteAddr();
        Instant expiryDate = Instant.now().plus(refreshTokenTimeout, ChronoUnit.MILLIS);

        RefreshTokens newRefreshToken = RefreshTokens.builder()
                .hashedRefreshToken(hashedRefreshToken)
                .user(user)
                .ipAddress(ipAddress)
                .expiryDate(expiryDate)
                .build();

        return new RefreshTokenObj(newRefreshToken, refreshToken);
    }

    /**
     * Generates both access and refresh tokens, persists refresh token to database,
     * and stores both tokens in HTTP-only cookies.
     *
     * @param user     the authenticated user
     * @param request  the HTTP request for extracting client IP
     * @param response the HTTP response for setting cookies
     */
    private void generateAndStoreTokens(Users user, HttpServletRequest request, HttpServletResponse response) {
        // Generate and store access token
        String accessToken = jwtService.generateToken(user);
        storeAccessTokenIntoHeader(accessToken, response);

        // Generate, save and store refresh token
        RefreshTokenObj refreshTokenObj = generateRefreshToken(request, user);
        refreshtokenRepository.save(refreshTokenObj.entity());
        storeRefreshTokenIntoHeader(refreshTokenObj.refreshToken(), response);
    }
}
