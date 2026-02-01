package com.ecommerce.sellerx.buybox;

import com.ecommerce.sellerx.buybox.dto.*;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.stores.TrendyolCredentials;
import com.ecommerce.sellerx.users.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Buybox takip sistemi ana servisi.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BuyboxService {

    private static final int MAX_TRACKED_PRODUCTS = 10;
    private static final int HISTORY_LIMIT = 30;
    private static final int RECENT_ALERTS_LIMIT = 5;

    private final BuyboxTrackedProductRepository trackedProductRepository;
    private final BuyboxSnapshotRepository snapshotRepository;
    private final BuyboxAlertRepository alertRepository;
    private final TrendyolProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final TrendyolBuyboxClient buyboxClient;
    private final ObjectMapper objectMapper;

    /**
     * Ürünü buybox takibine ekler.
     */
    @Transactional
    public BuyboxTrackedProductDto addProductToTrack(UUID storeId, UUID productId, User user) {
        // Mağaza kontrolü
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Mağaza bulunamadı: " + storeId));

        // Yetki kontrolü
        if (!store.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Bu mağazaya erişim yetkiniz yok");
        }

        // Ürün kontrolü - kullanıcının kendi ürünü olmalı
        TrendyolProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Ürün bulunamadı: " + productId));

        if (!product.getStore().getId().equals(storeId)) {
            throw new IllegalArgumentException("Bu ürün bu mağazaya ait değil");
        }

        // Zaten takipte mi?
        if (trackedProductRepository.existsByStoreIdAndProductId(storeId, productId)) {
            throw new IllegalStateException("Bu ürün zaten takipte");
        }

        // Limit kontrolü
        int currentCount = trackedProductRepository.countByStoreIdAndIsActiveTrue(storeId);
        if (currentCount >= MAX_TRACKED_PRODUCTS) {
            throw new IllegalStateException("Maksimum " + MAX_TRACKED_PRODUCTS + " ürün takip edilebilir");
        }

        // Yeni takip kaydı oluştur
        BuyboxTrackedProduct tracked = BuyboxTrackedProduct.builder()
                .store(store)
                .product(product)
                .isActive(true)
                .alertOnLoss(true)
                .alertOnNewCompetitor(true)
                .alertPriceThreshold(new BigDecimal("10.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        tracked = trackedProductRepository.save(tracked);

        // İlk buybox kontrolünü yap
        try {
            checkBuyboxForProduct(tracked);
        } catch (Exception e) {
            log.warn("Initial buybox check failed for product {}: {}", productId, e.getMessage());
        }

        return mapToTrackedProductDto(tracked);
    }

    /**
     * Ürünü buybox takibinden çıkarır.
     */
    @Transactional
    public void removeProductFromTrack(UUID storeId, UUID trackedProductId, User user) {
        BuyboxTrackedProduct tracked = trackedProductRepository.findById(trackedProductId)
                .orElseThrow(() -> new IllegalArgumentException("Takip kaydı bulunamadı: " + trackedProductId));

        // Yetki kontrolü
        if (!tracked.getStore().getId().equals(storeId)) {
            throw new SecurityException("Bu takip kaydına erişim yetkiniz yok");
        }

        if (!tracked.getStore().getUser().getId().equals(user.getId())) {
            throw new SecurityException("Bu takip kaydına erişim yetkiniz yok");
        }

        // Tamamen sil (cascade ile snapshot ve alertler de silinir)
        trackedProductRepository.delete(tracked);
    }

    /**
     * Mağazanın takip ettiği ürünleri listeler.
     */
    @Transactional(readOnly = true)
    public List<BuyboxTrackedProductDto> getTrackedProducts(UUID storeId, User user) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Mağaza bulunamadı: " + storeId));

        if (!store.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Bu mağazaya erişim yetkiniz yok");
        }

        List<BuyboxTrackedProduct> products = trackedProductRepository.findByStoreIdAndIsActiveTrue(storeId);
        return products.stream().map(this::mapToTrackedProductDto).collect(Collectors.toList());
    }

    /**
     * Takip edilen ürünün detaylarını getirir.
     */
    @Transactional(readOnly = true)
    public BuyboxProductDetailDto getProductDetail(UUID trackedProductId, User user) {
        BuyboxTrackedProduct tracked = trackedProductRepository.findById(trackedProductId)
                .orElseThrow(() -> new IllegalArgumentException("Takip kaydı bulunamadı: " + trackedProductId));

        if (!tracked.getStore().getUser().getId().equals(user.getId())) {
            throw new SecurityException("Bu takip kaydına erişim yetkiniz yok");
        }

        return mapToProductDetailDto(tracked);
    }

    /**
     * Alert ayarlarını günceller.
     */
    @Transactional
    public BuyboxTrackedProductDto updateAlertSettings(UUID trackedProductId, UpdateAlertSettingsRequest request, User user) {
        BuyboxTrackedProduct tracked = trackedProductRepository.findById(trackedProductId)
                .orElseThrow(() -> new IllegalArgumentException("Takip kaydı bulunamadı: " + trackedProductId));

        if (!tracked.getStore().getUser().getId().equals(user.getId())) {
            throw new SecurityException("Bu takip kaydına erişim yetkiniz yok");
        }

        if (request.getAlertOnLoss() != null) {
            tracked.setAlertOnLoss(request.getAlertOnLoss());
        }
        if (request.getAlertOnNewCompetitor() != null) {
            tracked.setAlertOnNewCompetitor(request.getAlertOnNewCompetitor());
        }
        if (request.getAlertPriceThreshold() != null) {
            tracked.setAlertPriceThreshold(request.getAlertPriceThreshold());
        }
        if (request.getIsActive() != null) {
            tracked.setActive(request.getIsActive());
        }

        tracked = trackedProductRepository.save(tracked);
        return mapToTrackedProductDto(tracked);
    }

    /**
     * Dashboard verilerini getirir.
     */
    @Transactional(readOnly = true)
    public BuyboxDashboardDto getDashboard(UUID storeId, User user) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Mağaza bulunamadı: " + storeId));

        if (!store.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Bu mağazaya erişim yetkiniz yok");
        }

        List<BuyboxTrackedProduct> products = trackedProductRepository.findByStoreIdAndIsActiveTrue(storeId);
        List<BuyboxTrackedProductDto> productDtos = products.stream().map(this::mapToTrackedProductDto).collect(Collectors.toList());

        // Durum sayıları
        int wonCount = 0, lostCount = 0, riskCount = 0, noCompetitionCount = 0;
        for (BuyboxTrackedProductDto dto : productDtos) {
            if (dto.getLastStatus() != null) {
                switch (dto.getLastStatus()) {
                    case WON -> wonCount++;
                    case LOST -> lostCount++;
                    case RISK -> riskCount++;
                    case NO_COMPETITION -> noCompetitionCount++;
                }
            }
        }

        // Okunmamış alertler
        int unreadAlertCount = alertRepository.countByStoreIdAndIsReadFalse(storeId);
        List<BuyboxAlert> recentAlerts = alertRepository.findByStoreIdAndIsReadFalseOrderByCreatedAtDesc(storeId);
        List<BuyboxAlertDto> alertDtos = recentAlerts.stream()
                .limit(RECENT_ALERTS_LIMIT)
                .map(this::mapToAlertDto)
                .collect(Collectors.toList());

        return BuyboxDashboardDto.builder()
                .storeId(storeId)
                .totalTrackedProducts(products.size())
                .wonCount(wonCount)
                .lostCount(lostCount)
                .riskCount(riskCount)
                .noCompetitionCount(noCompetitionCount)
                .unreadAlertCount(unreadAlertCount)
                .products(productDtos)
                .recentAlerts(alertDtos)
                .lastUpdatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Okunmamış alertleri getirir.
     */
    @Transactional(readOnly = true)
    public List<BuyboxAlertDto> getUnreadAlerts(UUID storeId, User user) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Mağaza bulunamadı: " + storeId));

        if (!store.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Bu mağazaya erişim yetkiniz yok");
        }

        return alertRepository.findByStoreIdAndIsReadFalseOrderByCreatedAtDesc(storeId)
                .stream()
                .map(this::mapToAlertDto)
                .collect(Collectors.toList());
    }

    /**
     * Alertleri okundu olarak işaretler.
     */
    @Transactional
    public void markAlertsAsRead(UUID storeId, User user) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Mağaza bulunamadı: " + storeId));

        if (!store.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Bu mağazaya erişim yetkiniz yok");
        }

        alertRepository.markAllAsReadByStoreId(storeId, LocalDateTime.now());
    }

    /**
     * Belirli bir ürün için buybox kontrolü yapar (scheduled job veya manuel).
     */
    @Transactional
    public BuyboxSnapshot checkBuyboxForProduct(BuyboxTrackedProduct tracked) {
        TrendyolProduct product = tracked.getProduct();
        String trendyolProductId = product.getProductId();

        log.info("Checking buybox for product: {} (Trendyol ID: {})", product.getTitle(), trendyolProductId);

        // API'den veri çek
        BuyboxApiResponse apiResponse = buyboxClient.fetchBuyboxData(trendyolProductId, tracked.getStore().getId());

        if (!apiResponse.isSuccess()) {
            log.error("Buybox API failed for product {}: {}", trendyolProductId, apiResponse.getErrorMessage());
            throw new RuntimeException("Buybox verisi alınamadı: " + apiResponse.getErrorMessage());
        }

        // Önceki snapshot'ı al (karşılaştırma için)
        Optional<BuyboxSnapshot> previousSnapshot = snapshotRepository.findTopByTrackedProductIdOrderByCheckedAtDesc(tracked.getId());

        // Mağazanın merchant ID'sini bul
        Long myMerchantId = ((TrendyolCredentials) tracked.getStore().getCredentials()).getSellerId();
        MerchantInfo myMerchant = findMyMerchant(apiResponse, myMerchantId);

        // Buybox durumunu hesapla
        BuyboxStatus status = calculateBuyboxStatus(apiResponse, myMerchant, tracked.getAlertPriceThreshold());

        // Pozisyon hesapla
        Integer myPosition = calculateMyPosition(apiResponse, myMerchantId);

        // Fiyat farkı hesapla
        BigDecimal priceDifference = calculatePriceDifference(apiResponse, myMerchant);

        // Snapshot oluştur
        BuyboxSnapshot snapshot = BuyboxSnapshot.builder()
                .trackedProduct(tracked)
                .checkedAt(LocalDateTime.now())
                .buyboxStatus(status)
                .winnerMerchantId(apiResponse.getWinner() != null ? apiResponse.getWinner().getMerchantId() : null)
                .winnerMerchantName(apiResponse.getWinner() != null ? apiResponse.getWinner().getMerchantName() : null)
                .winnerPrice(apiResponse.getWinner() != null ? apiResponse.getWinner().getPrice() : null)
                .winnerSellerScore(apiResponse.getWinner() != null ? apiResponse.getWinner().getSellerScore() : null)
                .myPrice(myMerchant != null ? myMerchant.getPrice() : null)
                .myPosition(myPosition)
                .priceDifference(priceDifference)
                .totalSellers(apiResponse.getTotalSellers())
                .lowestPrice(apiResponse.getLowestPrice())
                .highestPrice(apiResponse.getHighestPrice())
                .competitorsJson(serializeCompetitors(apiResponse.getAllMerchants()))
                .build();

        snapshot = snapshotRepository.save(snapshot);

        // Alert oluşturma kontrolü
        createAlertsIfNeeded(tracked, snapshot, previousSnapshot.orElse(null), apiResponse);

        log.info("Buybox check completed for product {}: status={}, position={}",
                product.getTitle(), status, myPosition);

        return snapshot;
    }

    private MerchantInfo findMyMerchant(BuyboxApiResponse response, Long myMerchantId) {
        if (myMerchantId == null || response.getAllMerchants() == null) {
            return null;
        }
        return response.getAllMerchants().stream()
                .filter(m -> myMerchantId.equals(m.getMerchantId()))
                .findFirst()
                .orElse(null);
    }

    private BuyboxStatus calculateBuyboxStatus(BuyboxApiResponse response, MerchantInfo myMerchant, BigDecimal threshold) {
        if (response.getTotalSellers() <= 1) {
            return BuyboxStatus.NO_COMPETITION;
        }

        if (myMerchant == null) {
            return BuyboxStatus.LOST;
        }

        if (myMerchant.isWinner()) {
            // Risk kontrolü - fiyat farkı eşiğin altındaysa risk var
            if (response.getAllMerchants() != null && response.getAllMerchants().size() > 1) {
                BigDecimal secondLowestPrice = response.getAllMerchants().stream()
                        .filter(m -> !m.getMerchantId().equals(myMerchant.getMerchantId()))
                        .map(MerchantInfo::getPrice)
                        .min(BigDecimal::compareTo)
                        .orElse(myMerchant.getPrice());

                BigDecimal diff = secondLowestPrice.subtract(myMerchant.getPrice());
                if (diff.compareTo(threshold) < 0) {
                    return BuyboxStatus.RISK;
                }
            }
            return BuyboxStatus.WON;
        }

        return BuyboxStatus.LOST;
    }

    private Integer calculateMyPosition(BuyboxApiResponse response, Long myMerchantId) {
        if (myMerchantId == null || response.getAllMerchants() == null) {
            return null;
        }

        List<MerchantInfo> sorted = response.getAllMerchants().stream()
                .sorted(Comparator.comparing(MerchantInfo::getPrice))
                .toList();

        for (int i = 0; i < sorted.size(); i++) {
            if (myMerchantId.equals(sorted.get(i).getMerchantId())) {
                return i + 1;
            }
        }
        return null;
    }

    private BigDecimal calculatePriceDifference(BuyboxApiResponse response, MerchantInfo myMerchant) {
        if (myMerchant == null || response.getWinner() == null) {
            return null;
        }
        return myMerchant.getPrice().subtract(response.getWinner().getPrice());
    }

    private String serializeCompetitors(List<MerchantInfo> merchants) {
        try {
            return objectMapper.writeValueAsString(merchants);
        } catch (JsonProcessingException e) {
            log.error("Error serializing competitors: {}", e.getMessage());
            return "[]";
        }
    }

    private void createAlertsIfNeeded(BuyboxTrackedProduct tracked, BuyboxSnapshot current,
                                       BuyboxSnapshot previous, BuyboxApiResponse apiResponse) {
        if (previous == null) {
            return; // İlk kontrol, alert gerekmez
        }

        // Buybox kaybı
        if (tracked.isAlertOnLoss() &&
            previous.getBuyboxStatus() == BuyboxStatus.WON &&
            current.getBuyboxStatus() == BuyboxStatus.LOST) {

            createAlert(tracked, BuyboxAlertType.BUYBOX_LOST,
                    "Buybox Kaybedildi: " + tracked.getProduct().getTitle(),
                    tracked.getProduct().getTitle() + " ürününün buybox'ı " + current.getWinnerMerchantName() + " satıcısına geçti.",
                    previous.getWinnerMerchantName(),
                    current.getWinnerMerchantName(),
                    previous.getWinnerPrice(),
                    current.getWinnerPrice());
        }

        // Buybox kazanımı
        if (previous.getBuyboxStatus() == BuyboxStatus.LOST && current.getBuyboxStatus() == BuyboxStatus.WON) {
            createAlert(tracked, BuyboxAlertType.BUYBOX_WON,
                    "Buybox Kazanıldı: " + tracked.getProduct().getTitle(),
                    tracked.getProduct().getTitle() + " ürününün buybox'ını kazandınız!",
                    previous.getWinnerMerchantName(),
                    current.getWinnerMerchantName(),
                    previous.getWinnerPrice(),
                    current.getWinnerPrice());
        }

        // Fiyat riski
        if (tracked.isAlertOnLoss() &&
            current.getBuyboxStatus() == BuyboxStatus.RISK &&
            previous.getBuyboxStatus() != BuyboxStatus.RISK) {

            createAlert(tracked, BuyboxAlertType.PRICE_RISK,
                    "Fiyat Riski: " + tracked.getProduct().getTitle(),
                    tracked.getProduct().getTitle() + " ürününde rakiple fiyat farkı " +
                    tracked.getAlertPriceThreshold() + " TL eşiğinin altına düştü!",
                    null, null,
                    previous.getMyPrice(),
                    current.getPriceDifference());
        }

        // Yeni rakip
        if (tracked.isAlertOnNewCompetitor() &&
            previous.getTotalSellers() != null &&
            current.getTotalSellers() != null &&
            current.getTotalSellers() > previous.getTotalSellers()) {

            createAlert(tracked, BuyboxAlertType.NEW_COMPETITOR,
                    "Yeni Rakip: " + tracked.getProduct().getTitle(),
                    tracked.getProduct().getTitle() + " ürününe yeni rakip eklendi. Toplam satıcı sayısı: " + current.getTotalSellers(),
                    null, null,
                    new BigDecimal(previous.getTotalSellers()),
                    new BigDecimal(current.getTotalSellers()));
        }
    }

    private void createAlert(BuyboxTrackedProduct tracked, BuyboxAlertType type, String title, String message,
                             String oldWinner, String newWinner, BigDecimal priceBefore, BigDecimal priceAfter) {
        BuyboxAlert alert = BuyboxAlert.builder()
                .store(tracked.getStore())
                .trackedProduct(tracked)
                .alertType(type)
                .title(title)
                .message(message)
                .oldWinnerName(oldWinner)
                .newWinnerName(newWinner)
                .priceBefore(priceBefore)
                .priceAfter(priceAfter)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        alertRepository.save(alert);
        log.info("Alert created: {} - {}", type, title);
    }

    // Mapping methods
    private BuyboxTrackedProductDto mapToTrackedProductDto(BuyboxTrackedProduct tracked) {
        Optional<BuyboxSnapshot> lastSnapshot = snapshotRepository.findTopByTrackedProductIdOrderByCheckedAtDesc(tracked.getId());

        BuyboxTrackedProductDto.BuyboxTrackedProductDtoBuilder builder = BuyboxTrackedProductDto.builder()
                .id(tracked.getId())
                .storeId(tracked.getStore().getId())
                .productId(tracked.getProduct().getId())
                .productTitle(tracked.getProduct().getTitle())
                .productBarcode(tracked.getProduct().getBarcode())
                .productImageUrl(tracked.getProduct().getImage())
                .trendyolProductId(tracked.getProduct().getProductId())
                .isActive(tracked.isActive())
                .alertOnLoss(tracked.isAlertOnLoss())
                .alertOnNewCompetitor(tracked.isAlertOnNewCompetitor())
                .alertPriceThreshold(tracked.getAlertPriceThreshold())
                .createdAt(tracked.getCreatedAt())
                .updatedAt(tracked.getUpdatedAt());

        if (lastSnapshot.isPresent()) {
            BuyboxSnapshot snapshot = lastSnapshot.get();
            builder.lastStatus(snapshot.getBuyboxStatus())
                    .lastWinnerPrice(snapshot.getWinnerPrice())
                    .lastWinnerName(snapshot.getWinnerMerchantName())
                    .myPrice(snapshot.getMyPrice())
                    .priceDifference(snapshot.getPriceDifference())
                    .myPosition(snapshot.getMyPosition())
                    .totalSellers(snapshot.getTotalSellers())
                    .lastCheckedAt(snapshot.getCheckedAt());
        }

        return builder.build();
    }

    private BuyboxProductDetailDto mapToProductDetailDto(BuyboxTrackedProduct tracked) {
        Optional<BuyboxSnapshot> lastSnapshot = snapshotRepository.findTopByTrackedProductIdOrderByCheckedAtDesc(tracked.getId());

        List<BuyboxSnapshot> history = snapshotRepository.findByTrackedProductIdOrderByCheckedAtDesc(
                tracked.getId(), PageRequest.of(0, HISTORY_LIMIT));

        List<BuyboxAlert> alerts = alertRepository.findByTrackedProductIdOrderByCreatedAtDesc(tracked.getId());

        BuyboxProductDetailDto.BuyboxProductDetailDtoBuilder builder = BuyboxProductDetailDto.builder()
                .id(tracked.getId())
                .storeId(tracked.getStore().getId())
                .productId(tracked.getProduct().getId())
                .productTitle(tracked.getProduct().getTitle())
                .productBarcode(tracked.getProduct().getBarcode())
                .productImageUrl(tracked.getProduct().getImage())
                .trendyolProductId(tracked.getProduct().getProductId())
                .trendyolUrl("https://www.trendyol.com/-p-" + tracked.getProduct().getProductId())
                .isActive(tracked.isActive())
                .alertOnLoss(tracked.isAlertOnLoss())
                .alertOnNewCompetitor(tracked.isAlertOnNewCompetitor())
                .alertPriceThreshold(tracked.getAlertPriceThreshold())
                .createdAt(tracked.getCreatedAt());

        if (lastSnapshot.isPresent()) {
            BuyboxSnapshot snapshot = lastSnapshot.get();
            builder.currentStatus(snapshot.getBuyboxStatus())
                    .winnerPrice(snapshot.getWinnerPrice())
                    .winnerName(snapshot.getWinnerMerchantName())
                    .winnerMerchantId(snapshot.getWinnerMerchantId())
                    .winnerSellerScore(snapshot.getWinnerSellerScore())
                    .myPrice(snapshot.getMyPrice())
                    .myPosition(snapshot.getMyPosition())
                    .priceDifference(snapshot.getPriceDifference())
                    .totalSellers(snapshot.getTotalSellers())
                    .lowestPrice(snapshot.getLowestPrice())
                    .highestPrice(snapshot.getHighestPrice())
                    .lastCheckedAt(snapshot.getCheckedAt());

            // Parse competitors
            if (snapshot.getCompetitorsJson() != null) {
                try {
                    List<MerchantInfo> competitors = objectMapper.readValue(
                            snapshot.getCompetitorsJson(),
                            objectMapper.getTypeFactory().constructCollectionType(List.class, MerchantInfo.class));
                    builder.competitors(competitors);
                } catch (JsonProcessingException e) {
                    log.error("Error parsing competitors JSON: {}", e.getMessage());
                    builder.competitors(Collections.emptyList());
                }
            }
        }

        builder.history(history.stream().map(this::mapToSnapshotDto).collect(Collectors.toList()));
        builder.recentAlerts(alerts.stream().limit(RECENT_ALERTS_LIMIT).map(this::mapToAlertDto).collect(Collectors.toList()));

        return builder.build();
    }

    private BuyboxSnapshotDto mapToSnapshotDto(BuyboxSnapshot snapshot) {
        return BuyboxSnapshotDto.builder()
                .id(snapshot.getId())
                .checkedAt(snapshot.getCheckedAt())
                .buyboxStatus(snapshot.getBuyboxStatus())
                .winnerMerchantId(snapshot.getWinnerMerchantId())
                .winnerMerchantName(snapshot.getWinnerMerchantName())
                .winnerPrice(snapshot.getWinnerPrice())
                .winnerSellerScore(snapshot.getWinnerSellerScore())
                .myPrice(snapshot.getMyPrice())
                .myPosition(snapshot.getMyPosition())
                .priceDifference(snapshot.getPriceDifference())
                .totalSellers(snapshot.getTotalSellers())
                .lowestPrice(snapshot.getLowestPrice())
                .highestPrice(snapshot.getHighestPrice())
                .build();
    }

    private BuyboxAlertDto mapToAlertDto(BuyboxAlert alert) {
        return BuyboxAlertDto.builder()
                .id(alert.getId())
                .storeId(alert.getStore().getId())
                .trackedProductId(alert.getTrackedProduct().getId())
                .alertType(alert.getAlertType())
                .title(alert.getTitle())
                .message(alert.getMessage())
                .productTitle(alert.getTrackedProduct().getProduct().getTitle())
                .productImageUrl(alert.getTrackedProduct().getProduct().getImage())
                .oldWinnerName(alert.getOldWinnerName())
                .newWinnerName(alert.getNewWinnerName())
                .priceBefore(alert.getPriceBefore())
                .priceAfter(alert.getPriceAfter())
                .isRead(alert.isRead())
                .createdAt(alert.getCreatedAt())
                .readAt(alert.getReadAt())
                .build();
    }
}
