package com.ecommerce.sellerx.returns;

import com.ecommerce.sellerx.returns.dto.*;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.stores.TrendyolCredentials;
import com.ecommerce.sellerx.stores.MarketplaceCredentials;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendyolClaimsService {

    private static final String TRENDYOL_BASE_URL = "https://apigw.trendyol.com";
    private static final ZoneId TURKEY_ZONE = ZoneId.of("Europe/Istanbul");

    private final TrendyolClaimRepository claimRepository;
    private final StoreRepository storeRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Sync claims from Trendyol API
     * GET /integration/order/sellers/{sellerId}/claims
     */
    @Transactional
    public ClaimsSyncResponse syncClaims(UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));
        TrendyolCredentials credentials = extractTrendyolCredentials(store);

        if (credentials == null) {
            return ClaimsSyncResponse.builder()
                    .totalFetched(0)
                    .newClaims(0)
                    .updatedClaims(0)
                    .message("Trendyol credentials not found")
                    .build();
        }

        int totalFetched = 0;
        int newClaims = 0;
        int updatedClaims = 0;
        int page = 0;
        int size = 50;
        boolean hasMore = true;

        // Fetch ALL claims from store's beginning
        // Trendyol Claims API has NO date limit (tested up to 10 years back)
        // Use 2017-10-01 as Trendyol's launch date for maximum coverage
        LocalDate startLocalDate = LocalDate.of(2017, 10, 1);
        LocalDate endLocalDate = LocalDate.now();

        long startDate = startLocalDate.atStartOfDay(TURKEY_ZONE).toInstant().toEpochMilli();
        long endDate = endLocalDate.atStartOfDay(TURKEY_ZONE).toInstant().toEpochMilli();

        log.info("Syncing ALL claims for store {} from {} to {}", storeId, startLocalDate, endLocalDate);

        try {
            while (hasMore) {
                String url = String.format(
                        "%s/integration/order/sellers/%s/claims?page=%d&size=%d&startDate=%d&endDate=%d",
                        TRENDYOL_BASE_URL, credentials.getSellerId(), page, size, startDate, endDate
                );
                log.info("Requesting Claims from URL: {}", url);

                HttpEntity<String> entity = createHttpEntity(credentials);
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode content = root.path("content");

                    if (content.isArray() && content.size() > 0) {
                        for (JsonNode claimNode : content) {
                            // Trendyol API migration: id -> claimId (08.12.2025)
                            String claimId = claimNode.has("claimId")
                                    ? claimNode.path("claimId").asText()
                                    : claimNode.path("id").asText();

                            Optional<TrendyolClaim> existingOpt = claimRepository.findByStoreIdAndClaimId(storeId, claimId);

                            if (existingOpt.isPresent()) {
                                TrendyolClaim existing = existingOpt.get();
                                updateClaimFromJson(existing, claimNode);
                                claimRepository.save(existing);
                                updatedClaims++;
                            } else {
                                TrendyolClaim newClaim = createClaimFromJson(store, claimNode);
                                claimRepository.save(newClaim);
                                newClaims++;
                            }
                            totalFetched++;
                        }

                        int totalPages = root.path("totalPages").asInt(1);
                        page++;
                        hasMore = page < totalPages;
                    } else {
                        hasMore = false;
                    }
                } else {
                    hasMore = false;
                }
            }

            log.info("Claims sync completed for store {}: {} fetched, {} new, {} updated",
                    storeId, totalFetched, newClaims, updatedClaims);

            return ClaimsSyncResponse.builder()
                    .totalFetched(totalFetched)
                    .newClaims(newClaims)
                    .updatedClaims(updatedClaims)
                    .message("Sync completed successfully")
                    .build();

        } catch (Exception e) {
            log.error("Error syncing claims for store {}: {}", storeId, e.getMessage());
            return ClaimsSyncResponse.builder()
                    .totalFetched(totalFetched)
                    .newClaims(newClaims)
                    .updatedClaims(updatedClaims)
                    .message("Sync failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Get claims from database with pagination and optional status filter
     */
    public Page<ClaimDto> getClaims(UUID storeId, String status, Pageable pageable) {
        Page<TrendyolClaim> claims;

        if (status != null && !status.isEmpty()) {
            claims = claimRepository.findByStoreIdAndStatusOrderByClaimDateDesc(storeId, status, pageable);
        } else {
            claims = claimRepository.findByStoreIdOrderByClaimDateDesc(storeId, pageable);
        }

        return claims.map(this::toClaimDto);
    }

    /**
     * Get claims with multiple statuses (e.g., pending = WaitingInAction + Created)
     */
    public Page<ClaimDto> getClaimsByStatuses(UUID storeId, List<String> statuses, Pageable pageable) {
        Page<TrendyolClaim> claims = claimRepository.findByStoreIdAndStatusInOrderByClaimDateDesc(storeId, statuses, pageable);
        return claims.map(this::toClaimDto);
    }

    /**
     * Get single claim by ID
     */
    public ClaimDto getClaim(UUID storeId, String claimId) {
        TrendyolClaim claim = claimRepository.findByStoreIdAndClaimId(storeId, claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found"));
        return toClaimDto(claim);
    }

    /**
     * Approve claim items
     * PUT /integration/order/sellers/{sellerId}/claims/{claimId}/items/approve
     */
    @Transactional
    public ClaimActionResponse approveClaim(UUID storeId, String claimId, List<String> claimLineItemIds) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));
        TrendyolCredentials credentials = extractTrendyolCredentials(store);

        if (credentials == null) {
            return ClaimActionResponse.builder()
                    .success(false)
                    .message("Trendyol credentials not found")
                    .build();
        }

        TrendyolClaim claim = claimRepository.findByStoreIdAndClaimId(storeId, claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found"));

        // Only WaitingInAction claims can be approved
        if (!"WaitingInAction".equals(claim.getStatus())) {
            return ClaimActionResponse.builder()
                    .success(false)
                    .message("Only claims with 'WaitingInAction' status can be approved. Current status: " + claim.getStatus())
                    .build();
        }

        try {
            String url = String.format(
                    "%s/integration/order/sellers/%s/claims/%s/items/approve",
                    TRENDYOL_BASE_URL, credentials.getSellerId(), claimId
            );

            Map<String, Object> body = new HashMap<>();
            body.put("claimLineItemIdList", claimLineItemIds);
            body.put("params", new HashMap<>());

            HttpEntity<Map<String, Object>> entity = createHttpEntityWithBody(credentials, body);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.NO_CONTENT) {
                // Update local status
                claim.setStatus("Accepted");
                claim.setSyncedAt(LocalDateTime.now());
                claimRepository.save(claim);

                log.info("Claim {} approved successfully for store {}", claimId, storeId);
                return ClaimActionResponse.builder()
                        .success(true)
                        .message("Claim approved successfully")
                        .claimId(claimId)
                        .newStatus("Accepted")
                        .build();
            } else {
                log.error("Failed to approve claim {}: Status {}", claimId, response.getStatusCode());
                return ClaimActionResponse.builder()
                        .success(false)
                        .message("Trendyol API returned status: " + response.getStatusCode())
                        .build();
            }

        } catch (Exception e) {
            log.error("Error approving claim {}: {}", claimId, e.getMessage());
            return ClaimActionResponse.builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Reject claim (create issue)
     * POST /integration/order/sellers/{sellerId}/claims/{claimId}/issue
     */
    @Transactional
    public ClaimActionResponse rejectClaim(UUID storeId, String claimId, Integer reasonId,
                                           List<String> claimItemIds, String description) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));
        TrendyolCredentials credentials = extractTrendyolCredentials(store);

        if (credentials == null) {
            return ClaimActionResponse.builder()
                    .success(false)
                    .message("Trendyol credentials not found")
                    .build();
        }

        TrendyolClaim claim = claimRepository.findByStoreIdAndClaimId(storeId, claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found"));

        // Only WaitingInAction claims can be rejected
        if (!"WaitingInAction".equals(claim.getStatus())) {
            return ClaimActionResponse.builder()
                    .success(false)
                    .message("Only claims with 'WaitingInAction' status can be rejected. Current status: " + claim.getStatus())
                    .build();
        }

        try {
            // Build URL with query params
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append(String.format(
                    "%s/integration/order/sellers/%s/claims/%s/issue",
                    TRENDYOL_BASE_URL, credentials.getSellerId(), claimId
            ));
            urlBuilder.append("?claimIssueReasonId=").append(reasonId);

            // Add claim item IDs
            for (String itemId : claimItemIds) {
                urlBuilder.append("&claimItemIdList=").append(itemId);
            }

            // Add description if provided
            if (description != null && !description.isEmpty()) {
                urlBuilder.append("&description=").append(java.net.URLEncoder.encode(description, "UTF-8"));
            }

            String url = urlBuilder.toString();
            log.info("Rejecting claim with URL: {}", url);

            HttpEntity<String> entity = createHttpEntity(credentials);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                // Update local status
                claim.setStatus("Unresolved");
                claim.setSyncedAt(LocalDateTime.now());
                claimRepository.save(claim);

                log.info("Claim {} rejected successfully for store {}", claimId, storeId);
                return ClaimActionResponse.builder()
                        .success(true)
                        .message("Claim rejection submitted successfully")
                        .claimId(claimId)
                        .newStatus("Unresolved")
                        .build();
            } else {
                log.error("Failed to reject claim {}: Status {}", claimId, response.getStatusCode());
                return ClaimActionResponse.builder()
                        .success(false)
                        .message("Trendyol API returned status: " + response.getStatusCode())
                        .build();
            }

        } catch (Exception e) {
            log.error("Error rejecting claim {}: {}", claimId, e.getMessage());
            return ClaimActionResponse.builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Get claim issue reasons (static list based on Trendyol documentation)
     */
    public List<ClaimIssueReasonDto> getClaimIssueReasons() {
        return Arrays.asList(
                ClaimIssueReasonDto.builder().id(1).name("İade gelen ürün sahte").requiresFile(true).build(),
                ClaimIssueReasonDto.builder().id(51).name("İade gelen ürün kullanılmış").requiresFile(true).build(),
                ClaimIssueReasonDto.builder().id(201).name("İade gelen ürün yanlış").requiresFile(true).build(),
                ClaimIssueReasonDto.builder().id(251).name("İade gelen ürün defolu/zarar görmüş").requiresFile(true).build(),
                ClaimIssueReasonDto.builder().id(401).name("Eksik ürün").requiresFile(true).build(),
                ClaimIssueReasonDto.builder().id(101).name("Fazla ürün").requiresFile(true).build(),
                ClaimIssueReasonDto.builder().id(151).name("Ürünün parçası/aksesuarı eksik").requiresFile(true).build(),
                ClaimIssueReasonDto.builder().id(301).name("Ürün bana ait değil").requiresFile(true).build(),
                ClaimIssueReasonDto.builder().id(351).name("İade paketi boş geldi").requiresFile(true).build(),
                ClaimIssueReasonDto.builder().id(1651).name("İade paketi elime ulaşmadı").requiresFile(false).build(),
                ClaimIssueReasonDto.builder().id(1701).name("Ürün yanlış değil").requiresFile(false).build(),
                ClaimIssueReasonDto.builder().id(1751).name("Ürün kusurlu değil").requiresFile(false).build(),
                ClaimIssueReasonDto.builder().id(451).name("Diğer").requiresFile(false).build()
        );
    }

    /**
     * Get claims statistics for a store
     */
    public ClaimsStatsDto getStats(UUID storeId) {
        long total = claimRepository.countByStoreId(storeId);
        long pending = claimRepository.countByStoreIdAndStatus(storeId, "WaitingInAction");
        long accepted = claimRepository.countByStoreIdAndStatus(storeId, "Accepted");
        long rejected = claimRepository.countByStoreIdAndStatus(storeId, "Rejected");
        long unresolved = claimRepository.countByStoreIdAndStatus(storeId, "Unresolved");
        long waitingFraudCheck = claimRepository.countByStoreIdAndStatus(storeId, "WaitingFraudCheck");

        return ClaimsStatsDto.builder()
                .totalClaims(total)
                .pendingClaims(pending)
                .acceptedClaims(accepted)
                .rejectedClaims(rejected)
                .unresolvedClaims(unresolved)
                .waitingFraudCheckClaims(waitingFraudCheck)
                .build();
    }

    /**
     * Test Claims API date range limits
     * Used to discover how far back the API allows querying
     */
    public ClaimsDateRangeTestResult testDateRange(UUID storeId, int yearsBack) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));
        TrendyolCredentials credentials = extractTrendyolCredentials(store);

        if (credentials == null) {
            return ClaimsDateRangeTestResult.builder()
                    .success(false)
                    .message("Trendyol credentials not found")
                    .yearsBack(yearsBack)
                    .build();
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusYears(yearsBack);

        // Convert to milliseconds
        long endDateMs = endDate.atStartOfDay(TURKEY_ZONE).toInstant().toEpochMilli();
        long startDateMs = startDate.atStartOfDay(TURKEY_ZONE).toInstant().toEpochMilli();

        long startTime = System.currentTimeMillis();

        try {
            String url = String.format(
                    "%s/integration/order/sellers/%s/claims?page=0&size=10&startDate=%d&endDate=%d",
                    TRENDYOL_BASE_URL, credentials.getSellerId(), startDateMs, endDateMs
            );
            log.info("Testing Claims API date range: {} to {} ({} years back)", startDate, endDate, yearsBack);
            log.info("Request URL: {}", url);

            HttpEntity<String> entity = createHttpEntity(credentials);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            long duration = System.currentTimeMillis() - startTime;

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode content = root.path("content");

                int totalElements = root.path("totalElements").asInt(0);
                int totalPages = root.path("totalPages").asInt(0);
                int claimsInFirstPage = content.isArray() ? content.size() : 0;

                log.info("SUCCESS - {} years back: totalElements={}, totalPages={}, firstPageClaims={}",
                        yearsBack, totalElements, totalPages, claimsInFirstPage);

                return ClaimsDateRangeTestResult.builder()
                        .success(true)
                        .message(String.format("API accepted %d year date range", yearsBack))
                        .startDate(startDate)
                        .endDate(endDate)
                        .yearsBack(yearsBack)
                        .httpStatus(response.getStatusCode().value())
                        .totalElements(totalElements)
                        .totalPages(totalPages)
                        .claimsInFirstPage(claimsInFirstPage)
                        .requestDurationMs(duration)
                        .build();
            } else {
                log.warn("UNEXPECTED - {} years back: status={}", yearsBack, response.getStatusCode());
                return ClaimsDateRangeTestResult.builder()
                        .success(false)
                        .message("Unexpected response status")
                        .startDate(startDate)
                        .endDate(endDate)
                        .yearsBack(yearsBack)
                        .httpStatus(response.getStatusCode().value())
                        .requestDurationMs(duration)
                        .build();
            }

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("HTTP ERROR - {} years back: status={}, body={}", yearsBack, e.getStatusCode(), e.getResponseBodyAsString());

            String errorBody = e.getResponseBodyAsString();
            String errorCode = null;
            String errorMessage = e.getMessage();

            try {
                JsonNode errorJson = objectMapper.readTree(errorBody);
                errorCode = errorJson.path("error").asText(null);
                errorMessage = errorJson.path("message").asText(e.getMessage());
            } catch (Exception ignored) {}

            return ClaimsDateRangeTestResult.builder()
                    .success(false)
                    .message(String.format("API rejected %d year date range", yearsBack))
                    .startDate(startDate)
                    .endDate(endDate)
                    .yearsBack(yearsBack)
                    .httpStatus(e.getStatusCode().value())
                    .errorCode(errorCode)
                    .errorMessage(errorMessage)
                    .requestDurationMs(duration)
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("ERROR - {} years back: {}", yearsBack, e.getMessage());

            return ClaimsDateRangeTestResult.builder()
                    .success(false)
                    .message("Request failed: " + e.getMessage())
                    .startDate(startDate)
                    .endDate(endDate)
                    .yearsBack(yearsBack)
                    .errorMessage(e.getMessage())
                    .requestDurationMs(duration)
                    .build();
        }
    }

    /**
     * Batch test multiple date ranges
     * Tests 1, 2, 3, 4, 5 years back
     */
    public ClaimsDateRangeTestResult testMultipleDateRanges(UUID storeId) {
        List<ClaimsDateRangeTestResult.SingleTestResult> results = new ArrayList<>();
        boolean anySuccess = false;
        int maxSuccessfulYears = 0;

        for (int years = 1; years <= 5; years++) {
            ClaimsDateRangeTestResult singleResult = testDateRange(storeId, years);

            ClaimsDateRangeTestResult.SingleTestResult testResult = ClaimsDateRangeTestResult.SingleTestResult.builder()
                    .yearsBack(years)
                    .startDate(singleResult.getStartDate())
                    .endDate(singleResult.getEndDate())
                    .success(singleResult.isSuccess())
                    .totalElements(singleResult.getTotalElements())
                    .httpStatus(singleResult.getHttpStatus())
                    .errorMessage(singleResult.getErrorMessage())
                    .durationMs(singleResult.getRequestDurationMs())
                    .build();

            results.add(testResult);

            if (singleResult.isSuccess()) {
                anySuccess = true;
                maxSuccessfulYears = years;
            }

            // Small delay between requests to avoid rate limiting
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {}
        }

        String message = anySuccess
                ? String.format("API accepts up to %d year(s) date range", maxSuccessfulYears)
                : "API rejected all date ranges";

        return ClaimsDateRangeTestResult.builder()
                .success(anySuccess)
                .message(message)
                .yearsBack(maxSuccessfulYears)
                .testResults(results)
                .build();
    }

    // Helper methods

    private TrendyolCredentials extractTrendyolCredentials(Store store) {
        MarketplaceCredentials credentials = store.getCredentials();
        if (credentials instanceof TrendyolCredentials) {
            return (TrendyolCredentials) credentials;
        }
        return null;
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

    private <T> HttpEntity<T> createHttpEntityWithBody(TrendyolCredentials credentials, T body) {
        String auth = credentials.getApiKey() + ":" + credentials.getApiSecret();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.set("User-Agent", credentials.getSellerId() + " - SelfIntegration");

        return new HttpEntity<>(body, headers);
    }

    private TrendyolClaim createClaimFromJson(Store store, JsonNode node) {
        List<ClaimItem> items = new ArrayList<>();
        JsonNode itemsNode = node.path("items");
        if (itemsNode.isArray()) {
            for (JsonNode itemNode : itemsNode) {
                items.add(ClaimItem.builder()
                        .claimItemId(itemNode.path("id").asText())
                        .barcode(itemNode.path("barcode").asText(null))
                        .productName(itemNode.path("productName").asText(null))
                        .productSize(itemNode.path("productSize").asText(null))
                        .productColor(itemNode.path("productColor").asText(null))
                        .price(new BigDecimal(itemNode.path("price").asText("0")))
                        .quantity(new BigDecimal(itemNode.path("quantity").asText("1")))
                        .reasonName(itemNode.path("reason").path("name").asText(null))
                        .reasonCode(itemNode.path("reason").path("code").asText(null))
                        .status(itemNode.path("status").asText(null))
                        .customerNote(itemNode.path("customerNote").asText(null))
                        .autoAccepted(itemNode.path("autoAccepted").asBoolean(false))
                        .acceptedBySeller(itemNode.path("acceptedBySeller").asBoolean(false))
                        .imageUrl(itemNode.path("imageUrl").asText(null))
                        .build());
            }
        }

        return TrendyolClaim.builder()
                .store(store)
                // Trendyol API migration: id -> claimId (08.12.2025)
                .claimId(node.has("claimId") ? node.path("claimId").asText() : node.path("id").asText())
                .orderNumber(node.path("orderNumber").asText(null))
                .customerFirstName(node.path("customerFirstName").asText(null))
                .customerLastName(node.path("customerLastName").asText(null))
                .claimDate(parseTimestamp(node.path("claimDate").asLong()))
                .cargoTrackingNumber(node.path("cargoTrackingNumber").asText(null))
                .cargoTrackingLink(node.path("cargoTrackingLink").asText(null))
                .cargoProviderName(node.path("cargoProviderName").asText(null))
                .status(node.path("status").asText("Created"))
                .items(items)
                .lastModifiedDate(parseTimestamp(node.path("lastModifiedDate").asLong()))
                .syncedAt(LocalDateTime.now())
                .build();
    }

    private void updateClaimFromJson(TrendyolClaim claim, JsonNode node) {
        claim.setStatus(node.path("status").asText(claim.getStatus()));
        claim.setCargoTrackingNumber(node.path("cargoTrackingNumber").asText(claim.getCargoTrackingNumber()));
        claim.setCargoTrackingLink(node.path("cargoTrackingLink").asText(claim.getCargoTrackingLink()));
        claim.setLastModifiedDate(parseTimestamp(node.path("lastModifiedDate").asLong()));
        claim.setSyncedAt(LocalDateTime.now());

        // Update items
        List<ClaimItem> items = new ArrayList<>();
        JsonNode itemsNode = node.path("items");
        if (itemsNode.isArray()) {
            for (JsonNode itemNode : itemsNode) {
                items.add(ClaimItem.builder()
                        .claimItemId(itemNode.path("id").asText())
                        .barcode(itemNode.path("barcode").asText(null))
                        .productName(itemNode.path("productName").asText(null))
                        .productSize(itemNode.path("productSize").asText(null))
                        .productColor(itemNode.path("productColor").asText(null))
                        .price(new BigDecimal(itemNode.path("price").asText("0")))
                        .quantity(new BigDecimal(itemNode.path("quantity").asText("1")))
                        .reasonName(itemNode.path("reason").path("name").asText(null))
                        .reasonCode(itemNode.path("reason").path("code").asText(null))
                        .status(itemNode.path("status").asText(null))
                        .customerNote(itemNode.path("customerNote").asText(null))
                        .autoAccepted(itemNode.path("autoAccepted").asBoolean(false))
                        .acceptedBySeller(itemNode.path("acceptedBySeller").asBoolean(false))
                        .imageUrl(itemNode.path("imageUrl").asText(null))
                        .build());
            }
        }
        claim.setItems(items);
    }

    private LocalDateTime parseTimestamp(long timestamp) {
        if (timestamp <= 0) {
            return LocalDateTime.now();
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), TURKEY_ZONE);
    }

    private ClaimDto toClaimDto(TrendyolClaim claim) {
        List<ClaimItemDto> itemDtos = claim.getItems().stream()
                .map(item -> ClaimItemDto.builder()
                        .claimItemId(item.getClaimItemId())
                        .barcode(item.getBarcode())
                        .productName(item.getProductName())
                        .productSize(item.getProductSize())
                        .productColor(item.getProductColor())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .reasonName(item.getReasonName())
                        .reasonCode(item.getReasonCode())
                        .status(item.getStatus())
                        .customerNote(item.getCustomerNote())
                        .autoAccepted(item.getAutoAccepted())
                        .acceptedBySeller(item.getAcceptedBySeller())
                        .imageUrl(item.getImageUrl())
                        .build())
                .collect(Collectors.toList());

        return ClaimDto.builder()
                .id(claim.getId())
                .claimId(claim.getClaimId())
                .orderNumber(claim.getOrderNumber())
                .customerFirstName(claim.getCustomerFirstName())
                .customerLastName(claim.getCustomerLastName())
                .customerFullName(claim.getCustomerFullName())
                .claimDate(claim.getClaimDate())
                .cargoTrackingNumber(claim.getCargoTrackingNumber())
                .cargoTrackingLink(claim.getCargoTrackingLink())
                .cargoProviderName(claim.getCargoProviderName())
                .status(claim.getStatus())
                .items(itemDtos)
                .totalItemCount(claim.getTotalItemCount())
                .lastModifiedDate(claim.getLastModifiedDate())
                .syncedAt(claim.getSyncedAt())
                .createdAt(claim.getCreatedAt())
                .updatedAt(claim.getUpdatedAt())
                .build();
    }
}
