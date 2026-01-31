# Security Improvements Roadmap

This document outlines recommended improvements for the ApexPay authentication system.

---

## Current Implementation Status

### ✅ Completed

- JWT-based stateless authentication with RS256 signing
- BCrypt password hashing (strength 12)
- Stateless session management
- Public/protected endpoint configuration
- JWT token generation with issuer, audience, and JTI claims
- Custom UserDetailsService implementation
- CORS configuration (in api-gateway)
- Refresh token mechanism with token rotation and family-based revocation
- Global exception handler with standardized error responses
- Logout with cookie clearing and token revocation

---

## Recommended Improvements

### 1. Add Roles to JWT Claims

**Priority:** Low  
**Effort:** Low

Currently, JWT includes issuer and audience but not user roles. Adding roles enables stateless authorization.

```java
// In JwtService.java - add roles claim
extraClaims.put("roles", List.of("ROLE_USER")); // or from user entity
```

**Why:** Allows stateless authorization without database lookup at the gateway.

---

### 2. Role-Based Access Control (RBAC)

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

### 3. Rate Limiting for Auth Endpoints

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

### 4. Immediate Access Token Invalidation on Logout

**Priority:** Medium  
**Effort:** Medium-High

Currently, access tokens remain valid until their natural expiry (~15 minutes) even after logout. This is a fundamental limitation of stateless JWTs. Options to address this:

#### Option A: Token Blacklist with Redis (Recommended for Production)

Add a blacklist cache that the gateway checks on every request.

**Implementation:**

1. Add Redis dependency to api-gateway
2. On logout, add access token to Redis with TTL = remaining token lifetime
3. Gateway checks blacklist before forwarding requests

```java
// In api-gateway GatewayJwtFilter
@Autowired
private RedisTemplate<String, String> redisTemplate;

private boolean isTokenBlacklisted(String token) {
    return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token));
}

// In user-service logout
public void blacklistAccessToken(String accessToken, long remainingTtlSeconds) {
    redisTemplate.opsForValue().set(
        "blacklist:" + accessToken, 
        "revoked", 
        remainingTtlSeconds, 
        TimeUnit.SECONDS
    );
}
```

**Pros:** Immediate invalidation, industry standard  
**Cons:** Requires Redis, adds ~1-2ms latency per request

#### Option B: Reduce Token Lifetime

Reduce `JWT_TIMEOUT` from 15 minutes to 5 minutes.

```yaml
# In user-service application.yaml
JWT_TIMEOUT=300000  # 5 minutes instead of 15
```

**Pros:** Simple, no infrastructure changes  
**Cons:** More frequent refresh calls

#### Option C: Token Version Per User

Store a `tokenVersion` in the user record. Include it in JWT. On logout, increment the version.

```java
// In Users entity
@Column(nullable = false)
private Long tokenVersion = 0L;

// On logout
user.setTokenVersion(user.getTokenVersion() + 1);

// In JWT claims
.claim("tokenVersion", user.getTokenVersion())

// Gateway validation
if (!tokenVersion.equals(userService.getTokenVersion(userId))) {
    throw new UnauthorizedException("Token invalidated");
}
```

**Pros:** No Redis needed  
**Cons:** Gateway needs to call user-service (defeats stateless JWT benefit)

#### Option D: Accept the Limitation (Current)

Most systems accept this trade-off:
- 15-minute window is acceptable for most use cases
- Cookies are cleared immediately (user appears logged out)
- Refresh tokens are revoked (can't get new access tokens)

**Recommendation:**
- Learning/MVP: Option D
- Medium security: Option B (reduce to 5 min)
- Production/High security: Option A (Redis blacklist)

---

### 5. Account Security Features

**Priority:** Medium  
**Effort:** High

Consider implementing:

1. **Account Lockout**: Lock account after 5 failed login attempts
2. **Password Requirements**: Enforce minimum length, complexity
3. **Email Verification**: Verify email before allowing login
4. **Password Reset**: Secure password reset flow with time-limited tokens
5. **Login History**: Track login attempts with IP and user agent

---

### 6. Security Headers

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

| Priority | Item                                       | Effort |
| -------- | ------------------------------------------ | ------ |
| 1        | Rate Limiting                              | Medium |
| 2        | Add Roles to JWT Claims                    | Low    |
| 3        | Role-Based Access Control                  | Medium |
| 4        | Access Token Invalidation (Redis blacklist)| Medium |
| 5        | Account Security Features                  | High   |
| 6        | Security Headers                           | Low    |

---

## Security Checklist Before Production

- [x] JWT uses RS256 asymmetric signing (private/public key pair)
- [x] JWT timeout is appropriately short (15 minutes for access tokens)
- [x] CORS is configured for specific origins (not `*`)
- [x] Refresh token rotation is implemented
- [x] Refresh tokens are hashed before storage
- [x] Token family tracking for cascade revocation
- [x] Error messages don't leak implementation details
- [x] Database credentials are in environment variables
- [ ] HTTPS is enforced in production
- [ ] Rate limiting is enabled on auth endpoints
- [ ] Account lockout is implemented
- [ ] Sensitive data is not logged (audit logging)

---

## Resources

- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [JWT Best Practices](https://auth0.com/blog/a-look-at-the-latest-draft-for-jwt-bcp/)
