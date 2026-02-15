package com.ecommerce.sellerx.email;

/**
 * Email types that correspond to database templates.
 * Each type maps to an email_templates record.
 */
public enum EmailType {
    // User lifecycle
    WELCOME,
    PASSWORD_RESET,
    EMAIL_VERIFICATION,

    // Subscription
    SUBSCRIPTION_CONFIRMED,
    SUBSCRIPTION_REMINDER_7,
    SUBSCRIPTION_REMINDER_1,
    SUBSCRIPTION_RENEWED,
    PAYMENT_FAILED,
    SUBSCRIPTION_CANCELLED,

    // Alerts & Reports
    ALERT_NOTIFICATION,
    DAILY_DIGEST,
    WEEKLY_REPORT,

    // Admin
    ADMIN_BROADCAST,

    // Support (legacy compatibility)
    TICKET_CREATED,
    TICKET_REPLY,
    TICKET_CLOSED,
    TICKET_ASSIGNED
}
