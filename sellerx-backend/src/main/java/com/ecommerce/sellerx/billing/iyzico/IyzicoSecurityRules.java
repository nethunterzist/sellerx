package com.ecommerce.sellerx.billing.iyzico;

import com.ecommerce.sellerx.common.SecurityRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * Security rules for iyzico billing endpoints.
 * Webhook endpoints must be publicly accessible for iyzico to call them.
 */
@Component
public class IyzicoSecurityRules implements SecurityRules {

    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                // iyzico webhook endpoints - must be public for iyzico callbacks
                .requestMatchers(HttpMethod.POST, "/api/webhook/iyzico/**").permitAll()
                // iyzico 3DS callback - public for payment completion
                .requestMatchers(HttpMethod.POST, "/api/billing/checkout/callback").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/billing/checkout/callback").permitAll()
                // Public pricing page endpoint
                .requestMatchers(HttpMethod.GET, "/api/billing/plans").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/billing/plans/**").permitAll();
    }
}
