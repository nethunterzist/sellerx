package com.ecommerce.sellerx.stores;

import com.ecommerce.sellerx.webhook.TrendyolWebhookManagementService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Service
public class StoreService {
    private final StoreRepository storeRepository;
    private final StoreMapper storeMapper;
    private final com.ecommerce.sellerx.users.UserService userService;
    private final TrendyolWebhookManagementService webhookManagementService;

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

    public StoreDto registerStore(RegisterStoreRequest request, com.ecommerce.sellerx.users.User user) {
        var store = storeMapper.toEntity(request);
        store.setUser(user);
        store.setCreatedAt(java.time.LocalDateTime.now());
        store.setUpdatedAt(java.time.LocalDateTime.now());
        
        // Save store first
        storeRepository.save(store);
        
        // Create webhook for Trendyol stores
        if ("TRENDYOL".equals(store.getMarketplace()) && store.getCredentials() instanceof TrendyolCredentials) {
            TrendyolCredentials trendyolCredentials = (TrendyolCredentials) store.getCredentials();
            String webhookId = webhookManagementService.createWebhookForStore(trendyolCredentials);
            if (webhookId != null) {
                store.setWebhookId(webhookId);
                storeRepository.save(store); // Save again with webhook ID
            }
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

    public StoreDto updateStoreByUser(UUID storeId, UpdateStoreRequest request, com.ecommerce.sellerx.users.User user) {
        var store = storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException("Store not found"));
        if (!store.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Store does not belong to user");
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
            throw new RuntimeException("Store does not belong to user");
        }
        
        // Delete webhook if exists
        if (store.getWebhookId() != null && "TRENDYOL".equals(store.getMarketplace())) {
            TrendyolCredentials trendyolCredentials = (TrendyolCredentials) store.getCredentials();
            webhookManagementService.deleteWebhookForStore(trendyolCredentials, store.getWebhookId());
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
}
