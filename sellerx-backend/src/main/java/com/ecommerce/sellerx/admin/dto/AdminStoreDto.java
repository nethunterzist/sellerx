package com.ecommerce.sellerx.admin.dto;

import com.ecommerce.sellerx.stores.HistoricalSyncStatus;
import com.ecommerce.sellerx.stores.OverallSyncStatus;
import com.ecommerce.sellerx.stores.SyncStatus;
import com.ecommerce.sellerx.stores.WebhookStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStoreDto {
    private UUID id;
    private String storeName;
    private String marketplace;

    // Owner info
    private Long userId;
    private String userEmail;
    private String userName;

    // Sync status
    private SyncStatus syncStatus;
    private String syncErrorMessage;
    private Boolean initialSyncCompleted;
    private OverallSyncStatus overallSyncStatus;
    private Map<String, Object> syncPhases;

    // Webhook status
    private String webhookId;
    private WebhookStatus webhookStatus;
    private String webhookErrorMessage;

    // Historical sync
    private HistoricalSyncStatus historicalSyncStatus;
    private LocalDateTime historicalSyncDate;
    private Integer historicalSyncTotalChunks;
    private Integer historicalSyncCompletedChunks;

    // Stats
    private Long productCount;
    private Long orderCount;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Credentials (masked)
    private String sellerId;
    private boolean hasApiKey;
    private boolean hasApiSecret;
}
