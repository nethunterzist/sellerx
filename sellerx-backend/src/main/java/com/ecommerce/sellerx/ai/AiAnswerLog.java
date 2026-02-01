package com.ecommerce.sellerx.ai;

import com.ecommerce.sellerx.qa.TrendyolQuestion;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "ai_answer_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAnswerLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private TrendyolQuestion question;

    @Column(name = "generated_answer", columnDefinition = "TEXT", nullable = false)
    private String generatedAnswer;

    @Column(name = "confidence_score", precision = 3, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "context_used", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> contextUsed;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "generation_time_ms")
    private Integer generationTimeMs;

    @Column(name = "was_approved")
    private Boolean wasApproved;

    @Column(name = "was_edited")
    private Boolean wasEdited;

    @Column(name = "final_answer", columnDefinition = "TEXT")
    private String finalAnswer;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
