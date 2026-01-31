package com.apexpay.userservice.service;

import com.apexpay.common.constants.ErrorMessages;
import com.apexpay.common.constants.ResponseMessages;
import com.apexpay.common.exception.BusinessException;
import com.apexpay.common.exception.ErrorCode;
import com.apexpay.userservice.constants.AuthConstants;
import com.apexpay.userservice.dto.RefreshTokenCookieText;
import com.apexpay.userservice.dto.RefreshTokenObj;
import com.apexpay.userservice.dto.request.LoginRequest;
import com.apexpay.userservice.dto.request.RegisterRequest;
import com.apexpay.userservice.dto.response.LoginResponse;
import com.apexpay.userservice.dto.response.LogoutResponse;
import com.apexpay.userservice.dto.response.RefreshResponse;
import com.apexpay.userservice.dto.response.RegisterResponse;
import com.apexpay.userservice.entity.RefreshTokens;
import com.apexpay.userservice.entity.UserPrincipal;
import com.apexpay.userservice.entity.Users;
import com.apexpay.userservice.repository.RefreshtokenRepository;
import com.apexpay.userservice.repository.UserRepository;
import com.apexpay.userservice.security.JwtService;
import com.apexpay.userservice.utils.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Service handling user authentication operations including registration and
 * login.
 * Manages JWT access tokens and refresh tokens with HTTP-only cookie storage
 * for secure authentication.
 * Implements refresh token rotation pattern for enhanced security.
 */
