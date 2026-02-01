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
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Conflict Detection Service
 *
 * AI cevap üretirken çelişki ve risk tespiti
 * - LEGAL_RISK: Hukuki anahtar kelimeler
 * - HEALTH_SAFETY: Sağlık/güvenlik riskleri
 * - KNOWLEDGE_VS_TRENDYOL: Bilgi Bankası vs Trendyol verisi çelişkileri
 * - BRAND_INCONSISTENCY: Marka tutarsızlıkları
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConflictDetectionService {

    private final ConflictAlertRepository alertRepository;
    private final StoreKnowledgeBaseRepository knowledgeBaseRepository;
    private final StoreRepository storeRepository;

    // Legal risk keywords (Turkish)
    private static final List<String> LEGAL_KEYWORDS = Arrays.asList(
            "avukat", "dava", "mahkeme", "savcı", "şikayet", "tazminat",
            "sahte", "taklit", "dolandırıcılık", "hırsızlık", "suç",
            "yasal işlem", "hukuki", "ceza", "kovuşturma", "ifade"
    );

    // Health/Safety risk keywords (Turkish)
    private static final List<String> HEALTH_SAFETY_KEYWORDS = Arrays.asList(
            "hamile", "emzirme", "bebek", "çocuk", "alerji", "alerjik",
            "hastalık", "ilaç", "doktor", "hastane", "zehir", "tehlike",
            "yan etki", "ölüm", "yaralanma", "güvenlik", "risk"
    );

    // Brand-sensitive keywords
    private static final List<String> BRAND_KEYWORDS = Arrays.asList(
            "garanti", "orijinal", "fiyat", "indirim", "kampanya",
            "iade", "değişim", "kargo", "teslimat", "stok"
    );

    /**
     * Pre-generation conflict check - runs BEFORE AI generates answer
     * Returns blocking conflicts that prevent AI from generating
     */
    @Transactional
    public ConflictCheckResult checkBeforeGeneration(TrendyolQuestion question) {
        List<ConflictAlert> detectedConflicts = new ArrayList<>();
        boolean shouldBlock = false;

        // Check for legal risk keywords
        List<String> legalMatches = findKeywordMatches(question.getCustomerQuestion(), LEGAL_KEYWORDS);
        if (!legalMatches.isEmpty()) {
            ConflictAlert alert = createAlert(
                    question.getStore(),
                    question,
                    ConflictAlert.TYPE_LEGAL_RISK,
                    ConflictAlert.SEVERITY_CRITICAL,
                    "CUSTOMER_QUESTION",
                    question.getCustomerQuestion(),
                    null,
                    null,
                    legalMatches
            );
            detectedConflicts.add(alert);
            shouldBlock = true;

            log.warn("Legal risk detected in question {}: keywords {}",
                    question.getId(), legalMatches);
        }

        // Check for health/safety risk keywords
        List<String> healthMatches = findKeywordMatches(question.getCustomerQuestion(), HEALTH_SAFETY_KEYWORDS);
        if (!healthMatches.isEmpty()) {
            ConflictAlert alert = createAlert(
                    question.getStore(),
                    question,
                    ConflictAlert.TYPE_HEALTH_SAFETY,
                    ConflictAlert.SEVERITY_HIGH,
                    "CUSTOMER_QUESTION",
                    question.getCustomerQuestion(),
                    null,
                    null,
                    healthMatches
            );
            detectedConflicts.add(alert);

            // Health/safety is warning, not blocking
            log.warn("Health/safety risk detected in question {}: keywords {}",
                    question.getId(), healthMatches);
        }

        return new ConflictCheckResult(shouldBlock, detectedConflicts);
    }

    /**
     * Post-generation conflict check - runs AFTER AI generates answer
     * Checks for data conflicts between knowledge base and Trendyol data
     */
    @Transactional
    public List<ConflictAlert> checkAfterGeneration(TrendyolQuestion question, String aiResponse) {
        List<ConflictAlert> detectedConflicts = new ArrayList<>();

        UUID storeId = question.getStore().getId();

        // Get relevant knowledge base entries
        List<StoreKnowledgeBase> knowledgeEntries = knowledgeBaseRepository
                .findByStoreIdAndIsActiveTrueOrderByPriorityDesc(storeId);

        // Check for knowledge conflicts
        for (StoreKnowledgeBase knowledge : knowledgeEntries) {
            if (knowledge.getContent() == null) continue;

            // Find overlapping keywords
            List<String> commonKeywords = findBrandKeywordMatches(
                    question.getCustomerQuestion() + " " + aiResponse, BRAND_KEYWORDS);

            if (commonKeywords.isEmpty()) continue;

            // Check for conflicting information
            ConflictInfo conflict = detectContentConflict(
                    knowledge.getContent(),
                    aiResponse,
                    commonKeywords
            );

            if (conflict != null) {
                ConflictAlert alert = createAlert(
                        question.getStore(),
                        question,
                        ConflictAlert.TYPE_KNOWLEDGE_VS_TRENDYOL,
                        conflict.severity,
                        "KNOWLEDGE_BASE",
                        knowledge.getContent(),
                        "AI_RESPONSE",
                        aiResponse,
                        conflict.keywords
                );
                detectedConflicts.add(alert);

                log.warn("Knowledge conflict detected in question {}: {} vs AI response",
                        question.getId(), knowledge.getTitle());
            }
        }

        // Check for brand inconsistency in the response
        List<String> brandMatches = findBrandKeywordMatches(aiResponse, BRAND_KEYWORDS);
        if (!brandMatches.isEmpty()) {
            // Verify numbers/facts in the response against stored data
            ConflictInfo brandConflict = detectBrandInconsistency(question, aiResponse, brandMatches);

            if (brandConflict != null) {
                ConflictAlert alert = createAlert(
                        question.getStore(),
                        question,
                        ConflictAlert.TYPE_BRAND_INCONSISTENCY,
                        brandConflict.severity,
                        "AI_RESPONSE",
                        aiResponse,
                        "TRENDYOL_DATA",
                        brandConflict.conflictingContent,
                        brandConflict.keywords
                );
                detectedConflicts.add(alert);

                log.warn("Brand inconsistency detected in question {}: keywords {}",
                        question.getId(), brandMatches);
            }
        }

        return detectedConflicts;
    }

    /**
     * Create and save a conflict alert
     */
    @Transactional
    public ConflictAlert createAlert(
            Store store,
            TrendyolQuestion question,
            String conflictType,
            String severity,
            String sourceAType,
            String sourceAContent,
            String sourceBType,
            String sourceBContent,
            List<String> detectedKeywords) {

        ConflictAlert alert = new ConflictAlert();
        alert.setStore(store);
        alert.setQuestion(question);
        alert.setConflictType(conflictType);
        alert.setSeverity(severity);
        alert.setSourceAType(sourceAType);
        alert.setSourceAContent(truncateContent(sourceAContent, 500));
        alert.setSourceBType(sourceBType);
        alert.setSourceBContent(truncateContent(sourceBContent, 500));
        alert.setDetectedKeywords(detectedKeywords);
        alert.setStatus(ConflictAlert.STATUS_ACTIVE);
        alert.setCreatedAt(LocalDateTime.now());

        return alertRepository.save(alert);
    }

    /**
     * Resolve a conflict alert
     */
    @Transactional
    public ConflictAlertDto resolveAlert(UUID alertId, User user, String resolutionNotes) {
        ConflictAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Conflict alert not found: " + alertId));

        alert.setStatus(ConflictAlert.STATUS_RESOLVED);
        alert.setResolvedAt(LocalDateTime.now());
        alert.setResolvedBy(user);
        alert.setResolutionNotes(resolutionNotes);

        alertRepository.save(alert);

        log.info("User {} resolved conflict alert {}", user.getEmail(), alertId);

        return ConflictAlertDto.fromEntity(alert);
    }

    /**
     * Dismiss a conflict alert
     */
    @Transactional
    public ConflictAlertDto dismissAlert(UUID alertId, User user) {
        ConflictAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Conflict alert not found: " + alertId));

        alert.setStatus(ConflictAlert.STATUS_DISMISSED);
        alert.setResolvedAt(LocalDateTime.now());
        alert.setResolvedBy(user);

        alertRepository.save(alert);

        log.info("User {} dismissed conflict alert {}", user.getEmail(), alertId);

        return ConflictAlertDto.fromEntity(alert);
    }

    /**
     * Get active alerts for a store
     */
    public List<ConflictAlertDto> getActiveAlerts(UUID storeId) {
        return alertRepository.findByStoreIdAndStatusOrderBySeverityDescCreatedAtDesc(
                        storeId, ConflictAlert.STATUS_ACTIVE)
                .stream()
                .map(ConflictAlertDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get all alerts for a store
     */
    public List<ConflictAlertDto> getAlerts(UUID storeId, String status) {
        List<ConflictAlert> alerts;

        if (status != null && !status.isEmpty()) {
            alerts = alertRepository.findByStoreIdAndStatusOrderBySeverityDescCreatedAtDesc(storeId, status);
        } else {
            alerts = alertRepository.findByStoreIdOrderByCreatedAtDesc(storeId);
        }

        return alerts.stream()
                .map(ConflictAlertDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get conflict statistics for a store
     */
    public ConflictStatsDto getConflictStats(UUID storeId) {
        return alertRepository.getConflictStats(storeId);
    }

    /**
     * Get active alert count
     */
    public long getActiveAlertCount(UUID storeId) {
        return alertRepository.countByStoreIdAndStatus(storeId, ConflictAlert.STATUS_ACTIVE);
    }

    /**
     * Check if a question has critical conflicts
     */
    public boolean hasCriticalConflicts(UUID questionId) {
        return alertRepository.hasCriticalAlertsForQuestion(questionId);
    }

    // Helper methods

    private List<String> findKeywordMatches(String text, List<String> keywords) {
        if (text == null || text.isEmpty()) return Collections.emptyList();

        String lowerText = text.toLowerCase(new Locale("tr", "TR"));
        return keywords.stream()
                .filter(keyword -> lowerText.contains(keyword.toLowerCase(new Locale("tr", "TR"))))
                .collect(Collectors.toList());
    }

    private List<String> findBrandKeywordMatches(String text, List<String> keywords) {
        if (text == null || text.isEmpty()) return Collections.emptyList();

        String lowerText = text.toLowerCase(new Locale("tr", "TR"));
        return keywords.stream()
                .filter(keyword -> {
                    String lowerKeyword = keyword.toLowerCase(new Locale("tr", "TR"));
                    return lowerText.contains(lowerKeyword);
                })
                .collect(Collectors.toList());
    }

    private ConflictInfo detectContentConflict(String knowledgeContent, String aiResponse, List<String> keywords) {
        // Simple number extraction and comparison
        Pattern numberPattern = Pattern.compile("\\d+");

        for (String keyword : keywords) {
            // Find context around keyword in both texts
            String knowledgeContext = extractContext(knowledgeContent, keyword);
            String responseContext = extractContext(aiResponse, keyword);

            if (knowledgeContext == null || responseContext == null) continue;

            // Extract numbers from both contexts
            java.util.regex.Matcher knowledgeMatcher = numberPattern.matcher(knowledgeContext);
            java.util.regex.Matcher responseMatcher = numberPattern.matcher(responseContext);

            List<Integer> knowledgeNumbers = new ArrayList<>();
            List<Integer> responseNumbers = new ArrayList<>();

            while (knowledgeMatcher.find()) {
                try {
                    knowledgeNumbers.add(Integer.parseInt(knowledgeMatcher.group()));
                } catch (NumberFormatException ignored) {}
            }

            while (responseMatcher.find()) {
                try {
                    responseNumbers.add(Integer.parseInt(responseMatcher.group()));
                } catch (NumberFormatException ignored) {}
            }

            // Check for number mismatches (e.g., "2 yıl garanti" vs "1 yıl garanti")
            if (!knowledgeNumbers.isEmpty() && !responseNumbers.isEmpty()) {
                for (Integer kNum : knowledgeNumbers) {
                    for (Integer rNum : responseNumbers) {
                        if (!kNum.equals(rNum) && Math.abs(kNum - rNum) > 0) {
                            return new ConflictInfo(
                                    ConflictAlert.SEVERITY_MEDIUM,
                                    Collections.singletonList(keyword),
                                    String.format("Bilgi Bankası: %s, AI Cevabı: %s", knowledgeContext, responseContext)
                            );
                        }
                    }
                }
            }
        }

        return null;
    }

    private ConflictInfo detectBrandInconsistency(TrendyolQuestion question, String aiResponse, List<String> keywords) {
        // This would normally compare against Trendyol product data
        // For now, we check for common inconsistency patterns

        String lowerResponse = aiResponse.toLowerCase(new Locale("tr", "TR"));

        // Check for definitive statements that might be wrong
        if (keywords.contains("garanti") && lowerResponse.contains("yıl")) {
            // Extract warranty period and verify
            Pattern warrantyPattern = Pattern.compile("(\\d+)\\s*yıl");
            java.util.regex.Matcher matcher = warrantyPattern.matcher(lowerResponse);

            if (matcher.find()) {
                int years = Integer.parseInt(matcher.group(1));
                if (years > 5) { // Suspicious warranty period
                    return new ConflictInfo(
                            ConflictAlert.SEVERITY_LOW,
                            Collections.singletonList("garanti"),
                            "Garanti süresi doğrulanmalı: " + years + " yıl"
                    );
                }
            }
        }

        return null;
    }

    private String extractContext(String text, String keyword) {
        if (text == null || keyword == null) return null;

        String lowerText = text.toLowerCase(new Locale("tr", "TR"));
        String lowerKeyword = keyword.toLowerCase(new Locale("tr", "TR"));

        int index = lowerText.indexOf(lowerKeyword);
        if (index == -1) return null;

        int start = Math.max(0, index - 30);
        int end = Math.min(text.length(), index + keyword.length() + 30);

        return text.substring(start, end);
    }

    private String truncateContent(String content, int maxLength) {
        if (content == null) return null;
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength) + "...";
    }

    // Result classes

    public static class ConflictCheckResult {
        public final boolean shouldBlock;
        public final List<ConflictAlert> conflicts;

        public ConflictCheckResult(boolean shouldBlock, List<ConflictAlert> conflicts) {
            this.shouldBlock = shouldBlock;
            this.conflicts = conflicts;
        }
    }

    private static class ConflictInfo {
        public final String severity;
        public final List<String> keywords;
        public final String conflictingContent;

        public ConflictInfo(String severity, List<String> keywords, String conflictingContent) {
            this.severity = severity;
            this.keywords = keywords;
            this.conflictingContent = conflictingContent;
        }
    }
}
