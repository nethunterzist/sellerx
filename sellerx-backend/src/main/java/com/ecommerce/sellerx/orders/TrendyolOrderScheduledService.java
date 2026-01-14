package com.ecommerce.sellerx.orders;

import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendyolOrderScheduledService {

    private final TrendyolOrderService orderService;
    private final StoreRepository storeRepository;

    /**
     * Scheduled task to sync orders for all Trendyol stores
     * Runs every day at 6:15 AM Turkey time (GMT+3)
     */
    @Scheduled(cron = "0 15 6 * * ?", zone = "Europe/Istanbul")
    public void syncOrdersForAllTrendyolStores() {
        log.info("Starting scheduled order sync for all Trendyol stores at 6:15 AM Turkey time");
        
        try {
            // Get all Trendyol stores (case-insensitive)
            List<Store> trendyolStores = storeRepository.findByMarketplaceIgnoreCase("trendyol");
            
            log.info("Found {} Trendyol stores for order sync", trendyolStores.size());
            
            int successCount = 0;
            int errorCount = 0;
            
            for (Store store : trendyolStores) {
                try {
                    log.info("Syncing orders for store: {} ({})", store.getStoreName(), store.getId());
                    orderService.fetchAndSaveOrdersForStore(store.getId());
                    successCount++;
                    log.info("Successfully synced orders for store: {}", store.getStoreName());
                } catch (Exception e) {
                    errorCount++;
                    log.error("Failed to sync orders for store {} ({}): {}", 
                             store.getStoreName(), store.getId(), e.getMessage(), e);
                }
            }
            
            log.info("Completed scheduled order sync: {} successful, {} errors", successCount, errorCount);
            
        } catch (Exception e) {
            log.error("Error during scheduled order sync: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Manual sync for all Trendyol stores (can be called via endpoint)
     */
    public void manualSyncAllStores() {
        log.info("Starting manual order sync for all Trendyol stores");
        syncOrdersForAllTrendyolStores();
    }
}
