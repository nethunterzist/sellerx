package com.ecommerce.sellerx.billing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Paraşüt e-invoice integration configuration
 */
@Configuration
@ConfigurationProperties(prefix = "parasut")
@Data
public class ParasutConfig {

    /**
     * Enable Paraşüt integration
     */
    private boolean enabled = false;

    /**
     * OAuth2 Client ID
     */
    private String clientId;

    /**
     * OAuth2 Client Secret
     */
    private String clientSecret;

    /**
     * Company ID in Paraşüt
     */
    private String companyId;

    /**
     * Paraşüt API base URL
     */
    private String baseUrl = "https://api.parasut.com";

    /**
     * OAuth2 redirect URI
     */
    private String redirectUri;

    /**
     * Check if integration is properly configured
     */
    public boolean isConfigured() {
        return enabled &&
                clientId != null && !clientId.isBlank() &&
                clientSecret != null && !clientSecret.isBlank() &&
                companyId != null && !companyId.isBlank();
    }

    /**
     * Get OAuth2 token endpoint
     */
    public String getTokenEndpoint() {
        return baseUrl + "/oauth/token";
    }

    /**
     * Get API v4 base URL
     */
    public String getApiBaseUrl() {
        return baseUrl + "/v4/" + companyId;
    }
}
