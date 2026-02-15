package com.ecommerce.sellerx.stores;

import com.ecommerce.sellerx.common.exception.UnauthorizedAccessException;
import com.ecommerce.sellerx.expenses.ExpenseCategory;
import com.ecommerce.sellerx.expenses.ExpenseCategoryRepository;
import com.ecommerce.sellerx.webhook.TrendyolWebhookManagementService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Service
@Slf4j
public class StoreService {
    private final StoreRepository storeRepository;
    private final StoreMapper storeMapper;
    private final com.ecommerce.sellerx.users.UserService userService;
    private final TrendyolWebhookManagementService webhookManagementService;
    private final StoreOnboardingService storeOnboardingService;
    private final ExpenseCategoryRepository expenseCategoryRepository;

    public List<StoreDto> getStoresByUser(com.ecommerce.sellerx.users.User user) {
        return storeRepository.findAllByUser(user)
                .stream()
                .map(storeMapper::toDto)
                .toList();
    }

    public List<StoreDto> getAllStores(String sortBy) {
        if (!List.of("storeName", "marketplace").contains(sortBy))
            sortBy = "storeName";
        return storeRepository.findAll(Sort.by(sortBy))
                .stream()
                .map(storeMapper::toDto)
                .toList();
    }

    public StoreDto getStore(UUID storeId) {
        var store = storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException("Store not found"));
        return storeMapper.toDto(store);
    }

    @Transactional
    public StoreDto registerStore(RegisterStoreRequest request, com.ecommerce.sellerx.users.User user) {
        var store = storeMapper.toEntity(request);
        store.setUser(user);
        store.setCreatedAt(java.time.LocalDateTime.now());
        store.setUpdatedAt(java.time.LocalDateTime.now());

        // Initialize sync status for new store
        store.setSyncStatus(SyncStatus.PENDING);
        store.setInitialSyncCompleted(false);

        // Note: Credentials are encrypted automatically by CredentialsEntityListener

        // Save store first
        storeRepository.save(store);

        // Create default expense categories for the new store
        createDefaultExpenseCategories(store);

        // Create webhook for Trendyol stores
        if ("TRENDYOL".equalsIgnoreCase(store.getMarketplace()) && store.getCredentials() instanceof TrendyolCredentials) {
            TrendyolCredentials trendyolCredentials = (TrendyolCredentials) store.getCredentials();

            try {
                // Create webhook and handle result
                TrendyolWebhookManagementService.WebhookResult webhookResult =
                        webhookManagementService.createWebhookForStore(trendyolCredentials);

                if (webhookResult.isDisabled()) {
                    // Webhooks are disabled globally - mark status accordingly
                    store.setWebhookStatus(WebhookStatus.INACTIVE);
                    log.info("Webhooks disabled - store {} created without webhook", store.getId());
                } else if (webhookResult.isSuccess()) {
                    // Webhook created successfully
                    store.setWebhookId(webhookResult.getWebhookId());
                    store.setWebhookStatus(WebhookStatus.ACTIVE);
                    store.setWebhookErrorMessage(null);
                    log.info("Webhook created successfully for store {} with ID: {}", store.getId(), webhookResult.getWebhookId());
                } else {
                    // Webhook creation failed
                    store.setWebhookStatus(WebhookStatus.FAILED);
                    store.setWebhookErrorMessage(webhookResult.getErrorMessage());
                    log.warn("Webhook creation failed for store {}: {}", store.getId(), webhookResult.getErrorMessage());
                }
            } catch (Exception e) {
                // Webhook creation threw an exception - mark as failed but continue with onboarding
                store.setWebhookStatus(WebhookStatus.FAILED);
                store.setWebhookErrorMessage("Exception during webhook creation: " + e.getMessage());
                log.error("Exception during webhook creation for store {}: {}", store.getId(), e.getMessage(), e);
            }

            storeRepository.save(store); // Save with webhook status

            // Start initial data sync asynchronously (always, even if webhook failed)
            // This runs in background: products → orders → financial
            log.info("[ONBOARDING] [START] Mağaza (ID: {}, İsim: {}) için motorlar ateşlendi. Webhook durumu: {}", 
                    store.getId(), store.getStoreName(), store.getWebhookStatus());
            storeOnboardingService.performInitialSync(store);
        }

        return storeMapper.toDto(store);
    }

    public StoreDto updateStore(UUID storeId, UpdateStoreRequest request) {
        var store = storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException("Store not found"));
        storeMapper.update(request, store);
        store.setUpdatedAt(java.time.LocalDateTime.now());
        storeRepository.save(store);
        return storeMapper.toDto(store);
    }

    @Transactional
    public StoreDto updateStoreByUser(UUID storeId, UpdateStoreRequest request, com.ecommerce.sellerx.users.User user) {
        var store = storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException("Store not found"));
        if (!store.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedAccessException("Store", storeId.toString());
        }
        storeMapper.update(request, store);
        store.setUpdatedAt(java.time.LocalDateTime.now());
        storeRepository.save(store);
        return storeMapper.toDto(store);
    }

    @Transactional
    public void deleteStoreByUser(UUID storeId, com.ecommerce.sellerx.users.User user) {
        var store = storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException("Store not found"));
        if (!store.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedAccessException("Store", storeId.toString());
        }

        // Delete webhook if exists
        if (store.getWebhookId() != null && "TRENDYOL".equalsIgnoreCase(store.getMarketplace())) {
            TrendyolCredentials trendyolCredentials = (TrendyolCredentials) store.getCredentials();
            TrendyolWebhookManagementService.WebhookResult deleteResult =
                    webhookManagementService.deleteWebhookForStore(trendyolCredentials, store.getWebhookId());
            if (!deleteResult.isSuccess() && !deleteResult.isDisabled()) {
                log.warn("Failed to delete webhook for store {}: {}", storeId, deleteResult.getErrorMessage());
            }
        }
        
        // Store siliniyor - selected store logic'i
        UUID currentSelectedStoreId = user.getSelectedStoreId();
        boolean isSelectedStore = currentSelectedStoreId != null && currentSelectedStoreId.equals(storeId);
        
        if (isSelectedStore) {
            // Silinecek store = selected store
            // Diğer store'ları bul
            List<Store> remainingStores = storeRepository.findAllByUser(user)
                    .stream()
                    .filter(s -> !s.getId().equals(storeId))
                    .toList();
            
            if (!remainingStores.isEmpty()) {
                // Başka store varsa ilkini seç
                UUID newSelectedStoreId = remainingStores.get(0).getId();
                userService.setSelectedStoreId(user.getId(), newSelectedStoreId);
            } else {
                // Son store siliniyorsa selected_store_id = null
                userService.setSelectedStoreId(user.getId(), null);
            }
        }
        
        storeRepository.deleteByIdAndUser(storeId, user);
    }

    public boolean isStoreOwnedByUser(UUID storeId, Long userId) {
        var store = storeRepository.findById(storeId).orElse(null);
        return store != null && store.getUser().getId().equals(userId);
    }

    public boolean isStoreOwnedByUser(String storeIdString, Long userId) {
        try {
            UUID storeId = UUID.fromString(storeIdString);
            return isStoreOwnedByUser(storeId, userId);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Creates default expense categories for a newly registered store.
     * These categories help users get started with expense tracking immediately.
     */
    private void createDefaultExpenseCategories(Store store) {
        List<String> defaultCategories = List.of(
            "Ambalaj",    // Packaging
            "Kargo",      // Shipping
            "Reklam",     // Advertising
            "Ofis",       // Office
            "Muhasebe",   // Accounting
            "Diğer"       // Other
        );

        LocalDateTime now = LocalDateTime.now();
        for (String categoryName : defaultCategories) {
            ExpenseCategory category = new ExpenseCategory();
            category.setStore(store);
            category.setName(categoryName);
            category.setCreatedAt(now);
            category.setUpdatedAt(now);
            expenseCategoryRepository.save(category);
        }

        log.info("Created {} default expense categories for store {}", defaultCategories.size(), store.getId());
    }
}
