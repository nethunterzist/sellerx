package com.ecommerce.sellerx.financial;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Entity for storing Trendyol invoices from the Invoices API.
 * Covers all invoice types received from Trendyol or to be issued to Trendyol.
 *
 * Invoice Categories:
 * 1. KOMISYON - Commission invoices (Platform Hizmet Bedeli, AZ-Komisyon)
 * 2. KARGO - Cargo invoices (Kargo Fatura, AZ-Kargo)
 * 3. ULUSLARARASI - International service fees
 * 4. CEZA - Penalties (Tedarik Edememe, Termin Gecikme, etc.)
 * 5. REKLAM - Advertising fees
 * 6. DIGER - Other fees
 * 7. IADE - Refunds to seller (TZM, KRM, DIF)
 */
@Entity
@Table(name = "trendyol_invoices",
        uniqueConstraints = @UniqueConstraint(name = "uk_invoice", columnNames = {"store_id", "invoice_number"}),
        indexes = {
            @Index(name = "idx_invoice_store_type", columnList = "store_id, invoice_type_code"),
            @Index(name = "idx_invoice_store_date", columnList = "store_id, invoice_date"),
            @Index(name = "idx_invoice_type_code", columnList = "invoice_type_code")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendyolInvoice {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "invoice_number", nullable = false, length = 100)
    private String invoiceNumber;

    @Column(name = "invoice_type", nullable = false, length = 100)
    private String invoiceType;

    @Column(name = "invoice_type_code", nullable = false, length = 50)
    private String invoiceTypeCode;

    @Column(name = "invoice_category", nullable = false, length = 30)
    private String invoiceCategory;

    @Column(name = "invoice_date", nullable = false)
    private LocalDateTime invoiceDate;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "vat_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(name = "vat_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal vatRate = BigDecimal.ZERO;

    @Column(name = "base_amount", precision = 15, scale = 2)
    private BigDecimal baseAmount;

    @Column(name = "is_deduction", nullable = false)
    @Builder.Default
    private Boolean isDeduction = true;

    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "TRY";

    @Column(name = "order_number", length = 50)
    private String orderNumber;

    @Column(name = "shipment_package_id")
    private Long shipmentPackageId;

    @Column(name = "payment_order_id")
    private Long paymentOrderId;

    @Column(name = "barcode", length = 100)
    private String barcode;

    @Column(name = "product_name", length = 500)
    private String productName;

    @Column(name = "desi", precision = 10, scale = 2)
    private BigDecimal desi;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Type(JsonBinaryType.class)
    @Column(name = "details", columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Column(name = "trendyol_invoice_id", length = 100)
    private String trendyolInvoiceId;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Invoice Categories
    public static final String CATEGORY_KOMISYON = "KOMISYON";
    public static final String CATEGORY_KARGO = "KARGO";
    public static final String CATEGORY_ULUSLARARASI = "ULUSLARARASI";
    public static final String CATEGORY_CEZA = "CEZA";
    public static final String CATEGORY_REKLAM = "REKLAM";
    public static final String CATEGORY_DIGER = "DIGER";
    public static final String CATEGORY_IADE = "IADE";

    // Invoice Type Codes - Komisyon
    public static final String TYPE_PLATFORM_HIZMET_BEDELI = "PLATFORM_HIZMET";
    public static final String TYPE_AZ_KOMISYON = "AZ_KOMISYON";
    public static final String TYPE_AZ_KOMISYON_GELIRI = "AZ_KOMISYON_GELIRI";

    // Invoice Type Codes - Kargo
    public static final String TYPE_KARGO_FATURA = "KARGO_FATURA";
    public static final String TYPE_AZ_KARGO = "AZ_KARGO";
    public static final String TYPE_KARGO_ITIRAZ_IADE = "KARGO_ITIRAZ_IADE";

    // Invoice Type Codes - Uluslararasi
    public static final String TYPE_ULUSLARARASI_HIZMET = "ULUSLARARASI_HIZMET";
    public static final String TYPE_AZ_YURTDISI_OPERASYON = "AZ_YURTDISI_OPERASYON";
    public static final String TYPE_AZ_PLATFORM_HIZMET = "AZ_PLATFORM_HIZMET";
    public static final String TYPE_AZ_ULUSLARARASI_HIZMET = "AZ_ULUSLARARASI_HIZMET";
    public static final String TYPE_YURTDISI_OPERASYON_IADE = "YURTDISI_OPERASYON_IADE";

    // Invoice Type Codes - Ceza
    public static final String TYPE_TEDARIK_EDEMEME = "TEDARIK_EDEMEME";
    public static final String TYPE_TERMIN_GECIKME = "TERMIN_GECIKME";
    public static final String TYPE_EKSIK_URUN = "EKSIK_URUN";
    public static final String TYPE_YANLIS_URUN = "YANLIS_URUN";
    public static final String TYPE_KUSURLU_URUN = "KUSURLU_URUN";

    // Invoice Type Codes - Reklam
    public static final String TYPE_REKLAM_BEDELI = "REKLAM_BEDELI";
    public static final String TYPE_INFLUENCER_SABIT = "INFLUENCER_SABIT";
    public static final String TYPE_INFLUENCER_KOMISYON = "INFLUENCER_KOMISYON";

    // Invoice Type Codes - Diger
    public static final String TYPE_KURUMSAL_KAMPANYA = "KURUMSAL_KAMPANYA";
    public static final String TYPE_ERKEN_ODEME_KESINTI = "ERKEN_ODEME_KESINTI";
    public static final String TYPE_KONTOR_SATIS = "KONTOR_SATIS";
    public static final String TYPE_MUSTERI_DUYURU = "MUSTERI_DUYURU";
    public static final String TYPE_TEX_TAZMIN = "TEX_TAZMIN";

    // Invoice Type Codes - Iade (Satıcıya Ödeme)
    public static final String TYPE_TZM_TAZMIN = "TZM_TAZMIN";
    public static final String TYPE_KRM_KURUMSAL_FATURA = "KRM_KURUMSAL";
    public static final String TYPE_DIF_KARGO_ITIRAZ = "DIF_KARGO_ITIRAZ";

    /**
     * Maps invoice type name to code
     */
    public static String getTypeCodeFromName(String typeName) {
        if (typeName == null) return TYPE_PLATFORM_HIZMET_BEDELI;

        return switch (typeName) {
            // Komisyon
            case "Platform Hizmet Bedeli" -> TYPE_PLATFORM_HIZMET_BEDELI;
            case "AZ - Komisyon Faturası" -> TYPE_AZ_KOMISYON;
            case "AZ-Komisyon Geliri" -> TYPE_AZ_KOMISYON_GELIRI;

            // Kargo
            case "Kargo Fatura" -> TYPE_KARGO_FATURA;
            case "AZ - Kargo Fatura" -> TYPE_AZ_KARGO;
            case "MP Kargo İtiraz İade Faturası" -> TYPE_KARGO_ITIRAZ_IADE;

            // Uluslararasi
            case "Uluslararası Hizmet Bedeli" -> TYPE_ULUSLARARASI_HIZMET;
            case "AZ-Yurtdışı Operasyon Bedeli" -> TYPE_AZ_YURTDISI_OPERASYON;
            case "AZ-Platform Hizmet Bedeli" -> TYPE_AZ_PLATFORM_HIZMET;
            case "AZ-Uluslararası Hizmet Bedeli" -> TYPE_AZ_ULUSLARARASI_HIZMET;
            case "Yurtdışı Operasyon Iade Bedeli" -> TYPE_YURTDISI_OPERASYON_IADE;

            // Ceza
            case "Tedarik Edememe" -> TYPE_TEDARIK_EDEMEME;
            case "Termin Gecikme Bedeli" -> TYPE_TERMIN_GECIKME;
            case "Eksik Ürün Faturası" -> TYPE_EKSIK_URUN;
            case "Yanlış Ürün Faturası" -> TYPE_YANLIS_URUN;
            case "Kusurlu Ürün Faturası" -> TYPE_KUSURLU_URUN;

            // Reklam
            case "Reklam Bedeli" -> TYPE_REKLAM_BEDELI;
            case "Sabit Bütçeli Influencer Reklam Bedeli" -> TYPE_INFLUENCER_SABIT;
            case "Komisyonlu İnfluencer Reklam Bedeli" -> TYPE_INFLUENCER_KOMISYON;

            // Diger
            case "Kurumsal Kampanya Yansıtma Bedeli" -> TYPE_KURUMSAL_KAMPANYA;
            case "Erken Ödeme Kesinti Faturası" -> TYPE_ERKEN_ODEME_KESINTI;
            case "Fatura Kontör Satış Bedeli" -> TYPE_KONTOR_SATIS;
            case "Müşteri Duyuruları Faturası" -> TYPE_MUSTERI_DUYURU;
            case "TEX Tazmin - İşleme- %0" -> TYPE_TEX_TAZMIN;

            default -> TYPE_PLATFORM_HIZMET_BEDELI;
        };
    }

    /**
     * Gets category from type code
     */
    public static String getCategoryFromTypeCode(String typeCode) {
        if (typeCode == null) return CATEGORY_DIGER;

        return switch (typeCode) {
            case TYPE_PLATFORM_HIZMET_BEDELI, TYPE_AZ_KOMISYON, TYPE_AZ_KOMISYON_GELIRI -> CATEGORY_KOMISYON;
            case TYPE_KARGO_FATURA, TYPE_AZ_KARGO, TYPE_KARGO_ITIRAZ_IADE -> CATEGORY_KARGO;
            case TYPE_ULUSLARARASI_HIZMET, TYPE_AZ_YURTDISI_OPERASYON, TYPE_AZ_PLATFORM_HIZMET,
                 TYPE_AZ_ULUSLARARASI_HIZMET, TYPE_YURTDISI_OPERASYON_IADE -> CATEGORY_ULUSLARARASI;
            case TYPE_TEDARIK_EDEMEME, TYPE_TERMIN_GECIKME, TYPE_EKSIK_URUN,
                 TYPE_YANLIS_URUN, TYPE_KUSURLU_URUN -> CATEGORY_CEZA;
            case TYPE_REKLAM_BEDELI, TYPE_INFLUENCER_SABIT, TYPE_INFLUENCER_KOMISYON -> CATEGORY_REKLAM;
            case TYPE_TZM_TAZMIN, TYPE_KRM_KURUMSAL_FATURA, TYPE_DIF_KARGO_ITIRAZ -> CATEGORY_IADE;
            default -> CATEGORY_DIGER;
        };
    }

    /**
     * Check if this is a refund (payment to seller)
     */
    public boolean isRefund() {
        return CATEGORY_IADE.equals(invoiceCategory) || !isDeduction;
    }
}
