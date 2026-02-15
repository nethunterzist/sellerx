package com.ecommerce.sellerx.websocket;

import com.ecommerce.sellerx.common.SecurityRules;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * Security rules for WebSocket endpoints.
 * WebSocket endpoints are permitted without authentication at HTTP level
 * because authentication happens at STOMP level via WebSocketAuthInterceptor.
 */
@Component
@Order(5)  // Before default rules
public class WebSocketSecurityRules implements SecurityRules {

    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                // WebSocket endpoints - auth happens at STOMP level
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/ws").permitAll();
    }
}
