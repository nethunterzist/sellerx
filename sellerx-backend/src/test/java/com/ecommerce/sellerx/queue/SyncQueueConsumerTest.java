package com.ecommerce.sellerx.queue;

import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.sync.AsyncOrderSyncService;
import com.ecommerce.sellerx.sync.AsyncProductSyncService;
import com.ecommerce.sellerx.sync.SyncTaskService;
import com.ecommerce.sellerx.sync.SyncTaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("SyncQueueConsumer")
class SyncQueueConsumerTest extends BaseUnitTest {

    @Mock
    private AsyncProductSyncService productSyncService;

    @Mock
    private AsyncOrderSyncService orderSyncService;

    @Mock
    private SyncTaskService syncTaskService;

    private SyncQueueConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new SyncQueueConsumer(productSyncService, orderSyncService, syncTaskService);
    }

    @Nested
    @DisplayName("processOrderSync()")
    class ProcessOrderSync {

        @Test
        @DisplayName("should call order sync service")
        void shouldCallOrderSyncService() {
            // Given
            UUID taskId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();
            SyncMessage message = createMessage(taskId, storeId, SyncTaskType.ORDERS);

            // When
            consumer.processOrderSync(message);

            // Then
            verify(orderSyncService).executeOrderSync(taskId, storeId);
        }

        @Test
        @DisplayName("should throw RuntimeException on failure for retry")
        void shouldThrowRuntimeExceptionForRetry() {
            // Given
            SyncMessage message = createMessage(UUID.randomUUID(), UUID.randomUUID(), SyncTaskType.ORDERS);
            message.setRetryCount(0); // First attempt
            doThrow(new RuntimeException("API error")).when(orderSyncService)
                    .executeOrderSync(any(), any());

            // When/Then
            assertThatThrownBy(() -> consumer.processOrderSync(message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Order sync failed");
        }

        @Test
        @DisplayName("should throw AmqpRejectAndDontRequeueException when max retries exceeded")
        void shouldRejectWhenMaxRetriesExceeded() {
            // Given
            SyncMessage message = createMessage(UUID.randomUUID(), UUID.randomUUID(), SyncTaskType.ORDERS);
            message.setRetryCount(3); // Max retries reached
            doThrow(new RuntimeException("API error")).when(orderSyncService)
                    .executeOrderSync(any(), any());

            // When/Then
            assertThatThrownBy(() -> consumer.processOrderSync(message))
                    .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        }
    }

    @Nested
    @DisplayName("processProductSync()")
    class ProcessProductSync {

        @Test
        @DisplayName("should call product sync service")
        void shouldCallProductSyncService() {
            // Given
            UUID taskId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();
            SyncMessage message = createMessage(taskId, storeId, SyncTaskType.PRODUCTS);

            // When
            consumer.processProductSync(message);

            // Then
            verify(productSyncService).executeProductSync(taskId, storeId);
        }
    }

    @Nested
    @DisplayName("processFinancialSync()")
    class ProcessFinancialSync {

        @Test
        @DisplayName("should complete task (placeholder implementation)")
        void shouldCompleteTask() {
            // Given
            UUID taskId = UUID.randomUUID();
            SyncMessage message = createMessage(taskId, UUID.randomUUID(), SyncTaskType.FINANCIAL);

            // When
            consumer.processFinancialSync(message);

            // Then
            verify(syncTaskService).completeTask(taskId, 0, 0, 0, 0);
        }
    }

    @Nested
    @DisplayName("processDLQ()")
    class ProcessDLQ {

        @Test
        @DisplayName("should mark task as failed")
        void shouldMarkTaskAsFailed() {
            // Given
            UUID taskId = UUID.randomUUID();
            SyncMessage message = createMessage(taskId, UUID.randomUUID(), SyncTaskType.PRODUCTS);
            message.setRetryCount(5);

            // When
            consumer.processDLQ(message);

            // Then
            verify(syncTaskService).failTask(eq(taskId), contains("Max retries exceeded"));
        }
    }

    private SyncMessage createMessage(UUID taskId, UUID storeId, SyncTaskType type) {
        return SyncMessage.builder()
                .taskId(taskId)
                .storeId(storeId)
                .taskType(type)
                .retryCount(0)
                .correlationId(UUID.randomUUID().toString())
                .build();
    }
}
