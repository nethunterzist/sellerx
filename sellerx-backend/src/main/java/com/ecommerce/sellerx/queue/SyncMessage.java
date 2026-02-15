package com.ecommerce.sellerx.queue;

import com.ecommerce.sellerx.sync.SyncTask;
import com.ecommerce.sellerx.sync.SyncTaskType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Message DTO for sync task queue operations.
 * Sent to RabbitMQ queues for async processing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The sync task ID (references sync_tasks table)
     */
    private UUID taskId;

    /**
     * The store ID to sync
     */
    private UUID storeId;

    /**
     * Type of sync operation
     */
    private SyncTaskType taskType;

    /**
     * When this message was created
     */
    private LocalDateTime createdAt;

    /**
     * Number of retry attempts (incremented on each failure)
     */
    private int retryCount;

    /**
     * Correlation ID for tracking message through the system
     */
    private String correlationId;

    /**
     * Create a SyncMessage from a SyncTask entity.
     *
     * @param task The sync task entity
     * @return A new SyncMessage
     */
    public static SyncMessage from(SyncTask task) {
        return SyncMessage.builder()
                .taskId(task.getId())
                .storeId(task.getStoreId())
                .taskType(task.getTaskType())
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .correlationId(UUID.randomUUID().toString())
                .build();
    }

    /**
     * Create a copy with incremented retry count.
     *
     * @return A new SyncMessage with incremented retryCount
     */
    public SyncMessage withIncrementedRetry() {
        return SyncMessage.builder()
                .taskId(this.taskId)
                .storeId(this.storeId)
                .taskType(this.taskType)
                .createdAt(this.createdAt)
                .retryCount(this.retryCount + 1)
                .correlationId(this.correlationId)
                .build();
    }
}
