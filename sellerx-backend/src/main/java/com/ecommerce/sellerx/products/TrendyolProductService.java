package com.ecommerce.sellerx.products;

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
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendyolProductService {
    
    private static final String TRENDYOL_BASE_URL = "https://apigw.trendyol.com";
    
    private final TrendyolProductRepository trendyolProductRepository;
    private final StoreRepository storeRepository;
    private final TrendyolProductMapper productMapper;
    private final RestTemplate restTemplate;
    private final StockOrderSynchronizationService stockOrderSyncService;
    private final TrendyolCategoryRepository trendyolCategoryRepository;
    
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
            
            while (hasMorePages) {
                String url = String.format("%s/integration/product/sellers/%d/products?size=%d&page=%d",
                        TRENDYOL_BASE_URL, credentials.getSellerId(), size, page);
                
                HttpHeaders headers = createAuthHeaders(credentials);
                HttpEntity<String> entity = new HttpEntity<>(headers);
                
                ResponseEntity<TrendyolApiProductResponse> response = restTemplate.exchange(
                        url, HttpMethod.GET, entity, TrendyolApiProductResponse.class);
                
                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    TrendyolApiProductResponse apiResponse = response.getBody();
                    
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
                    
                    // Check if there are more pages
                    hasMorePages = page < (apiResponse.getTotalPages() - 1);
                    page++;
                } else {
                    hasMorePages = false;
                    log.error("Failed to fetch products from Trendyol. Status: {}", response.getStatusCode());
                    return new SyncProductsResponse(false, "Failed to fetch products from Trendyol", 0, 0, 0, 0);
                }
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
        
        // Check if any field has changed (only if product exists)
        boolean hasChanges = isNew;
        
        if (!isNew) {
            hasChanges = hasProductChanged(product, apiProduct);
        }
        
        // Only update if there are changes
        if (hasChanges) {
            updateProductFields(product, apiProduct);
            trendyolProductRepository.save(product);
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
    
    public TrendyolProductDto updateCostAndStock(UUID productId, UpdateCostAndStockRequest request) {
        TrendyolProduct product = trendyolProductRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        // Create new cost and stock info
        CostAndStockInfo newInfo = CostAndStockInfo.builder()
                .quantity(request.getQuantity())
                .unitCost(request.getUnitCost())
                .costVatRate(request.getCostVatRate())
                .stockDate(request.getStockDate() != null ? request.getStockDate() : LocalDate.now())
                .build();
        
        // Add to existing list or merge with same date
        List<CostAndStockInfo> costAndStockList = product.getCostAndStockInfo();
        if (costAndStockList == null) {
            costAndStockList = new ArrayList<>();
        }
        
        addOrMergeCostAndStockInfo(costAndStockList, newInfo);
        product.setCostAndStockInfo(costAndStockList);
        
        TrendyolProduct savedProduct = trendyolProductRepository.save(product);
        return productMapper.toDto(savedProduct);
    }
    
    public TrendyolProductDto addStockInfo(UUID productId, AddStockInfoRequest request) {
        TrendyolProduct product = trendyolProductRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        CostAndStockInfo newInfo = CostAndStockInfo.builder()
                .quantity(request.getQuantity())
                .unitCost(request.getUnitCost())
                .costVatRate(request.getCostVatRate())
                .stockDate(request.getStockDate() != null ? request.getStockDate() : LocalDate.now())
                .build();
        
        List<CostAndStockInfo> costAndStockList = product.getCostAndStockInfo();
        if (costAndStockList == null) {
            costAndStockList = new ArrayList<>();
        }
        
        addOrMergeCostAndStockInfo(costAndStockList, newInfo);
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
    
    public TrendyolProductDto updateStockInfoByDate(UUID productId, LocalDate stockDate, UpdateStockInfoRequest request) {
        TrendyolProduct product = trendyolProductRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        List<CostAndStockInfo> costAndStockList = product.getCostAndStockInfo();
        if (costAndStockList == null) {
            throw new RuntimeException("No stock info found for this product");
        }
        
        // Find and update the specific date entry
        boolean found = false;
        for (CostAndStockInfo info : costAndStockList) {
            if (info.getStockDate().equals(stockDate)) {
                info.setQuantity(request.getQuantity());
                info.setUnitCost(request.getUnitCost());
                info.setCostVatRate(request.getCostVatRate());
                found = true;
                break;
            }
        }
        
        if (!found) {
            throw new RuntimeException("No stock info found for date: " + stockDate);
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
    
    public TrendyolProductDto deleteStockInfoByDate(UUID productId, LocalDate stockDate) {
        TrendyolProduct product = trendyolProductRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        List<CostAndStockInfo> costAndStockList = product.getCostAndStockInfo();
        if (costAndStockList == null) {
            throw new RuntimeException("No stock info found for this product");
        }
        
        // Remove the specific date entry
        boolean removed = costAndStockList.removeIf(info -> info.getStockDate().equals(stockDate));
        
        if (!removed) {
            throw new RuntimeException("No stock info found for date: " + stockDate);
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
    
    private void addOrMergeCostAndStockInfo(List<CostAndStockInfo> costAndStockList, CostAndStockInfo newInfo) {
        // Check if there's already an entry for this date
        for (CostAndStockInfo existingInfo : costAndStockList) {
            if (existingInfo.getStockDate().equals(newInfo.getStockDate())) {
                // Merge with existing entry (weighted average for both cost and VAT rate)
                int existingQuantity = existingInfo.getQuantity();
                int newQuantity = newInfo.getQuantity();
                int totalQuantity = existingQuantity + newQuantity;
                
                // Weighted average cost calculation
                double totalCost = (existingQuantity * existingInfo.getUnitCost()) + 
                                  (newQuantity * newInfo.getUnitCost());
                double weightedAverageCost = totalCost / totalQuantity;
                
                // Weighted average VAT rate calculation
                double totalVatWeighted = (existingQuantity * existingInfo.getCostVatRate()) + 
                                         (newQuantity * newInfo.getCostVatRate());
                double weightedAverageVatRate = totalVatWeighted / totalQuantity;
                
                existingInfo.setQuantity(totalQuantity);
                existingInfo.setUnitCost(weightedAverageCost);
                existingInfo.setCostVatRate((int) Math.round(weightedAverageVatRate));
                return;
            }
        }
        
        // No existing entry for this date, add new one
        costAndStockList.add(newInfo);
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
