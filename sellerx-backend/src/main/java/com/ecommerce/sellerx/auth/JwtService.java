package com.ecommerce.sellerx.auth;

import com.ecommerce.sellerx.common.exception.JwtAuthenticationException;
import com.ecommerce.sellerx.users.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Date;

@AllArgsConstructor
@Service
public class JwtService {
    private final JwtConfig jwtConfig;

    public Jwt generateAccessToken(User user) {
        return generateToken(user, jwtConfig.getAccessTokenExpiration());
    }

    public Jwt generateRefreshToken(User user) {
        return generateToken(user, jwtConfig.getRefreshTokenExpiration());
    }

    private Jwt generateToken(User user, long tokenExpiration) {
        var claims = Jwts.claims()
                .subject(user.getId().toString())
                .add("email", user.getEmail())
                .add("name", user.getName())
                .add("role", user.getRole())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * tokenExpiration))
                .build();

        return new Jwt(claims, jwtConfig.getSecretKey());
    }

    public Jwt generateImpersonationToken(User targetUser, Long adminUserId) {
        var claims = Jwts.claims()
                .subject(targetUser.getId().toString())
                .add("email", targetUser.getEmail())
                .add("name", targetUser.getName())
                .add("role", targetUser.getRole())
                .add("impersonatedBy", adminUserId)
                .add("readOnly", true)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * jwtConfig.getAccessTokenExpiration()))
                .build();

        return new Jwt(claims, jwtConfig.getSecretKey());
    }

    public Jwt parseToken(String token) {
        try {
            var claims = getClaims(token);
            return new Jwt(claims, jwtConfig.getSecretKey());
        } catch (JwtException e) {
            return null;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(jwtConfig.getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }    public Long getUserIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw JwtAuthenticationException.tokenNotFound();
        }

        String token = authHeader.substring(7);
        Jwt jwt = parseToken(token);
        if (jwt == null) {
            throw JwtAuthenticationException.invalidToken();
        }

        return jwt.getUserId();
    }
}

