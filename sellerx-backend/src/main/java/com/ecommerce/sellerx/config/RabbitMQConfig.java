package com.ecommerce.sellerx.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration for async sync operations.
 *
 * Queue topology:
 * - sellerx.sync (Direct Exchange)
 *   ├── sync.orders    (routing key: sync.orders)
 *   ├── sync.products  (routing key: sync.products)
 *   ├── sync.financial (routing key: sync.financial)
 *   └── sync.dlq       (Dead Letter Queue)
 *
 * Note: Only enabled when spring.rabbitmq.enabled=true
 */
@Configuration
@ConditionalOnProperty(name = "spring.rabbitmq.enabled", havingValue = "true", matchIfMissing = false)
public class RabbitMQConfig {

    @Value("${sellerx.queue.sync.exchange:sellerx.sync}")
    private String syncExchangeName;

    @Value("${sellerx.queue.sync.orders-queue:sync.orders}")
    private String ordersQueueName;

    @Value("${sellerx.queue.sync.products-queue:sync.products}")
    private String productsQueueName;

    @Value("${sellerx.queue.sync.financial-queue:sync.financial}")
    private String financialQueueName;

    @Value("${sellerx.queue.sync.dlq:sync.dlq}")
    private String dlqName;

    // ==================== Exchanges ====================

    @Bean
    public DirectExchange syncExchange() {
        return new DirectExchange(syncExchangeName);
    }

    @Bean
    public DirectExchange syncDeadLetterExchange() {
        return new DirectExchange(syncExchangeName + ".dlx");
    }

    // ==================== Queues ====================

    @Bean
    public Queue ordersQueue() {
        return QueueBuilder.durable(ordersQueueName)
                .withArgument("x-dead-letter-exchange", syncExchangeName + ".dlx")
                .withArgument("x-dead-letter-routing-key", "sync.failed")
                .build();
    }

    @Bean
    public Queue productsQueue() {
        return QueueBuilder.durable(productsQueueName)
                .withArgument("x-dead-letter-exchange", syncExchangeName + ".dlx")
                .withArgument("x-dead-letter-routing-key", "sync.failed")
                .build();
    }

    @Bean
    public Queue financialQueue() {
        return QueueBuilder.durable(financialQueueName)
                .withArgument("x-dead-letter-exchange", syncExchangeName + ".dlx")
                .withArgument("x-dead-letter-routing-key", "sync.failed")
                .build();
    }

    @Bean
    public Queue syncDeadLetterQueue() {
        return QueueBuilder.durable(dlqName).build();
    }

    // ==================== Bindings ====================

    @Bean
    public Binding ordersBinding(Queue ordersQueue, DirectExchange syncExchange) {
        return BindingBuilder.bind(ordersQueue)
                .to(syncExchange)
                .with(ordersQueueName);
    }

    @Bean
    public Binding productsBinding(Queue productsQueue, DirectExchange syncExchange) {
        return BindingBuilder.bind(productsQueue)
                .to(syncExchange)
                .with(productsQueueName);
    }

    @Bean
    public Binding financialBinding(Queue financialQueue, DirectExchange syncExchange) {
        return BindingBuilder.bind(financialQueue)
                .to(syncExchange)
                .with(financialQueueName);
    }

    @Bean
    public Binding deadLetterBinding(Queue syncDeadLetterQueue, DirectExchange syncDeadLetterExchange) {
        return BindingBuilder.bind(syncDeadLetterQueue)
                .to(syncDeadLetterExchange)
                .with("sync.failed");
    }

    // ==================== Message Converter ====================

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
