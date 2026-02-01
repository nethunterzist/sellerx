package com.ecommerce.sellerx.orders;

import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import com.ecommerce.sellerx.stores.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CommissionEstimationService.
 * Tests the commission estimation logic for orders coming from Orders API.
 */
@ExtendWith(MockitoExtension.class)
class CommissionEstimationServiceTest {

    @Mock
    private TrendyolProductRepository productRepository;

    @Mock
    private OrderCostCalculator orderCostCalculator;

    @InjectMocks
    private CommissionEstimationService commissionEstimationService;

    private UUID storeId;
    private Store store;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        store = Store.builder()
                .id(storeId)
                .storeName("Test Store")
                .marketplace("trendyol")
                .build();
    }

    // ==================== calculateItemEstimatedCommission Tests ====================

    @Test
    @DisplayName("Should calculate commission using lastCommissionRate from product")
    void testCalculateWithLastCommissionRate() {
        // Given: Product with lastCommissionRate (from Settlement API)
        String barcode = "BARCODE123";
        BigDecimal price = new BigDecimal("799.40");
        BigDecimal vatRate = new BigDecimal("20");
        BigDecimal lastCommissionRate = new BigDecimal("19");
        BigDecimal expectedCommission = new BigDecimal("126.57");

        TrendyolProduct product = TrendyolProduct.builder()
                .barcode(barcode)
                .lastCommissionRate(lastCommissionRate)
                .commissionRate(new BigDecimal("15")) // fallback should NOT be used
                .build();

        Map<String, TrendyolProduct> productCache = Map.of(barcode, product);

        OrderItem item = OrderItem.builder()
                .barcode(barcode)
                .price(price)
                .vatBaseAmount(vatRate)
                .quantity(1)
                .build();

        // Mock: getEffectiveCommissionRate returns lastCommissionRate
        when(orderCostCalculator.getEffectiveCommissionRate(product))
                .thenReturn(lastCommissionRate);

        // Mock: calculateUnitEstimatedCommission returns expected value
        when(orderCostCalculator.calculateUnitEstimatedCommission(price, vatRate, lastCommissionRate))
                .thenReturn(expectedCommission);

        // When
        BigDecimal result = commissionEstimationService.calculateItemEstimatedCommission(item, productCache);

        // Then
        assertThat(result).isEqualByComparingTo(expectedCommission);
        verify(orderCostCalculator).getEffectiveCommissionRate(product);
        verify(orderCostCalculator).calculateUnitEstimatedCommission(price, vatRate, lastCommissionRate);
    }

    @Test
    @DisplayName("Should use fallback commissionRate when lastCommissionRate is null")
    void testCalculateWithFallbackCommissionRate() {
        // Given: Product with only commissionRate (no lastCommissionRate)
        String barcode = "BARCODE456";
        BigDecimal price = new BigDecimal("500.00");
        BigDecimal vatRate = new BigDecimal("20");
        BigDecimal fallbackCommissionRate = new BigDecimal("15");
        BigDecimal expectedCommission = new BigDecimal("62.50");

        TrendyolProduct product = TrendyolProduct.builder()
                .barcode(barcode)
                .lastCommissionRate(null) // No Settlement API data
                .commissionRate(fallbackCommissionRate) // Category default
                .build();

        Map<String, TrendyolProduct> productCache = Map.of(barcode, product);

        OrderItem item = OrderItem.builder()
                .barcode(barcode)
                .price(price)
                .vatBaseAmount(vatRate)
                .quantity(1)
                .build();

        // Mock: getEffectiveCommissionRate returns fallback
        when(orderCostCalculator.getEffectiveCommissionRate(product))
                .thenReturn(fallbackCommissionRate);

        // Mock: calculateUnitEstimatedCommission returns expected value
        when(orderCostCalculator.calculateUnitEstimatedCommission(price, vatRate, fallbackCommissionRate))
                .thenReturn(expectedCommission);

        // When
        BigDecimal result = commissionEstimationService.calculateItemEstimatedCommission(item, productCache);

        // Then
        assertThat(result).isEqualByComparingTo(expectedCommission);
        verify(orderCostCalculator).getEffectiveCommissionRate(product);
    }

    @Test
    @DisplayName("Should return zero when product not found in cache")
    void testCalculateWithNoProduct() {
        // Given: Empty product cache (product not found)
        String barcode = "UNKNOWN_BARCODE";
        Map<String, TrendyolProduct> productCache = Map.of(); // Empty

        OrderItem item = OrderItem.builder()
                .barcode(barcode)
                .price(new BigDecimal("100.00"))
                .vatBaseAmount(new BigDecimal("20"))
                .quantity(1)
                .build();

        // When
        BigDecimal result = commissionEstimationService.calculateItemEstimatedCommission(item, productCache);

        // Then
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        verifyNoInteractions(orderCostCalculator);
    }

    @Test
    @DisplayName("Should return zero when barcode is null or empty")
    void testCalculateWithNullBarcode() {
        // Given: Item with null barcode
        OrderItem itemWithNullBarcode = OrderItem.builder()
                .barcode(null)
                .price(new BigDecimal("100.00"))
                .build();

        OrderItem itemWithEmptyBarcode = OrderItem.builder()
                .barcode("")
                .price(new BigDecimal("100.00"))
                .build();

        Map<String, TrendyolProduct> productCache = Map.of();

        // When & Then
        assertThat(commissionEstimationService.calculateItemEstimatedCommission(itemWithNullBarcode, productCache))
                .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(commissionEstimationService.calculateItemEstimatedCommission(itemWithEmptyBarcode, productCache))
                .isEqualByComparingTo(BigDecimal.ZERO);

        verifyNoInteractions(orderCostCalculator);
    }

    // ==================== calculateOrderEstimatedCommission Tests ====================

    @Test
    @DisplayName("Should calculate total commission for multiple items")
    void testCalculateMultipleItems() {
        // Given: Order with 2 items
        String barcode1 = "BARCODE001";
        String barcode2 = "BARCODE002";

        TrendyolProduct product1 = TrendyolProduct.builder()
                .barcode(barcode1)
                .lastCommissionRate(new BigDecimal("19"))
                .build();

        TrendyolProduct product2 = TrendyolProduct.builder()
                .barcode(barcode2)
                .lastCommissionRate(new BigDecimal("15"))
                .build();

        OrderItem item1 = OrderItem.builder()
                .barcode(barcode1)
                .price(new BigDecimal("799.40"))
                .vatBaseAmount(new BigDecimal("20"))
                .quantity(1)
                .build();

        OrderItem item2 = OrderItem.builder()
                .barcode(barcode2)
                .price(new BigDecimal("500.00"))
                .vatBaseAmount(new BigDecimal("20"))
                .quantity(2)
                .build();

        TrendyolOrder order = TrendyolOrder.builder()
                .tyOrderNumber("ORDER123")
                .store(store)
                .orderItems(Arrays.asList(item1, item2))
                .build();

        // Mock: Product repository batch lookup
        when(productRepository.findByStoreIdAndBarcodeIn(eq(storeId), anyList()))
                .thenReturn(Arrays.asList(product1, product2));

        // Mock: Commission calculations
        when(orderCostCalculator.getEffectiveCommissionRate(product1))
                .thenReturn(new BigDecimal("19"));
        when(orderCostCalculator.getEffectiveCommissionRate(product2))
                .thenReturn(new BigDecimal("15"));

        when(orderCostCalculator.calculateUnitEstimatedCommission(
                eq(new BigDecimal("799.40")), eq(new BigDecimal("20")), eq(new BigDecimal("19"))))
                .thenReturn(new BigDecimal("126.57"));

        when(orderCostCalculator.calculateUnitEstimatedCommission(
                eq(new BigDecimal("500.00")), eq(new BigDecimal("20")), eq(new BigDecimal("15"))))
                .thenReturn(new BigDecimal("62.50"));

        // When
        BigDecimal result = commissionEstimationService.calculateOrderEstimatedCommission(order);

        // Then: 126.57 + (62.50 * 2) = 126.57 + 125.00 = 251.57
        assertThat(result).isEqualByComparingTo(new BigDecimal("251.57"));
    }

    @Test
    @DisplayName("Should return zero for order with null or empty items")
    void testCalculateWithEmptyOrderItems() {
        // Given: Order with null items
        TrendyolOrder orderWithNullItems = TrendyolOrder.builder()
                .tyOrderNumber("ORDER_NULL")
                .store(store)
                .orderItems(null)
                .build();

        // Given: Order with empty items
        TrendyolOrder orderWithEmptyItems = TrendyolOrder.builder()
                .tyOrderNumber("ORDER_EMPTY")
                .store(store)
                .orderItems(Collections.emptyList())
                .build();

        // When & Then
        assertThat(commissionEstimationService.calculateOrderEstimatedCommission(orderWithNullItems))
                .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(commissionEstimationService.calculateOrderEstimatedCommission(orderWithEmptyItems))
                .isEqualByComparingTo(BigDecimal.ZERO);

        verifyNoInteractions(productRepository);
    }

    // ==================== estimateAndSetOrderCommission Tests ====================

    @Test
    @DisplayName("Should set estimated commission and flag on order")
    void testEstimateAndSetOrderCommission() {
        // Given
        String barcode = "BARCODE_SET";
        TrendyolProduct product = TrendyolProduct.builder()
                .barcode(barcode)
                .lastCommissionRate(new BigDecimal("19"))
                .build();

        OrderItem item = OrderItem.builder()
                .barcode(barcode)
                .price(new BigDecimal("799.40"))
                .vatBaseAmount(new BigDecimal("20"))
                .quantity(1)
                .build();

        TrendyolOrder order = TrendyolOrder.builder()
                .tyOrderNumber("ORDER_SET_TEST")
                .store(store)
                .dataSource("ORDER_API")
                .orderItems(Collections.singletonList(item))
                .isCommissionEstimated(null)
                .estimatedCommission(null)
                .build();

        when(productRepository.findByStoreIdAndBarcodeIn(eq(storeId), anyList()))
                .thenReturn(Collections.singletonList(product));

        when(orderCostCalculator.getEffectiveCommissionRate(product))
                .thenReturn(new BigDecimal("19"));

        when(orderCostCalculator.calculateUnitEstimatedCommission(
                eq(new BigDecimal("799.40")), eq(new BigDecimal("20")), eq(new BigDecimal("19"))))
                .thenReturn(new BigDecimal("126.57"));

        // When
        commissionEstimationService.estimateAndSetOrderCommission(order);

        // Then
        assertThat(order.getEstimatedCommission()).isEqualByComparingTo(new BigDecimal("126.57"));
        assertThat(order.getIsCommissionEstimated()).isTrue();
    }

    // ==================== needsReconciliation Tests ====================

    @Test
    @DisplayName("Should identify orders needing reconciliation")
    void testNeedsReconciliation() {
        // Given: Order from ORDER_API with estimated commission
        TrendyolOrder orderNeedsReconciliation = TrendyolOrder.builder()
                .tyOrderNumber("ORDER_NEEDS_RECON")
                .dataSource("ORDER_API")
                .isCommissionEstimated(true)
                .build();

        // Given: Order already reconciled (HYBRID)
        TrendyolOrder orderAlreadyReconciled = TrendyolOrder.builder()
                .tyOrderNumber("ORDER_RECONCILED")
                .dataSource("HYBRID")
                .isCommissionEstimated(false)
                .build();

        // Given: Order from Settlement API (doesn't need reconciliation)
        TrendyolOrder orderFromSettlement = TrendyolOrder.builder()
                .tyOrderNumber("ORDER_SETTLEMENT")
                .dataSource("SETTLEMENT_API")
                .isCommissionEstimated(false)
                .build();

        // When & Then
        assertThat(commissionEstimationService.needsReconciliation(orderNeedsReconciliation)).isTrue();
        assertThat(commissionEstimationService.needsReconciliation(orderAlreadyReconciled)).isFalse();
        assertThat(commissionEstimationService.needsReconciliation(orderFromSettlement)).isFalse();
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle commission rate of zero")
    void testZeroCommissionRate() {
        // Given: Product with zero commission rate
        String barcode = "BARCODE_ZERO";
        TrendyolProduct product = TrendyolProduct.builder()
                .barcode(barcode)
                .lastCommissionRate(BigDecimal.ZERO)
                .build();

        Map<String, TrendyolProduct> productCache = Map.of(barcode, product);

        OrderItem item = OrderItem.builder()
                .barcode(barcode)
                .price(new BigDecimal("100.00"))
                .vatBaseAmount(new BigDecimal("20"))
                .quantity(1)
                .build();

        when(orderCostCalculator.getEffectiveCommissionRate(product))
                .thenReturn(BigDecimal.ZERO);

        // When
        BigDecimal result = commissionEstimationService.calculateItemEstimatedCommission(item, productCache);

        // Then: Zero commission rate means zero commission
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        verify(orderCostCalculator, never()).calculateUnitEstimatedCommission(any(), any(), any());
    }

    @Test
    @DisplayName("Should handle null quantity as 1")
    void testNullQuantityDefaultsToOne() {
        // Given: Item with null quantity
        String barcode = "BARCODE_NULL_QTY";
        BigDecimal price = new BigDecimal("100.00");
        BigDecimal vatRate = new BigDecimal("20");
        BigDecimal commissionRate = new BigDecimal("10");
        BigDecimal unitCommission = new BigDecimal("8.33");

        TrendyolProduct product = TrendyolProduct.builder()
                .barcode(barcode)
                .lastCommissionRate(commissionRate)
                .build();

        Map<String, TrendyolProduct> productCache = Map.of(barcode, product);

        OrderItem item = OrderItem.builder()
                .barcode(barcode)
                .price(price)
                .vatBaseAmount(vatRate)
                .quantity(null) // Null quantity
                .build();

        when(orderCostCalculator.getEffectiveCommissionRate(product))
                .thenReturn(commissionRate);

        when(orderCostCalculator.calculateUnitEstimatedCommission(price, vatRate, commissionRate))
                .thenReturn(unitCommission);

        // When
        BigDecimal result = commissionEstimationService.calculateItemEstimatedCommission(item, productCache);

        // Then: Should use quantity=1 as default
        assertThat(result).isEqualByComparingTo(unitCommission);
    }
}
