package com.apexpay.userservice.security;

import com.apexpay.userservice.entity.Users;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Service for JWT token generation, validation, and claim extraction.
 * Uses RS256 (RSA + SHA-256) asymmetric signing for enhanced security.
 */
@Slf4j
@Service
public class JwtService {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final long jwtTimeout;

    public JwtService(
            @Value("${apexpay.jwt.private-key}") Resource privateKeyResource,
            @Value("${apexpay.jwt.public-key}") Resource publicKeyResource,
            @Value("${apexpay.jwt-timeout}") long jwtTimeout) {
        this.privateKey = loadPrivateKey(privateKeyResource);
        this.publicKey = loadPublicKey(publicKeyResource);
        this.jwtTimeout = jwtTimeout;

        // Log key loading status on startup
        log.info("✅ JWT RS256 keys loaded successfully");
        log.info("   Private key algorithm: {}", privateKey.getAlgorithm());
        log.info("   Public key algorithm: {}", publicKey.getAlgorithm());
    }

    /**
     * Generates a JWT access token for the given user.
     * Token includes email, username as claims and user ID as subject.
     * Signed with RS256 (RSA private key).
     */
    public String generateToken(Users user) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("email", user.getEmail());
        extraClaims.put("username", user.getUsername());

        return Jwts.builder()
                .claims()
                .add(extraClaims)
                .subject(user.getId().toString())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtTimeout))
                .issuer("apexpay-user-service")
                .audience().add("apexpay-api").and()
                .id(UUID.randomUUID().toString())
                .and()
                .signWith(privateKey)
                .compact();
    }

    public String extractUserName(String token) {
        return extractClaim(token, claims -> claims.get("username", String.class));
    }

    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
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

    /**
     * Validates a JWT token against user details.
     * Checks if the token belongs to the user and is not expired.
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUserName(token);
        if (username == null) {
            return false;
        }
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Loads RSA private key from PEM file.
     * Only PKCS#8 format (-----BEGIN PRIVATE KEY-----) is supported.
     * To convert PKCS#1 to PKCS#8: openssl pkcs8 -topk8 -nocrypt -in key.pem -out
     * key-pkcs8.pem
     */
    private PrivateKey loadPrivateKey(Resource resource) {
        try {
            String pem = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            if (pem.contains("-----BEGIN RSA PRIVATE KEY-----")) {
                throw new IllegalStateException(
                        "PKCS#1 format (-----BEGIN RSA PRIVATE KEY-----) is not supported. " +
                                "Please convert to PKCS#8 using: openssl pkcs8 -topk8 -nocrypt -in key.pem -out key-pkcs8.pem");
            }

            if (!pem.contains("-----BEGIN PRIVATE KEY-----")) {
                throw new IllegalStateException(
                        "Invalid private key format. Expected PKCS#8 (-----BEGIN PRIVATE KEY-----)");
            }

            String base64 = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(base64);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Failed to load private key", e);
        }
    }

    /** Loads RSA public key from PEM file */
    private PublicKey loadPublicKey(Resource resource) {
        try {
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

    /** Returns public key for sharing with other services */
    public PublicKey getPublicKey() {
        return publicKey;
    }
}
