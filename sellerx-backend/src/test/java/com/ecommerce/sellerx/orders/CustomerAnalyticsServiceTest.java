package com.ecommerce.sellerx.orders;

import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.common.TestDataBuilder;
import com.ecommerce.sellerx.orders.dto.*;
import com.ecommerce.sellerx.orders.dto.CustomerAnalyticsProjections.*;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.users.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("CustomerAnalyticsService")
class CustomerAnalyticsServiceTest extends BaseUnitTest {

    @Mock
    private TrendyolOrderRepository orderRepository;

    @Mock
    private TrendyolOrderService orderService;

    @Mock
    private TrendyolProductRepository productRepository;

    @Mock
    private EntityManager entityManager;

    private CustomerAnalyticsService service;

    private User testUser;
    private Store testStore;
    private UUID storeId;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetSequence();
        service = new CustomerAnalyticsService(orderRepository, orderService, productRepository, entityManager);

        testUser = TestDataBuilder.user().build();
        testStore = TestDataBuilder.completedStore(testUser).build();
        storeId = testStore.getId();
    }

    @Nested
    @DisplayName("getAnalytics")
    class GetAnalytics {

        @Test
        @DisplayName("should return complete analytics summary")
        void shouldReturnCompleteAnalyticsSummary() {
            // Given - Create mocks BEFORE when() calls to avoid nested stubbing
            SummaryWithOrderCountProjection summaryProjection = mockSummaryWithOrderCountProjection(
                    100, 30, new BigDecimal("50000.00"), new BigDecimal("20000.00"), 2.5, 250L, 3.0, 1.2
            );

            SegmentProjection seg1 = mockSegmentProjection("1", 70, new BigDecimal("15000.00"));
            SegmentProjection seg2 = mockSegmentProjection("2-3", 20, new BigDecimal("18000.00"));
            SegmentProjection seg3 = mockSegmentProjection("4-6", 10, new BigDecimal("17000.00"));

            CityRepeatProjection city1 = mockCityRepeatProjection("Istanbul", 40, 15, new BigDecimal("20000.00"));
            CityRepeatProjection city2 = mockCityRepeatProjection("Ankara", 25, 8, new BigDecimal("12000.00"));

            MonthlyTrendProjection trend1 = mockMonthlyTrendProjection("2025-01", 10, 5, new BigDecimal("5000.00"), new BigDecimal("3000.00"));

            when(orderRepository.getCustomerAnalyticsSummaryWithOrderCount(storeId)).thenReturn(summaryProjection);
            when(orderRepository.getAvgRepeatIntervalDays(storeId)).thenReturn(45.0);
            when(orderRepository.getCustomerSegmentation(storeId)).thenReturn(List.of(seg1, seg2, seg3));
            when(orderRepository.getCityRepeatAnalysis(storeId)).thenReturn(List.of(city1, city2));
            when(orderRepository.getMonthlyNewVsRepeatTrend(storeId)).thenReturn(List.of(trend1));

            // When
            CustomerAnalyticsResponse result = service.getAnalytics(storeId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getSummary().getTotalCustomers()).isEqualTo(100);
            assertThat(result.getSummary().getRepeatCustomers()).isEqualTo(30);
            assertThat(result.getSummary().getRepeatRate()).isEqualTo(30.0);
            assertThat(result.getSummary().getAvgOrdersPerCustomer()).isEqualTo(2.5);
            assertThat(result.getSummary().getAvgRepeatIntervalDays()).isEqualTo(45.0);
            assertThat(result.getSummary().getRepeatRevenueShare()).isEqualTo(40.0);

            assertThat(result.getSegmentation()).hasSize(3);
            assertThat(result.getCityAnalysis()).hasSize(2);
            assertThat(result.getMonthlyTrend()).hasSize(1);
        }

        @Test
        @DisplayName("should handle empty data gracefully")
        void shouldHandleEmptyDataGracefully() {
            // Given
            SummaryWithOrderCountProjection summaryProjection = mockSummaryWithOrderCountProjection(
                    0, 0, BigDecimal.ZERO, BigDecimal.ZERO, 0.0, 0L, 0.0, 0.0
            );
            when(orderRepository.getCustomerAnalyticsSummaryWithOrderCount(storeId)).thenReturn(summaryProjection);
            when(orderRepository.getAvgRepeatIntervalDays(storeId)).thenReturn(null);
            when(orderRepository.getCustomerSegmentation(storeId)).thenReturn(Collections.emptyList());
            when(orderRepository.getCityRepeatAnalysis(storeId)).thenReturn(Collections.emptyList());
            when(orderRepository.getMonthlyNewVsRepeatTrend(storeId)).thenReturn(Collections.emptyList());

            // When
            CustomerAnalyticsResponse result = service.getAnalytics(storeId);

            // Then
            assertThat(result.getSummary().getTotalCustomers()).isZero();
            assertThat(result.getSummary().getRepeatRate()).isZero();
            assertThat(result.getSummary().getAvgRepeatIntervalDays()).isZero();
            assertThat(result.getSummary().getRepeatRevenueShare()).isZero();
            assertThat(result.getSegmentation()).isEmpty();
            assertThat(result.getCityAnalysis()).isEmpty();
            assertThat(result.getMonthlyTrend()).isEmpty();
        }

        @Test
        @DisplayName("should handle null values in projection")
        void shouldHandleNullValuesInProjection() {
            // Given
            SummaryWithOrderCountProjection summaryProjection = mockSummaryWithOrderCountProjection(
                    null, null, null, null, null, null, null, null
            );
            when(orderRepository.getCustomerAnalyticsSummaryWithOrderCount(storeId)).thenReturn(summaryProjection);
            when(orderRepository.getAvgRepeatIntervalDays(storeId)).thenReturn(null);
            when(orderRepository.getCustomerSegmentation(storeId)).thenReturn(Collections.emptyList());
            when(orderRepository.getCityRepeatAnalysis(storeId)).thenReturn(Collections.emptyList());
            when(orderRepository.getMonthlyNewVsRepeatTrend(storeId)).thenReturn(Collections.emptyList());

            // When
            CustomerAnalyticsResponse result = service.getAnalytics(storeId);

            // Then
            assertThat(result.getSummary().getTotalCustomers()).isZero();
            assertThat(result.getSummary().getRepeatCustomers()).isZero();
            assertThat(result.getSummary().getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("getCustomerList")
    @Disabled("Requires EntityManager - moved to integration tests")
    class GetCustomerList {

        @Test
        @DisplayName("should return paginated customer list with RFM scores")
        void shouldReturnPaginatedCustomerListWithRfmScores() {
            // Given
            LocalDateTime recentOrder = LocalDateTime.now().minusDays(5);
            CustomerSummaryProjection customer = mockCustomerSummaryProjection(
                    1001L, "John Doe", "Istanbul", 5, new BigDecimal("2500.00"),
                    LocalDateTime.now().minusMonths(6), recentOrder
            );
            when(orderRepository.findCustomerSummaries(eq(storeId), eq(20), eq(0)))
                    .thenReturn(List.of(customer));
            when(orderRepository.countDistinctCustomers(storeId)).thenReturn(50L);

            // When
            Map<String, Object> result = service.getCustomerList(storeId, 0, 20, "orderCount", "desc", null, null);

            // Then
            assertThat(result.get("totalElements")).isEqualTo(50L);
            assertThat(result.get("totalPages")).isEqualTo(3);
            assertThat(result.get("page")).isEqualTo(0);
            assertThat(result.get("size")).isEqualTo(20);

            @SuppressWarnings("unchecked")
            List<CustomerListItem> content = (List<CustomerListItem>) result.get("content");
            assertThat(content).hasSize(1);
            assertThat(content.get(0).getCustomerKey()).isEqualTo("1001");
            assertThat(content.get(0).getDisplayName()).isEqualTo("John Doe");
            assertThat(content.get(0).getCity()).isEqualTo("Istanbul");
            assertThat(content.get(0).getOrderCount()).isEqualTo(5);
            assertThat(content.get(0).getTotalSpend()).isEqualByComparingTo(new BigDecimal("2500.00"));
            assertThat(content.get(0).getAvgOrderValue()).isEqualTo(500.0);
            // RFM Scores: Recent (<=7 days = 5), Frequency (5 orders = 3, new thresholds: 4-5 orders), Monetary (2500 = 4)
            assertThat(content.get(0).getRecencyScore()).isEqualTo(5);
            assertThat(content.get(0).getFrequencyScore()).isEqualTo(3);
            assertThat(content.get(0).getMonetaryScore()).isEqualTo(4);
            // RFM = 5+3+4 = 12 → "Sadık Müşteriler" segment (not Şampiyonlar which requires 5+5+5=15)
            assertThat(content.get(0).getRfmSegment()).isEqualTo("Sadık Müşteriler");
        }

        @Test
        @DisplayName("should handle empty customer list")
        void shouldHandleEmptyCustomerList() {
            // Given
            when(orderRepository.findCustomerSummaries(eq(storeId), eq(20), eq(0)))
                    .thenReturn(Collections.emptyList());
            when(orderRepository.countDistinctCustomers(storeId)).thenReturn(0L);

            // When
            Map<String, Object> result = service.getCustomerList(storeId, 0, 20, "orderCount", "desc", null, null);

            // Then
            assertThat(result.get("totalElements")).isEqualTo(0L);
            assertThat(result.get("totalPages")).isEqualTo(0);
            @SuppressWarnings("unchecked")
            List<CustomerListItem> content = (List<CustomerListItem>) result.get("content");
            assertThat(content).isEmpty();
        }

        @Test
        @DisplayName("should handle null customerId gracefully")
        void shouldHandleNullCustomerIdGracefully() {
            // Given
            CustomerSummaryProjection customer = mockCustomerSummaryProjection(
                    null, "Anonymous", "Unknown", 1, new BigDecimal("100.00"),
                    LocalDateTime.now().minusMonths(1), LocalDateTime.now().minusMonths(1)
            );
            when(orderRepository.findCustomerSummaries(eq(storeId), eq(20), eq(0)))
                    .thenReturn(List.of(customer));
            when(orderRepository.countDistinctCustomers(storeId)).thenReturn(1L);

            // When
            Map<String, Object> result = service.getCustomerList(storeId, 0, 20, "orderCount", "desc", null, null);

            // Then
            @SuppressWarnings("unchecked")
            List<CustomerListItem> content = (List<CustomerListItem>) result.get("content");
            assertThat(content.get(0).getCustomerKey()).isEmpty();
        }
    }

    @Nested
    @DisplayName("RFM Scoring")
    @Disabled("RFM functionality removed from UI - requires EntityManager for new method signature")
    class RfmScoring {

        @Test
        @DisplayName("should classify as Şampiyonlar when R>=4, F>=4, M>=4")
        void shouldClassifyAsChampions() {
            // Customer with: recent order (5), 10+ orders (5), 5000+ spend (5)
            CustomerSummaryProjection customer = mockCustomerSummaryProjection(
                    1L, "Champion", "Istanbul", 10, new BigDecimal("6000.00"),
                    LocalDateTime.now().minusMonths(12), LocalDateTime.now().minusDays(3)
            );
            when(orderRepository.findCustomerSummaries(any(), anyInt(), anyInt()))
                    .thenReturn(List.of(customer));
            when(orderRepository.countDistinctCustomers(any())).thenReturn(1L);

            Map<String, Object> result = service.getCustomerList(storeId, 0, 20, "orderCount", "desc", null, null);

            @SuppressWarnings("unchecked")
            List<CustomerListItem> content = (List<CustomerListItem>) result.get("content");
            assertThat(content.get(0).getRfmSegment()).isEqualTo("Şampiyonlar");
        }

        @Test
        @DisplayName("should classify as Sadık Müşteriler when R>=4, F>=3")
        void shouldClassifyAsLoyal() {
            // Customer with: recent order (4), 3-4 orders (3), mid spend (3)
            CustomerSummaryProjection customer = mockCustomerSummaryProjection(
                    2L, "Loyal", "Ankara", 4, new BigDecimal("1500.00"),
                    LocalDateTime.now().minusMonths(3), LocalDateTime.now().minusDays(20)
            );
            when(orderRepository.findCustomerSummaries(any(), anyInt(), anyInt()))
                    .thenReturn(List.of(customer));
            when(orderRepository.countDistinctCustomers(any())).thenReturn(1L);

            Map<String, Object> result = service.getCustomerList(storeId, 0, 20, "orderCount", "desc", null, null);

            @SuppressWarnings("unchecked")
            List<CustomerListItem> content = (List<CustomerListItem>) result.get("content");
            assertThat(content.get(0).getRfmSegment()).isEqualTo("Sadık Müşteriler");
        }

        @Test
        @DisplayName("should classify as Yeni Müşteriler when R>=4, F<=2")
        void shouldClassifyAsNewCustomers() {
            // Customer with: recent order (5), 1 order (1), low spend (1)
            CustomerSummaryProjection customer = mockCustomerSummaryProjection(
                    3L, "New Customer", "Izmir", 1, new BigDecimal("200.00"),
                    LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(2)
            );
            when(orderRepository.findCustomerSummaries(any(), anyInt(), anyInt()))
                    .thenReturn(List.of(customer));
            when(orderRepository.countDistinctCustomers(any())).thenReturn(1L);

            Map<String, Object> result = service.getCustomerList(storeId, 0, 20, "orderCount", "desc", null, null);

            @SuppressWarnings("unchecked")
            List<CustomerListItem> content = (List<CustomerListItem>) result.get("content");
            assertThat(content.get(0).getRfmSegment()).isEqualTo("Yeni Müşteriler");
        }

        @Test
        @DisplayName("should classify as Risk Altında when R<=2, F>=3")
        void shouldClassifyAsAtRisk() {
            // Customer with: old order (2), 5+ orders (4), high spend (4)
            CustomerSummaryProjection customer = mockCustomerSummaryProjection(
                    4L, "At Risk", "Bursa", 6, new BigDecimal("3000.00"),
                    LocalDateTime.now().minusMonths(12), LocalDateTime.now().minusDays(75)
            );
            when(orderRepository.findCustomerSummaries(any(), anyInt(), anyInt()))
                    .thenReturn(List.of(customer));
            when(orderRepository.countDistinctCustomers(any())).thenReturn(1L);

            Map<String, Object> result = service.getCustomerList(storeId, 0, 20, "orderCount", "desc", null, null);

            @SuppressWarnings("unchecked")
            List<CustomerListItem> content = (List<CustomerListItem>) result.get("content");
            assertThat(content.get(0).getRfmSegment()).isEqualTo("Risk Altında");
        }

        @Test
        @DisplayName("should classify as Kayıp when R<=2, F<=2")
        void shouldClassifyAsLost() {
            // Customer with: very old order (1), 1 order (1), low spend (1)
            CustomerSummaryProjection customer = mockCustomerSummaryProjection(
                    5L, "Lost", "Antalya", 1, new BigDecimal("100.00"),
                    LocalDateTime.now().minusMonths(12), LocalDateTime.now().minusDays(120)
            );
            when(orderRepository.findCustomerSummaries(any(), anyInt(), anyInt()))
                    .thenReturn(List.of(customer));
            when(orderRepository.countDistinctCustomers(any())).thenReturn(1L);

            Map<String, Object> result = service.getCustomerList(storeId, 0, 20, "orderCount", "desc", null, null);

            @SuppressWarnings("unchecked")
            List<CustomerListItem> content = (List<CustomerListItem>) result.get("content");
            assertThat(content.get(0).getRfmSegment()).isEqualTo("Kayıp");
        }

        @Test
        @DisplayName("should classify as Kaybedilmek Üzere when R<=2, F<=2, M>=3")
        void shouldClassifyAsAboutToLose() {
            // Customer with: old order (1), 2 orders (2), high spend (4)
            CustomerSummaryProjection customer = mockCustomerSummaryProjection(
                    6L, "About To Lose", "Konya", 2, new BigDecimal("2500.00"),
                    LocalDateTime.now().minusMonths(8), LocalDateTime.now().minusDays(100)
            );
            when(orderRepository.findCustomerSummaries(any(), anyInt(), anyInt()))
                    .thenReturn(List.of(customer));
            when(orderRepository.countDistinctCustomers(any())).thenReturn(1L);

            Map<String, Object> result = service.getCustomerList(storeId, 0, 20, "orderCount", "desc", null, null);

            @SuppressWarnings("unchecked")
            List<CustomerListItem> content = (List<CustomerListItem>) result.get("content");
            assertThat(content.get(0).getRfmSegment()).isEqualTo("Kaybedilmek Üzere");
        }
    }

    @Nested
    @DisplayName("getProductRepeatAnalysis")
    class GetProductRepeatAnalysis {

        @Test
        @DisplayName("should return product repeat analysis with avg days")
        void shouldReturnProductRepeatAnalysisWithAvgDays() {
            // Given
            ProductRepeatProjection product = mockProductRepeatProjection(
                    "BARCODE-001", "Product A", 50, 15, 120
            );
            when(orderRepository.getProductRepeatAnalysis(storeId)).thenReturn(List.of(product));
            List<Object[]> avgDays = new ArrayList<>();
            avgDays.add(new Object[]{"BARCODE-001", 30.5});
            when(orderRepository.getProductAvgRepeatIntervalDays(storeId)).thenReturn(avgDays);

            // When
            List<ProductRepeatData> result = service.getProductRepeatAnalysis(storeId);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getBarcode()).isEqualTo("BARCODE-001");
            assertThat(result.get(0).getProductName()).isEqualTo("Product A");
            assertThat(result.get(0).getTotalBuyers()).isEqualTo(50);
            assertThat(result.get(0).getRepeatBuyers()).isEqualTo(15);
            assertThat(result.get(0).getRepeatRate()).isEqualTo(30.0);
            assertThat(result.get(0).getAvgDaysBetweenRepurchase()).isEqualTo(30.5);
            assertThat(result.get(0).getTotalQuantitySold()).isEqualTo(120);
        }

        @Test
        @DisplayName("should handle missing avg days data")
        void shouldHandleMissingAvgDaysData() {
            // Given
            ProductRepeatProjection product = mockProductRepeatProjection(
                    "BARCODE-002", "Product B", 20, 5, 40
            );
            when(orderRepository.getProductRepeatAnalysis(storeId)).thenReturn(List.of(product));
            when(orderRepository.getProductAvgRepeatIntervalDays(storeId)).thenReturn(Collections.emptyList());

            // When
            List<ProductRepeatData> result = service.getProductRepeatAnalysis(storeId);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAvgDaysBetweenRepurchase()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("getCrossSellAnalysis")
    class GetCrossSellAnalysis {

        @Test
        @DisplayName("should return cross-sell pairs with confidence")
        void shouldReturnCrossSellPairsWithConfidence() {
            // Given
            CrossSellProjection pair = mockCrossSellProjection(
                    "BARCODE-A", "Product A", "BARCODE-B", "Product B", 25
            );
            when(orderRepository.getCrossSellAnalysis(storeId)).thenReturn(List.of(pair));
            when(orderRepository.countDistinctCustomers(storeId)).thenReturn(100L);

            // When
            List<CrossSellData> result = service.getCrossSellAnalysis(storeId);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSourceBarcode()).isEqualTo("BARCODE-A");
            assertThat(result.get(0).getTargetBarcode()).isEqualTo("BARCODE-B");
            assertThat(result.get(0).getCoOccurrenceCount()).isEqualTo(25);
            assertThat(result.get(0).getConfidence()).isEqualTo(25.0); // 25/100 * 100
        }

        @Test
        @DisplayName("should handle zero total customers")
        void shouldHandleZeroTotalCustomers() {
            // Given
            CrossSellProjection pair = mockCrossSellProjection(
                    "BARCODE-X", "Product X", "BARCODE-Y", "Product Y", 5
            );
            when(orderRepository.getCrossSellAnalysis(storeId)).thenReturn(List.of(pair));
            when(orderRepository.countDistinctCustomers(storeId)).thenReturn(0L);

            // When
            List<CrossSellData> result = service.getCrossSellAnalysis(storeId);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getConfidence()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("getBackfillCoverage")
    class GetBackfillCoverage {

        @Test
        @DisplayName("should return correct coverage statistics")
        void shouldReturnCorrectCoverageStatistics() {
            // Given
            BackfillCoverageProjection coverage = mockBackfillCoverageProjection(1000L, 750L);
            when(orderRepository.getCustomerDataCoverage(storeId)).thenReturn(coverage);

            // When
            Map<String, Object> result = service.getBackfillCoverage(storeId);

            // Then
            assertThat(result.get("totalOrders")).isEqualTo(1000L);
            assertThat(result.get("ordersWithCustomerData")).isEqualTo(750L);
            assertThat(result.get("coveragePercent")).isEqualTo(75.0);
            assertThat(result.get("ordersWithoutCustomerData")).isEqualTo(250L);
            assertThat(result.get("note")).isNotNull();
        }

        @Test
        @DisplayName("should handle zero orders")
        void shouldHandleZeroOrders() {
            // Given
            BackfillCoverageProjection coverage = mockBackfillCoverageProjection(0L, 0L);
            when(orderRepository.getCustomerDataCoverage(storeId)).thenReturn(coverage);

            // When
            Map<String, Object> result = service.getBackfillCoverage(storeId);

            // Then
            assertThat(result.get("totalOrders")).isEqualTo(0L);
            assertThat(result.get("coveragePercent")).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("triggerCustomerDataBackfill")
    class TriggerCustomerDataBackfill {

        @Test
        @DisplayName("should call orderService for backfill")
        void shouldCallOrderServiceForBackfill() {
            // When
            service.triggerCustomerDataBackfill(storeId);

            // Then
            verify(orderService).fetchAllOrdersForCustomerBackfill(storeId);
        }

        @Test
        @DisplayName("should handle backfill exception gracefully")
        void shouldHandleBackfillExceptionGracefully() {
            // Given
            doThrow(new RuntimeException("API Error")).when(orderService)
                    .fetchAllOrdersForCustomerBackfill(storeId);

            // When - should not throw
            service.triggerCustomerDataBackfill(storeId);

            // Then - verify method was called (exception is caught and logged)
            verify(orderService).fetchAllOrdersForCustomerBackfill(storeId);
        }
    }

    // ========== Mock Helper Methods ==========

    private SummaryWithOrderCountProjection mockSummaryWithOrderCountProjection(
            Integer totalCustomers, Integer repeatCustomers,
            BigDecimal totalRevenue, BigDecimal repeatRevenue,
            Double avgOrdersPerCustomer, Long totalOrders,
            Double avgItemsPerCustomer, Double avgItemsPerOrder) {
        SummaryWithOrderCountProjection mock = mock(SummaryWithOrderCountProjection.class);
        when(mock.getTotalCustomers()).thenReturn(totalCustomers);
        when(mock.getRepeatCustomers()).thenReturn(repeatCustomers);
        when(mock.getTotalRevenue()).thenReturn(totalRevenue);
        when(mock.getRepeatRevenue()).thenReturn(repeatRevenue);
        when(mock.getAvgOrdersPerCustomer()).thenReturn(avgOrdersPerCustomer);
        when(mock.getTotalOrders()).thenReturn(totalOrders);
        when(mock.getAvgItemsPerCustomer()).thenReturn(avgItemsPerCustomer);
        when(mock.getAvgItemsPerOrder()).thenReturn(avgItemsPerOrder);
        return mock;
    }

    private SegmentProjection mockSegmentProjection(String segment, Integer customerCount, BigDecimal totalRevenue) {
        SegmentProjection mock = mock(SegmentProjection.class);
        when(mock.getSegment()).thenReturn(segment);
        when(mock.getCustomerCount()).thenReturn(customerCount);
        when(mock.getTotalRevenue()).thenReturn(totalRevenue);
        return mock;
    }

    private CityRepeatProjection mockCityRepeatProjection(String city, Integer totalCustomers,
                                                           Integer repeatCustomers, BigDecimal totalRevenue) {
        CityRepeatProjection mock = mock(CityRepeatProjection.class);
        when(mock.getCity()).thenReturn(city);
        when(mock.getTotalCustomers()).thenReturn(totalCustomers);
        when(mock.getRepeatCustomers()).thenReturn(repeatCustomers);
        when(mock.getTotalRevenue()).thenReturn(totalRevenue);
        return mock;
    }

    private MonthlyTrendProjection mockMonthlyTrendProjection(String month, Integer newCustomers,
                                                               Integer repeatCustomers, BigDecimal newRevenue,
                                                               BigDecimal repeatRevenue) {
        MonthlyTrendProjection mock = mock(MonthlyTrendProjection.class);
        when(mock.getMonth()).thenReturn(month);
        when(mock.getNewCustomers()).thenReturn(newCustomers);
        when(mock.getRepeatCustomers()).thenReturn(repeatCustomers);
        when(mock.getNewRevenue()).thenReturn(newRevenue);
        when(mock.getRepeatRevenue()).thenReturn(repeatRevenue);
        return mock;
    }

    private CustomerSummaryProjection mockCustomerSummaryProjection(Long customerId, String displayName,
                                                                      String city, Integer orderCount,
                                                                      BigDecimal totalSpend,
                                                                      LocalDateTime firstOrderDate,
                                                                      LocalDateTime lastOrderDate) {
        CustomerSummaryProjection mock = mock(CustomerSummaryProjection.class);
        when(mock.getCustomerId()).thenReturn(customerId);
        when(mock.getDisplayName()).thenReturn(displayName);
        when(mock.getCity()).thenReturn(city);
        when(mock.getOrderCount()).thenReturn(orderCount);
        when(mock.getTotalSpend()).thenReturn(totalSpend);
        when(mock.getFirstOrderDate()).thenReturn(firstOrderDate);
        when(mock.getLastOrderDate()).thenReturn(lastOrderDate);
        return mock;
    }

    private ProductRepeatProjection mockProductRepeatProjection(String barcode, String productName,
                                                                  Integer totalBuyers, Integer repeatBuyers,
                                                                  Integer totalQuantitySold) {
        ProductRepeatProjection mock = mock(ProductRepeatProjection.class);
        when(mock.getBarcode()).thenReturn(barcode);
        when(mock.getProductName()).thenReturn(productName);
        when(mock.getTotalBuyers()).thenReturn(totalBuyers);
        when(mock.getRepeatBuyers()).thenReturn(repeatBuyers);
        when(mock.getTotalQuantitySold()).thenReturn(totalQuantitySold);
        return mock;
    }

    private CrossSellProjection mockCrossSellProjection(String sourceBarcode, String sourceProductName,
                                                          String targetBarcode, String targetProductName,
                                                          Integer coOccurrenceCount) {
        CrossSellProjection mock = mock(CrossSellProjection.class);
        when(mock.getSourceBarcode()).thenReturn(sourceBarcode);
        when(mock.getSourceProductName()).thenReturn(sourceProductName);
        when(mock.getTargetBarcode()).thenReturn(targetBarcode);
        when(mock.getTargetProductName()).thenReturn(targetProductName);
        when(mock.getCoOccurrenceCount()).thenReturn(coOccurrenceCount);
        return mock;
    }

    private BackfillCoverageProjection mockBackfillCoverageProjection(Long totalOrders, Long ordersWithCustomerData) {
        BackfillCoverageProjection mock = mock(BackfillCoverageProjection.class);
        when(mock.getTotalOrders()).thenReturn(totalOrders);
        when(mock.getOrdersWithCustomerData()).thenReturn(ordersWithCustomerData);
        return mock;
    }
}
