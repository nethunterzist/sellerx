package com.ecommerce.sellerx.trendyol;

import com.ecommerce.sellerx.auth.JwtService;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.users.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/trendyol")
public class TrendyolController {
    private final JwtService jwtService;
    private final UserService userService;
    private final StoreRepository storeRepository;
    private final TrendyolService trendyolService;

    @PostMapping("/test-credentials")
    public ResponseEntity<?> testCredentials(@RequestBody TestCredentialsRequest body, HttpServletRequest request) {
        try {
            // Verify JWT token is valid
            Long userId = jwtService.getUserIdFromToken(request);

            if (body.getSellerId() == null || body.getApiKey() == null || body.getApiSecret() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                            "connected", false,
                            "error", "Missing required credentials (sellerId, apiKey, apiSecret)"
                        ));
            }

            // Test Trendyol credentials
            TrendyolConnectionResult result = trendyolService.testCredentials(
                body.getSellerId(),
                body.getApiKey(),
                body.getApiSecret()
            );

            if (result.isConnected()) {
                return ResponseEntity.ok(Map.of(
                    "connected", true,
                    "message", result.getMessage(),
                    "sellerId", result.getSellerId(),
                    "statusCode", result.getStatusCode()
                ));
            } else {
                return ResponseEntity.status(result.getStatusCode())
                        .body(Map.of(
                            "connected", false,
                            "error", result.getMessage(),
                            "statusCode", result.getStatusCode()
                        ));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                        "connected", false,
                        "error", "Invalid token or authentication failed"
                    ));
        }
    }

    @GetMapping("/test-connection")
    public ResponseEntity<?> testTrendyolConnection(HttpServletRequest request) {
        try {
            Long userId = jwtService.getUserIdFromToken(request);
            UUID selectedStoreId = userService.getSelectedStoreId(userId);
            
            if (selectedStoreId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                            "connected", false,
                            "error", "No store selected"
                        ));
            }
            
            // Store bilgilerini getir
            Store store = storeRepository.findById(selectedStoreId)
                    .orElseThrow(() -> new RuntimeException("Store not found"));
            
            // Store'un bu user'a ait olduğunu kontrol et
            if (!store.getUser().getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                            "connected", false,
                            "error", "Access denied to this store"
                        ));
            }
            
            // Trendyol bağlantısını test et
            TrendyolConnectionResult result = trendyolService.testConnection(store);
            
            return ResponseEntity.ok(Map.of(
                "connected", result.isConnected(),
                "storeId", selectedStoreId.toString(),
                "storeName", store.getStoreName(),
                "marketplace", store.getMarketplace(),
                "message", result.getMessage(),
                "sellerId", result.getSellerId(),
                "statusCode", result.getStatusCode()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                        "connected", false,
                        "error", "Invalid token or authentication failed"
                    ));
        }
    }
}
