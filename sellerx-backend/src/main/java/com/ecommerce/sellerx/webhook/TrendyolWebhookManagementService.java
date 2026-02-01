package com.ecommerce.sellerx.webhook;

import com.ecommerce.sellerx.stores.TrendyolCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendyolWebhookManagementService {
    
    private static final String TRENDYOL_BASE_URL = "https://apigw.trendyol.com";
    private static final List<String> ALL_STATUSES = Arrays.asList(
            "CREATED", "PICKING", "INVOICED", "SHIPPED", "CANCELLED", 
            "DELIVERED", "UNDELIVERED", "RETURNED", "UNSUPPLIED", 
            "AWAITING", "UNPACKED", "AT_COLLECTION_POINT", "VERIFIED"
    );
    
    private final RestTemplate restTemplate;
    
    @Value("${app.webhook.base-url:http://localhost:8080}")
    private String webhookBaseUrl;
    
    @Value("${app.webhook.api-key:sellerx-webhook-key}")
    private String webhookApiKey;
    
    @Value("${app.webhook.enabled:false}")
    private boolean webhookEnabled;
    
    /**
     * Create webhook for a store when it's created.
     * Returns a result object with webhookId, success status, and error message.
     *
     * @param credentials Trendyol credentials
     * @return WebhookResult containing webhookId, success flag, and error message
     */
    public WebhookResult createWebhookForStore(TrendyolCredentials credentials) {
        if (!webhookEnabled) {
            log.info("Webhook is disabled - skipping webhook creation for seller: {}", credentials.getSellerId());
            return WebhookResult.disabled();
        }

        try {
            log.info("Creating webhook for seller: {}", credentials.getSellerId());

            // Build webhook URL for this seller
            String webhookUrl = webhookBaseUrl + "/api/webhook/trendyol/" + credentials.getSellerId();

            // Create webhook request
            WebhookCreateRequest request = WebhookCreateRequest.builder()
                    .url(webhookUrl)
                    .authenticationType("API_KEY")
                    .apiKey(webhookApiKey)
                    .subscribedStatuses(ALL_STATUSES) // Subscribe to all statuses
                    .build();

            // Send request to Trendyol
            String webhookId = sendCreateWebhookRequest(credentials, request);

            if (webhookId != null) {
                log.info("Successfully created webhook with ID: {} for seller: {}", webhookId, credentials.getSellerId());
                return WebhookResult.success(webhookId);
            } else {
                String errorMsg = "Failed to create webhook - no ID returned from Trendyol";
                log.error("{} for seller: {}", errorMsg, credentials.getSellerId());
                return WebhookResult.failure(errorMsg);
            }

        } catch (Exception e) {
            String errorMsg = "Error creating webhook: " + e.getMessage();
            log.error("Error creating webhook for seller {}: {}", credentials.getSellerId(), e.getMessage(), e);
            return WebhookResult.failure(errorMsg);
        }
    }
    
    /**
     * Delete webhook for a store when it's deleted.
     * Returns a result object with success status and error message.
     *
     * @param credentials Trendyol credentials
     * @param webhookId The webhook ID to delete
     * @return WebhookResult with success status and optional error message
     */
    public WebhookResult deleteWebhookForStore(TrendyolCredentials credentials, String webhookId) {
        if (!webhookEnabled) {
            log.info("Webhook is disabled - skipping webhook deletion for seller: {}", credentials.getSellerId());
            return WebhookResult.disabled();
        }

        try {
            log.info("Deleting webhook {} for seller: {}", webhookId, credentials.getSellerId());

            boolean deleted = sendDeleteWebhookRequest(credentials, webhookId);
            if (deleted) {
                return WebhookResult.success(null);
            } else {
                return WebhookResult.failure("Failed to delete webhook from Trendyol");
            }

        } catch (Exception e) {
            String errorMsg = "Error deleting webhook: " + e.getMessage();
            log.error("Error deleting webhook {} for seller {}: {}", webhookId, credentials.getSellerId(), e.getMessage(), e);
            return WebhookResult.failure(errorMsg);
        }
    }
    
    private String sendCreateWebhookRequest(TrendyolCredentials credentials, WebhookCreateRequest request) {
        String auth = credentials.getApiKey() + ":" + credentials.getApiSecret();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.set("User-Agent", credentials.getSellerId() + " - SelfIntegration");
        
        HttpEntity<WebhookCreateRequest> entity = new HttpEntity<>(request, headers);
        
        String url = String.format("%s/integration/webhook/sellers/%s/webhooks", 
                TRENDYOL_BASE_URL, credentials.getSellerId());
        
        try {
            ResponseEntity<WebhookCreateResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, WebhookCreateResponse.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getId();
            }
            
        } catch (Exception e) {
            log.error("Error calling Trendyol webhook create API: {}", e.getMessage());
        }
        
        return null;
    }
    
    private boolean sendDeleteWebhookRequest(TrendyolCredentials credentials, String webhookId) {
        String auth = credentials.getApiKey() + ":" + credentials.getApiSecret();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.set("User-Agent", credentials.getSellerId() + " - SelfIntegration");
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        String url = String.format("%s/integration/webhook/sellers/%s/webhooks/%s", 
                TRENDYOL_BASE_URL, credentials.getSellerId(), webhookId);
        
        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    url, HttpMethod.DELETE, entity, Void.class);
            
            return response.getStatusCode().is2xxSuccessful();
            
        } catch (Exception e) {
            log.error("Error calling Trendyol webhook delete API: {}", e.getMessage());
            return false;
        }
    }
    
    // Inner classes for request/response
    @lombok.Data
    @lombok.Builder
    public static class WebhookCreateRequest {
        private String url;
        private String username;
        private String password;
        private String authenticationType;
        private String apiKey;
        private List<String> subscribedStatuses;
    }

    @lombok.Data
    public static class WebhookCreateResponse {
        private String id;
    }

    /**
     * Result object for webhook operations.
     * Contains webhookId, success status, and optional error message.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    public static class WebhookResult {
        private String webhookId;
        private boolean success;
        private boolean disabled; // true if webhook is disabled globally
        private String errorMessage;

        public static WebhookResult success(String webhookId) {
            return WebhookResult.builder()
                    .webhookId(webhookId)
                    .success(true)
                    .disabled(false)
                    .build();
        }

        public static WebhookResult failure(String errorMessage) {
            return WebhookResult.builder()
                    .success(false)
                    .disabled(false)
                    .errorMessage(errorMessage)
                    .build();
        }

        public static WebhookResult disabled() {
            return WebhookResult.builder()
                    .success(true) // Consider disabled as "success" since it's intentional
                    .disabled(true)
                    .build();
        }
    }
}
