package com.ecommerce.sellerx.queue;

import com.ecommerce.sellerx.sync.AsyncOrderSyncService;
import com.ecommerce.sellerx.sync.AsyncProductSyncService;
import com.ecommerce.sellerx.sync.SyncTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumer service for processing sync tasks from RabbitMQ queues.
 * Messages that fail will be sent to the Dead Letter Queue (DLQ).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SyncQueueConsumer {

    private final AsyncProductSyncService productSyncService;
    private final AsyncOrderSyncService orderSyncService;
    private final SyncTaskService syncTaskService;

    private static final int MAX_RETRY_COUNT = 3;

    /**
     * Process order sync messages from the orders queue.
     * Concurrency: 3-5 concurrent consumers.
     */
    @RabbitListener(queues = "${sellerx.queue.sync.orders-queue:sync.orders}", concurrency = "3-5")
    public void processOrderSync(SyncMessage message) {
        log.info("Processing order sync: taskId={}, storeId={}, retryCount={}",
                message.getTaskId(), message.getStoreId(), message.getRetryCount());

        try {
            // Execute the sync directly (not async since we're already in a worker thread)
            orderSyncService.executeOrderSync(message.getTaskId(), message.getStoreId());
            log.info("Order sync completed: taskId={}", message.getTaskId());
        } catch (Exception e) {
            handleFailure(message, "Order sync", e);
        }
    }

    /**
     * Process product sync messages from the products queue.
     * Concurrency: 3-5 concurrent consumers.
     */
    @RabbitListener(queues = "${sellerx.queue.sync.products-queue:sync.products}", concurrency = "3-5")
    public void processProductSync(SyncMessage message) {
        log.info("Processing product sync: taskId={}, storeId={}, retryCount={}",
                message.getTaskId(), message.getStoreId(), message.getRetryCount());

        try {
            // Execute the sync directly (not async since we're already in a worker thread)
            productSyncService.executeProductSync(message.getTaskId(), message.getStoreId());
            log.info("Product sync completed: taskId={}", message.getTaskId());
        } catch (Exception e) {
            handleFailure(message, "Product sync", e);
        }
    }

    /**
     * Process financial sync messages from the financial queue.
     * Concurrency: 2-3 concurrent consumers (financial syncs are heavier).
     */
    @RabbitListener(queues = "${sellerx.queue.sync.financial-queue:sync.financial}", concurrency = "2-3")
    public void processFinancialSync(SyncMessage message) {
        log.info("Processing financial sync: taskId={}, storeId={}, retryCount={}",
                message.getTaskId(), message.getStoreId(), message.getRetryCount());

        try {
            // Financial sync is currently not implemented as async service
            // Mark task as completed for now, will be enhanced later
            log.warn("Financial sync not yet implemented for queue processing. taskId={}",
                    message.getTaskId());
            syncTaskService.completeTask(message.getTaskId(), 0, 0, 0, 0);
        } catch (Exception e) {
            handleFailure(message, "Financial sync", e);
        }
    }

    /**
     * Process messages from the Dead Letter Queue.
     * These are messages that have exceeded retry limits.
     */
    @RabbitListener(queues = "${sellerx.queue.sync.dlq:sync.dlq}")
    public void processDLQ(SyncMessage message) {
        log.error("DLQ message received: taskId={}, storeId={}, type={}, retryCount={}",
                message.getTaskId(), message.getStoreId(), message.getTaskType(), message.getRetryCount());

        try {
            // Mark the task as failed in the database
            syncTaskService.failTask(message.getTaskId(),
                    "Max retries exceeded. Message moved to Dead Letter Queue after " +
                            message.getRetryCount() + " attempts.");
        } catch (Exception e) {
            log.error("Failed to update task status for DLQ message: taskId={}, error={}",
                    message.getTaskId(), e.getMessage());
        }
    }

    /**
     * Handle sync failure with retry logic.
     * If max retries exceeded, message goes to DLQ.
     */
    private void handleFailure(SyncMessage message, String operation, Exception e) {
        log.error("{} failed: taskId={}, retryCount={}, error={}",
                operation, message.getTaskId(), message.getRetryCount(), e.getMessage());

        if (message.getRetryCount() >= MAX_RETRY_COUNT) {
            log.error("Max retries exceeded for taskId={}. Sending to DLQ.", message.getTaskId());
            // Reject and don't requeue - message will go to DLQ
            throw new AmqpRejectAndDontRequeueException(
                    "Max retries exceeded for " + operation + ": " + e.getMessage(), e);
        }

        // Retry will happen automatically via Spring AMQP retry configuration
        throw new RuntimeException(operation + " failed, will retry: " + e.getMessage(), e);
    }
}
