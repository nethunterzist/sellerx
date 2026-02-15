package com.ecommerce.sellerx.crosssell;

import com.ecommerce.sellerx.qa.TrendyolQuestion;
import com.ecommerce.sellerx.stores.Store;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cross_sell_analytics", indexes = {
    @Index(name = "idx_cross_sell_analytics_store", columnList = "store_id"),
    @Index(name = "idx_cross_sell_analytics_rule", columnList = "rule_id"),
    @Index(name = "idx_cross_sell_analytics_created", columnList = "store_id, created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossSellAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id")
    private CrossSellRule rule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private TrendyolQuestion question;

    @Column(name = "recommended_barcode", nullable = false, length = 100)
    private String recommendedBarcode;

    @Column(name = "was_included_in_answer", nullable = false)
    @Builder.Default
    private Boolean wasIncludedInAnswer = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
