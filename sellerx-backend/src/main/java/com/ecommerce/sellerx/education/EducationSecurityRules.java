package com.ecommerce.sellerx.education;

import com.ecommerce.sellerx.common.SecurityRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class EducationSecurityRules implements SecurityRules {
    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        // Public endpoints - video listesi ve detayları
        registry
                .requestMatchers(HttpMethod.GET, "/api/education/videos").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/education/videos/**").permitAll();
    }
}
