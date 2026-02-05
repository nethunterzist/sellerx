package com.ecommerce.sellerx.products;

import com.ecommerce.sellerx.alerts.AlertEngine;
import com.ecommerce.sellerx.common.TrendyolRateLimiter;
import com.ecommerce.sellerx.common.exception.ResourceNotFoundException;
import com.ecommerce.sellerx.orders.StockOrderSynchronizationService;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.stores.StoreNotFoundException;
import com.ecommerce.sellerx.stores.TrendyolCredentials;
import com.ecommerce.sellerx.stores.MarketplaceCredentials;
import com.ecommerce.sellerx.categories.TrendyolCategoryRepository;
import com.ecommerce.sellerx.categories.TrendyolCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendyolProductService {

    private static final String TRENDYOL_BASE_URL = "https://apigw.trendyol.com";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000;

    private final TrendyolProductRepository trendyolProductRepository;
    private final StoreRepository storeRepository;
    private final TrendyolProductMapper productMapper;
    private final RestTemplate restTemplate;
    private final StockOrderSynchronizationService stockOrderSyncService;
    private final TrendyolCategoryRepository trendyolCategoryRepository;
    private final TrendyolRateLimiter rateLimiter;
    private final AlertEngine alertEngine;
    private final AutoStockDetectionService autoStockDetectionService;
    
    /**
     * Helper method to compare BigDecimal values properly
     * Handles null values and numeric equality regardless of scale
     */
    private boolean isBigDecimalChanged(BigDecimal existing, BigDecimal incoming) {
        if (existing == null && incoming == null) {
            return false;
        }
        if (existing == null || incoming == null) {
            return true;
        }
        return existing.compareTo(incoming) != 0;
    }

    /**
     * Execute an operation with retry logic and rate limiting.
     * Retries up to MAX_RETRIES times with exponential backoff.
     */
    private <T> T executeWithRetry(Supplier<T> operation, String operationName, UUID storeId) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < MAX_RETRIES) {
            try {
                // Rate limit before each API call (per-store)
                rateLimiter.acquire(storeId);
                return operation.get();
            } catch (Exception e) {
                lastException = e;
                attempt++;
                if (attempt < MAX_RETRIES) {
                    long delayMs = RETRY_DELAY_MS * attempt;
                    log.warn("[{}] Attempt {}/{} failed, retrying in {}ms: {}",
                            operationName, attempt, MAX_RETRIES, delayMs, e.getMessage());
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted for " + operationName, ie);
                    }
                }
            }
        }
        throw new RuntimeException("Max retries (" + MAX_RETRIES + ") exceeded for " + operationName, lastException);
    }

    public SyncProductsResponse syncProductsFromTrendyol(UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException("Store not found"));
        
        TrendyolCredentials credentials = extractTrendyolCredentials(store);
        if (credentials == null) {
            return new SyncProductsResponse(false, "Trendyol credentials not found", 0, 0, 0, 0);
        }
        
        try {
            int totalFetched = 0;
            int totalSaved = 0;
            int totalUpdated = 0;
            int totalSkipped = 0;
            int page = 0;
            int size = 200;
            boolean hasMorePages = true;
            int totalPages = 0;
            
            while (hasMorePages) {
                final String url = String.format("%s/integration/product/sellers/%d/products?size=%d&page=%d",
                        TRENDYOL_BASE_URL, credentials.getSellerId(), size, page);

                HttpHeaders headers = createAuthHeaders(credentials);
                HttpEntity<String> entity = new HttpEntity<>(headers);

                // Use retry mechanism with rate limiting
                final int currentPage = page;
                ResponseEntity<TrendyolApiProductResponse> response = executeWithRetry(
                        () -> restTemplate.exchange(url, HttpMethod.GET, entity, TrendyolApiProductResponse.class),
                        "syncProducts-page-" + currentPage,
                        storeId
                );

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    TrendyolApiProductResponse apiResponse = response.getBody();
                    
                    // Get total pages from first response
                    if (page == 0 && apiResponse.getTotalPages() != null) {
                        totalPages = apiResponse.getTotalPages();
                    }
                    
                    if (apiResponse.getContent() != null) {
                        for (TrendyolApiProductResponse.TrendyolApiProduct apiProduct : apiResponse.getContent()) {
                            try {
                                ProductSyncResult result = saveOrUpdateProduct(store, apiProduct);
                                totalFetched++;
                                
                                switch (result) {
                                    case NEW:
                                        totalSaved++;
                                        break;
                                    case UPDATED:
                                        totalUpdated++;
                                        break;
                                    case SKIPPED:
                                        totalSkipped++;
                                        break;
                                }
                            } catch (Exception e) {
                                log.error("Error processing product {}: {}", apiProduct.getId(), e.getMessage());
                            }
                        }
                    }
                    
                    // Log page progress
                    int displayPage = page + 1;
                    int displayTotalPages = totalPages > 0 ? totalPages : displayPage;
                    log.info("[PRODUCTS] Sayfa {}/{} işleniyor... Toplam {} ürün raflara dizildi.",
                            displayPage, displayTotalPages, totalFetched);
                    
                    // Check if there are more pages
                    hasMorePages = page < (apiResponse.getTotalPages() - 1);
                    page++;
                } else {
                    hasMorePages = false;
                    log.error("Failed to fetch products from Trendyol. Status: {}", response.getStatusCode());
                    return new SyncProductsResponse(false, "Failed to fetch products from Trendyol", 0, 0, 0, 0);
                }
            }
            
            log.info("[PRODUCTS] [COMPLETED] Toplam {} ürün çekildi. Yeni: {}, Güncellenen: {}",
                    totalFetched, totalSaved, totalUpdated);

            // Check stock alerts after sync (async)
            try {
                List<TrendyolProduct> allProducts = trendyolProductRepository.findByStoreId(store.getId());
                alertEngine.checkStockAlerts(store, allProducts);
                log.debug("[PRODUCTS] Stock alert check triggered for {} products", allProducts.size());
            } catch (Exception e) {
                log.error("[PRODUCTS] Error triggering stock alerts: {}", e.getMessage());
                // Don't fail the sync because of alert errors
            }

            return new SyncProductsResponse(true,
                    String.format("Products synced successfully. Fetched: %d, New: %d, Updated: %d, Skipped: %d",
                            totalFetched, totalSaved, totalUpdated, totalSkipped),
                    totalFetched, totalSaved, totalUpdated, totalSkipped);
            
        } catch (Exception e) {
            log.error("Error syncing products from Trendyol: ", e);
            return new SyncProductsResponse(false, "Error syncing products: " + e.getMessage(), 0, 0, 0, 0);
        }
    }
    
    private ProductSyncResult saveOrUpdateProduct(Store store, TrendyolApiProductResponse.TrendyolApiProduct apiProduct) {
        // Check if product already exists
        boolean isNew = !trendyolProductRepository.existsByStoreIdAndProductId(store.getId(), apiProduct.getId());

        TrendyolProduct product = trendyolProductRepository
                .findByStoreIdAndProductId(store.getId(), apiProduct.getId())
                .orElse(TrendyolProduct.builder()
                        .store(store)
                        .productId(apiProduct.getId())
                        .costAndStockInfo(new ArrayList<>())
                        .build());

        // Capture old quantity BEFORE update for auto stock detection
        int oldQuantity = isNew ? 0 : (product.getTrendyolQuantity() != null ? product.getTrendyolQuantity() : 0);
        int newQuantity = apiProduct.getQuantity() != null ? apiProduct.getQuantity() : 0;

        // Check if any field has changed (only if product exists)
        boolean hasChanges = isNew;

        if (!isNew) {
            hasChanges = hasProductChanged(product, apiProduct);
        }

        // Only update if there are changes
        if (hasChanges) {
            updateProductFields(product, apiProduct);
            product.setPreviousTrendyolQuantity(oldQuantity);
            trendyolProductRepository.save(product);

            // Auto stock detection: only for EXISTING products with stock INCREASE
            if (!isNew && newQuantity > oldQuantity) {
                try {
                    autoStockDetectionService.handleStockIncrease(product, oldQuantity, newQuantity);
                } catch (Exception e) {
                    log.warn("[AUTO_STOCK] Error processing stock increase for product {}: {}",
                            product.getBarcode(), e.getMessage());
                }
            }
        }

        if (isNew) {
            return ProductSyncResult.NEW;
        } else if (hasChanges) {
            return ProductSyncResult.UPDATED;
        } else {
            return ProductSyncResult.SKIPPED;
        }
    }
    
    private boolean hasProductChanged(TrendyolProduct existingProduct, TrendyolApiProductResponse.TrendyolApiProduct apiProduct) {
        String productId = existingProduct.getProductId();
        
        // Check basic fields
        if (!Objects.equals(existingProduct.getBarcode(), apiProduct.getBarcode())) {
            log.debug("Product {} changed: barcode '{}' -> '{}'", productId, existingProduct.getBarcode(), apiProduct.getBarcode());
            return true;
        }
        if (!Objects.equals(existingProduct.getTitle(), apiProduct.getTitle())) {
            log.debug("Product {} changed: title", productId);
            return true;
        }
        if (!Objects.equals(existingProduct.getCategoryName(), apiProduct.getCategoryName())) {
            log.debug("Product {} changed: categoryName '{}' -> '{}'", productId, existingProduct.getCategoryName(), apiProduct.getCategoryName());
            return true;
        }
        if (!Objects.equals(existingProduct.getCreateDateTime(), apiProduct.getCreateDateTime())) {
            log.debug("Product {} changed: createDateTime '{}' -> '{}'", productId, existingProduct.getCreateDateTime(), apiProduct.getCreateDateTime());
            return true;
        }
        if (!Objects.equals(existingProduct.getHasActiveCampaign(), 
                apiProduct.getHasActiveCampaign() != null ? apiProduct.getHasActiveCampaign() : false)) {
            log.debug("Product {} changed: hasActiveCampaign '{}' -> '{}'", productId, existingProduct.getHasActiveCampaign(), apiProduct.getHasActiveCampaign());
            return true;
        }
        if (!Objects.equals(existingProduct.getBrand(), apiProduct.getBrand())) {
            log.debug("Product {} changed: brand '{}' -> '{}'", productId, existingProduct.getBrand(), apiProduct.getBrand());
            return true;
        }
        if (!Objects.equals(existingProduct.getBrandId(), apiProduct.getBrandId())) {
            log.debug("Product {} changed: brandId '{}' -> '{}'", productId, existingProduct.getBrandId(), apiProduct.getBrandId());
            return true;
        }
        if (!Objects.equals(existingProduct.getPimCategoryId(), apiProduct.getPimCategoryId())) {
            log.debug("Product {} changed: pimCategoryId '{}' -> '{}'", productId, existingProduct.getPimCategoryId(), apiProduct.getPimCategoryId());
            return true;
        }
        if (!Objects.equals(existingProduct.getProductMainId(), apiProduct.getProductMainId())) {
            log.debug("Product {} changed: productMainId '{}' -> '{}'", productId, existingProduct.getProductMainId(), apiProduct.getProductMainId());
            return true;
        }
        if (!Objects.equals(existingProduct.getProductUrl(), apiProduct.getProductUrl())) {
            log.debug("Product {} changed: productUrl", productId);
            return true;
        }
        if (isBigDecimalChanged(existingProduct.getDimensionalWeight(), apiProduct.getDimensionalWeight())) {
            log.debug("Product {} changed: dimensionalWeight '{}' -> '{}'", productId, existingProduct.getDimensionalWeight(), apiProduct.getDimensionalWeight());
            return true;
        }
        if (isBigDecimalChanged(existingProduct.getSalePrice(), apiProduct.getSalePrice())) {
            log.debug("Product {} changed: salePrice '{}' -> '{}'", productId, existingProduct.getSalePrice(), apiProduct.getSalePrice());
            return true;
        }
        if (!Objects.equals(existingProduct.getVatRate(), apiProduct.getVatRate())) {
            log.debug("Product {} changed: vatRate '{}' -> '{}'", productId, existingProduct.getVatRate(), apiProduct.getVatRate());
            return true;
        }
        if (!Objects.equals(existingProduct.getTrendyolQuantity(), 
                apiProduct.getQuantity() != null ? apiProduct.getQuantity() : 0)) {
            log.debug("Product {} changed: trendyolQuantity '{}' -> '{}'", productId, existingProduct.getTrendyolQuantity(), apiProduct.getQuantity());
            return true;
        }
        
        // Check category-related information
        if (apiProduct.getPimCategoryId() != null) {
            TrendyolCategory category = trendyolCategoryRepository.findByCategoryId(apiProduct.getPimCategoryId()).orElse(null);
            if (category != null) {
                if (isBigDecimalChanged(existingProduct.getCommissionRate(), category.getCommissionRate())) {
                    log.debug("Product {} changed: commissionRate '{}' -> '{}'", productId, existingProduct.getCommissionRate(), category.getCommissionRate());
                    return true;
                }
                if (isBigDecimalChanged(existingProduct.getShippingVolumeWeight(), category.getAverageShipmentSize())) {
                    log.debug("Product {} changed: shippingVolumeWeight '{}' -> '{}'", productId, existingProduct.getShippingVolumeWeight(), category.getAverageShipmentSize());
                    return true;
                }
            }
        }
        
        // Check status fields
        if (!Objects.equals(existingProduct.getApproved(), 
                apiProduct.getApproved() != null ? apiProduct.getApproved() : false)) {
            log.debug("Product {} changed: approved '{}' -> '{}'", productId, existingProduct.getApproved(), apiProduct.getApproved());
            return true;
        }
        if (!Objects.equals(existingProduct.getArchived(), 
                apiProduct.getArchived() != null ? apiProduct.getArchived() : false)) {
            log.debug("Product {} changed: archived '{}' -> '{}'", productId, existingProduct.getArchived(), apiProduct.getArchived());
            return true;
        }
        if (!Objects.equals(existingProduct.getBlacklisted(), 
                apiProduct.getBlacklisted() != null ? apiProduct.getBlacklisted() : false)) {
            log.debug("Product {} changed: blacklisted '{}' -> '{}'", productId, existingProduct.getBlacklisted(), apiProduct.getBlacklisted());
            return true;
        }
        if (!Objects.equals(existingProduct.getRejected(), 
                apiProduct.getRejected() != null ? apiProduct.getRejected() : false)) {
            log.debug("Product {} changed: rejected '{}' -> '{}'", productId, existingProduct.getRejected(), apiProduct.getRejected());
            return true;
        }
        if (!Objects.equals(existingProduct.getOnSale(), 
                apiProduct.getOnsale() != null ? apiProduct.getOnsale() : false)) {
            log.debug("Product {} changed: onSale '{}' -> '{}'", productId, existingProduct.getOnSale(), apiProduct.getOnsale());
            return true;
        }
        
        // Check image
        String newImage = null;
        if (apiProduct.getImages() != null && !apiProduct.getImages().isEmpty()) {
            newImage = apiProduct.getImages().get(0).getUrl();
        }
        if (!Objects.equals(existingProduct.getImage(), newImage)) {
            log.debug("Product {} changed: image '{}' -> '{}'", productId, existingProduct.getImage(), newImage);
            return true;
        }
        
        log.debug("Product {} - No changes detected", productId);
        return false; // No changes detected
    }
    
    private void updateProductFields(TrendyolProduct product, TrendyolApiProductResponse.TrendyolApiProduct apiProduct) {
        // Update product fields
        product.setBarcode(apiProduct.getBarcode());
        product.setTitle(apiProduct.getTitle());
        product.setCategoryName(apiProduct.getCategoryName());
        product.setCreateDateTime(apiProduct.getCreateDateTime());
        product.setHasActiveCampaign(apiProduct.getHasActiveCampaign() != null ? apiProduct.getHasActiveCampaign() : false);
        product.setBrand(apiProduct.getBrand());
        product.setBrandId(apiProduct.getBrandId());
        product.setPimCategoryId(apiProduct.getPimCategoryId());
        product.setProductMainId(apiProduct.getProductMainId());
        product.setProductUrl(apiProduct.getProductUrl());
        product.setDimensionalWeight(apiProduct.getDimensionalWeight());
        product.setSalePrice(apiProduct.getSalePrice());
        product.setVatRate(apiProduct.getVatRate());
        product.setTrendyolQuantity(apiProduct.getQuantity() != null ? apiProduct.getQuantity() : 0);
        
        // Set category-related information
        if (apiProduct.getPimCategoryId() != null) {
            trendyolCategoryRepository.findByCategoryId(apiProduct.getPimCategoryId())
                .ifPresent(category -> {
                    product.setCommissionRate(category.getCommissionRate());
                    product.setShippingVolumeWeight(category.getAverageShipmentSize());
                });
        }
        
        // Set status fields
        product.setApproved(apiProduct.getApproved() != null ? apiProduct.getApproved() : false);
        product.setArchived(apiProduct.getArchived() != null ? apiProduct.getArchived() : false);
        product.setBlacklisted(apiProduct.getBlacklisted() != null ? apiProduct.getBlacklisted() : false);
        product.setRejected(apiProduct.getRejected() != null ? apiProduct.getRejected() : false);
        product.setOnSale(apiProduct.getOnsale() != null ? apiProduct.getOnsale() : false);
        
        // Set first image if available
        if (apiProduct.getImages() != null && !apiProduct.getImages().isEmpty()) {
            product.setImage(apiProduct.getImages().get(0).getUrl());
        }
    }
    
    public AllProductsResponse getAllProductsByStore(UUID storeId) {
        if (!storeRepository.existsById(storeId)) {
            throw new StoreNotFoundException("Store not found");
        }
        
        List<TrendyolProduct> products = trendyolProductRepository.findByStoreId(storeId);
        List<TrendyolProductDto> productDtos = productMapper.toDtoList(products);
        
        return new AllProductsResponse(
            productDtos.size(),
            "Store products retrieved successfully",
            productDtos
        );
    }
    
    public ProductListResponse<TrendyolProductDto> getProductsByStoreWithPagination(UUID storeId, 
                                                                    Integer page, 
                                                                    Integer size, 
                                                                    String search, 
                                                                    String sortBy, 
                                                                    String sortDirection) {
        if (!storeRepository.existsById(storeId)) {
            throw new StoreNotFoundException("Store not found");
        }
        
        // Default values
        int pageNumber = page != null ? page : 0;
        int pageSize = size != null ? size : 50;
        String sortField = sortBy != null ? sortBy : "onSale";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? 
            Sort.Direction.ASC : Sort.Direction.DESC;
        
        // Create pageable with sorting
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, sortField));
        
        Page<TrendyolProduct> productsPage;
        
        // Search or get all
        if (search != null && !search.trim().isEmpty()) {
            productsPage = trendyolProductRepository.findByStoreIdAndSearch(storeId, search.trim(), pageable);
        } else {
            productsPage = trendyolProductRepository.findByStoreId(storeId, pageable);
        }
        
        // Convert to DTOs while preserving pagination info
        List<TrendyolProductDto> productDtos = productsPage.getContent().stream()
                .map(productMapper::toDto)
                .toList();
        
        return new ProductListResponse<>(
            productsPage.getTotalElements(),
            productsPage.getTotalPages(),
            productsPage.getNumber(),
            productsPage.getSize(),
            productsPage.isFirst(),
            productsPage.isLast(),
            productsPage.hasNext(),
            productsPage.hasPrevious(),
            productDtos
        );
    }
    
    @Transactional
    public TrendyolProductDto updateCostAndStock(UUID productId, UpdateCostAndStockRequest request) {
        TrendyolProduct product = trendyolProductRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId.toString()));
        
        // Create new cost and stock info
        CostAndStockInfo newInfo = CostAndStockInfo.builder()
                .quantity(request.getQuantity())
                .unitCost(request.getUnitCost())
                .costVatRate(request.getCostVatRate())
                .stockDate(request.getStockDate() != null ? request.getStockDate() : LocalDate.now())
                .costSource("MANUAL")
                .build();

        // Add to existing list or merge with same date
        List<CostAndStockInfo> costAndStockList = product.getCostAndStockInfo();
        if (costAndStockList == null) {
            costAndStockList = new ArrayList<>();
        }

        addOrMergeCostAndStockInfo(costAndStockList, newInfo);
        // Clear AUTO_DETECTED flags — user has reviewed this product
        clearAutoDetectedFlags(costAndStockList);
        product.setCostAndStockInfo(costAndStockList);

        TrendyolProduct savedProduct = trendyolProductRepository.save(product);
        return productMapper.toDto(savedProduct);
    }

    @Transactional
    public TrendyolProductDto addStockInfo(UUID productId, AddStockInfoRequest request) {
        TrendyolProduct product = trendyolProductRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId.toString()));

        CostAndStockInfo newInfo = CostAndStockInfo.builder()
                .quantity(request.getQuantity())
                .unitCost(request.getUnitCost())
                .costVatRate(request.getCostVatRate())
                .stockDate(request.getStockDate() != null ? request.getStockDate() : LocalDate.now())
                .costSource("MANUAL")
                .build();
        
        List<CostAndStockInfo> costAndStockList = product.getCostAndStockInfo();
        if (costAndStockList == null) {
            costAndStockList = new ArrayList<>();
        }
        
        addOrMergeCostAndStockInfo(costAndStockList, newInfo);
        // Clear AUTO_DETECTED flags — user has reviewed this product
        clearAutoDetectedFlags(costAndStockList);
        product.setCostAndStockInfo(costAndStockList);

        TrendyolProduct savedProduct = trendyolProductRepository.save(product);

        // Trigger stock-order synchronization after adding stock
        try {
            UUID storeId = savedProduct.getStore().getId();
            log.info("Triggering stock-order synchronization after adding stock for product {} in store {}", productId, storeId);
            stockOrderSyncService.synchronizeOrdersAfterStockChange(storeId, newInfo.getStockDate());
        } catch (Exception e) {
            log.warn("Failed to trigger stock-order synchronization after adding stock: {}", e.getMessage());
        }
        
        return productMapper.toDto(savedProduct);
    }
    
    @Transactional
    public TrendyolProductDto updateStockInfoByDate(UUID productId, LocalDate stockDate, UpdateStockInfoRequest request) {
        TrendyolProduct product = trendyolProductRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId.toString()));
        
        List<CostAndStockInfo> costAndStockList = product.getCostAndStockInfo();
        if (costAndStockList == null) {
            throw ResourceNotFoundException.stockInfo(productId.toString());
        }
        
        // Find and update the specific date entry
        boolean found = false;
        for (CostAndStockInfo info : costAndStockList) {
            if (info.getStockDate().equals(stockDate)) {
                info.setQuantity(request.getQuantity());
                info.setUnitCost(request.getUnitCost());
                info.setCostVatRate(request.getCostVatRate());
                info.setCostSource("MANUAL"); // Clear auto-detected flag on manual edit
                found = true;
                break;
            }
        }
        
        if (!found) {
            throw ResourceNotFoundException.stockInfoForDate(stockDate.toString());
        }
        
        product.setCostAndStockInfo(costAndStockList);
        
        TrendyolProduct savedProduct = trendyolProductRepository.save(product);
        
        // Trigger stock-order synchronization after updating stock
        try {
            UUID storeId = savedProduct.getStore().getId();
            log.info("Triggering stock-order synchronization after updating stock for product {} in store {} on date {}", productId, storeId, stockDate);
            stockOrderSyncService.synchronizeOrdersAfterStockChange(storeId, stockDate);
        } catch (Exception e) {
            log.warn("Failed to trigger stock-order synchronization after updating stock: {}", e.getMessage());
        }
        
        return productMapper.toDto(savedProduct);
    }
    
    @Transactional
    public TrendyolProductDto deleteStockInfoByDate(UUID productId, LocalDate stockDate) {
        TrendyolProduct product = trendyolProductRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId.toString()));

        List<CostAndStockInfo> costAndStockList = product.getCostAndStockInfo();
        if (costAndStockList == null) {
            throw ResourceNotFoundException.stockInfo(productId.toString());
        }

        // Remove the specific date entry
        boolean removed = costAndStockList.removeIf(info -> info.getStockDate().equals(stockDate));

        if (!removed) {
            throw ResourceNotFoundException.stockInfoForDate(stockDate.toString());
        }

        product.setCostAndStockInfo(costAndStockList);

        TrendyolProduct savedProduct = trendyolProductRepository.save(product);

        // Trigger stock-order synchronization after deleting stock
        try {
            UUID storeId = savedProduct.getStore().getId();
            log.info("Triggering stock-order synchronization after deleting stock for product {} in store {} on date {}", productId, storeId, stockDate);
            stockOrderSyncService.synchronizeOrdersAfterStockChange(storeId, stockDate);
        } catch (Exception e) {
            log.warn("Failed to trigger stock-order synchronization after deleting stock: {}", e.getMessage());
        }

        return productMapper.toDto(savedProduct);
    }

    @Transactional
    public BulkCostUpdateResponse bulkUpdateCosts(UUID storeId, BulkCostUpdateRequest request) {
        if (!storeRepository.existsById(storeId)) {
            throw new StoreNotFoundException("Store not found");
        }

        List<BulkCostUpdateResponse.FailedItem> failedItems = new ArrayList<>();
        int successCount = 0;
        LocalDate today = LocalDate.now();

        for (BulkCostUpdateRequest.CostUpdateItem item : request.getItems()) {
            try {
                // Find product by barcode and store
                TrendyolProduct product = trendyolProductRepository
                        .findByStoreIdAndBarcode(storeId, item.getBarcode())
                        .orElse(null);

                if (product == null) {
                    failedItems.add(BulkCostUpdateResponse.FailedItem.builder()
                            .barcode(item.getBarcode())
                            .reason("Ürün bulunamadı")
                            .build());
                    continue;
                }

                // Create new cost and stock info
                // Use stockDate from request, fallback to today if not provided
                LocalDate stockDate = item.getStockDate() != null ? item.getStockDate() : today;
                CostAndStockInfo newInfo = CostAndStockInfo.builder()
                        .quantity(item.getQuantity() != null ? item.getQuantity() : 1)
                        .unitCost(item.getUnitCost().doubleValue())
                        .costVatRate(item.getCostVatRate() != null ? item.getCostVatRate().intValue() : 18)
                        .stockDate(stockDate)
                        .costSource("MANUAL")
                        .build();

                // Add to existing list or merge with same date
                List<CostAndStockInfo> costAndStockList = product.getCostAndStockInfo();
                if (costAndStockList == null) {
                    costAndStockList = new ArrayList<>();
                }

                addOrMergeCostAndStockInfo(costAndStockList, newInfo);
                product.setCostAndStockInfo(costAndStockList);
                trendyolProductRepository.save(product);

                successCount++;
            } catch (Exception e) {
                log.error("Error updating cost for barcode {}: {}", item.getBarcode(), e.getMessage());
                failedItems.add(BulkCostUpdateResponse.FailedItem.builder()
                        .barcode(item.getBarcode())
                        .reason("Güncelleme hatası: " + e.getMessage())
                        .build());
            }
        }

        // Trigger stock-order synchronization after bulk update
        if (successCount > 0) {
            try {
                log.info("Triggering stock-order synchronization after bulk cost update for store {}", storeId);
                stockOrderSyncService.synchronizeOrdersAfterStockChange(storeId, today);
            } catch (Exception e) {
                log.warn("Failed to trigger stock-order synchronization after bulk cost update: {}", e.getMessage());
            }
        }

        return BulkCostUpdateResponse.builder()
                .totalProcessed(request.getItems().size())
                .successCount(successCount)
                .failureCount(failedItems.size())
                .failedItems(failedItems)
                .build();
    }

    private void addOrMergeCostAndStockInfo(List<CostAndStockInfo> costAndStockList, CostAndStockInfo newInfo) {
        // Check if there's already an entry for this date
        for (CostAndStockInfo existingInfo : costAndStockList) {
            if (existingInfo.getStockDate().equals(newInfo.getStockDate())) {
                // Merge with existing entry (weighted average for both cost and VAT rate)
                BigDecimal existingQuantity = BigDecimal.valueOf(existingInfo.getQuantity());
                BigDecimal newQuantity = BigDecimal.valueOf(newInfo.getQuantity());
                BigDecimal totalQuantity = existingQuantity.add(newQuantity);

                // Weighted average cost calculation using BigDecimal
                BigDecimal existingCost = existingInfo.getUnitCost() != null
                        ? BigDecimal.valueOf(existingInfo.getUnitCost()) : BigDecimal.ZERO;
                BigDecimal newCost = newInfo.getUnitCost() != null
                        ? BigDecimal.valueOf(newInfo.getUnitCost()) : BigDecimal.ZERO;
                BigDecimal totalCost = existingQuantity.multiply(existingCost)
                        .add(newQuantity.multiply(newCost));
                BigDecimal weightedAverageCost = totalCost.divide(totalQuantity, 2, RoundingMode.HALF_UP);

                // Weighted average VAT rate calculation using BigDecimal
                BigDecimal existingVatRate = BigDecimal.valueOf(
                        existingInfo.getCostVatRate() != null ? existingInfo.getCostVatRate() : 0);
                BigDecimal newVatRate = BigDecimal.valueOf(
                        newInfo.getCostVatRate() != null ? newInfo.getCostVatRate() : 0);
                BigDecimal totalVatWeighted = existingQuantity.multiply(existingVatRate)
                        .add(newQuantity.multiply(newVatRate));
                BigDecimal weightedAverageVatRate = totalVatWeighted.divide(totalQuantity, 0, RoundingMode.HALF_UP);

                existingInfo.setQuantity(totalQuantity.intValue());
                existingInfo.setUnitCost(weightedAverageCost.doubleValue());
                existingInfo.setCostVatRate(weightedAverageVatRate.intValue());
                // Propagate costSource from the newer entry
                if (newInfo.getCostSource() != null) {
                    existingInfo.setCostSource(newInfo.getCostSource());
                }
                return;
            }
        }

        // No existing entry for this date, add new one
        costAndStockList.add(newInfo);
    }

    private void clearAutoDetectedFlags(List<CostAndStockInfo> costAndStockList) {
        for (CostAndStockInfo info : costAndStockList) {
            if ("AUTO_DETECTED".equals(info.getCostSource())) {
                info.setCostSource("MANUAL");
            }
        }
    }

    private TrendyolCredentials extractTrendyolCredentials(Store store) {
        MarketplaceCredentials credentials = store.getCredentials();
        if (credentials instanceof TrendyolCredentials) {
            return (TrendyolCredentials) credentials;
        }
        return null;
    }
    
    private HttpHeaders createAuthHeaders(TrendyolCredentials credentials) {
        String auth = credentials.getApiKey() + ":" + credentials.getApiSecret();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.set("User-Agent", "SellerX/1.0");
        
        return headers;
    }
}
