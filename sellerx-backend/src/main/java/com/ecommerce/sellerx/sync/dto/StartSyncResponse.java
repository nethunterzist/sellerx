package com.ecommerce.sellerx.sync.dto;

import com.ecommerce.sellerx.sync.SyncTaskStatus;
import com.ecommerce.sellerx.sync.SyncTaskType;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Response when starting a new sync operation.
 */
@Data
@Builder
public class StartSyncResponse {

    private UUID taskId;
    private UUID storeId;
    private SyncTaskType taskType;
    private SyncTaskStatus status;
    private String message;

    /**
     * URL to poll for status updates
     */
    private String statusUrl;
}
