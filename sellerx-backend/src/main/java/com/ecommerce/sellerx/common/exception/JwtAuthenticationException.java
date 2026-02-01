package com.ecommerce.sellerx.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when JWT authentication fails.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class JwtAuthenticationException extends RuntimeException {

    public JwtAuthenticationException(String message) {
        super(message);
    }

    public static JwtAuthenticationException tokenNotFound() {
        return new JwtAuthenticationException("JWT token not found in request");
    }

    public static JwtAuthenticationException invalidToken() {
        return new JwtAuthenticationException("Invalid JWT token");
    }
}
