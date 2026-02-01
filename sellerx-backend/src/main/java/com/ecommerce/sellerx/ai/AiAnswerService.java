package com.ecommerce.sellerx.ai;

import com.ecommerce.sellerx.ai.dto.AiGenerateResponse;
import com.ecommerce.sellerx.qa.TrendyolAnswer;
import com.ecommerce.sellerx.qa.TrendyolAnswerRepository;
import com.ecommerce.sellerx.qa.TrendyolQaService;
import com.ecommerce.sellerx.qa.TrendyolQuestion;
import com.ecommerce.sellerx.qa.TrendyolQuestionRepository;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.users.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnswerService {

    private final OpenAiService openAiService;
    private final AiContextBuilder contextBuilder;
    private final StoreAiSettingsRepository settingsRepository;
    private final AiAnswerLogRepository logRepository;
    private final TrendyolQuestionRepository questionRepository;
    private final TrendyolAnswerRepository answerRepository;
    private final TrendyolQaService trendyolQaService;

    @Transactional
    public AiGenerateResponse generateAnswer(UUID questionId) {
        TrendyolQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Soru bulunamadı: " + questionId));

        Store store = question.getStore();
        UUID storeId = store.getId();

        // Get AI settings for the store
        StoreAiSettings settings = settingsRepository.findByStoreId(storeId)
                .orElseGet(() -> createDefaultSettings(store));

        if (!settings.getAiEnabled()) {
            throw new RuntimeException("Bu mağaza için AI cevaplama aktif değil");
        }

        // Build context
        AiContextBuilder.ContextResult contextResult = contextBuilder.buildContext(question, settings);

        // Generate answer using OpenAI
        AiGenerateResponse response = openAiService.generateAnswer(
                contextResult.systemPrompt(),
                question.getCustomerQuestion()
        );

        // Add context sources to response
        response.setContextSources(contextResult.sources());

        // Save to log
        AiAnswerLog aiLog = AiAnswerLog.builder()
                .question(question)
                .generatedAnswer(response.getGeneratedAnswer())
                .confidenceScore(response.getConfidenceScore())
                .contextUsed(buildContextUsedMap(contextResult.sources()))
                .modelVersion(response.getModelVersion())
                .tokensUsed(response.getTokensUsed())
                .generationTimeMs(response.getGenerationTimeMs())
                .build();

        aiLog = logRepository.save(aiLog);
        response.setLogId(aiLog.getId());

        log.info("AI answer generated for question {} with confidence {}",
                questionId, response.getConfidenceScore());

        return response;
    }

    @Transactional
    public TrendyolAnswer approveAndSubmit(UUID questionId, UUID logId, String finalAnswer, User user) {
        TrendyolQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Soru bulunamadı: " + questionId));

        // Get the AI log
        AiAnswerLog aiLog = logRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("AI log bulunamadı: " + logId));

        // Update AI log
        boolean wasEdited = !aiLog.getGeneratedAnswer().equals(finalAnswer);
        aiLog.setWasApproved(true);
        aiLog.setWasEdited(wasEdited);
        aiLog.setFinalAnswer(finalAnswer);
        logRepository.save(aiLog);

        // Create answer record
        TrendyolAnswer answer = TrendyolAnswer.builder()
                .question(question)
                .answerText(finalAnswer)
                .isSubmitted(false)
                .submittedBy(user)
                .build();

        answer = answerRepository.save(answer);

        // Submit to Trendyol
        try {
            trendyolQaService.submitAnswerToTrendyol(question, finalAnswer);
            answer.setIsSubmitted(true);
            answer.setSubmittedAt(LocalDateTime.now());
            answer = answerRepository.save(answer);

            // Update question status
            question.setStatus("ANSWERED");
            questionRepository.save(question);

            log.info("AI answer approved and submitted for question {}", questionId);
        } catch (Exception e) {
            log.error("Failed to submit answer to Trendyol for question {}: {}",
                    questionId, e.getMessage());
            throw new RuntimeException("Trendyol'a cevap gönderilemedi: " + e.getMessage());
        }

        return answer;
    }

    private StoreAiSettings createDefaultSettings(Store store) {
        StoreAiSettings settings = StoreAiSettings.builder()
                .store(store)
                .aiEnabled(true)
                .autoAnswer(false)
                .tone("professional")
                .language("tr")
                .maxAnswerLength(500)
                .includeGreeting(true)
                .includeSignature(true)
                .build();
        return settingsRepository.save(settings);
    }

    private Map<String, Object> buildContextUsedMap(java.util.List<AiGenerateResponse.ContextSource> sources) {
        Map<String, Object> contextMap = new HashMap<>();
        contextMap.put("sources", sources.stream()
                .map(s -> Map.of("type", s.getType(), "title", s.getTitle()))
                .toList());
        return contextMap;
    }
}
