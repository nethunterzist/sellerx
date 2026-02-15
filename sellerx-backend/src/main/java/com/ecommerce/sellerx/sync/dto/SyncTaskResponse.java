package com.ecommerce.sellerx.sync.dto;

import com.ecommerce.sellerx.sync.SyncTaskStatus;
import com.ecommerce.sellerx.sync.SyncTaskType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for sync task status.
 */
@Data
@Builder
public class SyncTaskResponse {

    private UUID taskId;
    private UUID storeId;
    private SyncTaskType taskType;
    private SyncTaskStatus status;
    private Integer progressPercentage;
    private Integer currentPage;
    private Integer totalPages;
    private Integer itemsProcessed;
    private Integer itemsNew;
    private Integer itemsUpdated;
    private Integer itemsSkipped;
    private Integer itemsFailed;
    private String errorMessage;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
