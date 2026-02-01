package com.ecommerce.sellerx.billing;

import com.ecommerce.sellerx.billing.service.InvoiceService;
import com.ecommerce.sellerx.users.User;
import com.ecommerce.sellerx.users.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Invoice management controller.
 * Handles invoice listing and retrieval.
 */
@RestController
@RequestMapping("/api/billing/invoices")
@RequiredArgsConstructor
@Slf4j
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final UserService userService;

    /**
     * Get current authenticated user ID from SecurityContext
     */
    private Long getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("User not authenticated");
        }
        return Long.valueOf(authentication.getPrincipal().toString());
    }

    /**
     * Get all invoices for current user (paginated)
     */
    @GetMapping
    public ResponseEntity<Page<InvoiceDto>> getInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long userId = getCurrentUserId();

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Invoice> invoices = invoiceService.findByUserId(userId, pageable);

        Page<InvoiceDto> invoiceDtos = invoices.map(this::toDto);
        return ResponseEntity.ok(invoiceDtos);
    }

    /**
     * Get a specific invoice by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDto> getInvoice(@PathVariable UUID id) {
        Long userId = getCurrentUserId();

        return invoiceService.findByIdAndUserId(id, userId)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get invoice PDF URL
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<PdfUrlResponse> getInvoicePdf(@PathVariable UUID id) {
        Long userId = getCurrentUserId();

        return invoiceService.findByIdAndUserId(id, userId)
                .map(invoice -> {
                    // TODO: Generate or retrieve PDF URL from storage
                    String pdfUrl = "/api/billing/invoices/" + id + "/download";
                    return ResponseEntity.ok(new PdfUrlResponse(pdfUrl));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private InvoiceDto toDto(Invoice invoice) {
        return InvoiceDto.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .status(invoice.getStatus().name())
                .subtotal(invoice.getSubtotal())
                .tax(invoice.getTaxAmount())
                .totalAmount(invoice.getTotalAmount())
                .currency(invoice.getCurrency())
                .periodStart(invoice.getBillingPeriodStart())
                .periodEnd(invoice.getBillingPeriodEnd())
                .dueDate(invoice.getDueDate())
                .paidAt(invoice.getPaidAt())
                .createdAt(invoice.getCreatedAt())
                .planName(invoice.getSubscription() != null && invoice.getSubscription().getPlan() != null
                        ? invoice.getSubscription().getPlan().getName()
                        : null)
                .build();
    }

    /**
     * DTO for invoice response
     */
    @lombok.Data
    @lombok.Builder
    public static class InvoiceDto {
        private UUID id;
        private String invoiceNumber;
        private String status;
        private java.math.BigDecimal subtotal;
        private java.math.BigDecimal tax;
        private java.math.BigDecimal totalAmount;
        private String currency;
        private java.time.LocalDateTime periodStart;
        private java.time.LocalDateTime periodEnd;
        private java.time.LocalDateTime dueDate;
        private java.time.LocalDateTime paidAt;
        private java.time.LocalDateTime createdAt;
        private String planName;
    }

    /**
     * Response for PDF URL
     */
    public record PdfUrlResponse(String url) {}
}
