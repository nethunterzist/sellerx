package com.ecommerce.sellerx.queue;

import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.sync.SyncTask;
import com.ecommerce.sellerx.sync.SyncTaskStatus;
import com.ecommerce.sellerx.sync.SyncTaskType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SyncMessage")
class SyncMessageTest extends BaseUnitTest {

    @Nested
    @DisplayName("from()")
    class From {

        @Test
        @DisplayName("should create message from SyncTask")
        void shouldCreateMessageFromSyncTask() {
            // Given
            UUID taskId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();
            SyncTask task = SyncTask.builder()
                    .id(taskId)
                    .storeId(storeId)
                    .taskType(SyncTaskType.PRODUCTS)
                    .status(SyncTaskStatus.PENDING)
                    .build();

            // When
            SyncMessage message = SyncMessage.from(task);

            // Then
            assertThat(message.getTaskId()).isEqualTo(taskId);
            assertThat(message.getStoreId()).isEqualTo(storeId);
            assertThat(message.getTaskType()).isEqualTo(SyncTaskType.PRODUCTS);
            assertThat(message.getRetryCount()).isZero();
            assertThat(message.getCorrelationId()).isNotBlank();
            assertThat(message.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should generate unique correlation ID")
        void shouldGenerateUniqueCorrelationId() {
            // Given
            SyncTask task = SyncTask.builder()
                    .id(UUID.randomUUID())
                    .storeId(UUID.randomUUID())
                    .taskType(SyncTaskType.ORDERS)
                    .build();

            // When
            SyncMessage message1 = SyncMessage.from(task);
            SyncMessage message2 = SyncMessage.from(task);

            // Then
            assertThat(message1.getCorrelationId()).isNotEqualTo(message2.getCorrelationId());
        }
    }

    @Nested
    @DisplayName("withIncrementedRetry()")
    class WithIncrementedRetry {

        @Test
        @DisplayName("should increment retry count")
        void shouldIncrementRetryCount() {
            // Given
            SyncMessage original = SyncMessage.builder()
                    .taskId(UUID.randomUUID())
                    .storeId(UUID.randomUUID())
                    .taskType(SyncTaskType.PRODUCTS)
                    .retryCount(0)
                    .correlationId("test-correlation")
                    .build();

            // When
            SyncMessage retried = original.withIncrementedRetry();

            // Then
            assertThat(retried.getRetryCount()).isEqualTo(1);
            assertThat(retried.getTaskId()).isEqualTo(original.getTaskId());
            assertThat(retried.getStoreId()).isEqualTo(original.getStoreId());
            assertThat(retried.getCorrelationId()).isEqualTo(original.getCorrelationId());
        }

        @Test
        @DisplayName("should preserve other fields")
        void shouldPreserveOtherFields() {
            // Given
            UUID taskId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();
            SyncMessage original = SyncMessage.builder()
                    .taskId(taskId)
                    .storeId(storeId)
                    .taskType(SyncTaskType.FINANCIAL)
                    .retryCount(2)
                    .correlationId("corr-123")
                    .build();

            // When
            SyncMessage retried = original.withIncrementedRetry();

            // Then
            assertThat(retried.getTaskId()).isEqualTo(taskId);
            assertThat(retried.getStoreId()).isEqualTo(storeId);
            assertThat(retried.getTaskType()).isEqualTo(SyncTaskType.FINANCIAL);
            assertThat(retried.getRetryCount()).isEqualTo(3);
        }
    }
}
