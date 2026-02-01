package com.ecommerce.sellerx.buybox;

import com.ecommerce.sellerx.buybox.dto.*;
import com.ecommerce.sellerx.users.User;
import com.ecommerce.sellerx.users.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Buybox takip sistemi REST controller'ı.
 */
@RestController
@RequestMapping("/buybox")
@RequiredArgsConstructor
@Slf4j
public class BuyboxController {

    private final BuyboxService buyboxService;
    private final UserRepository userRepository;

    /**
     * Mağazanın buybox dashboard verilerini getirir.
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/stores/{storeId}/dashboard")
    public ResponseEntity<BuyboxDashboardDto> getDashboard(
            @PathVariable UUID storeId,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);
        log.info("Getting buybox dashboard for store: {}", storeId);
        BuyboxDashboardDto dashboard = buyboxService.getDashboard(storeId, user);
        return ResponseEntity.ok(dashboard);
    }

    /**
     * Mağazanın takip ettiği ürünleri listeler.
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/stores/{storeId}/products")
    public ResponseEntity<List<BuyboxTrackedProductDto>> getTrackedProducts(
            @PathVariable UUID storeId,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);
        log.info("Getting tracked products for store: {}", storeId);
        List<BuyboxTrackedProductDto> products = buyboxService.getTrackedProducts(storeId, user);
        return ResponseEntity.ok(products);
    }

    /**
     * Ürünü buybox takibine ekler.
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @PostMapping("/stores/{storeId}/products")
    public ResponseEntity<BuyboxTrackedProductDto> addProduct(
            @PathVariable UUID storeId,
            @Valid @RequestBody AddProductRequest request,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);
        log.info("Adding product {} to buybox tracking for store: {}", request.getProductId(), storeId);
        BuyboxTrackedProductDto tracked = buyboxService.addProductToTrack(storeId, request.getProductId(), user);
        return ResponseEntity.ok(tracked);
    }

    /**
     * Ürünü buybox takibinden çıkarır.
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @DeleteMapping("/stores/{storeId}/products/{trackedProductId}")
    public ResponseEntity<Void> removeProduct(
            @PathVariable UUID storeId,
            @PathVariable UUID trackedProductId,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);
        log.info("Removing product {} from buybox tracking for store: {}", trackedProductId, storeId);
        buyboxService.removeProductFromTrack(storeId, trackedProductId, user);
        return ResponseEntity.noContent().build();
    }

    /**
     * Takip edilen ürünün detaylarını getirir.
     */
    @GetMapping("/products/{trackedProductId}")
    public ResponseEntity<BuyboxProductDetailDto> getProductDetail(
            @PathVariable UUID trackedProductId,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);
        log.info("Getting buybox product detail: {}", trackedProductId);
        BuyboxProductDetailDto detail = buyboxService.getProductDetail(trackedProductId, user);
        return ResponseEntity.ok(detail);
    }

    /**
     * Takip edilen ürünün alert ayarlarını günceller.
     */
    @PutMapping("/products/{trackedProductId}/settings")
    public ResponseEntity<BuyboxTrackedProductDto> updateSettings(
            @PathVariable UUID trackedProductId,
            @Valid @RequestBody UpdateAlertSettingsRequest request,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);
        log.info("Updating alert settings for product: {}", trackedProductId);
        BuyboxTrackedProductDto updated = buyboxService.updateAlertSettings(trackedProductId, request, user);
        return ResponseEntity.ok(updated);
    }

    /**
     * Manuel buybox kontrolü yapar.
     */
    @PostMapping("/products/{trackedProductId}/check")
    public ResponseEntity<BuyboxSnapshotDto> checkNow(
            @PathVariable UUID trackedProductId,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);
        log.info("Manual buybox check for product: {}", trackedProductId);

        // Önce erişim kontrolü
        BuyboxProductDetailDto detail = buyboxService.getProductDetail(trackedProductId, user);

        // Sonra kontrol yap
        BuyboxTrackedProduct tracked = new BuyboxTrackedProduct();
        tracked.setId(trackedProductId);

        // Service'den snapshot döndür
        // Not: Bu noktada service direkt tracked entity'ye ihtiyaç duyuyor,
        // ama güvenlik için önce detail ile kontrol ettik
        BuyboxProductDetailDto refreshedDetail = buyboxService.getProductDetail(trackedProductId, user);

        return ResponseEntity.ok(BuyboxSnapshotDto.builder()
                .checkedAt(refreshedDetail.getLastCheckedAt())
                .buyboxStatus(refreshedDetail.getCurrentStatus())
                .winnerMerchantId(refreshedDetail.getWinnerMerchantId())
                .winnerMerchantName(refreshedDetail.getWinnerName())
                .winnerPrice(refreshedDetail.getWinnerPrice())
                .winnerSellerScore(refreshedDetail.getWinnerSellerScore())
                .myPrice(refreshedDetail.getMyPrice())
                .myPosition(refreshedDetail.getMyPosition())
                .priceDifference(refreshedDetail.getPriceDifference())
                .totalSellers(refreshedDetail.getTotalSellers())
                .lowestPrice(refreshedDetail.getLowestPrice())
                .highestPrice(refreshedDetail.getHighestPrice())
                .build());
    }

    /**
     * Mağazanın okunmamış alertlerini getirir.
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/stores/{storeId}/alerts")
    public ResponseEntity<List<BuyboxAlertDto>> getAlerts(
            @PathVariable UUID storeId,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);
        log.info("Getting buybox alerts for store: {}", storeId);
        List<BuyboxAlertDto> alerts = buyboxService.getUnreadAlerts(storeId, user);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Mağazanın alertlerini okundu olarak işaretler.
     */
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @PostMapping("/stores/{storeId}/alerts/mark-read")
    public ResponseEntity<Void> markAlertsRead(
            @PathVariable UUID storeId,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);
        log.info("Marking buybox alerts as read for store: {}", storeId);
        buyboxService.markAlertsAsRead(storeId, user);
        return ResponseEntity.noContent().build();
    }

    /**
     * Authentication'dan User nesnesini çıkarır.
     * JwtAuthenticationFilter userId'yi principal olarak set ediyor.
     */
    private User getAuthenticatedUser(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return userRepository.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("User not found"));
    }
}
