package com.apexpay.userservice.security;

import com.apexpay.userservice.service.MyUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

/**
 * JWT authentication filter that intercepts requests to validate access tokens.
 * Extracts JWT from cookies (web) or Authorization header (mobile) and sets
 * the security context if the token is valid.
 */
@Slf4j
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final MyUserDetailsService myUserDetailsService;

    public JwtFilter(JwtService jwtService,
            MyUserDetailsService myUserDetailsService) {
        this.jwtService = jwtService;
        this.myUserDetailsService = myUserDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            // Try to get token from cookie first (web clients)
            String token = getCookieValue(request, "access_token");

            // Fallback: check Authorization header (for mobile apps)
            if (token == null) {
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            }

            if (token == null) {
                filterChain.doFilter(request, response);
                return;
            }

            String email = jwtService.extractEmail(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = myUserDetailsService.loadUserByUsername(email);

                if (jwtService.validateToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
                            null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Spring Security 6+ with STATELESS sessions requires creating a NEW
                    // SecurityContext
                    // rather than modifying the existing one via
                    // SecurityContextHolder.getContext().
                    //
                    // Why this pattern is necessary:
                    // 1. Thread Safety: Creates an isolated context, avoiding race conditions in
                    // multithreaded environments where multiple requests share thread pools.
                    // 2. Clean State: Ensures no stale authentication data from previous requests
                    // pollutes the current request's security context.
                    // 3. Spring Security 6 Best Practice: The official recommendation for stateless
                    // applications where SecurityContextHolderFilter doesn't persist context.
                    //
                    // Old pattern (Spring Security 5):
                    // SecurityContextHolder.getContext().setAuthentication(authToken);
                    // New pattern (Spring Security 6+):
                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(authToken);
                    SecurityContextHolder.setContext(context);
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    public String getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null)
            return null;

        return Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
