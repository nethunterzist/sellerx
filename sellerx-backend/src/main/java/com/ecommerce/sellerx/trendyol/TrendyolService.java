package com.ecommerce.sellerx.trendyol;

import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.TrendyolCredentials;
import com.ecommerce.sellerx.stores.MarketplaceCredentials;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Service
@Slf4j
public class TrendyolService {

    private static final String TRENDYOL_BASE_URL = "https://apigw.trendyol.com";
    private final RestTemplate restTemplate;
    private final MeterRegistry meterRegistry;

    public TrendyolService(RestTemplate restTemplate, MeterRegistry meterRegistry) {
        this.restTemplate = restTemplate;
        this.meterRegistry = meterRegistry;
    }

    public TrendyolConnectionResult testConnection(Store store) {
        incrementApiCallCounter("testConnection");
        try {
            // Store'dan Trendyol credentials'lari cikar
            TrendyolCredentials credentials = extractTrendyolCredentials(store);

            if (credentials == null) {
                incrementApiErrorCounter("credentials_missing");
                return new TrendyolConnectionResult(
                    false,
                    "Trendyol credentials not found in store",
                    null,
                    400
                );
            }

            // Basic Auth header olustur
            String auth = credentials.getApiKey() + ":" + credentials.getApiSecret();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

            // Headers ayarla
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + encodedAuth);
            headers.set("User-Agent", credentials.getSellerId() + " - SelfIntegration");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Trendyol'a test istegi gonder (seller adresleri endpoint'i - basit bir GET istegi)
            String testUrl = TRENDYOL_BASE_URL + "/integration/sellers/" + credentials.getSellerId() + "/addresses";

            ResponseEntity<String> response = restTemplate.exchange(
                testUrl,
                HttpMethod.GET,
                entity,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Trendyol connection successful for seller: {}", credentials.getSellerId());
                return new TrendyolConnectionResult(
                    true,
                    "Connection successful",
                    credentials.getSellerId().toString(),
                    200
                );
            } else {
                incrementApiErrorCounter("unexpected_status");
                return new TrendyolConnectionResult(
                    false,
                    "Unexpected response: " + response.getStatusCode(),
                    credentials.getSellerId().toString(),
                    response.getStatusCode().value()
                );
            }

        } catch (Exception e) {
            String errorMessage = e.getMessage();
            int statusCode = 500;
            String errorType = "unknown";

            // HTTP hatalarini yakalayip detayini ver
            if (errorMessage != null && errorMessage.contains("401")) {
                statusCode = 401;
                errorMessage = "Invalid API credentials";
                errorType = "auth_failure";
            } else if (errorMessage != null && errorMessage.contains("403")) {
                statusCode = 403;
                errorMessage = "Access forbidden - check seller ID and permissions";
                errorType = "forbidden";
            } else if (errorMessage != null && errorMessage.contains("404")) {
                statusCode = 404;
                errorMessage = "Seller not found or endpoint not available";
                errorType = "not_found";
            } else if (errorMessage != null && (errorMessage.contains("timeout") || errorMessage.contains("connect"))) {
                statusCode = 408;
                errorMessage = "Connection timeout - Trendyol API not reachable";
                errorType = "timeout";
            }

            incrementApiErrorCounter(errorType);
            return new TrendyolConnectionResult(
                false,
                errorMessage,
                null,
                statusCode
            );
        }
    }

    /**
     * Test Trendyol credentials without a store (for new store creation)
     */
    public TrendyolConnectionResult testCredentials(String sellerId, String apiKey, String apiSecret) {
        incrementApiCallCounter("testCredentials");
        try {
            // Basic Auth header olustur
            String auth = apiKey + ":" + apiSecret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

            // Headers ayarla
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + encodedAuth);
            headers.set("User-Agent", sellerId + " - SelfIntegration");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Trendyol'a test istegi gonder
            String testUrl = TRENDYOL_BASE_URL + "/integration/sellers/" + sellerId + "/addresses";

            ResponseEntity<String> response = restTemplate.exchange(
                testUrl,
                HttpMethod.GET,
                entity,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Trendyol credentials test successful for seller: {}", sellerId);
                return new TrendyolConnectionResult(
                    true,
                    "Connection successful",
                    sellerId,
                    200
                );
            } else {
                incrementApiErrorCounter("unexpected_status");
                return new TrendyolConnectionResult(
                    false,
                    "Unexpected response: " + response.getStatusCode(),
                    sellerId,
                    response.getStatusCode().value()
                );
            }

        } catch (Exception e) {
            String errorMessage = e.getMessage();
            int statusCode = 500;
            String errorType = "unknown";

            if (errorMessage != null && errorMessage.contains("401")) {
                statusCode = 401;
                errorMessage = "Invalid API credentials";
                errorType = "auth_failure";
            } else if (errorMessage != null && errorMessage.contains("403")) {
                statusCode = 403;
                errorMessage = "Access forbidden - check seller ID and permissions";
                errorType = "forbidden";
            } else if (errorMessage != null && errorMessage.contains("404")) {
                statusCode = 404;
                errorMessage = "Seller not found or endpoint not available";
                errorType = "not_found";
            } else if (errorMessage != null && (errorMessage.contains("timeout") || errorMessage.contains("connect"))) {
                statusCode = 408;
                errorMessage = "Connection timeout - Trendyol API not reachable";
                errorType = "timeout";
            }

            incrementApiErrorCounter(errorType);
            return new TrendyolConnectionResult(
                false,
                errorMessage,
                sellerId,
                statusCode
            );
        }
    }

    private TrendyolCredentials extractTrendyolCredentials(Store store) {
        try {
            // Store'daki credentials zaten MarketplaceCredentials tipinde
            // Jackson tarafindan otomatik olarak deserialize edilmis
            MarketplaceCredentials credentials = store.getCredentials();

            if (credentials instanceof TrendyolCredentials) {
                return (TrendyolCredentials) credentials;
            }

            return null;

        } catch (Exception e) {
            return null;
        }
    }

    private void incrementApiCallCounter(String endpoint) {
        Counter.builder("sellerx.trendyol.api.calls")
                .tag("endpoint", endpoint)
                .description("Number of Trendyol API calls")
                .register(meterRegistry)
                .increment();
    }

    private void incrementApiErrorCounter(String errorType) {
        Counter.builder("sellerx.trendyol.api.errors")
                .tag("type", errorType)
                .description("Number of Trendyol API errors")
                .register(meterRegistry)
                .increment();
    }
}
