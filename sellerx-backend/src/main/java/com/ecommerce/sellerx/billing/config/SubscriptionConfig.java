package com.ecommerce.sellerx.billing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Subscription system configuration
 */
@Configuration
@ConfigurationProperties(prefix = "subscription")
@Data
public class SubscriptionConfig {

    /**
     * Trial period in days (default: 14)
     */
    private int trialDays = 14;

    /**
     * Grace period for failed payments in days (default: 3)
     */
    private int gracePeriodDays = 3;

    /**
     * Maximum payment retry attempts (default: 3)
     */
    private int maxRetryAttempts = 3;

    /**
     * Scheduler configuration
     */
    private Scheduler scheduler = new Scheduler();

    /**
     * Invoice settings
     */
    private Invoice invoice = new Invoice();

    @Data
    public static class Scheduler {
        /**
         * Cron for checking trial expirations (default: daily at 9 AM)
         */
        private String trialCheckCron = "0 0 9 * * ?";

        /**
         * Cron for processing renewals (default: daily at 6 AM)
         */
        private String renewalCheckCron = "0 0 6 * * ?";

        /**
         * Cron for retrying failed payments (default: every 30 minutes)
         */
        private String retryCheckCron = "0 */30 * * * ?";

        /**
         * Cron for cleaning up suspended subscriptions (default: daily at 3 AM)
         */
        private String suspendedCleanupCron = "0 0 3 * * ?";
    }

    @Data
    public static class Invoice {
        /**
         * Invoice series prefix (default: SEL)
         */
        private String series = "SEL";

        /**
         * Tax rate percentage (default: 20 for Turkey KDV)
         */
        private int taxRate = 20;
    }
}
