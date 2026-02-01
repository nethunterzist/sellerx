package com.ecommerce.sellerx.ai;

import com.ecommerce.sellerx.ai.dto.AiGenerateResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
public class OpenAiService {

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.model:gpt-4-turbo}")
    private String model;

    @Value("${openai.max-tokens:1000}")
    private int maxTokens;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    public OpenAiService(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    public AiGenerateResponse generateAnswer(String systemPrompt, String userQuestion) {
        long startTime = System.currentTimeMillis();

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("OpenAI API key not configured, returning mock response");
            return createMockResponse(userQuestion, startTime);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", 0.7);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userQuestion));
            requestBody.put("messages", messages);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    OPENAI_API_URL,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            long endTime = System.currentTimeMillis();
            int generationTime = (int) (endTime - startTime);

            JsonNode root = objectMapper.readTree(response.getBody());
            String generatedText = root.path("choices").get(0).path("message").path("content").asText();
            int tokensUsed = root.path("usage").path("total_tokens").asInt();

            // Calculate confidence based on response quality
            BigDecimal confidence = calculateConfidence(generatedText);

            return AiGenerateResponse.builder()
                    .generatedAnswer(generatedText.trim())
                    .confidenceScore(confidence)
                    .modelVersion(model)
                    .tokensUsed(tokensUsed)
                    .generationTimeMs(generationTime)
                    .contextSources(new ArrayList<>())
                    .build();

        } catch (Exception e) {
            log.error("OpenAI API call failed: {}", e.getMessage(), e);
            return createMockResponse(userQuestion, startTime);
        }
    }

    private BigDecimal calculateConfidence(String response) {
        // Simple heuristic for confidence:
        // - Longer, more detailed responses = higher confidence
        // - Contains "emin değilim" or "bilmiyorum" = lower confidence
        int length = response.length();
        double baseScore = 0.75;

        if (length > 200) baseScore += 0.10;
        if (length > 400) baseScore += 0.05;

        if (response.toLowerCase().contains("emin değilim") ||
            response.toLowerCase().contains("bilmiyorum") ||
            response.toLowerCase().contains("kesin bilgi veremiyorum")) {
            baseScore -= 0.15;
        }

        return BigDecimal.valueOf(Math.min(0.99, Math.max(0.50, baseScore)));
    }

    private AiGenerateResponse createMockResponse(String question, long startTime) {
        // Mock response for development/testing when API key is not set
        String mockAnswer = """
            Merhaba,

            Sorunuz için teşekkür ederiz. Bu bir test yanıtıdır.

            Gerçek AI yanıtları için lütfen OpenAI API anahtarınızı yapılandırın.

            İyi günler dileriz.
            """;

        return AiGenerateResponse.builder()
                .generatedAnswer(mockAnswer)
                .confidenceScore(new BigDecimal("0.85"))
                .modelVersion("mock")
                .tokensUsed(0)
                .generationTimeMs((int) (System.currentTimeMillis() - startTime))
                .contextSources(new ArrayList<>())
                .build();
    }
}
