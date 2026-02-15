package com.ecommerce.sellerx.email;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for email service.
 * Properties are loaded from application.yaml under the 'email' prefix.
 */
@Configuration
@ConfigurationProperties(prefix = "email")
@Data
public class SmtpEmailConfig {

    /**
     * Email provider: "smtp" or "sendgrid"
     */
    private String provider = "smtp";

    /**
     * Whether email sending is enabled.
     * When false, emails are only logged but not actually sent.
     */
    private boolean enabled = false;

    /**
     * Email address to use as the sender (From field).
     */
    private String fromAddress = "noreply@sellerx.com";

    /**
     * Display name to use as the sender name.
     */
    private String fromName = "SellerX";

    /**
     * Queue configuration
     */
    private Queue queue = new Queue();

    @Data
    public static class Queue {
        private int batchSize = 50;
        private int retryDelayMinutes = 5;
        private int cleanupDays = 30;
    }
}
