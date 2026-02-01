package com.ecommerce.sellerx.webhook;

import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.stores.TrendyolCredentials;
import com.ecommerce.sellerx.stores.WebhookStatus;
import com.ecommerce.sellerx.users.User;
import com.ecommerce.sellerx.users.UserNotFoundException;
import com.ecommerce.sellerx.users.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/stores/{storeId}/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookManagementController {

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final WebhookEventRepository eventRepository;
    private final TrendyolWebhookManagementService webhookManagementService;

    /**
     * Get webhook status for a store
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getWebhookStatus(@PathVariable UUID storeId) {
        Store store = getStoreWithAccessCheck(storeId);

        Map<String, Object> status = new HashMap<>();
        status.put("storeId", storeId);
        status.put("webhookId", store.getWebhookId());
        status.put("enabled", store.getWebhookId() != null);

        // Build webhook URL
        if (store.getCredentials() instanceof TrendyolCredentials) {
            TrendyolCredentials credentials = (TrendyolCredentials) store.getCredentials();
            status.put("webhookUrl", "/api/webhook/trendyol/" + credentials.getSellerId());
        }

        // Get event statistics
        var stats = eventRepository.countByStoreIdGroupByStatus(storeId);
        Map<String, Long> eventStats = new HashMap<>();
        for (var row : stats) {
            eventStats.put(row.getProcessingStatus(), row.getStatusCount());
        }
        status.put("eventStats", eventStats);
        status.put("totalEvents", eventRepository.countByStoreId(storeId));

        return ResponseEntity.ok(status);
    }

    /**
     * Enable webhooks for a store (register with Trendyol)
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @PostMapping("/enable")
    public ResponseEntity<Map<String, Object>> enableWebhooks(@PathVariable UUID storeId) {
        Store store = getStoreWithAccessCheck(storeId);

        if (store.getWebhookId() != null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Webhooks already enabled for this store"
            ));
        }

        if (!(store.getCredentials() instanceof TrendyolCredentials)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Store is not a Trendyol store"
            ));
        }

        TrendyolCredentials credentials = (TrendyolCredentials) store.getCredentials();
        TrendyolWebhookManagementService.WebhookResult result = webhookManagementService.createWebhookForStore(credentials);

        if (result.isDisabled()) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Webhooks are disabled globally"
            ));
        }

        if (result.isSuccess()) {
            store.setWebhookId(result.getWebhookId());
            store.setWebhookStatus(WebhookStatus.ACTIVE);
            store.setWebhookErrorMessage(null);
            storeRepository.save(store);

            log.info("Enabled webhook {} for store {}", result.getWebhookId(), storeId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "webhookId", result.getWebhookId(),
                    "message", "Webhooks enabled successfully"
            ));
        }

        // Update store with error status
        store.setWebhookStatus(WebhookStatus.FAILED);
        store.setWebhookErrorMessage(result.getErrorMessage());
        storeRepository.save(store);

        return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", result.getErrorMessage() != null ? result.getErrorMessage() : "Failed to register webhook with Trendyol"
        ));
    }

    /**
     * Disable webhooks for a store (delete from Trendyol)
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @PostMapping("/disable")
    public ResponseEntity<Map<String, Object>> disableWebhooks(@PathVariable UUID storeId) {
        Store store = getStoreWithAccessCheck(storeId);

        if (store.getWebhookId() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Webhooks not enabled for this store"
            ));
        }

        if (!(store.getCredentials() instanceof TrendyolCredentials)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Store is not a Trendyol store"
            ));
        }

        TrendyolCredentials credentials = (TrendyolCredentials) store.getCredentials();
        TrendyolWebhookManagementService.WebhookResult result = webhookManagementService.deleteWebhookForStore(credentials, store.getWebhookId());

        if (result.isDisabled()) {
            // Webhooks are disabled globally, just clear the local reference
            String oldWebhookId = store.getWebhookId();
            store.setWebhookId(null);
            store.setWebhookStatus(WebhookStatus.INACTIVE);
            storeRepository.save(store);

            log.info("Cleared webhook {} for store {} (webhooks disabled globally)", oldWebhookId, storeId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Webhooks cleared (disabled globally)"
            ));
        }

        if (result.isSuccess()) {
            String oldWebhookId = store.getWebhookId();
            store.setWebhookId(null);
            store.setWebhookStatus(WebhookStatus.INACTIVE);
            store.setWebhookErrorMessage(null);
            storeRepository.save(store);

            log.info("Disabled webhook {} for store {}", oldWebhookId, storeId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Webhooks disabled successfully"
            ));
        }

        return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", result.getErrorMessage() != null ? result.getErrorMessage() : "Failed to delete webhook from Trendyol"
        ));
    }

    /**
     * Get webhook event logs for a store
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/events")
    public ResponseEntity<Page<WebhookEvent>> getWebhookEvents(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String eventType) {

        getStoreWithAccessCheck(storeId); // Just for access check

        size = Math.min(size, 100);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<WebhookEvent> events;
        if (eventType != null && !eventType.isEmpty()) {
            events = eventRepository.findByStoreIdAndEventTypeOrderByCreatedAtDesc(storeId, eventType, pageRequest);
        } else {
            events = eventRepository.findByStoreIdOrderByCreatedAtDesc(storeId, pageRequest);
        }

        return ResponseEntity.ok(events);
    }

    /**
     * Test webhook endpoint (creates a test event)
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testWebhook(@PathVariable UUID storeId) {
        Store store = getStoreWithAccessCheck(storeId);

        // Create a test event to verify webhook logging is working
        WebhookEvent testEvent = WebhookEvent.builder()
                .eventId("test-" + System.currentTimeMillis())
                .storeId(storeId)
                .sellerId("test")
                .eventType("test")
                .orderNumber("TEST-ORDER")
                .status("TEST")
                .processingStatus(WebhookEvent.ProcessingStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .processingTimeMs(0L)
                .build();

        eventRepository.save(testEvent);

        log.info("Created test webhook event for store {}", storeId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Test webhook event created",
                "eventId", testEvent.getEventId()
        ));
    }

    /**
     * Get store with access check
     */
    private Store getStoreWithAccessCheck(UUID storeId) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));

        // Check if store belongs to user
        if (!store.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Bu store'a erişim yetkiniz yok.");
        }

        return store;
    }
}
