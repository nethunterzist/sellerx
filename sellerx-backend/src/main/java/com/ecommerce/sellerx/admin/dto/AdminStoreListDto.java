package com.ecommerce.sellerx.admin.dto;

import com.ecommerce.sellerx.stores.SyncStatus;
import com.ecommerce.sellerx.stores.WebhookStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStoreListDto {
    private UUID id;
    private String storeName;
    private String marketplace;

    // Owner info
    private Long userId;
    private String userEmail;

    // Sync status
    private SyncStatus syncStatus;
    private Boolean initialSyncCompleted;
    private WebhookStatus webhookStatus;

    // Stats
    private Long productCount;
    private Long orderCount;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime lastSyncAt;
}
