package com.ecommerce.sellerx.orders;

import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.products.CostAndStockInfo;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("StockOrderSynchronizationService")
class StockOrderSynchronizationServiceTest extends BaseUnitTest {

    @Mock
    private TrendyolOrderRepository orderRepository;

    @Mock
    private TrendyolProductRepository productRepository;

    private StockOrderSynchronizationService service;

    private UUID storeId;

    @BeforeEach
    void setUp() {
        service = new StockOrderSynchronizationService(orderRepository, productRepository);
        storeId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("FIFO Split Allocation")
    class FifoSplitAllocation {

        @Test
        @DisplayName("should allocate from two lots with weighted average cost")
        void testFifoSplitAllocation_TwoLots() {
            // Given: Lot1 = 10 units @ 20 TL (Jan 1), Lot2 = 50 units @ 22 TL (Jan 15)
            TrendyolProduct product = createProduct("BARCODE-001",
                    createStockInfo(10, 20.0, 18, LocalDate.of(2025, 1, 1)),
                    createStockInfo(50, 22.0, 18, LocalDate.of(2025, 1, 15))
            );

            // Order: 15 units on Jan 20 (needs 10 from Lot1 + 5 from Lot2)
            OrderItem orderItem = OrderItem.builder()
                    .barcode("BARCODE-001")
                    .quantity(15)
                    .build();
            TrendyolOrder order = createOrder(storeId, LocalDateTime.of(2025, 1, 20, 10, 0), orderItem);

            when(productRepository.findByStoreId(storeId)).thenReturn(List.of(product));
            when(orderRepository.findOrdersWithProductFromDate(eq(storeId), eq("BARCODE-001"), any()))
                    .thenReturn(List.of(order));

            // When
            service.redistributeStockFIFO(storeId, LocalDate.of(2025, 1, 1));

            // Then: Weighted average = (10*20 + 5*22) / 15 = 310/15 = 20.67
            assertThat(orderItem.getCost()).isNotNull();
            assertThat(orderItem.getCost()).isEqualByComparingTo(new BigDecimal("20.67"));
            assertThat(orderItem.getCostSource()).isEqualTo("FIFO");
            assertThat(orderItem.getStockDate()).isEqualTo(LocalDate.of(2025, 1, 1));
            assertThat(orderItem.getCostVat()).isEqualTo(18);

            // Verify stock usage: Lot1 fully used (10), Lot2 partially used (5)
            assertThat(product.getCostAndStockInfo().get(0).getUsedQuantity()).isEqualTo(10);
            assertThat(product.getCostAndStockInfo().get(1).getUsedQuantity()).isEqualTo(5);

            // stockDepleted should be false (Lot2 still has remaining)
            assertThat(product.getStockDepleted()).isFalse();
        }

        @Test
        @DisplayName("should allocate from three lots with weighted average cost")
        void testFifoSplitAllocation_ThreeLots() {
            // Given: Lot1=5@10, Lot2=8@15, Lot3=100@20
            TrendyolProduct product = createProduct("BARCODE-002",
                    createStockInfo(5, 10.0, 18, LocalDate.of(2025, 1, 1)),
                    createStockInfo(8, 15.0, 18, LocalDate.of(2025, 1, 10)),
                    createStockInfo(100, 20.0, 18, LocalDate.of(2025, 1, 20))
            );

            // Order: 20 units on Feb 1 (needs 5+8+7 from three lots)
            OrderItem orderItem = OrderItem.builder()
                    .barcode("BARCODE-002")
                    .quantity(20)
                    .build();
            TrendyolOrder order = createOrder(storeId, LocalDateTime.of(2025, 2, 1, 10, 0), orderItem);

            when(productRepository.findByStoreId(storeId)).thenReturn(List.of(product));
            when(orderRepository.findOrdersWithProductFromDate(eq(storeId), eq("BARCODE-002"), any()))
                    .thenReturn(List.of(order));

            // When
            service.redistributeStockFIFO(storeId, LocalDate.of(2025, 1, 1));

            // Then: Weighted average = (5*10 + 8*15 + 7*20) / 20 = (50+120+140)/20 = 310/20 = 15.50
            assertThat(orderItem.getCost()).isEqualByComparingTo(new BigDecimal("15.50"));
            assertThat(orderItem.getCostSource()).isEqualTo("FIFO");

            // Verify stock usage: Lot1=5, Lot2=8, Lot3=7
            assertThat(product.getCostAndStockInfo().get(0).getUsedQuantity()).isEqualTo(5);
            assertThat(product.getCostAndStockInfo().get(1).getUsedQuantity()).isEqualTo(8);
            assertThat(product.getCostAndStockInfo().get(2).getUsedQuantity()).isEqualTo(7);
        }

        @Test
        @DisplayName("should allocate from single lot when sufficient")
        void testFifoSingleLotAllocation() {
            // Given: Single lot with enough stock
            TrendyolProduct product = createProduct("BARCODE-003",
                    createStockInfo(100, 25.0, 20, LocalDate.of(2025, 1, 1))
            );

            OrderItem orderItem = OrderItem.builder()
                    .barcode("BARCODE-003")
                    .quantity(10)
                    .build();
            TrendyolOrder order = createOrder(storeId, LocalDateTime.of(2025, 1, 15, 10, 0), orderItem);

            when(productRepository.findByStoreId(storeId)).thenReturn(List.of(product));
            when(orderRepository.findOrdersWithProductFromDate(eq(storeId), eq("BARCODE-003"), any()))
                    .thenReturn(List.of(order));

            // When
            service.redistributeStockFIFO(storeId, LocalDate.of(2025, 1, 1));

            // Then: Single lot, exact cost
            assertThat(orderItem.getCost()).isEqualByComparingTo(new BigDecimal("25.00"));
            assertThat(orderItem.getCostSource()).isEqualTo("FIFO");
            assertThat(product.getCostAndStockInfo().get(0).getUsedQuantity()).isEqualTo(10);
        }

        @Test
        @DisplayName("should respect FIFO date ordering — stock after order date is skipped")
        void testStockDateOrdering() {
            // Given: Lot1=5@10 (Jan 1), Lot2=100@30 (Feb 1 — AFTER order date)
            TrendyolProduct product = createProduct("BARCODE-004",
                    createStockInfo(5, 10.0, 18, LocalDate.of(2025, 1, 1)),
                    createStockInfo(100, 30.0, 18, LocalDate.of(2025, 2, 1))
            );

            // Order: 10 units on Jan 15 — can only use Lot1 (5 units), Lot2 is after order date
            OrderItem orderItem = OrderItem.builder()
                    .barcode("BARCODE-004")
                    .quantity(10)
                    .build();
            TrendyolOrder order = createOrder(storeId, LocalDateTime.of(2025, 1, 15, 10, 0), orderItem);

            when(productRepository.findByStoreId(storeId)).thenReturn(List.of(product));
            when(orderRepository.findOrdersWithProductFromDate(eq(storeId), eq("BARCODE-004"), any()))
                    .thenReturn(List.of(order));

            // When
            service.redistributeStockFIFO(storeId, LocalDate.of(2025, 1, 1));

            // Then: Only 5 units from Lot1, rest falls to LAST_KNOWN from Lot1
            // Since Lot1 has remaining=5 but we need 10, after using 5 from FIFO,
            // the code tries to use Lot2 but it's after order date.
            // totalAllocated=5, so it returns weighted average of those 5 = 10.0 with FIFO source
            assertThat(orderItem.getCost()).isEqualByComparingTo(new BigDecimal("10.00"));
            assertThat(orderItem.getCostSource()).isEqualTo("FIFO");
            assertThat(product.getCostAndStockInfo().get(0).getUsedQuantity()).isEqualTo(5);
            assertThat(product.getCostAndStockInfo().get(1).getUsedQuantity()).isEqualTo(0);
        }

        @Test
        @DisplayName("should handle multiple orders consuming stock sequentially")
        void testMultipleOrdersSequential() {
            // Given: Single lot with 20 units @ 15 TL
            TrendyolProduct product = createProduct("BARCODE-005",
                    createStockInfo(20, 15.0, 18, LocalDate.of(2025, 1, 1))
            );

            // Two orders: 12 units (Jan 10) + 8 units (Jan 15)
            OrderItem orderItem1 = OrderItem.builder().barcode("BARCODE-005").quantity(12).build();
            TrendyolOrder order1 = createOrder(storeId, LocalDateTime.of(2025, 1, 10, 10, 0), orderItem1);

            OrderItem orderItem2 = OrderItem.builder().barcode("BARCODE-005").quantity(8).build();
            TrendyolOrder order2 = createOrder(storeId, LocalDateTime.of(2025, 1, 15, 10, 0), orderItem2);

            when(productRepository.findByStoreId(storeId)).thenReturn(List.of(product));
            when(orderRepository.findOrdersWithProductFromDate(eq(storeId), eq("BARCODE-005"), any()))
                    .thenReturn(List.of(order1, order2));

            // When
            service.redistributeStockFIFO(storeId, LocalDate.of(2025, 1, 1));

            // Then: First order uses 12, second uses 8, total 20 = fully depleted
            assertThat(orderItem1.getCost()).isEqualByComparingTo(new BigDecimal("15.00"));
            assertThat(orderItem1.getCostSource()).isEqualTo("FIFO");
            assertThat(orderItem2.getCost()).isEqualByComparingTo(new BigDecimal("15.00"));
            assertThat(orderItem2.getCostSource()).isEqualTo("FIFO");
            assertThat(product.getCostAndStockInfo().get(0).getUsedQuantity()).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("Last Known Cost Fallback")
    class LastKnownCostFallback {

        @Test
        @DisplayName("should use last known cost when all lots are depleted")
        void testLastKnownCostFallback() {
            // Given: Lot before fromDate (so it won't be reset), fully consumed
            CostAndStockInfo depletedLot = createStockInfo(10, 20.0, 18, LocalDate.of(2024, 12, 1));
            depletedLot.setUsedQuantity(10); // Fully used

            TrendyolProduct product = createProduct("BARCODE-DEPLETED", depletedLot);

            // Order: 5 units, but no remaining stock
            OrderItem orderItem = OrderItem.builder()
                    .barcode("BARCODE-DEPLETED")
                    .quantity(5)
                    .build();
            TrendyolOrder order = createOrder(storeId, LocalDateTime.of(2025, 2, 1, 10, 0), orderItem);

            when(productRepository.findByStoreId(storeId)).thenReturn(List.of(product));
            when(orderRepository.findOrdersWithProductFromDate(eq(storeId), eq("BARCODE-DEPLETED"), any()))
                    .thenReturn(List.of(order));

            // When: fromDate=Jan 1 → lot at Dec 1 is NOT reset (stays depleted)
            service.redistributeStockFIFO(storeId, LocalDate.of(2025, 1, 1));

            // Then: Falls back to last known cost
            assertThat(orderItem.getCost()).isEqualByComparingTo(new BigDecimal("20.00"));
            assertThat(orderItem.getCostSource()).isEqualTo("LAST_KNOWN");
            assertThat(orderItem.getStockDate()).isEqualTo(LocalDate.of(2024, 12, 1));

            // stockDepleted should be true
            assertThat(product.getStockDepleted()).isTrue();
        }

        @Test
        @DisplayName("should use most recent lot cost as last known when multiple lots depleted")
        void testLastKnownCostUsesNewestLot() {
            // Given: Two depleted lots BEFORE fromDate (so they won't be reset)
            CostAndStockInfo lot1 = createStockInfo(5, 10.0, 18, LocalDate.of(2024, 11, 1));
            lot1.setUsedQuantity(5);
            CostAndStockInfo lot2 = createStockInfo(10, 25.0, 18, LocalDate.of(2024, 12, 1));
            lot2.setUsedQuantity(10);

            TrendyolProduct product = createProduct("BARCODE-MULTI-DEPLETED", lot1, lot2);

            OrderItem orderItem = OrderItem.builder()
                    .barcode("BARCODE-MULTI-DEPLETED")
                    .quantity(3)
                    .build();
            TrendyolOrder order = createOrder(storeId, LocalDateTime.of(2025, 3, 1, 10, 0), orderItem);

            when(productRepository.findByStoreId(storeId)).thenReturn(List.of(product));
            when(orderRepository.findOrdersWithProductFromDate(eq(storeId), eq("BARCODE-MULTI-DEPLETED"), any()))
                    .thenReturn(List.of(order));

            // When: fromDate=Jan 1 → lots at Nov/Dec are NOT reset (stay depleted)
            service.redistributeStockFIFO(storeId, LocalDate.of(2025, 1, 1));

            // Then: Should use Lot2's cost (25 TL) as it's the most recent
            assertThat(orderItem.getCost()).isEqualByComparingTo(new BigDecimal("25.00"));
            assertThat(orderItem.getCostSource()).isEqualTo("LAST_KNOWN");
        }

        @Test
        @DisplayName("should set null cost when no stock data exists at all")
        void testNoStockAtAll() {
            // Given: Product with empty stock info
            TrendyolProduct product = TrendyolProduct.builder()
                    .id(UUID.randomUUID())
                    .barcode("BARCODE-NO-STOCK")
                    .costAndStockInfo(new ArrayList<>())
                    .build();

            when(productRepository.findByStoreId(storeId)).thenReturn(List.of(product));

            // When
            service.redistributeStockFIFO(storeId, LocalDate.of(2025, 1, 1));

            // Then: No orders to process (product has no stock entries, will be skipped)
            verify(orderRepository, never()).findOrdersWithProductFromDate(any(), any(), any());
        }

        @Test
        @DisplayName("should set null cost when product has stock but no lots exist before order date")
        void testNoStockBeforeOrderDate() {
            // Given: Lot only exists AFTER the order date
            TrendyolProduct product = createProduct("BARCODE-FUTURE",
                    createStockInfo(100, 50.0, 18, LocalDate.of(2025, 6, 1))
            );

            OrderItem orderItem = OrderItem.builder()
                    .barcode("BARCODE-FUTURE")
                    .quantity(5)
                    .build();
            TrendyolOrder order = createOrder(storeId, LocalDateTime.of(2025, 1, 15, 10, 0), orderItem);

            when(productRepository.findByStoreId(storeId)).thenReturn(List.of(product));
            when(orderRepository.findOrdersWithProductFromDate(eq(storeId), eq("BARCODE-FUTURE"), any()))
                    .thenReturn(List.of(order));

            // When
            service.redistributeStockFIFO(storeId, LocalDate.of(2025, 1, 1));

            // Then: No eligible lots, cost should be null
            assertThat(orderItem.getCost()).isNull();
            assertThat(orderItem.getCostSource()).isNull();
        }
    }

    @Nested
    @DisplayName("Stock Depleted Flag")
    class StockDepletedFlag {

        @Test
        @DisplayName("should set stockDepleted=true when LAST_KNOWN cost is used")
        void testStockDepletedFlagSet() {
            // Given: Lot with 5 units, order needs 10 → partial FIFO + triggers depletion
            TrendyolProduct product = createProduct("BARCODE-FLAG",
                    createStockInfo(5, 20.0, 18, LocalDate.of(2025, 1, 1))
            );

            // First order uses all 5, second order will trigger LAST_KNOWN
            OrderItem orderItem1 = OrderItem.builder().barcode("BARCODE-FLAG").quantity(5).build();
            TrendyolOrder order1 = createOrder(storeId, LocalDateTime.of(2025, 1, 10, 10, 0), orderItem1);

            OrderItem orderItem2 = OrderItem.builder().barcode("BARCODE-FLAG").quantity(3).build();
            TrendyolOrder order2 = createOrder(storeId, LocalDateTime.of(2025, 1, 20, 10, 0), orderItem2);

            when(productRepository.findByStoreId(storeId)).thenReturn(List.of(product));
            when(orderRepository.findOrdersWithProductFromDate(eq(storeId), eq("BARCODE-FLAG"), any()))
                    .thenReturn(List.of(order1, order2));

            // When
            service.redistributeStockFIFO(storeId, LocalDate.of(2025, 1, 1));

            // Then
            assertThat(orderItem1.getCostSource()).isEqualTo("FIFO");
            assertThat(orderItem2.getCostSource()).isEqualTo("LAST_KNOWN");
            assertThat(product.getStockDepleted()).isTrue();

            verify(productRepository).save(product);
        }

        @Test
        @DisplayName("should set stockDepleted=false when all orders use FIFO")
        void testStockDepletedFlagNotSet() {
            // Given: Plenty of stock
            TrendyolProduct product = createProduct("BARCODE-PLENTY",
                    createStockInfo(100, 15.0, 18, LocalDate.of(2025, 1, 1))
            );

            OrderItem orderItem = OrderItem.builder().barcode("BARCODE-PLENTY").quantity(10).build();
            TrendyolOrder order = createOrder(storeId, LocalDateTime.of(2025, 1, 15, 10, 0), orderItem);

            when(productRepository.findByStoreId(storeId)).thenReturn(List.of(product));
            when(orderRepository.findOrdersWithProductFromDate(eq(storeId), eq("BARCODE-PLENTY"), any()))
                    .thenReturn(List.of(order));

            // When
            service.redistributeStockFIFO(storeId, LocalDate.of(2025, 1, 1));

            // Then
            assertThat(orderItem.getCostSource()).isEqualTo("FIFO");
            assertThat(product.getStockDepleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("Stock Usage Reset")
    class StockUsageReset {

        @Test
        @DisplayName("should reset usage counters from the specified date onwards")
        void testResetStockUsageFromDate() {
            // Given: Two lots — Lot1 (Jan 1, used=5), Lot2 (Feb 1, used=10)
            CostAndStockInfo lot1 = createStockInfo(20, 10.0, 18, LocalDate.of(2025, 1, 1));
            lot1.setUsedQuantity(5);
            CostAndStockInfo lot2 = createStockInfo(30, 15.0, 18, LocalDate.of(2025, 2, 1));
            lot2.setUsedQuantity(10);

            TrendyolProduct product = createProduct("BARCODE-RESET", lot1, lot2);

            // No orders for simplicity — just test reset
            when(productRepository.findByStoreId(storeId)).thenReturn(List.of(product));
            when(orderRepository.findOrdersWithProductFromDate(eq(storeId), eq("BARCODE-RESET"), any()))
                    .thenReturn(Collections.emptyList());

            // When: Reset from Feb 1 onwards
            service.redistributeStockFIFO(storeId, LocalDate.of(2025, 2, 1));

            // Then: Only Lot2 should be reset (it's >= Feb 1), Lot1 stays
            assertThat(lot1.getUsedQuantity()).isEqualTo(5); // Not reset (before fromDate)
            assertThat(lot2.getUsedQuantity()).isEqualTo(0); // Reset (on fromDate)
        }
    }

    // === Helper Methods ===

    private CostAndStockInfo createStockInfo(int quantity, double unitCost, int vatRate, LocalDate stockDate) {
        return CostAndStockInfo.builder()
                .quantity(quantity)
                .unitCost(unitCost)
                .costVatRate(vatRate)
                .stockDate(stockDate)
                .usedQuantity(0)
                .build();
    }

    private TrendyolProduct createProduct(String barcode, CostAndStockInfo... stockInfos) {
        return TrendyolProduct.builder()
                .id(UUID.randomUUID())
                .barcode(barcode)
                .costAndStockInfo(new ArrayList<>(Arrays.asList(stockInfos)))
                .stockDepleted(false)
                .build();
    }

    private TrendyolOrder createOrder(UUID storeId, LocalDateTime orderDate, OrderItem... items) {
        return TrendyolOrder.builder()
                .id(UUID.randomUUID())
                .orderDate(orderDate)
                .orderItems(new ArrayList<>(Arrays.asList(items)))
                .grossAmount(BigDecimal.ZERO)
                .totalPrice(BigDecimal.ZERO)
                .tyOrderNumber("TY-" + UUID.randomUUID().toString().substring(0, 8))
                .packageNo(System.nanoTime())
                .status("Delivered")
                .build();
    }
}
