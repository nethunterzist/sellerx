package com.ecommerce.sellerx.purchasing;

import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.common.TestDataBuilder;
import com.ecommerce.sellerx.orders.StockOrderSynchronizationService;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("PurchaseOrderService")
class PurchaseOrderServiceTest extends BaseUnitTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private PurchaseOrderItemRepository purchaseOrderItemRepository;

    @Mock
    private TrendyolProductRepository productRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StockOrderSynchronizationService stockSyncService;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private PurchaseOrderAttachmentRepository attachmentRepository;

    private PurchaseOrderService service;

    private User testUser;
    private Store testStore;
    private UUID storeId;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetSequence();
        service = new PurchaseOrderService(
                purchaseOrderRepository,
                purchaseOrderItemRepository,
                productRepository,
                storeRepository,
                stockSyncService,
                supplierRepository,
                attachmentRepository
        );

        testUser = TestDataBuilder.user().build();
        testStore = TestDataBuilder.completedStore(testUser).build();
        storeId = testStore.getId();
    }

    @Nested
    @DisplayName("getPurchaseOrders")
    class GetPurchaseOrders {

        @Test
        @DisplayName("should return all purchase orders when no status filter")
        void shouldReturnAllOrdersWithoutStatusFilter() {
            PurchaseOrder po = createTestPO("PO-000001", PurchaseOrderStatus.DRAFT);
            when(purchaseOrderRepository.findByStoreIdOrderByPoDateDesc(storeId))
                    .thenReturn(List.of(po));

            List<PurchaseOrderSummaryDto> result = service.getPurchaseOrders(storeId, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPoNumber()).isEqualTo("PO-000001");
            verify(purchaseOrderRepository).findByStoreIdOrderByPoDateDesc(storeId);
            verify(purchaseOrderRepository, never())
                    .findByStoreIdAndStatusOrderByPoDateDesc(any(), any());
        }

        @Test
        @DisplayName("should return filtered purchase orders with status")
        void shouldReturnFilteredOrdersWithStatus() {
            PurchaseOrder po = createTestPO("PO-000001", PurchaseOrderStatus.ORDERED);
            when(purchaseOrderRepository.findByStoreIdAndStatusOrderByPoDateDesc(storeId, PurchaseOrderStatus.ORDERED))
                    .thenReturn(List.of(po));

            List<PurchaseOrderSummaryDto> result = service.getPurchaseOrders(storeId, PurchaseOrderStatus.ORDERED);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(PurchaseOrderStatus.ORDERED);
        }

        @Test
        @DisplayName("should return empty list when no purchase orders exist")
        void shouldReturnEmptyListWhenNoPOs() {
            when(purchaseOrderRepository.findByStoreIdOrderByPoDateDesc(storeId))
                    .thenReturn(Collections.emptyList());

            List<PurchaseOrderSummaryDto> result = service.getPurchaseOrders(storeId, null);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getPurchaseOrder")
    class GetPurchaseOrder {

        @Test
        @DisplayName("should return purchase order by ID")
        void shouldReturnPurchaseOrderById() {
            PurchaseOrder po = createTestPO("PO-000001", PurchaseOrderStatus.DRAFT);
            when(purchaseOrderRepository.findByStoreIdAndId(storeId, 1L))
                    .thenReturn(Optional.of(po));

            PurchaseOrderDto result = service.getPurchaseOrder(storeId, 1L);

            assertThat(result.getPoNumber()).isEqualTo("PO-000001");
            assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.DRAFT);
        }

        @Test
        @DisplayName("should throw PurchaseOrderNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(purchaseOrderRepository.findByStoreIdAndId(storeId, 999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getPurchaseOrder(storeId, 999L))
                    .isInstanceOf(PurchaseOrderNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("createPurchaseOrder")
    class CreatePurchaseOrder {

        @Test
        @DisplayName("should create purchase order with generated PO number")
        void shouldCreatePOWithGeneratedNumber() {
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(purchaseOrderRepository.countByStoreId(storeId)).thenReturn(5L);

            CreatePurchaseOrderRequest request = CreatePurchaseOrderRequest.builder()
                    .supplierName("Test Supplier")
                    .comment("Test order")
                    .build();

            ArgumentCaptor<PurchaseOrder> captor = ArgumentCaptor.forClass(PurchaseOrder.class);
            when(purchaseOrderRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.createPurchaseOrder(storeId, request);

            PurchaseOrder saved = captor.getValue();
            assertThat(saved.getPoNumber()).isEqualTo("PO-000006");
            assertThat(saved.getStatus()).isEqualTo(PurchaseOrderStatus.DRAFT);
            assertThat(saved.getSupplierName()).isEqualTo("Test Supplier");
            assertThat(saved.getSupplierCurrency()).isEqualTo("TRY");
        }

        @Test
        @DisplayName("should create PO with supplier reference when supplierId provided")
        void shouldCreatePOWithSupplierReference() {
            Supplier supplier = createTestSupplier(1L, "Supplier A");
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(purchaseOrderRepository.countByStoreId(storeId)).thenReturn(0L);
            when(supplierRepository.findByStoreIdAndId(storeId, 1L))
                    .thenReturn(Optional.of(supplier));

            CreatePurchaseOrderRequest request = CreatePurchaseOrderRequest.builder()
                    .supplierId(1L)
                    .build();

            ArgumentCaptor<PurchaseOrder> captor = ArgumentCaptor.forClass(PurchaseOrder.class);
            when(purchaseOrderRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.createPurchaseOrder(storeId, request);

            PurchaseOrder saved = captor.getValue();
            assertThat(saved.getSupplier()).isEqualTo(supplier);
            assertThat(saved.getSupplierName()).isEqualTo("Supplier A");
        }

        @Test
        @DisplayName("should use provided poDate or default to today")
        void shouldUseProvidedDateOrDefault() {
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(purchaseOrderRepository.countByStoreId(storeId)).thenReturn(0L);

            CreatePurchaseOrderRequest request = CreatePurchaseOrderRequest.builder()
                    .supplierName("Test")
                    .poDate(LocalDate.of(2025, 6, 15))
                    .build();

            ArgumentCaptor<PurchaseOrder> captor = ArgumentCaptor.forClass(PurchaseOrder.class);
            when(purchaseOrderRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.createPurchaseOrder(storeId, request);

            assertThat(captor.getValue().getPoDate()).isEqualTo(LocalDate.of(2025, 6, 15));
        }

        @Test
        @DisplayName("should throw when store not found")
        void shouldThrowWhenStoreNotFound() {
            when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

            CreatePurchaseOrderRequest request = CreatePurchaseOrderRequest.builder().build();

            assertThatThrownBy(() -> service.createPurchaseOrder(storeId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Store not found");
        }

        @Test
        @DisplayName("should throw when supplier not found")
        void shouldThrowWhenSupplierNotFound() {
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(purchaseOrderRepository.countByStoreId(storeId)).thenReturn(0L);
            when(supplierRepository.findByStoreIdAndId(storeId, 999L))
                    .thenReturn(Optional.empty());

            CreatePurchaseOrderRequest request = CreatePurchaseOrderRequest.builder()
                    .supplierId(999L)
                    .build();

            assertThatThrownBy(() -> service.createPurchaseOrder(storeId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Supplier not found");
        }
    }

    @Nested
    @DisplayName("updatePurchaseOrder")
    class UpdatePurchaseOrder {

        @Test
        @DisplayName("should update only provided fields")
        void shouldUpdateOnlyProvidedFields() {
            PurchaseOrder po = createTestPO("PO-000001", PurchaseOrderStatus.DRAFT);
            po.setSupplierName("Original Supplier");
            po.setComment("Original Comment");

            when(purchaseOrderRepository.findByStoreIdAndId(storeId, 1L))
                    .thenReturn(Optional.of(po));
            when(purchaseOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdatePurchaseOrderRequest request = UpdatePurchaseOrderRequest.builder()
                    .comment("Updated Comment")
                    .build();

            PurchaseOrderDto result = service.updatePurchaseOrder(storeId, 1L, request);

            assertThat(result.getComment()).isEqualTo("Updated Comment");
            assertThat(result.getSupplierName()).isEqualTo("Original Supplier");
        }

        @Test
        @DisplayName("should update supplier reference and name")
        void shouldUpdateSupplierReference() {
            PurchaseOrder po = createTestPO("PO-000001", PurchaseOrderStatus.DRAFT);
            Supplier supplier = createTestSupplier(2L, "New Supplier");

            when(purchaseOrderRepository.findByStoreIdAndId(storeId, 1L))
                    .thenReturn(Optional.of(po));
            when(supplierRepository.findByStoreIdAndId(storeId, 2L))
                    .thenReturn(Optional.of(supplier));
            when(purchaseOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdatePurchaseOrderRequest request = UpdatePurchaseOrderRequest.builder()
                    .supplierId(2L)
                    .build();

            PurchaseOrderDto result = service.updatePurchaseOrder(storeId, 1L, request);

            assertThat(result.getSupplierName()).isEqualTo("New Supplier");
        }

        @Test
        @DisplayName("should throw when PO not found")
        void shouldThrowWhenPONotFound() {
            when(purchaseOrderRepository.findByStoreIdAndId(storeId, 999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updatePurchaseOrder(storeId, 999L,
                    UpdatePurchaseOrderRequest.builder().build()))
                    .isInstanceOf(PurchaseOrderNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deletePurchaseOrder")
    class DeletePurchaseOrder {

        @Test
        @DisplayName("should delete existing purchase order")
        void shouldDeleteExistingPO() {
            PurchaseOrder po = createTestPO("PO-000001", PurchaseOrderStatus.DRAFT);
            when(purchaseOrderRepository.findByStoreIdAndId(storeId, 1L))
                    .thenReturn(Optional.of(po));

            service.deletePurchaseOrder(storeId, 1L);

            verify(purchaseOrderRepository).delete(po);
        }

        @Test
        @DisplayName("should throw when PO not found on delete")
        void shouldThrowWhenPONotFoundOnDelete() {
            when(purchaseOrderRepository.findByStoreIdAndId(storeId, 999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deletePurchaseOrder(storeId, 999L))
                    .isInstanceOf(PurchaseOrderNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("should update status from DRAFT to ORDERED")
        void shouldUpdateStatusFromDraftToOrdered() {
            PurchaseOrder po = createTestPO("PO-000001", PurchaseOrderStatus.DRAFT);
            when(purchaseOrderRepository.findByStoreIdAndId(storeId, 1L))
                    .thenReturn(Optional.of(po));
            when(purchaseOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PurchaseOrderDto result = service.updateStatus(storeId, 1L, PurchaseOrderStatus.ORDERED);

            assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.ORDERED);
            verify(stockSyncService, never()).redistributeStockFIFO(any(), any());
        }

        @Test
        @DisplayName("should trigger product cost update when closing PO")
        void shouldTriggerCostUpdateWhenClosingPO() {
            TrendyolProduct product = TestDataBuilder.product(testStore).build();
            product.setCostAndStockInfo(new ArrayList<>());
            product.setVatRate(18);

            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .product(product)
                    .unitsOrdered(100)
                    .manufacturingCostPerUnit(new BigDecimal("50.00"))
                    .transportationCostPerUnit(new BigDecimal("5.00"))
                    .costVatRate(18)
                    .build();

            PurchaseOrder po = createTestPO("PO-000001", PurchaseOrderStatus.SHIPPED);
            po.setItems(new ArrayList<>(List.of(item)));

            when(purchaseOrderRepository.findByStoreIdAndId(storeId, 1L))
                    .thenReturn(Optional.of(po));
            when(purchaseOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.updateStatus(storeId, 1L, PurchaseOrderStatus.CLOSED);

            verify(productRepository).save(product);
            assertThat(product.getCostAndStockInfo()).hasSize(1);
            assertThat(product.getCostAndStockInfo().get(0).getUnitCost()).isEqualTo(55.0);
            assertThat(product.getCostAndStockInfo().get(0).getQuantity()).isEqualTo(100);
            verify(stockSyncService).redistributeStockFIFO(eq(storeId), any(LocalDate.class));
        }

        @Test
        @DisplayName("should not trigger cost update when already CLOSED")
        void shouldNotTriggerCostUpdateWhenAlreadyClosed() {
            PurchaseOrder po = createTestPO("PO-000001", PurchaseOrderStatus.CLOSED);
            when(purchaseOrderRepository.findByStoreIdAndId(storeId, 1L))
                    .thenReturn(Optional.of(po));
            when(purchaseOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.updateStatus(storeId, 1L, PurchaseOrderStatus.CLOSED);

            verify(productRepository, never()).save(any());
            verify(stockSyncService, never()).redistributeStockFIFO(any(), any());
        }
    }

    @Nested
    @DisplayName("addItem")
    class AddItem {

        @Test
        @DisplayName("should add item to purchase order and recalculate totals")
        void shouldAddItemAndRecalculateTotals() {
            PurchaseOrder po = createTestPO("PO-000001", PurchaseOrderStatus.DRAFT);
            TrendyolProduct product = TestDataBuilder.product(testStore).build();
            product.setVatRate(18);

            when(purchaseOrderRepository.findByStoreIdAndId(storeId, 1L))
                    .thenReturn(Optional.of(po));
            when(productRepository.findById(product.getId()))
                    .thenReturn(Optional.of(product));
            when(purchaseOrderItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(purchaseOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AddPurchaseOrderItemRequest request = AddPurchaseOrderItemRequest.builder()
                    .productId(product.getId())
                    .unitsOrdered(50)
                    .manufacturingCostPerUnit(new BigDecimal("100.00"))
                    .transportationCostPerUnit(new BigDecimal("10.00"))
                    .build();

            PurchaseOrderDto result = service.addItem(storeId, 1L, request);

            verify(purchaseOrderItemRepository).save(any(PurchaseOrderItem.class));
            assertThat(result.getTotalUnits()).isEqualTo(50);
            // Total cost: (100 + 10) * 50 = 5500
            assertThat(result.getTotalCost()).isEqualByComparingTo(new BigDecimal("5500.00"));
        }

        @Test
        @DisplayName("should default transportation cost to zero when null")
        void shouldDefaultTransportationCostToZero() {
            PurchaseOrder po = createTestPO("PO-000001", PurchaseOrderStatus.DRAFT);
            TrendyolProduct product = TestDataBuilder.product(testStore).build();

            when(purchaseOrderRepository.findByStoreIdAndId(storeId, 1L))
                    .thenReturn(Optional.of(po));
            when(productRepository.findById(product.getId()))
                    .thenReturn(Optional.of(product));

            ArgumentCaptor<PurchaseOrderItem> captor = ArgumentCaptor.forClass(PurchaseOrderItem.class);
            when(purchaseOrderItemRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
            when(purchaseOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AddPurchaseOrderItemRequest request = AddPurchaseOrderItemRequest.builder()
                    .productId(product.getId())
                    .unitsOrdered(10)
                    .manufacturingCostPerUnit(new BigDecimal("25.00"))
                    .build();

            service.addItem(storeId, 1L, request);

            assertThat(captor.getValue().getTransportationCostPerUnit())
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should throw when product not found")
        void shouldThrowWhenProductNotFound() {
            PurchaseOrder po = createTestPO("PO-000001", PurchaseOrderStatus.DRAFT);
            UUID productId = UUID.randomUUID();

            when(purchaseOrderRepository.findByStoreIdAndId(storeId, 1L))
                    .thenReturn(Optional.of(po));
            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            AddPurchaseOrderItemRequest request = AddPurchaseOrderItemRequest.builder()
                    .productId(productId)
                    .unitsOrdered(10)
                    .manufacturingCostPerUnit(BigDecimal.TEN)
                    .build();

            assertThatThrownBy(() -> service.addItem(storeId, 1L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Product not found");
        }
    }

    @Nested
    @DisplayName("removeItem")
    class RemoveItem {

        @Test
        @DisplayName("should remove item and recalculate totals")
        void shouldRemoveItemAndRecalculateTotals() {
            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .id(10L)
                    .unitsOrdered(20)
                    .manufacturingCostPerUnit(new BigDecimal("30.00"))
                    .transportationCostPerUnit(BigDecimal.ZERO)
                    .build();

            PurchaseOrder po = createTestPO("PO-000001", PurchaseOrderStatus.DRAFT);
            po.setItems(new ArrayList<>(List.of(item)));
            po.setTotalUnits(20);
            po.setTotalCost(new BigDecimal("600.00"));

            when(purchaseOrderRepository.findByStoreIdAndId(storeId, 1L))
                    .thenReturn(Optional.of(po));
            when(purchaseOrderItemRepository.findByPurchaseOrderIdAndId(1L, 10L))
                    .thenReturn(Optional.of(item));
            when(purchaseOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PurchaseOrderDto result = service.removeItem(storeId, 1L, 10L);

            verify(purchaseOrderItemRepository).delete(item);
            assertThat(result.getTotalUnits()).isZero();
            assertThat(result.getTotalCost()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should throw when item not found")
        void shouldThrowWhenItemNotFound() {
            PurchaseOrder po = createTestPO("PO-000001", PurchaseOrderStatus.DRAFT);
            when(purchaseOrderRepository.findByStoreIdAndId(storeId, 1L))
                    .thenReturn(Optional.of(po));
            when(purchaseOrderItemRepository.findByPurchaseOrderIdAndId(1L, 999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.removeItem(storeId, 1L, 999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Item not found");
        }
    }

    @Nested
    @DisplayName("duplicatePurchaseOrder")
    class DuplicatePurchaseOrder {

        @Test
        @DisplayName("should duplicate PO with new number and DRAFT status")
        void shouldDuplicatePOWithNewNumberAndDraftStatus() {
            TrendyolProduct product = TestDataBuilder.product(testStore).build();
            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .id(1L)
                    .product(product)
                    .unitsOrdered(50)
                    .manufacturingCostPerUnit(new BigDecimal("100.00"))
                    .transportationCostPerUnit(new BigDecimal("5.00"))
                    .costVatRate(18)
                    .build();

            PurchaseOrder original = createTestPO("PO-000001", PurchaseOrderStatus.ORDERED);
            original.setItems(new ArrayList<>(List.of(item)));
            original.setSupplierName("Supplier X");

            when(purchaseOrderRepository.findByStoreIdAndId(storeId, 1L))
                    .thenReturn(Optional.of(original));
            when(purchaseOrderRepository.countByStoreId(storeId)).thenReturn(3L);

            ArgumentCaptor<PurchaseOrder> poCaptor = ArgumentCaptor.forClass(PurchaseOrder.class);
            when(purchaseOrderRepository.save(poCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));
            when(purchaseOrderItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.duplicatePurchaseOrder(storeId, 1L);

            List<PurchaseOrder> savedPOs = poCaptor.getAllValues();
            // First save is the new PO, second is after recalculate
            PurchaseOrder duplicate = savedPOs.get(0);
            assertThat(duplicate.getPoNumber()).isEqualTo("PO-000004");
            assertThat(duplicate.getStatus()).isEqualTo(PurchaseOrderStatus.DRAFT);
            assertThat(duplicate.getSupplierName()).isEqualTo("Supplier X");
            assertThat(duplicate.getPoDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("should copy items to duplicated PO")
        void shouldCopyItemsToDuplicatedPO() {
            TrendyolProduct product = TestDataBuilder.product(testStore).build();
            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .id(1L)
                    .product(product)
                    .unitsOrdered(25)
                    .manufacturingCostPerUnit(new BigDecimal("80.00"))
                    .transportationCostPerUnit(new BigDecimal("10.00"))
                    .costVatRate(20)
                    .hsCode("1234.56")
                    .build();

            PurchaseOrder original = createTestPO("PO-000001", PurchaseOrderStatus.DRAFT);
            original.setItems(new ArrayList<>(List.of(item)));

            when(purchaseOrderRepository.findByStoreIdAndId(storeId, 1L))
                    .thenReturn(Optional.of(original));
            when(purchaseOrderRepository.countByStoreId(storeId)).thenReturn(1L);
            when(purchaseOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<PurchaseOrderItem> itemCaptor = ArgumentCaptor.forClass(PurchaseOrderItem.class);
            when(purchaseOrderItemRepository.save(itemCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.duplicatePurchaseOrder(storeId, 1L);

            PurchaseOrderItem copiedItem = itemCaptor.getValue();
            assertThat(copiedItem.getUnitsOrdered()).isEqualTo(25);
            assertThat(copiedItem.getManufacturingCostPerUnit())
                    .isEqualByComparingTo(new BigDecimal("80.00"));
            assertThat(copiedItem.getHsCode()).isEqualTo("1234.56");
        }
    }

    @Nested
    @DisplayName("splitPurchaseOrder")
    class SplitPurchaseOrder {

        @Test
        @DisplayName("should throw when no item IDs provided")
        void shouldThrowWhenNoItemIds() {
            PurchaseOrder po = createTestPO("PO-000001", PurchaseOrderStatus.DRAFT);
            when(purchaseOrderRepository.findByStoreIdAndId(storeId, 1L))
                    .thenReturn(Optional.of(po));

            assertThatThrownBy(() -> service.splitPurchaseOrder(storeId, 1L, List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("At least one item ID");
        }

        @Test
        @DisplayName("should throw when null item IDs provided")
        void shouldThrowWhenNullItemIds() {
            PurchaseOrder po = createTestPO("PO-000001", PurchaseOrderStatus.DRAFT);
            when(purchaseOrderRepository.findByStoreIdAndId(storeId, 1L))
                    .thenReturn(Optional.of(po));

            assertThatThrownBy(() -> service.splitPurchaseOrder(storeId, 1L, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("At least one item ID");
        }

        @Test
        @DisplayName("should create new PO with selected items moved from original")
        void shouldCreateNewPOWithSelectedItems() {
            TrendyolProduct product1 = TestDataBuilder.product(testStore).build();
            TrendyolProduct product2 = TestDataBuilder.product(testStore).build();

            PurchaseOrderItem item1 = PurchaseOrderItem.builder()
                    .id(10L)
                    .product(product1)
                    .unitsOrdered(30)
                    .manufacturingCostPerUnit(new BigDecimal("40.00"))
                    .transportationCostPerUnit(BigDecimal.ZERO)
                    .build();
            PurchaseOrderItem item2 = PurchaseOrderItem.builder()
                    .id(20L)
                    .product(product2)
                    .unitsOrdered(15)
                    .manufacturingCostPerUnit(new BigDecimal("60.00"))
                    .transportationCostPerUnit(BigDecimal.ZERO)
                    .build();

            PurchaseOrder original = createTestPO("PO-000001", PurchaseOrderStatus.ORDERED);
            original.setItems(new ArrayList<>(List.of(item1, item2)));

            when(purchaseOrderRepository.findByStoreIdAndId(storeId, 1L))
                    .thenReturn(Optional.of(original));
            when(purchaseOrderRepository.countByStoreId(storeId)).thenReturn(5L);
            when(purchaseOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(purchaseOrderItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PurchaseOrderDto result = service.splitPurchaseOrder(storeId, 1L, List.of(20L));

            // New PO should have item2, original should keep item1
            assertThat(result.getComment()).contains("Split from PO-000001");
            assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.ORDERED);
            assertThat(original.getItems()).hasSize(1);
            assertThat(original.getItems().get(0).getId()).isEqualTo(10L);
        }
    }

    @Nested
    @DisplayName("getStats")
    class GetStats {

        @Test
        @DisplayName("should aggregate stats for all statuses")
        void shouldAggregateStatsForAllStatuses() {
            when(purchaseOrderRepository.countByStoreIdAndStatus(storeId, PurchaseOrderStatus.DRAFT)).thenReturn(3L);
            when(purchaseOrderRepository.sumTotalCostByStoreIdAndStatus(storeId, PurchaseOrderStatus.DRAFT))
                    .thenReturn(new BigDecimal("1500.00"));
            when(purchaseOrderRepository.sumTotalUnitsByStoreIdAndStatus(storeId, PurchaseOrderStatus.DRAFT)).thenReturn(100L);

            when(purchaseOrderRepository.countByStoreIdAndStatus(storeId, PurchaseOrderStatus.ORDERED)).thenReturn(2L);
            when(purchaseOrderRepository.sumTotalCostByStoreIdAndStatus(storeId, PurchaseOrderStatus.ORDERED))
                    .thenReturn(new BigDecimal("3000.00"));
            when(purchaseOrderRepository.sumTotalUnitsByStoreIdAndStatus(storeId, PurchaseOrderStatus.ORDERED)).thenReturn(200L);

            when(purchaseOrderRepository.countByStoreIdAndStatus(storeId, PurchaseOrderStatus.SHIPPED)).thenReturn(1L);
            when(purchaseOrderRepository.sumTotalCostByStoreIdAndStatus(storeId, PurchaseOrderStatus.SHIPPED))
                    .thenReturn(new BigDecimal("500.00"));
            when(purchaseOrderRepository.sumTotalUnitsByStoreIdAndStatus(storeId, PurchaseOrderStatus.SHIPPED)).thenReturn(50L);

            when(purchaseOrderRepository.countByStoreIdAndStatus(storeId, PurchaseOrderStatus.CLOSED)).thenReturn(10L);
            when(purchaseOrderRepository.sumTotalCostByStoreIdAndStatus(storeId, PurchaseOrderStatus.CLOSED))
                    .thenReturn(new BigDecimal("25000.00"));
            when(purchaseOrderRepository.sumTotalUnitsByStoreIdAndStatus(storeId, PurchaseOrderStatus.CLOSED)).thenReturn(5000L);

            PurchaseOrderStatsDto stats = service.getStats(storeId);

            assertThat(stats.getDraft().getCount()).isEqualTo(3);
            assertThat(stats.getDraft().getTotalCost()).isEqualByComparingTo(new BigDecimal("1500.00"));
            assertThat(stats.getOrdered().getCount()).isEqualTo(2);
            assertThat(stats.getShipped().getTotalUnits()).isEqualTo(50);
            assertThat(stats.getClosed().getCount()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("searchPurchaseOrders")
    class SearchPurchaseOrders {

        @Test
        @DisplayName("should search by term and apply status filter")
        void shouldSearchByTermAndApplyStatusFilter() {
            PurchaseOrder po = createTestPO("PO-000001", PurchaseOrderStatus.DRAFT);
            when(purchaseOrderRepository.searchByTerm(storeId, "PO-000001"))
                    .thenReturn(List.of(po));

            List<PurchaseOrderSummaryDto> result = service.searchPurchaseOrders(
                    storeId, "PO-000001", PurchaseOrderStatus.DRAFT, null);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("should filter by supplier and status without search term")
        void shouldFilterBySupplierAndStatus() {
            PurchaseOrder po = createTestPO("PO-000001", PurchaseOrderStatus.ORDERED);
            when(purchaseOrderRepository.findByStoreIdAndSupplierIdAndStatus(storeId, 1L, PurchaseOrderStatus.ORDERED))
                    .thenReturn(List.of(po));

            List<PurchaseOrderSummaryDto> result = service.searchPurchaseOrders(
                    storeId, null, PurchaseOrderStatus.ORDERED, 1L);

            assertThat(result).hasSize(1);
            verify(purchaseOrderRepository).findByStoreIdAndSupplierIdAndStatus(storeId, 1L, PurchaseOrderStatus.ORDERED);
        }

        @Test
        @DisplayName("should return all orders when no filters provided")
        void shouldReturnAllWhenNoFilters() {
            when(purchaseOrderRepository.findByStoreIdOrderByPoDateDesc(storeId))
                    .thenReturn(Collections.emptyList());

            List<PurchaseOrderSummaryDto> result = service.searchPurchaseOrders(
                    storeId, null, null, null);

            verify(purchaseOrderRepository).findByStoreIdOrderByPoDateDesc(storeId);
        }
    }

    @Nested
    @DisplayName("attachments")
    class Attachments {

        @Test
        @DisplayName("should add attachment to purchase order")
        void shouldAddAttachment() {
            PurchaseOrder po = createTestPO("PO-000001", PurchaseOrderStatus.DRAFT);
            when(purchaseOrderRepository.findByStoreIdAndId(storeId, 1L))
                    .thenReturn(Optional.of(po));

            ArgumentCaptor<PurchaseOrderAttachment> captor =
                    ArgumentCaptor.forClass(PurchaseOrderAttachment.class);
            when(attachmentRepository.save(captor.capture())).thenAnswer(inv -> {
                PurchaseOrderAttachment att = inv.getArgument(0);
                att.setId(100L);
                return att;
            });

            AttachmentDto result = service.addAttachment(storeId, 1L,
                    "invoice.pdf", "application/pdf", 1024L, new byte[]{1, 2, 3});

            assertThat(captor.getValue().getFileName()).isEqualTo("invoice.pdf");
            assertThat(captor.getValue().getFileSize()).isEqualTo(1024L);
        }

        @Test
        @DisplayName("should delete attachment")
        void shouldDeleteAttachment() {
            PurchaseOrder po = createTestPO("PO-000001", PurchaseOrderStatus.DRAFT);
            PurchaseOrderAttachment attachment = PurchaseOrderAttachment.builder()
                    .id(100L)
                    .purchaseOrder(po)
                    .fileName("test.pdf")
                    .build();

            when(purchaseOrderRepository.findByStoreIdAndId(storeId, 1L))
                    .thenReturn(Optional.of(po));
            when(attachmentRepository.findByPurchaseOrderIdAndId(1L, 100L))
                    .thenReturn(Optional.of(attachment));

            service.deleteAttachment(storeId, 1L, 100L);

            verify(attachmentRepository).delete(attachment);
        }

        @Test
        @DisplayName("should throw when attachment not found on delete")
        void shouldThrowWhenAttachmentNotFound() {
            PurchaseOrder po = createTestPO("PO-000001", PurchaseOrderStatus.DRAFT);
            when(purchaseOrderRepository.findByStoreIdAndId(storeId, 1L))
                    .thenReturn(Optional.of(po));
            when(attachmentRepository.findByPurchaseOrderIdAndId(1L, 999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteAttachment(storeId, 1L, 999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Attachment not found");
        }
    }

    @Nested
    @DisplayName("stockEntryDate Propagation")
    class StockEntryDatePropagation {

        @Test
        @DisplayName("should use PO-level stockEntryDate when closing PO")
        void shouldUsePOLevelStockEntryDate() {
            TrendyolProduct product = TestDataBuilder.product(testStore).build();
            product.setCostAndStockInfo(new ArrayList<>());
            product.setVatRate(18);

            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .product(product)
                    .unitsOrdered(50)
                    .manufacturingCostPerUnit(new BigDecimal("30.00"))
                    .transportationCostPerUnit(new BigDecimal("5.00"))
                    .costVatRate(18)
                    .build();

            PurchaseOrder po = createTestPO("PO-SE-001", PurchaseOrderStatus.SHIPPED);
            po.setStockEntryDate(LocalDate.of(2025, 3, 10)); // PO-level stockEntryDate
            po.setItems(new ArrayList<>(List.of(item)));

            when(purchaseOrderRepository.findByStoreIdAndId(storeId, 1L))
                    .thenReturn(Optional.of(po));
            when(purchaseOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.updateStatus(storeId, 1L, PurchaseOrderStatus.CLOSED);

            // Verify cost entry uses PO-level stockEntryDate
            assertThat(product.getCostAndStockInfo()).hasSize(1);
            assertThat(product.getCostAndStockInfo().get(0).getStockDate())
                    .isEqualTo(LocalDate.of(2025, 3, 10));
            assertThat(product.getCostAndStockInfo().get(0).getUnitCost()).isEqualTo(35.0);
        }

        @Test
        @DisplayName("should use item-level stockEntryDate override when available")
        void shouldUseItemLevelStockEntryDateOverride() {
            TrendyolProduct product = TestDataBuilder.product(testStore).build();
            product.setCostAndStockInfo(new ArrayList<>());
            product.setVatRate(18);

            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .product(product)
                    .unitsOrdered(25)
                    .manufacturingCostPerUnit(new BigDecimal("40.00"))
                    .transportationCostPerUnit(BigDecimal.ZERO)
                    .costVatRate(18)
                    .stockEntryDate(LocalDate.of(2025, 4, 5)) // Item-level override
                    .build();

            PurchaseOrder po = createTestPO("PO-SE-002", PurchaseOrderStatus.SHIPPED);
            po.setStockEntryDate(LocalDate.of(2025, 3, 1)); // PO-level (should be overridden)
            po.setItems(new ArrayList<>(List.of(item)));

            when(purchaseOrderRepository.findByStoreIdAndId(storeId, 1L))
                    .thenReturn(Optional.of(po));
            when(purchaseOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.updateStatus(storeId, 1L, PurchaseOrderStatus.CLOSED);

            // Verify item-level date wins over PO-level
            assertThat(product.getCostAndStockInfo()).hasSize(1);
            assertThat(product.getCostAndStockInfo().get(0).getStockDate())
                    .isEqualTo(LocalDate.of(2025, 4, 5));
        }

        @Test
        @DisplayName("should fall back to poDate when no stockEntryDate is set")
        void shouldFallBackToPoDate() {
            TrendyolProduct product = TestDataBuilder.product(testStore).build();
            product.setCostAndStockInfo(new ArrayList<>());
            product.setVatRate(18);

            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .product(product)
                    .unitsOrdered(10)
                    .manufacturingCostPerUnit(new BigDecimal("20.00"))
                    .transportationCostPerUnit(BigDecimal.ZERO)
                    .costVatRate(18)
                    .build();

            PurchaseOrder po = createTestPO("PO-SE-003", PurchaseOrderStatus.SHIPPED);
            po.setPoDate(LocalDate.of(2025, 2, 15));
            // No stockEntryDate set on PO or item
            po.setItems(new ArrayList<>(List.of(item)));

            when(purchaseOrderRepository.findByStoreIdAndId(storeId, 1L))
                    .thenReturn(Optional.of(po));
            when(purchaseOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.updateStatus(storeId, 1L, PurchaseOrderStatus.CLOSED);

            // Verify falls back to poDate
            assertThat(product.getCostAndStockInfo()).hasSize(1);
            assertThat(product.getCostAndStockInfo().get(0).getStockDate())
                    .isEqualTo(LocalDate.of(2025, 2, 15));
        }

        @Test
        @DisplayName("should trigger FIFO redistribution with earliest effective date")
        void shouldTriggerFifoWithEarliestEffectiveDate() {
            TrendyolProduct product1 = TestDataBuilder.product(testStore).build();
            product1.setCostAndStockInfo(new ArrayList<>());
            product1.setVatRate(18);

            TrendyolProduct product2 = TestDataBuilder.product(testStore).build();
            product2.setCostAndStockInfo(new ArrayList<>());
            product2.setVatRate(18);

            PurchaseOrderItem item1 = PurchaseOrderItem.builder()
                    .product(product1)
                    .unitsOrdered(10)
                    .manufacturingCostPerUnit(new BigDecimal("10.00"))
                    .transportationCostPerUnit(BigDecimal.ZERO)
                    .costVatRate(18)
                    .stockEntryDate(LocalDate.of(2025, 3, 1)) // Earlier date
                    .build();

            PurchaseOrderItem item2 = PurchaseOrderItem.builder()
                    .product(product2)
                    .unitsOrdered(20)
                    .manufacturingCostPerUnit(new BigDecimal("15.00"))
                    .transportationCostPerUnit(BigDecimal.ZERO)
                    .costVatRate(18)
                    .stockEntryDate(LocalDate.of(2025, 5, 1)) // Later date
                    .build();

            PurchaseOrder po = createTestPO("PO-SE-004", PurchaseOrderStatus.SHIPPED);
            po.setItems(new ArrayList<>(List.of(item1, item2)));

            when(purchaseOrderRepository.findByStoreIdAndId(storeId, 1L))
                    .thenReturn(Optional.of(po));
            when(purchaseOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.updateStatus(storeId, 1L, PurchaseOrderStatus.CLOSED);

            // Verify FIFO is triggered with the EARLIEST date (March 1)
            ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
            verify(stockSyncService).redistributeStockFIFO(eq(storeId), dateCaptor.capture());
            assertThat(dateCaptor.getValue()).isEqualTo(LocalDate.of(2025, 3, 1));
        }

        @Test
        @DisplayName("should copy stockEntryDate when duplicating PO")
        void shouldCopyStockEntryDateOnDuplicate() {
            TrendyolProduct product = TestDataBuilder.product(testStore).build();
            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .id(1L)
                    .product(product)
                    .unitsOrdered(50)
                    .manufacturingCostPerUnit(new BigDecimal("100.00"))
                    .transportationCostPerUnit(BigDecimal.ZERO)
                    .costVatRate(18)
                    .stockEntryDate(LocalDate.of(2025, 6, 1)) // Item-level date
                    .build();

            PurchaseOrder original = createTestPO("PO-DUP-001", PurchaseOrderStatus.CLOSED);
            original.setStockEntryDate(LocalDate.of(2025, 5, 15)); // PO-level date
            original.setItems(new ArrayList<>(List.of(item)));

            when(purchaseOrderRepository.findByStoreIdAndId(storeId, 1L))
                    .thenReturn(Optional.of(original));
            when(purchaseOrderRepository.countByStoreId(storeId)).thenReturn(1L);

            ArgumentCaptor<PurchaseOrder> poCaptor = ArgumentCaptor.forClass(PurchaseOrder.class);
            when(purchaseOrderRepository.save(poCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<PurchaseOrderItem> itemCaptor = ArgumentCaptor.forClass(PurchaseOrderItem.class);
            when(purchaseOrderItemRepository.save(itemCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.duplicatePurchaseOrder(storeId, 1L);

            // Verify PO-level stockEntryDate copied
            PurchaseOrder duplicatedPO = poCaptor.getAllValues().get(0);
            assertThat(duplicatedPO.getStockEntryDate()).isEqualTo(LocalDate.of(2025, 5, 15));

            // Verify item-level stockEntryDate copied
            PurchaseOrderItem duplicatedItem = itemCaptor.getValue();
            assertThat(duplicatedItem.getStockEntryDate()).isEqualTo(LocalDate.of(2025, 6, 1));
        }
    }

    // === Helper Methods ===

    private PurchaseOrder createTestPO(String poNumber, PurchaseOrderStatus status) {
        return PurchaseOrder.builder()
                .id(1L)
                .store(testStore)
                .poNumber(poNumber)
                .poDate(LocalDate.now())
                .status(status)
                .supplierCurrency("TRY")
                .totalCost(BigDecimal.ZERO)
                .totalUnits(0)
                .items(new ArrayList<>())
                .build();
    }

    private Supplier createTestSupplier(Long id, String name) {
        return Supplier.builder()
                .id(id)
                .store(testStore)
                .name(name)
                .currency("TRY")
                .build();
    }
}
