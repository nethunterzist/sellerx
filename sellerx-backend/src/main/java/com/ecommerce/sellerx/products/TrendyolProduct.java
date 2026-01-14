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
    
    @Column(name = "commission_rate", precision = 5, scale = 2)
    private BigDecimal commissionRate;
    
    @Column(name = "shipping_volume_weight", precision = 5, scale = 2)
    private BigDecimal shippingVolumeWeight;
    
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
    
    @Type(JsonBinaryType.class)
    @Column(name = "cost_and_stock_info", columnDefinition = "jsonb")
    @Builder.Default
    @com.fasterxml.jackson.annotation.JsonProperty("costAndStockInfo")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private List<CostAndStockInfo> costAndStockInfo = new ArrayList<>();
    
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
