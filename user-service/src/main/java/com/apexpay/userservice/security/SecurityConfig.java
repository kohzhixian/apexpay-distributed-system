package com.apexpay.userservice.security;

import com.apexpay.userservice.service.MyUserDetailsService;
import lombok.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

/**
 * Spring Security configuration for the user service.
 * Configures stateless JWT-based authentication with CSRF disabled.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final MyUserDetailsService myUserDetailsService;
    private final JwtFilter jwtFilter;

    public SecurityConfig(
            MyUserDetailsService myUserDetailsService,
            JwtFilter jwtFilter) {
        this.myUserDetailsService = myUserDetailsService;
        this.jwtFilter = jwtFilter;
    }

    /**
     * Configures the security filter chain with JWT authentication.
     * Public endpoints: /api/v1/auth/**, /error
     * Admin endpoints: /api/admin/** (requires ADMIN role)
     * All other endpoints require authentication.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF for stateless REST APIs
                .csrf(csrf -> csrf.disable())

                // Set Session Management to STATELESS
                // Ensures Spring doesn't create a JSESSIONID cookie
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .authenticationProvider(authenticationProvider())
                // Configure Authorization Rules
                .authorizeHttpRequests(auth -> auth
                        // public endpoints (Login, Register)
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/error")
                        .permitAll()
                        // Role-based access
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // All other requests require authentication
                        .anyRequest().authenticated());
        return http.build();
    }

    // create our own AuthenticationProvider
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(myUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Prevents Spring Boot from auto-registering JwtFilter as a servlet filter.
     * Why this is necessary:
     * When a filter class is annotated with @Component, Spring Boot automatically
     * registers
     * it as a servlet filter that runs BEFORE the Spring Security filter chain.
     * This causes
     * the JwtFilter to execute twice:
     * 1. First as a servlet filter (outside security chain) - sets authentication
     * 2. Security chain starts - SecurityContextHolderFilter initializes fresh
     * context
     * 3. JwtFilter runs again inside security chain - but context was already reset
     * 4. AnonymousAuthenticationFilter sees no auth - marks request as anonymous →
     * 403 Forbidden
     * By setting enabled=false, we tell Spring Boot: "Don't auto-register this
     * filter,
     * I'm managing it myself via addFilterBefore() in the security chain."
     * Result: JwtFilter runs ONLY inside the Spring Security filter chain, in the
     * correct
     * position (before UsernamePasswordAuthenticationFilter), and authentication
     * persists.
     */
    @Bean
    public FilterRegistrationBean<@NonNull JwtFilter> jwtFilterRegistration(JwtFilter filter) {
        FilterRegistrationBean<@NonNull JwtFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
