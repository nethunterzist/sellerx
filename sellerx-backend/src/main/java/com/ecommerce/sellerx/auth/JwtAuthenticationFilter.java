package com.ecommerce.sellerx.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@AllArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String token = null;
        String tokenSource = null;

        var authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.replace("Bearer ", "");
            tokenSource = "header";
        } else if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    tokenSource = "cookie";
                    break;
                }
            }
        }

        if (token == null) {
            log.debug("[AUTH] No token found for: {}", uri);
            filterChain.doFilter(request, response);
            return;
        }

        log.debug("[AUTH] Validating token from {} for: {}", tokenSource, uri);

        var jwt = jwtService.parseToken(token);
        if (jwt == null) {
            log.warn("[AUTH] Invalid token (parse failed) for: {}", uri);
            filterChain.doFilter(request, response);
            return;
        }

        if (jwt.isExpired()) {
            log.warn("[AUTH] Token expired for user {} on: {}", jwt.getUserId(), uri);
            filterChain.doFilter(request, response);
            return;
        }

        var authentication = new UsernamePasswordAuthenticationToken(
                jwt.getUserId(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + jwt.getRole()))
        );
        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("[AUTH] Authenticated user {} (role: {}) for: {}", jwt.getUserId(), jwt.getRole(), uri);

        filterChain.doFilter(request, response);
    }
}
