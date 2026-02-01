package com.ecommerce.sellerx.billing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * iyzico payment gateway configuration
 */
@Configuration
@ConfigurationProperties(prefix = "iyzico")
@Data
public class IyzicoConfig {

    /**
     * iyzico API key
     */
    private String apiKey;

    /**
     * iyzico Secret key
     */
    private String secretKey;

    /**
     * Use sandbox environment (true for development)
     */
    private boolean sandbox = true;

    /**
     * iyzico API base URL
     */
    private String baseUrl = "https://sandbox-api.iyzipay.com";

    /**
     * Webhook callback URL
     */
    private String callbackUrl;

    /**
     * Get the appropriate base URL based on environment
     */
    public String getEffectiveBaseUrl() {
        if (sandbox) {
            return "https://sandbox-api.iyzipay.com";
        }
        return "https://api.iyzipay.com";
    }
}
