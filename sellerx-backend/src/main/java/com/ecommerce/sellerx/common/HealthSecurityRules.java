package com.ecommerce.sellerx.common;

import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class HealthSecurityRules implements SecurityRules {
    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                .requestMatchers(HttpMethod.GET, "/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/").permitAll()
                // Health and info endpoints are public (used by load balancers and k8s probes)
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/actuator/info").permitAll()
                // Metrics and loggers require authentication (sensitive operational data)
                .requestMatchers("/actuator/metrics/**").authenticated()
                .requestMatchers("/actuator/loggers/**").authenticated();
    }
}
