package com.ecommerce.sellerx.trendyol;

import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.TrendyolCredentials;
import com.ecommerce.sellerx.stores.MarketplaceCredentials;
import lombok.AllArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Service
@AllArgsConstructor
public class TrendyolService {
    
    private static final String TRENDYOL_BASE_URL = "https://apigw.trendyol.com";
    private final RestTemplate restTemplate;

    public TrendyolConnectionResult testConnection(Store store) {
        try {
            // Store'dan Trendyol credentials'ları çıkar
            TrendyolCredentials credentials = extractTrendyolCredentials(store);
            
            if (credentials == null) {
                return new TrendyolConnectionResult(
                    false, 
                    "Trendyol credentials not found in store", 
                    null, 
                    400
                );
            }

            // Basic Auth header oluştur
            String auth = credentials.getApiKey() + ":" + credentials.getApiSecret();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            
            // Headers ayarla
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + encodedAuth);
            headers.set("User-Agent", credentials.getSellerId() + " - SelfIntegration");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // Trendyol'a test isteği gönder (seller adresleri endpoint'i - basit bir GET isteği)
            String testUrl = TRENDYOL_BASE_URL + "/integration/sellers/" + credentials.getSellerId() + "/addresses";
            
            ResponseEntity<String> response = restTemplate.exchange(
                testUrl,
                HttpMethod.GET,
                entity,
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                //log the data
                // Başarılı bağlantı durumu
                System.out.println("Trendyol connection successful: " + response.getBody());
                return new TrendyolConnectionResult(
                    true,
                    "Connection successful",
                    credentials.getSellerId().toString(),
                    200
                );
            } else {
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
            
            // HTTP hatalarını yakalayıp detayını ver
            if (errorMessage.contains("401")) {
                statusCode = 401;
                errorMessage = "Invalid API credentials";
            } else if (errorMessage.contains("403")) {
                statusCode = 403;
                errorMessage = "Access forbidden - check seller ID and permissions";
            } else if (errorMessage.contains("404")) {
                statusCode = 404;
                errorMessage = "Seller not found or endpoint not available";
            } else if (errorMessage.contains("timeout") || errorMessage.contains("connect")) {
                statusCode = 408;
                errorMessage = "Connection timeout - Trendyol API not reachable";
            }
            
            return new TrendyolConnectionResult(
                false,
                errorMessage,
                null,
                statusCode
            );
        }
    }
    
    private TrendyolCredentials extractTrendyolCredentials(Store store) {
        try {
            // Store'daki credentials zaten MarketplaceCredentials tipinde
            // Jackson tarafından otomatik olarak deserialize edilmiş
            MarketplaceCredentials credentials = store.getCredentials();
            
            if (credentials instanceof TrendyolCredentials) {
                return (TrendyolCredentials) credentials;
            }
            
            return null;
                
        } catch (Exception e) {
            return null;
        }
    }
}
