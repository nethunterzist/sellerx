package com.ecommerce.sellerx.qa;

import com.ecommerce.sellerx.stores.Store;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "trendyol_questions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendyolQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "question_id", nullable = false)
    private String questionId;

    @Column(name = "product_id")
    private String productId;

    @Column(name = "barcode", length = 100)
    private String barcode;

    @Column(name = "product_title", length = 500)
    private String productTitle;

    @Column(name = "customer_question", columnDefinition = "TEXT", nullable = false)
    private String customerQuestion;

    @Column(name = "question_date", nullable = false)
    private LocalDateTime questionDate;

    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = true;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TrendyolAnswer> answers = new ArrayList<>();

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
