package com.ecommerce.sellerx.config;

import com.ecommerce.sellerx.common.SecurityRules;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * Security rules for OpenAPI/Swagger documentation endpoints.
 * These endpoints are public to allow API documentation access.
 */
@Component
public class OpenAPISecurityRules implements SecurityRules {
    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                // Swagger UI
                .requestMatchers("/swagger-ui.html").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                // OpenAPI JSON/YAML specs
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/v3/api-docs.yaml").permitAll();
    }
}
