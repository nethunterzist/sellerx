package com.ecommerce.sellerx.stocktracking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled service for periodic stock checks.
 * Runs every hour to check all tracked products that need updating.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockTrackingScheduledService {

    private final StockTrackedProductRepository trackedProductRepository;
    private final StockTrackingService stockTrackingService;
    private final TrendyolPublicStockClient stockClient;
    private final StockSnapshotRepository snapshotRepository;

    // Cleanup: Keep snapshots for 30 days
    private static final int SNAPSHOT_RETENTION_DAYS = 30;

    /**
     * Check all tracked products every hour.
     * Runs at minute 30 of every hour to spread load.
     * Cron: second minute hour day month weekday
     */
    @Scheduled(cron = "0 30 * * * ?", zone = "Europe/Istanbul")
    public void checkAllTrackedProducts() {
        log.info("Starting scheduled stock check at {}", LocalDateTime.now());

        List<StockTrackedProduct> products = trackedProductRepository.findProductsNeedingCheck();
        log.info("Found {} products needing stock check", products.size());

        int successCount = 0;
        int failCount = 0;

        for (StockTrackedProduct product : products) {
            try {
                checkSingleProduct(product);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to check stock for product: {} ({})",
                        product.getId(), product.getProductName(), e);
                failCount++;
            }
        }

        log.info("Scheduled stock check completed. Success: {}, Failed: {}", successCount, failCount);
    }

    /**
     * Check a single product's stock
     */
    private void checkSingleProduct(StockTrackedProduct product) {
        log.debug("Checking stock for product: {} ({})", product.getId(), product.getProductName());

        TrendyolPublicStockClient.StockData stockData = stockClient.fetchStock(product.getProductUrl());

        if (stockData == null) {
            log.warn("Could not fetch stock data for product: {}", product.getId());
            return;
        }

        stockTrackingService.processStockChange(product, stockData);

        log.debug("Stock check complete for product: {}, quantity: {}",
                product.getId(), stockData.getQuantity());
    }

    /**
     * Manual trigger for testing or admin use
     */
    public void triggerManualCheck() {
        log.info("Manual stock check triggered");
        checkAllTrackedProducts();
    }

    /**
     * Cleanup old snapshots (runs daily at 3 AM)
     */
    @Scheduled(cron = "0 0 3 * * ?", zone = "Europe/Istanbul")
    public void cleanupOldSnapshots() {
        log.info("Starting snapshot cleanup");

        LocalDateTime cutoff = LocalDateTime.now().minusDays(SNAPSHOT_RETENTION_DAYS);

        List<StockTrackedProduct> products = trackedProductRepository.findAllActiveProducts();
        int totalDeleted = 0;

        for (StockTrackedProduct product : products) {
            try {
                int deleted = snapshotRepository.deleteOldSnapshots(product.getId(), cutoff);
                totalDeleted += deleted;
            } catch (Exception e) {
                log.error("Failed to cleanup snapshots for product: {}", product.getId(), e);
            }
        }

        log.info("Snapshot cleanup completed. Deleted {} old snapshots", totalDeleted);
    }
}
