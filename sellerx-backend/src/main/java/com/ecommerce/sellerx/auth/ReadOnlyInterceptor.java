package com.ecommerce.sellerx.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Slf4j
@Component
public class ReadOnlyInterceptor implements HandlerInterceptor {

    private static final Set<String> ALLOWED_WRITE_PATHS = Set.of(
            "/auth/refresh",
            "/auth/me",
            "/auth/logout"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Boolean readOnly = (Boolean) request.getAttribute("readOnly");
        if (!Boolean.TRUE.equals(readOnly)) {
            return true;
        }

        String method = request.getMethod();
        if ("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)) {
            return true;
        }

        String uri = request.getRequestURI();
        if (ALLOWED_WRITE_PATHS.contains(uri)) {
            return true;
        }

        Long impersonatedBy = (Long) request.getAttribute("impersonatedBy");
        log.warn("[IMPERSONATION] Blocked {} {} - read-only session (admin: {})", method, uri, impersonatedBy);

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Read-only impersonation session\"}");
        return false;
    }
}
