package com.ecommerce.sellerx.admin;

import com.ecommerce.sellerx.admin.dto.AdminStoreDto;
import com.ecommerce.sellerx.admin.dto.AdminStoreListDto;
import com.ecommerce.sellerx.orders.TrendyolOrderRepository;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreNotFoundException;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.stores.StoreOnboardingService;
import com.ecommerce.sellerx.stores.SyncStatus;
import com.ecommerce.sellerx.stores.TrendyolCredentials;
import com.ecommerce.sellerx.stores.WebhookStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminStoreService {

    private final StoreRepository storeRepository;
    private final TrendyolProductRepository productRepository;
    private final TrendyolOrderRepository orderRepository;
    private final StoreOnboardingService storeOnboardingService;

    /**
     * Get paginated list of all stores with basic info
     */
    public Page<AdminStoreListDto> getAllStores(Pageable pageable) {
        return storeRepository.findAll(pageable)
                .map(this::toListDto);
    }

    /**
     * Get detailed store info by ID
     */
    public AdminStoreDto getStoreById(UUID id) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new StoreNotFoundException("Store not found with id: " + id));
        return toDetailDto(store);
    }

    /**
     * Trigger manual sync for a store
     */
    @Transactional
    public AdminStoreDto triggerSync(UUID id) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new StoreNotFoundException("Store not found with id: " + id));

        log.info("Admin triggering manual sync for store: {} ({})", store.getStoreName(), id);

        // Reset sync status and trigger onboarding service
        store.setSyncStatus(SyncStatus.PENDING);
        store.setSyncErrorMessage(null);
        store = storeRepository.save(store);

        // Trigger async sync
        storeOnboardingService.retryInitialSync(store.getId());

        return toDetailDto(store);
    }

    /**
     * Get total store count
     */
    public long getTotalStoreCount() {
        return storeRepository.count();
    }

    /**
     * Get store count by sync status
     */
    public long getStoreCountBySyncStatus(SyncStatus status) {
        return storeRepository.findAll().stream()
                .filter(store -> status == store.getSyncStatus())
                .count();
    }

    /**
     * Get store count with sync errors
     */
    public long getStoreCountWithSyncErrors() {
        return storeRepository.findAll().stream()
                .filter(store -> SyncStatus.FAILED == store.getSyncStatus() || store.getSyncErrorMessage() != null)
                .count();
    }

    /**
     * Get store count with webhook errors
     */
    public long getStoreCountWithWebhookErrors() {
        return storeRepository.findAll().stream()
                .filter(store -> WebhookStatus.FAILED == store.getWebhookStatus() || store.getWebhookErrorMessage() != null)
                .count();
    }

    /**
     * Search stores by store name or user email
     */
    public List<AdminStoreListDto> searchStores(String query) {
        return storeRepository.findAll().stream()
                .filter(store -> store.getStoreName().toLowerCase().contains(query.toLowerCase())
                        || store.getUser().getEmail().toLowerCase().contains(query.toLowerCase()))
                .map(this::toListDto)
                .collect(Collectors.toList());
    }

    private AdminStoreListDto toListDto(Store store) {
        long productCount = productRepository.countByStoreId(store.getId());
        long orderCount = orderRepository.countByStoreId(store.getId());

        return AdminStoreListDto.builder()
                .id(store.getId())
                .storeName(store.getStoreName())
                .marketplace(store.getMarketplace())
                .userId(store.getUser().getId())
                .userEmail(store.getUser().getEmail())
                .syncStatus(store.getSyncStatus())
                .initialSyncCompleted(store.getInitialSyncCompleted())
                .webhookStatus(store.getWebhookStatus())
                .productCount(productCount)
                .orderCount(orderCount)
                .createdAt(store.getCreatedAt())
                .lastSyncAt(store.getUpdatedAt())
                .build();
    }

    private AdminStoreDto toDetailDto(Store store) {
        long productCount = productRepository.countByStoreId(store.getId());
        long orderCount = orderRepository.countByStoreId(store.getId());

        // Convert sync phases to simple map
        Map<String, Object> syncPhasesMap = new HashMap<>();
        if (store.getSyncPhases() != null) {
            store.getSyncPhases().forEach((key, value) -> {
                Map<String, Object> phaseInfo = new HashMap<>();
                phaseInfo.put("status", value.getStatus());
                phaseInfo.put("startedAt", value.getStartedAt());
                phaseInfo.put("completedAt", value.getCompletedAt());
                phaseInfo.put("errorMessage", value.getErrorMessage());
                syncPhasesMap.put(key, phaseInfo);
            });
        }

        return AdminStoreDto.builder()
                .id(store.getId())
                .storeName(store.getStoreName())
                .marketplace(store.getMarketplace())
                .userId(store.getUser().getId())
                .userEmail(store.getUser().getEmail())
                .userName(store.getUser().getName())
                .syncStatus(store.getSyncStatus())
                .syncErrorMessage(store.getSyncErrorMessage())
                .initialSyncCompleted(store.getInitialSyncCompleted())
                .overallSyncStatus(store.getOverallSyncStatus())
                .syncPhases(syncPhasesMap)
                .webhookId(store.getWebhookId())
                .webhookStatus(store.getWebhookStatus())
                .webhookErrorMessage(store.getWebhookErrorMessage())
                .historicalSyncStatus(store.getHistoricalSyncStatus())
                .historicalSyncDate(store.getHistoricalSyncDate())
                .historicalSyncTotalChunks(store.getHistoricalSyncTotalChunks())
                .historicalSyncCompletedChunks(store.getHistoricalSyncCompletedChunks())
                .productCount(productCount)
                .orderCount(orderCount)
                .createdAt(store.getCreatedAt())
                .updatedAt(store.getUpdatedAt())
                .sellerId(getTrendyolSellerId(store))
                .hasApiKey(hasTrendyolApiKey(store))
                .hasApiSecret(hasTrendyolApiSecret(store))
                .build();
    }

    private String getTrendyolSellerId(Store store) {
        if (store.getCredentials() instanceof TrendyolCredentials creds && creds.getSellerId() != null) {
            return creds.getSellerId().toString();
        }
        return null;
    }

    private boolean hasTrendyolApiKey(Store store) {
        if (store.getCredentials() instanceof TrendyolCredentials creds) {
            return creds.getApiKey() != null;
        }
        return false;
    }

    private boolean hasTrendyolApiSecret(Store store) {
        if (store.getCredentials() instanceof TrendyolCredentials creds) {
            return creds.getApiSecret() != null;
        }
        return false;
    }
}
