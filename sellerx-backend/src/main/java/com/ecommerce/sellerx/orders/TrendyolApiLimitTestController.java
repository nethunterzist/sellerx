package com.ecommerce.sellerx.orders;

import com.ecommerce.sellerx.financial.TrendyolOtherFinancialsService;
import com.ecommerce.sellerx.stores.MarketplaceCredentials;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.stores.TrendyolCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Temporary test controller to check Trendyol API date range limits.
 * DELETE THIS FILE after testing!
 */
@RestController
@RequestMapping("/api/test/trendyol-limits")
@RequiredArgsConstructor
@Slf4j
public class TrendyolApiLimitTestController {

    private static final String TRENDYOL_BASE_URL = "https://apigw.trendyol.com";

    private final StoreRepository storeRepository;
    private final RestTemplate restTemplate;
    private final TrendyolOtherFinancialsService otherFinancialsService;

    /**
     * Test Orders API with custom date range
     * Example: GET /api/test/trendyol-limits/orders/{storeId}?monthsBack=12
     */
    @GetMapping("/orders/{storeId}")
    public ResponseEntity<Map<String, Object>> testOrdersApi(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "6") int monthsBack) {

        Store store = storeRepository.findById(storeId).orElse(null);
        if (store == null) {
            return ResponseEntity.notFound().build();
        }

        TrendyolCredentials credentials = extractCredentials(store);
        if (credentials == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "No Trendyol credentials"));
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Istanbul"));
        LocalDateTime startDate = now.minusMonths(monthsBack);

        long startTimestamp = startDate.atZone(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();
        long endTimestamp = now.atZone(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();

        String url = String.format("%s/integration/order/sellers/%s/orders?page=0&size=10&startDate=%d&endDate=%d",
                TRENDYOL_BASE_URL, credentials.getSellerId(), startTimestamp, endTimestamp);

        log.info("Testing Orders API with {} months back", monthsBack);
        log.info("URL: {}", url);
        log.info("Start date: {} ({})", startDate, startTimestamp);
        log.info("End date: {} ({})", now, endTimestamp);

        try {
            HttpEntity<String> entity = createHttpEntity(credentials);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("monthsBack", monthsBack);
            result.put("startDate", startDate.toString());
            result.put("endDate", now.toString());
            result.put("statusCode", response.getStatusCode().value());
            result.put("responseBody", response.getBody());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("API call failed: {}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("monthsBack", monthsBack);
            result.put("error", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    /**
     * Test OtherFinancials (PaymentOrders) API with custom date range
     * Example: GET /api/test/trendyol-limits/financials/{storeId}?monthsBack=12
     */
    @GetMapping("/financials/{storeId}")
    public ResponseEntity<Map<String, Object>> testFinancialsApi(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "6") int monthsBack) {

        Store store = storeRepository.findById(storeId).orElse(null);
        if (store == null) {
            return ResponseEntity.notFound().build();
        }

        TrendyolCredentials credentials = extractCredentials(store);
        if (credentials == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "No Trendyol credentials"));
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Istanbul"));
        // Use 14-day chunks to respect API limit
        LocalDateTime startDate = now.minusMonths(monthsBack);
        LocalDateTime chunkEnd = startDate.plusDays(14);

        long startTimestamp = startDate.atZone(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();
        long endTimestamp = chunkEnd.atZone(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();

        String url = String.format("%s/integration/finance/che/sellers/%s/otherfinancials?transactionType=PaymentOrder&startDate=%d&endDate=%d&page=0&size=500",
                TRENDYOL_BASE_URL, credentials.getSellerId(), startTimestamp, endTimestamp);

        log.info("Testing OtherFinancials API with {} months back (first 14-day chunk)", monthsBack);
        log.info("URL: {}", url);
        log.info("Start date: {} ({})", startDate, startTimestamp);
        log.info("End date: {} ({})", chunkEnd, endTimestamp);

        try {
            HttpEntity<String> entity = createHttpEntity(credentials);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("monthsBack", monthsBack);
            result.put("startDate", startDate.toString());
            result.put("chunkEndDate", chunkEnd.toString());
            result.put("statusCode", response.getStatusCode().value());
            result.put("responseBody", response.getBody());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("API call failed: {}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("monthsBack", monthsBack);
            result.put("error", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    private TrendyolCredentials extractCredentials(Store store) {
        MarketplaceCredentials credentials = store.getCredentials();
        if (credentials instanceof TrendyolCredentials) {
            return (TrendyolCredentials) credentials;
        }
        return null;
    }

    /**
     * Sync ALL historical PaymentOrders for the last 12 months
     * Example: POST /api/test/trendyol-limits/sync-all-financials/{storeId}
     */
    @PostMapping("/sync-all-financials/{storeId}")
    public ResponseEntity<Map<String, Object>> syncAllFinancials(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "12") int monthsBack) {

        Store store = storeRepository.findById(storeId).orElse(null);
        if (store == null) {
            return ResponseEntity.notFound().build();
        }

        TrendyolCredentials credentials = extractCredentials(store);
        if (credentials == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "No Trendyol credentials"));
        }

        LocalDate now = LocalDate.now(ZoneId.of("Europe/Istanbul"));
        LocalDate startDate = now.minusMonths(monthsBack);

        log.info("Starting FULL sync of PaymentOrders for store: {} from {} to {}", storeId, startDate, now);

        try {
            int savedCount = otherFinancialsService.syncPaymentOrders(storeId, startDate, now);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("storeId", storeId.toString());
            result.put("monthsBack", monthsBack);
            result.put("startDate", startDate.toString());
            result.put("endDate", now.toString());
            result.put("savedCount", savedCount);
            result.put("message", "Successfully synced " + savedCount + " PaymentOrders from " + startDate + " to " + now);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Full sync failed for store {}: {}", storeId, e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("storeId", storeId.toString());
            result.put("error", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    /**
     * Test Claims (Returns) API with custom date range
     * Example: GET /api/test/trendyol-limits/claims/{storeId}?monthsBack=12
     */
    @GetMapping("/claims/{storeId}")
    public ResponseEntity<Map<String, Object>> testClaimsApi(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "3") int monthsBack) {

        Store store = storeRepository.findById(storeId).orElse(null);
        if (store == null) {
            return ResponseEntity.notFound().build();
        }

        TrendyolCredentials credentials = extractCredentials(store);
        if (credentials == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "No Trendyol credentials"));
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Istanbul"));
        LocalDateTime startDate = now.minusMonths(monthsBack);

        long startTimestamp = startDate.atZone(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();
        long endTimestamp = now.atZone(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();

        String url = String.format("%s/integration/order/sellers/%s/claims?page=0&size=50&startDate=%d&endDate=%d",
                TRENDYOL_BASE_URL, credentials.getSellerId(), startTimestamp, endTimestamp);

        log.info("Testing Claims API with {} months back", monthsBack);
        log.info("URL: {}", url);
        log.info("Start date: {} ({})", startDate, startTimestamp);
        log.info("End date: {} ({})", now, endTimestamp);

        try {
            HttpEntity<String> entity = createHttpEntity(credentials);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("monthsBack", monthsBack);
            result.put("startDate", startDate.toString());
            result.put("endDate", now.toString());
            result.put("statusCode", response.getStatusCode().value());
            result.put("responseBody", response.getBody());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("API call failed: {}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("monthsBack", monthsBack);
            result.put("error", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    /**
     * Test Settlements API with custom date range - MAY CONTAIN ORDER DETAILS!
     * Example: GET /api/test/trendyol-limits/settlements/{storeId}?monthsBack=12
     */
    @GetMapping("/settlements/{storeId}")
    public ResponseEntity<Map<String, Object>> testSettlementsApi(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "6") int monthsBack,
            @RequestParam(defaultValue = "Sale") String transactionType) {

        Store store = storeRepository.findById(storeId).orElse(null);
        if (store == null) {
            return ResponseEntity.notFound().build();
        }

        TrendyolCredentials credentials = extractCredentials(store);
        if (credentials == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "No Trendyol credentials"));
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Istanbul"));
        // Start from X months ago, but only fetch 14 days (API limit is 15 days max)
        LocalDateTime startDate = now.minusMonths(monthsBack);
        LocalDateTime endDate = startDate.plusDays(14); // 14-day chunk

        long startTimestamp = startDate.atZone(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();
        long endTimestamp = endDate.atZone(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();

        // Settlements API - contains order-level details (Sale, Return, Discount, Coupon)
        // transactionType can be: Sale, Return, Discount, Coupon, Commission
        // Size must be 500 or 1000 for this API
        String url = String.format("%s/integration/finance/che/sellers/%s/settlements?transactionType=%s&startDate=%d&endDate=%d&page=0&size=500",
                TRENDYOL_BASE_URL, credentials.getSellerId(), transactionType, startTimestamp, endTimestamp);

        log.info("Testing Settlements API - fetching 14-day chunk starting {} months back", monthsBack);
        log.info("URL: {}", url);

        try {
            HttpEntity<String> entity = createHttpEntity(credentials);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("monthsBack", monthsBack);
            result.put("chunkStartDate", startDate.toString());
            result.put("chunkEndDate", endDate.toString());
            result.put("statusCode", response.getStatusCode().value());
            result.put("responseBody", response.getBody());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("API call failed: {}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("monthsBack", monthsBack);
            result.put("chunkStartDate", startDate.toString());
            result.put("chunkEndDate", endDate.toString());
            result.put("error", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    /**
     * Test DeductionInvoices API - CHECK FOR PLATFORM FEE INVOICE TYPES
     * Example: GET /api/test/trendyol-limits/deduction-invoices/{storeId}?monthsBack=3
     *
     * This endpoint fetches DeductionInvoices and extracts unique description values
     * to identify if there are platform service invoices besides cargo invoices.
     */
    @GetMapping("/deduction-invoices/{storeId}")
    public ResponseEntity<Map<String, Object>> testDeductionInvoicesApi(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "3") int monthsBack) {

        Store store = storeRepository.findById(storeId).orElse(null);
        if (store == null) {
            return ResponseEntity.notFound().build();
        }

        TrendyolCredentials credentials = extractCredentials(store);
        if (credentials == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "No Trendyol credentials"));
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Istanbul"));
        // Use 14-day chunks to respect API limit
        LocalDateTime startDate = now.minusMonths(monthsBack);
        LocalDateTime chunkEnd = startDate.plusDays(14);

        long startTimestamp = startDate.atZone(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();
        long endTimestamp = chunkEnd.atZone(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();

        String url = String.format("%s/integration/finance/che/sellers/%s/otherfinancials?transactionType=DeductionInvoices&startDate=%d&endDate=%d&page=0&size=500",
                TRENDYOL_BASE_URL, credentials.getSellerId(), startTimestamp, endTimestamp);

        log.info("Testing DeductionInvoices API with {} months back (first 14-day chunk)", monthsBack);
        log.info("URL: {}", url);
        log.info("Start date: {} ({})", startDate, startTimestamp);
        log.info("Chunk end date: {} ({})", chunkEnd, endTimestamp);

        try {
            HttpEntity<String> entity = createHttpEntity(credentials);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            // Parse response to extract unique descriptions
            Set<String> uniqueDescriptions = new HashSet<>();
            List<Map<String, Object>> invoiceDetails = new ArrayList<>();

            if (response.getBody() != null && response.getBody().contains("content")) {
                // Simple JSON parsing for descriptions
                String body = response.getBody();
                int contentStart = body.indexOf("\"content\"");
                if (contentStart > 0) {
                    // Extract description fields
                    int idx = 0;
                    while ((idx = body.indexOf("\"description\"", idx)) > 0) {
                        int start = body.indexOf(":", idx) + 1;
                        int end = body.indexOf(",", start);
                        if (end < 0) end = body.indexOf("}", start);
                        if (start > 0 && end > start) {
                            String desc = body.substring(start, end).trim();
                            if (desc.startsWith("\"")) desc = desc.substring(1);
                            if (desc.endsWith("\"")) desc = desc.substring(0, desc.length() - 1);
                            uniqueDescriptions.add(desc);
                        }
                        idx = end;
                    }
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("monthsBack", monthsBack);
            result.put("startDate", startDate.toString());
            result.put("chunkEndDate", chunkEnd.toString());
            result.put("statusCode", response.getStatusCode().value());
            result.put("uniqueDescriptions", uniqueDescriptions);
            result.put("descriptionCount", uniqueDescriptions.size());
            result.put("rawResponse", response.getBody());

            log.info("Found {} unique description types in DeductionInvoices: {}", uniqueDescriptions.size(), uniqueDescriptions);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("API call failed: {}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("monthsBack", monthsBack);
            result.put("error", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    /**
     * Test Q&A (Questions) API with custom date range
     * Example: GET /api/test/trendyol-limits/questions/{storeId}?daysBack=90
     */
    @GetMapping("/questions/{storeId}")
    public ResponseEntity<Map<String, Object>> testQuestionsApi(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "30") int daysBack) {

        Store store = storeRepository.findById(storeId).orElse(null);
        if (store == null) {
            return ResponseEntity.notFound().build();
        }

        TrendyolCredentials credentials = extractCredentials(store);
        if (credentials == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "No Trendyol credentials"));
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Istanbul"));
        LocalDateTime startDate = now.minusDays(daysBack);

        long startTimestamp = startDate.atZone(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();
        long endTimestamp = now.atZone(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();

        String url = String.format("%s/integration/sellers/%s/questions?page=0&size=50&startDate=%d&endDate=%d&status=WAITING_FOR_ANSWER",
                TRENDYOL_BASE_URL, credentials.getSellerId(), startTimestamp, endTimestamp);

        log.info("Testing Questions API with {} days back", daysBack);
        log.info("URL: {}", url);
        log.info("Start date: {} ({})", startDate, startTimestamp);
        log.info("End date: {} ({})", now, endTimestamp);

        try {
            HttpEntity<String> entity = createHttpEntity(credentials);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("daysBack", daysBack);
            result.put("startDate", startDate.toString());
            result.put("endDate", now.toString());
            result.put("statusCode", response.getStatusCode().value());
            result.put("responseBody", response.getBody());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("API call failed: {}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("daysBack", daysBack);
            result.put("error", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    private HttpEntity<String> createHttpEntity(TrendyolCredentials credentials) {
        String auth = credentials.getApiKey() + ":" + credentials.getApiSecret();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.set("User-Agent", credentials.getSellerId() + " - SelfIntegration");

        return new HttpEntity<>(headers);
    }
}
