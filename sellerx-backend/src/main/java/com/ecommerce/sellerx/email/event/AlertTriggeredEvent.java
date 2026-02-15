package com.ecommerce.sellerx.email.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when an alert is triggered.
 */
@Getter
public class AlertTriggeredEvent extends ApplicationEvent {

    private final Long userId;
    private final String email;
    private final String userName;
    private final String alertTitle;
    private final String alertMessage;
    private final String alertType;  // STOCK, PROFIT, PRICE, ORDER, SYSTEM
    private final String severity;   // LOW, MEDIUM, HIGH, CRITICAL
    private final String storeName;

    public AlertTriggeredEvent(Object source, Long userId, String email, String userName,
                               String alertTitle, String alertMessage, String alertType,
                               String severity, String storeName) {
        super(source);
        this.userId = userId;
        this.email = email;
        this.userName = userName;
        this.alertTitle = alertTitle;
        this.alertMessage = alertMessage;
        this.alertType = alertType;
        this.severity = severity;
        this.storeName = storeName;
    }
}
