package com.ecommerce.sellerx.products;

import com.ecommerce.sellerx.stores.Store;
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
@Table(name = "trendyol_products")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendyolProduct {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;
    
    @Column(name = "product_id", nullable = false)
    private String productId;
    
    @Column(name = "barcode")
    private String barcode;
    
    @Column(name = "title", nullable = false, length = 500)
    private String title;
    
    @Column(name = "category_name")
    private String categoryName;
    
    @Column(name = "create_date_time")
    private Long createDateTime;
    
    @Column(name = "has_active_campaign")
    @Builder.Default
    private Boolean hasActiveCampaign = false;
    
    @Column(name = "brand")
    private String brand;
    
    @Column(name = "brand_id")
    private Long brandId;
    
    @Column(name = "pim_category_id")
    private Long pimCategoryId;
    
    @Column(name = "product_main_id")
    private String productMainId;
    
    @Column(name = "image", columnDefinition = "TEXT")
    private String image;
    
    @Column(name = "product_url", columnDefinition = "TEXT")
    private String productUrl;
    
    @Column(name = "dimensional_weight", precision = 10, scale = 2)
    private BigDecimal dimensionalWeight;
    
    @Column(name = "sale_price", precision = 10, scale = 2)
    private BigDecimal salePrice;
    
    @Column(name = "vat_rate")
    private Integer vatRate;
    
    @Column(name = "trendyol_quantity")
    @Builder.Default
    private Integer trendyolQuantity = 0;

    @Column(name = "previous_trendyol_quantity")
    @Builder.Default
    private Integer previousTrendyolQuantity = 0;
    
    @Column(name = "commission_rate", precision = 5, scale = 2)
    private BigDecimal commissionRate;

    @Column(name = "last_commission_rate", precision = 5, scale = 2)
    private BigDecimal lastCommissionRate; // Financial API'den gelen son gerçek komisyon oranı

    @Column(name = "last_commission_date")
    private LocalDateTime lastCommissionDate; // Bu oranın geldiği tarih

    @Column(name = "last_shipping_cost_per_unit", precision = 10, scale = 2)
    private BigDecimal lastShippingCostPerUnit; // Son kargo faturasından hesaplanan birim kargo maliyeti

    @Column(name = "last_shipping_cost_date")
    private LocalDateTime lastShippingCostDate; // Bu maliyetin geldiği tarih

    @Column(name = "shipping_volume_weight", precision = 5, scale = 2)
    private BigDecimal shippingVolumeWeight;

    // ============== Reklam Metrikleri (Excel C23, C24) ==============
    @Column(name = "cpc", precision = 10, scale = 2)
    private BigDecimal cpc; // Cost Per Click (TL) - tıklama başı maliyet

    @Column(name = "cvr", precision = 5, scale = 4)
    private BigDecimal cvr; // Conversion Rate (örn: 0.0180 = %1.8)

    // ============== Varsayılan Döviz Kuru (Excel F1) ==============
    @Column(name = "default_currency", length = 3)
    private String defaultCurrency; // "TRY", "USD", "EUR"

    @Column(name = "default_exchange_rate", precision = 10, scale = 4)
    private BigDecimal defaultExchangeRate; // Varsayılan döviz kuru

    // ============== ÖTV Oranı (Excel F5) ==============
    @Column(name = "otv_rate", precision = 5, scale = 4)
    private BigDecimal otvRate; // Özel Tüketim Vergisi oranı (örn: 0.20 = %20)

    @Column(name = "approved")
    @Builder.Default
    private Boolean approved = false;
    
    @Column(name = "archived")
    @Builder.Default
    private Boolean archived = false;
    
    @Column(name = "blacklisted")
    @Builder.Default
    private Boolean blacklisted = false;
    
    @Column(name = "rejected")
    @Builder.Default
    private Boolean rejected = false;
    
    @Column(name = "on_sale")
    @Builder.Default
    private Boolean onSale = false;
    
    @Column(name = "stock_depleted")
    @Builder.Default
    private Boolean stockDepleted = false;

    @Type(JsonBinaryType.class)
    @Column(name = "cost_and_stock_info", columnDefinition = "jsonb")
    @Builder.Default
    @com.fasterxml.jackson.annotation.JsonProperty("costAndStockInfo")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private List<CostAndStockInfo> costAndStockInfo = new ArrayList<>();

    // ============== Buybox Bilgileri ==============
    @Column(name = "buybox_order")
    private Integer buyboxOrder;

    @Column(name = "buybox_price", precision = 10, scale = 2)
    private BigDecimal buyboxPrice;

    @Column(name = "has_multiple_seller")
    @Builder.Default
    private Boolean hasMultipleSeller = false;

    @Column(name = "buybox_updated_at")
    private LocalDateTime buyboxUpdatedAt;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
