package com.ecommerce.sellerx.queue;

import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.sync.SyncTask;
import com.ecommerce.sellerx.sync.SyncTaskStatus;
import com.ecommerce.sellerx.sync.SyncTaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@DisplayName("SyncQueueProducer")
class SyncQueueProducerTest extends BaseUnitTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private SyncQueueProducer producer;

    @BeforeEach
    void setUp() {
        producer = new SyncQueueProducer(rabbitTemplate);
        ReflectionTestUtils.setField(producer, "syncExchange", "sellerx.sync");
        ReflectionTestUtils.setField(producer, "ordersRoutingKey", "sync.orders");
        ReflectionTestUtils.setField(producer, "productsRoutingKey", "sync.products");
        ReflectionTestUtils.setField(producer, "financialRoutingKey", "sync.financial");
    }

    @Nested
    @DisplayName("enqueueSyncTask()")
    class EnqueueSyncTask {

        @Test
        @DisplayName("should send product sync to products queue")
        void shouldSendProductSyncToProductsQueue() {
            // Given
            SyncTask task = createTask(SyncTaskType.PRODUCTS);

            // When
            producer.enqueueSyncTask(task);

            // Then
            ArgumentCaptor<SyncMessage> messageCaptor = ArgumentCaptor.forClass(SyncMessage.class);
            verify(rabbitTemplate).convertAndSend(
                    eq("sellerx.sync"),
                    eq("sync.products"),
                    messageCaptor.capture(),
                    any(MessagePostProcessor.class)
            );

            SyncMessage sentMessage = messageCaptor.getValue();
            assertThat(sentMessage.getTaskId()).isEqualTo(task.getId());
            assertThat(sentMessage.getStoreId()).isEqualTo(task.getStoreId());
            assertThat(sentMessage.getTaskType()).isEqualTo(SyncTaskType.PRODUCTS);
        }

        @Test
        @DisplayName("should send order sync to orders queue")
        void shouldSendOrderSyncToOrdersQueue() {
            // Given
            SyncTask task = createTask(SyncTaskType.ORDERS);

            // When
            producer.enqueueSyncTask(task);

            // Then
            verify(rabbitTemplate).convertAndSend(
                    eq("sellerx.sync"),
                    eq("sync.orders"),
                    any(SyncMessage.class),
                    any(MessagePostProcessor.class)
            );
        }

        @Test
        @DisplayName("should send financial sync to financial queue")
        void shouldSendFinancialSyncToFinancialQueue() {
            // Given
            SyncTask task = createTask(SyncTaskType.FINANCIAL);

            // When
            producer.enqueueSyncTask(task);

            // Then
            verify(rabbitTemplate).convertAndSend(
                    eq("sellerx.sync"),
                    eq("sync.financial"),
                    any(SyncMessage.class),
                    any(MessagePostProcessor.class)
            );
        }

        @Test
        @DisplayName("should send ALL sync to products queue (starts with products)")
        void shouldSendAllSyncToProductsQueue() {
            // Given
            SyncTask task = createTask(SyncTaskType.ALL);

            // When
            producer.enqueueSyncTask(task);

            // Then
            verify(rabbitTemplate).convertAndSend(
                    eq("sellerx.sync"),
                    eq("sync.products"),
                    any(SyncMessage.class),
                    any(MessagePostProcessor.class)
            );
        }
    }

    @Nested
    @DisplayName("enqueueMessage()")
    class EnqueueMessage {

        @Test
        @DisplayName("should send message directly to appropriate queue")
        void shouldSendMessageDirectly() {
            // Given
            SyncMessage message = SyncMessage.builder()
                    .taskId(UUID.randomUUID())
                    .storeId(UUID.randomUUID())
                    .taskType(SyncTaskType.ORDERS)
                    .retryCount(1)
                    .correlationId("test-corr")
                    .build();

            // When
            producer.enqueueMessage(message);

            // Then
            verify(rabbitTemplate).convertAndSend(
                    eq("sellerx.sync"),
                    eq("sync.orders"),
                    eq(message),
                    any(MessagePostProcessor.class)
            );
        }
    }

    private SyncTask createTask(SyncTaskType type) {
        return SyncTask.builder()
                .id(UUID.randomUUID())
                .storeId(UUID.randomUUID())
                .taskType(type)
                .status(SyncTaskStatus.PENDING)
                .build();
    }
}
