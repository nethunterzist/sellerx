package com.ecommerce.sellerx.financial;

import com.ecommerce.sellerx.orders.TrendyolOrder;
import com.ecommerce.sellerx.orders.TrendyolOrderRepository;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CommissionReconciliationService.
 * Tests reconciliation between estimated (Orders API) and real (Settlement API) commissions.
 */
@ExtendWith(MockitoExtension.class)
class CommissionReconciliationServiceTest {

    @Mock
    private TrendyolOrderRepository orderRepository;

    @Mock
    private CommissionReconciliationLogRepository reconciliationLogRepository;

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private CommissionReconciliationService reconciliationService;

    private UUID storeId;
    private Store store;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        store = Store.builder()
                .id(storeId)
                .storeName("Test Store")
                .marketplace("trendyol")
                .initialSyncCompleted(true)
                .build();
    }

    // ==================== reconcileOrder Tests ====================

    @Test
    @DisplayName("Should reconcile order with real commission - ORDER_API to HYBRID transition")
    void testReconcileOrder() {
        // Given: Order from Orders API with estimated commission
        BigDecimal estimatedCommission = new BigDecimal("120.00");
        BigDecimal realCommission = new BigDecimal("126.57");
        BigDecimal expectedDifference = new BigDecimal("6.57"); // real - estimated

        TrendyolOrder order = TrendyolOrder.builder()
                .tyOrderNumber("ORDER_RECON_001")
                .store(store)
                .dataSource("ORDER_API")
                .isCommissionEstimated(true)
                .estimatedCommission(estimatedCommission)
                .build();

        // When
        reconciliationService.reconcileOrder(order, realCommission);

        // Then: Order should be updated
        assertThat(order.getEstimatedCommission()).isEqualByComparingTo(realCommission);
        assertThat(order.getIsCommissionEstimated()).isFalse();
        assertThat(order.getDataSource()).isEqualTo("HYBRID");
        assertThat(order.getCommissionDifference()).isEqualByComparingTo(expectedDifference);
    }

    @Test
    @DisplayName("Should not reconcile order that is already reconciled")
    void testReconcileOrderAlreadyReconciled() {
        // Given: Order already reconciled
        TrendyolOrder order = TrendyolOrder.builder()
                .tyOrderNumber("ORDER_ALREADY_RECON")
                .dataSource("HYBRID")
                .isCommissionEstimated(false)
                .estimatedCommission(new BigDecimal("100.00"))
                .build();

        BigDecimal originalCommission = order.getEstimatedCommission();

        // When
        reconciliationService.reconcileOrder(order, new BigDecimal("150.00"));

        // Then: Order should NOT be changed
        assertThat(order.getEstimatedCommission()).isEqualByComparingTo(originalCommission);
        assertThat(order.getIsCommissionEstimated()).isFalse();
    }

    @Test
    @DisplayName("Should handle null estimated commission during reconciliation")
    void testReconcileOrderWithNullEstimatedCommission() {
        // Given: Order with null estimated commission
        BigDecimal realCommission = new BigDecimal("50.00");

        TrendyolOrder order = TrendyolOrder.builder()
                .tyOrderNumber("ORDER_NULL_EST")
                .store(store)
                .dataSource("ORDER_API")
                .isCommissionEstimated(true)
                .estimatedCommission(null)
                .build();

        // When
        reconciliationService.reconcileOrder(order, realCommission);

        // Then: Difference should be calculated from ZERO
        assertThat(order.getEstimatedCommission()).isEqualByComparingTo(realCommission);
        assertThat(order.getCommissionDifference()).isEqualByComparingTo(realCommission); // 50 - 0 = 50
        assertThat(order.getDataSource()).isEqualTo("HYBRID");
    }

    // ==================== reconcileStore Tests ====================

    @Test
    @DisplayName("Should reconcile store and create log entry")
    void testReconcileStore() {
        // Given: Store with orders needing reconciliation
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

        // Create orders with financial transaction data (simulating Settlement data arrival)
        TrendyolOrder order1 = createOrderWithFinancialData("ORDER001", new BigDecimal("100.00"), new BigDecimal("126.57"));
        TrendyolOrder order2 = createOrderWithFinancialData("ORDER002", new BigDecimal("50.00"), new BigDecimal("62.50"));

        when(orderRepository.findByStoreIdAndIsCommissionEstimatedTrue(storeId))
                .thenReturn(Arrays.asList(order1, order2));

        // When
        CommissionReconciliationService.ReconciliationResult result =
                reconciliationService.reconcileStore(storeId);

        // Then
        assertThat(result.getStoreId()).isEqualTo(storeId);
        assertThat(result.getTotalProcessed()).isEqualTo(2);
        assertThat(result.getReconciled()).isEqualTo(2);
        assertThat(result.getNotYetSettled()).isEqualTo(0);

        // Verify log entry was saved
        verify(reconciliationLogRepository).save(any(CommissionReconciliationLog.class));
    }

    @Test
    @DisplayName("Should return empty result when no orders need reconciliation")
    void testReconcileStoreNoOrders() {
        // Given: No orders need reconciliation
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(orderRepository.findByStoreIdAndIsCommissionEstimatedTrue(storeId))
                .thenReturn(Collections.emptyList());

        // When
        CommissionReconciliationService.ReconciliationResult result =
                reconciliationService.reconcileStore(storeId);

        // Then
        assertThat(result.getTotalProcessed()).isEqualTo(0);
        assertThat(result.getReconciled()).isEqualTo(0);
        assertThat(result.getNotYetSettled()).isEqualTo(0);

        // Verify no log entry was saved (nothing to log)
        verify(reconciliationLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should count orders not yet settled (no financial data)")
    void testReconcileStoreWithNotYetSettledOrders() {
        // Given: Mix of orders - some with financial data, some without
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

        TrendyolOrder orderWithData = createOrderWithFinancialData("ORDER_WITH", new BigDecimal("100.00"), new BigDecimal("120.00"));
        TrendyolOrder orderWithoutData = createOrderWithoutFinancialData("ORDER_WITHOUT", new BigDecimal("50.00"));

        when(orderRepository.findByStoreIdAndIsCommissionEstimatedTrue(storeId))
                .thenReturn(Arrays.asList(orderWithData, orderWithoutData));

        // When
        CommissionReconciliationService.ReconciliationResult result =
                reconciliationService.reconcileStore(storeId);

        // Then
        assertThat(result.getTotalProcessed()).isEqualTo(2);
        assertThat(result.getReconciled()).isEqualTo(1);
        assertThat(result.getNotYetSettled()).isEqualTo(1);
    }

    // ==================== reconcileAllStores Tests ====================

    @Test
    @DisplayName("Should reconcile all stores with completed initial sync")
    void testReconcileAllStores() {
        // Given: Multiple stores
        Store store2 = Store.builder()
                .id(UUID.randomUUID())
                .storeName("Test Store 2")
                .initialSyncCompleted(true)
                .build();

        when(storeRepository.findByInitialSyncCompletedTrue())
                .thenReturn(Arrays.asList(store, store2));

        when(storeRepository.findById(any(UUID.class)))
                .thenReturn(Optional.of(store));

        when(orderRepository.findByStoreIdAndIsCommissionEstimatedTrue(any()))
                .thenReturn(Collections.emptyList());

        // When
        reconciliationService.reconcileAllStores();

        // Then: Should process both stores
        verify(storeRepository).findByInitialSyncCompletedTrue();
        verify(orderRepository, times(2)).findByStoreIdAndIsCommissionEstimatedTrue(any());
    }

    // ==================== getReconciliationHistory Tests ====================

    @Test
    @DisplayName("Should return reconciliation history for store")
    void testGetReconciliationHistory() {
        // Given
        CommissionReconciliationLog log1 = CommissionReconciliationLog.builder()
                .reconciliationDate(LocalDate.now().minusDays(1))
                .store(store)
                .totalReconciled(10)
                .build();

        CommissionReconciliationLog log2 = CommissionReconciliationLog.builder()
                .reconciliationDate(LocalDate.now())
                .store(store)
                .totalReconciled(5)
                .build();

        when(reconciliationLogRepository.findByStoreIdOrderByReconciliationDateDesc(storeId))
                .thenReturn(Arrays.asList(log2, log1));

        // When
        List<CommissionReconciliationLog> history = reconciliationService.getReconciliationHistory(storeId);

        // Then
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getReconciliationDate()).isEqualTo(LocalDate.now());
    }

    // ==================== getReconciliationSummary Tests ====================

    @Test
    @DisplayName("Should return reconciliation summary with statistics")
    void testGetReconciliationSummary() {
        // Given
        when(reconciliationLogRepository.sumTotalReconciledByStoreId(storeId))
                .thenReturn(100);
        when(reconciliationLogRepository.sumTotalDifferenceByStoreId(storeId))
                .thenReturn(new BigDecimal("250.00"));
        when(reconciliationLogRepository.avgAccuracyByStoreId(storeId))
                .thenReturn(new BigDecimal("95.50"));
        when(reconciliationLogRepository.findLatestReconciliationDateByStoreId(storeId))
                .thenReturn(LocalDate.now());
        when(orderRepository.countByStoreIdAndIsCommissionEstimatedTrue(storeId))
                .thenReturn(15);

        // When
        CommissionReconciliationService.ReconciliationSummary summary =
                reconciliationService.getReconciliationSummary(storeId);

        // Then
        assertThat(summary.getStoreId()).isEqualTo(storeId);
        assertThat(summary.getTotalReconciled()).isEqualTo(100);
        assertThat(summary.getTotalDifference()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(summary.getAverageAccuracy()).isEqualByComparingTo(new BigDecimal("95.50"));
        assertThat(summary.getLatestReconciliationDate()).isEqualTo(LocalDate.now());
        assertThat(summary.getPendingReconciliation()).isEqualTo(15);
    }

    @Test
    @DisplayName("Should handle null values in summary")
    void testGetReconciliationSummaryWithNulls() {
        // Given: No reconciliation data exists
        when(reconciliationLogRepository.sumTotalReconciledByStoreId(storeId))
                .thenReturn(null);
        when(reconciliationLogRepository.sumTotalDifferenceByStoreId(storeId))
                .thenReturn(null);
        when(reconciliationLogRepository.avgAccuracyByStoreId(storeId))
                .thenReturn(null);
        when(reconciliationLogRepository.findLatestReconciliationDateByStoreId(storeId))
                .thenReturn(null);
        when(orderRepository.countByStoreIdAndIsCommissionEstimatedTrue(storeId))
                .thenReturn(0);

        // When
        CommissionReconciliationService.ReconciliationSummary summary =
                reconciliationService.getReconciliationSummary(storeId);

        // Then: Should handle nulls gracefully
        assertThat(summary.getTotalReconciled()).isEqualTo(0);
        assertThat(summary.getTotalDifference()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getAverageAccuracy()).isNull();
        assertThat(summary.getLatestReconciliationDate()).isNull();
        assertThat(summary.getPendingReconciliation()).isEqualTo(0);
    }

    // ==================== Commission Difference Calculation Tests ====================

    @Test
    @DisplayName("Should calculate positive difference when underestimated")
    void testCommissionDifferenceUnderestimated() {
        // Given: Estimated < Real (underestimated)
        BigDecimal estimated = new BigDecimal("100.00");
        BigDecimal real = new BigDecimal("120.00");

        TrendyolOrder order = TrendyolOrder.builder()
                .tyOrderNumber("ORDER_UNDER")
                .dataSource("ORDER_API")
                .isCommissionEstimated(true)
                .estimatedCommission(estimated)
                .build();

        // When
        reconciliationService.reconcileOrder(order, real);

        // Then: Difference should be positive (120 - 100 = 20)
        assertThat(order.getCommissionDifference()).isEqualByComparingTo(new BigDecimal("20.00"));
    }

    @Test
    @DisplayName("Should calculate negative difference when overestimated")
    void testCommissionDifferenceOverestimated() {
        // Given: Estimated > Real (overestimated)
        BigDecimal estimated = new BigDecimal("150.00");
        BigDecimal real = new BigDecimal("120.00");

        TrendyolOrder order = TrendyolOrder.builder()
                .tyOrderNumber("ORDER_OVER")
                .dataSource("ORDER_API")
                .isCommissionEstimated(true)
                .estimatedCommission(estimated)
                .build();

        // When
        reconciliationService.reconcileOrder(order, real);

        // Then: Difference should be negative (120 - 150 = -30)
        assertThat(order.getCommissionDifference()).isEqualByComparingTo(new BigDecimal("-30.00"));
    }

    // ==================== Helper Methods ====================

    /**
     * Creates an order with financial transaction data (simulating Settlement API data arrival)
     */
    private TrendyolOrder createOrderWithFinancialData(String orderNumber,
                                                        BigDecimal estimatedCommission,
                                                        BigDecimal realCommission) {
        // Create financial transaction with commission
        FinancialSettlement transaction = new FinancialSettlement();
        transaction.setCommissionAmount(realCommission);

        FinancialOrderItemData itemData = new FinancialOrderItemData();
        itemData.setTransactions(Collections.singletonList(transaction));

        TrendyolOrder order = TrendyolOrder.builder()
                .tyOrderNumber(orderNumber)
                .store(store)
                .dataSource("ORDER_API")
                .isCommissionEstimated(true)
                .estimatedCommission(estimatedCommission)
                .financialTransactions(Collections.singletonList(itemData))
                .build();

        return order;
    }

    /**
     * Creates an order without financial transaction data (Settlement data not yet arrived)
     */
    private TrendyolOrder createOrderWithoutFinancialData(String orderNumber,
                                                           BigDecimal estimatedCommission) {
        return TrendyolOrder.builder()
                .tyOrderNumber(orderNumber)
                .store(store)
                .dataSource("ORDER_API")
                .isCommissionEstimated(true)
                .estimatedCommission(estimatedCommission)
                .financialTransactions(null) // No settlement data
                .build();
    }
}
