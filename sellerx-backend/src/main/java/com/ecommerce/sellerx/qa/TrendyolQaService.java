package com.ecommerce.sellerx.qa;

import com.ecommerce.sellerx.qa.dto.*;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.stores.TrendyolCredentials;
import com.ecommerce.sellerx.stores.MarketplaceCredentials;
import com.ecommerce.sellerx.users.User;
import com.ecommerce.sellerx.users.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendyolQaService {

    // Q&A API - same base URL as other Trendyol APIs
    // api.trendyol.com was deprecated/blocked by Cloudflare
    private static final String TRENDYOL_QA_BASE_URL = "https://apigw.trendyol.com";
    private static final ZoneId TURKEY_ZONE = ZoneId.of("Europe/Istanbul");

    private final TrendyolQuestionRepository questionRepository;
    private final TrendyolAnswerRepository answerRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Sync questions from Trendyol API
     * Uses the correct endpoint: /integration/qna/sellers/{sellerId}/questions/filter
     * API requires startDate and endDate parameters (max 2 weeks range)
     */
    @Transactional
    public QaSyncResponse syncQuestions(UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));
        TrendyolCredentials credentials = extractTrendyolCredentials(store);

        if (credentials == null) {
            return QaSyncResponse.builder()
                    .totalFetched(0)
                    .newQuestions(0)
                    .updatedQuestions(0)
                    .message("Trendyol credentials not found")
                    .build();
        }

        int totalFetched = 0;
        int newQuestions = 0;
        int updatedQuestions = 0;
        int page = 0;
        int size = 50; // Max allowed by Trendyol API
        boolean hasMore = true;

        log.info("[QA] Son 90 günlük müşteri soruları çekiliyor...");

        // Trendyol Q&A API date range - using 90 days for comprehensive coverage
        long endDate = System.currentTimeMillis();
        long startDate = endDate - (90L * 24 * 60 * 60 * 1000); // 90 days ago

        try {
            while (hasMore) {
                // CORRECT ENDPOINT: /integration/qna/sellers/{sellerId}/questions/filter
                // OLD (WRONG): /sapigw/integration/suppliers/{sellerId}/questions
                String url = String.format(
                        "%s/integration/qna/sellers/%s/questions/filter?supplierId=%s&page=%d&size=%d&startDate=%d&endDate=%d",
                        TRENDYOL_QA_BASE_URL, credentials.getSellerId(), credentials.getSellerId(), page, size, startDate, endDate
                );
                log.info("Requesting Q&A from URL: {}", url);

                HttpEntity<String> entity = createHttpEntity(credentials);
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode content = root.path("content");

                    if (content.isArray() && content.size() > 0) {
                        for (JsonNode questionNode : content) {
                            String questionId = questionNode.path("id").asText();

                            Optional<TrendyolQuestion> existingOpt = questionRepository.findByStoreIdAndQuestionId(storeId, questionId);

                            if (existingOpt.isPresent()) {
                                // Update existing question
                                TrendyolQuestion existing = existingOpt.get();
                                updateQuestionFromJson(existing, questionNode);
                                questionRepository.save(existing);
                                // Also update/create answer if present
                                saveAnswerFromJson(existing, questionNode);
                                updatedQuestions++;
                            } else {
                                // Create new question
                                TrendyolQuestion newQuestion = createQuestionFromJson(store, questionNode);
                                questionRepository.save(newQuestion);
                                // Also save answer if present
                                saveAnswerFromJson(newQuestion, questionNode);
                                newQuestions++;
                            }
                            totalFetched++;
                        }

                        // Check if there are more pages
                        int totalPages = root.path("totalPages").asInt(1);
                        page++;
                        hasMore = page < totalPages;
                    } else {
                        hasMore = false;
                    }
                } else {
                    hasMore = false;
                }
            }

            log.info("[QA] [COMPLETED] {} soru çekildi. Yeni: {}, Güncellenen: {}",
                    totalFetched, newQuestions, updatedQuestions);
            log.info("Q&A sync completed for store {}: {} fetched, {} new, {} updated",
                    storeId, totalFetched, newQuestions, updatedQuestions);

            return QaSyncResponse.builder()
                    .totalFetched(totalFetched)
                    .newQuestions(newQuestions)
                    .updatedQuestions(updatedQuestions)
                    .message("Sync completed successfully")
                    .build();

        } catch (Exception e) {
            log.error("Error syncing questions for store {}: {}", storeId, e.getMessage());
            return QaSyncResponse.builder()
                    .totalFetched(totalFetched)
                    .newQuestions(newQuestions)
                    .updatedQuestions(updatedQuestions)
                    .message("Sync failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Save AI-generated answer locally (draft mode)
     * NOT submitting to Trendyol - AI will handle automatic submission when ready
     *
     * IMPORTANT: This is a DRAFT save only. Actual Trendyol submission will be
     * handled by AI auto-answer feature in future phase.
     */
    @Transactional
    public AnswerDto saveAnswerDraft(UUID questionId, String answerText, Long userId) {
        TrendyolQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;

        // Create answer entity - NOT submitted to Trendyol
        TrendyolAnswer answer = TrendyolAnswer.builder()
                .question(question)
                .answerText(answerText)
                .submittedBy(user)
                .isSubmitted(false) // Always false - AI will submit when ready
                .build();

        answer = answerRepository.save(answer);

        log.info("Answer draft saved for question {} (NOT submitted to Trendyol)", questionId);

        return toAnswerDto(answer);
    }

    /**
     * Submit answer to Trendyol API
     * WARNING: Only call this when AI auto-answer feature is ready
     * DO NOT use for test accounts!
     */
    @Transactional
    public AnswerDto submitAnswerToTrendyol(UUID answerId) {
        TrendyolAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("Answer not found"));

        if (answer.getIsSubmitted()) {
            log.warn("Answer {} already submitted", answerId);
            return toAnswerDto(answer);
        }

        TrendyolQuestion question = answer.getQuestion();
        Store store = question.getStore();
        TrendyolCredentials credentials = extractTrendyolCredentials(store);

        if (credentials == null) {
            throw new RuntimeException("Trendyol credentials not found");
        }

        // SAFETY CHECK: Log but don't actually submit for now
        // This will be enabled when AI auto-answer feature is ready
        log.warn("SAFETY: submitAnswerToTrendyol called but submission is DISABLED. " +
                "Answer ID: {}, Question ID: {}, Seller ID: {}",
                answerId, question.getQuestionId(), credentials.getSellerId());

        // TODO: Enable actual submission when AI feature is ready
        // For now, just mark as "pending submission"
        /*
        try {
            String url = String.format(
                    "%s/sapigw/integration/suppliers/%s/questions/%s/answers",
                    TRENDYOL_QA_BASE_URL, credentials.getSellerId(), question.getQuestionId()
            );

            Map<String, String> body = new HashMap<>();
            body.put("text", answer.getAnswerText());

            HttpEntity<Map<String, String>> entity = createHttpEntityWithBody(credentials, body);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                answer.setIsSubmitted(true);
                answer.setSubmittedAt(LocalDateTime.now());

                if (response.getBody() != null) {
                    try {
                        JsonNode responseNode = objectMapper.readTree(response.getBody());
                        if (responseNode.has("id")) {
                            answer.setTrendyolAnswerId(responseNode.path("id").asText());
                        }
                    } catch (Exception ignored) {}
                }

                question.setStatus("ANSWERED");
                questionRepository.save(question);
                answerRepository.save(answer);

                log.info("Answer submitted to Trendyol for question {}", question.getQuestionId());
            }
        } catch (Exception e) {
            log.error("Error submitting answer to Trendyol: {}", e.getMessage());
        }
        */

        return toAnswerDto(answer);
    }

    /**
     * Submit answer to Trendyol API with question and answer text
     * Used by AI auto-answer feature
     */
    @Transactional
    public void submitAnswerToTrendyol(TrendyolQuestion question, String answerText) {
        Store store = question.getStore();
        TrendyolCredentials credentials = extractTrendyolCredentials(store);

        if (credentials == null) {
            throw new RuntimeException("Trendyol credentials not found");
        }

        // SAFETY CHECK: Log but don't actually submit for now
        log.warn("SAFETY: submitAnswerToTrendyol called but submission is DISABLED. " +
                "Question ID: {}, Seller ID: {}",
                question.getQuestionId(), credentials.getSellerId());

        // TODO: Enable actual submission when AI feature is ready
        // For now, just log the action
    }

    /**
     * Submit manual answer to Trendyol API
     * This is the user-facing manual answer submission endpoint
     *
     * SAFETY: In development mode, this will NOT actually submit to Trendyol
     * Set TRENDYOL_SUBMIT_ENABLED=true environment variable to enable real submission
     */
    @Transactional
    public void submitManualAnswer(UUID storeId, String questionId, String answerText, User user) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));
        TrendyolCredentials credentials = extractTrendyolCredentials(store);

        if (credentials == null) {
            throw new RuntimeException("Trendyol credentials not found");
        }

        // Find the question
        TrendyolQuestion question = questionRepository.findByStoreIdAndQuestionId(storeId, questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        // Check if already answered
        if ("ANSWERED".equals(question.getStatus())) {
            throw new RuntimeException("Question already answered");
        }

        // SAFETY CHECK: Only submit to Trendyol if explicitly enabled
        boolean submitEnabled = Boolean.parseBoolean(System.getenv("TRENDYOL_SUBMIT_ENABLED"));

        if (submitEnabled) {
            // POST to Trendyol API - NEW endpoint format
            String url = String.format(
                    "%s/integration/qna/sellers/%s/questions/%s/answers",
                    TRENDYOL_QA_BASE_URL, credentials.getSellerId(), questionId
            );

            Map<String, String> body = new HashMap<>();
            body.put("text", answerText);

            try {
                HttpEntity<Map<String, String>> entity = createHttpEntityWithBody(credentials, body);
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

                if (!response.getStatusCode().is2xxSuccessful()) {
                    log.error("Trendyol API error: {} - {}", response.getStatusCode(), response.getBody());
                    throw new RuntimeException("Trendyol API error: " + response.getStatusCode());
                }

                log.info("Answer submitted to Trendyol for question {} by user {}", questionId, user.getEmail());
            } catch (Exception e) {
                log.error("Error submitting answer to Trendyol: {}", e.getMessage());
                throw new RuntimeException("Failed to submit answer to Trendyol: " + e.getMessage());
            }
        } else {
            // DEVELOPMENT MODE: Just log, don't actually submit
            log.warn("DEVELOPMENT MODE: Answer NOT submitted to Trendyol (set TRENDYOL_SUBMIT_ENABLED=true to enable). " +
                    "Question ID: {}, Seller ID: {}, Answer: {}",
                    questionId, credentials.getSellerId(), answerText.substring(0, Math.min(50, answerText.length())));
        }

        // Save answer to local database
        TrendyolAnswer answer = TrendyolAnswer.builder()
                .question(question)
                .answerText(answerText)
                .submittedBy(user)
                .isSubmitted(submitEnabled) // Only mark as submitted if actually sent
                .submittedAt(submitEnabled ? LocalDateTime.now() : null)
                .build();
        answerRepository.save(answer);

        // Update question status
        question.setStatus("ANSWERED");
        questionRepository.save(question);

        log.info("Manual answer saved for question {} (submitted to Trendyol: {})", questionId, submitEnabled);
    }

    /**
     * Get questions for a store with pagination
     */
    public Page<QuestionDto> getQuestions(UUID storeId, String status, Pageable pageable) {
        Page<TrendyolQuestion> questions;

        if (status != null && !status.isEmpty()) {
            questions = questionRepository.findByStoreIdAndStatusOrderByQuestionDateDesc(storeId, status, pageable);
        } else {
            questions = questionRepository.findByStoreIdOrderByQuestionDateDesc(storeId, pageable);
        }

        return questions.map(this::toQuestionDto);
    }

    /**
     * Get single question by ID
     */
    public QuestionDto getQuestion(UUID questionId) {
        TrendyolQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        return toQuestionDto(question);
    }

    /**
     * Get Q&A statistics for a store
     */
    public QaStatsDto getStats(UUID storeId) {
        long total = questionRepository.countByStoreId(storeId);
        long pending = questionRepository.countByStoreIdAndStatus(storeId, "PENDING");
        long answered = questionRepository.countByStoreIdAndStatus(storeId, "ANSWERED");

        return QaStatsDto.builder()
                .totalQuestions(total)
                .pendingQuestions(pending)
                .answeredQuestions(answered)
                .build();
    }

    // Helper methods

    private TrendyolCredentials extractTrendyolCredentials(Store store) {
        MarketplaceCredentials credentials = store.getCredentials();
        if (credentials instanceof TrendyolCredentials) {
            return (TrendyolCredentials) credentials;
        }
        return null;
    }

    private HttpEntity<String> createHttpEntity(TrendyolCredentials credentials) {
        String auth = credentials.getApiKey() + ":" + credentials.getApiSecret();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.set("User-Agent", credentials.getSellerId() + " - SelfIntegration");

        return new HttpEntity<>(headers);
    }

    private <T> HttpEntity<T> createHttpEntityWithBody(TrendyolCredentials credentials, T body) {
        String auth = credentials.getApiKey() + ":" + credentials.getApiSecret();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.set("User-Agent", credentials.getSellerId() + " - SelfIntegration");

        return new HttpEntity<>(body, headers);
    }

    private TrendyolQuestion createQuestionFromJson(Store store, JsonNode node) {
        // Map Trendyol status to our internal status
        String trendyolStatus = node.path("status").asText("WAITING_FOR_ANSWER");
        String internalStatus = mapTrendyolStatus(trendyolStatus);

        return TrendyolQuestion.builder()
                .store(store)
                .questionId(node.path("id").asText())
                // New API uses "productMainId" instead of "productId"
                .productId(node.path("productMainId").asText(null))
                .barcode(node.path("barcode").asText(null))
                .productTitle(node.path("productName").asText(null))
                .customerQuestion(node.path("text").asText())
                .questionDate(parseTimestamp(node.path("creationDate").asLong()))
                .status(internalStatus)
                .isPublic(node.path("public").asBoolean(true))
                .build();
    }

    /**
     * Map Trendyol Q&A status to internal status
     * Trendyol statuses: WAITING_FOR_ANSWER, WAITING_FOR_APPROVE, ANSWERED, REPORTED, REJECTED
     * Internal statuses: PENDING, ANSWERED
     */
    private String mapTrendyolStatus(String trendyolStatus) {
        if (trendyolStatus == null) {
            return "PENDING";
        }
        return switch (trendyolStatus) {
            case "ANSWERED", "WAITING_FOR_APPROVE" -> "ANSWERED";
            case "REJECTED", "REPORTED" -> "REJECTED";
            default -> "PENDING"; // WAITING_FOR_ANSWER and others
        };
    }

    /**
     * Save answer from Trendyol API response if present
     * Trendyol response includes "answer" object with seller's response
     */
    private void saveAnswerFromJson(TrendyolQuestion question, JsonNode questionNode) {
        JsonNode answerNode = questionNode.path("answer");

        // Check if answer exists and has text
        if (answerNode.isMissingNode() || answerNode.isNull()) {
            return;
        }

        String answerText = answerNode.path("text").asText(null);
        if (answerText == null || answerText.isEmpty()) {
            return;
        }

        String trendyolAnswerId = answerNode.path("id").asText(null);

        // Check if we already have this answer
        if (trendyolAnswerId != null) {
            Optional<TrendyolAnswer> existingAnswer = answerRepository.findByTrendyolAnswerId(trendyolAnswerId);
            if (existingAnswer.isPresent()) {
                // Answer already exists, no need to create again
                return;
            }
        }

        // Also check if question already has an answer from Trendyol
        boolean hasExistingTrendyolAnswer = question.getAnswers().stream()
                .anyMatch(a -> a.getTrendyolAnswerId() != null);
        if (hasExistingTrendyolAnswer) {
            return;
        }

        // Create new answer from Trendyol data
        TrendyolAnswer answer = TrendyolAnswer.builder()
                .question(question)
                .answerText(answerText)
                .trendyolAnswerId(trendyolAnswerId)
                .isSubmitted(true) // Already submitted since it came from Trendyol
                .submittedAt(parseTimestamp(answerNode.path("creationDate").asLong()))
                .build();

        answerRepository.save(answer);
        log.debug("Saved answer from Trendyol for question {}", question.getQuestionId());
    }

    private void updateQuestionFromJson(TrendyolQuestion question, JsonNode node) {
        String trendyolStatus = node.path("status").asText(null);
        if (trendyolStatus != null) {
            question.setStatus(mapTrendyolStatus(trendyolStatus));
        }
        question.setIsPublic(node.path("public").asBoolean(question.getIsPublic()));
    }

    private LocalDateTime parseTimestamp(long timestamp) {
        if (timestamp <= 0) {
            return LocalDateTime.now();
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), TURKEY_ZONE);
    }

    private QuestionDto toQuestionDto(TrendyolQuestion question) {
        List<AnswerDto> answerDtos = question.getAnswers().stream()
                .map(this::toAnswerDto)
                .collect(Collectors.toList());

        return QuestionDto.builder()
                .id(question.getId())
                .questionId(question.getQuestionId())
                .productId(question.getProductId())
                .barcode(question.getBarcode())
                .productTitle(question.getProductTitle())
                .customerQuestion(question.getCustomerQuestion())
                .questionDate(question.getQuestionDate())
                .status(question.getStatus())
                .isPublic(question.getIsPublic())
                .answers(answerDtos)
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .build();
    }

    private AnswerDto toAnswerDto(TrendyolAnswer answer) {
        User submitter = answer.getSubmittedBy();
        return AnswerDto.builder()
                .id(answer.getId())
                .questionId(answer.getQuestion() != null ? answer.getQuestion().getId() : null)
                .answerText(answer.getAnswerText())
                .isSubmitted(answer.getIsSubmitted())
                .trendyolAnswerId(answer.getTrendyolAnswerId())
                .submittedAt(answer.getSubmittedAt())
                .submittedBy(submitter != null ? submitter.getId() : null)
                .submittedByEmail(submitter != null ? submitter.getEmail() : null)
                .createdAt(answer.getCreatedAt())
                .build();
    }
}
