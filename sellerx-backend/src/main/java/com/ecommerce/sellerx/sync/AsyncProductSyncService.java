package com.ecommerce.sellerx.sync;

import com.ecommerce.sellerx.alerts.AlertEngine;
import com.ecommerce.sellerx.categories.TrendyolCategory;
import com.ecommerce.sellerx.categories.TrendyolCategoryRepository;
import com.ecommerce.sellerx.common.TrendyolRateLimiter;
import com.ecommerce.sellerx.products.*;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreNotFoundException;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.stores.TrendyolCredentials;
import com.ecommerce.sellerx.stores.MarketplaceCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.function.Supplier;

/**
 * Async service for running product sync operations in the background.
 * Delegates to TrendyolProductService for actual sync logic but tracks progress.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncProductSyncService {

    private static final String TRENDYOL_BASE_URL = "https://apigw.trendyol.com";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000;

    private final SyncTaskService syncTaskService;
    private final TrendyolProductRepository productRepository;
    private final TrendyolCategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final RestTemplate restTemplate;
    private final TrendyolRateLimiter rateLimiter;
    private final AlertEngine alertEngine;
    private final AutoStockDetectionService autoStockDetectionService;

    /**
     * Execute product sync asynchronously.
     * This method runs in a separate thread and updates task progress.
     */
    @Async("taskExecutor")
    public void executeProductSync(UUID taskId, UUID storeId) {
        log.info("Starting async product sync for task {} store {}", taskId, storeId);

        try {
            // Mark task as started
            syncTaskService.startTask(taskId);

            Store store = storeRepository.findById(storeId)
                    .orElseThrow(() -> new StoreNotFoundException("Store not found"));

            TrendyolCredentials credentials = extractTrendyolCredentials(store);
            if (credentials == null) {
                syncTaskService.failTask(taskId, "Trendyol credentials not found");
                return;
            }

            int totalFetched = 0;
            int totalSaved = 0;
            int totalUpdated = 0;
            int totalSkipped = 0;
            int page = 0;
            int size = 200;
            boolean hasMorePages = true;
            int totalPages = 1;

            while (hasMorePages) {
                final String url = String.format("%s/integration/product/sellers/%d/products?size=%d&page=%d",
                        TRENDYOL_BASE_URL, credentials.getSellerId(), size, page);

                HttpHeaders headers = createAuthHeaders(credentials);
                HttpEntity<String> entity = new HttpEntity<>(headers);

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

                    // Update task progress
                    syncTaskService.updateProgress(
                            taskId,
                            page + 1,
                            totalPages,
                            totalFetched,
                            totalSaved,
                            totalUpdated,
                            totalSkipped
                    );

                    log.info("[PRODUCTS] Task {} - Page {}/{} complete: {} items processed",
                            taskId, page + 1, totalPages, totalFetched);

                    hasMorePages = page < (apiResponse.getTotalPages() - 1);
                    page++;
                } else {
                    syncTaskService.failTask(taskId,
                            "Failed to fetch products from Trendyol. Status: " + response.getStatusCode());
                    return;
                }
            }

            // Complete the task
            syncTaskService.completeTask(taskId, totalFetched, totalSaved, totalUpdated, totalSkipped);

            // Check stock alerts after sync (best effort)
            try {
                List<TrendyolProduct> allProducts = productRepository.findByStoreId(storeId);
                alertEngine.checkStockAlerts(store, allProducts);
            } catch (Exception e) {
                log.error("[PRODUCTS] Task {} - Error triggering stock alerts: {}", taskId, e.getMessage());
            }

            log.info("[PRODUCTS] Task {} completed: {} total, {} new, {} updated, {} skipped",
                    taskId, totalFetched, totalSaved, totalUpdated, totalSkipped);

        } catch (Exception e) {
            log.error("Error in async product sync task {}: {}", taskId, e.getMessage(), e);
            syncTaskService.failTask(taskId, "Error syncing products: " + e.getMessage());
        }
    }

    private ProductSyncResult saveOrUpdateProduct(Store store, TrendyolApiProductResponse.TrendyolApiProduct apiProduct) {
        boolean isNew = !productRepository.existsByStoreIdAndProductId(store.getId(), apiProduct.getId());

        TrendyolProduct product = productRepository
                .findByStoreIdAndProductId(store.getId(), apiProduct.getId())
                .orElse(TrendyolProduct.builder()
                        .store(store)
                        .productId(apiProduct.getId())
                        .costAndStockInfo(new ArrayList<>())
                        .build());

        int oldQuantity = isNew ? 0 : (product.getTrendyolQuantity() != null ? product.getTrendyolQuantity() : 0);
        int newQuantity = apiProduct.getQuantity() != null ? apiProduct.getQuantity() : 0;

        boolean hasChanges = isNew || hasProductChanged(product, apiProduct);

        if (hasChanges) {
            updateProductFields(product, apiProduct);
            product.setPreviousTrendyolQuantity(oldQuantity);
            productRepository.save(product);

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

    private boolean hasProductChanged(TrendyolProduct existing, TrendyolApiProductResponse.TrendyolApiProduct incoming) {
        return !Objects.equals(existing.getBarcode(), incoming.getBarcode())
                || !Objects.equals(existing.getTitle(), incoming.getTitle())
                || !Objects.equals(existing.getCategoryName(), incoming.getCategoryName())
                || !Objects.equals(existing.getTrendyolQuantity(), incoming.getQuantity())
                || isBigDecimalChanged(existing.getSalePrice(), incoming.getSalePrice())
                || !Objects.equals(existing.getBrand(), incoming.getBrand())
                || !Objects.equals(existing.getOnSale(), incoming.getOnsale());
    }

    private boolean isBigDecimalChanged(java.math.BigDecimal existing, java.math.BigDecimal incoming) {
        if (existing == null && incoming == null) return false;
        if (existing == null || incoming == null) return true;
        return existing.compareTo(incoming) != 0;
    }

    private void updateProductFields(TrendyolProduct product, TrendyolApiProductResponse.TrendyolApiProduct apiProduct) {
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

        if (apiProduct.getPimCategoryId() != null) {
            categoryRepository.findByCategoryId(apiProduct.getPimCategoryId())
                    .ifPresent(category -> {
                        product.setCommissionRate(category.getCommissionRate());
                        product.setShippingVolumeWeight(category.getAverageShipmentSize());
                    });
        }

        product.setApproved(apiProduct.getApproved() != null ? apiProduct.getApproved() : false);
        product.setArchived(apiProduct.getArchived() != null ? apiProduct.getArchived() : false);
        product.setBlacklisted(apiProduct.getBlacklisted() != null ? apiProduct.getBlacklisted() : false);
        product.setRejected(apiProduct.getRejected() != null ? apiProduct.getRejected() : false);
        product.setOnSale(apiProduct.getOnsale() != null ? apiProduct.getOnsale() : false);

        if (apiProduct.getImages() != null && !apiProduct.getImages().isEmpty()) {
            product.setImage(apiProduct.getImages().get(0).getUrl());
        }
    }

    private <T> T executeWithRetry(Supplier<T> operation, String operationName, UUID storeId) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < MAX_RETRIES) {
            try {
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
