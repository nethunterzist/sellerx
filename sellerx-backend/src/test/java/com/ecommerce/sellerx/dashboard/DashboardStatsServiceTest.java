package com.ecommerce.sellerx.dashboard;

import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.expenses.ExpenseFrequency;
import com.ecommerce.sellerx.expenses.StoreExpense;
import com.ecommerce.sellerx.expenses.StoreExpenseRepository;
import com.ecommerce.sellerx.orders.OrderItem;
import com.ecommerce.sellerx.orders.TrendyolOrder;
import com.ecommerce.sellerx.orders.TrendyolOrderRepository;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import com.ecommerce.sellerx.returns.ReturnRecordRepository;
import com.ecommerce.sellerx.financial.TrendyolStoppageRepository;
import com.ecommerce.sellerx.financial.TrendyolDeductionInvoiceRepository;
import com.ecommerce.sellerx.financial.TrendyolCargoInvoiceRepository;
import com.ecommerce.sellerx.financial.TrendyolInvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DashboardStatsService.
 * Tests net profit calculation, profit margin, ROI, and various aggregation logic.
 */
@DisplayName("DashboardStatsService")
class DashboardStatsServiceTest extends BaseUnitTest {

    @Mock
    private TrendyolOrderRepository orderRepository;

    @Mock
    private StoreExpenseRepository storeExpenseRepository;

    @Mock
    private TrendyolProductRepository productRepository;

    @Mock
    private ReturnRecordRepository returnRecordRepository;

    @Mock
    private TrendyolStoppageRepository stoppageRepository;

    @Mock
    private TrendyolDeductionInvoiceRepository deductionInvoiceRepository;

    @Mock
    private TrendyolCargoInvoiceRepository cargoInvoiceRepository;

    @Mock
    private TrendyolInvoiceRepository invoiceRepository;

    private DashboardStatsService dashboardService;

    private UUID testStoreId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dashboardService = new DashboardStatsService(orderRepository, storeExpenseRepository, productRepository, returnRecordRepository, stoppageRepository, deductionInvoiceRepository, cargoInvoiceRepository, invoiceRepository);
        testStoreId = UUID.randomUUID();

