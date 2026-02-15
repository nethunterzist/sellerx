# Faz 1: RabbitMQ Entegrasyonu

> **Durum**: ✅ Tamamlandı
> **Tarih**: Şubat 2026
> **Süre**: ~1 Saat

Bu faz, sync işlemleri için message queue altyapısının kurulmasını kapsar.

---

## 1.1 Docker Compose Güncellemesi ✅

### Yapılan Değişiklik

`docker-compose.dev.yml` dosyasına RabbitMQ service'i eklendi:

```yaml
# RabbitMQ Message Broker
rabbitmq:
  image: rabbitmq:3.13-management-alpine
  container_name: sellerx-rabbitmq
  restart: no
  ports:
    - "5672:5672"    # AMQP
    - "15672:15672"  # Management UI
  environment:
    RABBITMQ_DEFAULT_USER: guest
    RABBITMQ_DEFAULT_PASS: guest
  volumes:
    - rabbitmq_data:/var/lib/rabbitmq
  healthcheck:
    test: ["CMD", "rabbitmq-diagnostics", "-q", "ping"]
    interval: 30s
    timeout: 10s
    retries: 5
  networks:
    - sellerx-network

volumes:
  postgres_data:
  rabbitmq_data:  # YENİ
```

### Erişim Bilgileri

| Port | Kullanım |
|------|----------|
| 5672 | AMQP protokolü (uygulama bağlantısı) |
| 15672 | Management UI (admin paneli) |

Management UI: http://localhost:15672 (guest/guest)

---

## 1.2 Maven Dependency Ekleme ✅

### pom.xml Değişiklikleri

```xml
<!-- RabbitMQ / AMQP for message queuing -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>

<!-- RabbitMQ Test Support -->
<dependency>
    <groupId>org.springframework.amqp</groupId>
    <artifactId>spring-rabbit-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- TestContainers RabbitMQ -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>rabbitmq</artifactId>
    <version>${testcontainers.version}</version>
    <scope>test</scope>
</dependency>
```

---

## 1.3 Application YAML Konfigürasyonu ✅

### application.yaml Değişiklikleri

```yaml
# RabbitMQ Configuration
spring.rabbitmq:
  host: ${RABBITMQ_HOST:localhost}
  port: ${RABBITMQ_PORT:5672}
  username: ${RABBITMQ_USER:guest}
  password: ${RABBITMQ_PASSWORD:guest}
  connection-timeout: 10s
  listener:
    simple:
      concurrency: 3
      max-concurrency: 10
      prefetch: 1
      acknowledge-mode: auto
      retry:
        enabled: true
        initial-interval: 1000
        max-attempts: 3
        multiplier: 2.0

# Sync Queue Configuration
sellerx:
  queue:
    sync:
      exchange: sellerx.sync
      orders-queue: sync.orders
      products-queue: sync.products
      financial-queue: sync.financial
      dlq: sync.dlq
      routing-key-prefix: sync
  sync:
    queue-enabled: ${SYNC_QUEUE_ENABLED:false}  # Varsayılan kapalı
```

### Konfigürasyon Açıklamaları

| Ayar | Değer | Açıklama |
|------|-------|----------|
| `concurrency` | 3 | Minimum consumer sayısı |
| `max-concurrency` | 10 | Maksimum consumer sayısı |
| `prefetch` | 1 | Aynı anda işlenecek mesaj sayısı |
| `retry.max-attempts` | 3 | Maksimum retry denemesi |
| `queue-enabled` | false | Queue modunu aktifleştirme (opsiyonel) |

---

## 1.4 Queue Topolojisi ✅

### Exchange ve Queue Yapısı

```
sellerx.sync (Direct Exchange)
├── sync.orders    (routing key: sync.orders)
│   └── x-dead-letter-exchange: sellerx.sync.dlx
│   └── x-dead-letter-routing-key: sync.failed
├── sync.products  (routing key: sync.products)
│   └── x-dead-letter-exchange: sellerx.sync.dlx
│   └── x-dead-letter-routing-key: sync.failed
├── sync.financial (routing key: sync.financial)
│   └── x-dead-letter-exchange: sellerx.sync.dlx
│   └── x-dead-letter-routing-key: sync.failed
│
sellerx.sync.dlx (Dead Letter Exchange)
└── sync.dlq (Dead Letter Queue)
    └── routing key: sync.failed
```

### Mesaj Akışı

