# Security Improvements Roadmap

This document outlines recommended improvements for the ApexPay authentication system.

---

## Current Implementation Status

### ✅ Completed

- JWT-based stateless authentication
- BCrypt password hashing (strength 12)
- Stateless session management
- Public/protected endpoint configuration
- JWT token generation and validation
- Custom UserDetailsService implementation

---

## Recommended Improvements

### 1. Add `shouldNotFilter()` to JwtFilter

**Priority:** Medium  
**Effort:** Low

Skip JWT validation for public endpoints to improve performance.

```java
// In JwtFilter.java
@Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith("/api/v1/auth/") || path.equals("/error");
}
```

**Why:** Currently, the JWT filter runs on every request including public endpoints. This adds unnecessary processing overhead.

---

### 2. Add CORS Configuration

**Priority:** High (if frontend is on different domain)  
**Effort:** Low

```java
// In SecurityConfig.java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of("http://localhost:3000")); // Your frontend URL
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}

// Then add to securityFilterChain:
http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
```

**Why:** Required when your frontend (React/Vue) runs on a different origin than your API.

---

### 3. Enhanced JWT Claims

**Priority:** Low  
**Effort:** Low

Add issuer, audience, and custom claims for better security and flexibility.

```java
// In JwtService.java
public String generateToken(String username, Collection<? extends GrantedAuthority> authorities) {
    List<String> roles = authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .toList();

    return Jwts.builder()
            .claims()
            .subject(username)
            .issuer("apexpay-user-service")
            .audience().add("apexpay-services").and()
            .add("roles", roles)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + jwtTimeout))
            .and()
            .signWith(getKey())
            .compact();
}
```

**Why:**

- `issuer` identifies which service created the token
- `audience` specifies intended recipients
- `roles` allows stateless authorization without database lookup

---

### 4. Implement Refresh Token Mechanism

**Priority:** Medium  
**Effort:** High

Implement refresh tokens for better security with short-lived access tokens.

#### Recommended Flow:

```
1. Login → Returns access_token (15 min) + refresh_token (7 days)
2. Access token expires → Call /api/v1/auth/refresh with refresh_token
3. Server validates refresh_token → Returns new access_token
4. Logout → Invalidate refresh_token in database
```

#### Implementation Steps:

1. Create `RefreshToken` entity (you already have `RefreshTokens`)
2. Add `/api/v1/auth/refresh` endpoint
3. Add `/api/v1/auth/logout` endpoint
4. Store refresh tokens in database with expiration
5. Implement token rotation (new refresh token on each refresh)

#### Database Schema:

```sql
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN DEFAULT FALSE
);
```

**Why:** Short-lived access tokens reduce the window of attack if a token is compromised. Refresh tokens can be revoked on logout or security events.

---

### 5. Global Exception Handler for Auth Errors

**Priority:** Medium  
**Effort:** Low

Create consistent error responses for authentication failures.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleExpiredJwt(ExpiredJwtException e) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("TOKEN_EXPIRED", "Your session has expired. Please login again."));
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtException(JwtException e) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("INVALID_TOKEN", "Invalid authentication token."));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UsernameNotFoundException e) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("USER_NOT_FOUND", "Invalid credentials."));
    }
}

public record ErrorResponse(String code, String message) {}
```

**Why:** Provides consistent, structured error responses that frontends can handle programmatically.

---

### 6. Role-Based Access Control (RBAC)

**Priority:** Low (implement when needed)  
**Effort:** Medium

#### Step 1: Add role field to Users entity

```java
@Entity
@Table(name = "users")
public class Users {
    // ... existing fields

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;
}

public enum Role {
    USER,
    ADMIN,
    MODERATOR
}
```

#### Step 2: Update UserPrincipal

```java
@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
}
```

#### Step 3: Update database

```sql
ALTER TABLE users ADD COLUMN role VARCHAR(20) DEFAULT 'USER';
```

**Why:** Enables the `hasRole("ADMIN")` check in SecurityConfig to actually work.

---

### 7. Rate Limiting for Auth Endpoints

**Priority:** High  
**Effort:** Medium

Prevent brute-force attacks on login endpoint.

#### Option A: Using Bucket4j

```xml
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.7.0</version>
</dependency>
```

#### Option B: Using Spring Cloud Gateway (at API Gateway level)

Configure rate limiting in your `api-gateway` service.

**Recommended Limits:**

- Login: 5 attempts per minute per IP
- Register: 3 attempts per minute per IP
- Refresh token: 10 attempts per minute per user

---

### 8. Account Security Features

**Priority:** Medium  
**Effort:** High

Consider implementing:

1. **Account Lockout**: Lock account after 5 failed login attempts
2. **Password Requirements**: Enforce minimum length, complexity
3. **Email Verification**: Verify email before allowing login
4. **Password Reset**: Secure password reset flow with time-limited tokens
5. **Login History**: Track login attempts with IP and user agent

---

### 9. Security Headers

**Priority:** Medium  
**Effort:** Low

Add security headers to responses:

```java
// In SecurityConfig.java
http.headers(headers -> headers
    .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
    .frameOptions(frame -> frame.deny())
    .xssProtection(xss -> xss.disable()) // Modern browsers handle this
);
```

---

## Implementation Priority Order

| Priority | Item                                    | Effort |
| -------- | --------------------------------------- | ------ |
| 1        | CORS Configuration (if frontend exists) | Low    |
| 2        | Rate Limiting                           | Medium |
| 3        | shouldNotFilter() in JwtFilter          | Low    |
| 4        | Global Exception Handler                | Low    |
| 5        | Refresh Token Mechanism                 | High   |
| 6        | Enhanced JWT Claims                     | Low    |
| 7        | Role-Based Access Control               | Medium |
| 8        | Account Security Features               | High   |
| 9        | Security Headers                        | Low    |

---

## Security Checklist Before Production

- [ ] JWT secret key is at least 256 bits and stored securely
- [ ] JWT timeout is appropriately short (15-30 minutes for access tokens)
- [ ] HTTPS is enforced in production
- [ ] CORS is configured for specific origins (not `*`)
- [ ] Rate limiting is enabled on auth endpoints
- [ ] Sensitive data is not logged
- [ ] Error messages don't leak implementation details
- [ ] Database credentials are in environment variables
- [ ] Refresh token rotation is implemented
- [ ] Account lockout is implemented

---

## Resources

- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [JWT Best Practices](https://auth0.com/blog/a-look-at-the-latest-draft-for-jwt-bcp/)