        // Default mocks for platform fees calculation (DeductionInvoices)
        lenient().when(deductionInvoiceRepository.sumByTransactionType(any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        lenient().when(deductionInvoiceRepository.sumOtherDeductionFees(any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        lenient().when(stoppageRepository.sumStoppageOnly(any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        // Default mocks for invoiced deductions (TrendyolInvoice - kesilen faturalar)
        lenient().when(invoiceRepository.sumAmountByStoreIdAndCategory(any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
    }

    @Nested
    @DisplayName("getStatsForDateRange - Net Profit Calculation")
    class NetProfitCalculation {

        @Test
        @DisplayName("should calculate net profit correctly with all cost components")
        void shouldCalculateNetProfitWithAllCosts() {
            // Given
            LocalDate startDate = LocalDate.of(2024, 6, 1);
            LocalDate endDate = LocalDate.of(2024, 6, 30);

            // Create order with items
            OrderItem item = OrderItem.builder()
                    .barcode("TEST-123")
                    .productName("Test Product")
                    .quantity(2)
                    .price(new BigDecimal("200.00")) // Total price
                    .cost(new BigDecimal("50.00"))   // Unit cost
                    .costVat(20)
                    .vatBaseAmount(new BigDecimal("20")) // VAT rate 20%
                    .build();

            TrendyolOrder order = TrendyolOrder.builder()
                    .tyOrderNumber("TY-001")
                    .grossAmount(new BigDecimal("200.00"))
                    .totalDiscount(BigDecimal.ZERO)
                    .stoppage(new BigDecimal("10.00"))
                    .estimatedCommission(new BigDecimal("30.00"))
                    .transactionStatus("NOT_SETTLED")
                    .orderItems(List.of(item))
                    .build();

            when(orderRepository.findRevenueOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of(order));
            when(orderRepository.findReturnedOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of()); // No returns
            when(storeExpenseRepository.findByStoreIdOrderByDateDesc(any()))
                    .thenReturn(List.of()); // No expenses
            when(returnRecordRepository.sumTotalLossByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(null); // No return cost data
            when(productRepository.findByStoreIdAndBarcodeIn(any(), any()))
                    .thenReturn(List.of());
            // Mock stoppage from stoppages table (tax withholding)
            when(stoppageRepository.sumStoppageOnly(any(), any(), any()))
                    .thenReturn(new BigDecimal("10.00"));

            // When
            DashboardStatsDto result = dashboardService.getStatsForDateRange(testStoreId, startDate, endDate, "test");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("200.00"));
            // Total product cost = 50 * 2 = 100
            assertThat(result.getTotalProductCosts()).isEqualByComparingTo(new BigDecimal("100.00"));
            // Gross profit = 200 - 100 = 100
            assertThat(result.getGrossProfit()).isEqualByComparingTo(new BigDecimal("100.00"));
            // Stoppage (from stoppages table, not from order)
            assertThat(result.getTotalStoppage()).isEqualByComparingTo(new BigDecimal("10.00"));
        }

        @Test
        @DisplayName("should handle orders without costs correctly")
        void shouldHandleOrdersWithoutCosts() {
            // Given
            LocalDate startDate = LocalDate.of(2024, 6, 1);
            LocalDate endDate = LocalDate.of(2024, 6, 30);

            // Order item without cost
            OrderItem item = OrderItem.builder()
                    .barcode("TEST-123")
                    .productName("Test Product")
                    .quantity(1)
                    .price(new BigDecimal("150.00"))
                    .cost(null) // No cost
                    .build();

            TrendyolOrder order = TrendyolOrder.builder()
                    .tyOrderNumber("TY-001")
                    .grossAmount(new BigDecimal("150.00"))
                    .totalDiscount(BigDecimal.ZERO)
                    .stoppage(BigDecimal.ZERO)
                    .estimatedCommission(BigDecimal.ZERO)
                    .transactionStatus("NOT_SETTLED")
                    .orderItems(List.of(item))
                    .build();

            when(orderRepository.findRevenueOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of(order));
            when(orderRepository.findReturnedOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of());
            when(storeExpenseRepository.findByStoreIdOrderByDateDesc(any()))
                    .thenReturn(List.of());
            when(returnRecordRepository.sumTotalLossByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(null);
            when(productRepository.findByStoreIdAndBarcodeIn(any(), any()))
                    .thenReturn(List.of());

            // When
            DashboardStatsDto result = dashboardService.getStatsForDateRange(testStoreId, startDate, endDate, "test");

            // Then
            assertThat(result.getTotalProductCosts()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getItemsWithoutCost()).isEqualTo(1);
            // Gross profit = revenue - 0 = revenue
            assertThat(result.getGrossProfit()).isEqualByComparingTo(new BigDecimal("150.00"));
        }

        @Test
        @DisplayName("should estimate return cost from orders when no return records exist")
        void shouldUseFallbackReturnCost() {
            // Given
            LocalDate startDate = LocalDate.of(2024, 6, 1);
            LocalDate endDate = LocalDate.of(2024, 6, 30);

            // Revenue order
            OrderItem revenueItem = OrderItem.builder()
                    .barcode("TEST-123")
                    .quantity(1)
                    .price(new BigDecimal("100.00"))
                    .cost(new BigDecimal("40.00"))
                    .build();

            TrendyolOrder revenueOrder = TrendyolOrder.builder()
                    .tyOrderNumber("TY-001")
                    .grossAmount(new BigDecimal("100.00"))
                    .totalDiscount(BigDecimal.ZERO)
                    .stoppage(BigDecimal.ZERO)
                    .orderItems(List.of(revenueItem))
                    .build();

            // Return order with cost and shipping data for fallback estimation
            OrderItem returnItem = OrderItem.builder()
                    .barcode("TEST-456")
                    .quantity(1)
                    .price(new BigDecimal("50.00"))
                    .cost(new BigDecimal("20.00"))
                    .build();

            TrendyolOrder returnOrder = TrendyolOrder.builder()
                    .tyOrderNumber("TY-002")
                    .grossAmount(new BigDecimal("50.00"))
                    .estimatedShippingCost(new BigDecimal("15.00"))
                    .estimatedCommission(new BigDecimal("5.00"))
                    .orderItems(List.of(returnItem))
                    .build();

            when(orderRepository.findRevenueOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of(revenueOrder));
            when(orderRepository.findReturnedOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of(returnOrder)); // 1 return
            when(storeExpenseRepository.findByStoreIdOrderByDateDesc(any()))
                    .thenReturn(List.of());
            when(returnRecordRepository.sumTotalLossByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(null); // No return record data
            when(productRepository.findByStoreIdAndBarcodeIn(any(), any()))
                    .thenReturn(List.of());

            // When
            DashboardStatsDto result = dashboardService.getStatsForDateRange(testStoreId, startDate, endDate, "test");

            // Then - fallback: productCost(20) + outboundShipping(15) + returnShipping(15, estimated from outbound) + commission(5) = 55
            assertThat(result.getReturnCost()).isEqualByComparingTo(new BigDecimal("55.00"));
            assertThat(result.getReturnCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should return zero return cost when no return records and no returned orders")
        void shouldReturnZeroWhenNoReturnsAtAll() {
            // Given
            LocalDate startDate = LocalDate.of(2024, 6, 1);
            LocalDate endDate = LocalDate.of(2024, 6, 30);

            OrderItem item = OrderItem.builder()
                    .barcode("TEST-123")
                    .quantity(1)
                    .price(new BigDecimal("100.00"))
                    .cost(new BigDecimal("40.00"))
                    .build();

            TrendyolOrder order = TrendyolOrder.builder()
                    .tyOrderNumber("TY-001")
                    .grossAmount(new BigDecimal("100.00"))
                    .totalDiscount(BigDecimal.ZERO)
                    .stoppage(BigDecimal.ZERO)
                    .orderItems(List.of(item))
                    .build();

            when(orderRepository.findRevenueOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of(order));
            when(orderRepository.findReturnedOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of()); // No returns
            when(storeExpenseRepository.findByStoreIdOrderByDateDesc(any()))
                    .thenReturn(List.of());
            when(returnRecordRepository.sumTotalLossByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(null);
            when(productRepository.findByStoreIdAndBarcodeIn(any(), any()))
                    .thenReturn(List.of());

            // When
            DashboardStatsDto result = dashboardService.getStatsForDateRange(testStoreId, startDate, endDate, "test");

            // Then - no return records and no returned orders → zero
            assertThat(result.getReturnCost()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getReturnCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("should use actual return cost when return records exist")
        void shouldUseActualReturnCost() {
            // Given
            LocalDate startDate = LocalDate.of(2024, 6, 1);
            LocalDate endDate = LocalDate.of(2024, 6, 30);

            OrderItem item = OrderItem.builder()
                    .barcode("TEST-123")
                    .quantity(1)
                    .price(new BigDecimal("100.00"))
                    .cost(new BigDecimal("40.00"))
                    .build();

            TrendyolOrder order = TrendyolOrder.builder()
                    .tyOrderNumber("TY-001")
                    .grossAmount(new BigDecimal("100.00"))
                    .totalDiscount(BigDecimal.ZERO)
                    .orderItems(List.of(item))
                    .build();

            TrendyolOrder returnOrder = TrendyolOrder.builder()
                    .tyOrderNumber("TY-002")
                    .orderItems(List.of())
                    .build();

            when(orderRepository.findRevenueOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of(order));
            when(orderRepository.findReturnedOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of(returnOrder));
            when(storeExpenseRepository.findByStoreIdOrderByDateDesc(any()))
                    .thenReturn(List.of());
            // Return actual return cost from ReturnRecord
            when(returnRecordRepository.sumTotalLossByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(new BigDecimal("85.50"));
            when(productRepository.findByStoreIdAndBarcodeIn(any(), any()))
                    .thenReturn(List.of());

            // When
            DashboardStatsDto result = dashboardService.getStatsForDateRange(testStoreId, startDate, endDate, "test");

            // Then - Should use actual return cost from ReturnRecord
            assertThat(result.getReturnCost()).isEqualByComparingTo(new BigDecimal("85.50"));
        }
    }

    @Nested
    @DisplayName("Profit Margin Calculation")
    class ProfitMarginCalculation {

        @Test
        @DisplayName("should calculate profit margin correctly")
        void shouldCalculateProfitMarginCorrectly() {
            // Given
            LocalDate startDate = LocalDate.of(2024, 6, 1);
            LocalDate endDate = LocalDate.of(2024, 6, 30);

            // Revenue = 1000, Cost = 400, Gross Profit = 600
            // Profit Margin = (600 / 1000) * 100 = 60%
            OrderItem item = OrderItem.builder()
                    .barcode("TEST-123")
                    .quantity(10)
                    .price(new BigDecimal("1000.00"))
                    .cost(new BigDecimal("40.00")) // 40 * 10 = 400 total cost
                    .build();

            TrendyolOrder order = TrendyolOrder.builder()
                    .tyOrderNumber("TY-001")
                    .grossAmount(new BigDecimal("1000.00"))
                    .totalDiscount(BigDecimal.ZERO)
                    .stoppage(BigDecimal.ZERO)
                    .orderItems(List.of(item))
                    .build();

            when(orderRepository.findRevenueOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of(order));
            when(orderRepository.findReturnedOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of());
            when(storeExpenseRepository.findByStoreIdOrderByDateDesc(any()))
                    .thenReturn(List.of());
            when(returnRecordRepository.sumTotalLossByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(null);
            when(productRepository.findByStoreIdAndBarcodeIn(any(), any()))
                    .thenReturn(List.of());

            // When
            DashboardStatsDto result = dashboardService.getStatsForDateRange(testStoreId, startDate, endDate, "test");

            // Then
            // Gross Profit = 1000 - 400 = 600
            // Profit Margin = (600 / 1000) * 100 = 60%
            assertThat(result.getGrossProfit()).isEqualByComparingTo(new BigDecimal("600.00"));
            assertThat(result.getProfitMargin()).isEqualByComparingTo(new BigDecimal("60.00"));
        }

        @Test
        @DisplayName("should return zero profit margin when revenue is zero")
        void shouldReturnZeroMarginWhenRevenueIsZero() {
            // Given
            LocalDate startDate = LocalDate.of(2024, 6, 1);
            LocalDate endDate = LocalDate.of(2024, 6, 30);

            when(orderRepository.findRevenueOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of()); // No orders
            when(orderRepository.findReturnedOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of());
            when(storeExpenseRepository.findByStoreIdOrderByDateDesc(any()))
                    .thenReturn(List.of());
            when(returnRecordRepository.sumTotalLossByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(null);

            // When
            DashboardStatsDto result = dashboardService.getStatsForDateRange(testStoreId, startDate, endDate, "test");

            // Then
            assertThat(result.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getProfitMargin()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("VAT Difference Calculation")
    class VatDifferenceCalculation {

        @Test
        @DisplayName("should calculate VAT difference correctly")
        void shouldCalculateVatDifferenceCorrectly() {
            // Given
            LocalDate startDate = LocalDate.of(2024, 6, 1);
            LocalDate endDate = LocalDate.of(2024, 6, 30);

            // Sale price 120 TL (20% VAT included) → VAT base = 100, Sales VAT = 20
            // Cost 60 TL (20% VAT included) → Cost base = 50, Cost VAT = 10
            // VAT Difference = 20 - 10 = 10 TL
            OrderItem item = OrderItem.builder()
                    .barcode("TEST-123")
                    .quantity(1)
                    .price(new BigDecimal("120.00"))
                    .vatBaseAmount(new BigDecimal("20")) // VAT rate 20%
                    .cost(new BigDecimal("60.00"))
                    .costVat(20) // 20% VAT rate
                    .build();

            TrendyolOrder order = TrendyolOrder.builder()
                    .tyOrderNumber("TY-001")
                    .grossAmount(new BigDecimal("120.00"))
                    .totalDiscount(BigDecimal.ZERO)
                    .orderItems(List.of(item))
                    .build();

            when(orderRepository.findRevenueOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of(order));
            when(orderRepository.findReturnedOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of());
            when(storeExpenseRepository.findByStoreIdOrderByDateDesc(any()))
                    .thenReturn(List.of());
            when(returnRecordRepository.sumTotalLossByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(null);
            when(productRepository.findByStoreIdAndBarcodeIn(any(), any()))
                    .thenReturn(List.of());

            // When
            DashboardStatsDto result = dashboardService.getStatsForDateRange(testStoreId, startDate, endDate, "test");

            // Then
            // Sales VAT = (120 / 1.20) * 0.20 = 100 * 0.20 = 20
            // Cost VAT = (60 / 1.20) * 0.20 = 50 * 0.20 = 10
            // VAT Difference = 20 - 10 = 10
            assertThat(result.getVatDifference()).isEqualByComparingTo(new BigDecimal("10.00"));
        }

        @Test
        @DisplayName("should skip VAT calculation when cost is null")
        void shouldSkipVatWhenCostIsNull() {
            // Given
            LocalDate startDate = LocalDate.of(2024, 6, 1);
            LocalDate endDate = LocalDate.of(2024, 6, 30);

            OrderItem item = OrderItem.builder()
                    .barcode("TEST-123")
                    .quantity(1)
                    .price(new BigDecimal("120.00"))
                    .vatBaseAmount(new BigDecimal("20"))
                    .cost(null) // No cost
                    .build();

            TrendyolOrder order = TrendyolOrder.builder()
                    .tyOrderNumber("TY-001")
                    .grossAmount(new BigDecimal("120.00"))
                    .totalDiscount(BigDecimal.ZERO)
                    .orderItems(List.of(item))
                    .build();

            when(orderRepository.findRevenueOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of(order));
            when(orderRepository.findReturnedOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of());
            when(storeExpenseRepository.findByStoreIdOrderByDateDesc(any()))
                    .thenReturn(List.of());
            when(returnRecordRepository.sumTotalLossByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(null);
            when(productRepository.findByStoreIdAndBarcodeIn(any(), any()))
                    .thenReturn(List.of());

            // When
            DashboardStatsDto result = dashboardService.getStatsForDateRange(testStoreId, startDate, endDate, "test");

            // Then - VAT difference should be zero since cost is null
            assertThat(result.getVatDifference()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Stoppage Calculation")
    class StoppageCalculation {

        @Test
        @DisplayName("should sum stoppage from all orders")
        void shouldSumStoppageFromAllOrders() {
            // Given
            LocalDate startDate = LocalDate.of(2024, 6, 1);
            LocalDate endDate = LocalDate.of(2024, 6, 30);

            TrendyolOrder order1 = TrendyolOrder.builder()
                    .tyOrderNumber("TY-001")
                    .grossAmount(new BigDecimal("100.00"))
                    .totalDiscount(BigDecimal.ZERO)
                    .stoppage(new BigDecimal("5.00"))
                    .orderItems(List.of(createSimpleOrderItem()))
                    .build();

            TrendyolOrder order2 = TrendyolOrder.builder()
                    .tyOrderNumber("TY-002")
                    .grossAmount(new BigDecimal("200.00"))
                    .totalDiscount(BigDecimal.ZERO)
                    .stoppage(new BigDecimal("10.00"))
                    .orderItems(List.of(createSimpleOrderItem()))
                    .build();

            TrendyolOrder order3 = TrendyolOrder.builder()
                    .tyOrderNumber("TY-003")
                    .grossAmount(new BigDecimal("150.00"))
                    .totalDiscount(BigDecimal.ZERO)
                    .stoppage(null) // Null stoppage
                    .orderItems(List.of(createSimpleOrderItem()))
                    .build();

            when(orderRepository.findRevenueOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of(order1, order2, order3));
            when(orderRepository.findReturnedOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of());
            when(storeExpenseRepository.findByStoreIdOrderByDateDesc(any()))
                    .thenReturn(List.of());
            when(returnRecordRepository.sumTotalLossByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(null);
            when(productRepository.findByStoreIdAndBarcodeIn(any(), any()))
                    .thenReturn(List.of());
            // Mock stoppage from stoppages table (tax withholding)
            when(stoppageRepository.sumStoppageOnly(any(), any(), any()))
                    .thenReturn(new BigDecimal("15.00"));

            // When
            DashboardStatsDto result = dashboardService.getStatsForDateRange(testStoreId, startDate, endDate, "test");

            // Then - Total stoppage from stoppages table
            assertThat(result.getTotalStoppage()).isEqualByComparingTo(new BigDecimal("15.00"));
        }
    }

    @Nested
    @DisplayName("Commission Calculation")
    class CommissionCalculation {

        @Test
        @DisplayName("should use invoiced commission from TrendyolDeductionInvoice")
        void shouldUseInvoicedCommission() {
            // Given - Commission is now calculated from TrendyolDeductionInvoice (invoice-based)
            // This matches the Invoices page calculation for consistency
            LocalDate startDate = LocalDate.of(2024, 6, 1);
            LocalDate endDate = LocalDate.of(2024, 6, 30);

            OrderItem item = OrderItem.builder()
                    .barcode("TEST-123")
                    .quantity(1)
                    .price(new BigDecimal("100.00"))
                    .build();

            TrendyolOrder order = TrendyolOrder.builder()
                    .tyOrderNumber("TY-001")
                    .grossAmount(new BigDecimal("100.00"))
                    .totalDiscount(BigDecimal.ZERO)
                    .transactionStatus("NOT_SETTLED")
                    .orderItems(List.of(item))
                    .build();

            when(orderRepository.findRevenueOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of(order));
            when(orderRepository.findReturnedOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of());
            when(storeExpenseRepository.findByStoreIdOrderByDateDesc(any()))
                    .thenReturn(List.of());
            when(returnRecordRepository.sumTotalLossByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(null);
            when(productRepository.findByStoreIdAndBarcodeIn(any(), any()))
                    .thenReturn(List.of());
            // Mock invoice-based commission (matches Invoices page)
            when(deductionInvoiceRepository.sumCommissionFeesByStoreIdAndDateRange(any(), any(), any()))
                    .thenReturn(new BigDecimal("15.00"));

            // When
            DashboardStatsDto result = dashboardService.getStatsForDateRange(testStoreId, startDate, endDate, "test");

            // Then - Commission comes from invoiced amount (TrendyolDeductionInvoice)
            assertThat(result.getTotalEstimatedCommission()).isEqualByComparingTo(new BigDecimal("15.00"));
        }

        @Test
        @DisplayName("should return zero commission when no invoices exist")
        void shouldReturnZeroCommissionWhenNoInvoices() {
            // Given - No commission invoices in the date range
            LocalDate startDate = LocalDate.of(2024, 6, 1);
            LocalDate endDate = LocalDate.of(2024, 6, 30);

            OrderItem item = OrderItem.builder()
                    .barcode("TEST-123")
                    .quantity(1)
                    .price(new BigDecimal("100.00"))
                    .build();

            TrendyolOrder order = TrendyolOrder.builder()
                    .tyOrderNumber("TY-001")
                    .grossAmount(new BigDecimal("100.00"))
                    .totalDiscount(BigDecimal.ZERO)
                    .transactionStatus("NOT_SETTLED")
                    .orderItems(List.of(item))
                    .build();

            when(orderRepository.findRevenueOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of(order));
            when(orderRepository.findReturnedOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of());
            when(storeExpenseRepository.findByStoreIdOrderByDateDesc(any()))
                    .thenReturn(List.of());
            when(returnRecordRepository.sumTotalLossByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(null);
            when(productRepository.findByStoreIdAndBarcodeIn(any(), any()))
                    .thenReturn(List.of());
            // No invoiced commission
            when(deductionInvoiceRepository.sumCommissionFeesByStoreIdAndDateRange(any(), any(), any()))
                    .thenReturn(null);

            // When
            DashboardStatsDto result = dashboardService.getStatsForDateRange(testStoreId, startDate, endDate, "test");

            // Then - Zero commission when no invoices
            assertThat(result.getTotalEstimatedCommission()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Expense Calculation")
    class ExpenseCalculation {

        @Test
        @DisplayName("should include one-time expense in period")
        void shouldIncludeOneTimeExpenseInPeriod() {
            // Given
            LocalDate startDate = LocalDate.of(2024, 6, 1);
            LocalDate endDate = LocalDate.of(2024, 6, 30);

            StoreExpense expense = StoreExpense.builder()
                    .name("Office Supplies")
                    .amount(new BigDecimal("250.00"))
                    .frequency(ExpenseFrequency.ONE_TIME)
                    .date(LocalDateTime.of(2024, 6, 15, 10, 0))
                    .build();

            when(orderRepository.findRevenueOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of());
            when(orderRepository.findReturnedOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of());
            when(storeExpenseRepository.findByStoreIdOrderByDateDesc(any()))
                    .thenReturn(List.of(expense));
            when(returnRecordRepository.sumTotalLossByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(null);

            // When
            DashboardStatsDto result = dashboardService.getStatsForDateRange(testStoreId, startDate, endDate, "test");

            // Then
            assertThat(result.getTotalExpenseNumber()).isEqualTo(1);
            assertThat(result.getTotalExpenseAmount()).isEqualByComparingTo(new BigDecimal("250.00"));
        }

        @Test
        @DisplayName("should exclude one-time expense outside period")
        void shouldExcludeOneTimeExpenseOutsidePeriod() {
            // Given
            LocalDate startDate = LocalDate.of(2024, 6, 1);
            LocalDate endDate = LocalDate.of(2024, 6, 30);

            // Expense is in May, outside the June period
            StoreExpense expense = StoreExpense.builder()
                    .name("Office Supplies")
                    .amount(new BigDecimal("250.00"))
                    .frequency(ExpenseFrequency.ONE_TIME)
                    .date(LocalDateTime.of(2024, 5, 15, 10, 0))
                    .build();

            when(orderRepository.findRevenueOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of());
            when(orderRepository.findReturnedOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of());
            when(storeExpenseRepository.findByStoreIdOrderByDateDesc(any()))
                    .thenReturn(List.of(expense));
            when(returnRecordRepository.sumTotalLossByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(null);

            // When
            DashboardStatsDto result = dashboardService.getStatsForDateRange(testStoreId, startDate, endDate, "test");

            // Then
            assertThat(result.getTotalExpenseNumber()).isEqualTo(0);
            assertThat(result.getTotalExpenseAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should calculate monthly expense for full month")
        void shouldCalculateMonthlyExpenseForFullMonth() {
            // Given
            LocalDate startDate = LocalDate.of(2024, 6, 1);
            LocalDate endDate = LocalDate.of(2024, 6, 30);

            // Monthly expense created before the period
            StoreExpense expense = StoreExpense.builder()
                    .name("Rent")
                    .amount(new BigDecimal("1000.00"))
                    .frequency(ExpenseFrequency.MONTHLY)
                    .date(LocalDateTime.of(2024, 1, 1, 10, 0)) // Created in January
                    .build();

            when(orderRepository.findRevenueOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of());
            when(orderRepository.findReturnedOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of());
            when(storeExpenseRepository.findByStoreIdOrderByDateDesc(any()))
                    .thenReturn(List.of(expense));
            when(returnRecordRepository.sumTotalLossByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(null);

            // When
            DashboardStatsDto result = dashboardService.getStatsForDateRange(testStoreId, startDate, endDate, "test");

            // Then - Should include 1 monthly expense for June
            assertThat(result.getTotalExpenseNumber()).isEqualTo(1);
            assertThat(result.getTotalExpenseAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        }
    }

    @Nested
    @DisplayName("Kesilen Faturalar (Invoiced Deductions) Calculation Tests")
    class InvoicedDeductionsCalculationTests {

        @Test
        @DisplayName("Tüm kategoriler toplandığında doğru invoicedDeductions hesaplanmalı")
        void shouldCalculateInvoicedDeductionsCorrectly() {
            // Given
            LocalDate startDate = LocalDate.of(2024, 6, 1);
            LocalDate endDate = LocalDate.of(2024, 6, 30);

            // Mock order repository to return empty list (we're only testing invoiced deductions)
            when(orderRepository.findRevenueOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of());
            when(orderRepository.findReturnedOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of());
            when(storeExpenseRepository.findByStoreIdOrderByDateDesc(any()))
                    .thenReturn(List.of());
            when(returnRecordRepository.sumTotalLossByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(null);

            // Mock deduction invoice repository with specific values for each category
            // Formula: REKLAM + CEZA + ULUSLARARASI + DIGER - IADE
            when(deductionInvoiceRepository.sumInvoicedAdvertisingFees(any(), any(), any()))
                    .thenReturn(new BigDecimal("250.00"));
            when(deductionInvoiceRepository.sumInvoicedPenaltyFees(any(), any(), any()))
                    .thenReturn(new BigDecimal("50.00"));
            when(deductionInvoiceRepository.sumInvoicedInternationalFees(any(), any(), any()))
                    .thenReturn(new BigDecimal("75.00"));
            when(deductionInvoiceRepository.sumInvoicedOtherFees(any(), any(), any()))
                    .thenReturn(new BigDecimal("30.00"));
            when(deductionInvoiceRepository.sumInvoicedRefunds(any(), any(), any()))
                    .thenReturn(new BigDecimal("25.00"));

            // When
            DashboardStatsDto result = dashboardService.getStatsForDateRange(testStoreId, startDate, endDate, "test");

            // Then: 250 + 50 + 75 + 30 - 25 = 380
            assertThat(result.getInvoicedDeductions())
                    .isEqualByComparingTo(new BigDecimal("380.00"));
            assertThat(result.getInvoicedAdvertisingFees())
                    .isEqualByComparingTo(new BigDecimal("250.00"));
            assertThat(result.getInvoicedPenaltyFees())
                    .isEqualByComparingTo(new BigDecimal("50.00"));
            assertThat(result.getInvoicedInternationalFees())
                    .isEqualByComparingTo(new BigDecimal("75.00"));
            assertThat(result.getInvoicedOtherFees())
                    .isEqualByComparingTo(new BigDecimal("30.00"));
            assertThat(result.getInvoicedRefunds())
                    .isEqualByComparingTo(new BigDecimal("25.00"));
        }

        @Test
        @DisplayName("Fatura yoksa invoicedDeductions sıfır olmalı")
        void shouldReturnZeroWhenNoInvoices() {
            // Given
            LocalDate startDate = LocalDate.of(2024, 6, 1);
            LocalDate endDate = LocalDate.of(2024, 6, 30);

            // Mock order repository to return empty list
            when(orderRepository.findRevenueOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of());
            when(orderRepository.findReturnedOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of());
            when(storeExpenseRepository.findByStoreIdOrderByDateDesc(any()))
                    .thenReturn(List.of());
            when(returnRecordRepository.sumTotalLossByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(null);

            // All invoice categories return null (no invoices)
            lenient().when(invoiceRepository.sumAmountByStoreIdAndCategory(any(), any(), any(), any()))
                    .thenReturn(null);

            // When
            DashboardStatsDto result = dashboardService.getStatsForDateRange(testStoreId, startDate, endDate, "test");

            // Then
            assertThat(result.getInvoicedDeductions())
                    .isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getInvoicedAdvertisingFees())
                    .isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getInvoicedPenaltyFees())
                    .isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getInvoicedInternationalFees())
                    .isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getInvoicedOtherFees())
                    .isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getInvoicedRefunds())
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Sadece iade varsa invoicedDeductions negatif olmalı")
        void shouldReturnNegativeWhenOnlyRefunds() {
            // Given
            LocalDate startDate = LocalDate.of(2024, 6, 1);
            LocalDate endDate = LocalDate.of(2024, 6, 30);

            // Mock order repository to return empty list
            when(orderRepository.findRevenueOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of());
            when(orderRepository.findReturnedOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of());
            when(storeExpenseRepository.findByStoreIdOrderByDateDesc(any()))
                    .thenReturn(List.of());
            when(returnRecordRepository.sumTotalLossByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(null);

            // Only refunds have value, others are zero (from setUp defaults)
            when(deductionInvoiceRepository.sumInvoicedRefunds(any(), any(), any()))
                    .thenReturn(new BigDecimal("150.00"));

            // When
            DashboardStatsDto result = dashboardService.getStatsForDateRange(testStoreId, startDate, endDate, "test");

            // Then: 0 + 0 + 0 + 0 - 150 = -150
            assertThat(result.getInvoicedDeductions())
                    .isEqualByComparingTo(new BigDecimal("-150.00"));
            assertThat(result.getInvoicedRefunds())
                    .isEqualByComparingTo(new BigDecimal("150.00"));
        }
    }

    @Nested
    @DisplayName("Total Products Sold")
    class TotalProductsSold {

        @Test
        @DisplayName("should sum quantities from all order items")
        void shouldSumQuantitiesFromAllOrderItems() {
            // Given
            LocalDate startDate = LocalDate.of(2024, 6, 1);
            LocalDate endDate = LocalDate.of(2024, 6, 30);

            OrderItem item1 = OrderItem.builder()
                    .barcode("TEST-1")
                    .quantity(3)
                    .price(new BigDecimal("30.00"))
                    .build();

            OrderItem item2 = OrderItem.builder()
                    .barcode("TEST-2")
                    .quantity(5)
                    .price(new BigDecimal("50.00"))
                    .build();

            TrendyolOrder order1 = TrendyolOrder.builder()
                    .tyOrderNumber("TY-001")
                    .grossAmount(new BigDecimal("30.00"))
                    .totalDiscount(BigDecimal.ZERO)
                    .orderItems(List.of(item1))
                    .build();

            TrendyolOrder order2 = TrendyolOrder.builder()
                    .tyOrderNumber("TY-002")
                    .grossAmount(new BigDecimal("50.00"))
                    .totalDiscount(BigDecimal.ZERO)
                    .orderItems(List.of(item2))
                    .build();

            when(orderRepository.findRevenueOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of(order1, order2));
            when(orderRepository.findReturnedOrdersByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(List.of());
            when(storeExpenseRepository.findByStoreIdOrderByDateDesc(any()))
                    .thenReturn(List.of());
            when(returnRecordRepository.sumTotalLossByStoreAndDateRange(any(), any(), any()))
                    .thenReturn(null);
            when(productRepository.findByStoreIdAndBarcodeIn(any(), any()))
                    .thenReturn(List.of());

            // When
            DashboardStatsDto result = dashboardService.getStatsForDateRange(testStoreId, startDate, endDate, "test");

            // Then
            assertThat(result.getTotalOrders()).isEqualTo(2);
            assertThat(result.getTotalProductsSold()).isEqualTo(8); // 3 + 5
        }
    }

    // Helper method to create a simple order item
    private OrderItem createSimpleOrderItem() {
        return OrderItem.builder()
                .barcode("TEST-123")
                .quantity(1)
                .price(new BigDecimal("100.00"))
                .build();
    }
}
