package com.ecommerce.sellerx.billing.service;

import com.ecommerce.sellerx.billing.*;
import com.ecommerce.sellerx.billing.config.SubscriptionConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Handles payment retry logic for failed payments
 *
 * Retry schedule:
 * - Day 0: Immediate retry
 * - Day 1: Second retry
 * - Day 2: Third retry
 * - Day 3: Subscription suspended
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentRetryService {

    private final PaymentTransactionRepository transactionRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final SubscriptionService subscriptionService;
    private final PaymentService paymentService;
    private final SubscriptionConfig config;

    /**
     * Process all pending retries
     */
    @Transactional
    public void processRetries() {
        log.info("Processing payment retries...");

        List<PaymentTransaction> transactionsToRetry =
                transactionRepository.findTransactionsToRetry(LocalDateTime.now());

        log.info("Found {} transactions to retry", transactionsToRetry.size());

        for (PaymentTransaction transaction : transactionsToRetry) {
            try {
                retryPayment(transaction);
            } catch (Exception e) {
                log.error("Error retrying payment: transactionId={}", transaction.getId(), e);
            }
        }
    }

    /**
     * Retry a specific failed payment
     */
    @Transactional
    public void retryPayment(PaymentTransaction failedTransaction) {
        log.info("Retrying payment: transactionId={}, attempt={}",
                failedTransaction.getId(), failedTransaction.getAttemptNumber() + 1);

        Invoice invoice = failedTransaction.getInvoice();
        Subscription subscription = invoice.getSubscription();

        // Check if already at max retries
        if (failedTransaction.getAttemptNumber() >= config.getMaxRetryAttempts()) {
            log.warn("Max retries reached: transactionId={}", failedTransaction.getId());

            // Suspend subscription after max retries
            if (subscription.getStatus() == SubscriptionStatus.PAST_DUE) {
                subscriptionService.suspendSubscription(subscription.getId());
            }
            return;
        }

        // Find default payment method
        Optional<PaymentMethod> defaultPaymentMethod =
                paymentMethodRepository.findByUserIdAndIsDefaultTrueAndIsActiveTrue(
                        invoice.getUser().getId());

        if (defaultPaymentMethod.isEmpty()) {
            log.warn("No default payment method for retry: userId={}", invoice.getUser().getId());
            handleNoPaymentMethod(failedTransaction, subscription);
            return;
        }

        // Attempt retry
        PaymentService.PaymentResult result = paymentService.processPayment(
                invoice,
                defaultPaymentMethod.get(),
                "127.0.0.1" // Retry IP
        );

        if (result.success()) {
            log.info("Retry successful: transactionId={}", failedTransaction.getId());
        } else {
            log.warn("Retry failed: transactionId={}, error={}",
                    failedTransaction.getId(), result.errorMessage());

            // Check if we've exhausted retries
            if (failedTransaction.getAttemptNumber() >= config.getMaxRetryAttempts() - 1) {
                if (subscription.getStatus() == SubscriptionStatus.PAST_DUE) {
                    subscriptionService.suspendSubscription(subscription.getId());
                }
            }
        }
    }

    /**
     * Handle case where user has no payment method
     */
    private void handleNoPaymentMethod(PaymentTransaction transaction, Subscription subscription) {
        // Increment attempt counter even without actual retry
        transaction.setAttemptNumber(transaction.getAttemptNumber() + 1);

        if (transaction.getAttemptNumber() >= config.getMaxRetryAttempts()) {
            transaction.setNextRetryAt(null); // No more retries
            if (subscription.getStatus() == SubscriptionStatus.PAST_DUE) {
                subscriptionService.suspendSubscription(subscription.getId());
            }
        } else {
            transaction.scheduleRetry();
        }

        transactionRepository.save(transaction);
    }

    /**
     * Check and suspend subscriptions with expired grace periods
     */
    @Transactional
    public void processExpiredGracePeriods() {
        log.info("Processing expired grace periods...");

        List<Subscription> expiredGracePeriods =
                subscriptionService.findGracePeriodExpired(LocalDateTime.now());

        log.info("Found {} subscriptions with expired grace periods", expiredGracePeriods.size());

        for (Subscription subscription : expiredGracePeriods) {
            try {
                subscriptionService.suspendSubscription(subscription.getId());
            } catch (Exception e) {
                log.error("Error suspending subscription: {}", subscription.getId(), e);
            }
        }
    }

    /**
     * Expire long-suspended subscriptions (30 days after suspension)
     */
    @Transactional
    public void processExpiredSuspensions() {
        log.info("Processing expired suspensions...");

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        List<Subscription> expiredSuspensions = subscriptionService.findSuspendedOlderThan(cutoffDate);

        log.info("Found {} suspended subscriptions to expire", expiredSuspensions.size());

        for (Subscription subscription : expiredSuspensions) {
            try {
                subscriptionService.expireSubscription(subscription.getId());
            } catch (Exception e) {
                log.error("Error expiring subscription: {}", subscription.getId(), e);
            }
        }
    }
}
