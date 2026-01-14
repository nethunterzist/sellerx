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
     * Create webhook for a store when it's created
     */
    public String createWebhookForStore(TrendyolCredentials credentials) {
        if (!webhookEnabled) {
            log.info("Webhook is disabled - skipping webhook creation for seller: {}", credentials.getSellerId());
            return null;
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
                return webhookId;
            } else {
                log.error("Failed to create webhook for seller: {}", credentials.getSellerId());
                return null;
            }
            
        } catch (Exception e) {
            log.error("Error creating webhook for seller {}: {}", credentials.getSellerId(), e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Delete webhook for a store when it's deleted
     */
    public boolean deleteWebhookForStore(TrendyolCredentials credentials, String webhookId) {
        if (!webhookEnabled) {
            log.info("Webhook is disabled - skipping webhook deletion for seller: {}", credentials.getSellerId());
            return true;
        }
        
        try {
            log.info("Deleting webhook {} for seller: {}", webhookId, credentials.getSellerId());
            
            return sendDeleteWebhookRequest(credentials, webhookId);
            
        } catch (Exception e) {
            log.error("Error deleting webhook {} for seller {}: {}", webhookId, credentials.getSellerId(), e.getMessage(), e);
            return false;
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
}
