package com.ecommerce.sellerx.sync;

import com.ecommerce.sellerx.common.exception.ResourceNotFoundException;
import com.ecommerce.sellerx.queue.SyncQueueProducer;
import com.ecommerce.sellerx.sync.dto.StartSyncResponse;
import com.ecommerce.sellerx.sync.dto.SyncTaskResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing sync task lifecycle.
 * Tracks async sync operations and provides status updates.
 * Supports both direct @Async execution and queue-based execution via RabbitMQ.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SyncTaskService {

    private final SyncTaskRepository syncTaskRepository;
    private final Optional<SyncQueueProducer> syncQueueProducer;

    @Value("${sellerx.sync.queue-enabled:false}")
    private boolean queueEnabled;

    /**
     * Create a new sync task in PENDING status.
     *
     * @return The created task
     */
    @Transactional
    public SyncTask createTask(UUID storeId, SyncTaskType taskType) {
        // Check if there's already an active task of this type
        Optional<SyncTask> existingTask = syncTaskRepository.findActiveTaskByStoreIdAndType(storeId, taskType);
        if (existingTask.isPresent()) {
            log.warn("Active {} task already exists for store {}: {}",
                    taskType, storeId, existingTask.get().getId());
            return existingTask.get();
        }

        SyncTask task = SyncTask.builder()
                .storeId(storeId)
                .taskType(taskType)
                .status(SyncTaskStatus.PENDING)
                .progressPercentage(0)
                .itemsProcessed(0)
                .itemsNew(0)
                .itemsUpdated(0)
                .itemsSkipped(0)
                .itemsFailed(0)
                .build();

        return syncTaskRepository.save(task);
    }

    /**
     * Create a sync task and queue it for processing via RabbitMQ.
     * This method should be used when queue-based sync is enabled.
     *
     * @return The created and queued task
     */
    @Transactional
    public SyncTask createAndQueueTask(UUID storeId, SyncTaskType taskType) {
        // Create the task first
        SyncTask task = createTask(storeId, taskType);

        // Queue the task if queue is enabled and producer is available
        if (queueEnabled && syncQueueProducer.isPresent()) {
            log.info("Queueing sync task {} for store {} via RabbitMQ", task.getId(), storeId);
            syncQueueProducer.get().enqueueSyncTask(task);
        } else {
            log.debug("Queue disabled or producer not available, task {} created but not queued", task.getId());
        }

        return task;
    }

    /**
     * Check if queue-based sync is enabled.
     */
    public boolean isQueueEnabled() {
        return queueEnabled && syncQueueProducer.isPresent();
    }

    /**
     * Mark task as started.
     */
    @Transactional
    public void startTask(UUID taskId) {
        SyncTask task = findTaskById(taskId);
        task.setStatus(SyncTaskStatus.RUNNING);
        task.setStartedAt(LocalDateTime.now());
        syncTaskRepository.save(task);
        log.info("Sync task {} started for store {}", taskId, task.getStoreId());
    }

    /**
     * Update task progress.
     */
    @Transactional
    public void updateProgress(UUID taskId, int currentPage, int totalPages, int itemsProcessed,
                               int itemsNew, int itemsUpdated, int itemsSkipped) {
        SyncTask task = findTaskById(taskId);

        int progressPercentage = totalPages > 0
                ? Math.min(99, (currentPage * 100) / totalPages)
                : 0;

        task.setProgressPercentage(progressPercentage);
        task.setCurrentPage(currentPage);
        task.setTotalPages(totalPages);
        task.setItemsProcessed(itemsProcessed);
        task.setItemsNew(itemsNew);
        task.setItemsUpdated(itemsUpdated);
        task.setItemsSkipped(itemsSkipped);
        syncTaskRepository.save(task);

        log.debug("Task {} progress: page {}/{}, {}% complete, {} items",
                taskId, currentPage, totalPages, progressPercentage, itemsProcessed);
    }

    /**
     * Mark task as completed successfully.
     */
    @Transactional
    public void completeTask(UUID taskId, int itemsProcessed, int itemsNew, int itemsUpdated, int itemsSkipped) {
        SyncTask task = findTaskById(taskId);
        task.setStatus(SyncTaskStatus.COMPLETED);
        task.setProgressPercentage(100);
        task.setItemsProcessed(itemsProcessed);
        task.setItemsNew(itemsNew);
        task.setItemsUpdated(itemsUpdated);
        task.setItemsSkipped(itemsSkipped);
        task.setCompletedAt(LocalDateTime.now());
        syncTaskRepository.save(task);
        log.info("Sync task {} completed: {} processed, {} new, {} updated, {} skipped",
                taskId, itemsProcessed, itemsNew, itemsUpdated, itemsSkipped);
    }

    /**
     * Mark task as failed.
     */
    @Transactional
    public void failTask(UUID taskId, String errorMessage) {
        SyncTask task = findTaskById(taskId);
        task.setStatus(SyncTaskStatus.FAILED);
        task.setErrorMessage(errorMessage != null && errorMessage.length() > 1000
                ? errorMessage.substring(0, 1000)
                : errorMessage);
        task.setCompletedAt(LocalDateTime.now());
        syncTaskRepository.save(task);
        log.error("Sync task {} failed: {}", taskId, errorMessage);
    }

    /**
     * Get task status for user.
     */
    public SyncTaskResponse getTaskStatus(UUID taskId, UUID storeId) {
        SyncTask task = syncTaskRepository.findByIdAndStoreId(taskId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("SyncTask", taskId.toString()));
        return toResponse(task);
    }

    /**
     * Get active tasks for a store.
     */
    public List<SyncTaskResponse> getActiveTasks(UUID storeId) {
        return syncTaskRepository.findActiveTasksByStoreId(storeId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Check if there's an active sync task of a specific type for a store.
     */
    public boolean hasActiveTask(UUID storeId, SyncTaskType taskType) {
        return syncTaskRepository.findActiveTaskByStoreIdAndType(storeId, taskType).isPresent();
    }

    /**
     * Create a StartSyncResponse from a task.
     */
    public StartSyncResponse toStartResponse(SyncTask task) {
        return StartSyncResponse.builder()
                .taskId(task.getId())
                .storeId(task.getStoreId())
                .taskType(task.getTaskType())
                .status(task.getStatus())
                .message("Sync operation started. Poll the status URL to check progress.")
                .statusUrl("/products/sync/" + task.getStoreId() + "/status/" + task.getId())
                .build();
    }

    private SyncTask findTaskById(UUID taskId) {
        return syncTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("SyncTask", taskId.toString()));
    }

    private SyncTaskResponse toResponse(SyncTask task) {
        String message = switch (task.getStatus()) {
            case PENDING -> "Sync operation is queued and waiting to start.";
            case RUNNING -> String.format("Syncing... Page %d/%d (%d%%)",
                    task.getCurrentPage() != null ? task.getCurrentPage() : 0,
                    task.getTotalPages() != null ? task.getTotalPages() : 0,
                    task.getProgressPercentage() != null ? task.getProgressPercentage() : 0);
            case COMPLETED -> String.format("Sync completed. %d items processed: %d new, %d updated, %d skipped.",
                    task.getItemsProcessed() != null ? task.getItemsProcessed() : 0,
                    task.getItemsNew() != null ? task.getItemsNew() : 0,
                    task.getItemsUpdated() != null ? task.getItemsUpdated() : 0,
                    task.getItemsSkipped() != null ? task.getItemsSkipped() : 0);
            case FAILED -> "Sync failed: " + (task.getErrorMessage() != null ? task.getErrorMessage() : "Unknown error");
            case CANCELLED -> "Sync operation was cancelled.";
        };

        return SyncTaskResponse.builder()
                .taskId(task.getId())
                .storeId(task.getStoreId())
                .taskType(task.getTaskType())
                .status(task.getStatus())
                .progressPercentage(task.getProgressPercentage())
                .currentPage(task.getCurrentPage())
                .totalPages(task.getTotalPages())
                .itemsProcessed(task.getItemsProcessed())
                .itemsNew(task.getItemsNew())
                .itemsUpdated(task.getItemsUpdated())
                .itemsSkipped(task.getItemsSkipped())
                .itemsFailed(task.getItemsFailed())
                .errorMessage(task.getErrorMessage())
                .message(message)
                .createdAt(task.getCreatedAt())
                .startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt())
                .build();
    }
}
