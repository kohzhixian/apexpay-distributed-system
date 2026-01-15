package com.apexpay.userservice.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Utility class for HTTP cookie operations.
 */
@Component
public class CookieUtils {
    /**
     * Extracts a cookie value from the HTTP request by name.
     *
     * @param request the HTTP servlet request
     * @param cookieName the name of the cookie to retrieve
     * @return the cookie value, or null if not found
     */
    public String getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
