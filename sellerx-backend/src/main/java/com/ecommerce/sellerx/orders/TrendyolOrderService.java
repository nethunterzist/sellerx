package com.ecommerce.sellerx.orders;

import com.ecommerce.sellerx.common.exception.InvalidStoreConfigurationException;
import com.ecommerce.sellerx.common.exception.ResourceNotFoundException;
import com.ecommerce.sellerx.common.exception.TrendyolApiException;
import com.ecommerce.sellerx.config.FinancialConstants;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.stores.TrendyolCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendyolOrderService {

    private static final String TRENDYOL_BASE_URL = "https://apigw.trendyol.com";

    private final TrendyolOrderRepository orderRepository;
    private final StoreRepository storeRepository;
    private final TrendyolProductRepository productRepository;
    private final TrendyolOrderMapper orderMapper;
    private final RestTemplate restTemplate;
    private final OrderCostCalculator costCalculator;
    private final StockOrderSynchronizationService stockOrderSyncService;
    private final CommissionEstimationService commissionEstimationService;
    private final ShippingCostEstimationService shippingCostEstimationService;
    private final OrderGapAnalysisService gapAnalysisService;

    /**
     * Fetch and save orders for a specific store from Trendyol API
     */
    @Transactional
    public void fetchAndSaveOrdersForStore(UUID storeId) {
        fetchAndSaveOrdersForStore(storeId, false);
    }

    /**
     * Fetch and save orders for a specific store from Trendyol API
     *
     * @param storeId Store ID
     * @param skipCommissionCalculation If true, skip commission calculation during onboarding
     */
    @Transactional
    public void fetchAndSaveOrdersForStore(UUID storeId, boolean skipCommissionCalculation) {
        log.info("Starting to fetch orders for store: {} (skipCommissionCalculation: {})", storeId, skipCommissionCalculation);
        
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));

        if (!"trendyol".equalsIgnoreCase(store.getMarketplace())) {
            throw InvalidStoreConfigurationException.notTrendyolStore(storeId.toString());
        }

        TrendyolCredentials credentials = extractTrendyolCredentials(store);
        if (credentials == null) {
            throw InvalidStoreConfigurationException.missingCredentials(storeId.toString());
        }
        
        try {
            // Calculate date ranges - fetch last 3 months in 15-day chunks (GMT+3)
            LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Istanbul"));
            LocalDateTime startDate = now.minusMonths(3);
            
            int totalSaved = 0;
            int totalSkipped = 0;
            
            // Process in 15-day chunks from 3 months ago to now
            LocalDateTime currentStart = startDate;
            
            while (currentStart.isBefore(now)) {
                LocalDateTime currentEnd = currentStart.plusDays(15);
                if (currentEnd.isAfter(now)) {
                    currentEnd = now;
                }
                
                log.info("Fetching orders for store {} from {} to {}", storeId, currentStart, currentEnd);
                
                // Convert to GMT+3 milliseconds
                long startMillis = currentStart.atZone(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();
                long endMillis = currentEnd.atZone(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();
                
                // Fetch all pages for this date range
                int[] results = fetchOrdersForDateRange(credentials, storeId, store, startMillis, endMillis, skipCommissionCalculation);
                totalSaved += results[0];
                totalSkipped += results[1];
                
                currentStart = currentEnd;
                
                // Small delay to avoid rate limiting
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during order sync", e);
                }
            }
            
            log.info("Completed order fetch for store {}: {} total saved, {} total skipped", 
                    storeId, totalSaved, totalSkipped);
            
        } catch (Exception e) {
            log.error("Error fetching orders for store {}: {}", storeId, e.getMessage(), e);
            throw TrendyolApiException.orderFetchFailed(storeId.toString(), e);
        }
    }

    /**
     * Fetch and save orders for a specific store within a given time range.
     * Used by catch-up polling to sync recent orders (e.g., last 2 hours).
     *
     * @param storeId Store ID
     * @param startTime Start of the time range
     * @param endTime End of the time range
     */
    @Transactional
    public void fetchAndSaveOrdersForStoreInRange(UUID storeId, LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("Fetching orders for store {} from {} to {}", storeId, startTime, endTime);

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));

        if (!"trendyol".equalsIgnoreCase(store.getMarketplace())) {
            throw InvalidStoreConfigurationException.notTrendyolStore(storeId.toString());
        }

        TrendyolCredentials credentials = extractTrendyolCredentials(store);
        if (credentials == null) {
            throw InvalidStoreConfigurationException.missingCredentials(storeId.toString());
        }

        try {
            long startMillis = startTime.atZone(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();
            long endMillis = endTime.atZone(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();

            int[] results = fetchOrdersForDateRange(credentials, storeId, store, startMillis, endMillis, false);
            log.debug("Catch-up sync for store {}: {} saved, {} skipped", storeId, results[0], results[1]);

        } catch (Exception e) {
            log.error("Error during catch-up sync for store {}: {}", storeId, e.getMessage());
            throw TrendyolApiException.orderFetchFailed(storeId.toString(), e);
        }
    }

    /**
     * Fetch all orders for a specific date range with pagination
     */
    private int[] fetchOrdersForDateRange(TrendyolCredentials credentials, UUID storeId, Store store, 
                                         long startDate, long endDate, boolean skipCommissionCalculation) {
        int savedCount = 0;
        int skippedCount = 0;
        int page = 0;
        boolean hasMorePages = true;
        
        // Pre-load all products for this store to avoid N+1 queries
        Map<String, TrendyolProduct> productCache = productRepository.findByStoreId(storeId)
                .stream()
                .filter(p -> p.getBarcode() != null && !p.getBarcode().isEmpty())
                .collect(Collectors.toMap(TrendyolProduct::getBarcode, p -> p));
        
        log.info("Loaded {} products into cache for store {}", productCache.size(), storeId);
        
        while (hasMorePages) {
            try {
                TrendyolOrderApiResponse apiResponse = fetchOrdersFromTrendyol(credentials, page, 200, startDate, endDate);
                
                if (apiResponse == null || apiResponse.getContent() == null || apiResponse.getContent().isEmpty()) {
                    break;
                }
                
                if (page % 10 == 0) { // Log progress every 10 pages
                    log.info("Processing page {} with {} orders", page, apiResponse.getContent().size());
                }
                
                // Batch process orders
                List<TrendyolOrder> ordersToSave = new ArrayList<>();
                Set<String> existingPackages = new HashSet<>();
                
                // Check existing orders in batch
                List<Long> packageNumbers = apiResponse.getContent().stream()
                        .filter(order -> order.getId() != null)
                        .map(TrendyolOrderApiResponse.TrendyolOrderContent::getId)
                        .collect(Collectors.toList());

                // Map to track existing orders that need city update
                Map<Long, TrendyolOrder> existingOrdersMap = new HashMap<>();
                if (!packageNumbers.isEmpty()) {
                    Set<Long> existingPackageSet = new HashSet<>(orderRepository.findExistingPackageNumbers(storeId, packageNumbers));
                    existingPackages = existingPackageSet.stream().map(String::valueOf).collect(Collectors.toSet());

                    // Find existing orders that need city update
                    List<TrendyolOrder> existingOrders = orderRepository.findByStoreIdAndPackageNoIn(storeId, packageNumbers);
                    for (TrendyolOrder order : existingOrders) {
                        if (order.getShipmentCity() == null) {
                            existingOrdersMap.put(order.getPackageNo(), order);
                        }
                    }
                }

                // List to track orders that need city update
                List<TrendyolOrder> ordersToUpdate = new ArrayList<>();

                // Process orders in this page
                for (TrendyolOrderApiResponse.TrendyolOrderContent orderContent : apiResponse.getContent()) {
                    try {
                        // Skip orders without cargoTrackingNumber (package number)
                        if (orderContent.getCargoTrackingNumber() == null || orderContent.getId() == null) {
                            skippedCount++;
                            continue;
                        }

                        // Check if existing order needs city update
                        TrendyolOrder existingOrder = existingOrdersMap.get(orderContent.getId());
                        if (existingOrder != null && orderContent.getShipmentAddress() != null) {
                            TrendyolOrderApiResponse.ShipmentAddress address = orderContent.getShipmentAddress();
                            existingOrder.setShipmentCity(address.getCity());
                            existingOrder.setShipmentCityCode(address.getCityCode());
                            existingOrder.setShipmentDistrict(address.getDistrict());
                            existingOrder.setShipmentDistrictId(address.getDistrictId());
                            ordersToUpdate.add(existingOrder);
                        }

                        // Check if order already exists (from batch check)
                        if (existingPackages.contains(orderContent.getId().toString())) {
                            skippedCount++;
                            continue;
                        }
                        
                        // Skip UnPacked orders (these are original packages that will be split)
                        if ("UnPacked".equals(orderContent.getStatus())) {
                            log.debug("Skipping UnPacked order {}", orderContent.getId());
                            skippedCount++;
                            continue;
                        }
                        
                        // Convert order using product cache
                        TrendyolOrder order = convertApiResponseToEntity(orderContent, store, productCache, skipCommissionCalculation);
                        ordersToSave.add(order);
                        savedCount++;
                        
                    } catch (Exception e) {
                        log.error("Error processing order {}: {}", orderContent.getOrderNumber(), e.getMessage());
                        skippedCount++;
                    }
                }
                
                // Batch save new orders
                if (!ordersToSave.isEmpty()) {
                    orderRepository.saveAll(ordersToSave);
                    if (page % 10 == 0) {
                        log.info("Saved batch of {} orders", ordersToSave.size());
                    }

                    // Trigger stock-order synchronization for newly saved orders
                    try {
                        stockOrderSyncService.synchronizeOrdersAfterStockChange(storeId, null);
                        log.debug("Triggered stock-order synchronization after saving {} orders", ordersToSave.size());
                    } catch (Exception e) {
                        log.warn("Failed to synchronize stock-order after saving orders: {}", e.getMessage());
                    }
                }

                // Batch update existing orders with city information
                if (!ordersToUpdate.isEmpty()) {
                    orderRepository.saveAll(ordersToUpdate);
                    log.info("Updated city information for {} existing orders", ordersToUpdate.size());
                }
                
                // Check if we have more pages
                hasMorePages = (page + 1) < apiResponse.getTotalPages();
                page++;
                
                // Small delay between pages
                if (hasMorePages) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during order fetch", e);
                    }
                }
                
            } catch (Exception e) {
                log.error("Error fetching page {} for date range: {}", page, e.getMessage(), e);
                break;
            }
        }
        
        return new int[]{savedCount, skippedCount};
    }
    
    /**
     * Get orders for a store with pagination
     */
    public Page<TrendyolOrderDto> getOrdersForStore(UUID storeId, Pageable pageable) {
        Page<TrendyolOrder> orders = orderRepository.findByStoreIdOrderByOrderDateDesc(storeId, pageable);
        return orders.map(orderMapper::toDto);
    }
    
    /**
     * Get orders for store by date range
     */
    public Page<TrendyolOrderDto> getOrdersForStoreByDateRange(UUID storeId, 
                                                              LocalDateTime startDate, 
                                                              LocalDateTime endDate, 
                                                              Pageable pageable) {
        Page<TrendyolOrder> orders = orderRepository.findByStoreAndDateRange(storeId, startDate, endDate, pageable);
        return orders.map(orderMapper::toDto);
    }
    
    /**
     * Get orders by status
     */
    public Page<TrendyolOrderDto> getOrdersByStatus(UUID storeId, String status, Pageable pageable) {
        Page<TrendyolOrder> orders = orderRepository.findByStoreAndStatus(storeId, status, pageable);
        return orders.map(orderMapper::toDto);
    }
    
    /**
     * Enrich historical orders (SETTLEMENT_API source) with operational data from Orders API.
     * Fetches orders in batches by date range instead of one-by-one for better performance.
     * Only enriches orders from the last 90 days (Orders API limit).
     *
     * @param storeId Store ID
     */
    @Transactional
    public void enrichHistoricalOrdersWithOperationalData(UUID storeId) {
        log.info("Starting enrichment of historical orders for store: {}", storeId);
        
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));
        
        TrendyolCredentials credentials = extractTrendyolCredentials(store);
        if (credentials == null) {
            throw InvalidStoreConfigurationException.missingCredentials(storeId.toString());
        }
        
        // Son 90 günün tarih aralığı
        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
        
        // SETTLEMENT_API kaynaklı siparişleri bul (son 90 gün)
        List<TrendyolOrder> historicalOrders = orderRepository
            .findByStoreIdAndDataSourceAndOrderDateAfter(storeId, "SETTLEMENT_API", ninetyDaysAgo);
        
        if (historicalOrders.isEmpty()) {
            log.info("No historical orders to enrich for store: {}", storeId);
            return;
        }
        
        log.info("Found {} historical orders to enrich for store: {}", historicalOrders.size(), storeId);
        
        // Historical orders'ı packageNo'ya göre indexle (in-memory eşleştirme için)
        Map<Long, TrendyolOrder> ordersByPackageNo = new HashMap<>();
        for (TrendyolOrder order : historicalOrders) {
            ordersByPackageNo.put(order.getPackageNo(), order);
        }
        
        // Orders API'den toplu çekme (15 günlük chunk'larda)
        LocalDateTime currentStart = ninetyDaysAgo;
        LocalDateTime now = LocalDateTime.now();
        int enrichedCount = 0;
        
        while (currentStart.isBefore(now)) {
            LocalDateTime currentEnd = currentStart.plusDays(15);
            if (currentEnd.isAfter(now)) {
                currentEnd = now;
            }
            
            log.debug("Fetching orders from Orders API for enrichment: {} to {}", currentStart, currentEnd);
            
            // Orders API'den bu tarih aralığındaki tüm siparişleri çek
            long startMillis = currentStart.atZone(ZoneId.of("Europe/Istanbul"))
                .toInstant().toEpochMilli();
            long endMillis = currentEnd.atZone(ZoneId.of("Europe/Istanbul"))
                .toInstant().toEpochMilli();
            
            try {
                // Use existing fetchOrdersForDateRange method but get raw API response
                // We'll process it differently - just match and enrich
                TrendyolOrderApiResponse apiResponse = fetchOrdersFromTrendyol(
                    credentials, 0, 200, startMillis, endMillis);
                
                if (apiResponse != null && apiResponse.getContent() != null) {
                    // Eşleştir ve zenginleştir
                    for (TrendyolOrderApiResponse.TrendyolOrderContent apiOrderContent : apiResponse.getContent()) {
                        if (apiOrderContent.getId() == null) {
                            continue;
                        }
                        
                        TrendyolOrder historicalOrder = ordersByPackageNo.get(apiOrderContent.getId());
                        
                        if (historicalOrder != null && "SETTLEMENT_API".equals(historicalOrder.getDataSource())) {
                            // Operasyonel verileri merge et
                            enrichOrderWithOperationalData(historicalOrder, apiOrderContent, store);
                            orderRepository.save(historicalOrder);
                            enrichedCount++;
                        }
                    }
                    
                    // Handle pagination if needed
                    if (apiResponse.getTotalPages() != null && apiResponse.getTotalPages() > 1) {
                        for (int page = 1; page < apiResponse.getTotalPages(); page++) {
                            TrendyolOrderApiResponse pageResponse = fetchOrdersFromTrendyol(
                                credentials, page, 200, startMillis, endMillis);
                            
                            if (pageResponse != null && pageResponse.getContent() != null) {
                                for (TrendyolOrderApiResponse.TrendyolOrderContent apiOrderContent : pageResponse.getContent()) {
                                    if (apiOrderContent.getId() == null) {
                                        continue;
                                    }
                                    
                                    TrendyolOrder historicalOrder = ordersByPackageNo.get(apiOrderContent.getId());
                                    
                                    if (historicalOrder != null && "SETTLEMENT_API".equals(historicalOrder.getDataSource())) {
                                        enrichOrderWithOperationalData(historicalOrder, apiOrderContent, store);
                                        orderRepository.save(historicalOrder);
                                        enrichedCount++;
                                    }
                                }
                            }
                            
                            try {
                                Thread.sleep(500); // Rate limiting
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new RuntimeException("Interrupted during order enrichment", e);
                            }
                        }
                    }
                }
                
            } catch (Exception e) {
                log.warn("Failed to enrich orders for date range {} to {}: {}", 
                    currentStart, currentEnd, e.getMessage());
            }
            
            currentStart = currentEnd;
            try {
                Thread.sleep(500); // Rate limiting between chunks
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted during order enrichment", e);
            }
        }
        
        log.info("Enrichment completed for store {}: {} orders enriched", storeId, enrichedCount);
    }

    // ================= HYBRID SYNC - Gap Fill Methods =================

    /**
     * Sync orders from Orders API for the gap period where Settlement API doesn't have data yet.
     *
     * Gap Period = Period between last Settlement order date and today
     * (typically 3-7 days due to Settlement delay)
     *
     * Orders fetched from Orders API will have:
     * - dataSource = "ORDER_API"
     * - isCommissionEstimated = true
     * - estimatedCommission = calculated using barcode reference
     *
     * @param storeId Store ID to sync
     * @return Number of orders synced
     */
    @Transactional
    public int syncGapFromOrdersApi(UUID storeId) {
        log.info("Starting gap sync from Orders API for store: {}", storeId);

        // Analyze the gap
        OrderGapAnalysisService.GapAnalysis gapAnalysis = gapAnalysisService.analyzeGap(storeId);
        OrderGapAnalysisService.SyncRecommendation recommendation = gapAnalysisService.getOrdersApiSyncRecommendation(storeId);

        log.info("Gap analysis for store {}: {} days gap ({} to {}), " +
                        "recommended sync from {} to {}",
                storeId,
                gapAnalysis.getGapDays(),
                gapAnalysis.getGapStartDate(),
                gapAnalysis.getGapEndDate(),
                recommendation.getStartDate(),
                recommendation.getEndDate());

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));

        TrendyolCredentials credentials = extractTrendyolCredentials(store);
        if (credentials == null) {
            throw InvalidStoreConfigurationException.missingCredentials(storeId.toString());
        }

        // Convert dates to milliseconds
        LocalDateTime startDateTime = recommendation.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = recommendation.getEndDate().atTime(23, 59, 59);

        long startMillis = startDateTime.atZone(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();
        long endMillis = endDateTime.atZone(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();

        // Fetch orders with gap-fill mode (forces ORDER_API dataSource)
        int[] results = fetchOrdersForGapFill(credentials, storeId, store, startMillis, endMillis);

        log.info("Gap sync completed for store {}: {} orders synced from Orders API, {} skipped",
                storeId, results[0], results[1]);

        return results[0];
    }

    /**
     * Fetch orders specifically for gap filling.
     * Orders will be marked as ORDER_API source with estimated commission.
     */
    private int[] fetchOrdersForGapFill(TrendyolCredentials credentials, UUID storeId, Store store,
                                        long startDate, long endDate) {
        int savedCount = 0;
        int skippedCount = 0;
        int page = 0;
        boolean hasMorePages = true;

        // Pre-load all products for commission estimation
        Map<String, TrendyolProduct> productCache = productRepository.findByStoreId(storeId)
                .stream()
                .filter(p -> p.getBarcode() != null && !p.getBarcode().isEmpty())
                .collect(Collectors.toMap(TrendyolProduct::getBarcode, p -> p));

        log.info("Loaded {} products into cache for gap fill (store {})", productCache.size(), storeId);

        while (hasMorePages) {
            try {
                TrendyolOrderApiResponse apiResponse = fetchOrdersFromTrendyol(credentials, page, 200, startDate, endDate);

                if (apiResponse == null || apiResponse.getContent() == null || apiResponse.getContent().isEmpty()) {
                    break;
                }

                log.debug("Gap fill processing page {} with {} orders", page, apiResponse.getContent().size());

                List<TrendyolOrder> ordersToSave = new ArrayList<>();

                // Get existing package numbers to skip
                List<Long> packageNumbers = apiResponse.getContent().stream()
                        .filter(order -> order.getId() != null)
                        .map(TrendyolOrderApiResponse.TrendyolOrderContent::getId)
                        .collect(Collectors.toList());

                Set<Long> existingPackages = new HashSet<>();
                if (!packageNumbers.isEmpty()) {
                    existingPackages = new HashSet<>(orderRepository.findExistingPackageNumbers(storeId, packageNumbers));
                }

                for (TrendyolOrderApiResponse.TrendyolOrderContent orderContent : apiResponse.getContent()) {
                    try {
                        if (orderContent.getCargoTrackingNumber() == null || orderContent.getId() == null) {
                            skippedCount++;
                            continue;
                        }

                        // Skip if already exists (from either source)
                        if (existingPackages.contains(orderContent.getId())) {
                            skippedCount++;
                            continue;
                        }

                        // Skip UnPacked orders
                        if ("UnPacked".equals(orderContent.getStatus())) {
                            skippedCount++;
                            continue;
                        }

                        // Convert to entity with ORDER_API source and estimated commission
                        TrendyolOrder order = convertApiResponseToEntityForGapFill(
                                orderContent, store, productCache);
                        ordersToSave.add(order);
                        savedCount++;

                    } catch (Exception e) {
                        log.error("Error processing gap fill order {}: {}", orderContent.getOrderNumber(), e.getMessage());
                        skippedCount++;
                    }
                }

                // Batch save
                if (!ordersToSave.isEmpty()) {
                    orderRepository.saveAll(ordersToSave);
                    log.debug("Gap fill saved {} orders", ordersToSave.size());
                }

                hasMorePages = (page + 1) < apiResponse.getTotalPages();
                page++;

                if (hasMorePages) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during gap fill", e);
                    }
                }

            } catch (Exception e) {
                log.error("Error during gap fill page {}: {}", page, e.getMessage(), e);
                break;
            }
        }

        return new int[]{savedCount, skippedCount};
    }

    /**
     * Convert API response to entity specifically for gap fill.
     * Sets dataSource = ORDER_API and calculates estimated commission.
     */
    private TrendyolOrder convertApiResponseToEntityForGapFill(
            TrendyolOrderApiResponse.TrendyolOrderContent orderContent,
            Store store,
            Map<String, TrendyolProduct> productCache) {

        // Base conversion
        TrendyolOrder order = convertApiResponseToEntity(orderContent, store, productCache, false);

        // Override for gap fill specifics
        order.setDataSource("ORDER_API");
        order.setIsCommissionEstimated(true);

        // Recalculate commission using CommissionEstimationService for accuracy
        BigDecimal estimatedCommission = commissionEstimationService.calculateOrderEstimatedCommission(order);
        order.setEstimatedCommission(estimatedCommission);

        // Estimate shipping cost using ShippingCostEstimationService (lastShippingCostPerUnit reference)
        // Kargo tahmini: ürünün lastShippingCostPerUnit değerini referans alır
        BigDecimal estimatedShipping = shippingCostEstimationService.calculateOrderEstimatedShipping(order);
        order.setEstimatedShippingCost(estimatedShipping);
        order.setIsShippingEstimated(true);

        log.debug("Gap fill order {}: estimatedCommission={}, estimatedShipping={}, dataSource=ORDER_API",
                order.getTyOrderNumber(), estimatedCommission, estimatedShipping);

        return order;
    }

    /**
     * Sync recent orders (last 2 hours) with estimated commission for ongoing gap fill.
     * Called by hourly scheduled job.
     *
     * @param storeId Store ID
     */
    @Transactional
    public void syncRecentOrdersWithEstimation(UUID storeId) {
        log.debug("Syncing recent orders with estimation for store: {}", storeId);

        LocalDateTime endTime = LocalDateTime.now(ZoneId.of("Europe/Istanbul"));
        LocalDateTime startTime = endTime.minusHours(2);

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));

        TrendyolCredentials credentials = extractTrendyolCredentials(store);
        if (credentials == null) {
            throw InvalidStoreConfigurationException.missingCredentials(storeId.toString());
        }

        long startMillis = startTime.atZone(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();
        long endMillis = endTime.atZone(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();

        int[] results = fetchOrdersForGapFill(credentials, storeId, store, startMillis, endMillis);

        log.debug("Recent orders sync for store {}: {} saved, {} skipped", storeId, results[0], results[1]);
    }

    /**
     * Merge operational data from Orders API into historical order.
     * Only fills missing operational fields, preserves financial data.
     * 
     * @param historicalOrder Historical order from SETTLEMENT_API
     * @param apiOrderContent Order data from Orders API
     * @param store Store entity
     */
    private void enrichOrderWithOperationalData(
            TrendyolOrder historicalOrder,
            TrendyolOrderApiResponse.TrendyolOrderContent apiOrderContent,
            Store store) {
        
        // Sadece eksik operasyonel verileri ekle, finansal verileri koru
        if (historicalOrder.getShipmentCity() == null && apiOrderContent.getShipmentAddress() != null) {
            TrendyolOrderApiResponse.ShipmentAddress address = apiOrderContent.getShipmentAddress();
            historicalOrder.setShipmentCity(address.getCity());
            historicalOrder.setShipmentCityCode(address.getCityCode());
            historicalOrder.setShipmentDistrict(address.getDistrict());
            historicalOrder.setShipmentDistrictId(address.getDistrictId());
        }
        
        // Status güncelle (eğer daha güncelse)
        if (apiOrderContent.getStatus() != null && 
            (historicalOrder.getStatus() == null || 
             !historicalOrder.getStatus().equals(apiOrderContent.getStatus()))) {
            historicalOrder.setStatus(apiOrderContent.getStatus());
            if (apiOrderContent.getShipmentPackageStatus() != null) {
                historicalOrder.setShipmentPackageStatus(apiOrderContent.getShipmentPackageStatus());
            }
        }
        
        // Cargo deci güncelle
        if (apiOrderContent.getCargoDeci() != null && apiOrderContent.getCargoDeci() > 0) {
            historicalOrder.setCargoDeci(apiOrderContent.getCargoDeci().intValue());
        }
        
        // Data source'u güncelle: Artık hem SETTLEMENT hem ORDER_API'den veri var
        historicalOrder.setDataSource("HYBRID");
        
        log.debug("Enriched order {} with operational data", historicalOrder.getTyOrderNumber());
    }

    /**
     * Get order statistics for a store
     */
    public OrderStatistics getOrderStatistics(UUID storeId) {
        long totalOrders = orderRepository.countByStoreId(storeId);
        long pendingOrders = orderRepository.countByStoreIdAndStatusIn(storeId, List.of("Created", "Picking", "Invoiced"));
        long shippedOrders = orderRepository.countByStoreIdAndStatus(storeId, "Shipped");
        long deliveredOrders = orderRepository.countByStoreIdAndStatus(storeId, "Delivered");
        long cancelledOrders = orderRepository.countByStoreIdAndStatus(storeId, "Cancelled");
        long returnedOrders = orderRepository.countByStoreIdAndStatus(storeId, "Returned");

        // Calculate total revenue (excluding cancelled and returned orders)
        BigDecimal totalRevenue = orderRepository.sumTotalPriceByStoreIdAndStatusNotIn(storeId, List.of("Cancelled", "Returned"));
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        // Calculate average order value
        long validOrders = totalOrders - cancelledOrders - returnedOrders;
        BigDecimal avgOrderValue = validOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(validOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return OrderStatistics.builder()
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .shippedOrders(shippedOrders)
                .deliveredOrders(deliveredOrders)
                .cancelledOrders(cancelledOrders)
                .returnedOrders(returnedOrders)
                .totalRevenue(totalRevenue)
                .averageOrderValue(avgOrderValue)
                .build();
    }
    
    private TrendyolOrderApiResponse fetchOrdersFromTrendyol(TrendyolCredentials credentials, int page, int size, 
                                                           Long startDate, Long endDate) {
        String auth = credentials.getApiKey() + ":" + credentials.getApiSecret();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.set("User-Agent", credentials.getSellerId() + " - SelfIntegration");
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        // Build URL with date parameters
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(String.format("%s/integration/order/sellers/%s/orders?page=%d&size=%d", 
                TRENDYOL_BASE_URL, credentials.getSellerId(), page, size));
        
        if (startDate != null) {
            urlBuilder.append("&startDate=").append(startDate);
        }
        if (endDate != null) {
            urlBuilder.append("&endDate=").append(endDate);
        }
        
        String url = urlBuilder.toString();
        log.debug("Fetching orders from URL: {}", url);
        
        ResponseEntity<TrendyolOrderApiResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, TrendyolOrderApiResponse.class);
        
        return response.getBody();
    }
    
    private TrendyolOrder convertApiResponseToEntity(TrendyolOrderApiResponse.TrendyolOrderContent orderContent, Store store, Map<String, TrendyolProduct> productCache, boolean skipCommissionCalculation) {
        // Convert milliseconds to LocalDateTime (Trendyol sends in GMT+3, keep it as is)
        LocalDateTime orderDate = Instant.ofEpochMilli(orderContent.getOriginShipmentDate())
                .atZone(ZoneId.of("Europe/Istanbul")) // GMT+3 timezone
                .toLocalDateTime();
        
        // Convert order lines to order items with cost information
        List<OrderItem> orderItems = orderContent.getLines().stream()
                .map(line -> convertLineToOrderItem(line, store.getId(), orderDate, productCache, skipCommissionCalculation))
                .collect(Collectors.toList());
        
        // Calculate total estimated commission
        BigDecimal totalEstimatedCommission = orderItems.stream()
                .filter(item -> item.getUnitEstimatedCommission() != null)
                .map(item -> {
                    BigDecimal unitCommission = item.getUnitEstimatedCommission();
                    int quantity = item.getQuantity() != null ? item.getQuantity() : 1;
                    return unitCommission.multiply(BigDecimal.valueOf(quantity));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Use total price from Trendyol API response
        BigDecimal totalPrice = orderContent.getTotalPrice() != null ? 
                                orderContent.getTotalPrice() : BigDecimal.ZERO;
        
        // Calculate stoppage (withholding tax) as totalPrice * stoppage rate
        BigDecimal stoppage = totalPrice.multiply(FinancialConstants.STOPPAGE_RATE_DECIMAL);

        // Extract shipment address city information
        String shipmentCity = null;
        Integer shipmentCityCode = null;
        String shipmentDistrict = null;
        Integer shipmentDistrictId = null;
        if (orderContent.getShipmentAddress() != null) {
            TrendyolOrderApiResponse.ShipmentAddress address = orderContent.getShipmentAddress();
            shipmentCity = address.getCity();
            shipmentCityCode = address.getCityCode();
            shipmentDistrict = address.getDistrict();
            shipmentDistrictId = address.getDistrictId();
        }

        TrendyolOrder order = TrendyolOrder.builder()
                .store(store)
                .tyOrderNumber(orderContent.getOrderNumber())
                .packageNo(orderContent.getId())
                .orderDate(orderDate)
                .grossAmount(orderContent.getGrossAmount())
                .totalDiscount(orderContent.getTotalDiscount())
                .totalTyDiscount(orderContent.getTotalTyDiscount())
                .totalPrice(totalPrice)
                .stoppage(stoppage)
                .estimatedCommission(totalEstimatedCommission)
                .isCommissionEstimated(true) // Always start as estimated; set to false when real settlement data arrives
                .orderItems(orderItems)
                .shipmentPackageStatus(orderContent.getShipmentPackageStatus())
                .status(orderContent.getStatus())
                .cargoDeci(orderContent.getCargoDeci())
                .shipmentCity(shipmentCity)
                .shipmentCityCode(shipmentCityCode)
                .shipmentDistrict(shipmentDistrict)
                .shipmentDistrictId(shipmentDistrictId)
                .build();

        // Calculate estimated shipping cost using product references (lastShippingCostPerUnit)
        // Kargo tahmini: ürünün lastShippingCostPerUnit değerini referans alır
        if (!skipCommissionCalculation) {
            BigDecimal estimatedShipping = shippingCostEstimationService.calculateOrderEstimatedShipping(order);
            order.setEstimatedShippingCost(estimatedShipping);
            order.setIsShippingEstimated(true);
        }

        return order;
    }
    
    private OrderItem convertLineToOrderItem(TrendyolOrderApiResponse.TrendyolOrderLine line, UUID storeId, LocalDateTime orderDate, Map<String, TrendyolProduct> productCache, boolean skipCommissionCalculation) {
        OrderItem.OrderItemBuilder itemBuilder = OrderItem.builder()
                .barcode(line.getBarcode())
                .productName(line.getProductName())
                .quantity(line.getQuantity())
                .unitPriceOrder(line.getAmount())
                .unitPriceDiscount(line.getDiscount())
                .unitPriceTyDiscount(line.getTyDiscount())
                .vatBaseAmount(line.getVatBaseAmount())
                .price(line.getPrice());
        
        // Use the cost calculator to set cost and commission information
        costCalculator.setCostInfo(itemBuilder, line.getBarcode(), storeId, orderDate, productCache);
        
        // Skip commission calculation during onboarding if requested
        if (!skipCommissionCalculation) {
            // Calculate unit estimated commission with fallback logic
            // Priority: lastCommissionRate (Financial API) → commissionRate (Product API) → 0
            BigDecimal estimatedCommissionRate = BigDecimal.ZERO;
            if (productCache != null && line.getBarcode() != null) {
                TrendyolProduct product = productCache.get(line.getBarcode());
                if (product != null) {
                    estimatedCommissionRate = costCalculator.getEffectiveCommissionRate(product);
                }
            }

            if (estimatedCommissionRate.compareTo(BigDecimal.ZERO) > 0 && line.getPrice() != null) {
                // CORRECTED FORMULA: (price / (1 + vatRate/100)) * commissionRate / 100
                // NOTE: vatBaseAmount is actually VAT RATE (e.g., 20 for 20%), not VAT base amount
                BigDecimal unitEstimatedCommission = costCalculator.calculateUnitEstimatedCommission(
                    line.getPrice(), line.getVatBaseAmount(), estimatedCommissionRate);
                itemBuilder.unitEstimatedCommission(unitEstimatedCommission)
                           .estimatedCommissionRate(estimatedCommissionRate);
            }
        }
        
        // Set estimated shipping volume weight from product if available
        if (productCache != null && line.getBarcode() != null) {
            TrendyolProduct product = productCache.get(line.getBarcode());
            if (product != null && product.getShippingVolumeWeight() != null) {
                itemBuilder.estimatedShippingVolumeWeight(product.getShippingVolumeWeight());
            }
        }
        
        return itemBuilder.build();
    }
    
    private TrendyolCredentials extractTrendyolCredentials(Store store) {
        if (store.getCredentials() instanceof TrendyolCredentials) {
            return (TrendyolCredentials) store.getCredentials();
        }
        return null;
    }
}
