package com.ecommerce.sellerx.sync;

import com.ecommerce.sellerx.common.exception.InvalidStoreConfigurationException;
import com.ecommerce.sellerx.common.exception.ResourceNotFoundException;
import com.ecommerce.sellerx.orders.TrendyolOrderService;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.stores.TrendyolCredentials;
import com.ecommerce.sellerx.stores.MarketplaceCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Async service for running order sync operations in the background.
 * Delegates to TrendyolOrderService for actual sync logic but tracks progress.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncOrderSyncService {

    private final SyncTaskService syncTaskService;
    private final TrendyolOrderService orderService;
    private final StoreRepository storeRepository;

    /**
     * Execute order sync asynchronously.
     * This method runs in a separate thread and updates task progress.
     */
    @Async("taskExecutor")
    public void executeOrderSync(UUID taskId, UUID storeId) {
        log.info("Starting async order sync for task {} store {}", taskId, storeId);

        try {
            // Mark task as started
            syncTaskService.startTask(taskId);

            // Validate store
            Store store = storeRepository.findById(storeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));

            if (!"trendyol".equalsIgnoreCase(store.getMarketplace())) {
                syncTaskService.failTask(taskId, "Store is not a Trendyol store");
                return;
            }

            TrendyolCredentials credentials = extractTrendyolCredentials(store);
            if (credentials == null) {
                syncTaskService.failTask(taskId, "Trendyol credentials not found");
                return;
            }

            // Update progress - syncing started
            syncTaskService.updateProgress(taskId, 0, 100, 0, 0, 0, 0);

            // Execute the actual sync (blocking call in this thread)
            // Note: TrendyolOrderService.fetchAndSaveOrdersForStore handles its own transaction
            orderService.fetchAndSaveOrdersForStore(storeId);

            // Complete the task
            // Note: We don't have exact counts from the order service, so we estimate
            syncTaskService.completeTask(taskId, 0, 0, 0, 0);

            log.info("[ORDERS] Task {} completed successfully", taskId);

        } catch (Exception e) {
            log.error("Error in async order sync task {}: {}", taskId, e.getMessage(), e);
            syncTaskService.failTask(taskId, "Error syncing orders: " + e.getMessage());
        }
    }

    private TrendyolCredentials extractTrendyolCredentials(Store store) {
        MarketplaceCredentials credentials = store.getCredentials();
        if (credentials instanceof TrendyolCredentials) {
            return (TrendyolCredentials) credentials;
        }
        return null;
    }
}
