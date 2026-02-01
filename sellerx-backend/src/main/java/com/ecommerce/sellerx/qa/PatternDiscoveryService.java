package com.ecommerce.sellerx.qa;

import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Pattern Discovery Service
 *
 * Real-time: Her yeni soru geldiğinde hafif MinHash kontrolü
 * Batch: Günlük analiz ile yeni kalıplar keşfetme
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PatternDiscoveryService {

    private final QaPatternRepository patternRepository;
    private final KnowledgeSuggestionRepository suggestionRepository;
    private final TrendyolQuestionRepository questionRepository;
    private final StoreRepository storeRepository;

    // Similarity threshold for pattern matching
    private static final double SIMILARITY_THRESHOLD = 0.7;

    // Minimum questions to create a suggestion
    private static final int MIN_QUESTIONS_FOR_SUGGESTION = 3;

    /**
     * Real-time pattern check - called when a new question arrives
     * Returns existing pattern if found, null otherwise
     */
    @Transactional
    public QaPattern checkQuestionPattern(TrendyolQuestion question) {
        String patternHash = generatePatternHash(question.getCustomerQuestion());

        Optional<QaPattern> existingPattern = patternRepository
                .findByStoreIdAndPatternHash(question.getStore().getId(), patternHash);

        if (existingPattern.isPresent()) {
            QaPattern pattern = existingPattern.get();
            incrementPatternOccurrence(pattern);
            return pattern;
        }

        // Try fuzzy matching with existing patterns
        List<QaPattern> storePatterns = patternRepository.findByStoreId(question.getStore().getId());
        for (QaPattern pattern : storePatterns) {
            double similarity = calculateSimilarity(
                    normalizeQuestion(question.getCustomerQuestion()),
                    normalizeQuestion(pattern.getCanonicalQuestion())
            );

            if (similarity >= SIMILARITY_THRESHOLD) {
                incrementPatternOccurrence(pattern);
                return pattern;
            }
        }

        return null;
    }

    /**
     * Create a new pattern for a question
     */
    @Transactional
    public QaPattern createPattern(TrendyolQuestion question, String answer) {
        String patternHash = generatePatternHash(question.getCustomerQuestion());

        QaPattern pattern = new QaPattern();
        pattern.setStore(question.getStore());
        pattern.setPatternHash(patternHash);
        pattern.setCanonicalQuestion(question.getCustomerQuestion());
        pattern.setCanonicalAnswer(answer);
        pattern.setOccurrenceCount(1);
        pattern.setApprovalCount(0);
        pattern.setRejectionCount(0);
        pattern.setModificationCount(0);
        pattern.setConfidenceScore(BigDecimal.ZERO);
        pattern.setSeniorityLevel(QaPattern.SENIORITY_JUNIOR);
        pattern.setIsAutoSubmitEligible(false);
        pattern.setProductId(question.getProductId());
        pattern.setCategory(null); // TrendyolQuestion doesn't have category field
        pattern.setFirstSeenAt(LocalDateTime.now());
        pattern.setLastSeenAt(LocalDateTime.now());

        return patternRepository.save(pattern);
    }

    /**
     * Increment pattern occurrence count
     */
    @Transactional
    public void incrementPatternOccurrence(QaPattern pattern) {
        pattern.setOccurrenceCount(pattern.getOccurrenceCount() + 1);
        pattern.setLastSeenAt(LocalDateTime.now());
        patternRepository.save(pattern);
    }

    /**
     * Batch job: Analyze questions and create knowledge suggestions
     * Runs daily at 03:00
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void runDailyPatternAnalysis() {
        log.info("Starting daily pattern analysis...");

        List<Store> stores = storeRepository.findAll();
        for (Store store : stores) {
            try {
                analyzeStoreQuestions(store.getId());
            } catch (Exception e) {
                log.error("Error analyzing questions for store {}: {}", store.getId(), e.getMessage());
            }
        }

        log.info("Daily pattern analysis completed");
    }

    /**
     * Analyze questions for a specific store
     */
    @Transactional
    public void analyzeStoreQuestions(UUID storeId) {
        // Get questions from last 7 days that don't have patterns yet
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<TrendyolQuestion> recentQuestions = questionRepository
                .findByStoreIdAndCreatedAtAfter(storeId, weekAgo);

        if (recentQuestions.isEmpty()) {
            return;
        }

        // Group similar questions
        Map<String, List<TrendyolQuestion>> clusters = clusterSimilarQuestions(recentQuestions);

        // Create suggestions for clusters meeting threshold
        for (Map.Entry<String, List<TrendyolQuestion>> entry : clusters.entrySet()) {
            List<TrendyolQuestion> cluster = entry.getValue();

            if (cluster.size() >= MIN_QUESTIONS_FOR_SUGGESTION) {
                createSuggestionFromCluster(storeId, cluster);
            }
        }
    }

    /**
     * Cluster similar questions using normalized hash
     */
    private Map<String, List<TrendyolQuestion>> clusterSimilarQuestions(List<TrendyolQuestion> questions) {
        Map<String, List<TrendyolQuestion>> clusters = new HashMap<>();

        for (TrendyolQuestion question : questions) {
            String normalized = normalizeQuestion(question.getCustomerQuestion());
            String clusterKey = generatePatternHash(normalized);

            // Try to find existing cluster with similar questions
            boolean foundCluster = false;
            for (Map.Entry<String, List<TrendyolQuestion>> entry : clusters.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    String existingNormalized = normalizeQuestion(
                            entry.getValue().get(0).getCustomerQuestion()
                    );

                    if (calculateSimilarity(normalized, existingNormalized) >= SIMILARITY_THRESHOLD) {
                        entry.getValue().add(question);
                        foundCluster = true;
                        break;
                    }
                }
            }

            if (!foundCluster) {
                clusters.computeIfAbsent(clusterKey, k -> new ArrayList<>()).add(question);
            }
        }

        return clusters;
    }

    /**
     * Create a knowledge suggestion from a question cluster
     */
    @Transactional
    public void createSuggestionFromCluster(UUID storeId, List<TrendyolQuestion> cluster) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found: " + storeId));

        // Find representative question (most common pattern)
        TrendyolQuestion representative = cluster.get(0);

        // Check if similar suggestion already exists
        String suggestedTitle = extractKeywords(representative.getCustomerQuestion());
        List<KnowledgeSuggestion> existing = suggestionRepository
                .findByStoreIdAndStatusOrderByPriorityDescCreatedAtDesc(storeId, KnowledgeSuggestion.STATUS_PENDING);

        for (KnowledgeSuggestion suggestion : existing) {
            if (calculateSimilarity(
                    normalizeQuestion(suggestion.getSuggestedTitle()),
                    normalizeQuestion(suggestedTitle)) >= SIMILARITY_THRESHOLD) {
                // Update existing suggestion
                suggestion.setQuestionCount(suggestion.getQuestionCount() + cluster.size());
                suggestion.setLastSeenAt(LocalDateTime.now());
                updatePriority(suggestion);
                suggestionRepository.save(suggestion);
                return;
            }
        }

        // Create new suggestion
        KnowledgeSuggestion suggestion = new KnowledgeSuggestion();
        suggestion.setStore(store);
        suggestion.setSuggestedTitle(suggestedTitle);
        suggestion.setSuggestedContent(generateSuggestedContent(cluster));
        suggestion.setSampleQuestions(cluster.stream()
                .limit(5)
                .map(TrendyolQuestion::getCustomerQuestion)
                .collect(Collectors.toList()));
        suggestion.setQuestionCount(cluster.size());
        suggestion.setAvgSimilarity(BigDecimal.valueOf(0.85)); // Approximate
        suggestion.setStatus(KnowledgeSuggestion.STATUS_PENDING);
        suggestion.setFirstSeenAt(LocalDateTime.now());
        suggestion.setLastSeenAt(LocalDateTime.now());

        updatePriority(suggestion);

        suggestionRepository.save(suggestion);

        log.info("Created knowledge suggestion for store {} with {} questions: {}",
                storeId, cluster.size(), suggestedTitle);
    }

    /**
     * Update suggestion priority based on question count
     */
    private void updatePriority(KnowledgeSuggestion suggestion) {
        if (suggestion.getQuestionCount() >= 15) {
            suggestion.setPriority(KnowledgeSuggestion.PRIORITY_HIGH);
        } else if (suggestion.getQuestionCount() >= 8) {
            suggestion.setPriority(KnowledgeSuggestion.PRIORITY_MEDIUM);
        } else {
            suggestion.setPriority(KnowledgeSuggestion.PRIORITY_LOW);
        }
    }

    /**
     * Extract keywords from question for title
     */
    private String extractKeywords(String question) {
        // Simple keyword extraction - remove common words
        String[] stopWords = {"bu", "bir", "ne", "mı", "mi", "mu", "mü", "nasıl", "kaç",
                "var", "mıdır", "midir", "ürün", "ürünü", "ürünün"};

        String normalized = question.toLowerCase()
                .replaceAll("[^a-zçğıöşü0-9\\s]", "")
                .trim();

        String[] words = normalized.split("\\s+");
        StringBuilder title = new StringBuilder();

        for (String word : words) {
            boolean isStopWord = false;
            for (String stop : stopWords) {
                if (word.equals(stop)) {
                    isStopWord = true;
                    break;
                }
            }

            if (!isStopWord && word.length() > 2) {
                if (title.length() > 0) title.append(" ");
                title.append(word);

                if (title.length() > 50) break;
            }
        }

        return title.length() > 0 ? title.toString() : question.substring(0, Math.min(50, question.length()));
    }

    /**
     * Generate suggested content from cluster
     */
    private String generateSuggestedContent(List<TrendyolQuestion> cluster) {
        // Check if any questions have answers
        for (TrendyolQuestion q : cluster) {
            if (q.getAnswers() != null && !q.getAnswers().isEmpty()) {
                TrendyolAnswer firstAnswer = q.getAnswers().get(0);
                if (firstAnswer.getAnswerText() != null && !firstAnswer.getAnswerText().isEmpty()) {
                    return firstAnswer.getAnswerText();
                }
            }
        }

        // Return placeholder for manual entry
        return "Bu soru için önerilen cevap henüz belirlenmemiştir. " +
               "Lütfen müşterilere verilecek standart cevabı buraya girin.";
    }

    /**
     * Generate pattern hash using SHA-256
     */
    private String generatePatternHash(String question) {
        String normalized = normalizeQuestion(question);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString().substring(0, 32);
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple hash
            return String.valueOf(normalized.hashCode());
        }
    }

    /**
     * Normalize question for comparison
     */
    private String normalizeQuestion(String question) {
        if (question == null) return "";

        return question.toLowerCase()
                .replaceAll("[^a-zçğıöşü0-9\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Calculate Jaccard similarity between two strings
     */
    private double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null || s1.isEmpty() || s2.isEmpty()) {
            return 0.0;
        }

        Set<String> words1 = new HashSet<>(Arrays.asList(s1.split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(s2.split("\\s+")));

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        if (union.isEmpty()) return 0.0;

        return (double) intersection.size() / union.size();
    }

    /**
     * Get suggestions for a store
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
     * Get pending suggestion count
     */
    public long getPendingSuggestionCount(UUID storeId) {
        return suggestionRepository.countByStoreIdAndStatus(storeId, KnowledgeSuggestion.STATUS_PENDING);
    }
}
