package com.ecommerce.sellerx.financial;

import com.ecommerce.sellerx.financial.dto.AggregatedProductDto;
import com.ecommerce.sellerx.financial.dto.CargoInvoiceItemDto;
import com.ecommerce.sellerx.financial.dto.CommissionInvoiceItemDto;
import com.ecommerce.sellerx.financial.dto.InvoiceDetailDto;
import com.ecommerce.sellerx.financial.dto.InvoiceItemDto;
import com.ecommerce.sellerx.financial.dto.InvoiceSummaryDto;
import com.ecommerce.sellerx.financial.dto.ProductCargoBreakdownDto;
import com.ecommerce.sellerx.financial.dto.ProductCommissionBreakdownDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Invoice operations.
 * Provides endpoints for invoice summary, by-type queries, and sync operations.
 */
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Slf4j
public class TrendyolInvoiceController {

    private final TrendyolInvoiceService invoiceService;
    private final TrendyolOtherFinancialsService otherFinancialsService;

    /**
     * Get invoice summary for a store.
     * Returns all invoice types with totals and counts.
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/stores/{storeId}/summary")
    public ResponseEntity<InvoiceSummaryDto> getInvoiceSummary(
            @PathVariable UUID storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // Default to last 30 days if dates not provided
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusDays(30);
        }

        log.info("Getting invoice summary for store: {} from {} to {}", storeId, startDate, endDate);
        InvoiceSummaryDto summary = invoiceService.getInvoiceSummary(storeId, startDate, endDate);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get invoices by type code with pagination.
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/stores/{storeId}/by-type/{typeCode}")
    public ResponseEntity<Page<InvoiceDetailDto>> getInvoicesByType(
            @PathVariable UUID storeId,
            @PathVariable String typeCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Default to last 30 days if dates not provided
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusDays(30);
        }

        log.info("Getting invoices by type {} for store: {} from {} to {}", typeCode, storeId, startDate, endDate);
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size);
        Page<InvoiceDetailDto> invoices = invoiceService.getInvoicesByType(storeId, typeCode, startDate, endDate, pageable);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Get invoices by category with pagination.
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/stores/{storeId}/by-category/{category}")
    public ResponseEntity<Page<InvoiceDetailDto>> getInvoicesByCategory(
            @PathVariable UUID storeId,
            @PathVariable String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Default to last 30 days if dates not provided
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusDays(30);
        }

        log.info("Getting invoices by category {} for store: {} from {} to {}", category, storeId, startDate, endDate);
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size);
        Page<InvoiceDetailDto> invoices = invoiceService.getInvoicesByCategory(storeId, category, startDate, endDate, pageable);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Get all invoices with pagination.
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/stores/{storeId}")
    public ResponseEntity<Page<InvoiceDetailDto>> getAllInvoices(
            @PathVariable UUID storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Default to last 30 days if dates not provided
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusDays(30);
        }

        log.info("Getting all invoices for store: {} from {} to {}", storeId, startDate, endDate);
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size);
        Page<InvoiceDetailDto> invoices = invoiceService.getAllInvoices(storeId, startDate, endDate, pageable);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Get cargo invoice items by invoice serial number.
     * Returns all shipments/orders within a specific cargo invoice (Kargo Fatura).
     * This shows the breakdown of what is included in a cargo invoice.
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/stores/{storeId}/cargo-items/{invoiceSerialNumber}")
    public ResponseEntity<CargoItemsResponse> getCargoInvoiceItems(
            @PathVariable UUID storeId,
            @PathVariable String invoiceSerialNumber) {

        log.info("Getting cargo invoice items for store: {}, invoice: {}", storeId, invoiceSerialNumber);
        List<CargoInvoiceItemDto> items = invoiceService.getCargoInvoiceItems(storeId, invoiceSerialNumber);

        // Calculate totals
        var totalAmount = items.stream()
                .map(CargoInvoiceItemDto::getAmount)
                .filter(a -> a != null)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        var totalVatAmount = items.stream()
                .map(CargoInvoiceItemDto::getVatAmount)
                .filter(a -> a != null)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        return ResponseEntity.ok(CargoItemsResponse.builder()
                .invoiceSerialNumber(invoiceSerialNumber)
                .itemCount(items.size())
                .totalAmount(totalAmount)
                .totalVatAmount(totalVatAmount)
                .items(items)
                .build());
    }

    /**
     * Cargo items response DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CargoItemsResponse {
        private String invoiceSerialNumber;
        private int itemCount;
        private java.math.BigDecimal totalAmount;
        private java.math.BigDecimal totalVatAmount;
        private List<CargoInvoiceItemDto> items;
    }

    /**
     * Get generic invoice items by invoice serial number.
     * Returns all orders/items within a specific invoice (for CEZA, KOMISYON, etc. types).
     * This endpoint is used for non-cargo invoice detail views.
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/stores/{storeId}/items/{invoiceSerialNumber}")
    public ResponseEntity<InvoiceItemsResponse> getInvoiceItems(
            @PathVariable UUID storeId,
            @PathVariable String invoiceSerialNumber) {

        log.info("Getting invoice items for store: {}, invoice: {}", storeId, invoiceSerialNumber);
        List<InvoiceItemDto> items = invoiceService.getInvoiceItems(storeId, invoiceSerialNumber);

        // Calculate totals
        var totalAmount = items.stream()
                .map(InvoiceItemDto::getAmount)
                .filter(a -> a != null)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        var totalVatAmount = items.stream()
                .map(InvoiceItemDto::getVatAmount)
                .filter(a -> a != null)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        return ResponseEntity.ok(InvoiceItemsResponse.builder()
                .invoiceSerialNumber(invoiceSerialNumber)
                .itemCount(items.size())
                .totalAmount(totalAmount)
                .totalVatAmount(totalVatAmount)
                .items(items)
                .build());
    }

    /**
     * Invoice items response DTO (for non-cargo invoices)
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class InvoiceItemsResponse {
        private String invoiceSerialNumber;
        private int itemCount;
        private java.math.BigDecimal totalAmount;
        private java.math.BigDecimal totalVatAmount;
        private List<InvoiceItemDto> items;
    }

    /**
     * Get commission invoice items by invoice serial number.
     * Returns all orders within a specific commission invoice (Komisyon Fatura).
     * Shows order-level commission breakdown similar to Trendyol Excel export.
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/stores/{storeId}/commission-items/{invoiceSerialNumber}")
    public ResponseEntity<CommissionItemsResponse> getCommissionInvoiceItems(
            @PathVariable UUID storeId,
            @PathVariable String invoiceSerialNumber) {

        log.info("Getting commission invoice items for store: {}, invoice: {}", storeId, invoiceSerialNumber);
        List<CommissionInvoiceItemDto> items = invoiceService.getCommissionInvoiceItems(storeId, invoiceSerialNumber);

        // Calculate totals
        var totalCommission = items.stream()
                .map(CommissionInvoiceItemDto::getCommissionAmount)
                .filter(a -> a != null)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        var totalSellerRevenue = items.stream()
                .map(CommissionInvoiceItemDto::getSellerRevenue)
                .filter(a -> a != null)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        return ResponseEntity.ok(CommissionItemsResponse.builder()
                .invoiceSerialNumber(invoiceSerialNumber)
                .itemCount(items.size())
                .totalCommission(totalCommission)
                .totalSellerRevenue(totalSellerRevenue)
                .items(items)
                .build());
    }

    /**
     * Commission items response DTO (for commission invoices)
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CommissionItemsResponse {
        private String invoiceSerialNumber;
        private int itemCount;
        private java.math.BigDecimal totalCommission;
        private java.math.BigDecimal totalSellerRevenue;
        private List<CommissionInvoiceItemDto> items;
    }

    /**
     * Sync invoices for a store.
     * Fetches deduction invoices from Trendyol API.
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @PostMapping("/stores/{storeId}/sync")
    public ResponseEntity<SyncResponse> syncInvoices(
            @PathVariable UUID storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // Default to last 90 days if dates not provided
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusDays(90);
        }

        log.info("Syncing invoices for store: {} from {} to {}", storeId, startDate, endDate);

        int syncedCount = otherFinancialsService.syncDeductionInvoices(storeId, startDate, endDate);

        return ResponseEntity.ok(SyncResponse.builder()
                .success(true)
                .message("Successfully synced " + syncedCount + " invoices")
                .syncedCount(syncedCount)
                .startDate(startDate)
                .endDate(endDate)
                .build());
    }

    /**
     * Sync response DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SyncResponse {
        private boolean success;
        private String message;
        private int syncedCount;
        private LocalDate startDate;
        private LocalDate endDate;
    }

    // ================================================================================
    // Category-level endpoints (for "Fatura Kalemleri" and "Ürünler" tabs)
    // ================================================================================

    /**
     * Get all cargo invoice items for a date range with pagination.
     * Used for "Fatura Kalemleri" tab when KARGO category card is selected.
     * Returns paginated list of all cargo invoice items, not grouped by invoice number.
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/stores/{storeId}/category/cargo-items")
    public ResponseEntity<Page<CargoInvoiceItemDto>> getCargoItemsByDateRange(
            @PathVariable UUID storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Default to last 30 days if dates not provided
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusDays(30);
        }

        log.info("Getting cargo items by date range for store: {} from {} to {}", storeId, startDate, endDate);
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size);
        Page<CargoInvoiceItemDto> items = invoiceService.getCargoItemsByDateRange(storeId, startDate, endDate, pageable);
        return ResponseEntity.ok(items);
    }

    /**
     * Get all commission invoice items for a date range with pagination.
     * Used for "Fatura Kalemleri" tab when KOMISYON category card is selected.
     * Returns paginated list of all commission invoices (Platform Hizmet, Komisyon Faturası, etc.)
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/stores/{storeId}/category/commission-items")
    public ResponseEntity<Page<InvoiceDetailDto>> getCommissionItemsByDateRange(
            @PathVariable UUID storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Default to last 30 days if dates not provided
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusDays(30);
        }

        log.info("Getting commission items by date range for store: {} from {} to {}", storeId, startDate, endDate);
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size);
        Page<InvoiceDetailDto> items = invoiceService.getCommissionItemsByDateRange(storeId, startDate, endDate, pageable);
        return ResponseEntity.ok(items);
    }

    /**
     * Get aggregated products for a category (KARGO or KOMISYON).
     * Used for "Ürünler" tab when a category card is selected.
     * Groups invoice items by barcode (SKU) and returns totals.
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/stores/{storeId}/category/{category}/products")
    public ResponseEntity<AggregatedProductsResponse> getAggregatedProducts(
            @PathVariable UUID storeId,
            @PathVariable String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // Default to last 30 days if dates not provided
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusDays(30);
        }

        // Validate category
        if (!"KARGO".equals(category) && !"KOMISYON".equals(category)) {
            return ResponseEntity.badRequest().build();
        }

        log.info("Getting aggregated products for category {} in store: {} from {} to {}",
                category, storeId, startDate, endDate);

        List<AggregatedProductDto> products = invoiceService.getAggregatedProductsByCategory(
                storeId, category, startDate, endDate);

        // Calculate totals
        var totalAmount = products.stream()
                .map(AggregatedProductDto::getTotalAmount)
                .filter(a -> a != null)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        var totalVatAmount = products.stream()
                .map(AggregatedProductDto::getTotalVatAmount)
                .filter(a -> a != null)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        var totalQuantity = products.stream()
                .mapToInt(AggregatedProductDto::getTotalQuantity)
                .sum();

        return ResponseEntity.ok(AggregatedProductsResponse.builder()
                .category(category)
                .productCount(products.size())
                .totalQuantity(totalQuantity)
                .totalAmount(totalAmount)
                .totalVatAmount(totalVatAmount)
                .products(products)
                .build());
    }

    /**
     * Aggregated products response DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AggregatedProductsResponse {
        private String category;
        private int productCount;
        private int totalQuantity;
        private java.math.BigDecimal totalAmount;
        private java.math.BigDecimal totalVatAmount;
        private List<AggregatedProductDto> products;
    }

    // ================================================================================
    // Product Commission Breakdown (for "Detay" panel in KOMISYON Ürünler tab)
    // ================================================================================

    /**
     * Get commission breakdown by transaction type for a specific product (barcode).
     * Used for "Detay" panel in KOMISYON Ürünler tab.
     * Shows breakdown by Sale, Coupon, Discount, Return transaction types.
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/stores/{storeId}/products/{barcode}/commission-breakdown")
    public ResponseEntity<ProductCommissionBreakdownDto> getProductCommissionBreakdown(
            @PathVariable UUID storeId,
            @PathVariable String barcode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // Default to last 30 days if dates not provided
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusDays(30);
        }

        log.info("Getting commission breakdown for product {} in store: {} from {} to {}",
                barcode, storeId, startDate, endDate);

        ProductCommissionBreakdownDto breakdown = invoiceService.getProductCommissionBreakdown(
                storeId, barcode, startDate, endDate);
        return ResponseEntity.ok(breakdown);
    }

    // ================================================================================
    // Product Cargo Breakdown (for "Detay" panel in KARGO Ürünler tab)
    // ================================================================================

    /**
     * Get cargo breakdown for a specific product (barcode).
     * Used for "Detay" panel in KARGO Ürünler tab.
     * Shows cargo cost breakdown with shipment list.
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/stores/{storeId}/products/{barcode}/cargo-breakdown")
    public ResponseEntity<ProductCargoBreakdownDto> getProductCargoBreakdown(
            @PathVariable UUID storeId,
            @PathVariable String barcode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // Default to last 30 days if dates not provided
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusDays(30);
        }

        log.info("Getting cargo breakdown for product {} in store: {} from {} to {}",
                barcode, storeId, startDate, endDate);

        ProductCargoBreakdownDto breakdown = invoiceService.getProductCargoBreakdown(
                storeId, barcode, startDate, endDate);
        return ResponseEntity.ok(breakdown);
    }
}
