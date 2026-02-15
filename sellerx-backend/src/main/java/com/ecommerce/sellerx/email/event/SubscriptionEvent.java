package com.ecommerce.sellerx.email.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

/**
 * Event published for subscription lifecycle changes.
 */
@Getter
public class SubscriptionEvent extends ApplicationEvent {

    public enum Type {
        CONFIRMED,      // New subscription purchased
        REMINDER_7,     // 7 days before expiry
        REMINDER_1,     // 1 day before expiry
        RENEWED,        // Subscription renewed
        PAYMENT_FAILED, // Payment failed
        CANCELLED       // Subscription cancelled
    }

    private final Type type;
    private final Long userId;
    private final String email;
    private final String userName;
    private final String planName;
    private final LocalDate expiryDate;
    private final String invoiceUrl;

    public SubscriptionEvent(Object source, Type type, Long userId, String email, String userName,
                             String planName, LocalDate expiryDate, String invoiceUrl) {
        super(source);
        this.type = type;
        this.userId = userId;
        this.email = email;
        this.userName = userName;
        this.planName = planName;
        this.expiryDate = expiryDate;
        this.invoiceUrl = invoiceUrl;
    }

    // Convenience constructors
    public static SubscriptionEvent confirmed(Object source, Long userId, String email, String userName,
                                              String planName, String invoiceUrl) {
        return new SubscriptionEvent(source, Type.CONFIRMED, userId, email, userName, planName, null, invoiceUrl);
    }

    public static SubscriptionEvent reminder(Object source, Type reminderType, Long userId, String email,
                                             String userName, String planName, LocalDate expiryDate) {
        return new SubscriptionEvent(source, reminderType, userId, email, userName, planName, expiryDate, null);
    }

    public static SubscriptionEvent renewed(Object source, Long userId, String email, String userName,
                                            String planName, String invoiceUrl) {
        return new SubscriptionEvent(source, Type.RENEWED, userId, email, userName, planName, null, invoiceUrl);
    }

    public static SubscriptionEvent paymentFailed(Object source, Long userId, String email, String userName,
                                                  String planName) {
        return new SubscriptionEvent(source, Type.PAYMENT_FAILED, userId, email, userName, planName, null, null);
    }

    public static SubscriptionEvent cancelled(Object source, Long userId, String email, String userName,
                                              String planName) {
        return new SubscriptionEvent(source, Type.CANCELLED, userId, email, userName, planName, null, null);
    }
}
