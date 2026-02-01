package com.ecommerce.sellerx.billing.config;

import com.ecommerce.sellerx.common.SecurityRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * Security rules for billing endpoints.
 * Plans endpoint is public for pricing page display.
 */
@Component
public class BillingSecurityRules implements SecurityRules {

    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                // Plans endpoint - public for pricing page
                .requestMatchers(HttpMethod.GET, "/api/billing/plans").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/billing/plans/**").permitAll();
    }
}