```
1. SyncTask oluşturulur (PENDING)
           │
2. SyncQueueProducer → RabbitMQ Queue
           │
3. SyncQueueConsumer mesajı alır
           │
4. Sync işlemi başlar (RUNNING)
           │
    ┌──────┴──────┐
    │             │
  Başarılı     Başarısız
    │             │
COMPLETED     Retry?
              ┌──┴──┐
              │     │
            Evet   Hayır (max retry)
              │     │
           Yeniden  DLQ'ya taşı
           dene     (FAILED)
```

---

## 1.5 Oluşturulan Dosyalar ✅

### Yeni Dosyalar

| Dosya | Paket | Açıklama |
|-------|-------|----------|
| `RabbitMQConfig.java` | config | Exchange, Queue, Binding tanımları |
| `SyncMessage.java` | queue | Mesaj DTO'su |
| `SyncQueueProducer.java` | queue | Mesaj gönderici |
| `SyncQueueConsumer.java` | queue | Mesaj tüketici (@RabbitListener) |

### Güncellenen Dosyalar

| Dosya | Değişiklik |
|-------|------------|
| `docker-compose.dev.yml` | RabbitMQ service eklendi |
| `pom.xml` | AMQP dependency eklendi |
| `application.yaml` | RabbitMQ ve queue config eklendi |
| `SyncTaskService.java` | Queue desteği eklendi |

---

## 1.6 RabbitMQConfig.java Detayları

```java
@Configuration
public class RabbitMQConfig {

    // Direct Exchange - routing key ile yönlendirme
    @Bean
    public DirectExchange syncExchange() {
        return new DirectExchange("sellerx.sync");
    }

    // Queue with DLQ support
    @Bean
    public Queue ordersQueue() {
        return QueueBuilder.durable("sync.orders")
            .withArgument("x-dead-letter-exchange", "sellerx.sync.dlx")
            .withArgument("x-dead-letter-routing-key", "sync.failed")
            .build();
    }

    // JSON Message Converter
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
```

---

## 1.7 SyncMessage DTO

```java
@Data
@Builder
public class SyncMessage implements Serializable {
    private UUID taskId;
    private UUID storeId;
    private SyncTaskType taskType;
    private LocalDateTime createdAt;
    private int retryCount;
    private String correlationId;

    public static SyncMessage from(SyncTask task) { ... }
    public SyncMessage withIncrementedRetry() { ... }
}
```

---

## 1.8 Consumer Concurrency

| Queue | Concurrency | Açıklama |
|-------|-------------|----------|
| sync.orders | 3-5 | Order sync için |
| sync.products | 3-5 | Product sync için |
| sync.financial | 2-3 | Financial sync (daha ağır işlem) |
| sync.dlq | 1 | DLQ işleme |

---

## 1.9 Test Coverage ✅

### Yeni Test Dosyaları

| Test Dosyası | Test Sayısı | Açıklama |
|--------------|-------------|----------|
| `SyncMessageTest.java` | 4 | DTO testleri |
| `SyncQueueProducerTest.java` | 5 | Producer testleri |
| `SyncQueueConsumerTest.java` | 6 | Consumer testleri |
| `RabbitMQConfigTest.java` | 10 | Config testleri |

### Test Sonuçları

```bash
Tests run: 748, Failures: 0, Errors: 0, Skipped: 9
BUILD SUCCESS
```

---

## Kullanım

### Queue Modunu Aktifleştirme

```bash
# Environment variable ile
export SYNC_QUEUE_ENABLED=true

# veya application.yaml'da
sellerx:
  sync:
    queue-enabled: true
```

### RabbitMQ Başlatma

```bash
# Docker ile
docker-compose -f docker-compose.dev.yml up rabbitmq -d

# Management UI
open http://localhost:15672  # guest/guest
```

### Queue Durumu Kontrolü

```bash
# Queue listesi
docker exec sellerx-rabbitmq rabbitmqctl list_queues name messages

# Beklenen output:
# sync.orders    0
# sync.products  0
# sync.financial 0
# sync.dlq       0
```

---

## Sonraki Adımlar

Faz 1 tamamlandı. Sıradaki fazlar:

1. **Faz 2**: Resilience4j entegrasyonu - Circuit breaker, retry
2. **Faz 3**: WebSocket entegrasyonu - Real-time alerts
3. **Faz 4**: Database optimizasyonları - Partitioning, retention

Detaylar için: [Scaling Plan README](./README.md)
