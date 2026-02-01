package com.ecommerce.sellerx.email;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for SendGrid email service.
 * Properties are loaded from application.yaml under the 'sendgrid' prefix.
 */
@Configuration
@ConfigurationProperties(prefix = "sendgrid")
@Data
public class EmailConfig {

    /**
     * Whether email sending is enabled.
     * When false, emails are only logged but not actually sent.
     */
    private boolean enabled = false;

    /**
     * SendGrid API key for authentication.
     */
    private String apiKey;

    /**
     * Email address to use as the sender (From field).
     */
    private String fromEmail = "noreply@sellerx.com";

    /**
     * Display name to use as the sender name.
     */
    private String fromName = "SellerX";
}
