package com.ecommerce.sellerx.email.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * Event published when admin sends a broadcast message.
 */
@Getter
public class AdminBroadcastEvent extends ApplicationEvent {

    private final String subject;
    private final String content;
    private final List<RecipientInfo> recipients;

    public AdminBroadcastEvent(Object source, String subject, String content, List<RecipientInfo> recipients) {
        super(source);
        this.subject = subject;
        this.content = content;
        this.recipients = recipients;
    }

    /**
     * Recipient information for broadcast.
     */
    public record RecipientInfo(Long userId, String email, String name) {}
}
