package com.ecommerce.sellerx.stores;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@AllArgsConstructor
@Builder
@Getter
public class StoreDto {
    private java.util.UUID id;
    private Long userId;
    private String storeName;
    private String marketplace;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MarketplaceCredentials credentials;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;

    // Webhook status fields
    private String webhookId;
    private WebhookStatus webhookStatus;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String webhookErrorMessage;

    // Legacy sync status fields (backward compatibility)
    private SyncStatus syncStatus;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String syncErrorMessage;
    private Boolean initialSyncCompleted;

    // New parallel sync fields
    private OverallSyncStatus overallSyncStatus;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, PhaseStatus> syncPhases;
}
