package com.ecommerce.sellerx.webhook;

import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
@Slf4j
public class TrendyolWebhookController {

    private final TrendyolWebhookService webhookService;
    private final WebhookSignatureValidator signatureValidator;
    private final WebhookEventRepository eventRepository;
    private final StoreRepository storeRepository;
    private final ObjectMapper objectMapper;

    /**
     * Receive webhook notifications from Trendyol
     * MUST return 200 OK within 5 seconds to prevent retries
     */
    @PostMapping("/trendyol/{sellerId}")
    public ResponseEntity<String> receiveTrendyolWebhook(
            @PathVariable String sellerId,
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Trendyol-Signature", required = false) String signature,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey) {

        long startTime = System.currentTimeMillis();

        try {
            // 1. Validate signature (HMAC-SHA256)
            if (!signatureValidator.validateSignature(signature, rawPayload, sellerId)) {
                log.warn("Invalid signature for webhook from seller: {}", sellerId);
                return ResponseEntity.status(401).body("Invalid signature");
            }

            // 2. Parse payload
            TrendyolWebhookPayload payload = objectMapper.readValue(rawPayload, TrendyolWebhookPayload.class);

            log.info("Received webhook for seller: {} with order: {} and status: {}",
                    sellerId, payload.getOrderNumber(), payload.getStatus());

            // 3. Find store by seller ID
            Optional<Store> storeOpt = storeRepository.findBySellerId(sellerId);
            if (storeOpt.isEmpty()) {
                log.warn("Store not found for sellerId: {}", sellerId);
                return ResponseEntity.ok("Store not found");
            }
            Store store = storeOpt.get();

            // 4. Generate event ID for idempotency
            String eventId = signatureValidator.generateEventId(
                    sellerId,
                    payload.getOrderNumber(),
                    payload.getStatus(),
                    payload.getLastModifiedDate() != null ? payload.getLastModifiedDate() : System.currentTimeMillis()
            );

            // 5. Check for duplicate (idempotency)
            if (eventRepository.existsByEventId(eventId)) {
                log.info("Duplicate webhook received, eventId: {}", eventId);

                // Log as duplicate but still return 200 OK
                WebhookEvent duplicateEvent = WebhookEvent.builder()
                        .eventId(eventId + "_dup_" + System.currentTimeMillis())
                        .storeId(store.getId())
                        .sellerId(sellerId)
                        .eventType(determineEventType(payload))
                        .orderNumber(payload.getOrderNumber())
                        .status(payload.getStatus())
                        .processingStatus(WebhookEvent.ProcessingStatus.DUPLICATE)
                        .createdAt(LocalDateTime.now())
                        .processedAt(LocalDateTime.now())
                        .processingTimeMs(System.currentTimeMillis() - startTime)
                        .build();
                eventRepository.save(duplicateEvent);

                return ResponseEntity.ok("Webhook already processed");
            }

            // 6. Determine event type
            String eventType = determineEventType(payload);

            // 7. Create event log entry
            WebhookEvent event = WebhookEvent.builder()
                    .eventId(eventId)
                    .storeId(store.getId())
                    .sellerId(sellerId)
                    .eventType(eventType)
                    .orderNumber(payload.getOrderNumber())
                    .status(payload.getStatus())
                    .payload(rawPayload)
                    .processingStatus(WebhookEvent.ProcessingStatus.PROCESSING)
                    .createdAt(LocalDateTime.now())
                    .build();

            // 8. Process the webhook
            try {
                webhookService.processWebhookOrder(payload, sellerId);

                event.setProcessingStatus(WebhookEvent.ProcessingStatus.COMPLETED);
                event.setProcessedAt(LocalDateTime.now());
                event.setProcessingTimeMs(System.currentTimeMillis() - startTime);

            } catch (Exception e) {
                log.error("Error processing webhook: {}", e.getMessage());
                event.setProcessingStatus(WebhookEvent.ProcessingStatus.FAILED);
                event.setErrorMessage(e.getMessage());
                event.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            }

            // 9. Save event log
            eventRepository.save(event);

            // Return 200 OK to acknowledge receipt (required by Trendyol)
            return ResponseEntity.ok("Webhook processed successfully");

        } catch (Exception e) {
            log.error("Error processing webhook for seller {}: {}", sellerId, e.getMessage(), e);
            // Return 200 OK even on error to prevent Trendyol retries
            return ResponseEntity.ok("Webhook received");
        }
    }

    /**
     * Determine event type based on payload
     */
    private String determineEventType(TrendyolWebhookPayload payload) {
        String status = payload.getStatus();
        String shipmentStatus = payload.getShipmentPackageStatus();

        if ("Created".equalsIgnoreCase(status) || "CREATED".equalsIgnoreCase(shipmentStatus)) {
            return "order.created";
        }
        return "order.status.changed";
    }

    /**
     * Health check endpoint for webhook
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Webhook service is running");
    }
}
