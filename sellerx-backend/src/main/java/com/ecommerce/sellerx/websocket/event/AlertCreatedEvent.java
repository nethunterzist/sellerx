package com.ecommerce.sellerx.websocket.event;

import com.ecommerce.sellerx.alerts.AlertHistory;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a new alert is created.
 * Used to decouple alert creation from WebSocket notification.
 */
@Getter
public class AlertCreatedEvent extends ApplicationEvent {

    private final AlertHistory alert;

    public AlertCreatedEvent(Object source, AlertHistory alert) {
        super(source);
        this.alert = alert;
    }
}
