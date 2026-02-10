package com.ecommerce.sellerx.purchasing;

import com.ecommerce.sellerx.purchasing.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stores/{storeId}/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;
    private final PurchaseReportService purchaseReportService;
    private final PurchaseOrderExcelService purchaseOrderExcelService;
    private final StockDepletionService stockDepletionService;

    // === List & Get ===

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping
    public ResponseEntity<List<PurchaseOrderSummaryDto>> getPurchaseOrders(
            @PathVariable UUID storeId,
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<PurchaseOrderSummaryDto> orders;
        if (search != null || supplierId != null) {
            orders = purchaseOrderService.searchPurchaseOrders(storeId, search, status, supplierId, startDate, endDate);
        } else {
            orders = purchaseOrderService.getPurchaseOrders(storeId, status, startDate, endDate);
        }
        return ResponseEntity.ok(orders);
    }

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/stats")
    public ResponseEntity<PurchaseOrderStatsDto> getStats(
            @PathVariable UUID storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        PurchaseOrderStatsDto stats = purchaseOrderService.getStats(storeId, startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/{poId}")
    public ResponseEntity<PurchaseOrderDto> getPurchaseOrder(
            @PathVariable UUID storeId,
            @PathVariable Long poId) {
        PurchaseOrderDto po = purchaseOrderService.getPurchaseOrder(storeId, poId);
        return ResponseEntity.ok(po);
    }

    // === CRUD ===

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @PostMapping
    public ResponseEntity<PurchaseOrderDto> createPurchaseOrder(
            @PathVariable UUID storeId,
            @RequestBody CreatePurchaseOrderRequest request) {
        PurchaseOrderDto po = purchaseOrderService.createPurchaseOrder(storeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(po);
    }

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @PutMapping("/{poId}")
    public ResponseEntity<PurchaseOrderDto> updatePurchaseOrder(
            @PathVariable UUID storeId,
            @PathVariable Long poId,
            @RequestBody UpdatePurchaseOrderRequest request) {
        PurchaseOrderDto po = purchaseOrderService.updatePurchaseOrder(storeId, poId, request);
        return ResponseEntity.ok(po);
    }

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @DeleteMapping("/{poId}")
    public ResponseEntity<Void> deletePurchaseOrder(
            @PathVariable UUID storeId,
            @PathVariable Long poId) {
        purchaseOrderService.deletePurchaseOrder(storeId, poId);
        return ResponseEntity.noContent().build();
    }

    // === Status ===

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @PutMapping("/{poId}/status")
    public ResponseEntity<PurchaseOrderDto> updateStatus(
            @PathVariable UUID storeId,
            @PathVariable Long poId,
            @RequestBody UpdateStatusRequest request) {
        PurchaseOrderDto po = purchaseOrderService.updateStatus(storeId, poId, request.getStatus());
        return ResponseEntity.ok(po);
    }

    // === Items ===

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @PostMapping("/{poId}/items")
    public ResponseEntity<PurchaseOrderDto> addItem(
            @PathVariable UUID storeId,
            @PathVariable Long poId,
            @RequestBody AddPurchaseOrderItemRequest request) {
        PurchaseOrderDto po = purchaseOrderService.addItem(storeId, poId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(po);
    }

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @PutMapping("/{poId}/items/{itemId}")
    public ResponseEntity<PurchaseOrderDto> updateItem(
            @PathVariable UUID storeId,
            @PathVariable Long poId,
            @PathVariable Long itemId,
            @RequestBody AddPurchaseOrderItemRequest request) {
        PurchaseOrderDto po = purchaseOrderService.updateItem(storeId, poId, itemId, request);
        return ResponseEntity.ok(po);
    }

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @DeleteMapping("/{poId}/items/{itemId}")
    public ResponseEntity<PurchaseOrderDto> removeItem(
            @PathVariable UUID storeId,
            @PathVariable Long poId,
            @PathVariable Long itemId) {
        PurchaseOrderDto po = purchaseOrderService.removeItem(storeId, poId, itemId);
        return ResponseEntity.ok(po);
    }

    // === Duplicate & Split ===

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @PostMapping("/{poId}/duplicate")
    public ResponseEntity<PurchaseOrderDto> duplicatePurchaseOrder(
            @PathVariable UUID storeId,
            @PathVariable Long poId) {
        PurchaseOrderDto po = purchaseOrderService.duplicatePurchaseOrder(storeId, poId);
        return ResponseEntity.status(HttpStatus.CREATED).body(po);
    }

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @PostMapping("/{poId}/split")
    public ResponseEntity<PurchaseOrderDto> splitPurchaseOrder(
            @PathVariable UUID storeId,
            @PathVariable Long poId,
            @RequestBody SplitPurchaseOrderRequest request) {
        PurchaseOrderDto po = purchaseOrderService.splitPurchaseOrder(storeId, poId, request.getItemIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(po);
    }

    // === Attachments ===

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/{poId}/attachments")
    public ResponseEntity<List<AttachmentDto>> getAttachments(
            @PathVariable UUID storeId,
            @PathVariable Long poId) {
        return ResponseEntity.ok(purchaseOrderService.getAttachments(storeId, poId));
    }

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @PostMapping(value = "/{poId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentDto> uploadAttachment(
            @PathVariable UUID storeId,
            @PathVariable Long poId,
            @RequestParam("file") MultipartFile file) throws IOException {
        AttachmentDto dto = purchaseOrderService.addAttachment(
                storeId, poId,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getBytes());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/{poId}/attachments/{attachmentId}/download")
    public ResponseEntity<byte[]> downloadAttachment(
            @PathVariable UUID storeId,
            @PathVariable Long poId,
            @PathVariable Long attachmentId) {
        PurchaseOrderAttachment attachment = purchaseOrderService.getAttachmentWithData(storeId, poId, attachmentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(attachment.getFileType() != null ? attachment.getFileType() : "application/octet-stream"))
                .body(attachment.getFileData());
    }

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @DeleteMapping("/{poId}/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable UUID storeId,
            @PathVariable Long poId,
            @PathVariable Long attachmentId) {
        purchaseOrderService.deleteAttachment(storeId, poId, attachmentId);
        return ResponseEntity.noContent().build();
    }

    // === Export/Import ===

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/{poId}/export")
    public ResponseEntity<byte[]> exportToExcel(
            @PathVariable UUID storeId,
            @PathVariable Long poId) {
        PurchaseOrderDto po = purchaseOrderService.getPurchaseOrder(storeId, poId);
        byte[] excelData = purchaseOrderExcelService.exportToExcel(po);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + po.getPoNumber() + ".xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
    }

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @PostMapping(value = "/{poId}/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PurchaseOrderDto> importFromExcel(
            @PathVariable UUID storeId,
            @PathVariable Long poId,
            @RequestParam("file") MultipartFile file) throws IOException {
        PurchaseOrderDto po = purchaseOrderExcelService.importFromExcel(storeId, poId, file.getInputStream());
        return ResponseEntity.ok(po);
    }

    // === Reports ===

    /**
     * Get cost history for a specific product
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/reports/product/{productId}/cost-history")
    public ResponseEntity<ProductCostHistoryResponse> getProductCostHistory(
            @PathVariable UUID storeId,
            @PathVariable UUID productId) {
        ProductCostHistoryResponse response = purchaseReportService.getProductCostHistory(storeId, productId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get FIFO analysis for a product within a date range
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/reports/fifo-analysis")
    public ResponseEntity<FifoAnalysisResponse> getFifoAnalysis(
            @PathVariable UUID storeId,
            @RequestParam String barcode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        FifoAnalysisResponse response = purchaseReportService.getFifoAnalysis(storeId, barcode, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    /**
     * Get stock valuation report
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/reports/stock-valuation")
    public ResponseEntity<StockValuationResponse> getStockValuation(@PathVariable UUID storeId) {
        StockValuationResponse response = purchaseReportService.getStockValuation(storeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get profitability analysis for a date range
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/reports/profitability")
    public ResponseEntity<ProfitabilityResponse> getProfitabilityAnalysis(
            @PathVariable UUID storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        ProfitabilityResponse response = purchaseReportService.getProfitabilityAnalysis(storeId, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    /**
     * Get purchase summary for a date range
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/reports/summary")
    public ResponseEntity<PurchaseSummaryResponse> getPurchaseSummary(
            @PathVariable UUID storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        PurchaseSummaryResponse response = purchaseReportService.getPurchaseSummary(storeId, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    // === Stock Depletion ===

    /**
     * Get products with depleted stock (using LAST_KNOWN cost fallback)
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/reports/stock-depletion")
    public ResponseEntity<List<DepletedProductDto>> getDepletedProducts(@PathVariable UUID storeId) {
        List<DepletedProductDto> depleted = stockDepletionService.getDepletedProducts(storeId);
        return ResponseEntity.ok(depleted);
    }
}
