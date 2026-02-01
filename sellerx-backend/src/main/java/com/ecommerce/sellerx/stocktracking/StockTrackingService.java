package com.ecommerce.sellerx.stocktracking;

import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockTrackingService {

    private final StockTrackedProductRepository trackedProductRepository;
    private final StockSnapshotRepository snapshotRepository;
    private final StockAlertRepository alertRepository;
    private final StoreRepository storeRepository;
    private final TrendyolPublicStockClient stockClient;

    private static final int MAX_TRACKED_PRODUCTS = 10;
    private static final int HISTORY_DAYS = 30;
    private static final int RECENT_ALERTS_LIMIT = 10;

    /**
     * Add a product to track
     */
    @Transactional
    public TrackedProductDto addProductToTrack(UUID storeId, AddTrackedProductRequest request) {
        // Validate store exists
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));

        // Validate URL
        if (!stockClient.isValidProductUrl(request.getProductUrl())) {
            throw new IllegalArgumentException("Invalid Trendyol product URL");
        }

        // Extract product ID
        Long productId = stockClient.extractProductId(request.getProductUrl());
        if (productId == null) {
            throw new IllegalArgumentException("Could not extract product ID from URL");
        }

        // Check if already tracking
        if (trackedProductRepository.existsByStoreIdAndTrendyolProductId(storeId, productId)) {
            throw new IllegalArgumentException("This product is already being tracked");
        }

        // Check limit
        int currentCount = trackedProductRepository.countByStoreIdAndIsActiveTrue(storeId);
        if (currentCount >= MAX_TRACKED_PRODUCTS) {
            throw new IllegalArgumentException(
                    String.format("Maximum %d products can be tracked per store", MAX_TRACKED_PRODUCTS));
        }

        // Fetch initial stock data
        TrendyolPublicStockClient.StockData stockData = stockClient.fetchStock(request.getProductUrl());
        if (stockData == null) {
            throw new RuntimeException("Could not fetch stock data from Trendyol");
        }

        // Create tracked product
        StockTrackedProduct product = StockTrackedProduct.builder()
                .store(store)
                .trendyolProductId(productId)
                .productUrl(request.getProductUrl())
                .productName(stockData.getProductName())
                .brandName(stockData.getBrandName())
                .imageUrl(stockData.getImageUrl())
                .lastStockQuantity(stockData.getQuantity())
                .lastPrice(stockData.getPrice())
                .lastCheckedAt(LocalDateTime.now())
                .isActive(true)
                .alertOnOutOfStock(request.getAlertOnOutOfStock() != null ? request.getAlertOnOutOfStock() : true)
                .alertOnLowStock(request.getAlertOnLowStock() != null ? request.getAlertOnLowStock() : true)
                .lowStockThreshold(request.getLowStockThreshold() != null ? request.getLowStockThreshold() : 10)
                .alertOnStockIncrease(request.getAlertOnStockIncrease() != null ? request.getAlertOnStockIncrease() : false)
                .alertOnBackInStock(request.getAlertOnBackInStock() != null ? request.getAlertOnBackInStock() : true)
                .build();

        product = trackedProductRepository.save(product);

        // Create initial snapshot
        StockSnapshot snapshot = StockSnapshot.create(
                product,
                stockData.getQuantity(),
                stockData.getInStock(),
                stockData.getPrice()
        );
        snapshotRepository.save(snapshot);

        log.info("Added product to track: {} for store: {}", productId, storeId);

        return TrackedProductDto.fromEntity(product);
    }

    /**
     * Remove a product from tracking
     */
    @Transactional
    public void removeProduct(UUID trackedProductId, UUID storeId) {
        StockTrackedProduct product = trackedProductRepository.findByIdWithStore(trackedProductId)
                .orElseThrow(() -> new IllegalArgumentException("Tracked product not found"));

        // Security check
        if (!product.getStore().getId().equals(storeId)) {
            throw new SecurityException("Product does not belong to this store");
        }

        // Cascade delete handles snapshots and alerts
        trackedProductRepository.delete(product);

        log.info("Removed tracked product: {} from store: {}", trackedProductId, storeId);
    }

    /**
     * Get all tracked products for a store
     */
    @Transactional(readOnly = true)
    public List<TrackedProductDto> getTrackedProducts(UUID storeId) {
        List<StockTrackedProduct> products = trackedProductRepository
                .findByStoreIdAndIsActiveTrueOrderByCreatedAtDesc(storeId);

        return products.stream()
                .map(p -> {
                    TrackedProductDto dto = TrackedProductDto.fromEntity(p);
                    dto.setUnreadAlertCount(alertRepository.countByStoreIdAndIsReadFalse(storeId));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get detailed view of a tracked product
     */
    @Transactional(readOnly = true)
    public TrackedProductDetailDto getProductDetail(UUID trackedProductId, UUID storeId) {
        StockTrackedProduct product = trackedProductRepository.findByIdWithStore(trackedProductId)
                .orElseThrow(() -> new IllegalArgumentException("Tracked product not found"));

        // Security check
        if (!product.getStore().getId().equals(storeId)) {
            throw new SecurityException("Product does not belong to this store");
        }

        // Get recent snapshots (last 30 days)
        LocalDateTime since = LocalDateTime.now().minusDays(HISTORY_DAYS);
        List<StockSnapshotDto> snapshots = snapshotRepository
                .findRecentSnapshots(trackedProductId, since)
                .stream()
                .map(StockSnapshotDto::fromEntity)
                .collect(Collectors.toList());

        // Get recent alerts
        List<StockAlertDto> alerts = alertRepository
                .findByTrackedProductIdOrderByCreatedAtDesc(trackedProductId)
                .stream()
                .limit(RECENT_ALERTS_LIMIT)
                .map(StockAlertDto::fromEntity)
                .collect(Collectors.toList());

        // Calculate statistics
        TrackedProductDetailDto.StockStatistics stats = calculateStatistics(snapshots);

        return TrackedProductDetailDto.fromEntity(product, snapshots, alerts, stats);
    }

    /**
     * Update alert settings
     */
    @Transactional
    public TrackedProductDto updateAlertSettings(UUID trackedProductId, UUID storeId, UpdateAlertSettingsRequest request) {
        StockTrackedProduct product = trackedProductRepository.findByIdWithStore(trackedProductId)
                .orElseThrow(() -> new IllegalArgumentException("Tracked product not found"));

        // Security check
        if (!product.getStore().getId().equals(storeId)) {
            throw new SecurityException("Product does not belong to this store");
        }

        // Update settings
        if (request.getAlertOnOutOfStock() != null) {
            product.setAlertOnOutOfStock(request.getAlertOnOutOfStock());
        }
        if (request.getAlertOnLowStock() != null) {
            product.setAlertOnLowStock(request.getAlertOnLowStock());
        }
        if (request.getLowStockThreshold() != null) {
            product.setLowStockThreshold(request.getLowStockThreshold());
        }
        if (request.getAlertOnStockIncrease() != null) {
            product.setAlertOnStockIncrease(request.getAlertOnStockIncrease());
        }
        if (request.getAlertOnBackInStock() != null) {
            product.setAlertOnBackInStock(request.getAlertOnBackInStock());
        }
        if (request.getIsActive() != null) {
            product.setIsActive(request.getIsActive());
        }

        product = trackedProductRepository.save(product);

        log.info("Updated alert settings for product: {}", trackedProductId);

        return TrackedProductDto.fromEntity(product);
    }

    /**
     * Manually trigger a stock check
     */
    @Transactional
    public TrackedProductDto checkStockNow(UUID trackedProductId, UUID storeId) {
        StockTrackedProduct product = trackedProductRepository.findByIdWithStore(trackedProductId)
                .orElseThrow(() -> new IllegalArgumentException("Tracked product not found"));

        // Security check
        if (!product.getStore().getId().equals(storeId)) {
            throw new SecurityException("Product does not belong to this store");
        }

        // Fetch current stock
        TrendyolPublicStockClient.StockData stockData = stockClient.fetchStock(product.getProductUrl());
        if (stockData == null) {
            throw new RuntimeException("Could not fetch stock data from Trendyol");
        }

        // Process stock change
        processStockChange(product, stockData);

        return TrackedProductDto.fromEntity(product);
    }

    /**
     * Process stock change and create alerts if needed
     */
    @Transactional
    public void processStockChange(StockTrackedProduct product, TrendyolPublicStockClient.StockData stockData) {
        Integer oldQty = product.getLastStockQuantity();
        int newQty = stockData.getQuantity();

        // Create snapshot
        StockSnapshot snapshot = StockSnapshot.create(
                product,
                newQty,
                stockData.getInStock(),
                stockData.getPrice()
        );
        snapshotRepository.save(snapshot);

        // Update product state
        product.setLastStockQuantity(newQty);
        product.setLastPrice(stockData.getPrice());
        product.setLastCheckedAt(LocalDateTime.now());

        // Update product info if changed
        if (stockData.getProductName() != null) {
            product.setProductName(stockData.getProductName());
        }
        if (stockData.getImageUrl() != null) {
            product.setImageUrl(stockData.getImageUrl());
        }

        trackedProductRepository.save(product);

        // Check and create alerts
        checkAndCreateAlerts(product, oldQty, newQty);
    }

    /**
     * Check conditions and create alerts
     */
    private void checkAndCreateAlerts(StockTrackedProduct product, Integer oldQty, int newQty) {
        if (oldQty == null) {
            return; // First check, no comparison possible
        }

        // OUT_OF_STOCK: Stock became zero
        if (oldQty > 0 && newQty == 0 && product.getAlertOnOutOfStock()) {
            StockAlert alert = StockAlert.outOfStock(product, oldQty);
            alertRepository.save(alert);
            log.info("Created OUT_OF_STOCK alert for product: {}", product.getId());
        }

        // BACK_IN_STOCK: Stock returned after being zero
        if (oldQty == 0 && newQty > 0 && product.getAlertOnBackInStock()) {
            StockAlert alert = StockAlert.backInStock(product, newQty);
            alertRepository.save(alert);
            log.info("Created BACK_IN_STOCK alert for product: {}", product.getId());
        }

        // LOW_STOCK: Stock fell below threshold (but not zero)
        if (newQty > 0 && newQty <= product.getLowStockThreshold()
                && oldQty > product.getLowStockThreshold()
                && product.getAlertOnLowStock()) {
            StockAlert alert = StockAlert.lowStock(product, oldQty, newQty);
            alertRepository.save(alert);
            log.info("Created LOW_STOCK alert for product: {}", product.getId());
        }

        // STOCK_INCREASED: Significant increase (>50%)
        if (product.getAlertOnStockIncrease() && oldQty > 0 && newQty > oldQty * 1.5) {
            StockAlert alert = StockAlert.stockIncreased(product, oldQty, newQty);
            alertRepository.save(alert);
            log.info("Created STOCK_INCREASED alert for product: {}", product.getId());
        }
    }

    /**
     * Get alerts for a store
     */
    @Transactional(readOnly = true)
    public List<StockAlertDto> getAlerts(UUID storeId, boolean unreadOnly) {
        List<StockAlert> alerts;
        if (unreadOnly) {
            alerts = alertRepository.findByStoreIdAndIsReadFalseOrderByCreatedAtDesc(storeId);
        } else {
            alerts = alertRepository.findRecentAlerts(storeId, LocalDateTime.now().minusDays(7));
        }

        return alerts.stream()
                .map(StockAlertDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Mark alert as read
     */
    @Transactional
    public void markAlertAsRead(UUID alertId, UUID storeId) {
        StockAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found"));

        if (!alert.getStore().getId().equals(storeId)) {
            throw new SecurityException("Alert does not belong to this store");
        }

        alert.markAsRead();
        alertRepository.save(alert);
    }

    /**
     * Mark all alerts as read for a store
     */
    @Transactional
    public int markAllAlertsAsRead(UUID storeId) {
        return alertRepository.markAllAsReadForStore(storeId);
    }

    /**
     * Get dashboard summary
     */
    @Transactional(readOnly = true)
    public StockTrackingDashboardDto getDashboard(UUID storeId) {
        List<StockTrackedProduct> products = trackedProductRepository
                .findByStoreIdOrderByCreatedAtDesc(storeId);

        int total = products.size();
        int active = (int) products.stream().filter(StockTrackedProduct::getIsActive).count();
        int outOfStock = (int) products.stream()
                .filter(p -> p.getLastStockQuantity() != null && p.getLastStockQuantity() == 0)
                .count();
        int lowStock = (int) products.stream()
                .filter(p -> p.getLastStockQuantity() != null
                        && p.getLastStockQuantity() > 0
                        && p.getLastStockQuantity() <= p.getLowStockThreshold())
                .count();

        LocalDateTime since24h = LocalDateTime.now().minusHours(24);

        return StockTrackingDashboardDto.builder()
                .totalTrackedProducts(total)
                .activeTrackedProducts(active)
                .outOfStockProducts(outOfStock)
                .lowStockProducts(lowStock)
                .outOfStockAlertsToday(alertRepository.countByStoreAndTypeAndSince(storeId, StockAlertType.OUT_OF_STOCK, since24h))
                .lowStockAlertsToday(alertRepository.countByStoreAndTypeAndSince(storeId, StockAlertType.LOW_STOCK, since24h))
                .backInStockAlertsToday(alertRepository.countByStoreAndTypeAndSince(storeId, StockAlertType.BACK_IN_STOCK, since24h))
                .totalUnreadAlerts(alertRepository.countByStoreIdAndIsReadFalse(storeId))
                .recentAlerts(getAlerts(storeId, false).stream().limit(5).collect(Collectors.toList()))
                .outOfStockList(products.stream()
                        .filter(p -> p.getLastStockQuantity() != null && p.getLastStockQuantity() == 0)
                        .map(TrackedProductDto::fromEntity)
                        .collect(Collectors.toList()))
                .lowStockList(products.stream()
                        .filter(p -> p.getLastStockQuantity() != null
                                && p.getLastStockQuantity() > 0
                                && p.getLastStockQuantity() <= p.getLowStockThreshold())
                        .map(TrackedProductDto::fromEntity)
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * Calculate statistics from snapshots
     */
    private TrackedProductDetailDto.StockStatistics calculateStatistics(List<StockSnapshotDto> snapshots) {
        if (snapshots.isEmpty()) {
            return TrackedProductDetailDto.StockStatistics.builder().build();
        }

        int min = snapshots.stream().mapToInt(StockSnapshotDto::getQuantity).min().orElse(0);
        int max = snapshots.stream().mapToInt(StockSnapshotDto::getQuantity).max().orElse(0);
        double avg = snapshots.stream().mapToInt(StockSnapshotDto::getQuantity).average().orElse(0);
        int outOfStockCount = (int) snapshots.stream().filter(s -> s.getQuantity() == 0).count();

        LocalDateTime lastOutOfStock = snapshots.stream()
                .filter(s -> s.getQuantity() == 0)
                .map(StockSnapshotDto::getCheckedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return TrackedProductDetailDto.StockStatistics.builder()
                .minStock(min)
                .maxStock(max)
                .avgStock(avg)
                .totalChecks(snapshots.size())
                .outOfStockCount(outOfStockCount)
                .lastOutOfStock(lastOutOfStock)
                .build();
    }
}
