package com.ecommerce.sellerx.orders;

import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.products.CostAndStockInfo;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OrderCostCalculator.
 * Tests FIFO cost allocation, commission calculation, and edge cases.
 */
@DisplayName("OrderCostCalculator")
class OrderCostCalculatorTest extends BaseUnitTest {

    @Mock
    private TrendyolProductRepository productRepository;

    private OrderCostCalculator calculator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        calculator = new OrderCostCalculator(productRepository);
    }

    @Nested
    @DisplayName("findAppropriateCostForProduct")
    class FindAppropriateCostForProduct {

        @Test
        @DisplayName("should return null when product has no cost info")
        void shouldReturnNullWhenNoCostInfo() {
            // Given
            TrendyolProduct product = TrendyolProduct.builder()
                    .barcode("TEST-BARCODE")
                    .costAndStockInfo(new ArrayList<>())
                    .build();

            // When
            CostAndStockInfo result = calculator.findAppropriateCostForProduct(product, LocalDate.now());

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return single cost entry when only one exists")
        void shouldReturnSingleCostEntry() {
            // Given
            CostAndStockInfo cost = CostAndStockInfo.builder()
                    .unitCost(100.0)
                    .quantity(10)
                    .costVatRate(20)
                    .stockDate(LocalDate.of(2024, 1, 1))
                    .usedQuantity(0)
                    .build();

            TrendyolProduct product = TrendyolProduct.builder()
                    .barcode("TEST-BARCODE")
                    .costAndStockInfo(List.of(cost))
                    .build();

            // When
            CostAndStockInfo result = calculator.findAppropriateCostForProduct(product, LocalDate.of(2024, 6, 15));

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUnitCost()).isEqualTo(100.0);
            assertThat(result.getStockDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        }

        @Test
        @DisplayName("should return cost from before order date using FIFO")
        void shouldReturnCostBeforeOrderDate() {
            // Given - Multiple cost entries at different dates
            CostAndStockInfo cost1 = CostAndStockInfo.builder()
                    .unitCost(80.0)
                    .quantity(10)
                    .stockDate(LocalDate.of(2024, 1, 1))
                    .usedQuantity(0)
                    .build();

            CostAndStockInfo cost2 = CostAndStockInfo.builder()
                    .unitCost(90.0)
                    .quantity(15)
                    .stockDate(LocalDate.of(2024, 3, 1))
                    .usedQuantity(0)
                    .build();

            CostAndStockInfo cost3 = CostAndStockInfo.builder()
                    .unitCost(100.0)
                    .quantity(20)
                    .stockDate(LocalDate.of(2024, 6, 1))
                    .usedQuantity(0)
                    .build();

            TrendyolProduct product = TrendyolProduct.builder()
                    .barcode("TEST-BARCODE")
                    .costAndStockInfo(List.of(cost1, cost2, cost3))
                    .build();

            // When - Order date is April 15, should use cost from March 1
            CostAndStockInfo result = calculator.findAppropriateCostForProduct(product, LocalDate.of(2024, 4, 15));

            // Then - Should return the latest cost that's on or before order date
            assertThat(result).isNotNull();
            assertThat(result.getUnitCost()).isEqualTo(90.0);
            assertThat(result.getStockDate()).isEqualTo(LocalDate.of(2024, 3, 1));
        }

        @Test
        @DisplayName("should return earliest cost when order date is before all stock dates")
        void shouldReturnEarliestCostWhenOrderBeforeAllStocks() {
            // Given
            CostAndStockInfo cost1 = CostAndStockInfo.builder()
                    .unitCost(100.0)
                    .quantity(10)
                    .stockDate(LocalDate.of(2024, 3, 1))
                    .usedQuantity(0)
                    .build();

            CostAndStockInfo cost2 = CostAndStockInfo.builder()
                    .unitCost(110.0)
                    .quantity(15)
                    .stockDate(LocalDate.of(2024, 6, 1))
                    .usedQuantity(0)
                    .build();

            TrendyolProduct product = TrendyolProduct.builder()
                    .barcode("TEST-BARCODE")
                    .costAndStockInfo(List.of(cost1, cost2))
                    .build();

            // When - Order date is before all stock dates
            CostAndStockInfo result = calculator.findAppropriateCostForProduct(product, LocalDate.of(2024, 1, 1));

            // Then - Should return earliest available cost as fallback
            assertThat(result).isNotNull();
            assertThat(result.getUnitCost()).isEqualTo(100.0);
            assertThat(result.getStockDate()).isEqualTo(LocalDate.of(2024, 3, 1));
        }

        @Test
        @DisplayName("should ignore cost entries with null stock date")
        void shouldIgnoreNullStockDates() {
            // Given
            CostAndStockInfo costWithoutDate = CostAndStockInfo.builder()
                    .unitCost(50.0)
                    .quantity(5)
                    .stockDate(null)
                    .build();

            CostAndStockInfo costWithDate = CostAndStockInfo.builder()
                    .unitCost(100.0)
                    .quantity(10)
                    .stockDate(LocalDate.of(2024, 1, 1))
                    .usedQuantity(0)
                    .build();

            TrendyolProduct product = TrendyolProduct.builder()
                    .barcode("TEST-BARCODE")
                    .costAndStockInfo(List.of(costWithoutDate, costWithDate))
                    .build();

            // When
            CostAndStockInfo result = calculator.findAppropriateCostForProduct(product, LocalDate.of(2024, 6, 1));

            // Then - Should ignore null date entry and return valid one
            assertThat(result).isNotNull();
            assertThat(result.getUnitCost()).isEqualTo(100.0);
        }
    }

    @Nested
    @DisplayName("getEffectiveCommissionRate")
    class GetEffectiveCommissionRate {

        @Test
        @DisplayName("should return lastCommissionRate when available")
        void shouldReturnLastCommissionRate() {
            // Given
            TrendyolProduct product = TrendyolProduct.builder()
                    .barcode("TEST-BARCODE")
                    .commissionRate(new BigDecimal("15.00"))
                    .lastCommissionRate(new BigDecimal("18.50"))
                    .build();

            // When
            BigDecimal result = calculator.getEffectiveCommissionRate(product);

            // Then
            assertThat(result).isEqualByComparingTo(new BigDecimal("18.50"));
        }

        @Test
        @DisplayName("should fallback to commissionRate when lastCommissionRate is null")
        void shouldFallbackToCommissionRate() {
            // Given
            TrendyolProduct product = TrendyolProduct.builder()
                    .barcode("TEST-BARCODE")
                    .commissionRate(new BigDecimal("15.00"))
                    .lastCommissionRate(null)
                    .build();

            // When
            BigDecimal result = calculator.getEffectiveCommissionRate(product);

            // Then
            assertThat(result).isEqualByComparingTo(new BigDecimal("15.00"));
        }

        @Test
        @DisplayName("should return zero when both rates are null")
        void shouldReturnZeroWhenBothNull() {
            // Given
            TrendyolProduct product = TrendyolProduct.builder()
                    .barcode("TEST-BARCODE")
                    .commissionRate(null)
                    .lastCommissionRate(null)
                    .build();

            // When
            BigDecimal result = calculator.getEffectiveCommissionRate(product);

            // Then
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("calculateUnitEstimatedCommission")
    class CalculateUnitEstimatedCommission {

        @Test
        @DisplayName("should calculate commission correctly with standard VAT rate")
        void shouldCalculateCommissionWithStandardVat() {
            // Given
            // Price: 799.40 TL (VAT included)
            // VAT Rate: 20%
            // Commission Rate: 19%
            // Expected: 799.40 / 1.20 = 666.17 TL (VAT base)
            // Commission: 666.17 * 0.19 = 126.57 TL
            BigDecimal price = new BigDecimal("799.40");
            BigDecimal vatRate = new BigDecimal("20");
            BigDecimal commissionRate = new BigDecimal("19");

            // When
            BigDecimal result = calculator.calculateUnitEstimatedCommission(price, vatRate, commissionRate);

            // Then
            assertThat(result).isEqualByComparingTo(new BigDecimal("126.57"));
        }

        @Test
        @DisplayName("should use default 20% VAT when vatRate is null")
        void shouldUseDefaultVatWhenNull() {
            // Given
            BigDecimal price = new BigDecimal("120.00");
            BigDecimal commissionRate = new BigDecimal("10");

            // When - VAT rate is null, should default to 20%
            BigDecimal result = calculator.calculateUnitEstimatedCommission(price, null, commissionRate);

            // Then
            // VAT base: 120 / 1.20 = 100
            // Commission: 100 * 0.10 = 10
            assertThat(result).isEqualByComparingTo(new BigDecimal("10.00"));
        }

        @Test
        @DisplayName("should return zero when price is null")
        void shouldReturnZeroWhenPriceNull() {
            // Given
            BigDecimal vatRate = new BigDecimal("20");
            BigDecimal commissionRate = new BigDecimal("15");

            // When
            BigDecimal result = calculator.calculateUnitEstimatedCommission(null, vatRate, commissionRate);

            // Then
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should return zero when commissionRate is null")
        void shouldReturnZeroWhenCommissionRateNull() {
            // Given
            BigDecimal price = new BigDecimal("100.00");
            BigDecimal vatRate = new BigDecimal("20");

            // When
            BigDecimal result = calculator.calculateUnitEstimatedCommission(price, vatRate, null);

            // Then
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should handle zero commission rate")
        void shouldHandleZeroCommissionRate() {
            // Given
            BigDecimal price = new BigDecimal("200.00");
            BigDecimal vatRate = new BigDecimal("18");
            BigDecimal commissionRate = BigDecimal.ZERO;

            // When
            BigDecimal result = calculator.calculateUnitEstimatedCommission(price, vatRate, commissionRate);

            // Then
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should calculate with different VAT rates correctly")
        void shouldCalculateWithDifferentVatRates() {
            // Given - VAT rate 8% (food category)
            BigDecimal price = new BigDecimal("108.00");
            BigDecimal vatRate = new BigDecimal("8");
            BigDecimal commissionRate = new BigDecimal("10");

            // When
            BigDecimal result = calculator.calculateUnitEstimatedCommission(price, vatRate, commissionRate);

            // Then
            // VAT base: 108 / 1.08 = 100
            // Commission: 100 * 0.10 = 10
            assertThat(result).isEqualByComparingTo(new BigDecimal("10.00"));
        }
    }

    @Nested
    @DisplayName("setCostInfo - Integration")
    class SetCostInfoIntegration {

        @Test
        @DisplayName("should set cost info from product when found in repository")
        void shouldSetCostInfoFromProduct() {
            // Given
            UUID storeId = UUID.randomUUID();
            String barcode = "TEST-BARCODE-123";

            CostAndStockInfo costInfo = CostAndStockInfo.builder()
                    .unitCost(75.50)
                    .quantity(50)
                    .costVatRate(20)
                    .stockDate(LocalDate.of(2024, 1, 15))
                    .usedQuantity(10)
                    .build();

            TrendyolProduct product = TrendyolProduct.builder()
                    .barcode(barcode)
                    .commissionRate(new BigDecimal("15.00"))
                    .costAndStockInfo(List.of(costInfo))
                    .build();

            when(productRepository.findByStoreIdAndBarcode(eq(storeId), eq(barcode)))
                    .thenReturn(Optional.of(product));

            OrderItem.OrderItemBuilder builder = OrderItem.builder()
                    .barcode(barcode);

            // When
            calculator.setCostInfo(builder, barcode, storeId,
                    LocalDate.of(2024, 6, 1).atStartOfDay());

            // Then
            OrderItem item = builder.build();
            assertThat(item.getCost()).isEqualByComparingTo(new BigDecimal("75.50"));
            assertThat(item.getCostVat()).isEqualTo(20);
            assertThat(item.getStockDate()).isEqualTo(LocalDate.of(2024, 1, 15));
            assertThat(item.getEstimatedCommissionRate()).isEqualByComparingTo(new BigDecimal("15.00"));
        }

        @Test
        @DisplayName("should not set cost info when barcode is null")
        void shouldNotSetCostInfoWhenBarcodeNull() {
            // Given
            UUID storeId = UUID.randomUUID();
            OrderItem.OrderItemBuilder builder = OrderItem.builder();

            // When
            calculator.setCostInfo(builder, null, storeId, LocalDate.now().atStartOfDay());

            // Then
            OrderItem item = builder.build();
            assertThat(item.getCost()).isNull();
        }

        @Test
        @DisplayName("should not set cost info when barcode is empty")
        void shouldNotSetCostInfoWhenBarcodeEmpty() {
            // Given
            UUID storeId = UUID.randomUUID();
            OrderItem.OrderItemBuilder builder = OrderItem.builder();

            // When
            calculator.setCostInfo(builder, "", storeId, LocalDate.now().atStartOfDay());

            // Then
            OrderItem item = builder.build();
            assertThat(item.getCost()).isNull();
        }

        @Test
        @DisplayName("should not set cost info when product not found")
        void shouldNotSetCostInfoWhenProductNotFound() {
            // Given
            UUID storeId = UUID.randomUUID();
            String barcode = "NON-EXISTENT";

            when(productRepository.findByStoreIdAndBarcode(any(), any()))
                    .thenReturn(Optional.empty());

            OrderItem.OrderItemBuilder builder = OrderItem.builder()
                    .barcode(barcode);

            // When
            calculator.setCostInfo(builder, barcode, storeId, LocalDate.now().atStartOfDay());

            // Then
            OrderItem item = builder.build();
            assertThat(item.getCost()).isNull();
            assertThat(item.getEstimatedCommissionRate()).isNull();
        }

        @Test
        @DisplayName("should use last known cost fallback when no available stock (all used)")
        void shouldUseLastKnownCostWhenNoAvailableStock() {
            // Given
            UUID storeId = UUID.randomUUID();
            String barcode = "TEST-BARCODE";

            // Stock is fully used
            CostAndStockInfo costInfo = CostAndStockInfo.builder()
                    .unitCost(100.0)
                    .quantity(10)
                    .costVatRate(20)
                    .stockDate(LocalDate.of(2024, 1, 1))
                    .usedQuantity(10) // All stock used
                    .build();

            TrendyolProduct product = TrendyolProduct.builder()
                    .barcode(barcode)
                    .commissionRate(new BigDecimal("15.00"))
                    .costAndStockInfo(List.of(costInfo))
                    .build();

            when(productRepository.findByStoreIdAndBarcode(eq(storeId), eq(barcode)))
                    .thenReturn(Optional.of(product));

            OrderItem.OrderItemBuilder builder = OrderItem.builder()
                    .barcode(barcode);

            // When
            calculator.setCostInfo(builder, barcode, storeId, LocalDate.of(2024, 6, 1).atStartOfDay());

            // Then - LAST_KNOWN_COST fallback should be used when FIFO stock is depleted
            OrderItem item = builder.build();
            assertThat(item.getCost()).isEqualByComparingTo(new BigDecimal("100.0"));
            assertThat(item.getCostSource()).isEqualTo("LAST_KNOWN");
            assertThat(item.getStockDate()).isEqualTo(LocalDate.of(2024, 1, 1));
            // Commission info should also be set
            assertThat(item.getEstimatedCommissionRate()).isEqualByComparingTo(new BigDecimal("15.00"));
        }

        @Test
        @DisplayName("should use FIFO - select oldest available stock first")
        void shouldUseFifoSelectOldestFirst() {
            // Given
            UUID storeId = UUID.randomUUID();
            String barcode = "TEST-BARCODE";

            // Older stock (partially used)
            CostAndStockInfo oldStock = CostAndStockInfo.builder()
                    .unitCost(80.0) // Cheaper older stock
                    .quantity(10)
                    .costVatRate(20)
                    .stockDate(LocalDate.of(2024, 1, 1))
                    .usedQuantity(5) // 5 remaining
                    .build();

            // Newer stock (unused)
            CostAndStockInfo newStock = CostAndStockInfo.builder()
                    .unitCost(100.0) // More expensive newer stock
                    .quantity(20)
                    .costVatRate(20)
                    .stockDate(LocalDate.of(2024, 3, 1))
                    .usedQuantity(0)
                    .build();

            TrendyolProduct product = TrendyolProduct.builder()
                    .barcode(barcode)
                    .commissionRate(new BigDecimal("15.00"))
                    .costAndStockInfo(List.of(newStock, oldStock)) // Order doesn't matter - should be sorted
                    .build();

            when(productRepository.findByStoreIdAndBarcode(eq(storeId), eq(barcode)))
                    .thenReturn(Optional.of(product));

            OrderItem.OrderItemBuilder builder = OrderItem.builder()
                    .barcode(barcode);

            // When
            calculator.setCostInfo(builder, barcode, storeId, LocalDate.of(2024, 6, 1).atStartOfDay());

            // Then - Should use older stock first (FIFO)
            OrderItem item = builder.build();
            assertThat(item.getCost()).isEqualByComparingTo(new BigDecimal("80.0"));
            assertThat(item.getStockDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        }
    }
}
