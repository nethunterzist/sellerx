package com.ecommerce.sellerx.qa;

import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.users.User;
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
@Table(name = "knowledge_suggestions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "suggested_title", nullable = false)
    private String suggestedTitle;

    @Column(name = "suggested_content", columnDefinition = "TEXT", nullable = false)
    private String suggestedContent;

    @Type(JsonBinaryType.class)
    @Column(name = "sample_questions", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> sampleQuestions = new ArrayList<>();

    @Column(name = "question_count", nullable = false)
    @Builder.Default
    private Integer questionCount = 1;

    @Column(name = "avg_similarity", precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal avgSimilarity = BigDecimal.ZERO;

    @Column(name = "first_seen_at", nullable = false)
    @Builder.Default
    private LocalDateTime firstSeenAt = LocalDateTime.now();

    @Column(name = "last_seen_at", nullable = false)
    @Builder.Default
    private LocalDateTime lastSeenAt = LocalDateTime.now();

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "priority", nullable = false, length = 10)
    @Builder.Default
    private String priority = "MEDIUM";

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "created_knowledge_id")
    private UUID createdKnowledgeId;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Status enum values
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_MODIFIED = "MODIFIED";

    // Priority enum values
    public static final String PRIORITY_LOW = "LOW";
    public static final String PRIORITY_MEDIUM = "MEDIUM";
    public static final String PRIORITY_HIGH = "HIGH";
}
