package com.ecommerce.sellerx.webhook;

import com.ecommerce.sellerx.common.SecurityRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * Security rules for webhook endpoints.
 * Webhook receiver endpoint must be publicly accessible for Trendyol to call it.
 */
@Component
public class WebhookSecurityRules implements SecurityRules {

    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                // Webhook receiver endpoint - must be public for Trendyol
                .requestMatchers(HttpMethod.POST, "/api/webhook/trendyol/**").permitAll()
                // iyzico payment webhook - must be public for payment callbacks
                .requestMatchers(HttpMethod.POST, "/api/webhook/iyzico/**").permitAll()
                // Parasut e-invoice webhook - must be public for invoice callbacks
                .requestMatchers(HttpMethod.POST, "/api/webhook/parasut/**").permitAll()
                // Webhook health check - public
                .requestMatchers(HttpMethod.GET, "/api/webhook/health").permitAll();
    }
}
