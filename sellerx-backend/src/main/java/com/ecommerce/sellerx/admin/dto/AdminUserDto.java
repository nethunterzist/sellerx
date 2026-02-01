package com.ecommerce.sellerx.admin.dto;

import com.ecommerce.sellerx.stores.SyncStatus;
import com.ecommerce.sellerx.users.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDto {
    private Long id;
    private String name;
    private String email;
    private Role role;
    private UUID selectedStoreId;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    // Store summary
    private int storeCount;
    private List<UserStoreInfo> stores;

    // Subscription info (if billing module is active)
    private String subscriptionStatus;
    private String planName;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStoreInfo {
        private UUID id;
        private String storeName;
        private String marketplace;
        private SyncStatus syncStatus;
        private Boolean initialSyncCompleted;
        private LocalDateTime createdAt;
    }
}
