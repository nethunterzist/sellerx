package com.ecommerce.sellerx.email.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a password reset is requested.
 */
@Getter
public class PasswordResetRequestedEvent extends ApplicationEvent {

    private final Long userId;
    private final String email;
    private final String name;
    private final String resetToken;
    private final String resetLink;

    public PasswordResetRequestedEvent(Object source, Long userId, String email, String name,
                                       String resetToken, String resetLink) {
        super(source);
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.resetToken = resetToken;
        this.resetLink = resetLink;
    }
}
