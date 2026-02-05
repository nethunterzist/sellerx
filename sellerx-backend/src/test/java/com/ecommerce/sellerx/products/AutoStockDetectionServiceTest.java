package com.ecommerce.sellerx.products;

import com.ecommerce.sellerx.alerts.AlertHistory;
import com.ecommerce.sellerx.alerts.AlertHistoryRepository;
import com.ecommerce.sellerx.alerts.AlertSeverity;
import com.ecommerce.sellerx.alerts.AlertType;
import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.common.TestDataBuilder;
import com.ecommerce.sellerx.purchasing.PurchaseOrder;
import com.ecommerce.sellerx.purchasing.PurchaseOrderItem;
import com.ecommerce.sellerx.purchasing.PurchaseOrderItemRepository;
import com.ecommerce.sellerx.purchasing.PurchaseOrderStatus;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("AutoStockDetectionService")
class AutoStockDetectionServiceTest extends BaseUnitTest {

    @Mock
    private TrendyolProductRepository productRepository;

    @Mock
    private PurchaseOrderItemRepository poItemRepository;

    @Mock
    private AlertHistoryRepository alertHistoryRepository;

    @InjectMocks
    private AutoStockDetectionService service;

    private User testUser;
    private Store testStore;

    @BeforeEach
    void setUp() {
        testUser = TestDataBuilder.user().build();
        testUser.setId(1L);

        testStore = TestDataBuilder.store(testUser).build();
        testStore.setId(UUID.randomUUID());
    }

    private TrendyolProduct createProductWithCost(double unitCost, int costVatRate) {
        List<CostAndStockInfo> costList = new ArrayList<>();
        costList.add(CostAndStockInfo.builder()
                .unitCost(unitCost)
                .costVatRate(costVatRate)
                .quantity(100)
                .stockDate(LocalDate.now().minusDays(5))
                .usedQuantity(0)
                .costSource("MANUAL")
                .build());

        TrendyolProduct product = TestDataBuilder.product(testStore)
                .costAndStockInfo(costList)
                .trendyolQuantity(300)
                .build();
        product.setId(UUID.randomUUID());
        return product;
    }

    private TrendyolProduct createProductWithoutCost() {
        TrendyolProduct product = TestDataBuilder.product(testStore)
                .costAndStockInfo(new ArrayList<>())
                .trendyolQuantity(300)
                .build();
        product.setId(UUID.randomUUID());
        return product;
    }

    @Nested
    @DisplayName("handleStockIncrease")
    class HandleStockIncrease {

        @Test
        @DisplayName("should create PENDING_APPROVAL alert when stock increases and cost exists")
        void shouldCreatePendingApprovalAlert() {
            TrendyolProduct product = createProductWithCost(50.0, 20);
            when(poItemRepository.findByProductIdAndStoreId(product.getId(), testStore.getId()))
                    .thenReturn(List.of());

            service.handleStockIncrease(product, 250, 300);

            // Should NOT create cost entry (deferred to approval)
            assertThat(product.getCostAndStockInfo()).hasSize(1); // only original
            verify(productRepository, never()).save(any());

            // Should create PENDING_APPROVAL alert
            ArgumentCaptor<AlertHistory> alertCaptor = ArgumentCaptor.forClass(AlertHistory.class);
            verify(alertHistoryRepository).save(alertCaptor.capture());
            AlertHistory alert = alertCaptor.getValue();
            assertThat(alert.getStatus()).isEqualTo("PENDING_APPROVAL");
            assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.MEDIUM);
            assertThat(alert.getAlertType()).isEqualTo(AlertType.STOCK);

            Map<String, Object> data = alert.getData();
            assertThat(data.get("pendingApproval")).isEqualTo(true);
            assertThat(data.get("hasCostInfo")).isEqualTo(true);
            assertThat(data.get("delta")).isEqualTo(50);
            assertThat(data.get("unitCost")).isEqualTo(50.0);
            assertThat(data.get("costVatRate")).isEqualTo(20);
            assertThat(data.get("productId")).isEqualTo(product.getId().toString());
        }

