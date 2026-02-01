package com.ecommerce.sellerx.financial;

import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.common.TrendyolRateLimiter;
import com.ecommerce.sellerx.common.exception.InvalidStoreConfigurationException;
import com.ecommerce.sellerx.common.exception.ResourceNotFoundException;
import com.ecommerce.sellerx.orders.TrendyolOrder;
import com.ecommerce.sellerx.orders.TrendyolOrderRepository;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.stores.TrendyolCredentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TrendyolFinancialSettlementService.
 * Tests settlement fetching, processing, and statistics calculation.
 */
@DisplayName("TrendyolFinancialSettlementService")
class TrendyolFinancialSettlementServiceTest extends BaseUnitTest {

    @Mock
    private TrendyolOrderRepository orderRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private TrendyolProductRepository productRepository;

    @Mock
    private TrendyolFinancialSettlementMapper settlementMapper;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private TrendyolRateLimiter rateLimiter;

    private TrendyolFinancialSettlementService settlementService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        settlementService = new TrendyolFinancialSettlementService(
                orderRepository,
                storeRepository,
                productRepository,
                settlementMapper,
                restTemplate,
                rateLimiter
        );
    }

    @Nested
    @DisplayName("getSettlementStats")
    class GetSettlementStats {

        @Test
        @DisplayName("should return correct settlement statistics")
        void shouldReturnCorrectSettlementStatistics() {
            // Given
            UUID storeId = UUID.randomUUID();
            Store store = createTestStore(storeId);

            when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
            when(orderRepository.countByStore(store)).thenReturn(100L);
            when(orderRepository.countByStoreAndTransactionStatus(store, "SETTLED")).thenReturn(75L);
            when(orderRepository.countByStoreAndTransactionStatus(store, "NOT_SETTLED")).thenReturn(25L);
            when(orderRepository.findByStoreAndTransactionStatus(store, "SETTLED")).thenReturn(Collections.emptyList());

            // When
            Map<String, Object> stats = settlementService.getSettlementStats(storeId);

            // Then
            assertThat(stats).isNotNull();
            assertThat(stats.get("totalOrders")).isEqualTo(100L);
            assertThat(stats.get("settledOrders")).isEqualTo(75L);
            assertThat(stats.get("notSettledOrders")).isEqualTo(25L);
            assertThat((Double) stats.get("settlementRate")).isCloseTo(75.0, within(0.01));
        }

        @Test
        @DisplayName("should return zero settlement rate when no orders")
        void shouldReturnZeroSettlementRateWhenNoOrders() {
            // Given
            UUID storeId = UUID.randomUUID();
            Store store = createTestStore(storeId);

            when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
            when(orderRepository.countByStore(store)).thenReturn(0L);
            when(orderRepository.countByStoreAndTransactionStatus(store, "SETTLED")).thenReturn(0L);
            when(orderRepository.countByStoreAndTransactionStatus(store, "NOT_SETTLED")).thenReturn(0L);
            when(orderRepository.findByStoreAndTransactionStatus(store, "SETTLED")).thenReturn(Collections.emptyList());

            // When
            Map<String, Object> stats = settlementService.getSettlementStats(storeId);

            // Then
            assertThat(stats.get("settlementRate")).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when store not found")
        void shouldThrowResourceNotFoundExceptionWhenStoreNotFound() {
            // Given
            UUID storeId = UUID.randomUUID();
            when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> settlementService.getSettlementStats(storeId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should include transaction statistics in response")
        void shouldIncludeTransactionStatisticsInResponse() {
            // Given
            UUID storeId = UUID.randomUUID();
            Store store = createTestStore(storeId);

            // Create settled orders with financial transactions
            List<TrendyolOrder> settledOrders = createSettledOrdersWithTransactions();

            when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
            when(orderRepository.countByStore(store)).thenReturn(10L);
            when(orderRepository.countByStoreAndTransactionStatus(store, "SETTLED")).thenReturn(5L);
            when(orderRepository.countByStoreAndTransactionStatus(store, "NOT_SETTLED")).thenReturn(5L);
            when(orderRepository.findByStoreAndTransactionStatus(store, "SETTLED")).thenReturn(settledOrders);

            // When
            Map<String, Object> stats = settlementService.getSettlementStats(storeId);

            // Then
            assertThat(stats).containsKey("transactionStats");
            @SuppressWarnings("unchecked")
            Map<String, Object> transactionStats = (Map<String, Object>) stats.get("transactionStats");
            assertThat(transactionStats).isNotNull();
            assertThat(transactionStats).containsKeys(
                    "totalSaleTransactions",
                    "totalReturnTransactions",
                    "totalSaleRevenue",
                    "totalReturnAmount",
                    "netRevenue"
            );
        }
    }

    @Nested
    @DisplayName("fetchAndUpdateSettlementsForStore")
    class FetchAndUpdateSettlementsForStore {

        @Test
        @DisplayName("should throw ResourceNotFoundException when store not found")
        void shouldThrowResourceNotFoundExceptionWhenStoreNotFound() {
            // Given
            UUID storeId = UUID.randomUUID();
            when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> settlementService.fetchAndUpdateSettlementsForStore(storeId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw InvalidStoreConfigurationException for non-Trendyol store")
        void shouldThrowInvalidStoreConfigurationExceptionForNonTrendyolStore() {
            // Given
            UUID storeId = UUID.randomUUID();
            Store store = Store.builder()
                    .id(storeId)
                    .marketplace("amazon") // Not Trendyol
                    .build();

            when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

            // When/Then
            assertThatThrownBy(() -> settlementService.fetchAndUpdateSettlementsForStore(storeId))
                    .isInstanceOf(InvalidStoreConfigurationException.class);
        }

        @Test
        @DisplayName("should throw InvalidStoreConfigurationException when credentials missing")
        void shouldThrowInvalidStoreConfigurationExceptionWhenCredentialsMissing() {
            // Given
            UUID storeId = UUID.randomUUID();
            Store store = Store.builder()
                    .id(storeId)
                    .marketplace("trendyol")
                    .credentials(null) // No credentials
                    .build();

            when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

            // When/Then
            assertThatThrownBy(() -> settlementService.fetchAndUpdateSettlementsForStore(storeId))
                    .isInstanceOf(InvalidStoreConfigurationException.class);
        }

        @Test
        @DisplayName("should throw InvalidStoreConfigurationException when sellerId is null")
        void shouldThrowInvalidStoreConfigurationExceptionWhenSellerIdNull() {
            // Given
            UUID storeId = UUID.randomUUID();
            TrendyolCredentials credentials = TrendyolCredentials.builder()
                    .apiKey("test-api-key")
                    .apiSecret("test-api-secret")
                    .sellerId(null) // Null seller ID
                    .build();

            Store store = Store.builder()
                    .id(storeId)
                    .marketplace("trendyol")
                    .credentials(credentials)
                    .build();

            when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

            // When/Then
            assertThatThrownBy(() -> settlementService.fetchAndUpdateSettlementsForStore(storeId))
                    .isInstanceOf(InvalidStoreConfigurationException.class);
        }
    }

    @Nested
    @DisplayName("fetchAndUpdateSettlementsForAllStores")
    class FetchAndUpdateSettlementsForAllStores {

        @Test
        @DisplayName("should skip stores with incomplete initial sync")
        void shouldSkipStoresWithIncompleteInitialSync() {
            // Given
            Store incompleteStore = Store.builder()
                    .id(UUID.randomUUID())
                    .marketplace("trendyol")
                    .initialSyncCompleted(false)
                    .build();

            Store completeStore = createTestStore(UUID.randomUUID());
            completeStore.setInitialSyncCompleted(true);

            when(storeRepository.findByMarketplaceIgnoreCase("trendyol"))
                    .thenReturn(Arrays.asList(incompleteStore, completeStore));

            // When
            settlementService.fetchAndUpdateSettlementsForAllStores();

            // Then
            // The incomplete store should be skipped, so findById should only be called for complete store
            verify(storeRepository, times(1)).findById(completeStore.getId());
            verify(storeRepository, never()).findById(incompleteStore.getId());
        }

        @Test
        @DisplayName("should apply rate limiting between stores")
        void shouldApplyRateLimitingBetweenStores() {
            // Given
            Store store1 = createTestStore(UUID.randomUUID());
            store1.setInitialSyncCompleted(true);
            Store store2 = createTestStore(UUID.randomUUID());
            store2.setInitialSyncCompleted(true);

            when(storeRepository.findByMarketplaceIgnoreCase("trendyol"))
                    .thenReturn(Arrays.asList(store1, store2));
            when(storeRepository.findById(any())).thenReturn(Optional.empty()); // Will throw, but rate limiter should be called

            // When
            settlementService.fetchAndUpdateSettlementsForAllStores();

            // Then
            verify(rateLimiter, times(2)).acquire(any(UUID.class));
        }

        @Test
        @DisplayName("should continue processing after error in one store")
        void shouldContinueProcessingAfterErrorInOneStore() {
            // Given
            Store store1 = createTestStore(UUID.randomUUID());
            store1.setInitialSyncCompleted(true);
            Store store2 = createTestStore(UUID.randomUUID());
            store2.setInitialSyncCompleted(true);

            when(storeRepository.findByMarketplaceIgnoreCase("trendyol"))
                    .thenReturn(Arrays.asList(store1, store2));
            // First store throws exception
            when(storeRepository.findById(store1.getId())).thenThrow(new RuntimeException("Test error"));
            when(storeRepository.findById(store2.getId())).thenReturn(Optional.empty()); // Will throw different error

            // When
            settlementService.fetchAndUpdateSettlementsForAllStores();

            // Then - both stores should be attempted despite first error
            verify(storeRepository).findById(store1.getId());
            verify(storeRepository).findById(store2.getId());
        }
    }

    @Nested
    @DisplayName("TransactionStatistics")
    class TransactionStatistics {

        @Test
        @DisplayName("should calculate sale revenue correctly")
        void shouldCalculateSaleRevenueCorrectly() {
            // Given
            UUID storeId = UUID.randomUUID();
            Store store = createTestStore(storeId);

            List<TrendyolOrder> orders = createOrdersWithSaleTransactions();

            when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
            when(orderRepository.countByStore(store)).thenReturn(2L);
            when(orderRepository.countByStoreAndTransactionStatus(store, "SETTLED")).thenReturn(2L);
            when(orderRepository.countByStoreAndTransactionStatus(store, "NOT_SETTLED")).thenReturn(0L);
            when(orderRepository.findByStoreAndTransactionStatus(store, "SETTLED")).thenReturn(orders);

            // When
            Map<String, Object> stats = settlementService.getSettlementStats(storeId);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> transactionStats = (Map<String, Object>) stats.get("transactionStats");
            assertThat(transactionStats.get("totalSaleTransactions")).isEqualTo(2);
            assertThat(((java.math.BigDecimal) transactionStats.get("totalSaleRevenue")).doubleValue()).isCloseTo(400.0, within(0.01));
        }

        @Test
        @DisplayName("should calculate return amount correctly")
        void shouldCalculateReturnAmountCorrectly() {
            // Given
            UUID storeId = UUID.randomUUID();
            Store store = createTestStore(storeId);

            List<TrendyolOrder> orders = createOrdersWithReturnTransactions();

            when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
            when(orderRepository.countByStore(store)).thenReturn(1L);
            when(orderRepository.countByStoreAndTransactionStatus(store, "SETTLED")).thenReturn(1L);
            when(orderRepository.countByStoreAndTransactionStatus(store, "NOT_SETTLED")).thenReturn(0L);
            when(orderRepository.findByStoreAndTransactionStatus(store, "SETTLED")).thenReturn(orders);

            // When
            Map<String, Object> stats = settlementService.getSettlementStats(storeId);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> transactionStats = (Map<String, Object>) stats.get("transactionStats");
            assertThat(transactionStats.get("totalReturnTransactions")).isEqualTo(1);
            assertThat(((java.math.BigDecimal) transactionStats.get("totalReturnAmount")).doubleValue()).isCloseTo(100.0, within(0.01));
        }

        @Test
        @DisplayName("should calculate net revenue as sales minus returns")
        void shouldCalculateNetRevenueAsSalesMinusReturns() {
            // Given
            UUID storeId = UUID.randomUUID();
            Store store = createTestStore(storeId);

            List<TrendyolOrder> orders = createOrdersWithMixedTransactions();

            when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
            when(orderRepository.countByStore(store)).thenReturn(1L);
            when(orderRepository.countByStoreAndTransactionStatus(store, "SETTLED")).thenReturn(1L);
            when(orderRepository.countByStoreAndTransactionStatus(store, "NOT_SETTLED")).thenReturn(0L);
            when(orderRepository.findByStoreAndTransactionStatus(store, "SETTLED")).thenReturn(orders);

            // When
            Map<String, Object> stats = settlementService.getSettlementStats(storeId);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> transactionStats = (Map<String, Object>) stats.get("transactionStats");
            double saleRevenue = ((java.math.BigDecimal) transactionStats.get("totalSaleRevenue")).doubleValue();
            double returnAmount = ((java.math.BigDecimal) transactionStats.get("totalReturnAmount")).doubleValue();
            double netRevenue = ((java.math.BigDecimal) transactionStats.get("netRevenue")).doubleValue();
            assertThat(netRevenue).isCloseTo(saleRevenue - returnAmount, within(0.01));
        }
    }

    // Helper methods

    private Store createTestStore(UUID storeId) {
        TrendyolCredentials credentials = TrendyolCredentials.builder()
                .apiKey("test-api-key")
                .apiSecret("test-api-secret")
                .sellerId(123456L)
                .build();

        return Store.builder()
                .id(storeId)
                .storeName("Test Store")
                .marketplace("trendyol")
                .credentials(credentials)
                .initialSyncCompleted(false)
                .build();
    }

    private List<TrendyolOrder> createSettledOrdersWithTransactions() {
        TrendyolOrder order = TrendyolOrder.builder()
                .id(UUID.randomUUID())
                .tyOrderNumber("TY-001")
                .transactionStatus("SETTLED")
                .financialTransactions(new ArrayList<>())
                .build();

        FinancialOrderItemData itemData = FinancialOrderItemData.builder()
                .barcode("BARCODE-001")
                .transactions(Arrays.asList(
                        FinancialSettlement.builder()
                                .id("settlement-1")
                                .transactionType("Satış")
                                .status("SOLD")
                                .sellerRevenue(new BigDecimal("150.00"))
                                .build()
                ))
                .build();

        order.getFinancialTransactions().add(itemData);
        return Collections.singletonList(order);
    }

    private List<TrendyolOrder> createOrdersWithSaleTransactions() {
        TrendyolOrder order1 = TrendyolOrder.builder()
                .id(UUID.randomUUID())
                .tyOrderNumber("TY-001")
                .transactionStatus("SETTLED")
                .financialTransactions(new ArrayList<>())
                .build();

        FinancialOrderItemData itemData1 = FinancialOrderItemData.builder()
                .barcode("BARCODE-001")
                .transactions(Arrays.asList(
                        FinancialSettlement.builder()
                                .id("settlement-1")
                                .transactionType("Satış")
                                .status("SOLD")
                                .sellerRevenue(new BigDecimal("200.00"))
                                .build()
                ))
                .build();
        order1.getFinancialTransactions().add(itemData1);

        TrendyolOrder order2 = TrendyolOrder.builder()
                .id(UUID.randomUUID())
                .tyOrderNumber("TY-002")
                .transactionStatus("SETTLED")
                .financialTransactions(new ArrayList<>())
                .build();

        FinancialOrderItemData itemData2 = FinancialOrderItemData.builder()
                .barcode("BARCODE-002")
                .transactions(Arrays.asList(
                        FinancialSettlement.builder()
                                .id("settlement-2")
                                .transactionType("Sale")
                                .status("SOLD")
                                .sellerRevenue(new BigDecimal("200.00"))
                                .build()
                ))
                .build();
        order2.getFinancialTransactions().add(itemData2);

        return Arrays.asList(order1, order2);
    }

    private List<TrendyolOrder> createOrdersWithReturnTransactions() {
        TrendyolOrder order = TrendyolOrder.builder()
                .id(UUID.randomUUID())
                .tyOrderNumber("TY-003")
                .transactionStatus("SETTLED")
                .financialTransactions(new ArrayList<>())
                .build();

        FinancialOrderItemData itemData = FinancialOrderItemData.builder()
                .barcode("BARCODE-003")
                .transactions(Arrays.asList(
                        FinancialSettlement.builder()
                                .id("settlement-3")
                                .transactionType("İade")
                                .status("RETURNED")
                                .sellerRevenue(new BigDecimal("100.00"))
                                .build()
                ))
                .build();
        order.getFinancialTransactions().add(itemData);

        return Collections.singletonList(order);
    }

    private List<TrendyolOrder> createOrdersWithMixedTransactions() {
        TrendyolOrder order = TrendyolOrder.builder()
                .id(UUID.randomUUID())
                .tyOrderNumber("TY-004")
                .transactionStatus("SETTLED")
                .financialTransactions(new ArrayList<>())
                .build();

        FinancialOrderItemData itemData = FinancialOrderItemData.builder()
                .barcode("BARCODE-004")
                .transactions(Arrays.asList(
                        FinancialSettlement.builder()
                                .id("settlement-4a")
                                .transactionType("Satış")
                                .status("SOLD")
                                .sellerRevenue(new BigDecimal("300.00"))
                                .build(),
                        FinancialSettlement.builder()
                                .id("settlement-4b")
                                .transactionType("Return")
                                .status("RETURNED")
                                .sellerRevenue(new BigDecimal("50.00"))
                                .build()
                ))
                .build();
        order.getFinancialTransactions().add(itemData);

        return Collections.singletonList(order);
    }
}