@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final RefreshtokenRepository refreshtokenRepository;
    private final long jwtTimeout;
    private final long refreshTokenTimeout;
    private final boolean cookieSecureValue;
    private final RefreshTokenRevocationService refreshTokenRevocationService;
    private final CookieUtils cookieUtils;

    public UserService(
            UserRepository userRepository,
            AuthenticationManager authManager,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            RefreshtokenRepository refreshtokenRepository,
            @Value("${apexpay.jwt-timeout}") long jwtTimeout,
            @Value("${apexpay.refresh-token-timeout}") long refreshTokenTimeout,
            @Value("${apexpay.cookie-secure-value}") boolean cookieSecureValue,
            RefreshTokenRevocationService refreshTokenRevocationService,
            CookieUtils cookieUtils) {
        this.userRepository = userRepository;
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.refreshtokenRepository = refreshtokenRepository;
        this.jwtTimeout = jwtTimeout;
        this.refreshTokenTimeout = refreshTokenTimeout;
        this.cookieSecureValue = cookieSecureValue;
        this.refreshTokenRevocationService = refreshTokenRevocationService;
        this.cookieUtils = cookieUtils;
    }

    /**
     * Registers a new user account.
     * Validates uniqueness of username and email, creates the user,
     * and sets HTTP-only access and refresh token cookies.
     */
    @Transactional
    public RegisterResponse register(RegisterRequest registerRequest, HttpServletResponse response,
                                     HttpServletRequest request) {
        if (checkIfUsernameExist(registerRequest.username())) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS, ErrorMessages.USERNAME_ALREADY_TAKEN);
        }

        if (checkIfUserEmailExist(registerRequest.email())) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS, ErrorMessages.EMAIL_ALREADY_REGISTERED);
        }

        String encodedPassword = passwordEncoder.encode(registerRequest.password());
        UUID familyId = UUID.randomUUID();
        Users newUser = Users.builder()
                .username(registerRequest.username())
                .hashedPassword(encodedPassword)
                .email(registerRequest.email())
                .build();

        userRepository.save(newUser);
        generateAndStoreTokens(newUser, request, response, familyId);
        return new RegisterResponse(ResponseMessages.REGISTRATION_SUCCESS);
    }

    /**
     * Authenticates a user with email and password.
     * On success, generates and stores HTTP-only access and refresh token cookies.
     */
    @Transactional
    public LoginResponse login(LoginRequest loginRequest, HttpServletResponse response, HttpServletRequest request) {
        try {
            Authentication authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password()));

            if (authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof UserPrincipal(Users user)) {
                UUID familyId = UUID.randomUUID();
                refreshtokenRepository.revokeAllRefreshTokensByUserId(user.getId());
                generateAndStoreTokens(user, request, response, familyId);
                return new LoginResponse(ResponseMessages.LOGIN_SUCCESS);
            }
        } catch (AuthenticationException e) {
            log.error("Authentication failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, ErrorMessages.INVALID_EMAIL_OR_PASSWORD);
        }

        throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED);
    }

    /**
     * Refreshes access and refresh tokens using token rotation pattern.
     * Uses pessimistic locking (SELECT FOR UPDATE) to prevent race conditions
     * where concurrent requests could bypass token consumption checks.
     */
    @Transactional
    public RefreshResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshTokenCookieText = cookieUtils.getCookieValue(request, AuthConstants.COOKIE_REFRESH_TOKEN);
        String ipAddress = request.getRemoteAddr();

        if (refreshTokenCookieText == null || refreshTokenCookieText.isBlank()) {
            throw new BusinessException(ErrorCode.TOKEN_MISSING, ErrorMessages.REFRESH_TOKEN_NOT_FOUND);
        }

        RefreshTokenCookieText refreshTokenCookieTextObj = splitRefreshTokenCookieText(refreshTokenCookieText);

        // Single atomic fetch with pessimistic lock - prevents race condition
        // Second concurrent request will block until this transaction completes
        RefreshTokens refreshToken = refreshtokenRepository
                .findByIdAndIpAddressForUpdate(refreshTokenCookieTextObj.refreshTokenId(), ipAddress)
                .orElseThrow(() -> {
                    log.error("Invalid refresh token");
                    return new BusinessException(ErrorCode.TOKEN_INVALID, ErrorMessages.INVALID_REFRESH_TOKEN);
                });

        // Check token states AFTER acquiring lock
        if (refreshToken.isConsumed()) {
            log.warn("Token reuse detected! FamilyId: {}, UserId: {}",
                    refreshToken.getFamilyId(),
                    refreshToken.getUser().getId());

            // Revoke all OTHER tokens in family (excludes current to avoid deadlock)
            // The REQUIRES_NEW transaction would block on our locked row otherwise
            refreshTokenRevocationService.revokeTokenFamilyExcluding(
                    refreshToken.getFamilyId(),
                    refreshToken.getId());

            // Mark current token as revoked in this transaction (we hold the lock)
            refreshToken.setRevoked(true);
            refreshtokenRepository.save(refreshToken);

            throw new BusinessException(ErrorCode.TOKEN_REUSE_DETECTED, ErrorMessages.TOKEN_REUSE_DETECTED);
        }

        if (refreshToken.isRevoked() || refreshToken.getExpiryDate().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, ErrorMessages.REFRESH_TOKEN_EXPIRED_OR_REVOKED);
        }

        if (!passwordEncoder.matches(refreshTokenCookieTextObj.rawRefreshToken(),
                refreshToken.getHashedRefreshToken())) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, ErrorMessages.REFRESH_TOKEN_VERIFICATION_FAILED);
        }

        Users user = refreshToken.getUser();

        // Mark token as consumed (rotation) - still inside the lock
        refreshToken.setConsumed(true);
        refreshtokenRepository.save(refreshToken);

        // Generate new access and refresh tokens
        generateAndStoreTokens(user, request, response, refreshToken.getFamilyId());

        return new RefreshResponse(ResponseMessages.TOKEN_REFRESHED);
    }

    /**
     * Logs out a user by revoking all their refresh tokens and clearing auth cookies.
     * <p>
     * Note: The access token remains valid until its expiry (typically 15 minutes)
     * since JWTs are stateless. However, cookies are cleared immediately and
     * refresh tokens are revoked, preventing session renewal.
     * </p>
     *
     * @param userId   the authenticated user's ID
     * @param response the HTTP response for clearing cookies
     * @return logout confirmation response
     */
    @Transactional
    public LogoutResponse logout(String userId, HttpServletResponse response) {
        UUID userUuid = UUID.fromString(userId);
        refreshtokenRepository.revokeAllRefreshTokensByUserId(userUuid);

        // Clear auth cookies
        clearAccessTokenCookie(response);
        clearRefreshTokenCookie(response);

        return new LogoutResponse(ResponseMessages.LOGOUT_SUCCESS);
    }

    private void storeAccessTokenIntoHeader(String accessToken, HttpServletResponse response) {
        ResponseCookie accessTokenCookie = ResponseCookie.from(AuthConstants.COOKIE_ACCESS_TOKEN, accessToken)
                .httpOnly(true)
                .secure(cookieSecureValue)
                .path(AuthConstants.COOKIE_PATH_ROOT)
                .maxAge(jwtTimeout / 1000)
                .sameSite(AuthConstants.COOKIE_SAME_SITE_STRICT)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
    }

    private void storeRefreshTokenIntoHeader(String refreshToken, HttpServletResponse response) {
        ResponseCookie refreshTokenCookie = ResponseCookie.from(AuthConstants.COOKIE_REFRESH_TOKEN, refreshToken)
                .httpOnly(true)
                .secure(cookieSecureValue)
                .path(AuthConstants.COOKIE_PATH_REFRESH)
                .maxAge(refreshTokenTimeout / 1000)
                .sameSite(AuthConstants.COOKIE_SAME_SITE_STRICT)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
    }

    private void clearAccessTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(AuthConstants.COOKIE_ACCESS_TOKEN, "")
                .httpOnly(true)
                .secure(cookieSecureValue)
                .path(AuthConstants.COOKIE_PATH_ROOT)
                .maxAge(0)
                .sameSite(AuthConstants.COOKIE_SAME_SITE_STRICT)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(AuthConstants.COOKIE_REFRESH_TOKEN, "")
                .httpOnly(true)
                .secure(cookieSecureValue)
                .path(AuthConstants.COOKIE_PATH_REFRESH)
                .maxAge(0)
                .sameSite(AuthConstants.COOKIE_SAME_SITE_STRICT)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private boolean checkIfUserEmailExist(String email) {
        return userRepository.findByEmail(email) != null;
    }

    private boolean checkIfUsernameExist(String username) {
        return userRepository.findByUsername(username) != null;
    }

    private RefreshTokenObj generateRefreshToken(HttpServletRequest request, Users user, UUID familyId) {
        String refreshToken = UUID.randomUUID().toString();
        String hashedRefreshToken = passwordEncoder.encode(refreshToken);
        String ipAddress = request.getRemoteAddr();
        Instant expiryDate = Instant.now().plus(refreshTokenTimeout, ChronoUnit.MILLIS);

        RefreshTokens newRefreshToken = RefreshTokens.builder()
                .hashedRefreshToken(hashedRefreshToken)
                .user(user)
                .ipAddress(ipAddress)
                .expiryDate(expiryDate)
                .familyId(familyId)
                .build();

        return new RefreshTokenObj(newRefreshToken, refreshToken);
    }

    private void generateAndStoreTokens(Users user, HttpServletRequest request, HttpServletResponse response,
                                        UUID familyId) {
        String accessToken = jwtService.generateToken(user);
        storeAccessTokenIntoHeader(accessToken, response);

        RefreshTokenObj refreshTokenObj = generateRefreshToken(request, user, familyId);
        RefreshTokens refreshTokenResponse = refreshtokenRepository.save(refreshTokenObj.entity());
        String refreshTokenCookieText = refreshTokenResponse.getId().toString() + AuthConstants.REFRESH_TOKEN_SEPARATOR + refreshTokenObj.refreshToken();
        storeRefreshTokenIntoHeader(refreshTokenCookieText, response);
    }

    private RefreshTokenCookieText splitRefreshTokenCookieText(String refreshTokenCookieText) {
        String[] parts = refreshTokenCookieText.split(AuthConstants.REFRESH_TOKEN_SEPARATOR, 2);
        if (parts.length != 2) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, ErrorMessages.INVALID_REFRESH_TOKEN_FORMAT);
        }

        UUID refreshTokenId;
        try {
            refreshTokenId = UUID.fromString(parts[0]);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, ErrorMessages.INVALID_REFRESH_TOKEN_FORMAT);
        }

        return new RefreshTokenCookieText(refreshTokenId, parts[1]);
    }
}