        @Test
        @DisplayName("should create HIGH severity PENDING_APPROVAL alert when no cost info exists")
        void shouldSendHighAlertWhenNoCost() {
            TrendyolProduct product = createProductWithoutCost();
            when(poItemRepository.findByProductIdAndStoreId(product.getId(), testStore.getId()))
                    .thenReturn(List.of());

            service.handleStockIncrease(product, 250, 300);

            // Should NOT create cost entry
            assertThat(product.getCostAndStockInfo()).isEmpty();
            verify(productRepository, never()).save(any());

            // Should create HIGH severity PENDING_APPROVAL alert
            ArgumentCaptor<AlertHistory> alertCaptor = ArgumentCaptor.forClass(AlertHistory.class);
            verify(alertHistoryRepository).save(alertCaptor.capture());
            AlertHistory alert = alertCaptor.getValue();
            assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.HIGH);
            assertThat(alert.getStatus()).isEqualTo("PENDING_APPROVAL");
            assertThat(alert.getAlertType()).isEqualTo(AlertType.STOCK);

            Map<String, Object> data = alert.getData();
            assertThat(data.get("hasCostInfo")).isEqualTo(false);
            assertThat(data.get("pendingApproval")).isEqualTo(true);
        }

        @Test
        @DisplayName("should create MEDIUM severity alert when cost exists")
        void shouldSendMediumAlertWhenCostExists() {
            TrendyolProduct product = createProductWithCost(50.0, 20);
            when(poItemRepository.findByProductIdAndStoreId(product.getId(), testStore.getId()))
                    .thenReturn(List.of());

            service.handleStockIncrease(product, 250, 300);

            ArgumentCaptor<AlertHistory> alertCaptor = ArgumentCaptor.forClass(AlertHistory.class);
            verify(alertHistoryRepository).save(alertCaptor.capture());
            AlertHistory alert = alertCaptor.getValue();
            assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.MEDIUM);
            assertThat(alert.getStatus()).isEqualTo("PENDING_APPROVAL");
            assertThat(alert.getUser()).isEqualTo(testUser);
            assertThat(alert.getStore()).isEqualTo(testStore);
        }

        @Test
        @DisplayName("should not trigger when delta is zero or negative")
        void shouldNotTriggerOnZeroOrNegativeDelta() {
            TrendyolProduct product = createProductWithCost(50.0, 20);

            service.handleStockIncrease(product, 300, 300);
            service.handleStockIncrease(product, 300, 250);

            verifyNoInteractions(poItemRepository, productRepository, alertHistoryRepository);
        }

        @Test
        @DisplayName("should include latest cost info in alert data when multiple cost entries exist")
        void shouldUseLatestCostEntry() {
            List<CostAndStockInfo> costList = new ArrayList<>();
            costList.add(CostAndStockInfo.builder()
                    .unitCost(30.0).costVatRate(18).quantity(50)
                    .stockDate(LocalDate.now().minusDays(10)).usedQuantity(0).build());
            costList.add(CostAndStockInfo.builder()
                    .unitCost(45.0).costVatRate(20).quantity(80)
                    .stockDate(LocalDate.now().minusDays(3)).usedQuantity(0).build());
            costList.add(CostAndStockInfo.builder()
                    .unitCost(40.0).costVatRate(18).quantity(60)
                    .stockDate(LocalDate.now().minusDays(7)).usedQuantity(0).build());

            TrendyolProduct product = TestDataBuilder.product(testStore)
                    .costAndStockInfo(costList).trendyolQuantity(300).build();
            product.setId(UUID.randomUUID());

            when(poItemRepository.findByProductIdAndStoreId(product.getId(), testStore.getId()))
                    .thenReturn(List.of());

            service.handleStockIncrease(product, 250, 300);

            ArgumentCaptor<AlertHistory> alertCaptor = ArgumentCaptor.forClass(AlertHistory.class);
            verify(alertHistoryRepository).save(alertCaptor.capture());
            Map<String, Object> data = alertCaptor.getValue().getData();
            assertThat(data.get("unitCost")).isEqualTo(45.0);
            assertThat(data.get("costVatRate")).isEqualTo(20);
            assertThat(data.get("hasCostInfo")).isEqualTo(true);
        }
    }

    @Nested
    @DisplayName("isExplainedByRecentPO")
    class IsExplainedByRecentPO {

        @Test
        @DisplayName("should return true when recent PO CLOSED with matching quantity")
        void shouldReturnTrueForRecentMatchingPO() {
            TrendyolProduct product = createProductWithCost(50.0, 20);

            PurchaseOrder po = PurchaseOrder.builder()
                    .status(PurchaseOrderStatus.CLOSED)
                    .updatedAt(LocalDateTime.now().minusHours(6))
                    .build();

            PurchaseOrderItem poItem = PurchaseOrderItem.builder()
                    .purchaseOrder(po)
                    .product(product)
                    .unitsOrdered(50)
                    .build();

            when(poItemRepository.findByProductIdAndStoreId(product.getId(), testStore.getId()))
                    .thenReturn(List.of(poItem));

            boolean result = service.isExplainedByRecentPO(product, 50);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return true when PO quantity within 20% tolerance")
        void shouldReturnTrueWithinTolerance() {
            TrendyolProduct product = createProductWithCost(50.0, 20);

            PurchaseOrder po = PurchaseOrder.builder()
                    .status(PurchaseOrderStatus.CLOSED)
                    .updatedAt(LocalDateTime.now().minusHours(12))
                    .build();

            PurchaseOrderItem poItem = PurchaseOrderItem.builder()
                    .purchaseOrder(po)
                    .product(product)
                    .unitsOrdered(55) // delta=50, ratio=50/55=0.91, within +/-20%
                    .build();

            when(poItemRepository.findByProductIdAndStoreId(product.getId(), testStore.getId()))
                    .thenReturn(List.of(poItem));

            boolean result = service.isExplainedByRecentPO(product, 50);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when PO quantity outside 20% tolerance")
        void shouldReturnFalseOutsideTolerance() {
            TrendyolProduct product = createProductWithCost(50.0, 20);

            PurchaseOrder po = PurchaseOrder.builder()
                    .status(PurchaseOrderStatus.CLOSED)
                    .updatedAt(LocalDateTime.now().minusHours(6))
                    .build();

            PurchaseOrderItem poItem = PurchaseOrderItem.builder()
                    .purchaseOrder(po)
                    .product(product)
                    .unitsOrdered(100) // delta=50, ratio=50/100=0.5, way outside +/-20%
                    .build();

            when(poItemRepository.findByProductIdAndStoreId(product.getId(), testStore.getId()))
                    .thenReturn(List.of(poItem));

            boolean result = service.isExplainedByRecentPO(product, 50);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when PO is older than 2 days")
        void shouldReturnFalseForOldPO() {
            TrendyolProduct product = createProductWithCost(50.0, 20);

            PurchaseOrder po = PurchaseOrder.builder()
                    .status(PurchaseOrderStatus.CLOSED)
                    .updatedAt(LocalDateTime.now().minusDays(3))
                    .build();

            PurchaseOrderItem poItem = PurchaseOrderItem.builder()
                    .purchaseOrder(po)
                    .product(product)
                    .unitsOrdered(50)
                    .build();

            when(poItemRepository.findByProductIdAndStoreId(product.getId(), testStore.getId()))
                    .thenReturn(List.of(poItem));

            boolean result = service.isExplainedByRecentPO(product, 50);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when PO status is not CLOSED")
        void shouldReturnFalseForNonClosedPO() {
            TrendyolProduct product = createProductWithCost(50.0, 20);

            PurchaseOrder po = PurchaseOrder.builder()
                    .status(PurchaseOrderStatus.ORDERED)
                    .updatedAt(LocalDateTime.now().minusHours(6))
                    .build();

            PurchaseOrderItem poItem = PurchaseOrderItem.builder()
                    .purchaseOrder(po)
                    .product(product)
                    .unitsOrdered(50)
                    .build();

            when(poItemRepository.findByProductIdAndStoreId(product.getId(), testStore.getId()))
                    .thenReturn(List.of(poItem));

            boolean result = service.isExplainedByRecentPO(product, 50);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when no PO items exist")
        void shouldReturnFalseWhenNoPOItems() {
            TrendyolProduct product = createProductWithCost(50.0, 20);

            when(poItemRepository.findByProductIdAndStoreId(product.getId(), testStore.getId()))
                    .thenReturn(List.of());

            boolean result = service.isExplainedByRecentPO(product, 50);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("handleStockIncrease - PO duplicate prevention")
    class DuplicatePrevention {

        @Test
        @DisplayName("should skip auto-detection when explained by recent PO")
        void shouldSkipWhenExplainedByPO() {
            TrendyolProduct product = createProductWithCost(50.0, 20);

            PurchaseOrder po = PurchaseOrder.builder()
                    .status(PurchaseOrderStatus.CLOSED)
                    .updatedAt(LocalDateTime.now().minusHours(6))
                    .build();

            PurchaseOrderItem poItem = PurchaseOrderItem.builder()
                    .purchaseOrder(po)
                    .product(product)
                    .unitsOrdered(50)
                    .build();

            when(poItemRepository.findByProductIdAndStoreId(product.getId(), testStore.getId()))
                    .thenReturn(List.of(poItem));

            service.handleStockIncrease(product, 250, 300);

            // Should not create alert
            assertThat(product.getCostAndStockInfo()).hasSize(1); // only the original
            verify(productRepository, never()).save(any());
            verify(alertHistoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getLastKnownCost")
    class GetLastKnownCost {

        @Test
        @DisplayName("should return null when no cost info exists")
        void shouldReturnNullWhenEmpty() {
            TrendyolProduct product = createProductWithoutCost();

            CostAndStockInfo result = service.getLastKnownCost(product);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null when cost info is null")
        void shouldReturnNullWhenNull() {
            TrendyolProduct product = TestDataBuilder.product(testStore)
                    .costAndStockInfo(null).build();
            product.setId(UUID.randomUUID());

            CostAndStockInfo result = service.getLastKnownCost(product);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should skip entries with null unitCost")
        void shouldSkipNullUnitCost() {
            List<CostAndStockInfo> costList = new ArrayList<>();
            costList.add(CostAndStockInfo.builder()
                    .unitCost(30.0).costVatRate(18).quantity(50)
                    .stockDate(LocalDate.now().minusDays(10)).usedQuantity(0).build());
            costList.add(CostAndStockInfo.builder()
                    .unitCost(null).costVatRate(20).quantity(80)
                    .stockDate(LocalDate.now().minusDays(1)).usedQuantity(0).build());

            TrendyolProduct product = TestDataBuilder.product(testStore)
                    .costAndStockInfo(costList).build();
            product.setId(UUID.randomUUID());

            CostAndStockInfo result = service.getLastKnownCost(product);

            assertThat(result).isNotNull();
            assertThat(result.getUnitCost()).isEqualTo(30.0);
        }

        @Test
        @DisplayName("should return latest entry by stockDate")
        void shouldReturnLatestByDate() {
            List<CostAndStockInfo> costList = new ArrayList<>();
            costList.add(CostAndStockInfo.builder()
                    .unitCost(30.0).costVatRate(18).quantity(50)
                    .stockDate(LocalDate.now().minusDays(10)).usedQuantity(0).build());
            costList.add(CostAndStockInfo.builder()
                    .unitCost(50.0).costVatRate(20).quantity(80)
                    .stockDate(LocalDate.now().minusDays(2)).usedQuantity(0).build());

            TrendyolProduct product = TestDataBuilder.product(testStore)
                    .costAndStockInfo(costList).build();
            product.setId(UUID.randomUUID());

            CostAndStockInfo result = service.getLastKnownCost(product);

            assertThat(result.getUnitCost()).isEqualTo(50.0);
        }
    }
}
