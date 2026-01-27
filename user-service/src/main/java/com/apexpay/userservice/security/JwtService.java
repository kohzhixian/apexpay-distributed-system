package com.apexpay.userservice.security;

import com.apexpay.userservice.constants.AuthConstants;
import com.apexpay.userservice.entity.Users;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

/**
 * Service for JWT token generation.
 * Uses RS256 (RSA + SHA-256) asymmetric signing for enhanced security.
 * Token validation is handled by the API Gateway.
 */
@Slf4j
@Service
public class JwtService {

    private final PrivateKey privateKey;
    private final long jwtTimeout;

    public JwtService(
            @Value("${apexpay.jwt.private-key}") Resource privateKeyResource,
            @Value("${apexpay.jwt-timeout}") long jwtTimeout) {
        this.privateKey = loadPrivateKey(privateKeyResource);
        this.jwtTimeout = jwtTimeout;
    }

    /**
     * Generates a JWT access token for the given user.
     * Token includes email, username as claims and user ID as subject.
     * Signed with RS256 (RSA private key).
     */
    public String generateToken(Users user) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put(AuthConstants.JWT_CLAIM_EMAIL, user.getEmail());
        extraClaims.put(AuthConstants.JWT_CLAIM_USERNAME, user.getUsername());

        return Jwts.builder()
                .claims()
                .add(extraClaims)
                .subject(user.getId().toString())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtTimeout))
                .issuer(AuthConstants.JWT_ISSUER)
                .audience().add(AuthConstants.JWT_AUDIENCE).and()
                .id(UUID.randomUUID().toString())
                .and()
                .signWith(privateKey)
                .compact();
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

            if (pem.contains(AuthConstants.PKCS1_PRIVATE_KEY_HEADER)) {
                throw new IllegalStateException(
                        "PKCS#1 format (" + AuthConstants.PKCS1_PRIVATE_KEY_HEADER + ") is not supported. " +
                                "Please convert to PKCS#8 using: openssl pkcs8 -topk8 -nocrypt -in key.pem -out key-pkcs8.pem");
            }

            if (!pem.contains(AuthConstants.PKCS8_PRIVATE_KEY_HEADER)) {
                throw new IllegalStateException(
                        "Invalid private key format. Expected PKCS#8 (" + AuthConstants.PKCS8_PRIVATE_KEY_HEADER + ")");
            }

            String base64 = pem
                    .replace(AuthConstants.PKCS8_PRIVATE_KEY_HEADER, "")
                    .replace(AuthConstants.PKCS8_PRIVATE_KEY_FOOTER, "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(base64);
            KeyFactory keyFactory = KeyFactory.getInstance(AuthConstants.KEY_ALGORITHM_RSA);
            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Failed to load private key", e);
        }
    }
}
