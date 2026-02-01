package com.ecommerce.sellerx.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates that all required environment variables are set when running in production.
 * Fails fast at startup if critical configuration is missing.
 */
@Configuration
@Profile("production")
@Slf4j
public class ProductionConfigValidator {

    @Value("${spring.jwt.secret:}")
    private String jwtSecret;

    @Value("${cors.allowed-origins:}")
    private String corsAllowedOrigins;

    @Value("${spring.datasource.url:}")
    private String dbUrl;

    @Value("${spring.datasource.username:}")
    private String dbUsername;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${app.webhook.base-url:}")
    private String webhookBaseUrl;

    @PostConstruct
    public void validate() {
        List<String> missing = new ArrayList<>();

        if (isBlank(jwtSecret)) {
            missing.add("JWT_SECRET");
        } else if (jwtSecret.length() < 32) {
            missing.add("JWT_SECRET (must be at least 32 characters)");
        }

        if (isBlank(corsAllowedOrigins)) {
            missing.add("CORS_ALLOWED_ORIGINS");
        } else if (corsAllowedOrigins.contains("localhost")) {
            log.warn("CORS_ALLOWED_ORIGINS contains 'localhost' — this is unusual for production");
        }

        if (isBlank(dbUrl) || dbUrl.contains("localhost")) {
            log.warn("DB URL points to localhost — verify this is intentional for production");
        }

        if (isBlank(dbPassword)) {
            missing.add("DB_PASSWORD");
        }

        if (isBlank(webhookBaseUrl) || webhookBaseUrl.contains("localhost")) {
            log.warn("WEBHOOK_BASE_URL is empty or points to localhost");
        }

        if (!missing.isEmpty()) {
            String message = "Missing required production configuration: " + String.join(", ", missing);
            log.error(message);
            throw new IllegalStateException(message);
        }

        log.info("Production configuration validation passed");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
