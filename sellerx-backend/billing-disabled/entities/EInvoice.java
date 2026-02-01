package com.ecommerce.sellerx.billing;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * E-Invoice record for Parasut integration
 */
@Entity
@Table(name = "e_invoices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    // Parasut data
    @Column(name = "parasut_id", length = 100)
    private String parasutId;

    @Column(name = "parasut_contact_id", length = 100)
    private String parasutContactId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EInvoiceStatus status = EInvoiceStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", nullable = false, length = 20)
    @Builder.Default
    private EInvoiceType invoiceType = EInvoiceType.E_ARSIV;

    @Column(name = "invoice_series", length = 10)
    @Builder.Default
    private String invoiceSeries = "SEL";

    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    // Customer tax info
    @Column(name = "tax_number", length = 20)
    private String taxNumber;

    @Column(name = "tax_office", length = 100)
    private String taxOffice;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Determine invoice type based on tax number
     */
    public void determineInvoiceType() {
        if (taxNumber != null && !taxNumber.isBlank()) {
            this.invoiceType = EInvoiceType.E_FATURA;
        } else {
            this.invoiceType = EInvoiceType.E_ARSIV;
        }
    }
}
