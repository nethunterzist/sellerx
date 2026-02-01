package com.ecommerce.sellerx.qa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrendyolAnswerRepository extends JpaRepository<TrendyolAnswer, UUID> {

    // Find answers by question
    List<TrendyolAnswer> findByQuestionIdOrderByCreatedAtDesc(UUID questionId);

    // Find latest answer for a question
    Optional<TrendyolAnswer> findFirstByQuestionIdOrderByCreatedAtDesc(UUID questionId);

    // Find unsubmitted answers
    List<TrendyolAnswer> findByIsSubmittedFalse();

    // Check if question has a submitted answer
    boolean existsByQuestionIdAndIsSubmittedTrue(UUID questionId);

    // Find by Trendyol answer ID (to prevent duplicates when syncing)
    Optional<TrendyolAnswer> findByTrendyolAnswerId(String trendyolAnswerId);
}
