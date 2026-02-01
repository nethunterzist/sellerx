package com.ecommerce.sellerx.qa;

import com.ecommerce.sellerx.users.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "trendyol_answers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendyolAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private TrendyolQuestion question;

    @Column(name = "answer_text", columnDefinition = "TEXT", nullable = false)
    private String answerText;

    @Column(name = "is_submitted")
    @Builder.Default
    private Boolean isSubmitted = false;

    @Column(name = "trendyol_answer_id")
    private String trendyolAnswerId;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by")
    private User submittedBy;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
