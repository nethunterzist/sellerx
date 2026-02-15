package com.ecommerce.sellerx.email.event;

import com.ecommerce.sellerx.email.EmailType;
import com.ecommerce.sellerx.email.service.EmailQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Listens to application events and queues appropriate emails.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailEventListener {

    private final EmailQueueService queueService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    /**
     * Handle user registration - send welcome email.
     */
    @EventListener
    @Async("taskExecutor")
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("[EMAIL-EVENT] User registered: userId={}, email={}", event.getUserId(), event.getEmail());

        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", event.getName());
        variables.put("userEmail", event.getEmail());

        queueService.enqueueForUser(
                EmailType.WELCOME,
                event.getEmail(),
                event.getName(),
                variables,
                event.getUserId()
        );
    }

    /**
     * Handle password reset request.
     */
    @EventListener
    @Async("taskExecutor")
    public void handlePasswordResetRequested(PasswordResetRequestedEvent event) {
        log.info("[EMAIL-EVENT] Password reset requested: userId={}", event.getUserId());

        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", event.getName());
        variables.put("resetLink", event.getResetLink());
        variables.put("resetToken", event.getResetToken());

        queueService.enqueueForUser(
                EmailType.PASSWORD_RESET,
                event.getEmail(),
                event.getName(),
                variables,
                event.getUserId()
        );
    }

    /**
     * Handle email verification request.
     */
    @EventListener
    @Async("taskExecutor")
    public void handleEmailVerification(EmailVerificationEvent event) {
        log.info("[EMAIL-EVENT] Email verification requested: userId={}", event.getUserId());

        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", event.getName());
        variables.put("verificationLink", event.getVerificationLink());
        variables.put("verificationToken", event.getVerificationToken());

        queueService.enqueueForUser(
                EmailType.EMAIL_VERIFICATION,
                event.getEmail(),
                event.getName(),
                variables,
                event.getUserId()
        );
    }

    /**
     * Handle subscription events.
     */
    @EventListener
    @Async("taskExecutor")
    public void handleSubscriptionEvent(SubscriptionEvent event) {
        log.info("[EMAIL-EVENT] Subscription event: type={}, userId={}", event.getType(), event.getUserId());

        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", event.getUserName());
        variables.put("planName", event.getPlanName());

        if (event.getExpiryDate() != null) {
            variables.put("expiryDate", event.getExpiryDate().format(DATE_FORMATTER));
        }
        if (event.getInvoiceUrl() != null) {
            variables.put("invoiceUrl", event.getInvoiceUrl());
        }

        EmailType emailType = mapSubscriptionEventToEmailType(event.getType());

        queueService.enqueueForUser(
                emailType,
                event.getEmail(),
                event.getUserName(),
                variables,
                event.getUserId()
        );
    }

    /**
     * Handle alert triggered.
     */
    @EventListener
    @Async("taskExecutor")
    public void handleAlertTriggered(AlertTriggeredEvent event) {
        log.info("[EMAIL-EVENT] Alert triggered: type={}, userId={}", event.getAlertType(), event.getUserId());

        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", event.getUserName());
        variables.put("alertTitle", event.getAlertTitle());
        variables.put("alertMessage", event.getAlertMessage());
        variables.put("alertType", event.getAlertType());
        variables.put("severity", event.getSeverity());
        if (event.getStoreName() != null) {
            variables.put("storeName", event.getStoreName());
        }

        queueService.enqueueForUser(
                EmailType.ALERT_NOTIFICATION,
                event.getEmail(),
                event.getUserName(),
                variables,
                event.getUserId()
        );
    }

    /**
     * Handle admin broadcast.
     */
    @EventListener
    @Async("taskExecutor")
    public void handleAdminBroadcast(AdminBroadcastEvent event) {
        log.info("[EMAIL-EVENT] Admin broadcast: subject={}, recipients={}",
                event.getSubject(), event.getRecipients().size());

        for (AdminBroadcastEvent.RecipientInfo recipient : event.getRecipients()) {
            Map<String, Object> variables = new HashMap<>();
            variables.put("userName", recipient.name());
            variables.put("subject", event.getSubject());
            variables.put("content", event.getContent());

            queueService.enqueueForUser(
                    EmailType.ADMIN_BROADCAST,
                    recipient.email(),
                    recipient.name(),
                    variables,
                    recipient.userId()
            );
        }
    }

    /**
     * Map subscription event type to email type.
     */
    private EmailType mapSubscriptionEventToEmailType(SubscriptionEvent.Type type) {
        return switch (type) {
            case CONFIRMED -> EmailType.SUBSCRIPTION_CONFIRMED;
            case REMINDER_7 -> EmailType.SUBSCRIPTION_REMINDER_7;
            case REMINDER_1 -> EmailType.SUBSCRIPTION_REMINDER_1;
            case RENEWED -> EmailType.SUBSCRIPTION_RENEWED;
            case PAYMENT_FAILED -> EmailType.PAYMENT_FAILED;
            case CANCELLED -> EmailType.SUBSCRIPTION_CANCELLED;
        };
    }
}
