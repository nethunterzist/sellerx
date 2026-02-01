package com.ecommerce.sellerx.orders;

import com.ecommerce.sellerx.common.SecurityRules;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * TEMPORARY - DELETE AFTER TESTING!
 * Security rules for Trendyol API limit test endpoints.
 */
@Component
public class TestSecurityRules implements SecurityRules {
    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                .requestMatchers("/api/test/trendyol-limits/**").permitAll();
    }
}
