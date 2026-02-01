package com.ecommerce.sellerx.referral;

import com.ecommerce.sellerx.common.SecurityRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * Security rules for referral endpoints.
 * Validate endpoint is public (used during registration).
 * All other referral endpoints require authentication.
 */
@Component
public class ReferralSecurityRules implements SecurityRules {

    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers(HttpMethod.GET, "/api/referrals/validate/**").permitAll();
    }
}
