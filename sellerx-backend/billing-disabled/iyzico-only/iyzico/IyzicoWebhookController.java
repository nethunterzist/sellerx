package com.ecommerce.sellerx.billing.iyzico;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for iyzico webhook callbacks.
 *
 * iyzico sends webhooks for:
 * - Payment success/failure
 * - 3DS completion
 * - Refund notifications
 * - Card storage events
 *
 * IMPORTANT: This endpoint is public and must return 200 OK quickly.
 */
@RestController
@RequestMapping("/api/webhook/iyzico")
@RequiredArgsConstructor
@Slf4j
public class IyzicoWebhookController {

    private final IyzicoWebhookService webhookService;

    /**
     * Handle payment webhook from iyzico
     *
     * iyzico sends payment notifications here after processing
     */
    @PostMapping("/payment")
    public ResponseEntity<Map<String, Object>> handlePaymentWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-Iyzico-Signature", required = false) String signature) {

        log.info("Received iyzico payment webhook: paymentId={}",
                payload.get("paymentId"));

        try {
            webhookService.processPaymentWebhook(payload, signature);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Webhook processed"
            ));
        } catch (DuplicateWebhookException e) {
            // Already processed - return success to prevent retries
            log.info("Duplicate webhook ignored: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "status", "duplicate",
                    "message", "Already processed"
            ));
        } catch (Exception e) {
            log.error("Error processing payment webhook", e);
            // Return 200 to prevent iyzico from retrying
            // We'll handle the error internally
            return ResponseEntity.ok(Map.of(
                    "status", "error",
                    "message", "Processing failed, will retry internally"
            ));
        }
    }

    /**
     * Handle 3DS callback from iyzico
     *
     * User is redirected here after completing 3DS verification
     */
    @PostMapping("/threeds")
    public ResponseEntity<Map<String, Object>> handleThreeDsCallback(
            @RequestBody Map<String, Object> payload) {

        log.info("Received iyzico 3DS callback: conversationId={}",
                payload.get("conversationId"));

        try {
            webhookService.processThreeDsCallback(payload);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "3DS callback processed"
            ));
        } catch (Exception e) {
            log.error("Error processing 3DS callback", e);
            return ResponseEntity.ok(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Handle refund webhook from iyzico
     */
    @PostMapping("/refund")
    public ResponseEntity<Map<String, Object>> handleRefundWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-Iyzico-Signature", required = false) String signature) {

        log.info("Received iyzico refund webhook: paymentId={}",
                payload.get("paymentId"));

        try {
            webhookService.processRefundWebhook(payload, signature);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Refund webhook processed"
            ));
        } catch (DuplicateWebhookException e) {
            log.info("Duplicate refund webhook ignored: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "status", "duplicate",
                    "message", "Already processed"
            ));
        } catch (Exception e) {
            log.error("Error processing refund webhook", e);
            return ResponseEntity.ok(Map.of(
                    "status", "error",
                    "message", "Processing failed"
            ));
        }
    }

    /**
     * Handle card storage webhook from iyzico
     */
    @PostMapping("/card")
    public ResponseEntity<Map<String, Object>> handleCardWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-Iyzico-Signature", required = false) String signature) {

        log.info("Received iyzico card webhook: cardUserKey={}",
                payload.get("cardUserKey"));

        try {
            webhookService.processCardWebhook(payload, signature);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Card webhook processed"
            ));
        } catch (Exception e) {
            log.error("Error processing card webhook", e);
            return ResponseEntity.ok(Map.of(
                    "status", "error",
                    "message", "Processing failed"
            ));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "iyzico-webhook"
        ));
    }

    /**
     * Exception for duplicate webhooks
     */
    public static class DuplicateWebhookException extends RuntimeException {
        public DuplicateWebhookException(String message) {
            super(message);
        }
    }
}
