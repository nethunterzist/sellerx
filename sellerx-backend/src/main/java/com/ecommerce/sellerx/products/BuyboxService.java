package com.ecommerce.sellerx.products;

import com.ecommerce.sellerx.common.TrendyolRateLimiter;
import com.ecommerce.sellerx.products.dto.BuyboxInfoDto;
import com.ecommerce.sellerx.products.dto.BuyboxSummaryDto;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.stores.StoreNotFoundException;
import com.ecommerce.sellerx.stores.TrendyolCredentials;
import com.ecommerce.sellerx.stores.MarketplaceCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BuyboxService {

    private static final String TRENDYOL_BASE_URL = "https://apigw.trendyol.com";
    private static final int BUYBOX_BATCH_SIZE = 10;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000;

    private final TrendyolProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final RestTemplate restTemplate;
    private final TrendyolRateLimiter rateLimiter;

    /**
     * Sync buybox information for a single store by storeId.
     */
    @Transactional
    public void syncBuyboxForStore(UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException("Store not found"));
        syncBuyboxForStore(store);
    }

    /**
     * Sync buybox information for a single store.
     * Calls Trendyol Buybox API with batches of 10 barcodes.
     */
    @Transactional
    public void syncBuyboxForStore(Store store) {
        TrendyolCredentials credentials = extractTrendyolCredentials(store);
        if (credentials == null) {
            log.warn("[BUYBOX] No Trendyol credentials for store: {}", store.getId());
            return;
        }

        List<TrendyolProduct> onSaleProducts = productRepository.findOnSaleProductsWithBarcode(store.getId());
        if (onSaleProducts.isEmpty()) {
            log.info("[BUYBOX] No on-sale products for store: {}", store.getId());
            return;
        }

        log.info("[BUYBOX] Starting buybox sync for store {} with {} products", store.getId(), onSaleProducts.size());

        // Create barcode -> product map
        Map<String, TrendyolProduct> barcodeMap = onSaleProducts.stream()
                .filter(p -> p.getBarcode() != null && !p.getBarcode().isEmpty())
                .collect(Collectors.toMap(TrendyolProduct::getBarcode, p -> p, (a, b) -> a));

        List<String> barcodes = new ArrayList<>(barcodeMap.keySet());
        int totalUpdated = 0;

        // Process in batches of 10
        for (int i = 0; i < barcodes.size(); i += BUYBOX_BATCH_SIZE) {
            List<String> batch = barcodes.subList(i, Math.min(i + BUYBOX_BATCH_SIZE, barcodes.size()));

            try {
                List<BuyboxApiResponse> buyboxResults = fetchBuyboxBatch(credentials, batch, store.getId());

                for (BuyboxApiResponse result : buyboxResults) {
                    TrendyolProduct product = barcodeMap.get(result.barcode());
                    if (product != null) {
                        product.setBuyboxOrder(result.buyboxOrder());
                        product.setBuyboxPrice(result.buyboxPrice());
                        product.setHasMultipleSeller(result.hasMultipleSeller());
                        product.setBuyboxUpdatedAt(LocalDateTime.now());
                        productRepository.save(product);
                        totalUpdated++;
                    }
                }
            } catch (Exception e) {
                log.error("[BUYBOX] Error processing batch starting at index {} for store {}: {}",
                        i, store.getId(), e.getMessage());
            }
        }

        log.info("[BUYBOX] Buybox sync completed for store {}: {} products updated", store.getId(), totalUpdated);
    }

    /**
     * Sync buybox information for all stores.
     */
    public void syncBuyboxForAllStores() {
        List<Store> stores = storeRepository.findByInitialSyncCompletedTrue();
        int successCount = 0;
        int failCount = 0;

        for (Store store : stores) {
            if (!"trendyol".equalsIgnoreCase(store.getMarketplace())) {
                continue;
            }

            try {
                syncBuyboxForStore(store);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("[BUYBOX] Buybox sync failed for store {}: {}", store.getId(), e.getMessage());
            }
        }

        log.info("[BUYBOX] Buybox sync completed for all stores: {} success, {} failed", successCount, failCount);
    }

    /**
     * Get buybox summary statistics for a store.
     */
    public BuyboxSummaryDto getBuyboxSummary(UUID storeId) {
        if (!storeRepository.existsById(storeId)) {
            throw new StoreNotFoundException("Store not found");
        }

        int totalProducts = productRepository.countOnSaleProducts(storeId);
        int winning = productRepository.countBuyboxWinning(storeId);
        int losing = productRepository.countBuyboxLosing(storeId);
        int withCompetitors = productRepository.countWithCompetitors(storeId);
        int noCompetition = totalProducts - withCompetitors;
        int notChecked = totalProducts - winning - losing - noCompetition;
        double winRate = (winning + losing) > 0
                ? ((double) winning / (winning + losing)) * 100
                : 0.0;

        return BuyboxSummaryDto.builder()
                .totalProducts(totalProducts)
                .buyboxWinning(winning)
                .buyboxLosing(losing)
                .withCompetitors(withCompetitors)
                .noCompetition(noCompetition)
                .notChecked(notChecked)
                .winRate(Math.round(winRate * 100.0) / 100.0)
                .build();
    }

    /**
     * Get paginated buybox product list with filters.
     */
    public Page<BuyboxInfoDto> getBuyboxProducts(UUID storeId, String search, String status,
                                                  String sortBy, String sortDir, int page, int size) {
        if (!storeRepository.existsById(storeId)) {
            throw new StoreNotFoundException("Store not found");
        }

        // Default sort
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "buyboxOrder";
        }
        if (sortDir == null || sortDir.isEmpty()) {
            sortDir = "asc";
        }

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));

        Page<TrendyolProduct> productsPage = productRepository.findBuyboxProducts(
                storeId, search, status, sortBy, sortDir, pageable);

        return productsPage.map(this::toBuyboxInfoDto);
    }

    private BuyboxInfoDto toBuyboxInfoDto(TrendyolProduct product) {
        BigDecimal priceDifference = null;
        if (product.getSalePrice() != null && product.getBuyboxPrice() != null) {
            priceDifference = product.getSalePrice().subtract(product.getBuyboxPrice());
        }

        String buyboxStatus;
        if (product.getBuyboxOrder() == null) {
            buyboxStatus = "NOT_CHECKED";
        } else if (!Boolean.TRUE.equals(product.getHasMultipleSeller())) {
            buyboxStatus = "NO_COMPETITION";
        } else if (product.getBuyboxOrder() == 1) {
            buyboxStatus = "WINNING";
        } else {
            buyboxStatus = "LOSING";
        }

        return BuyboxInfoDto.builder()
                .productId(product.getId())
                .barcode(product.getBarcode())
                .title(product.getTitle())
                .image(product.getImage())
                .productUrl(product.getProductUrl())
                .salePrice(product.getSalePrice())
                .buyboxOrder(product.getBuyboxOrder())
                .buyboxPrice(product.getBuyboxPrice())
                .hasMultipleSeller(product.getHasMultipleSeller())
                .priceDifference(priceDifference)
                .buyboxStatus(buyboxStatus)
                .buyboxUpdatedAt(product.getBuyboxUpdatedAt())
                .build();
    }

    // ==================== Trendyol API Integration ====================

    private record BuyboxApiResponse(String barcode, Integer buyboxOrder, BigDecimal buyboxPrice, Boolean hasMultipleSeller) {}

    @SuppressWarnings("unchecked")
    private List<BuyboxApiResponse> fetchBuyboxBatch(TrendyolCredentials credentials, List<String> barcodes, UUID storeId) {
        String url = String.format("%s/integration/product/sellers/%d/products/buybox-information",
                TRENDYOL_BASE_URL, credentials.getSellerId());

        HttpHeaders headers = createAuthHeaders(credentials);
        Map<String, Object> requestBody = Map.of("barcodes", barcodes);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = executeWithRetry(
                () -> restTemplate.exchange(url, HttpMethod.POST, entity, Map.class),
                "buybox-batch",
                storeId
        );

        List<BuyboxApiResponse> results = new ArrayList<>();

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Map<String, Object> body = response.getBody();

            // Trendyol API returns "buyboxInfo" array with flat objects:
            // {"buyboxInfo": [{"barcode":"...", "buyboxOrder":1, "buyboxPrice":849.75, "hasMultipleSeller":false}]}
            Object buyboxData = body.get("buyboxInfo");
            if (buyboxData instanceof List<?> buyboxList) {
                for (Object item : buyboxList) {
                    if (item instanceof Map<?, ?> itemMap) {
                        String barcode = (String) itemMap.get("barcode");

                        // buyboxOrder: seller's position (1 = winning)
                        Number orderNum = (Number) itemMap.get("buyboxOrder");
                        Integer buyboxOrder = orderNum != null ? orderNum.intValue() : null;

                        // buyboxPrice: the buybox winner's price
                        Number priceNum = (Number) itemMap.get("buyboxPrice");
                        BigDecimal buyboxPrice = priceNum != null
                                ? BigDecimal.valueOf(priceNum.doubleValue())
                                : null;

                        // hasMultipleSeller: whether there are competitors
                        Boolean hasMultiple = (Boolean) itemMap.get("hasMultipleSeller");
                        if (hasMultiple == null) hasMultiple = false;

                        results.add(new BuyboxApiResponse(barcode, buyboxOrder, buyboxPrice, hasMultiple));
                    }
                }
            }
        }

        return results;
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
                    log.warn("[BUYBOX] [{}] Attempt {}/{} failed, retrying in {}ms: {}",
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
