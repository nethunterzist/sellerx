package com.ecommerce.sellerx.email.scheduler;

import com.ecommerce.sellerx.billing.Subscription;
import com.ecommerce.sellerx.billing.SubscriptionRepository;
import com.ecommerce.sellerx.billing.SubscriptionStatus;
import com.ecommerce.sellerx.email.event.SubscriptionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Scheduled job for sending subscription reminder emails.
 * Runs daily at 09:00 Turkey time.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionReminderJob {

    private final SubscriptionRepository subscriptionRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Check for subscriptions expiring in 7 days and send reminder.
     * Runs daily at 09:00 AM.
     */
    @Scheduled(cron = "0 0 9 * * ?") // 09:00 every day
    @SchedulerLock(name = "subscriptionReminder7Days", lockAtMostFor = "30m", lockAtLeastFor = "5m")
    @Transactional(readOnly = true)
    public void sendSevenDayReminders() {
        log.info("[SUBSCRIPTION-REMINDER] Checking 7-day reminders...");

        LocalDateTime sevenDaysFromNow = LocalDateTime.now().plusDays(7);
        LocalDateTime startOfDay = sevenDaysFromNow.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = sevenDaysFromNow.toLocalDate().atTime(LocalTime.MAX);

        // Find subscriptions expiring in exactly 7 days
        List<Subscription> expiringSubscriptions = findSubscriptionsExpiringBetween(startOfDay, endOfDay);

        log.info("[SUBSCRIPTION-REMINDER] Found {} subscriptions expiring in 7 days", expiringSubscriptions.size());

        for (Subscription subscription : expiringSubscriptions) {
            sendReminderEmail(subscription, SubscriptionEvent.Type.REMINDER_7);
        }
    }

    /**
     * Check for subscriptions expiring tomorrow and send final reminder.
     * Runs daily at 09:00 AM.
     */
    @Scheduled(cron = "0 5 9 * * ?") // 09:05 every day (slightly offset)
    @SchedulerLock(name = "subscriptionReminder1Day", lockAtMostFor = "30m", lockAtLeastFor = "5m")
    @Transactional(readOnly = true)
    public void sendOneDayReminders() {
        log.info("[SUBSCRIPTION-REMINDER] Checking 1-day reminders...");

        LocalDateTime oneDayFromNow = LocalDateTime.now().plusDays(1);
        LocalDateTime startOfDay = oneDayFromNow.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = oneDayFromNow.toLocalDate().atTime(LocalTime.MAX);

        // Find subscriptions expiring tomorrow
        List<Subscription> expiringSubscriptions = findSubscriptionsExpiringBetween(startOfDay, endOfDay);

        log.info("[SUBSCRIPTION-REMINDER] Found {} subscriptions expiring tomorrow", expiringSubscriptions.size());

        for (Subscription subscription : expiringSubscriptions) {
            sendReminderEmail(subscription, SubscriptionEvent.Type.REMINDER_1);
        }
    }

    /**
     * Find subscriptions that are active/trial and expire between given dates.
     */
    private List<Subscription> findSubscriptionsExpiringBetween(LocalDateTime start, LocalDateTime end) {
        return subscriptionRepository.findAll().stream()
                .filter(s -> isEligibleForReminder(s))
                .filter(s -> s.getCurrentPeriodEnd() != null)
                .filter(s -> !s.getCurrentPeriodEnd().isBefore(start))
                .filter(s -> !s.getCurrentPeriodEnd().isAfter(end))
                .toList();
    }

    /**
     * Check if subscription is eligible for reminder email.
     */
    private boolean isEligibleForReminder(Subscription subscription) {
        // Only active or trial subscriptions that won't auto-renew
        SubscriptionStatus status = subscription.getStatus();
        if (status != SubscriptionStatus.ACTIVE && status != SubscriptionStatus.TRIAL) {
            return false;
        }

        // If auto-renew is enabled and not cancelled, no need for reminder
        // We want to remind users who might lose access
        return !subscription.getAutoRenew() || subscription.getCancelAtPeriodEnd();
    }

    /**
     * Send reminder email for subscription.
     */
    private void sendReminderEmail(Subscription subscription, SubscriptionEvent.Type reminderType) {
        try {
            var user = subscription.getUser();
            var plan = subscription.getPlan();
            LocalDate expiryDate = subscription.getCurrentPeriodEnd().toLocalDate();

            SubscriptionEvent event = SubscriptionEvent.reminder(
                    this,
                    reminderType,
                    user.getId(),
                    user.getEmail(),
                    user.getName(),
                    plan.getName(),
                    expiryDate
            );

            eventPublisher.publishEvent(event);

            log.info("[SUBSCRIPTION-REMINDER] Sent {} reminder to userId={}, expiryDate={}",
                    reminderType, user.getId(), expiryDate);

        } catch (Exception e) {
            log.error("[SUBSCRIPTION-REMINDER] Failed to send reminder for subscriptionId={}: {}",
                    subscription.getId(), e.getMessage());
        }
    }
}
