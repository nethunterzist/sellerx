package com.ecommerce.sellerx.stocktracking;

import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.common.TestDataBuilder;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("StockTrackingService")
class StockTrackingServiceTest extends BaseUnitTest {

    @Mock
    private StockTrackedProductRepository trackedProductRepository;

    @Mock
    private StockSnapshotRepository snapshotRepository;

    @Mock
    private StockAlertRepository alertRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private TrendyolPublicStockClient stockClient;

    private StockTrackingService service;

    private User testUser;
    private Store testStore;
    private UUID storeId;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetSequence();
        service = new StockTrackingService(
                trackedProductRepository, snapshotRepository,
                alertRepository, storeRepository, stockClient);

        testUser = TestDataBuilder.user().build();
        testUser.setId(1L);
        testStore = TestDataBuilder.completedStore(testUser).build();
        storeId = UUID.randomUUID();
        testStore.setId(storeId);
    }

    @Nested
    @DisplayName("addProductToTrack")
    class AddProductToTrack {

        @Test
        @DisplayName("should successfully add product to tracking")
        void shouldAddProduct() {
            String url = "https://www.trendyol.com/test-product-p-12345";
            AddTrackedProductRequest request = AddTrackedProductRequest.builder()
                    .productUrl(url)
                    .build();

            TrendyolPublicStockClient.StockData stockData = TrendyolPublicStockClient.StockData.builder()
                    .productId(12345L)
                    .quantity(50)
                    .inStock(true)
                    .price(new BigDecimal("199.99"))
                    .productName("Test Product")
                    .brandName("Test Brand")
                    .imageUrl("https://image.url/img.jpg")
                    .build();

            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(stockClient.isValidProductUrl(url)).thenReturn(true);
            when(stockClient.extractProductId(url)).thenReturn(12345L);
            when(trackedProductRepository.existsByStoreIdAndTrendyolProductId(storeId, 12345L)).thenReturn(false);
            when(trackedProductRepository.countByStoreIdAndIsActiveTrue(storeId)).thenReturn(0);
            when(stockClient.fetchStock(url)).thenReturn(stockData);
            when(trackedProductRepository.save(any(StockTrackedProduct.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(snapshotRepository.save(any(StockSnapshot.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            TrackedProductDto result = service.addProductToTrack(storeId, request);

            assertThat(result).isNotNull();
            assertThat(result.getProductName()).isEqualTo("Test Product");
            assertThat(result.getLastStockQuantity()).isEqualTo(50);
            verify(trackedProductRepository).save(any(StockTrackedProduct.class));
            verify(snapshotRepository).save(any(StockSnapshot.class));
        }

        @Test
        @DisplayName("should throw when store not found")
        void shouldThrowWhenStoreNotFound() {
            when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

            AddTrackedProductRequest request = AddTrackedProductRequest.builder()
                    .productUrl("https://www.trendyol.com/test-p-123").build();

            assertThatThrownBy(() -> service.addProductToTrack(storeId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Store not found");
        }

        @Test
        @DisplayName("should throw when product URL is invalid")
        void shouldThrowWhenInvalidUrl() {
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(stockClient.isValidProductUrl("https://invalid.com")).thenReturn(false);

            AddTrackedProductRequest request = AddTrackedProductRequest.builder()
                    .productUrl("https://invalid.com").build();

            assertThatThrownBy(() -> service.addProductToTrack(storeId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid Trendyol product URL");
        }

        @Test
        @DisplayName("should throw when product already tracked")
        void shouldThrowWhenAlreadyTracked() {
            String url = "https://www.trendyol.com/test-p-999";

            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(stockClient.isValidProductUrl(url)).thenReturn(true);
            when(stockClient.extractProductId(url)).thenReturn(999L);
            when(trackedProductRepository.existsByStoreIdAndTrendyolProductId(storeId, 999L)).thenReturn(true);

            AddTrackedProductRequest request = AddTrackedProductRequest.builder().productUrl(url).build();

            assertThatThrownBy(() -> service.addProductToTrack(storeId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already being tracked");
        }

        @Test
        @DisplayName("should throw when max tracked products limit reached")
        void shouldThrowWhenLimitReached() {
            String url = "https://www.trendyol.com/test-p-555";

            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(stockClient.isValidProductUrl(url)).thenReturn(true);
            when(stockClient.extractProductId(url)).thenReturn(555L);
            when(trackedProductRepository.existsByStoreIdAndTrendyolProductId(storeId, 555L)).thenReturn(false);
            when(trackedProductRepository.countByStoreIdAndIsActiveTrue(storeId)).thenReturn(10);

            AddTrackedProductRequest request = AddTrackedProductRequest.builder().productUrl(url).build();

            assertThatThrownBy(() -> service.addProductToTrack(storeId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Maximum 10 products");
        }

        @Test
        @DisplayName("should throw when stock fetch fails")
        void shouldThrowWhenStockFetchFails() {
            String url = "https://www.trendyol.com/test-p-777";

            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(stockClient.isValidProductUrl(url)).thenReturn(true);
            when(stockClient.extractProductId(url)).thenReturn(777L);
            when(trackedProductRepository.existsByStoreIdAndTrendyolProductId(storeId, 777L)).thenReturn(false);
            when(trackedProductRepository.countByStoreIdAndIsActiveTrue(storeId)).thenReturn(0);
            when(stockClient.fetchStock(url)).thenReturn(null);

            AddTrackedProductRequest request = AddTrackedProductRequest.builder().productUrl(url).build();

            assertThatThrownBy(() -> service.addProductToTrack(storeId, request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Could not fetch stock data");
        }
    }

    @Nested
    @DisplayName("removeProduct")
    class RemoveProduct {

        @Test
        @DisplayName("should remove tracked product")
        void shouldRemoveProduct() {
            UUID productId = UUID.randomUUID();
            StockTrackedProduct product = createTrackedProduct(productId, 50);

            when(trackedProductRepository.findByIdWithStore(productId))
                    .thenReturn(Optional.of(product));

            service.removeProduct(productId, storeId);

            verify(trackedProductRepository).delete(product);
        }

        @Test
        @DisplayName("should throw when product not found")
        void shouldThrowWhenNotFound() {
            UUID productId = UUID.randomUUID();
            when(trackedProductRepository.findByIdWithStore(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.removeProduct(productId, storeId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Tracked product not found");
        }

        @Test
        @DisplayName("should throw when product belongs to different store")
        void shouldThrowSecurityExceptionForWrongStore() {
            UUID productId = UUID.randomUUID();
            UUID otherStoreId = UUID.randomUUID();
            StockTrackedProduct product = createTrackedProduct(productId, 50);

            when(trackedProductRepository.findByIdWithStore(productId))
                    .thenReturn(Optional.of(product));

            assertThatThrownBy(() -> service.removeProduct(productId, otherStoreId))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("does not belong to this store");
        }
    }

    @Nested
    @DisplayName("processStockChange / checkAndCreateAlerts")
    class ProcessStockChange {

        @Test
        @DisplayName("should create OUT_OF_STOCK alert when stock becomes zero")
        void shouldCreateOutOfStockAlert() {
            UUID productId = UUID.randomUUID();
            StockTrackedProduct product = createTrackedProduct(productId, 10);
            product.setAlertOnOutOfStock(true);

            TrendyolPublicStockClient.StockData stockData = TrendyolPublicStockClient.StockData.builder()
                    .quantity(0).inStock(false).price(new BigDecimal("99.99")).build();

            when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(trackedProductRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.processStockChange(product, stockData);

            ArgumentCaptor<StockAlert> alertCaptor = ArgumentCaptor.forClass(StockAlert.class);
            verify(alertRepository).save(alertCaptor.capture());

            StockAlert alert = alertCaptor.getValue();
            assertThat(alert.getAlertType()).isEqualTo(StockAlertType.OUT_OF_STOCK);
            assertThat(alert.getSeverity()).isEqualTo(StockAlertSeverity.CRITICAL);
            assertThat(alert.getOldQuantity()).isEqualTo(10);
            assertThat(alert.getNewQuantity()).isEqualTo(0);
        }

        @Test
        @DisplayName("should create BACK_IN_STOCK alert when stock returns from zero")
        void shouldCreateBackInStockAlert() {
            UUID productId = UUID.randomUUID();
            StockTrackedProduct product = createTrackedProduct(productId, 0);
            product.setAlertOnBackInStock(true);

            TrendyolPublicStockClient.StockData stockData = TrendyolPublicStockClient.StockData.builder()
                    .quantity(25).inStock(true).price(new BigDecimal("99.99")).build();

            when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(trackedProductRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.processStockChange(product, stockData);

            ArgumentCaptor<StockAlert> alertCaptor = ArgumentCaptor.forClass(StockAlert.class);
            verify(alertRepository).save(alertCaptor.capture());

            StockAlert alert = alertCaptor.getValue();
            assertThat(alert.getAlertType()).isEqualTo(StockAlertType.BACK_IN_STOCK);
            assertThat(alert.getSeverity()).isEqualTo(StockAlertSeverity.MEDIUM);
        }

        @Test
        @DisplayName("should create LOW_STOCK alert when stock falls below threshold")
        void shouldCreateLowStockAlert() {
            UUID productId = UUID.randomUUID();
            StockTrackedProduct product = createTrackedProduct(productId, 15);
            product.setAlertOnLowStock(true);
            product.setLowStockThreshold(10);

            TrendyolPublicStockClient.StockData stockData = TrendyolPublicStockClient.StockData.builder()
                    .quantity(5).inStock(true).price(new BigDecimal("99.99")).build();

            when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(trackedProductRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.processStockChange(product, stockData);

            ArgumentCaptor<StockAlert> alertCaptor = ArgumentCaptor.forClass(StockAlert.class);
            verify(alertRepository).save(alertCaptor.capture());

            StockAlert alert = alertCaptor.getValue();
            assertThat(alert.getAlertType()).isEqualTo(StockAlertType.LOW_STOCK);
            assertThat(alert.getSeverity()).isEqualTo(StockAlertSeverity.HIGH);
        }

        @Test
        @DisplayName("should create STOCK_INCREASED alert when stock increases by more than 50%")
        void shouldCreateStockIncreasedAlert() {
            UUID productId = UUID.randomUUID();
            StockTrackedProduct product = createTrackedProduct(productId, 20);
            product.setAlertOnStockIncrease(true);

            // 20 * 1.5 = 30, so 31 should trigger
            TrendyolPublicStockClient.StockData stockData = TrendyolPublicStockClient.StockData.builder()
                    .quantity(31).inStock(true).price(new BigDecimal("99.99")).build();

            when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(trackedProductRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.processStockChange(product, stockData);

            ArgumentCaptor<StockAlert> alertCaptor = ArgumentCaptor.forClass(StockAlert.class);
            verify(alertRepository).save(alertCaptor.capture());

            StockAlert alert = alertCaptor.getValue();
            assertThat(alert.getAlertType()).isEqualTo(StockAlertType.STOCK_INCREASED);
            assertThat(alert.getSeverity()).isEqualTo(StockAlertSeverity.LOW);
        }

        @Test
        @DisplayName("should not create alert on first check (null old quantity)")
        void shouldNotCreateAlertOnFirstCheck() {
            UUID productId = UUID.randomUUID();
            StockTrackedProduct product = createTrackedProduct(productId, null);

            TrendyolPublicStockClient.StockData stockData = TrendyolPublicStockClient.StockData.builder()
                    .quantity(0).inStock(false).price(new BigDecimal("99.99")).build();

            when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(trackedProductRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.processStockChange(product, stockData);

            verify(alertRepository, never()).save(any());
        }

        @Test
        @DisplayName("should not create alert when alert setting is disabled")
        void shouldNotCreateAlertWhenDisabled() {
            UUID productId = UUID.randomUUID();
            StockTrackedProduct product = createTrackedProduct(productId, 10);
            product.setAlertOnOutOfStock(false);

            TrendyolPublicStockClient.StockData stockData = TrendyolPublicStockClient.StockData.builder()
                    .quantity(0).inStock(false).price(new BigDecimal("99.99")).build();

            when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(trackedProductRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.processStockChange(product, stockData);

            verify(alertRepository, never()).save(any());
        }

        @Test
        @DisplayName("should not create LOW_STOCK alert if stock became zero (OUT_OF_STOCK takes precedence)")
        void shouldNotCreateLowStockWhenBecameZero() {
            UUID productId = UUID.randomUUID();
            StockTrackedProduct product = createTrackedProduct(productId, 15);
            product.setAlertOnLowStock(true);
            product.setAlertOnOutOfStock(true);
            product.setLowStockThreshold(10);

            // Stock goes from 15 to 0 - should trigger OUT_OF_STOCK but NOT LOW_STOCK
            TrendyolPublicStockClient.StockData stockData = TrendyolPublicStockClient.StockData.builder()
                    .quantity(0).inStock(false).price(new BigDecimal("99.99")).build();

            when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(trackedProductRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.processStockChange(product, stockData);

            // Only OUT_OF_STOCK should be saved (LOW_STOCK requires newQty > 0)
            ArgumentCaptor<StockAlert> alertCaptor = ArgumentCaptor.forClass(StockAlert.class);
            verify(alertRepository, times(1)).save(alertCaptor.capture());
            assertThat(alertCaptor.getValue().getAlertType()).isEqualTo(StockAlertType.OUT_OF_STOCK);
        }

        @Test
        @DisplayName("should update product info when stock data has new values")
        void shouldUpdateProductInfo() {
            UUID productId = UUID.randomUUID();
            StockTrackedProduct product = createTrackedProduct(productId, null);
            product.setProductName("Old Name");

            TrendyolPublicStockClient.StockData stockData = TrendyolPublicStockClient.StockData.builder()
                    .quantity(30).inStock(true).price(new BigDecimal("199.99"))
                    .productName("New Name").imageUrl("https://new-image.jpg").build();

            when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(trackedProductRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.processStockChange(product, stockData);

            assertThat(product.getProductName()).isEqualTo("New Name");
            assertThat(product.getImageUrl()).isEqualTo("https://new-image.jpg");
            assertThat(product.getLastStockQuantity()).isEqualTo(30);
            assertThat(product.getLastPrice()).isEqualByComparingTo(new BigDecimal("199.99"));
        }
    }

    @Nested
    @DisplayName("updateAlertSettings")
    class UpdateAlertSettings {

        @Test
        @DisplayName("should update alert settings selectively")
        void shouldUpdateAlertSettings() {
            UUID productId = UUID.randomUUID();
            StockTrackedProduct product = createTrackedProduct(productId, 50);
            product.setAlertOnOutOfStock(true);
            product.setAlertOnLowStock(false);
            product.setLowStockThreshold(10);

            when(trackedProductRepository.findByIdWithStore(productId)).thenReturn(Optional.of(product));
            when(trackedProductRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateAlertSettingsRequest request = UpdateAlertSettingsRequest.builder()
                    .alertOnLowStock(true)
                    .lowStockThreshold(20)
                    .build();

            TrackedProductDto result = service.updateAlertSettings(productId, storeId, request);

            assertThat(product.getAlertOnLowStock()).isTrue();
            assertThat(product.getLowStockThreshold()).isEqualTo(20);
            // Should not change alertOnOutOfStock since not in request
            assertThat(product.getAlertOnOutOfStock()).isTrue();
        }

        @Test
        @DisplayName("should throw for wrong store")
        void shouldThrowForWrongStore() {
            UUID productId = UUID.randomUUID();
            StockTrackedProduct product = createTrackedProduct(productId, 50);

            when(trackedProductRepository.findByIdWithStore(productId)).thenReturn(Optional.of(product));

            UpdateAlertSettingsRequest request = UpdateAlertSettingsRequest.builder().build();

            assertThatThrownBy(() -> service.updateAlertSettings(productId, UUID.randomUUID(), request))
                    .isInstanceOf(SecurityException.class);
        }
    }

    @Nested
    @DisplayName("markAlertAsRead")
    class MarkAlertAsRead {

        @Test
        @DisplayName("should mark alert as read")
        void shouldMarkAsRead() {
            UUID alertId = UUID.randomUUID();
            StockAlert alert = StockAlert.builder()
                    .id(alertId)
                    .store(testStore)
                    .isRead(false)
                    .build();

            when(alertRepository.findById(alertId)).thenReturn(Optional.of(alert));
            when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.markAlertAsRead(alertId, storeId);

            assertThat(alert.getIsRead()).isTrue();
            assertThat(alert.getReadAt()).isNotNull();
            verify(alertRepository).save(alert);
        }

        @Test
        @DisplayName("should throw when alert belongs to different store")
        void shouldThrowWhenAlertBelongsToDifferentStore() {
            UUID alertId = UUID.randomUUID();
            StockAlert alert = StockAlert.builder()
                    .id(alertId)
                    .store(testStore)
                    .isRead(false)
                    .build();

            when(alertRepository.findById(alertId)).thenReturn(Optional.of(alert));

            assertThatThrownBy(() -> service.markAlertAsRead(alertId, UUID.randomUUID()))
                    .isInstanceOf(SecurityException.class);
        }
    }

    // Helper methods

    private StockTrackedProduct createTrackedProduct(UUID id, Integer lastStockQuantity) {
        return StockTrackedProduct.builder()
                .id(id)
                .store(testStore)
                .trendyolProductId(12345L)
                .productUrl("https://www.trendyol.com/test-p-12345")
                .productName("Test Product")
                .brandName("Test Brand")
                .lastStockQuantity(lastStockQuantity)
                .lastPrice(new BigDecimal("99.99"))
                .isActive(true)
                .alertOnOutOfStock(true)
                .alertOnLowStock(true)
                .lowStockThreshold(10)
                .alertOnStockIncrease(false)
                .alertOnBackInStock(true)
                .lastCheckedAt(LocalDateTime.now().minusHours(2))
                .build();
    }
}
