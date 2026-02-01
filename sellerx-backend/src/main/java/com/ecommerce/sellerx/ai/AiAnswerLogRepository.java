package com.ecommerce.sellerx.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiAnswerLogRepository extends JpaRepository<AiAnswerLog, UUID> {

    List<AiAnswerLog> findByQuestionIdOrderByCreatedAtDesc(UUID questionId);

    Optional<AiAnswerLog> findFirstByQuestionIdOrderByCreatedAtDesc(UUID questionId);

    long countByQuestionStoreId(UUID storeId);

    long countByQuestionStoreIdAndWasApprovedTrue(UUID storeId);
}
