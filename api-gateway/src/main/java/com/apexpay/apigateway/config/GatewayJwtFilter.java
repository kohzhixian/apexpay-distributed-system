package com.apexpay.apigateway.config;

import com.apexpay.apigateway.exception.UnauthorizedException;
import com.apexpay.common.constants.HttpHeaders;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

/**
 * WebFlux filter that handles JWT authentication for the API Gateway.
 * Validates JWT tokens from cookies or Authorization headers, extracts user
 * claims,
 * and propagates user identity via X-User-* headers to downstream services.
 * Public endpoints bypass authentication; protected endpoints require valid
 * tokens.
 */
public class GatewayJwtFilter implements WebFilter {

    private static final List<String> PUBLIC_ENDPOINTS = List.of("/api/v1/auth/**", "/user-fallback",
            "/actuator/health");
    private static final Logger logger = LoggerFactory.getLogger(GatewayJwtFilter.class);
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final PublicKey publicKey;

    public GatewayJwtFilter(
            Resource publicKey) {
        this.publicKey = loadPublicKey(publicKey);
    }

    @Override
    @NullMarked
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        String path = exchange.getRequest().getPath().value();

        // Strip any incoming headers to prevent spoofing
        ServerHttpRequest sanitizedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(HttpHeaders.X_USER_ID);
                    headers.remove(HttpHeaders.X_USER_EMAIL);
                    headers.remove(HttpHeaders.X_USER_NAME);
                })
                .build();

        ServerWebExchange sanitizedExchange = exchange.mutate()
                .request(sanitizedRequest)
                .build();

        // Check if request path is public endpoint
        // If yes, skip validation
        if (isPublicEndpoint(path)) {
            logger.debug("Public endpoint accessed: {}", path);
            return chain.filter(sanitizedExchange);
        }

        // Extract token from cookie or Authorization Header
        String token = extractToken(sanitizedExchange);

        if (token == null || token.isBlank()) {
            logger.warn("No token found for protected endpoint: {}", path);
            return onUnauthorized("Missing authentication token");
        }

        // Check if token is valid
        // parseSignnedClaims() already checks for token expiration
        if (!isTokenValid(token)) {
            logger.warn("Invalid token for endpoint: {}", path);
            return onUnauthorized("Invalid or expired token.");
        }

        // Extract user details from token
        String userId = extractUserId(token);
        String email = extractEmail(token);
        String username = extractUsername(token);

        logger.debug("Authenticated user: {} ({})", username, email);

        // Add users details to headers of request
        ServerHttpRequest mutatedRequest = sanitizedExchange.getRequest().mutate()
                .header(HttpHeaders.X_USER_ID, userId)
                .header(HttpHeaders.X_USER_EMAIL, email)
                .header(HttpHeaders.X_USER_NAME, username)
                .build();

        ServerWebExchange mutatedExchange = sanitizedExchange.mutate()
                .request(mutatedRequest)
                .build();

        // continue filter chain with mutated exchange
        return chain.filter(mutatedExchange);
    }

    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(publicKey) // validate signature
                    .build()
                    .parseSignedClaims(token); // throw if invalid or expired
            return true;
        } catch (Exception e) {
            logger.debug("Token validation failed: {}", e.getMessage());
            return false;
        }

    }

    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    public String extractUsername(String token) {
        return extractClaim(token, claims -> claims.get("username", String.class));
    }

    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("sub", String.class));
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private PublicKey loadPublicKey(Resource resource) {
        // prevents file desciptor leaks for classpath resources and connection leaks
        try (InputStream inputStream = resource.getInputStream()) {
            String key = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String publicKeyPEM = key
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(publicKeyPEM);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(keySpec);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Failed to load public key", e);
        }
    }

    public String extractToken(ServerWebExchange exchange) {
        // Get token from cookie
        HttpCookie accessTokenCookie = exchange.getRequest().getCookies().getFirst("access_token");
        if (accessTokenCookie != null && !accessTokenCookie.getValue().isBlank()) {
            return accessTokenCookie.getValue();
        }

        // Fallback: check Authorization header
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }

    @NullMarked
    private Mono<Void> onUnauthorized(String message) {
        // Simply return an error, GlobalErrorWebExceptionHandler will handle it
        return Mono.error(new UnauthorizedException(message));
    }

}
