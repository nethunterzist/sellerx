package com.ecommerce.sellerx.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
@Slf4j
public class TrendyolWebhookController {
    
    private final TrendyolWebhookService webhookService;
    
    /**
     * Receive webhook notifications from Trendyol
     * URL format: /api/webhook/trendyol/{sellerId}
     */
    @PostMapping("/trendyol/{sellerId}")
    public ResponseEntity<String> receiveTrendyolWebhook(
            @PathVariable String sellerId,
            @RequestBody TrendyolWebhookPayload payload,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey) {
        
        try {
            log.info("Received webhook for seller: {} with order: {} and status: {}", 
                    sellerId, payload.getOrderNumber(), payload.getStatus());
            
            // Process the webhook
            webhookService.processWebhookOrder(payload, sellerId);
            
            // Return 200 OK to acknowledge receipt
            return ResponseEntity.ok("Webhook processed successfully");
            
        } catch (Exception e) {
            log.error("Error processing webhook for seller {}: {}", sellerId, e.getMessage(), e);
            
            // Return 200 OK even on error to prevent Trendyol retries for processing errors
            // Only return 4xx/5xx if it's a validation or system issue
            return ResponseEntity.ok("Webhook received");
        }
    }
    
    /**
     * Health check endpoint for webhook
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Webhook service is running");
    }
}
