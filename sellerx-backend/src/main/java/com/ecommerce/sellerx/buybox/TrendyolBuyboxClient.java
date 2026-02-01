package com.ecommerce.sellerx.buybox;

import com.ecommerce.sellerx.buybox.dto.BuyboxApiResponse;
import com.ecommerce.sellerx.buybox.dto.MerchantInfo;
import com.ecommerce.sellerx.common.TrendyolRateLimiter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Trendyol Buybox API client.
 * Product Detail API kullanarak buybox bilgilerini çeker.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrendyolBuyboxClient {

    private static final String PRODUCT_DETAIL_URL = "https://apigw.trendyol.com/discovery-web-productgw-service/api/productDetail/";

    private final RestTemplate restTemplate;
    private final TrendyolRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    /**
     * Trendyol Product ID kullanarak buybox verilerini çeker.
     *
     * @param trendyolProductId Trendyol ürün ID'si (örn: "811009408")
     * @param storeId the store UUID for per-store rate limiting
     * @return BuyboxApiResponse
     */
    public BuyboxApiResponse fetchBuyboxData(String trendyolProductId, UUID storeId) {
        rateLimiter.acquire(storeId);

        try {
            String url = PRODUCT_DETAIL_URL + trendyolProductId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.set("Accept", "application/json");
            headers.set("Accept-Language", "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.error("Trendyol API request failed for product {}: status={}", trendyolProductId, response.getStatusCode());
                return BuyboxApiResponse.builder()
                        .success(false)
                        .errorMessage("API isteği başarısız: " + response.getStatusCode())
                        .build();
            }

            return parseResponse(response.getBody(), trendyolProductId);

        } catch (Exception e) {
            log.error("Error fetching buybox data for product {}: {}", trendyolProductId, e.getMessage(), e);
            return BuyboxApiResponse.builder()
                    .success(false)
                    .errorMessage("API hatası: " + e.getMessage())
                    .build();
        }
    }

    private BuyboxApiResponse parseResponse(String jsonResponse, String productId) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode result = root.path("result");

            if (result.isMissingNode()) {
                return BuyboxApiResponse.builder()
                        .success(false)
                        .errorMessage("API yanıtında 'result' bulunamadı")
                        .build();
            }

            String productName = result.path("name").asText("");
            String brandName = result.path("brand").path("name").asText("");
            BigDecimal averageRating = getDecimalValue(result.path("ratingScore").path("averageRating"));

            List<MerchantInfo> allMerchants = new ArrayList<>();
            MerchantInfo winner = null;

            // merchantListings - buybox kazananı ve rakipler
            JsonNode merchantListings = result.path("merchantListings");
            if (merchantListings.isArray()) {
                for (JsonNode listing : merchantListings) {
                    MerchantInfo merchant = parseMerchantListing(listing);
                    allMerchants.add(merchant);
                    if (merchant.isWinner()) {
                        winner = merchant;
                    }
                }
            }

            // otherMerchants - diğer satıcılar
            JsonNode otherMerchants = result.path("otherMerchants");
            if (otherMerchants.isArray()) {
                for (JsonNode merchant : otherMerchants) {
                    allMerchants.add(parseOtherMerchant(merchant));
                }
            }

            // Fiyata göre sırala
            allMerchants.sort(Comparator.comparing(MerchantInfo::getPrice));

            // İstatistikler
            int totalSellers = allMerchants.size();
            BigDecimal lowestPrice = allMerchants.isEmpty() ? BigDecimal.ZERO :
                    allMerchants.stream().map(MerchantInfo::getPrice).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal highestPrice = allMerchants.isEmpty() ? BigDecimal.ZERO :
                    allMerchants.stream().map(MerchantInfo::getPrice).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

            return BuyboxApiResponse.builder()
                    .success(true)
                    .productName(productName)
                    .brandName(brandName)
                    .averageRating(averageRating)
                    .allMerchants(allMerchants)
                    .winner(winner)
                    .totalSellers(totalSellers)
                    .lowestPrice(lowestPrice)
                    .highestPrice(highestPrice)
                    .build();

        } catch (Exception e) {
            log.error("Error parsing buybox response for product {}: {}", productId, e.getMessage(), e);
            return BuyboxApiResponse.builder()
                    .success(false)
                    .errorMessage("JSON parse hatası: " + e.getMessage())
                    .build();
        }
    }

    private MerchantInfo parseMerchantListing(JsonNode listing) {
        Long merchantId = listing.path("merchantId").asLong();
        String merchantName = listing.path("merchantName").asText("");
        BigDecimal price = getDecimalValue(listing.path("price").path("sellingPrice"));
        BigDecimal sellerScore = getDecimalValue(listing.path("sellerScore"));
        boolean isWinner = listing.path("isWinner").asBoolean(false);
        boolean hasStock = listing.path("hasStock").asBoolean(true);
        boolean isFreeCargo = listing.path("freeCargo").asBoolean(false);
        String deliveryDate = listing.path("deliveryDate").asText("");

        return MerchantInfo.builder()
                .merchantId(merchantId)
                .merchantName(merchantName)
                .price(price)
                .sellerScore(sellerScore)
                .isWinner(isWinner)
                .hasStock(hasStock)
                .isFreeCargo(isFreeCargo)
                .deliveryDate(deliveryDate)
                .build();
    }

    private MerchantInfo parseOtherMerchant(JsonNode merchant) {
        Long merchantId = merchant.path("merchantId").asLong();
        String merchantName = merchant.path("merchantName").asText("");
        BigDecimal price = getDecimalValue(merchant.path("price"));
        BigDecimal sellerScore = getDecimalValue(merchant.path("sellerScore"));
        boolean hasStock = merchant.path("hasStock").asBoolean(true);
        boolean isFreeCargo = merchant.path("freeCargo").asBoolean(false);
        String deliveryDate = merchant.path("deliveryDate").asText("");

        return MerchantInfo.builder()
                .merchantId(merchantId)
                .merchantName(merchantName)
                .price(price)
                .sellerScore(sellerScore)
                .isWinner(false)
                .hasStock(hasStock)
                .isFreeCargo(isFreeCargo)
                .deliveryDate(deliveryDate)
                .build();
    }

    private BigDecimal getDecimalValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(node.asText("0"));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
