package com.apexpay.userservice.security;

import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {
    @Value("${apexpay.jwt-secret-key")
    private String JWT_SECRET_KEY;

    @Value("${apexpay.jwt-timeout}")
    private String JWT_TIMEOUT;

    public String extractUsername(String token){
        return extractClaim(token, Claims:: getSubject);
    }

    public <T> T extractClaim(String token, Functrion<Claims, T> claimsResolver){
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails){
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails){
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject()
    }
}
