package com.ecommerce.sellerx.financial;

import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.financial.dto.InvoiceSummaryDto;
import com.ecommerce.sellerx.financial.dto.PurchaseVatDto;
import com.ecommerce.sellerx.orders.TrendyolOrder;
import com.ecommerce.sellerx.orders.TrendyolOrderRepository;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import com.ecommerce.sellerx.purchasing.PurchaseOrder;
import com.ecommerce.sellerx.purchasing.PurchaseOrderItem;
import com.ecommerce.sellerx.purchasing.PurchaseOrderRepository;
import com.ecommerce.sellerx.purchasing.PurchaseOrderStatus;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("TrendyolInvoiceService - Purchase VAT Calculation")
class TrendyolInvoiceServicePurchaseVatTest extends BaseUnitTest {

    @Mock
    private TrendyolDeductionInvoiceRepository deductionInvoiceRepository;
    @Mock
    private TrendyolInvoiceRepository invoiceRepository;
    @Mock
    private TrendyolCargoInvoiceRepository cargoInvoiceRepository;
    @Mock
    private TrendyolOrderRepository orderRepository;
    @Mock
    private TrendyolProductRepository productRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @InjectMocks
    private TrendyolInvoiceService invoiceService;

    private UUID storeId;
    private Store store;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        store = new Store();
        store.setId(storeId);
    }

    private PurchaseOrder createClosedPO(LocalDate poDate, LocalDate stockEntryDate) {
        PurchaseOrder po = new PurchaseOrder();
        po.setId(1L);
        po.setStore(store);
        po.setPoDate(poDate);
        po.setStockEntryDate(stockEntryDate);
        po.setStatus(PurchaseOrderStatus.CLOSED);
        po.setItems(new ArrayList<>());
        return po;
    }

    private PurchaseOrderItem createItem(PurchaseOrder po, BigDecimal mfgCost, BigDecimal transportCost,
                                          int units, int vatRate, LocalDate itemStockEntryDate) {
        PurchaseOrderItem item = PurchaseOrderItem.builder()
                .purchaseOrder(po)
                .manufacturingCostPerUnit(mfgCost)
                .transportationCostPerUnit(transportCost)
                .unitsOrdered(units)
                .costVatRate(vatRate)
                .stockEntryDate(itemStockEntryDate)
                .build();
        // Simulate the computed column totalCostPerUnit
        // In real DB, this is a generated column = mfg + transport
        // For test, we use reflection or a setter approach
        // Since totalCostPerUnit is insertable=false/updatable=false, we set it via reflection
        try {
            var field = PurchaseOrderItem.class.getDeclaredField("totalCostPerUnit");
            field.setAccessible(true);
            field.set(item, mfgCost.add(transportCost != null ? transportCost : BigDecimal.ZERO));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        po.getItems().add(item);
        return item;
    }

    private void setupDefaultMocks() {
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(deductionInvoiceRepository.findByStoreIdAndTransactionDateBetween(eq(storeId), any(), any()))
                .thenReturn(Collections.emptyList());
        when(orderRepository.findByStoreIdAndOrderDateBetweenAndStatusIn(eq(storeId), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(productRepository.findByStoreId(storeId))
                .thenReturn(Collections.emptyList());
    }

    @Nested
    @DisplayName("Purchase VAT calculation")
    class PurchaseVatCalculation {

        @Test
        @DisplayName("should calculate purchase VAT for items with stockEntryDate in period")
        void shouldCalculatePurchaseVatForItemsInPeriod() {
            setupDefaultMocks();

            PurchaseOrder po = createClosedPO(LocalDate.of(2025, 1, 5), LocalDate.of(2025, 1, 10));
            createItem(po, new BigDecimal("100.00"), new BigDecimal("10.00"), 50, 20, null);

            when(purchaseOrderRepository.findClosedWithItemsByStoreId(storeId))
                    .thenReturn(List.of(po));

            InvoiceSummaryDto result = invoiceService.getInvoiceSummary(
                    storeId, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));

            PurchaseVatDto purchaseVat = result.getPurchaseVat();
            assertThat(purchaseVat).isNotNull();
            // totalCostPerUnit = 110, units = 50, lineCost = 5500
            // VAT = 5500 * 20 / 100 = 1100
            assertThat(purchaseVat.getTotalPurchaseCostExclVat()).isEqualByComparingTo("5500.00");
            assertThat(purchaseVat.getTotalPurchaseVatAmount()).isEqualByComparingTo("1100.00");
            assertThat(purchaseVat.getTotalItemsPurchased()).isEqualTo(50);
        }

        @Test
        @DisplayName("should exclude items with stockEntryDate outside period")
        void shouldExcludeItemsOutsidePeriod() {
            setupDefaultMocks();

            PurchaseOrder po = createClosedPO(LocalDate.of(2025, 2, 15), LocalDate.of(2025, 2, 20));
            createItem(po, new BigDecimal("100.00"), BigDecimal.ZERO, 10, 20, null);

            when(purchaseOrderRepository.findClosedWithItemsByStoreId(storeId))
                    .thenReturn(List.of(po));

            // Query for January — PO has stockEntryDate in February
            InvoiceSummaryDto result = invoiceService.getInvoiceSummary(
                    storeId, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));

            PurchaseVatDto purchaseVat = result.getPurchaseVat();
            assertThat(purchaseVat.getTotalPurchaseCostExclVat()).isEqualByComparingTo("0");
            assertThat(purchaseVat.getTotalPurchaseVatAmount()).isEqualByComparingTo("0");
            assertThat(purchaseVat.getTotalItemsPurchased()).isEqualTo(0);
        }

        @Test
        @DisplayName("should use item-level stockEntryDate over PO-level")
        void shouldUseItemLevelStockEntryDate() {
            setupDefaultMocks();

            // PO stockEntryDate is in February, but item stockEntryDate is in January
            PurchaseOrder po = createClosedPO(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1));
            createItem(po, new BigDecimal("200.00"), BigDecimal.ZERO, 5, 20,
                    LocalDate.of(2025, 1, 15)); // item-level date overrides PO-level

            when(purchaseOrderRepository.findClosedWithItemsByStoreId(storeId))
                    .thenReturn(List.of(po));

            InvoiceSummaryDto result = invoiceService.getInvoiceSummary(
                    storeId, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));

            PurchaseVatDto purchaseVat = result.getPurchaseVat();
            // Item stockEntryDate is Jan 15 → within period
            assertThat(purchaseVat.getTotalPurchaseCostExclVat()).isEqualByComparingTo("1000.00");
            assertThat(purchaseVat.getTotalPurchaseVatAmount()).isEqualByComparingTo("200.00");
        }

        @Test
        @DisplayName("should fallback to poDate when stockEntryDate is null")
        void shouldFallbackToPoDate() {
            setupDefaultMocks();

            // Both PO and item stockEntryDate are null → fallback to poDate
            PurchaseOrder po = createClosedPO(LocalDate.of(2025, 1, 20), null);
            createItem(po, new BigDecimal("50.00"), BigDecimal.ZERO, 10, 20, null);

            when(purchaseOrderRepository.findClosedWithItemsByStoreId(storeId))
                    .thenReturn(List.of(po));

            InvoiceSummaryDto result = invoiceService.getInvoiceSummary(
                    storeId, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));

            PurchaseVatDto purchaseVat = result.getPurchaseVat();
            assertThat(purchaseVat.getTotalPurchaseCostExclVat()).isEqualByComparingTo("500.00");
            assertThat(purchaseVat.getTotalPurchaseVatAmount()).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("should group by VAT rate correctly")
        void shouldGroupByVatRate() {
            setupDefaultMocks();

            PurchaseOrder po = createClosedPO(LocalDate.of(2025, 1, 5), LocalDate.of(2025, 1, 10));
            createItem(po, new BigDecimal("100.00"), BigDecimal.ZERO, 10, 20, null);
            createItem(po, new BigDecimal("50.00"), BigDecimal.ZERO, 20, 10, null);
            createItem(po, new BigDecimal("30.00"), BigDecimal.ZERO, 5, 0, null);

            when(purchaseOrderRepository.findClosedWithItemsByStoreId(storeId))
                    .thenReturn(List.of(po));

            InvoiceSummaryDto result = invoiceService.getInvoiceSummary(
                    storeId, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));

            PurchaseVatDto purchaseVat = result.getPurchaseVat();
            assertThat(purchaseVat.getByRate()).hasSize(3);

            // 0% rate
            var rate0 = purchaseVat.getByRate().stream().filter(r -> r.getVatRate() == 0).findFirst().orElseThrow();
            assertThat(rate0.getCostAmount()).isEqualByComparingTo("150.00"); // 30 * 5
            assertThat(rate0.getVatAmount()).isEqualByComparingTo("0");

            // 10% rate
            var rate10 = purchaseVat.getByRate().stream().filter(r -> r.getVatRate() == 10).findFirst().orElseThrow();
            assertThat(rate10.getCostAmount()).isEqualByComparingTo("1000.00"); // 50 * 20
            assertThat(rate10.getVatAmount()).isEqualByComparingTo("100.00");   // 1000 * 10 / 100

            // 20% rate
            var rate20 = purchaseVat.getByRate().stream().filter(r -> r.getVatRate() == 20).findFirst().orElseThrow();
            assertThat(rate20.getCostAmount()).isEqualByComparingTo("1000.00"); // 100 * 10
            assertThat(rate20.getVatAmount()).isEqualByComparingTo("200.00");   // 1000 * 20 / 100
        }

        @Test
        @DisplayName("should return zero purchase VAT when no CLOSED POs exist")
        void shouldReturnZeroWhenNoClosedPOs() {
            setupDefaultMocks();

            when(purchaseOrderRepository.findClosedWithItemsByStoreId(storeId))
                    .thenReturn(Collections.emptyList());

            InvoiceSummaryDto result = invoiceService.getInvoiceSummary(
                    storeId, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));

            PurchaseVatDto purchaseVat = result.getPurchaseVat();
            assertThat(purchaseVat.getTotalPurchaseCostExclVat()).isEqualByComparingTo("0");
            assertThat(purchaseVat.getTotalPurchaseVatAmount()).isEqualByComparingTo("0");
            assertThat(purchaseVat.getTotalItemsPurchased()).isEqualTo(0);
            assertThat(purchaseVat.getByRate()).isEmpty();
        }
    }
}
