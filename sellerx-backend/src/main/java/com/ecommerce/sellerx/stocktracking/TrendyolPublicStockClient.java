package com.ecommerce.sellerx.stocktracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client for fetching stock data from public Trendyol product pages.
 * Uses HTML scraping since there's no public API for competitor stock levels.
 */
@Slf4j
@Component
public class TrendyolPublicStockClient {

    private final RestTemplate restTemplate;

    // Rate limiting: 1 request per second for public API
    private static final long MIN_REQUEST_INTERVAL_MS = 1000;
    private long lastRequestTime = 0;

    // Regex patterns for extracting data from HTML
    private static final Pattern QUANTITY_PATTERN = Pattern.compile("\"quantity\":\\s*(\\d+)");
    private static final Pattern IN_STOCK_PATTERN = Pattern.compile("\"inStock\":\\s*(true|false)");
    private static final Pattern PRICE_PATTERN = Pattern.compile("\"sellingPrice\":\\s*([\\d.]+)");
    private static final Pattern PRODUCT_NAME_PATTERN = Pattern.compile("\"name\":\\s*\"([^\"]+)\"");
    private static final Pattern BRAND_PATTERN = Pattern.compile("\"brand\":\\s*\\{[^}]*\"name\":\\s*\"([^\"]+)\"");
    private static final Pattern IMAGE_PATTERN = Pattern.compile("\"imageUrl\":\\s*\"([^\"]+)\"");
    private static final Pattern PRODUCT_ID_PATTERN = Pattern.compile("-p-(\\d+)");

    public TrendyolPublicStockClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Fetch stock data for a product URL
     */
    public StockData fetchStock(String productUrl) {
        enforceRateLimit();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.set("Accept", "text/html,application/xhtml+xml");
            headers.set("Accept-Language", "tr-TR,tr;q=0.9");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    productUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseHtml(response.getBody(), productUrl);
            }

            log.warn("Failed to fetch stock for URL: {}, status: {}", productUrl, response.getStatusCode());
            return null;

        } catch (Exception e) {
            log.error("Error fetching stock for URL: {}", productUrl, e);
            return null;
        }
    }

    /**
     * Parse HTML to extract stock data
     */
    private StockData parseHtml(String html, String productUrl) {
        StockData.StockDataBuilder builder = StockData.builder();

        // Extract product ID from URL
        Matcher productIdMatcher = PRODUCT_ID_PATTERN.matcher(productUrl);
        if (productIdMatcher.find()) {
            builder.productId(Long.parseLong(productIdMatcher.group(1)));
        }

        // Extract quantity
        Matcher quantityMatcher = QUANTITY_PATTERN.matcher(html);
        if (quantityMatcher.find()) {
            builder.quantity(Integer.parseInt(quantityMatcher.group(1)));
        } else {
            builder.quantity(0);
        }

        // Extract inStock
        Matcher inStockMatcher = IN_STOCK_PATTERN.matcher(html);
        if (inStockMatcher.find()) {
            builder.inStock(Boolean.parseBoolean(inStockMatcher.group(1)));
        } else {
            builder.inStock(false);
        }

        // Extract price
        Matcher priceMatcher = PRICE_PATTERN.matcher(html);
        if (priceMatcher.find()) {
            builder.price(new BigDecimal(priceMatcher.group(1)));
        }

        // Extract product name
        Matcher nameMatcher = PRODUCT_NAME_PATTERN.matcher(html);
        if (nameMatcher.find()) {
            builder.productName(nameMatcher.group(1));
        }

        // Extract brand
        Matcher brandMatcher = BRAND_PATTERN.matcher(html);
        if (brandMatcher.find()) {
            builder.brandName(brandMatcher.group(1));
        }

        // Extract image URL
        Matcher imageMatcher = IMAGE_PATTERN.matcher(html);
        if (imageMatcher.find()) {
            builder.imageUrl(imageMatcher.group(1));
        }

        return builder.build();
    }

    /**
     * Enforce rate limiting (1 req/sec)
     */
    private synchronized void enforceRateLimit() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRequestTime;

        if (elapsed < MIN_REQUEST_INTERVAL_MS) {
            try {
                Thread.sleep(MIN_REQUEST_INTERVAL_MS - elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        lastRequestTime = System.currentTimeMillis();
    }

    /**
     * Extract product ID from Trendyol URL
     */
    public Long extractProductId(String url) {
        Matcher matcher = PRODUCT_ID_PATTERN.matcher(url);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return null;
    }

    /**
     * Validate if URL is a valid Trendyol product URL
     */
    public boolean isValidProductUrl(String url) {
        return url != null
                && url.contains("trendyol.com")
                && PRODUCT_ID_PATTERN.matcher(url).find();
    }

    /**
     * Data class for stock information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockData {
        private Long productId;
        private Integer quantity;
        private Boolean inStock;
        private BigDecimal price;
        private String productName;
        private String brandName;
        private String imageUrl;
    }
}
