package com.ecommerce.sellerx.financial;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for storing Trendyol DeductionInvoices from OtherFinancials API.
 * Includes platform fees, cargo fees, ad fees, and other deductions.
 *
 * Transaction types include:
 * - Platform Hizmet Bedeli (Platform Service Fee)
 * - Uluslararası Hizmet Bedeli (International Service Fee)
 * - AZ-Platform Hizmet Bedeli (Azerbaijan Platform Service Fee)
 * - AZ-Uluslararası Hizmet Bedeli (Azerbaijan International Service Fee)
 * - Yurt Dışı Operasyon Bedeli (Overseas Operation Fee)
 * - Kargo Fatura (Cargo Invoice)
 * - Reklam Bedeli (Advertising Fee)
 * - Komisyon Faturası (Commission Invoice)
 * - Kusurlu Ürün Faturası (Defective Product Invoice)
 * - Erken Ödeme Kesinti Faturası (Early Payment Deduction Invoice)
 */
@Entity
@Table(name = "trendyol_deduction_invoices",
        uniqueConstraints = @UniqueConstraint(name = "uk_deduction_invoice", columnNames = {"store_id", "trendyol_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendyolDeductionInvoice {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "trendyol_id", nullable = false, length = 100)
    private String trendyolId;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "transaction_type", nullable = false, length = 100)
    private String transactionType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "debt", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal debt = BigDecimal.ZERO;

    @Column(name = "credit", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal credit = BigDecimal.ZERO;

    @Column(name = "invoice_serial_number", length = 100)
    private String invoiceSerialNumber;

    @Column(name = "payment_order_id")
    private Long paymentOrderId;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "order_number", length = 50)
    private String orderNumber;

    @Column(name = "shipment_package_id")
    private Long shipmentPackageId;

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "TRY";

    @Column(name = "affiliate", length = 50)
    private String affiliate;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constants for transaction types (Platform Fees)
    public static final String TYPE_PLATFORM_SERVICE_FEE = "Platform Hizmet Bedeli";
    public static final String TYPE_INTERNATIONAL_SERVICE_FEE = "Uluslararası Hizmet Bedeli";
    public static final String TYPE_AZ_PLATFORM_SERVICE_FEE = "AZ-Platform Hizmet Bedeli";
    public static final String TYPE_AZ_INTERNATIONAL_SERVICE_FEE = "AZ-Uluslararası Hizmet Bedeli";
    public static final String TYPE_OVERSEAS_OPERATION_FEE = "Yurt Dışı Operasyon Bedeli";
    public static final String TYPE_COMMISSION_INVOICE = "Komisyon Faturası";
    public static final String TYPE_DEFECTIVE_PRODUCT = "Kusurlu Ürün Faturası";
    public static final String TYPE_EARLY_PAYMENT_DEDUCTION = "Erken Ödeme Kesinti Faturası";

    // Constants for other types
    public static final String TYPE_CARGO_INVOICE = "Kargo Fatura";
    public static final String TYPE_ADVERTISING_FEE = "Reklam Bedeli";

    /**
     * Check if this invoice is a platform fee (should be included in Platform Ücretleri)
     */
    public boolean isPlatformFee() {
        return transactionType != null && (
                transactionType.equals(TYPE_PLATFORM_SERVICE_FEE) ||
                transactionType.equals(TYPE_INTERNATIONAL_SERVICE_FEE) ||
                transactionType.equals(TYPE_AZ_PLATFORM_SERVICE_FEE) ||
                transactionType.equals(TYPE_AZ_INTERNATIONAL_SERVICE_FEE) ||
                transactionType.equals(TYPE_OVERSEAS_OPERATION_FEE) ||
                transactionType.equals(TYPE_COMMISSION_INVOICE) ||
                transactionType.equals(TYPE_DEFECTIVE_PRODUCT) ||
                transactionType.equals(TYPE_EARLY_PAYMENT_DEDUCTION)
        );
    }

    /**
     * Check if this is a cargo invoice
     */
    public boolean isCargoInvoice() {
        return TYPE_CARGO_INVOICE.equals(transactionType);
    }

    /**
     * Check if this is an advertising fee
     */
    public boolean isAdvertisingFee() {
        return TYPE_ADVERTISING_FEE.equals(transactionType);
    }

    /**
     * Check if this is a zero-VAT type (IADE or international operation).
     * These types should NOT have VAT calculated - they have 0% VAT rate.
     * Covers: "Yurtdışı Operasyon Iade Bedeli", "AZ-Yurtdışı Operasyon Bedeli",
     * and corrupted variants like "AZ-YURTDÕ_Õ OPERASYON BEDELI %18".
     */
    public boolean isRefundType() {
        if (transactionType == null) {
            return false;
        }
        String upper = transactionType.toUpperCase(java.util.Locale.ENGLISH);

        // Check for "IADE" (refund) in transaction type
        if (upper.contains("IADE")) {
            return true;
        }

        // ALL international operation types have 0% VAT (both debt and credit records)
        // Covers: "Yurtdışı Operasyon Bedeli", "AZ-Yurtdışı Operasyon Bedeli", corrupted variants
        if (upper.contains("YURT") && (upper.contains("OPERASYON") || upper.contains("OPERAS"))) {
            return true;
        }

        return false;
    }
}
