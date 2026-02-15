package com.ecommerce.sellerx.websocket;

import com.ecommerce.sellerx.auth.Jwt;
import com.ecommerce.sellerx.auth.JwtService;
import com.ecommerce.sellerx.users.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * WebSocket channel interceptor for JWT authentication.
 * Validates JWT token on CONNECT and sets user principal.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Try to get token from Authorization header
            String token = extractToken(accessor);

            if (token != null) {
                try {
                    Jwt jwt = jwtService.parseToken(token);
                    if (jwt != null && !jwt.isExpired()) {
                        Long userId = jwt.getUserId();

                        // Create authentication with user ID as principal
                        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + jwt.getRole().name()));
                        var auth = new UsernamePasswordAuthenticationToken(
                                userId.toString(),
                                null,
                                authorities
                        );

                        accessor.setUser(auth);
                        log.debug("[WS-AUTH] User {} connected via WebSocket", userId);
                    } else {
                        log.warn("[WS-AUTH] Invalid or expired token on WebSocket connect");
                    }
                } catch (Exception e) {
                    log.error("[WS-AUTH] Failed to authenticate WebSocket connection: {}", e.getMessage());
                }
            } else {
                log.debug("[WS-AUTH] No token provided on WebSocket connect");
            }
        }

        return message;
    }

    /**
     * Extract JWT token from STOMP headers.
     * Supports multiple header formats:
     * - Authorization: Bearer <token>
     * - token: <token>
     */
    private String extractToken(StompHeaderAccessor accessor) {
        // Try Authorization header first
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Try custom token header
        String tokenHeader = accessor.getFirstNativeHeader("token");
        if (tokenHeader != null && !tokenHeader.isEmpty()) {
            return tokenHeader;
        }

        // Try X-Auth-Token header
        String xAuthToken = accessor.getFirstNativeHeader("X-Auth-Token");
        if (xAuthToken != null && !xAuthToken.isEmpty()) {
            return xAuthToken;
        }

        return null;
    }
}
