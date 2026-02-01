package com.ecommerce.sellerx.orders;

import com.ecommerce.sellerx.financial.FinancialOrderTransactionSummary;
import com.ecommerce.sellerx.financial.FinancialOrderItemData;
import com.ecommerce.sellerx.stores.Store;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "trendyol_orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"tyOrderNumber", "packageNo", "orderDate", "orderTransactionSummary", "orderItems"})
public class TrendyolOrder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;
    
    @Column(name = "ty_order_number", nullable = false)
    private String tyOrderNumber;
    
    @Column(name = "package_no", nullable = false)
    private Long packageNo; // This is the "id" field from Trendyol API
    
    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate; // Converted from originShipmentDate milliseconds
    
    @Column(name = "gross_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal grossAmount;
    
    @Column(name = "total_discount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalDiscount = BigDecimal.ZERO;
    
    @Column(name = "total_ty_discount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalTyDiscount = BigDecimal.ZERO;

    @Column(name = "coupon_discount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal couponDiscount = BigDecimal.ZERO;

    @Column(name = "early_payment_fee", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal earlyPaymentFee = BigDecimal.ZERO;

    @Type(JsonBinaryType.class)
    @Column(name = "order_items", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();
    
    @Type(JsonBinaryType.class)
    @Column(name = "financial_transactions", columnDefinition = "jsonb")
    @Builder.Default
    private List<FinancialOrderItemData> financialTransactions = new ArrayList<>();
    
    @Column(name = "shipment_package_status")
    private String shipmentPackageStatus;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "total_price", precision = 19, scale = 2)
    private BigDecimal totalPrice;
    
    @Column(name = "stoppage", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal stoppage = BigDecimal.ZERO;
    
    @Column(name = "estimated_commission", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal estimatedCommission = BigDecimal.ZERO;

    @Column(name = "is_commission_estimated")
    @Builder.Default
    private Boolean isCommissionEstimated = true; // true: tahmini, false: Financial API'den gerçek değer

    @Column(name = "estimated_shipping_cost", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal estimatedShippingCost = BigDecimal.ZERO; // Tahmini kargo maliyeti

    @Column(name = "is_shipping_estimated")
    @Builder.Default
    private Boolean isShippingEstimated = true; // true: tahmini, false: Kargo faturasından gerçek değer

    /**
     * Difference between estimated and real commission after reconciliation.
     * Positive: Underestimated (real > estimated)
     * Negative: Overestimated (real < estimated)
     * Null: Not yet reconciled
     */
    @Column(name = "commission_difference", precision = 10, scale = 2)
    private BigDecimal commissionDifference;

    @Column(name = "cargo_deci")
    @Builder.Default
    private Integer cargoDeci = 0;
    
    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;
    
    @Column(name = "transaction_status")
    @Builder.Default
    private String transactionStatus = "NOT_SETTLED";
    
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Shipment address city information (from Trendyol webhook)
    @Column(name = "shipment_city")
    private String shipmentCity;

    @Column(name = "shipment_city_code")
    private Integer shipmentCityCode;

    @Column(name = "shipment_district")
    private String shipmentDistrict;

    @Column(name = "shipment_district_id")
    private Integer shipmentDistrictId;

    /**
     * Source of order data:
     * - ORDER_API: Full data from Trendyol Orders API (last 90 days)
     * - SETTLEMENT_API: Limited data from Financial Settlements API (historical orders >90 days)
     */
    @Column(name = "data_source", nullable = false)
    @Builder.Default
    private String dataSource = "ORDER_API";

    // Financial summary of all order items (calculated field, not stored in DB)
    @Transient
    private FinancialOrderTransactionSummary orderTransactionSummary;
    
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
