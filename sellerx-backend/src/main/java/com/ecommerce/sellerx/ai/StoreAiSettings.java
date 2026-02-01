package com.ecommerce.sellerx.ai;

import com.ecommerce.sellerx.stores.Store;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "store_ai_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreAiSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false, unique = true)
    private Store store;

    @Column(name = "ai_enabled")
    @Builder.Default
    private Boolean aiEnabled = false;

    @Column(name = "auto_answer")
    @Builder.Default
    private Boolean autoAnswer = false;

    @Column(name = "tone", length = 50)
    @Builder.Default
    private String tone = "professional";

    @Column(name = "language", length = 10)
    @Builder.Default
    private String language = "tr";

    @Column(name = "max_answer_length")
    @Builder.Default
    private Integer maxAnswerLength = 500;

    @Column(name = "include_greeting")
    @Builder.Default
    private Boolean includeGreeting = true;

    @Column(name = "include_signature")
    @Builder.Default
    private Boolean includeSignature = true;

    @Column(name = "signature_text", length = 255)
    private String signatureText;

    @Column(name = "confidence_threshold", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal confidenceThreshold = new BigDecimal("0.80");

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
