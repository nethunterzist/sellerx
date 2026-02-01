package com.ecommerce.sellerx.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class LoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // Skip logging for actuator endpoints to reduce noise
        String uri = request.getRequestURI();
        if (uri.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Set MDC context for request tracing
        MDC.put("requestId", UUID.randomUUID().toString());

        long startTime = System.currentTimeMillis();
        String method = request.getMethod();
        String remoteAddr = request.getRemoteAddr();
        String queryString = request.getQueryString();

        log.info("[REQUEST] {} {} from {}{}",
            method,
            uri,
            remoteAddr,
            queryString != null ? " ?" + queryString : "");

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Extract userId from SecurityContext (populated by JwtAuthenticationFilter)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() != null
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                MDC.put("userId", authentication.getPrincipal().toString());
            }

            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();

            // Use different log levels based on status code
            if (status >= 500) {
                log.error("[RESPONSE] {} {} - {} ({}ms)", method, uri, status, duration);
            } else if (status >= 400) {
                log.warn("[RESPONSE] {} {} - {} ({}ms)", method, uri, status, duration);
            } else {
                log.info("[RESPONSE] {} {} - {} ({}ms)", method, uri, status, duration);
            }

            // Clear MDC to prevent context leaking between requests
            MDC.clear();
        }
    }
}
