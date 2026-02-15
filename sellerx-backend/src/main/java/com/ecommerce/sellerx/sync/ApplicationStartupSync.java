package com.ecommerce.sellerx.sync;

import com.ecommerce.sellerx.orders.TrendyolOrderService;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Runs a catch-up sync on application startup.
 * Covers the downtime window (last 48 hours) to ensure
 * no order status transitions are missed during restarts/deployments.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationStartupSync {

    private final StoreRepository storeRepository;
    private final TrendyolOrderService orderService;

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void onApplicationReady() {
        log.info("Starting application startup sync (48-hour window)");

        try {
            List<Store> stores = storeRepository.findByInitialSyncCompletedTrue();
            int successCount = 0;
            int failCount = 0;

            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusHours(48);

            for (Store store : stores) {
                if (!"trendyol".equalsIgnoreCase(store.getMarketplace())) {
                    continue;
                }

                try {
                    orderService.fetchAndSaveOrdersForStoreInRange(store.getId(), startTime, endTime);
                    successCount++;
                    log.debug("Startup sync completed for store: {}", store.getId());
                } catch (Exception e) {
                    failCount++;
                    log.error("Startup sync failed for store {}: {}", store.getId(), e.getMessage());
                }
            }

            log.info("Application startup sync completed: {} success, {} failed", successCount, failCount);
        } catch (Exception e) {
            log.error("Application startup sync failed: {}", e.getMessage(), e);
        }
    }
}
