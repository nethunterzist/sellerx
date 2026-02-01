package com.ecommerce.sellerx.billing;

import com.ecommerce.sellerx.users.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Turkish e-invoice record for Paraşüt integration
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false, unique = true)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Paraşüt integration
    @Column(name = "parasut_id", length = 100)
    private String parasutId;

    @Column(name = "parasut_contact_id", length = 100)
    private String parasutContactId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EInvoiceStatus status = EInvoiceStatus.DRAFT;

    /**
     * E_ARSIV: B2C (individuals with TC Kimlik)
     * E_FATURA: B2B (companies with Vergi No)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", nullable = false, length = 20)
    private EInvoiceType invoiceType;

    @Column(name = "invoice_series", nullable = false, length = 10)
    @Builder.Default
    private String invoiceSeries = "SEL";

    /**
     * GİB assigned invoice number
     */
    @Column(name = "invoice_number", length = 50)
    private String invoiceNumber;

    // Tax information
    @Column(name = "tax_office", length = 100)
    private String taxOffice;

    /**
     * TC Kimlik (11 digits) for individuals
     * Vergi No (10 digits) for companies
     */
    @Column(name = "tax_number", nullable = false, length = 20)
    private String taxNumber;

    /**
     * Company name (for B2B invoices)
     */
    @Column(name = "company_title", length = 200)
    private String companyTitle;

    // Document storage
    @Column(name = "xml_content", columnDefinition = "TEXT")
    private String xmlContent;

    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    // Processing timestamps
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
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
     * Check if e-invoice is for company (B2B)
     */
    public boolean isB2B() {
        return invoiceType == EInvoiceType.E_FATURA;
    }

    /**
     * Check if e-invoice is for individual (B2C)
     */
    public boolean isB2C() {
        return invoiceType == EInvoiceType.E_ARSIV;
    }

    /**
     * Check if e-invoice was successfully sent
     */
    public boolean isSent() {
        return status == EInvoiceStatus.SENT || status == EInvoiceStatus.APPROVED;
    }

    /**
     * Validate TC Kimlik (11 digits)
     */
    public static boolean isValidTcKimlik(String tcKimlik) {
        return tcKimlik != null && tcKimlik.matches("^[0-9]{11}$");
    }

    /**
     * Validate Vergi No (10 digits)
     */
    public static boolean isValidVergiNo(String vergiNo) {
        return vergiNo != null && vergiNo.matches("^[0-9]{10}$");
    }
}
