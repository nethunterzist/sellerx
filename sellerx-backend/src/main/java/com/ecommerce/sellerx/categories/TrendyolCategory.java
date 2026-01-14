package com.ecommerce.sellerx.categories;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "trendyol_categories")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendyolCategory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "category_id", nullable = false, unique = true)
    private Long categoryId;
    
    @Column(name = "category_name", nullable = false, length = 500)
    private String categoryName;
    
    @Column(name = "parent_category", length = 500)
    private String parentCategory;
    
    @Column(name = "commission_rate", precision = 5, scale = 2)
    private BigDecimal commissionRate;
    
    @Column(name = "average_shipment_size", precision = 5, scale = 2)
    private BigDecimal averageShipmentSize;
}
