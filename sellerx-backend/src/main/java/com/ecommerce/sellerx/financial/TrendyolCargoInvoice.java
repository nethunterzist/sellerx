package com.ecommerce.sellerx.financial;

import com.ecommerce.sellerx.stores.Store;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "trendyol_cargo_invoices")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TrendyolCargoInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "invoice_serial_number", nullable = false)
    private String invoiceSerialNumber;

    @Column(name = "order_number")
    private String orderNumber;

    @Column(name = "shipment_package_id")
    private Long shipmentPackageId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "desi")
    private Integer desi;

    @Column(name = "shipment_package_type")
    private String shipmentPackageType;

    @Column(name = "vat_rate")
    @Builder.Default
    private Integer vatRate = 20;

    @Column(name = "vat_amount", precision = 10, scale = 2)
    private BigDecimal vatAmount;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Type(JsonBinaryType.class)
    @Column(name = "raw_data", columnDefinition = "jsonb")
    private Map<String, Object> rawData;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Calculate VAT amount based on amount and vatRate
     * VAT Amount = Amount * vatRate / (100 + vatRate) for VAT-inclusive amounts
     * Or Amount * vatRate / 100 for VAT-exclusive amounts
     */
    public void calculateVatAmount() {
        if (this.amount != null && this.vatRate != null && this.vatRate > 0) {
            // Assuming amount is VAT-inclusive
            this.vatAmount = this.amount.multiply(BigDecimal.valueOf(this.vatRate))
                    .divide(BigDecimal.valueOf(100 + this.vatRate), 2, java.math.RoundingMode.HALF_UP);
        }
    }
}
