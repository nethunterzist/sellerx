package com.ecommerce.sellerx.webhook;

import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.common.TestDataBuilder;
import com.ecommerce.sellerx.orders.OrderCostCalculator;
import com.ecommerce.sellerx.orders.OrderItem;
import com.ecommerce.sellerx.orders.TrendyolOrder;
import com.ecommerce.sellerx.orders.TrendyolOrderRepository;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TrendyolWebhookService.
 * Tests webhook order processing, creation, and update logic.
 */
@DisplayName("TrendyolWebhookService")
class TrendyolWebhookServiceTest extends BaseUnitTest {

    @Mock
    private TrendyolOrderRepository orderRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private OrderCostCalculator costCalculator;

    private TrendyolWebhookService webhookService;

    private User testUser;
    private Store testStore;
    private static final String SELLER_ID = "100001";

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetSequence();
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        webhookService = new TrendyolWebhookService(orderRepository, storeRepository, costCalculator, meterRegistry);
        testUser = TestDataBuilder.user().build();
        testUser.setId(1L);
        testStore = TestDataBuilder.store(testUser).build();
        testStore.setId(UUID.randomUUID());
    }

    private TrendyolWebhookPayload createTestPayload(String orderNumber, String status) {
        TrendyolWebhookPayload payload = new TrendyolWebhookPayload();
        payload.setId(1000001L);
        payload.setOrderNumber(orderNumber);
        payload.setStatus(status);
        payload.setShipmentPackageStatus(status);
        payload.setOrderDate(System.currentTimeMillis());
        payload.setGrossAmount(new BigDecimal("299.99"));
        payload.setTotalDiscount(BigDecimal.ZERO);
        payload.setTotalTyDiscount(BigDecimal.ZERO);
        payload.setTotalPrice(new BigDecimal("299.99"));

        TrendyolWebhookPayload.OrderLine line = new TrendyolWebhookPayload.OrderLine();
        line.setBarcode("BARCODE-001");
        line.setProductName("Test Product");
        line.setQuantity(1);
        line.setAmount(new BigDecimal("299.99"));
        line.setDiscount(BigDecimal.ZERO);
        line.setTyDiscount(BigDecimal.ZERO);
        line.setVatBaseAmount(new BigDecimal("249.99"));
        line.setPrice(new BigDecimal("299.99"));
        payload.setLines(List.of(line));

        return payload;
    }

    @Nested
    @DisplayName("processWebhookOrder")
    class ProcessWebhookOrder {

        @Test
        @DisplayName("should create new order when order does not exist")
        void shouldCreateNewOrderWhenNotExists() {
            // Given
            TrendyolWebhookPayload payload = createTestPayload("TY-NEW-001", "Created");
            when(storeRepository.findBySellerId(SELLER_ID)).thenReturn(Optional.of(testStore));
            when(orderRepository.findByStoreIdAndPackageNo(testStore.getId(), payload.getId()))
                    .thenReturn(Optional.empty());
            when(orderRepository.save(any(TrendyolOrder.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            webhookService.processWebhookOrder(payload, SELLER_ID);

            // Then
            ArgumentCaptor<TrendyolOrder> orderCaptor = ArgumentCaptor.forClass(TrendyolOrder.class);
            verify(orderRepository).save(orderCaptor.capture());

            TrendyolOrder savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getTyOrderNumber())
                    .as("New order should have correct order number")
                    .isEqualTo("TY-NEW-001");
            assertThat(savedOrder.getStatus())
                    .as("New order should have correct status")
                    .isEqualTo("Created");
            assertThat(savedOrder.getStore())
                    .as("New order should be associated with the correct store")
                    .isEqualTo(testStore);
        }

        @Test
        @DisplayName("should update existing order when order already exists")
        void shouldUpdateExistingOrderWhenExists() {
            // Given
            TrendyolWebhookPayload payload = createTestPayload("TY-EXIST-001", "Delivered");
            payload.setLastModifiedDate(System.currentTimeMillis());

            TrendyolOrder existingOrder = TestDataBuilder.order(testStore)
                    .tyOrderNumber("TY-EXIST-001")
                    .packageNo(payload.getId())
                    .status("Created")
                    .build();

            when(storeRepository.findBySellerId(SELLER_ID)).thenReturn(Optional.of(testStore));
            when(orderRepository.findByStoreIdAndPackageNo(testStore.getId(), payload.getId()))
                    .thenReturn(Optional.of(existingOrder));
            when(orderRepository.save(any(TrendyolOrder.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            webhookService.processWebhookOrder(payload, SELLER_ID);

            // Then
            verify(orderRepository).save(existingOrder);
            assertThat(existingOrder.getStatus())
                    .as("Existing order status should be updated")
                    .isEqualTo("Delivered");
        }

        @Test
        @DisplayName("should skip processing when store not found for seller ID")
        void shouldSkipWhenStoreNotFound() {
            // Given
            TrendyolWebhookPayload payload = createTestPayload("TY-SKIP-001", "Created");
            when(storeRepository.findBySellerId("unknown-seller")).thenReturn(Optional.empty());

            // When
            webhookService.processWebhookOrder(payload, "unknown-seller");

            // Then
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("should not throw exception on processing error")
        void shouldNotThrowOnProcessingError() {
            // Given
            TrendyolWebhookPayload payload = createTestPayload("TY-ERR-001", "Created");
            when(storeRepository.findBySellerId(SELLER_ID)).thenReturn(Optional.of(testStore));
            when(orderRepository.findByStoreIdAndPackageNo(any(), anyLong()))
                    .thenThrow(new RuntimeException("Database error"));

            // When/Then - should not throw
            webhookService.processWebhookOrder(payload, SELLER_ID);

            // Verify no save was attempted after the error
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("should set data source as ORDER_API for new orders")
        void shouldSetDataSourceForNewOrders() {
            // Given
            TrendyolWebhookPayload payload = createTestPayload("TY-DS-001", "Created");
            when(storeRepository.findBySellerId(SELLER_ID)).thenReturn(Optional.of(testStore));
            when(orderRepository.findByStoreIdAndPackageNo(testStore.getId(), payload.getId()))
                    .thenReturn(Optional.empty());
            when(orderRepository.save(any(TrendyolOrder.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            webhookService.processWebhookOrder(payload, SELLER_ID);

            // Then
            ArgumentCaptor<TrendyolOrder> orderCaptor = ArgumentCaptor.forClass(TrendyolOrder.class);
            verify(orderRepository).save(orderCaptor.capture());

            assertThat(orderCaptor.getValue().getDataSource())
                    .as("New orders from webhook should have ORDER_API data source")
                    .isEqualTo("ORDER_API");
        }

        @Test
        @DisplayName("should set shipment city from webhook address")
        void shouldSetShipmentCityFromAddress() {
            // Given
            TrendyolWebhookPayload payload = createTestPayload("TY-CITY-001", "Created");
            TrendyolWebhookPayload.Address address = new TrendyolWebhookPayload.Address();
            address.setCity("Istanbul");
            address.setCityCode(34);
            address.setDistrict("Kadikoy");
            address.setDistrictId(123);
            payload.setShipmentAddress(address);

            when(storeRepository.findBySellerId(SELLER_ID)).thenReturn(Optional.of(testStore));
            when(orderRepository.findByStoreIdAndPackageNo(testStore.getId(), payload.getId()))
                    .thenReturn(Optional.empty());
            when(orderRepository.save(any(TrendyolOrder.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            webhookService.processWebhookOrder(payload, SELLER_ID);

            // Then
            ArgumentCaptor<TrendyolOrder> orderCaptor = ArgumentCaptor.forClass(TrendyolOrder.class);
            verify(orderRepository).save(orderCaptor.capture());

            TrendyolOrder savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getShipmentCity())
                    .as("Shipment city should be set from address")
                    .isEqualTo("Istanbul");
            assertThat(savedOrder.getShipmentCityCode())
                    .as("Shipment city code should be set from address")
                    .isEqualTo(34);
        }
    }
}
