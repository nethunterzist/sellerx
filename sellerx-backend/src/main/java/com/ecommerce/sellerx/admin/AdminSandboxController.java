package com.ecommerce.sellerx.admin;

import com.ecommerce.sellerx.admin.dto.CreateSandboxInvoiceRequest;
import com.ecommerce.sellerx.admin.dto.CreateSandboxOrderRequest;
import com.ecommerce.sellerx.admin.dto.CreateSandboxProductRequest;
import com.ecommerce.sellerx.admin.dto.CreateSandboxReturnRequest;
import com.ecommerce.sellerx.admin.dto.SandboxInvoiceDto;
import com.ecommerce.sellerx.orders.TrendyolOrderDto;
import com.ecommerce.sellerx.products.TrendyolProductDto;
import com.ecommerce.sellerx.returns.ReturnRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/sandbox")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminSandboxController {

    private final AdminSandboxService sandboxService;

    /**
     * Sandbox mağazasının ürünlerini listeler
     */
    @GetMapping("/products")
    public ResponseEntity<List<TrendyolProductDto>> getSandboxProducts() {
        log.info("[SANDBOX] Ürünler listeleniyor");
        List<TrendyolProductDto> products = sandboxService.getSandboxProducts();
        return ResponseEntity.ok(products);
    }

    /**
     * Sandbox mağazasına yeni ürün ekler
     */
    @PostMapping("/products")
    public ResponseEntity<TrendyolProductDto> createSandboxProduct(
            @RequestBody CreateSandboxProductRequest request) {
        log.info("[SANDBOX] Yeni ürün ekleniyor: {}", request.getBarcode());
        TrendyolProductDto product = sandboxService.createSandboxProduct(request);
        return ResponseEntity.ok(product);
    }

    /**
     * Sandbox ürününü siler
     */
    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteSandboxProduct(@PathVariable UUID id) {
        log.info("[SANDBOX] Ürün siliniyor: {}", id);
        sandboxService.deleteSandboxProduct(id);
        return ResponseEntity.noContent().build();
    }

    // ============================================
    // SANDBOX ORDER ENDPOINTS
    // ============================================

    /**
     * Sandbox mağazasının siparişlerini listeler
     */
    @GetMapping("/orders")
    public ResponseEntity<List<TrendyolOrderDto>> getSandboxOrders() {
        log.info("[SANDBOX] Siparişler listeleniyor");
        List<TrendyolOrderDto> orders = sandboxService.getSandboxOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * Sandbox mağazasına yeni sipariş ekler
     */
    @PostMapping("/orders")
    public ResponseEntity<TrendyolOrderDto> createSandboxOrder(
            @RequestBody CreateSandboxOrderRequest request) {
        log.info("[SANDBOX] Yeni sipariş ekleniyor: {} adet {}", request.getQuantity(), request.getBarcode());
        TrendyolOrderDto order = sandboxService.createSandboxOrder(request);
        return ResponseEntity.ok(order);
    }

    /**
     * Sandbox siparişini siler
     */
    @DeleteMapping("/orders/{id}")
    public ResponseEntity<Void> deleteSandboxOrder(@PathVariable UUID id) {
        log.info("[SANDBOX] Sipariş siliniyor: {}", id);
        sandboxService.deleteSandboxOrder(id);
        return ResponseEntity.noContent().build();
    }

    // ============================================
    // SANDBOX INVOICE ENDPOINTS
    // ============================================

    /**
     * Sandbox mağazasının faturalarını listeler
     */
    @GetMapping("/invoices")
    public ResponseEntity<List<SandboxInvoiceDto>> getSandboxInvoices() {
        log.info("[SANDBOX] Faturalar listeleniyor");
        List<SandboxInvoiceDto> invoices = sandboxService.getSandboxInvoices();
        return ResponseEntity.ok(invoices);
    }

    /**
     * Sandbox mağazasına yeni fatura ekler
     */
    @PostMapping("/invoices")
    public ResponseEntity<SandboxInvoiceDto> createSandboxInvoice(
            @RequestBody CreateSandboxInvoiceRequest request) {
        log.info("[SANDBOX] Yeni fatura ekleniyor: {} - {}", request.getTransactionType(), request.getAmount());
        SandboxInvoiceDto invoice = sandboxService.createSandboxInvoice(request);
        return ResponseEntity.ok(invoice);
    }

    /**
     * Sandbox faturasını günceller
     */
    @PutMapping("/invoices/{id}")
    public ResponseEntity<SandboxInvoiceDto> updateSandboxInvoice(
            @PathVariable UUID id,
            @RequestBody CreateSandboxInvoiceRequest request) {
        log.info("[SANDBOX] Fatura güncelleniyor: {}", id);
        SandboxInvoiceDto invoice = sandboxService.updateSandboxInvoice(id, request);
        return ResponseEntity.ok(invoice);
    }

    /**
     * Sandbox faturasını siler
     */
    @DeleteMapping("/invoices/{id}")
    public ResponseEntity<Void> deleteSandboxInvoice(@PathVariable UUID id) {
        log.info("[SANDBOX] Fatura siliniyor: {}", id);
        sandboxService.deleteSandboxInvoice(id);
        return ResponseEntity.noContent().build();
    }

    // ============================================
    // SANDBOX RETURN ENDPOINTS
    // ============================================

    /**
     * Sandbox mağazasının iadelerini listeler
     */
    @GetMapping("/returns")
    public ResponseEntity<List<ReturnRecord>> getSandboxReturns() {
        log.info("[SANDBOX] İadeler listeleniyor");
        List<ReturnRecord> returns = sandboxService.getSandboxReturns();
        return ResponseEntity.ok(returns);
    }

    /**
     * Sandbox mağazasına yeni iade ekler
     */
    @PostMapping("/returns")
    public ResponseEntity<ReturnRecord> createSandboxReturn(
            @RequestBody CreateSandboxReturnRequest request) {
        log.info("[SANDBOX] Yeni iade ekleniyor: {} - {}", request.getOrderNumber(), request.getBarcode());
        ReturnRecord returnRecord = sandboxService.createSandboxReturn(request);
        return ResponseEntity.ok(returnRecord);
    }

    /**
     * Sandbox iadesini siler
     */
    @DeleteMapping("/returns/{id}")
    public ResponseEntity<Void> deleteSandboxReturn(@PathVariable UUID id) {
        log.info("[SANDBOX] İade siliniyor: {}", id);
        sandboxService.deleteSandboxReturn(id);
        return ResponseEntity.noContent().build();
    }

    // ============================================
    // SANDBOX SETTLEMENT ENDPOINTS
    // ============================================

    /**
     * Tek bir sandbox siparişini settle eder (komisyonu gerçek yapar)
     */
    @PostMapping("/orders/{id}/settle")
    public ResponseEntity<TrendyolOrderDto> settleSandboxOrder(@PathVariable UUID id) {
        log.info("[SANDBOX] Sipariş settle ediliyor: {}", id);
        TrendyolOrderDto order = sandboxService.settleSandboxOrder(id);
        return ResponseEntity.ok(order);
    }

    /**
     * Tüm sandbox siparişlerini settle eder
     */
    @PostMapping("/orders/settle-all")
    public ResponseEntity<Map<String, Integer>> settleAllSandboxOrders() {
        log.info("[SANDBOX] Tüm siparişler settle ediliyor");
        int count = sandboxService.settleAllSandboxOrders();
        return ResponseEntity.ok(Map.of("settledCount", count));
    }
}
