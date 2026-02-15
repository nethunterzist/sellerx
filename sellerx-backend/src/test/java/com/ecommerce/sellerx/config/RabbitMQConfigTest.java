package com.ecommerce.sellerx.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RabbitMQConfig")
class RabbitMQConfigTest {

    private RabbitMQConfig config;

    @BeforeEach
    void setUp() {
        config = new RabbitMQConfig();
        ReflectionTestUtils.setField(config, "syncExchangeName", "sellerx.sync");
        ReflectionTestUtils.setField(config, "ordersQueueName", "sync.orders");
        ReflectionTestUtils.setField(config, "productsQueueName", "sync.products");
        ReflectionTestUtils.setField(config, "financialQueueName", "sync.financial");
        ReflectionTestUtils.setField(config, "dlqName", "sync.dlq");
    }

    @Nested
    @DisplayName("Exchanges")
    class Exchanges {

        @Test
        @DisplayName("should create sync exchange")
        void shouldCreateSyncExchange() {
            DirectExchange exchange = config.syncExchange();

            assertThat(exchange.getName()).isEqualTo("sellerx.sync");
            assertThat(exchange.isDurable()).isTrue();
        }

        @Test
        @DisplayName("should create dead letter exchange")
        void shouldCreateDeadLetterExchange() {
            DirectExchange exchange = config.syncDeadLetterExchange();

            assertThat(exchange.getName()).isEqualTo("sellerx.sync.dlx");
            assertThat(exchange.isDurable()).isTrue();
        }
    }

    @Nested
    @DisplayName("Queues")
    class Queues {

        @Test
        @DisplayName("should create orders queue with DLQ settings")
        void shouldCreateOrdersQueue() {
            Queue queue = config.ordersQueue();

            assertThat(queue.getName()).isEqualTo("sync.orders");
            assertThat(queue.isDurable()).isTrue();
            assertThat(queue.getArguments()).containsEntry("x-dead-letter-exchange", "sellerx.sync.dlx");
            assertThat(queue.getArguments()).containsEntry("x-dead-letter-routing-key", "sync.failed");
        }

        @Test
        @DisplayName("should create products queue with DLQ settings")
        void shouldCreateProductsQueue() {
            Queue queue = config.productsQueue();

            assertThat(queue.getName()).isEqualTo("sync.products");
            assertThat(queue.isDurable()).isTrue();
            assertThat(queue.getArguments()).containsEntry("x-dead-letter-exchange", "sellerx.sync.dlx");
        }

        @Test
        @DisplayName("should create financial queue with DLQ settings")
        void shouldCreateFinancialQueue() {
            Queue queue = config.financialQueue();

            assertThat(queue.getName()).isEqualTo("sync.financial");
            assertThat(queue.isDurable()).isTrue();
            assertThat(queue.getArguments()).containsEntry("x-dead-letter-exchange", "sellerx.sync.dlx");
        }

        @Test
        @DisplayName("should create dead letter queue")
        void shouldCreateDeadLetterQueue() {
            Queue queue = config.syncDeadLetterQueue();

            assertThat(queue.getName()).isEqualTo("sync.dlq");
            assertThat(queue.isDurable()).isTrue();
        }
    }

    @Nested
    @DisplayName("Bindings")
    class Bindings {

        @Test
        @DisplayName("should create orders binding")
        void shouldCreateOrdersBinding() {
            Queue queue = config.ordersQueue();
            DirectExchange exchange = config.syncExchange();

            Binding binding = config.ordersBinding(queue, exchange);

            assertThat(binding.getExchange()).isEqualTo("sellerx.sync");
            assertThat(binding.getDestination()).isEqualTo("sync.orders");
            assertThat(binding.getRoutingKey()).isEqualTo("sync.orders");
        }

        @Test
        @DisplayName("should create products binding")
        void shouldCreateProductsBinding() {
            Queue queue = config.productsQueue();
            DirectExchange exchange = config.syncExchange();

            Binding binding = config.productsBinding(queue, exchange);

            assertThat(binding.getExchange()).isEqualTo("sellerx.sync");
            assertThat(binding.getDestination()).isEqualTo("sync.products");
            assertThat(binding.getRoutingKey()).isEqualTo("sync.products");
        }

        @Test
        @DisplayName("should create dead letter binding")
        void shouldCreateDeadLetterBinding() {
            Queue queue = config.syncDeadLetterQueue();
            DirectExchange exchange = config.syncDeadLetterExchange();

            Binding binding = config.deadLetterBinding(queue, exchange);

            assertThat(binding.getExchange()).isEqualTo("sellerx.sync.dlx");
            assertThat(binding.getDestination()).isEqualTo("sync.dlq");
            assertThat(binding.getRoutingKey()).isEqualTo("sync.failed");
        }
    }

    @Nested
    @DisplayName("MessageConverter")
    class MessageConverterTests {

        @Test
        @DisplayName("should create JSON message converter")
        void shouldCreateJsonMessageConverter() {
            var converter = config.jsonMessageConverter();

            assertThat(converter).isInstanceOf(Jackson2JsonMessageConverter.class);
        }
    }
}
