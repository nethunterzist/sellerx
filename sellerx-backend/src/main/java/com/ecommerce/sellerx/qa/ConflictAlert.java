package com.ecommerce.sellerx.qa;

import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.users.User;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "conflict_alerts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private TrendyolQuestion question;

    @Column(name = "conflict_type", nullable = false, length = 30)
    private String conflictType;

    @Column(name = "severity", nullable = false, length = 10)
    private String severity;

    @Column(name = "source_a_type", length = 50)
    private String sourceAType;

    @Column(name = "source_a_content", columnDefinition = "TEXT")
    private String sourceAContent;

    @Column(name = "source_b_type", length = 50)
    private String sourceBType;

    @Column(name = "source_b_content", columnDefinition = "TEXT")
    private String sourceBContent;

    @Type(JsonBinaryType.class)
    @Column(name = "detected_keywords", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> detectedKeywords = new ArrayList<>();

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = STATUS_ACTIVE;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // Status constants
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_DISMISSED = "DISMISSED";

    // Conflict type constants
    public static final String TYPE_KNOWLEDGE_VS_TRENDYOL = "KNOWLEDGE_VS_TRENDYOL";
    public static final String TYPE_BRAND_INCONSISTENCY = "BRAND_INCONSISTENCY";
    public static final String TYPE_LEGAL_RISK = "LEGAL_RISK";
    public static final String TYPE_HEALTH_SAFETY = "HEALTH_SAFETY";

    // Severity constants
    public static final String SEVERITY_LOW = "LOW";
    public static final String SEVERITY_MEDIUM = "MEDIUM";
    public static final String SEVERITY_HIGH = "HIGH";
    public static final String SEVERITY_CRITICAL = "CRITICAL";

    // Source type constants
    public static final String SOURCE_KNOWLEDGE_BASE = "KNOWLEDGE_BASE";
    public static final String SOURCE_TRENDYOL_DATA = "TRENDYOL_DATA";
    public static final String SOURCE_BRAND_INFO = "BRAND_INFO";
}
