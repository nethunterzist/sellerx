package com.ecommerce.sellerx.billing.iyzico;

import com.ecommerce.sellerx.billing.*;
import com.ecommerce.sellerx.billing.config.IyzicoConfig;
import com.ecommerce.sellerx.billing.service.InvoiceService;
import com.ecommerce.sellerx.billing.service.SubscriptionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for processing iyzico webhooks with idempotency
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IyzicoWebhookService {

    private final PaymentWebhookEventRepository webhookEventRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final InvoiceRepository invoiceRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final InvoiceService invoiceService;
    private final IyzicoConfig iyzicoConfig;
    private final ObjectMapper objectMapper;

    /**
     * Process payment webhook from iyzico
     */
    @Transactional
    public void processPaymentWebhook(Map<String, Object> payload, String signature) {
        long startTime = System.currentTimeMillis();

        String eventId = extractEventId(payload, "PAYMENT");
        String eventType = "PAYMENT_" + payload.getOrDefault("status", "UNKNOWN");

        // Check for duplicate
        Optional<PaymentWebhookEvent> existingEvent = webhookEventRepository.findByEventId(eventId);
        if (existingEvent.isPresent()) {
            if (existingEvent.get().isAlreadyProcessed()) {
                throw new IyzicoWebhookController.DuplicateWebhookException(
                        "Webhook already processed: " + eventId);
            }
        }

        // Create webhook event record
        PaymentWebhookEvent webhookEvent = existingEvent.orElseGet(() ->
                PaymentWebhookEvent.builder()
                        .eventId(eventId)
                        .eventType(eventType)
                        .payload(toJson(payload))
                        .processingStatus(WebhookProcessingStatus.RECEIVED)
                        .build());

        webhookEvent.markProcessing();
        webhookEvent = webhookEventRepository.save(webhookEvent);

        try {
            // Validate signature if provided
            if (signature != null && !validateSignature(payload, signature)) {
                log.warn("Invalid webhook signature for event: {}", eventId);
                // Continue processing in sandbox mode
                if (!iyzicoConfig.isSandbox()) {
                    throw new SecurityException("Invalid webhook signature");
                }
            }

            // Process based on payment status
            String status = (String) payload.get("status");
            String paymentId = (String) payload.get("paymentId");
            String conversationId = (String) payload.get("conversationId");

            if ("SUCCESS".equals(status)) {
                handlePaymentSuccess(conversationId, paymentId, payload);
            } else if ("FAILURE".equals(status)) {
                handlePaymentFailure(conversationId, payload);
            }

            long processingTime = System.currentTimeMillis() - startTime;
            webhookEvent.markCompleted(processingTime);

            log.info("Payment webhook processed: eventId={}, status={}, time={}ms",
                    eventId, status, processingTime);

        } catch (IyzicoWebhookController.DuplicateWebhookException e) {
            webhookEvent.markDuplicate();
            throw e;
        } catch (Exception e) {
            webhookEvent.markFailed(e.getMessage());
            log.error("Failed to process payment webhook: {}", eventId, e);
            throw e;
        } finally {
            webhookEventRepository.save(webhookEvent);
        }
    }

    /**
     * Process 3DS callback from iyzico
     */
    @Transactional
    public void processThreeDsCallback(Map<String, Object> payload) {
        String conversationId = (String) payload.get("conversationId");
        String status = (String) payload.get("status");
        String paymentId = (String) payload.get("paymentId");

        log.info("Processing 3DS callback: conversationId={}, status={}",
                conversationId, status);

        // Find the pending transaction
        Optional<PaymentTransaction> transactionOpt =
                transactionRepository.findByIyzicoConversationId(conversationId);

        if (transactionOpt.isEmpty()) {
            log.warn("No transaction found for 3DS callback: {}", conversationId);
            return;
        }

        PaymentTransaction transaction = transactionOpt.get();

        if ("success".equalsIgnoreCase(status)) {
            // Update transaction
            transaction.setStatus(PaymentStatus.SUCCESS);
            transaction.setIyzicoPaymentId(paymentId);
            transactionRepository.save(transaction);

            // Mark invoice as paid
            Invoice invoice = transaction.getInvoice();
            invoiceService.markAsPaid(invoice.getId());

            // Activate subscription
            Subscription subscription = invoice.getSubscription();
            if (subscription.getStatus() == SubscriptionStatus.PENDING_PAYMENT ||
                    subscription.getStatus() == SubscriptionStatus.TRIAL) {
                subscriptionService.activateSubscription(subscription.getId());
            }

            log.info("3DS payment successful: transactionId={}", transaction.getId());
        } else {
            // Payment failed
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setFailureMessage("3DS verification failed: " + status);
            transactionRepository.save(transaction);

            // Mark invoice as failed
            invoiceService.markAsFailed(transaction.getInvoice().getId());

            log.warn("3DS payment failed: transactionId={}, status={}",
                    transaction.getId(), status);
        }
    }

    /**
     * Process refund webhook from iyzico
     */
    @Transactional
    public void processRefundWebhook(Map<String, Object> payload, String signature) {
        long startTime = System.currentTimeMillis();

        String eventId = extractEventId(payload, "REFUND");

        // Check for duplicate
        if (webhookEventRepository.existsByEventId(eventId)) {
            throw new IyzicoWebhookController.DuplicateWebhookException(
                    "Refund webhook already processed: " + eventId);
        }

        PaymentWebhookEvent webhookEvent = PaymentWebhookEvent.builder()
                .eventId(eventId)
                .eventType("REFUND")
                .payload(toJson(payload))
                .processingStatus(WebhookProcessingStatus.RECEIVED)
                .build();

        webhookEvent.markProcessing();
        webhookEvent = webhookEventRepository.save(webhookEvent);

        try {
            String paymentId = (String) payload.get("paymentId");
            String status = (String) payload.get("status");

            // Find the original transaction
            Optional<PaymentTransaction> transactionOpt =
                    transactionRepository.findByIyzicoPaymentId(paymentId);

            if (transactionOpt.isPresent() && "SUCCESS".equals(status)) {
                PaymentTransaction transaction = transactionOpt.get();
                transaction.setStatus(PaymentStatus.REFUNDED);
                transactionRepository.save(transaction);

                // Update invoice status
                Invoice invoice = transaction.getInvoice();
                invoice.setStatus(InvoiceStatus.REFUNDED);
                invoiceRepository.save(invoice);

                webhookEvent.setPayment(transaction);

                log.info("Refund processed: paymentId={}", paymentId);
            }

            long processingTime = System.currentTimeMillis() - startTime;
            webhookEvent.markCompleted(processingTime);

        } catch (Exception e) {
            webhookEvent.markFailed(e.getMessage());
            throw e;
        } finally {
            webhookEventRepository.save(webhookEvent);
        }
    }

    /**
     * Process card storage webhook from iyzico
     */
    @Transactional
    public void processCardWebhook(Map<String, Object> payload, String signature) {
        String cardUserKey = (String) payload.get("cardUserKey");
        String eventType = (String) payload.get("eventType");

        log.info("Processing card webhook: cardUserKey={}, eventType={}",
                cardUserKey, eventType);

        // Card webhooks are informational - we don't need to take action
        // Card storage/deletion is already handled synchronously in our API
    }

    /**
     * Handle successful payment
     */
    private void handlePaymentSuccess(String conversationId, String paymentId, Map<String, Object> payload) {
        Optional<PaymentTransaction> transactionOpt =
                transactionRepository.findByIyzicoConversationId(conversationId);

        if (transactionOpt.isEmpty()) {
            log.warn("No transaction found for successful payment: {}", conversationId);
            return;
        }

        PaymentTransaction transaction = transactionOpt.get();

        // Already successful - skip
        if (transaction.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment already marked as successful: {}", transaction.getId());
            return;
        }

        transaction.setStatus(PaymentStatus.SUCCESS);
        transaction.setIyzicoPaymentId(paymentId);
        transactionRepository.save(transaction);

        // Mark invoice as paid
        Invoice invoice = transaction.getInvoice();
        invoiceService.markAsPaid(invoice.getId());

        // Activate subscription if needed
        Subscription subscription = invoice.getSubscription();
        if (subscription.getStatus() == SubscriptionStatus.PENDING_PAYMENT ||
                subscription.getStatus() == SubscriptionStatus.TRIAL ||
                subscription.getStatus() == SubscriptionStatus.PAST_DUE) {
            subscriptionService.activateSubscription(subscription.getId());
        }

        log.info("Payment success processed: transactionId={}, invoiceId={}",
                transaction.getId(), invoice.getId());
    }

    /**
     * Handle failed payment
     */
    private void handlePaymentFailure(String conversationId, Map<String, Object> payload) {
        Optional<PaymentTransaction> transactionOpt =
                transactionRepository.findByIyzicoConversationId(conversationId);

        if (transactionOpt.isEmpty()) {
            log.warn("No transaction found for failed payment: {}", conversationId);
            return;
        }

        PaymentTransaction transaction = transactionOpt.get();

        // Already processed - skip
        if (transaction.getStatus() != PaymentStatus.PROCESSING &&
                transaction.getStatus() != PaymentStatus.PENDING) {
            log.info("Payment already processed: {}", transaction.getId());
            return;
        }

        String errorCode = (String) payload.get("errorCode");
        String errorMessage = (String) payload.get("errorMessage");

        transaction.setStatus(PaymentStatus.FAILED);
        transaction.setFailureCode(errorCode);
        transaction.setFailureMessage(errorMessage);

        // Schedule retry if eligible
        if (transaction.canRetry()) {
            transaction.scheduleRetry();
        }

        transactionRepository.save(transaction);

        // Mark invoice as failed
        Invoice invoice = transaction.getInvoice();
        invoiceService.markAsFailed(invoice.getId());

        // Mark subscription as past due if active
        Subscription subscription = invoice.getSubscription();
        if (subscription.getStatus() == SubscriptionStatus.ACTIVE ||
                subscription.getStatus() == SubscriptionStatus.TRIAL) {
            subscriptionService.markPastDue(subscription.getId());
        }

        log.warn("Payment failure processed: transactionId={}, error={}: {}",
                transaction.getId(), errorCode, errorMessage);
    }

    /**
     * Extract or generate event ID for idempotency
     */
    private String extractEventId(Map<String, Object> payload, String prefix) {
        // Try to get iyzico's event ID
        Object eventId = payload.get("eventId");
        if (eventId != null) {
            return eventId.toString();
        }

        // Fall back to payment ID + timestamp
        Object paymentId = payload.get("paymentId");
        Object conversationId = payload.get("conversationId");

        String id = paymentId != null ? paymentId.toString() :
                (conversationId != null ? conversationId.toString() : UUID.randomUUID().toString());

        return prefix + "_" + id + "_" + System.currentTimeMillis();
    }

    /**
     * Validate iyzico webhook signature
     */
    private boolean validateSignature(Map<String, Object> payload, String signature) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            String secretKey = iyzicoConfig.getSecretKey();

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(payloadJson.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = Base64.getEncoder().encodeToString(hash);

            return calculatedSignature.equals(signature);
        } catch (Exception e) {
            log.error("Error validating webhook signature", e);
            return false;
        }
    }

    /**
     * Convert object to JSON string
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }
}
