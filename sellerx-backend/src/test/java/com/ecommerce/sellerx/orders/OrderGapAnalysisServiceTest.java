package com.ecommerce.sellerx.orders;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderGapAnalysisService.
 * Tests gap analysis between Settlement API and Orders API data.
 */
@ExtendWith(MockitoExtension.class)
class OrderGapAnalysisServiceTest {

    @Mock
    private TrendyolOrderRepository orderRepository;

    @InjectMocks
    private OrderGapAnalysisService gapAnalysisService;

    private UUID storeId;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
    }

    // ==================== analyzeGap Tests ====================

    @Test
    @DisplayName("Should calculate gap days correctly when settlement data exists")
    void testAnalyzeGapWithSettlementData() {
        // Given: Settlement data exists up to 7 days ago
        LocalDate latestSettlementDate = LocalDate.now().minusDays(7);

        when(orderRepository.findLatestOrderDateByStoreIdAndDataSource(storeId, "SETTLEMENT_API"))
                .thenReturn(latestSettlementDate);
        when(orderRepository.countByStoreIdAndIsCommissionEstimatedTrue(storeId))
                .thenReturn(15);
        when(orderRepository.countByStoreIdAndDataSource(storeId, "ORDER_API"))
                .thenReturn(20);
        when(orderRepository.countByStoreIdAndDataSource(storeId, "SETTLEMENT_API"))
                .thenReturn(100);
        when(orderRepository.countByStoreIdAndDataSource(storeId, "HYBRID"))
                .thenReturn(5);

        // When
        OrderGapAnalysisService.GapAnalysis analysis = gapAnalysisService.analyzeGap(storeId);

        // Then
        assertThat(analysis.getStoreId()).isEqualTo(storeId);
        assertThat(analysis.getLatestSettlementOrderDate()).isEqualTo(latestSettlementDate);
        assertThat(analysis.getGapStartDate()).isEqualTo(latestSettlementDate.plusDays(1));
        assertThat(analysis.getGapEndDate()).isEqualTo(LocalDate.now());
        assertThat(analysis.getGapDays()).isEqualTo(6); // 7 days ago + 1 = 6 days gap
        assertThat(analysis.getOrdersNeedingCommission()).isEqualTo(15);
        assertThat(analysis.getOrdersFromOrdersApi()).isEqualTo(20);
        assertThat(analysis.getOrdersFromSettlementApi()).isEqualTo(100);
        assertThat(analysis.getHybridOrders()).isEqualTo(5);
        assertThat(analysis.getAnalysisTime()).isNotNull();
    }

    @Test
    @DisplayName("Should use default gap when no settlement data exists")
    void testAnalyzeGapWithNoSettlementData() {
        // Given: No settlement data exists
        when(orderRepository.findLatestOrderDateByStoreIdAndDataSource(storeId, "SETTLEMENT_API"))
                .thenReturn(null);
        when(orderRepository.countByStoreIdAndIsCommissionEstimatedTrue(storeId))
                .thenReturn(0);
        when(orderRepository.countByStoreIdAndDataSource(storeId, "ORDER_API"))
                .thenReturn(0);
        when(orderRepository.countByStoreIdAndDataSource(storeId, "SETTLEMENT_API"))
                .thenReturn(0);
        when(orderRepository.countByStoreIdAndDataSource(storeId, "HYBRID"))
                .thenReturn(0);

        // When
        OrderGapAnalysisService.GapAnalysis analysis = gapAnalysisService.analyzeGap(storeId);

        // Then: Should use default of 8 days ago (SETTLEMENT_DELAY_DAYS + BUFFER_DAYS)
        assertThat(analysis.getLatestSettlementOrderDate()).isNull();
        assertThat(analysis.getGapStartDate()).isEqualTo(LocalDate.now().minusDays(8));
        assertThat(analysis.getGapEndDate()).isEqualTo(LocalDate.now());
        assertThat(analysis.getGapDays()).isEqualTo(8);
    }

    // ==================== findOrdersNeedingReconciliation Tests ====================

    @Test
    @DisplayName("Should find orders with estimated commission")
    void testFindOrdersNeedingReconciliation() {
        // Given
        TrendyolOrder order1 = TrendyolOrder.builder()
                .tyOrderNumber("ORDER001")
                .isCommissionEstimated(true)
                .dataSource("ORDER_API")
                .build();

        TrendyolOrder order2 = TrendyolOrder.builder()
                .tyOrderNumber("ORDER002")
                .isCommissionEstimated(true)
                .dataSource("ORDER_API")
                .build();

        List<TrendyolOrder> expectedOrders = Arrays.asList(order1, order2);

        when(orderRepository.findByStoreIdAndIsCommissionEstimatedTrue(storeId))
                .thenReturn(expectedOrders);

        // When
        List<TrendyolOrder> result = gapAnalysisService.findOrdersNeedingReconciliation(storeId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(expectedOrders);
        verify(orderRepository).findByStoreIdAndIsCommissionEstimatedTrue(storeId);
    }

    @Test
    @DisplayName("Should return empty list when no orders need reconciliation")
    void testFindOrdersNeedingReconciliationEmpty() {
        // Given
        when(orderRepository.findByStoreIdAndIsCommissionEstimatedTrue(storeId))
                .thenReturn(Collections.emptyList());

        // When
        List<TrendyolOrder> result = gapAnalysisService.findOrdersNeedingReconciliation(storeId);

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== getOrdersApiSyncRecommendation Tests ====================

    @Test
    @DisplayName("Should recommend HIGH priority when orders need commission")
    void testGetSyncRecommendationHighPriority() {
        // Given: Orders need commission
        LocalDate latestSettlementDate = LocalDate.now().minusDays(5);

        when(orderRepository.findLatestOrderDateByStoreIdAndDataSource(storeId, "SETTLEMENT_API"))
                .thenReturn(latestSettlementDate);
        when(orderRepository.countByStoreIdAndIsCommissionEstimatedTrue(storeId))
                .thenReturn(10); // Has orders needing commission
        when(orderRepository.countByStoreIdAndDataSource(storeId, "ORDER_API"))
                .thenReturn(10);
        when(orderRepository.countByStoreIdAndDataSource(storeId, "SETTLEMENT_API"))
                .thenReturn(50);
        when(orderRepository.countByStoreIdAndDataSource(storeId, "HYBRID"))
                .thenReturn(0);

        // When
        OrderGapAnalysisService.SyncRecommendation recommendation =
                gapAnalysisService.getOrdersApiSyncRecommendation(storeId);

        // Then
        assertThat(recommendation.getPriority()).isEqualTo("HIGH");
        assertThat(recommendation.getEstimatedOrders()).isEqualTo(10);
        assertThat(recommendation.getReason()).isEqualTo("Gap fill from Orders API");
        assertThat(recommendation.getStartDate()).isNotNull();
        assertThat(recommendation.getEndDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("Should recommend NORMAL priority when no orders need commission")
    void testGetSyncRecommendationNormalPriority() {
        // Given: No orders need commission
        LocalDate latestSettlementDate = LocalDate.now().minusDays(3);

        when(orderRepository.findLatestOrderDateByStoreIdAndDataSource(storeId, "SETTLEMENT_API"))
                .thenReturn(latestSettlementDate);
        when(orderRepository.countByStoreIdAndIsCommissionEstimatedTrue(storeId))
                .thenReturn(0); // No orders needing commission
        when(orderRepository.countByStoreIdAndDataSource(storeId, "ORDER_API"))
                .thenReturn(0);
        when(orderRepository.countByStoreIdAndDataSource(storeId, "SETTLEMENT_API"))
                .thenReturn(100);
        when(orderRepository.countByStoreIdAndDataSource(storeId, "HYBRID"))
                .thenReturn(0);

        // When
        OrderGapAnalysisService.SyncRecommendation recommendation =
                gapAnalysisService.getOrdersApiSyncRecommendation(storeId);

        // Then
        assertThat(recommendation.getPriority()).isEqualTo("NORMAL");
        assertThat(recommendation.getEstimatedOrders()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should not go back more than 90 days")
    void testSyncRecommendationMaxHistoricalLimit() {
        // Given: Very old settlement date
        LocalDate veryOldSettlementDate = LocalDate.now().minusDays(120);

        when(orderRepository.findLatestOrderDateByStoreIdAndDataSource(storeId, "SETTLEMENT_API"))
                .thenReturn(veryOldSettlementDate);
        when(orderRepository.countByStoreIdAndIsCommissionEstimatedTrue(storeId))
                .thenReturn(5);
        when(orderRepository.countByStoreIdAndDataSource(storeId, "ORDER_API"))
                .thenReturn(5);
        when(orderRepository.countByStoreIdAndDataSource(storeId, "SETTLEMENT_API"))
                .thenReturn(200);
        when(orderRepository.countByStoreIdAndDataSource(storeId, "HYBRID"))
                .thenReturn(0);

        // When
        OrderGapAnalysisService.SyncRecommendation recommendation =
                gapAnalysisService.getOrdersApiSyncRecommendation(storeId);

        // Then: Start date should be limited to 90 days ago
        assertThat(recommendation.getStartDate())
                .isAfterOrEqualTo(LocalDate.now().minusDays(90));
    }

    // ==================== isOrderReconciled Tests ====================

    @Test
    @DisplayName("Should identify reconciled orders correctly")
    void testIsOrderReconciled() {
        // Order from Settlement API - already reconciled
        TrendyolOrder settlementOrder = TrendyolOrder.builder()
                .dataSource("SETTLEMENT_API")
                .isCommissionEstimated(false)
                .build();
        assertThat(gapAnalysisService.isOrderReconciled(settlementOrder)).isTrue();

        // Order from HYBRID - already reconciled
        TrendyolOrder hybridOrder = TrendyolOrder.builder()
                .dataSource("HYBRID")
                .isCommissionEstimated(false)
                .build();
        assertThat(gapAnalysisService.isOrderReconciled(hybridOrder)).isTrue();

        // Order from ORDER_API with estimated commission - NOT reconciled
        TrendyolOrder estimatedOrder = TrendyolOrder.builder()
                .dataSource("ORDER_API")
                .isCommissionEstimated(true)
                .build();
        assertThat(gapAnalysisService.isOrderReconciled(estimatedOrder)).isFalse();

        // Order from ORDER_API but commission not estimated (edge case) - reconciled
        TrendyolOrder orderApiReconciledOrder = TrendyolOrder.builder()
                .dataSource("ORDER_API")
                .isCommissionEstimated(false)
                .build();
        assertThat(gapAnalysisService.isOrderReconciled(orderApiReconciledOrder)).isTrue();
    }

    // ==================== findLatestSettlementOrderDate Tests ====================

    @Test
    @DisplayName("Should find latest settlement order date")
    void testFindLatestSettlementOrderDate() {
        // Given
        LocalDate expectedDate = LocalDate.now().minusDays(5);
        when(orderRepository.findLatestOrderDateByStoreIdAndDataSource(storeId, "SETTLEMENT_API"))
                .thenReturn(expectedDate);

        // When
        LocalDate result = gapAnalysisService.findLatestSettlementOrderDate(storeId);

        // Then
        assertThat(result).isEqualTo(expectedDate);
        verify(orderRepository).findLatestOrderDateByStoreIdAndDataSource(storeId, "SETTLEMENT_API");
    }

    // ==================== findEarliestSettlementOrderDate Tests ====================

    @Test
    @DisplayName("Should find earliest settlement order date")
    void testFindEarliestSettlementOrderDate() {
        // Given
        LocalDate expectedDate = LocalDate.of(2024, 1, 15);
        when(orderRepository.findEarliestOrderDateByStoreIdAndDataSource(storeId, "SETTLEMENT_API"))
                .thenReturn(expectedDate);

        // When
        LocalDate result = gapAnalysisService.findEarliestSettlementOrderDate(storeId);

        // Then
        assertThat(result).isEqualTo(expectedDate);
        verify(orderRepository).findEarliestOrderDateByStoreIdAndDataSource(storeId, "SETTLEMENT_API");
    }

    // ==================== findOrdersNeedingReconciliationInRange Tests ====================

    @Test
    @DisplayName("Should find orders needing reconciliation in date range")
    void testFindOrdersNeedingReconciliationInRange() {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        TrendyolOrder order = TrendyolOrder.builder()
                .tyOrderNumber("ORDER_IN_RANGE")
                .isCommissionEstimated(true)
                .build();

        when(orderRepository.findByStoreIdAndOrderDateBetweenAndIsCommissionEstimatedTrue(
                eq(storeId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(order));

        // When
        List<TrendyolOrder> result = gapAnalysisService.findOrdersNeedingReconciliationInRange(
                storeId, startDate, endDate);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTyOrderNumber()).isEqualTo("ORDER_IN_RANGE");
    }
}
