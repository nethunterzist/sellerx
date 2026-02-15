package com.ecommerce.sellerx.email.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when email verification is requested.
 */
@Getter
public class EmailVerificationEvent extends ApplicationEvent {

    private final Long userId;
    private final String email;
    private final String name;
    private final String verificationToken;
    private final String verificationLink;

    public EmailVerificationEvent(Object source, Long userId, String email, String name,
                                  String verificationToken, String verificationLink) {
        super(source);
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.verificationToken = verificationToken;
        this.verificationLink = verificationLink;
    }
}
