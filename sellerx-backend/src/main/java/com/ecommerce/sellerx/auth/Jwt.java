package com.ecommerce.sellerx.auth;

import com.ecommerce.sellerx.users.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import java.util.Date;

public class Jwt {
    private final Claims claims;
    private final SecretKey secretKey;

    public Jwt(Claims claims, SecretKey secretKey) {
        this.claims = claims;
        this.secretKey = secretKey;
    }

    public boolean isExpired() {
        return claims.getExpiration().before(new Date());
    }

    public Long getUserId() {
        return Long.valueOf(claims.getSubject());
    }

    public Role getRole() {
        return Role.valueOf(claims.get("role", String.class));
    }

    public boolean isImpersonated() {
        return claims.get("impersonatedBy") != null;
    }

    public Long getImpersonatedBy() {
        Object val = claims.get("impersonatedBy");
        return val != null ? Long.valueOf(val.toString()) : null;
    }

    public boolean isReadOnly() {
        Boolean readOnly = claims.get("readOnly", Boolean.class);
        return Boolean.TRUE.equals(readOnly);
    }

    public String toString() {
        return Jwts.builder().claims(claims).signWith(secretKey).compact();
    }
}
