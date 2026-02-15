package com.ecommerce.sellerx.queue;

import com.ecommerce.sellerx.sync.SyncTask;
import com.ecommerce.sellerx.sync.SyncTaskType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Producer service for sending sync tasks to RabbitMQ queues.
 * Messages are routed based on task type to the appropriate queue.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SyncQueueProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${sellerx.queue.sync.exchange:sellerx.sync}")
    private String syncExchange;

    @Value("${sellerx.queue.sync.orders-queue:sync.orders}")
    private String ordersRoutingKey;

    @Value("${sellerx.queue.sync.products-queue:sync.products}")
    private String productsRoutingKey;

    @Value("${sellerx.queue.sync.financial-queue:sync.financial}")
    private String financialRoutingKey;

    /**
     * Enqueue a sync task for processing.
     *
     * @param task The sync task to enqueue
     */
    public void enqueueSyncTask(SyncTask task) {
        SyncMessage message = SyncMessage.from(task);
        String routingKey = getRoutingKey(task.getTaskType());

        log.info("Enqueueing sync task: taskId={}, storeId={}, type={}, routingKey={}",
                task.getId(), task.getStoreId(), task.getTaskType(), routingKey);

        MessagePostProcessor postProcessor = m -> {
            m.getMessageProperties().setCorrelationId(message.getCorrelationId());
            m.getMessageProperties().setContentType("application/json");
            return m;
        };

        rabbitTemplate.convertAndSend(syncExchange, routingKey, message, postProcessor);

        log.info("Sync task {} enqueued successfully to {}", task.getId(), routingKey);
    }

    /**
     * Enqueue a sync message directly (for retries).
     *
     * @param message The sync message to enqueue
     */
    public void enqueueMessage(SyncMessage message) {
        String routingKey = getRoutingKey(message.getTaskType());

        log.info("Enqueueing sync message: taskId={}, storeId={}, type={}, retryCount={}",
                message.getTaskId(), message.getStoreId(), message.getTaskType(), message.getRetryCount());

        MessagePostProcessor postProcessor = m -> {
            m.getMessageProperties().setCorrelationId(message.getCorrelationId());
            m.getMessageProperties().setContentType("application/json");
            return m;
        };

        rabbitTemplate.convertAndSend(syncExchange, routingKey, message, postProcessor);
    }

    /**
     * Get the routing key for a task type.
     *
     * @param taskType The sync task type
     * @return The routing key for the queue
     */
    private String getRoutingKey(SyncTaskType taskType) {
        return switch (taskType) {
            case ORDERS -> ordersRoutingKey;
            case PRODUCTS -> productsRoutingKey;
            case FINANCIAL -> financialRoutingKey;
            case RETURNS -> ordersRoutingKey; // Returns processed with orders
            case ALL -> productsRoutingKey;   // ALL starts with products
        };
    }
}
