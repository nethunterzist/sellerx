package com.ecommerce.sellerx.financial;

import com.ecommerce.sellerx.common.TrendyolRateLimiter;
import com.ecommerce.sellerx.common.exception.InvalidStoreConfigurationException;
import com.ecommerce.sellerx.common.exception.ResourceNotFoundException;
import com.ecommerce.sellerx.orders.TrendyolOrderRepository;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import com.ecommerce.sellerx.stores.MarketplaceCredentials;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.stores.TrendyolCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import com.ecommerce.sellerx.orders.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendyolFinancialSettlementService {

    private static final String TRENDYOL_BASE_URL = "https://apigw.trendyol.com";
    private static final String SETTLEMENT_ENDPOINT = "/integration/finance/che/sellers/{sellerId}/settlements";

    private final TrendyolOrderRepository orderRepository;
    private final StoreRepository storeRepository;
    private final TrendyolProductRepository productRepository;
    private final TrendyolFinancialSettlementMapper settlementMapper;
    private final RestTemplate restTemplate;
    private final TrendyolRateLimiter rateLimiter;

    /**
     * Fetch settlements for a store and update corresponding orders
     */
    @Transactional
    public void fetchAndUpdateSettlementsForStore(UUID storeId) {
        log.info("[FINANCIAL] Son 3 ayın komisyon pasaportları güncelleniyor...");
        log.info("Starting to fetch settlements for store: {}", storeId);
        
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));

        if (!"trendyol".equalsIgnoreCase(store.getMarketplace())) {
            throw InvalidStoreConfigurationException.notTrendyolStore(storeId.toString());
        }

        TrendyolCredentials credentials = extractTrendyolCredentials(store);
        if (credentials == null || credentials.getSellerId() == null) {
            throw InvalidStoreConfigurationException.missingCredentials(storeId.toString());
        }

        // Fetch settlements for the last 3 months in 15-day intervals
        int updatedProductsCount = fetchSettlementsForLast3Months(store, credentials);
        
        log.info("[FINANCIAL] [COMPLETED] {} ürünün komisyon oranı güncellendi.", updatedProductsCount);
        log.info("Completed fetching settlements for store: {}", storeId);
    }

    /**
     * Fetch settlements for all Trendyol stores with rate limiting.
     * Skips stores that haven't completed initial sync.
     */
    public void fetchAndUpdateSettlementsForAllStores() {
        log.info("Starting to fetch settlements for all Trendyol stores");

        List<Store> trendyolStores = storeRepository.findByMarketplaceIgnoreCase("trendyol");
        int successCount = 0;
        int errorCount = 0;
        int skippedCount = 0;

        for (Store store : trendyolStores) {
            // Skip stores that haven't completed initial sync
            if (!Boolean.TRUE.equals(store.getInitialSyncCompleted())) {
                log.debug("Skipping store {} - initial sync not completed", store.getId());
                skippedCount++;
                continue;
            }

            // Apply rate limiting before each API call (per-store)
            rateLimiter.acquire(store.getId());

            try {
                fetchAndUpdateSettlementsForStore(store.getId());
                successCount++;
            } catch (Exception e) {
                errorCount++;
                log.error("Failed to fetch settlements for store: {}", store.getId(), e);
            }
        }

        log.info("Completed fetching settlements for all Trendyol stores: {} successful, {} errors, {} skipped",
                successCount, errorCount, skippedCount);
    }

    /**
     * Extract TrendyolCredentials from Store
     */
    private TrendyolCredentials extractTrendyolCredentials(Store store) {
        MarketplaceCredentials credentials = store.getCredentials();
        if (credentials instanceof TrendyolCredentials) {
            return (TrendyolCredentials) credentials;
        }
        return null;
    }

    private int fetchSettlementsForLast3Months(Store store, TrendyolCredentials credentials) {
        LocalDateTime now = LocalDateTime.now();
        // Fetch 12 months of data - Trendyol OtherFinancials API supports longer history
        LocalDateTime startDate = now.minusMonths(12);

        // Split the period into 15-day intervals (Trendyol API limit)
        LocalDateTime currentStart = startDate;
        int totalUpdatedProducts = 0;
        
        while (currentStart.isBefore(now)) {
            LocalDateTime currentEnd = currentStart.plusDays(15);
            if (currentEnd.isAfter(now)) {
                currentEnd = now;
            }
            
            try {
                fetchSettlementsForPeriod(store, credentials, currentStart, currentEnd);
                
                // Add a small delay between API calls to avoid rate limiting
                Thread.sleep(500);
            } catch (Exception e) {
                log.error("Failed to fetch settlements for store: {} in period {} - {}", 
                    store.getId(), currentStart, currentEnd, e);
            }
            
            currentStart = currentEnd;
        }
        
        // Count updated products (this is approximate, actual count is tracked in updateProductCommissionRates)
        // We'll return a placeholder for now since tracking exact count would require refactoring
        return 0; // Placeholder - actual count would require tracking across all periods
    }

    private void fetchSettlementsForPeriod(Store store, TrendyolCredentials credentials, 
                                         LocalDateTime startDate, LocalDateTime endDate) {
        log.info("[FINANCIAL] Dönem {} - {} işleniyor...", startDate.toLocalDate(), endDate.toLocalDate());
        log.info("Fetching settlements for store: {} from {} to {}", 
            store.getId(), startDate, endDate);

        long startTimestamp = startDate.atZone(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();
        long endTimestamp = endDate.atZone(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();

        // Create Basic Auth header like other Trendyol API calls
        String auth = credentials.getApiKey() + ":" + credentials.getApiSecret();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.set("User-Agent", credentials.getSellerId() + " - SelfIntegration");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Fetch all settlement types: Sale, Return, Discount, Coupon, EarlyPayment
        String[] transactionTypes = {"Sale", "Return", "Discount", "Coupon", "EarlyPayment"};
        
        for (String transactionType : transactionTypes) {
            log.info("Fetching {} settlements for store: {} from {} to {}", 
                transactionType, store.getId(), startDate, endDate);
                
            fetchSettlementsByType(store, credentials, entity, startTimestamp, endTimestamp, transactionType);
            
            // Add delay between different transaction types
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void fetchSettlementsByType(Store store, TrendyolCredentials credentials, HttpEntity<String> entity,
                                      long startTimestamp, long endTimestamp, String transactionType) {
        // Start with page 0 and fetch all pages
        int currentPage = 0;
        int totalPages = 1; // Start with 1, will be updated from first response
        int totalProcessed = 0;

        while (currentPage < totalPages) {
            String url = TRENDYOL_BASE_URL + SETTLEMENT_ENDPOINT + 
                        "?transactionType=" + transactionType +
                        "&startDate=" + startTimestamp +
                        "&endDate=" + endTimestamp +
                        "&page=" + currentPage +
                        "&size=1000";

            try {
                log.info("Fetching {} settlements page {} of {} for store: {}", 
                    transactionType, currentPage + 1, totalPages, store.getId());
                
                ResponseEntity<TrendyolFinancialSettlementResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    TrendyolFinancialSettlementResponse.class,
                    credentials.getSellerId()
                );

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    TrendyolFinancialSettlementResponse settlementResponse = response.getBody();
                    
                    // Update totalPages from the first response
                    if (currentPage == 0) {
                        totalPages = settlementResponse.getTotalPages() != null ? 
                                   settlementResponse.getTotalPages() : 1;
                        log.info("Total {} pages for {} settlements, total elements: {} for store: {}", 
                            totalPages, transactionType, settlementResponse.getTotalElements(), store.getId());
                    }
                    
                    // Process this page's settlements
                    if (settlementResponse.getContent() != null && !settlementResponse.getContent().isEmpty()) {
                        processSettlementResponse(store, settlementResponse);
                        totalProcessed += settlementResponse.getContent().size();
                    }
                } else {
                    log.warn("Failed to fetch {} settlements page {} for store: {} - Status: {}", 
                        transactionType, currentPage, store.getId(), response.getStatusCode());
                    break; // Stop processing if we get an error
                }

                currentPage++;
                
                // Add a small delay between pages to avoid rate limiting
                if (currentPage < totalPages) {
                    Thread.sleep(200);
                }

            } catch (Exception e) {
                log.error("Error fetching {} settlements page {} for store: {}", 
                    transactionType, currentPage, store.getId(), e);
                break; // Stop processing if we get an error
            }
        }
        
        log.info("Completed fetching {} settlements for store: {} - Processed {} settlements across {} pages", 
            transactionType, store.getId(), totalProcessed, currentPage);
    }

    private void processSettlementResponse(Store store, TrendyolFinancialSettlementResponse response) {
        if (response.getContent() == null || response.getContent().isEmpty()) {
            log.info("No settlements found in this page for store: {}", store.getId());
            return;
        }

        log.info("Processing {} settlement items for store: {}", 
            response.getContent().size(), store.getId());

        // Separate different types of settlements for different processing logic
        Map<String, List<TrendyolFinancialSettlementItem>> saleSettlements = new HashMap<>();
        Map<String, List<TrendyolFinancialSettlementItem>> returnSettlements = new HashMap<>();
        Map<String, List<TrendyolFinancialSettlementItem>> discountSettlements = new HashMap<>();
        Map<String, List<TrendyolFinancialSettlementItem>> couponSettlements = new HashMap<>();
        Map<String, List<TrendyolFinancialSettlementItem>> earlyPaymentSettlements = new HashMap<>();

        for (TrendyolFinancialSettlementItem item : response.getContent()) {
            String key = item.getOrderNumber() + "_" + item.getShipmentPackageId();

            if ("Satış".equals(item.getTransactionType()) || "Sale".equals(item.getTransactionType())) {
                saleSettlements.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
            } else if ("İade".equals(item.getTransactionType()) || "Return".equals(item.getTransactionType())) {
                // For returns, we might need to search by order number only since package might be different
                String returnKey = item.getOrderNumber();
                returnSettlements.computeIfAbsent(returnKey, k -> new ArrayList<>()).add(item);
            } else if ("İndirim".equals(item.getTransactionType()) || "Discount".equals(item.getTransactionType())) {
                // For discounts, use package ID like sales
                discountSettlements.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
            } else if ("Kupon".equals(item.getTransactionType()) || "Coupon".equals(item.getTransactionType())) {
                // For coupons, use package ID like sales
                couponSettlements.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
            } else if ("ErkenÖdeme".equals(item.getTransactionType()) || "EarlyPayment".equals(item.getTransactionType())) {
                // For early payment fees, use package ID like sales
                earlyPaymentSettlements.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
            }
        }

        log.info("Grouped settlements for store: {} - Sales: {}, Returns: {}, Discounts: {}, Coupons: {}, EarlyPayments: {}",
            store.getId(), saleSettlements.size(), returnSettlements.size(),
            discountSettlements.size(), couponSettlements.size(), earlyPaymentSettlements.size());

        int processedOrders = 0;
        int foundOrders = 0;
        
        // Process sale settlements (existing logic)
        for (Map.Entry<String, List<TrendyolFinancialSettlementItem>> entry : saleSettlements.entrySet()) {
            String[] parts = entry.getKey().split("_");
            String orderNumber = parts[0];
            Long packageId = Long.valueOf(parts[1]);
            
            try {
                boolean orderFound = updateOrderWithSettlements(store, orderNumber, packageId, entry.getValue());
                processedOrders++;
                if (orderFound) {
                    foundOrders++;
                }
            } catch (Exception e) {
                log.error("Failed to update order {} package {} with sale settlements", 
                    orderNumber, packageId, e);
            }
        }
        
        // Process return settlements (special logic)
        for (Map.Entry<String, List<TrendyolFinancialSettlementItem>> entry : returnSettlements.entrySet()) {
            String orderNumber = entry.getKey();
            
            try {
                boolean orderFound = updateOrderWithReturnSettlements(store, orderNumber, entry.getValue());
                processedOrders++;
                if (orderFound) {
                    foundOrders++;
                }
            } catch (Exception e) {
                log.error("Failed to update order {} with return settlements", orderNumber, e);
            }
        }
        
        // Process discount settlements (use package ID like sales)
        for (Map.Entry<String, List<TrendyolFinancialSettlementItem>> entry : discountSettlements.entrySet()) {
            String[] parts = entry.getKey().split("_");
            String orderNumber = parts[0];
            Long packageId = Long.valueOf(parts[1]);
            
            try {
                boolean orderFound = updateOrderWithSettlements(store, orderNumber, packageId, entry.getValue());
                processedOrders++;
                if (orderFound) {
                    foundOrders++;
                }
            } catch (Exception e) {
                log.error("Failed to update order {} package {} with discount settlements", 
                    orderNumber, packageId, e);
            }
        }
        
        // Process coupon settlements (use package ID like sales)
        for (Map.Entry<String, List<TrendyolFinancialSettlementItem>> entry : couponSettlements.entrySet()) {
            String[] parts = entry.getKey().split("_");
            String orderNumber = parts[0];
            Long packageId = Long.valueOf(parts[1]);

            try {
                boolean orderFound = updateOrderWithSettlements(store, orderNumber, packageId, entry.getValue());
                processedOrders++;
                if (orderFound) {
                    foundOrders++;
                }
            } catch (Exception e) {
                log.error("Failed to update order {} package {} with coupon settlements",
                    orderNumber, packageId, e);
            }
        }

        // Process early payment settlements (use package ID like sales)
        for (Map.Entry<String, List<TrendyolFinancialSettlementItem>> entry : earlyPaymentSettlements.entrySet()) {
            String[] parts = entry.getKey().split("_");
            String orderNumber = parts[0];
            Long packageId = Long.valueOf(parts[1]);

            try {
                boolean orderFound = updateOrderWithSettlements(store, orderNumber, packageId, entry.getValue());
                processedOrders++;
                if (orderFound) {
                    foundOrders++;
                }
            } catch (Exception e) {
                log.error("Failed to update order {} package {} with early payment settlements",
                    orderNumber, packageId, e);
            }
        }

        log.info("Settlement processing completed for store: {} - Processed: {} orders, Found in system: {}",
            store.getId(), processedOrders, foundOrders);
    }

    private boolean updateOrderWithSettlements(Store store, String orderNumber, Long packageId, 
                                          List<TrendyolFinancialSettlementItem> settlementItems) {
        
        // Find the order by order number, package ID and store
        Optional<TrendyolOrder> orderOptional = orderRepository
            .findByTyOrderNumberAndPackageNoAndStore(orderNumber, packageId, store);

        if (orderOptional.isEmpty()) {
            log.debug("Order not found for settlement update: orderNumber={}, packageId={}, store={}", 
                orderNumber, packageId, store.getId());
            return false;
        }

        TrendyolOrder order = orderOptional.get();
        List<OrderItem> orderItems = order.getOrderItems();
        
        if (orderItems == null || orderItems.isEmpty()) {
            log.warn("No order items found for order: orderNumber={}, packageId={}", 
                orderNumber, packageId);
            return false;
        }

        // Initialize financial transactions list if null
        if (order.getFinancialTransactions() == null) {
            order.setFinancialTransactions(new ArrayList<>());
        }

        // Group settlements by barcode, but ONLY for this specific packageId
        Map<String, List<TrendyolFinancialSettlementItem>> settlementsByBarcode = settlementItems.stream()
            .filter(item -> packageId.equals(item.getShipmentPackageId())) // Filter by package ID
            .collect(Collectors.groupingBy(TrendyolFinancialSettlementItem::getBarcode));

        if (settlementsByBarcode.isEmpty()) {
            log.debug("No settlements found for package {} in order {}", packageId, orderNumber);
            return false;
        }

        boolean orderUpdated = false;
        int itemsWithSettlements = 0;
        
        // Update each order item with its corresponding settlements
        for (OrderItem orderItem : orderItems) {
            String barcode = orderItem.getBarcode();
            List<TrendyolFinancialSettlementItem> itemSettlements = settlementsByBarcode.get(barcode);
            
            if (itemSettlements != null && !itemSettlements.isEmpty()) {
                // Process settlements for this order item in financial transactions
                boolean itemUpdated = processItemFinancialSettlements(order, orderItem, itemSettlements, orderNumber, packageId);
                
                if (itemUpdated) {
                    itemsWithSettlements++;
                    orderUpdated = true;
                }
            }
        }

        if (orderUpdated) {
            // Set transaction status and date based on settlements
            if (!settlementItems.isEmpty()) {
                TrendyolFinancialSettlementItem firstSettlement = settlementItems.get(0);
                order.setTransactionDate(convertTimestampToLocalDateTime(firstSettlement.getTransactionDate()));
                order.setTransactionStatus("SETTLED");
            }

            // Update couponDiscount and earlyPaymentFee from settlement items
            for (TrendyolFinancialSettlementItem item : settlementItems) {
                String transactionType = item.getTransactionType();
                if ("Kupon".equals(transactionType) || "Coupon".equals(transactionType)) {
                    // Coupon discount is stored in debt field
                    if (item.getDebt() != null) {
                        BigDecimal currentCoupon = order.getCouponDiscount() != null ? order.getCouponDiscount() : BigDecimal.ZERO;
                        order.setCouponDiscount(currentCoupon.add(item.getDebt()));
                        log.debug("Updated couponDiscount for order {} package {}: {}", orderNumber, packageId, order.getCouponDiscount());
                    }
                }
                if ("ErkenÖdeme".equals(transactionType) || "EarlyPayment".equals(transactionType)) {
                    // Early payment fee is stored in debt field
                    if (item.getDebt() != null) {
                        BigDecimal currentEarlyPayment = order.getEarlyPaymentFee() != null ? order.getEarlyPaymentFee() : BigDecimal.ZERO;
                        order.setEarlyPaymentFee(currentEarlyPayment.add(item.getDebt()));
                        log.debug("Updated earlyPaymentFee for order {} package {}: {}", orderNumber, packageId, order.getEarlyPaymentFee());
                    }
                }
            }

            // Mark commission as actual (not estimated) since it comes from Financial API
            order.setIsCommissionEstimated(false);

            // Calculate transaction summaries for all financial transactions
            updateFinancialTransactionSummaries(order);

            orderRepository.save(order);

            // Update product commission rates from settlement data
            updateProductCommissionRates(store, settlementItems);

            log.info("Updated order {} package {} with settlements for {}/{} products",
                orderNumber, packageId, itemsWithSettlements, orderItems.size());
        } else {
            log.debug("No new settlements to add for order {} package {}", orderNumber, packageId);
        }

        return true; // Order was found
    }

    /**
     * Process settlements for a single order item with smart status management
     * Works with the new financial_transactions column instead of order_items transactions
     */
    private boolean processItemFinancialSettlements(TrendyolOrder order, OrderItem orderItem, 
                                                   List<TrendyolFinancialSettlementItem> itemSettlements, 
                                                   String orderNumber, Long packageId) {
        
        String barcode = orderItem.getBarcode();
        
        // Find existing financial item data for this barcode
        FinancialOrderItemData financialItemData = order.getFinancialTransactions().stream()
            .filter(item -> barcode.equals(item.getBarcode()))
            .findFirst()
            .orElse(null);
        
        // Create new financial item data if it doesn't exist
        if (financialItemData == null) {
            financialItemData = FinancialOrderItemData.builder()
                .barcode(barcode)
                .transactions(new ArrayList<>())
                .build();
            order.getFinancialTransactions().add(financialItemData);
        }

        boolean itemUpdated = false;
        
        // Separate settlements by type to handle them intelligently
        List<TrendyolFinancialSettlementItem> sales = new ArrayList<>();
        List<TrendyolFinancialSettlementItem> returns = new ArrayList<>();
        List<TrendyolFinancialSettlementItem> discounts = new ArrayList<>();
        List<TrendyolFinancialSettlementItem> coupons = new ArrayList<>();
        List<TrendyolFinancialSettlementItem> earlyPayments = new ArrayList<>();
        List<TrendyolFinancialSettlementItem> others = new ArrayList<>();

        for (TrendyolFinancialSettlementItem settlement : itemSettlements) {
            String transactionType = settlement.getTransactionType();
            if ("Satış".equals(transactionType) || "Sale".equals(transactionType)) {
                sales.add(settlement);
            } else if ("İade".equals(transactionType) || "Return".equals(transactionType)) {
                returns.add(settlement);
            } else if ("İndirim".equals(transactionType) || "Discount".equals(transactionType)) {
                discounts.add(settlement);
            } else if ("Kupon".equals(transactionType) || "Coupon".equals(transactionType)) {
                coupons.add(settlement);
            } else if ("ErkenÖdeme".equals(transactionType) || "EarlyPayment".equals(transactionType)) {
                earlyPayments.add(settlement);
            } else {
                others.add(settlement);
            }
        }

        // First, add all sales as SOLD
        for (TrendyolFinancialSettlementItem saleSettlement : sales) {
            FinancialSettlement settlement = settlementMapper.mapToOrderItemSettlement(saleSettlement);
            
            // Check if this settlement already exists
            boolean exists = financialItemData.getTransactions().stream()
                .anyMatch(existing -> existing.getId().equals(settlement.getId()));
                
            if (!exists) {
                settlement.setStatus("SOLD");
                financialItemData.getTransactions().add(settlement);
                itemUpdated = true;
                log.debug("Added SOLD settlement {} for product {} in order {} package {}", 
                    settlement.getId(), barcode, orderNumber, packageId);
            }
        }
        
        // Then, handle returns - update existing SOLD transactions to RETURNED
        for (TrendyolFinancialSettlementItem returnSettlement : returns) {
            FinancialSettlement returnSettlementObj = settlementMapper.mapToOrderItemSettlement(returnSettlement);
            
            // Check if this return settlement already exists
            boolean returnExists = financialItemData.getTransactions().stream()
                .anyMatch(existing -> existing.getId().equals(returnSettlementObj.getId()));
                
            if (!returnExists) {
                // Find a SOLD transaction to convert to RETURNED
                Optional<FinancialSettlement> soldTransaction = financialItemData.getTransactions().stream()
                    .filter(t -> "SOLD".equals(t.getStatus()) && t.getBarcode().equals(returnSettlement.getBarcode()))
                    .findFirst();
                
                if (soldTransaction.isPresent()) {
                    // Update existing SOLD transaction to RETURNED
                    FinancialSettlement existingTransaction = soldTransaction.get();
                    existingTransaction.setStatus("RETURNED");
                    existingTransaction.setTransactionType("İade");
                    // Keep original sale data but mark as returned
                    log.info("Updated transaction {} from SOLD to RETURNED for product {} in order {} package {}", 
                        existingTransaction.getId(), barcode, orderNumber, packageId);
                } else {
                    // No SOLD transaction found, add as new RETURNED transaction
                    returnSettlementObj.setStatus("RETURNED");
                    financialItemData.getTransactions().add(returnSettlementObj);
                    log.info("Added new RETURNED settlement {} for product {} in order {} package {}", 
                        returnSettlementObj.getId(), barcode, orderNumber, packageId);
                }
                itemUpdated = true;
            }
        }
        
        // Handle discount settlements (they reduce revenue)
        for (TrendyolFinancialSettlementItem discountSettlement : discounts) {
            FinancialSettlement settlement = settlementMapper.mapToOrderItemSettlement(discountSettlement);
            
            boolean exists = financialItemData.getTransactions().stream()
                .anyMatch(existing -> existing.getId().equals(settlement.getId()));
                
            if (!exists) {
                settlement.setStatus("DISCOUNT");
                financialItemData.getTransactions().add(settlement);
                itemUpdated = true;
                log.debug("Added DISCOUNT settlement {} for product {} in order {} package {}", 
                    settlement.getId(), barcode, orderNumber, packageId);
            }
        }
        
        // Handle coupon settlements (they also reduce revenue)
        for (TrendyolFinancialSettlementItem couponSettlement : coupons) {
            FinancialSettlement settlement = settlementMapper.mapToOrderItemSettlement(couponSettlement);

            boolean exists = financialItemData.getTransactions().stream()
                .anyMatch(existing -> existing.getId().equals(settlement.getId()));

            if (!exists) {
                settlement.setStatus("COUPON");
                financialItemData.getTransactions().add(settlement);
                itemUpdated = true;
                log.debug("Added COUPON settlement {} for product {} in order {} package {}",
                    settlement.getId(), barcode, orderNumber, packageId);
            }
        }

        // Handle early payment settlements (they reduce revenue as fees)
        for (TrendyolFinancialSettlementItem earlyPaymentSettlement : earlyPayments) {
            FinancialSettlement settlement = settlementMapper.mapToOrderItemSettlement(earlyPaymentSettlement);

            boolean exists = financialItemData.getTransactions().stream()
                .anyMatch(existing -> existing.getId().equals(settlement.getId()));

            if (!exists) {
                settlement.setStatus("EARLY_PAYMENT");
                financialItemData.getTransactions().add(settlement);
                itemUpdated = true;
                log.debug("Added EARLY_PAYMENT settlement {} for product {} in order {} package {}",
                    settlement.getId(), barcode, orderNumber, packageId);
            }
        }

        // Handle other transaction types
        for (TrendyolFinancialSettlementItem otherSettlement : others) {
            FinancialSettlement settlement = settlementMapper.mapToOrderItemSettlement(otherSettlement);
            
            boolean exists = financialItemData.getTransactions().stream()
                .anyMatch(existing -> existing.getId().equals(settlement.getId()));
                
            if (!exists) {
                financialItemData.getTransactions().add(settlement);
                itemUpdated = true;
                log.debug("Added {} settlement {} for product {} in order {} package {}", 
                    settlement.getStatus(), settlement.getId(), barcode, orderNumber, packageId);
            }
        }
        
        return itemUpdated;
    }

    /**
     * Update orders with return settlements.
     * For returns, we find SOLD transactions in financial_transactions and update their status to RETURNED.
     */
    private boolean updateOrderWithReturnSettlements(Store store, String orderNumber, 
                                                   List<TrendyolFinancialSettlementItem> returnSettlements) {
        
        // Find all orders with this order number for the store
        List<TrendyolOrder> orders = orderRepository.findByStoreIdAndTyOrderNumber(store.getId(), orderNumber);

        if (orders.isEmpty()) {
            log.debug("No orders found for return settlement: orderNumber={}, store={}", 
                orderNumber, store.getId());
            return false;
        }

        log.debug("Found {} order packages for return settlement: orderNumber={}", orders.size(), orderNumber);

        // Group return settlements by barcode and count them
        Map<String, Integer> returnCountByBarcode = returnSettlements.stream()
            .collect(Collectors.groupingBy(
                TrendyolFinancialSettlementItem::getBarcode,
                Collectors.summingInt(item -> 1)
            ));

        log.info("Return settlement counts by barcode for order {}: {}", orderNumber, returnCountByBarcode);

        boolean anyOrderUpdated = false;

        // Process each barcode across all order packages
        for (Map.Entry<String, Integer> entry : returnCountByBarcode.entrySet()) {
            String barcode = entry.getKey();
            int totalReturnsForBarcode = entry.getValue();
            
            log.info("Processing {} returns for barcode {} in order {}", totalReturnsForBarcode, barcode, orderNumber);
            
            // Find all order items with this barcode across all packages
            List<OrderItem> matchingOrderItems = orders.stream()
                .flatMap(order -> order.getOrderItems().stream())
                .filter(item -> barcode.equals(item.getBarcode()))
                .collect(Collectors.toList());
            
            if (matchingOrderItems.isEmpty()) {
                log.warn("No order items found for barcode {} in order {}", barcode, orderNumber);
                continue;
            }
            
            // Calculate total quantity for this barcode
            int totalQuantityForBarcode = matchingOrderItems.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
                
            log.info("Total quantity for barcode {} in order {}: {}", barcode, orderNumber, totalQuantityForBarcode);
            
            // Count existing SOLD transactions from financial_transactions and update them to RETURNED
            int soldTransactionsUpdated = 0;
            
            for (TrendyolOrder order : orders) {
                if (order.getFinancialTransactions() == null) {
                    continue;
                }
                
                // Find financial item data for this barcode
                Optional<FinancialOrderItemData> financialItemData = order.getFinancialTransactions().stream()
                    .filter(item -> barcode.equals(item.getBarcode()))
                    .findFirst();
                
                if (financialItemData.isPresent() && financialItemData.get().getTransactions() != null) {
                    // Find SOLD transactions for this barcode and update them to RETURNED
                    List<FinancialSettlement> soldTransactions = financialItemData.get().getTransactions().stream()
                        .filter(t -> "SOLD".equals(t.getStatus()) && barcode.equals(t.getBarcode()))
                        .collect(Collectors.toList());
                    
                    for (FinancialSettlement soldTransaction : soldTransactions) {
                        if (soldTransactionsUpdated < totalReturnsForBarcode) {
                            soldTransaction.setStatus("RETURNED");
                            soldTransaction.setTransactionType("İade");
                            soldTransactionsUpdated++;
                            anyOrderUpdated = true;
                            
                            log.info("Updated transaction {} from SOLD to RETURNED for barcode {} in order {}", 
                                soldTransaction.getId(), barcode, orderNumber);
                        }
                    }
                }
            }
            
            log.info("Updated {}/{} SOLD transactions to RETURNED for barcode {} in order {}", 
                soldTransactionsUpdated, totalReturnsForBarcode, barcode, orderNumber);
        }

        // Save all updated orders
        if (anyOrderUpdated) {
            for (TrendyolOrder order : orders) {
                if (order.getTransactionStatus() == null || "NOT_SETTLED".equals(order.getTransactionStatus())) {
                    order.setTransactionStatus("SETTLED");
                }

                // Mark commission as actual (not estimated) since it comes from Financial API
                order.setIsCommissionEstimated(false);

                // Calculate transaction summaries for all financial transactions
                updateFinancialTransactionSummaries(order);

                orderRepository.save(order);
            }

            // Update product commission rates from return settlement data
            updateProductCommissionRates(orders.get(0).getStore(), returnSettlements);

            log.info("Successfully processed return settlements for order {}", orderNumber);
        }

        return anyOrderUpdated;
    }
    
    /**
     * Converts milliseconds timestamp to LocalDateTime
     */
    private LocalDateTime convertTimestampToLocalDateTime(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.of("Europe/Istanbul"))
                .toLocalDateTime();
    }

    /**
     * Get settlement statistics for a store including both sales and returns
     */
    public Map<String, Object> getSettlementStats(UUID storeId) {
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));

        Map<String, Object> stats = new HashMap<>();
        
        long totalOrders = orderRepository.countByStore(store);
        long settledOrders = orderRepository.countByStoreAndTransactionStatus(store, "SETTLED");
        long notSettledOrders = orderRepository.countByStoreAndTransactionStatus(store, "NOT_SETTLED");
        
        stats.put("totalOrders", totalOrders);
        stats.put("settledOrders", settledOrders);
        stats.put("notSettledOrders", notSettledOrders);
        stats.put("settlementRate", totalOrders > 0 ? (double) settledOrders / totalOrders * 100 : 0);
        
        // Get detailed transaction statistics
        Map<String, Object> transactionStats = getTransactionStatistics(store);
        stats.put("transactionStats", transactionStats);
        
        return stats;
    }

    /**
     * Get detailed transaction statistics (sales vs returns)
     * Uses financial_transactions instead of order_items transactions
     */
    private Map<String, Object> getTransactionStatistics(Store store) {
        List<TrendyolOrder> settledOrders = orderRepository.findByStoreAndTransactionStatus(store, "SETTLED");

        int totalSaleTransactions = 0;
        int totalReturnTransactions = 0;
        BigDecimal totalSaleRevenue = BigDecimal.ZERO;
        BigDecimal totalReturnAmount = BigDecimal.ZERO;

        for (TrendyolOrder order : settledOrders) {
            if (order.getFinancialTransactions() != null) {
                for (FinancialOrderItemData financialItem : order.getFinancialTransactions()) {
                    if (financialItem.getTransactions() != null) {
                        for (FinancialSettlement transaction : financialItem.getTransactions()) {
                            if ("Satış".equals(transaction.getTransactionType()) || "Sale".equals(transaction.getTransactionType())) {
                                totalSaleTransactions++;
                                if (transaction.getSellerRevenue() != null) {
                                    totalSaleRevenue = totalSaleRevenue.add(transaction.getSellerRevenue());
                                }
                            } else if ("İade".equals(transaction.getTransactionType()) || "Return".equals(transaction.getTransactionType())) {
                                totalReturnTransactions++;
                                if (transaction.getSellerRevenue() != null) {
                                    totalReturnAmount = totalReturnAmount.add(transaction.getSellerRevenue());
                                }
                            }
                        }
                    }
                }
            }
        }

        Map<String, Object> transactionStats = new HashMap<>();
        transactionStats.put("totalSaleTransactions", totalSaleTransactions);
        transactionStats.put("totalReturnTransactions", totalReturnTransactions);
        transactionStats.put("totalSaleRevenue", totalSaleRevenue);
        transactionStats.put("totalReturnAmount", totalReturnAmount);
        transactionStats.put("netRevenue", totalSaleRevenue.subtract(totalReturnAmount));

        return transactionStats;
    }
    
    /**
     * Update product commission rates from settlement data.
     * This updates the lastCommissionRate and lastCommissionDate fields on TrendyolProduct
     * when we receive actual commission data from the Financial API.
     *
     * @param store The store
     * @param settlementItems The settlement items containing actual commission rates
     */
    private void updateProductCommissionRates(Store store, List<TrendyolFinancialSettlementItem> settlementItems) {
        if (settlementItems == null || settlementItems.isEmpty()) {
            return;
        }

        // Group settlements by barcode to find the most recent commission rate for each product
        Map<String, TrendyolFinancialSettlementItem> latestSettlementByBarcode = new HashMap<>();

        for (TrendyolFinancialSettlementItem item : settlementItems) {
            // Only consider Sale transactions for commission rate updates
            String transactionType = item.getTransactionType();
            if (!"Satış".equals(transactionType) && !"Sale".equals(transactionType)) {
                continue;
            }

            if (item.getBarcode() == null || item.getCommissionRate() == null) {
                continue;
            }

            String barcode = item.getBarcode();
            TrendyolFinancialSettlementItem existing = latestSettlementByBarcode.get(barcode);

            // Keep the most recent settlement (by transaction date)
            if (existing == null ||
                (item.getTransactionDate() != null &&
                 (existing.getTransactionDate() == null || item.getTransactionDate() > existing.getTransactionDate()))) {
                latestSettlementByBarcode.put(barcode, item);
            }
        }

        if (latestSettlementByBarcode.isEmpty()) {
            return;
        }

        // Update products with new commission rates
        List<TrendyolProduct> productsToUpdate = new ArrayList<>();

        for (Map.Entry<String, TrendyolFinancialSettlementItem> entry : latestSettlementByBarcode.entrySet()) {
            String barcode = entry.getKey();
            TrendyolFinancialSettlementItem settlement = entry.getValue();

            Optional<TrendyolProduct> productOpt = productRepository.findByStoreIdAndBarcode(store.getId(), barcode);
            if (productOpt.isPresent()) {
                TrendyolProduct product = productOpt.get();
                LocalDateTime settlementDate = convertTimestampToLocalDateTime(settlement.getTransactionDate());

                // Only update if this settlement is more recent than the last commission date
                if (product.getLastCommissionDate() == null ||
                    (settlementDate != null && settlementDate.isAfter(product.getLastCommissionDate()))) {

                    product.setLastCommissionRate(settlement.getCommissionRate());
                    product.setLastCommissionDate(settlementDate);
                    productsToUpdate.add(product);

                    log.debug("Updated commission rate for product {} (barcode: {}): {} -> {} (date: {})",
                        product.getId(), barcode, product.getCommissionRate(),
                        settlement.getCommissionRate(), settlementDate);
                }
            }
        }

        if (!productsToUpdate.isEmpty()) {
            productRepository.saveAll(productsToUpdate);
            log.info("Updated commission rates for {} products from Financial API settlements", productsToUpdate.size());
        }
    }

    /**
     * Calculate transaction summary for a financial order item data
     */
    private FinancialOrderItemsTransactionSummary calculateFinancialTransactionSummary(FinancialOrderItemData financialItemData) {
        if (financialItemData.getTransactions() == null || financialItemData.getTransactions().isEmpty()) {
            return FinancialOrderItemsTransactionSummary.builder()
                    .totalPrice(BigDecimal.ZERO)
                    .totalDiscount(BigDecimal.ZERO)
                    .totalCoupon(BigDecimal.ZERO)
                    .totalCommission(BigDecimal.ZERO)
                    .finalPrice(BigDecimal.ZERO)
                    .netAmount(BigDecimal.ZERO)
                    .soldQuantity(0)
                    .returnedQuantity(0)
                    .build();
        }
        
        BigDecimal totalPrice = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalCoupon = BigDecimal.ZERO;
        BigDecimal totalSoldCommission = BigDecimal.ZERO;
        BigDecimal totalDiscountCommission = BigDecimal.ZERO;
        BigDecimal totalCouponCommission = BigDecimal.ZERO;
        
        int soldQuantity = 0;
        int returnedQuantity = 0;
        
        // First pass: count sold and returned quantities
        for (FinancialSettlement transaction : financialItemData.getTransactions()) {
            String status = transaction.getStatus();
            if ("SOLD".equals(status)) {
                soldQuantity++;
            } else if ("RETURNED".equals(status)) {
                returnedQuantity++;
            }
        }
        
        // Second pass: calculate financial values based on sold quantity
        int discountCount = 0;
        int couponCount = 0;
        
        for (FinancialSettlement transaction : financialItemData.getTransactions()) {
            String status = transaction.getStatus();
            BigDecimal credit = transaction.getCredit() != null ? transaction.getCredit() : BigDecimal.ZERO;
            BigDecimal debt = transaction.getDebt() != null ? transaction.getDebt() : BigDecimal.ZERO;
            BigDecimal commissionAmount = transaction.getCommissionAmount() != null ? transaction.getCommissionAmount() : BigDecimal.ZERO;
            
            switch (status) {
                case "SOLD":
                    totalPrice = totalPrice.add(credit);
                    totalSoldCommission = totalSoldCommission.add(commissionAmount);
                    break;
                case "RETURNED":
                    // Don't count returned items in financial calculations
                    break;
                case "DISCOUNT":
                    // Count discounts up to sold quantity (not net active quantity)
                    if (discountCount < soldQuantity) {
                        totalDiscount = totalDiscount.add(debt);
                        totalDiscountCommission = totalDiscountCommission.add(commissionAmount);
                        discountCount++;
                    }
                    break;
                case "COUPON":
                    // Count coupons up to sold quantity (not net active quantity)
                    if (couponCount < soldQuantity) {
                        totalCoupon = totalCoupon.add(debt);
                        totalCouponCommission = totalCouponCommission.add(commissionAmount);
                        couponCount++;
                    }
                    break;
            }
        }
        
        // Calculate net values
        BigDecimal finalPrice = totalPrice.subtract(totalDiscount).subtract(totalCoupon);
        BigDecimal totalCommission = totalSoldCommission.subtract(totalDiscountCommission).subtract(totalCouponCommission);
        BigDecimal netAmount = finalPrice.subtract(totalCommission);
        
        return FinancialOrderItemsTransactionSummary.builder()
                .totalPrice(totalPrice)
                .totalDiscount(totalDiscount)
                .totalCoupon(totalCoupon)
                .totalCommission(totalCommission)
                .finalPrice(finalPrice)
                .netAmount(netAmount)
                .soldQuantity(soldQuantity)
                .returnedQuantity(returnedQuantity)
                .build();
    }
    
    /**
     * Update transaction summaries for all financial transactions
     */
    private void updateFinancialTransactionSummaries(TrendyolOrder order) {
        if (order.getFinancialTransactions() == null || order.getFinancialTransactions().isEmpty()) {
            return;
        }
        
        // Calculate individual financial item summaries
        for (FinancialOrderItemData financialItemData : order.getFinancialTransactions()) {
            FinancialOrderItemsTransactionSummary summary = calculateFinancialTransactionSummary(financialItemData);
            financialItemData.setTransactionSummary(summary);
        }
        
        // Calculate order-level transaction summary
        FinancialOrderTransactionSummary orderSummary = calculateFinancialOrderTransactionSummary(order);
        order.setOrderTransactionSummary(orderSummary);
        
        log.debug("Updated financial transaction summaries for {} financial items in order {}", 
            order.getFinancialTransactions().size(), order.getTyOrderNumber());
    }
    
    /**
     * Calculate order-level transaction summary by aggregating all financial transactions
     */
    private FinancialOrderTransactionSummary calculateFinancialOrderTransactionSummary(TrendyolOrder order) {
        if (order.getFinancialTransactions() == null || order.getFinancialTransactions().isEmpty()) {
            return FinancialOrderTransactionSummary.builder()
                    .totalPrice(BigDecimal.ZERO)
                    .totalDiscount(BigDecimal.ZERO)
                    .totalCoupon(BigDecimal.ZERO)
                    .totalCommission(BigDecimal.ZERO)
                    .finalPrice(BigDecimal.ZERO)
                    .netAmount(BigDecimal.ZERO)
                    .totalSoldQuantity(0)
                    .totalReturnedQuantity(0)
                    .uniqueProductCount(0)
                    .build();
        }
        
        BigDecimal totalPrice = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalCoupon = BigDecimal.ZERO;
        BigDecimal totalCommission = BigDecimal.ZERO;
        BigDecimal finalPrice = BigDecimal.ZERO;
        BigDecimal netAmount = BigDecimal.ZERO;
        
        int totalSoldQuantity = 0;
        int totalReturnedQuantity = 0;
        int uniqueProductCount = order.getFinancialTransactions().size();
        
        // Aggregate data from all financial items
        for (FinancialOrderItemData financialItem : order.getFinancialTransactions()) {
            FinancialOrderItemsTransactionSummary itemSummary = financialItem.getTransactionSummary();
            if (itemSummary != null) {
                totalPrice = totalPrice.add(itemSummary.getTotalPrice() != null ? itemSummary.getTotalPrice() : BigDecimal.ZERO);
                totalDiscount = totalDiscount.add(itemSummary.getTotalDiscount() != null ? itemSummary.getTotalDiscount() : BigDecimal.ZERO);
                totalCoupon = totalCoupon.add(itemSummary.getTotalCoupon() != null ? itemSummary.getTotalCoupon() : BigDecimal.ZERO);
                totalCommission = totalCommission.add(itemSummary.getTotalCommission() != null ? itemSummary.getTotalCommission() : BigDecimal.ZERO);
                finalPrice = finalPrice.add(itemSummary.getFinalPrice() != null ? itemSummary.getFinalPrice() : BigDecimal.ZERO);
                netAmount = netAmount.add(itemSummary.getNetAmount() != null ? itemSummary.getNetAmount() : BigDecimal.ZERO);
                
                totalSoldQuantity += itemSummary.getSoldQuantity() != null ? itemSummary.getSoldQuantity() : 0;
                totalReturnedQuantity += itemSummary.getReturnedQuantity() != null ? itemSummary.getReturnedQuantity() : 0;
            }
        }
        
        return FinancialOrderTransactionSummary.builder()
                .totalPrice(totalPrice)
                .totalDiscount(totalDiscount)
                .totalCoupon(totalCoupon)
                .totalCommission(totalCommission)
                .finalPrice(finalPrice)
                .netAmount(netAmount)
                .totalSoldQuantity(totalSoldQuantity)
                .totalReturnedQuantity(totalReturnedQuantity)
                .uniqueProductCount(uniqueProductCount)
                .build();
    }
    
    /**
     * Calculate transaction summary for an order item
     * @deprecated Use calculateFinancialTransactionSummary instead
     */
    @Deprecated
    private FinancialOrderItemsTransactionSummary calculateTransactionSummary(OrderItem orderItem) {
        if (orderItem.getTransactions() == null || orderItem.getTransactions().isEmpty()) {
            return FinancialOrderItemsTransactionSummary.builder()
                    .totalPrice(BigDecimal.ZERO)
                    .totalDiscount(BigDecimal.ZERO)
                    .totalCoupon(BigDecimal.ZERO)
                    .totalCommission(BigDecimal.ZERO)
                    .finalPrice(BigDecimal.ZERO)
                    .netAmount(BigDecimal.ZERO)
                    .soldQuantity(0)
                    .returnedQuantity(0)
                    .build();
        }
        
        BigDecimal totalPrice = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalCoupon = BigDecimal.ZERO;
        BigDecimal totalSoldCommission = BigDecimal.ZERO;
        BigDecimal totalDiscountCommission = BigDecimal.ZERO;
        BigDecimal totalCouponCommission = BigDecimal.ZERO;
        
        int soldQuantity = 0;
        int returnedQuantity = 0;
        
        // First pass: count sold and returned quantities
        for (FinancialSettlement transaction : orderItem.getTransactions()) {
            String status = transaction.getStatus();
            if ("SOLD".equals(status)) {
                soldQuantity++;
            } else if ("RETURNED".equals(status)) {
                returnedQuantity++;
            }
        }
        
        // Second pass: calculate financial values based on sold quantity
        int discountCount = 0;
        int couponCount = 0;
        
        for (FinancialSettlement transaction : orderItem.getTransactions()) {
            String status = transaction.getStatus();
            BigDecimal credit = transaction.getCredit() != null ? transaction.getCredit() : BigDecimal.ZERO;
            BigDecimal debt = transaction.getDebt() != null ? transaction.getDebt() : BigDecimal.ZERO;
            BigDecimal commissionAmount = transaction.getCommissionAmount() != null ? transaction.getCommissionAmount() : BigDecimal.ZERO;
            
            switch (status) {
                case "SOLD":
                    totalPrice = totalPrice.add(credit);
                    totalSoldCommission = totalSoldCommission.add(commissionAmount);
                    break;
                case "RETURNED":
                    // Don't count returned items in financial calculations
                    break;
                case "DISCOUNT":
                    // Count discounts up to sold quantity (not net active quantity)
                    // This ensures that if we sold 1 and returned 1, we still count 1 discount
                    if (discountCount < soldQuantity) {
                        totalDiscount = totalDiscount.add(debt);
                        totalDiscountCommission = totalDiscountCommission.add(commissionAmount);
                        discountCount++;
                    }
                    break;
                case "COUPON":
                    // Count coupons up to sold quantity (not net active quantity)
                    // This ensures that if we sold 1 and returned 1, we still count 1 coupon
                    if (couponCount < soldQuantity) {
                        totalCoupon = totalCoupon.add(debt);
                        totalCouponCommission = totalCouponCommission.add(commissionAmount);
                        couponCount++;
                    }
                    break;
            }
        }
        
        // Calculate net values
        BigDecimal finalPrice = totalPrice.subtract(totalDiscount).subtract(totalCoupon);
        BigDecimal totalCommission = totalSoldCommission.subtract(totalDiscountCommission).subtract(totalCouponCommission);
        BigDecimal netAmount = finalPrice.subtract(totalCommission);
        
        return FinancialOrderItemsTransactionSummary.builder()
                .totalPrice(totalPrice)
                .totalDiscount(totalDiscount)
                .totalCoupon(totalCoupon)
                .totalCommission(totalCommission)
                .finalPrice(finalPrice)
                .netAmount(netAmount)
                .soldQuantity(soldQuantity)
                .returnedQuantity(returnedQuantity)
                .build();
    }
    
    /**
     * Update transaction summaries for all order items
     * @deprecated Use updateFinancialTransactionSummaries instead
     */
    @Deprecated
    private void updateTransactionSummaries(TrendyolOrder order) {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            return;
        }
        
        // Calculate individual order item summaries
        for (OrderItem orderItem : order.getOrderItems()) {
            FinancialOrderItemsTransactionSummary summary = calculateTransactionSummary(orderItem);
            orderItem.setTransactionSummary(summary);
        }
        
        // Calculate order-level transaction summary
        FinancialOrderTransactionSummary orderSummary = calculateOrderTransactionSummary(order);
        order.setOrderTransactionSummary(orderSummary);
        
        log.debug("Updated transaction summaries for {} order items in order {}", 
            order.getOrderItems().size(), order.getTyOrderNumber());
    }
    
    /**
     * Calculate order-level transaction summary by aggregating all order items
     * @deprecated Use calculateFinancialOrderTransactionSummary instead
     */
    @Deprecated
    private FinancialOrderTransactionSummary calculateOrderTransactionSummary(TrendyolOrder order) {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            return FinancialOrderTransactionSummary.builder()
                    .totalPrice(BigDecimal.ZERO)
                    .totalDiscount(BigDecimal.ZERO)
                    .totalCoupon(BigDecimal.ZERO)
                    .totalCommission(BigDecimal.ZERO)
                    .finalPrice(BigDecimal.ZERO)
                    .netAmount(BigDecimal.ZERO)
                    .totalSoldQuantity(0)
                    .totalReturnedQuantity(0)
                    .uniqueProductCount(0)
                    .build();
        }
        
        BigDecimal totalPrice = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalCoupon = BigDecimal.ZERO;
        BigDecimal totalCommission = BigDecimal.ZERO;
        BigDecimal finalPrice = BigDecimal.ZERO;
        BigDecimal netAmount = BigDecimal.ZERO;
        
        int totalSoldQuantity = 0;
        int totalReturnedQuantity = 0;
        int uniqueProductCount = order.getOrderItems().size();
        
        // Aggregate data from all order items
        for (OrderItem orderItem : order.getOrderItems()) {
            FinancialOrderItemsTransactionSummary itemSummary = orderItem.getTransactionSummary();
            if (itemSummary != null) {
                totalPrice = totalPrice.add(itemSummary.getTotalPrice() != null ? itemSummary.getTotalPrice() : BigDecimal.ZERO);
                totalDiscount = totalDiscount.add(itemSummary.getTotalDiscount() != null ? itemSummary.getTotalDiscount() : BigDecimal.ZERO);
                totalCoupon = totalCoupon.add(itemSummary.getTotalCoupon() != null ? itemSummary.getTotalCoupon() : BigDecimal.ZERO);
                totalCommission = totalCommission.add(itemSummary.getTotalCommission() != null ? itemSummary.getTotalCommission() : BigDecimal.ZERO);
                finalPrice = finalPrice.add(itemSummary.getFinalPrice() != null ? itemSummary.getFinalPrice() : BigDecimal.ZERO);
                netAmount = netAmount.add(itemSummary.getNetAmount() != null ? itemSummary.getNetAmount() : BigDecimal.ZERO);
                
                totalSoldQuantity += itemSummary.getSoldQuantity() != null ? itemSummary.getSoldQuantity() : 0;
                totalReturnedQuantity += itemSummary.getReturnedQuantity() != null ? itemSummary.getReturnedQuantity() : 0;
            }
        }
        
        return FinancialOrderTransactionSummary.builder()
                .totalPrice(totalPrice)
                .totalDiscount(totalDiscount)
                .totalCoupon(totalCoupon)
                .totalCommission(totalCommission)
                .finalPrice(finalPrice)
                .netAmount(netAmount)
                .totalSoldQuantity(totalSoldQuantity)
                .totalReturnedQuantity(totalReturnedQuantity)
                .uniqueProductCount(uniqueProductCount)
                .build();
    }
}
