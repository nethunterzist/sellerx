package com.ecommerce.sellerx.qa;

import com.ecommerce.sellerx.ai.StoreKnowledgeBase;
import com.ecommerce.sellerx.ai.StoreKnowledgeBaseRepository;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.users.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Knowledge Suggestion Service
 *
 * Manages AI-discovered knowledge suggestions:
 * - Approve: Add to Knowledge Base
 * - Reject: Mark as rejected
 * - Modify: Edit and approve
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeSuggestionService {

    private final KnowledgeSuggestionRepository suggestionRepository;
    private final StoreKnowledgeBaseRepository knowledgeBaseRepository;
    private final StoreRepository storeRepository;

    /**
     * Get all suggestions for a store with optional status filter
     */
    public List<KnowledgeSuggestionDto> getSuggestions(UUID storeId, String status) {
        List<KnowledgeSuggestion> suggestions;

        if (status != null && !status.isEmpty()) {
            suggestions = suggestionRepository
                    .findByStoreIdAndStatusOrderByPriorityDescCreatedAtDesc(storeId, status);
        } else {
            suggestions = suggestionRepository
                    .findByStoreIdOrderByPriorityDescCreatedAtDesc(storeId);
        }

        return suggestions.stream()
                .map(KnowledgeSuggestionDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get pending suggestions count
     */
    public long getPendingSuggestionCount(UUID storeId) {
        return suggestionRepository.countByStoreIdAndStatus(storeId, KnowledgeSuggestion.STATUS_PENDING);
    }

    /**
     * Get suggestion by ID
     */
    public KnowledgeSuggestionDto getSuggestion(UUID suggestionId) {
        return suggestionRepository.findById(suggestionId)
                .map(KnowledgeSuggestionDto::fromEntity)
                .orElseThrow(() -> new RuntimeException("Suggestion not found: " + suggestionId));
    }

    /**
     * Approve a suggestion - creates a Knowledge Base entry
     */
    @Transactional
    public KnowledgeSuggestionDto approveSuggestion(UUID suggestionId, User user) {
        KnowledgeSuggestion suggestion = suggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new RuntimeException("Suggestion not found: " + suggestionId));

        if (!KnowledgeSuggestion.STATUS_PENDING.equals(suggestion.getStatus())) {
            throw new IllegalStateException("Suggestion is not in PENDING status");
        }

        // Create Knowledge Base entry
        StoreKnowledgeBase knowledge = StoreKnowledgeBase.builder()
                .store(suggestion.getStore())
                .title(suggestion.getSuggestedTitle())
                .content(suggestion.getSuggestedContent())
                .category("AI_DISCOVERED")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        StoreKnowledgeBase savedKnowledge = knowledgeBaseRepository.save(knowledge);

        // Update suggestion
        suggestion.setStatus(KnowledgeSuggestion.STATUS_ACCEPTED);
        suggestion.setReviewedAt(LocalDateTime.now());
        suggestion.setReviewedBy(user);
        suggestion.setCreatedKnowledgeId(savedKnowledge.getId());

        suggestionRepository.save(suggestion);

        log.info("User {} approved suggestion {} -> created knowledge {}",
                user.getEmail(), suggestionId, savedKnowledge.getId());

        return KnowledgeSuggestionDto.fromEntity(suggestion);
    }

    /**
     * Reject a suggestion
     */
    @Transactional
    public KnowledgeSuggestionDto rejectSuggestion(UUID suggestionId, User user, String reason) {
        KnowledgeSuggestion suggestion = suggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new RuntimeException("Suggestion not found: " + suggestionId));

        if (!KnowledgeSuggestion.STATUS_PENDING.equals(suggestion.getStatus())) {
            throw new IllegalStateException("Suggestion is not in PENDING status");
        }

        suggestion.setStatus(KnowledgeSuggestion.STATUS_REJECTED);
        suggestion.setReviewedAt(LocalDateTime.now());
        suggestion.setReviewedBy(user);
        suggestion.setReviewNotes(reason);

        suggestionRepository.save(suggestion);

        log.info("User {} rejected suggestion {}, reason: {}",
                user.getEmail(), suggestionId, reason);

        return KnowledgeSuggestionDto.fromEntity(suggestion);
    }

    /**
     * Modify and approve a suggestion
     */
    @Transactional
    public KnowledgeSuggestionDto modifySuggestion(
            UUID suggestionId,
            User user,
            String modifiedTitle,
            String modifiedContent) {

        KnowledgeSuggestion suggestion = suggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new RuntimeException("Suggestion not found: " + suggestionId));

        if (!KnowledgeSuggestion.STATUS_PENDING.equals(suggestion.getStatus())) {
            throw new IllegalStateException("Suggestion is not in PENDING status");
        }

        // Create Knowledge Base entry with modified content
        StoreKnowledgeBase knowledge = StoreKnowledgeBase.builder()
                .store(suggestion.getStore())
                .title(modifiedTitle != null ? modifiedTitle : suggestion.getSuggestedTitle())
                .content(modifiedContent != null ? modifiedContent : suggestion.getSuggestedContent())
                .category("AI_DISCOVERED_MODIFIED")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        StoreKnowledgeBase savedKnowledge = knowledgeBaseRepository.save(knowledge);

        // Update suggestion
        suggestion.setStatus(KnowledgeSuggestion.STATUS_MODIFIED);
        suggestion.setReviewedAt(LocalDateTime.now());
        suggestion.setReviewedBy(user);
        suggestion.setReviewNotes("İçerik düzenlenerek onaylandı");
        suggestion.setCreatedKnowledgeId(savedKnowledge.getId());

        suggestionRepository.save(suggestion);

        log.info("User {} modified and approved suggestion {} -> created knowledge {}",
                user.getEmail(), suggestionId, savedKnowledge.getId());

        return KnowledgeSuggestionDto.fromEntity(suggestion);
    }

    /**
     * Get suggestion counts grouped by priority
     */
    public Map<String, Long> getSuggestionCountsByPriority(UUID storeId) {
        var results = suggestionRepository.countByStoreIdGroupByPriority(storeId);

        return results.stream()
                .collect(Collectors.toMap(
                        row -> row.getGroupName(),
                        row -> row.getGroupCount()
                ));
    }
}
