package com.ecommerce.sellerx.financial;

import com.ecommerce.sellerx.common.TrendyolRateLimiter;
import com.ecommerce.sellerx.common.exception.InvalidStoreConfigurationException;
import com.ecommerce.sellerx.common.exception.ResourceNotFoundException;
import com.ecommerce.sellerx.orders.*;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import com.ecommerce.sellerx.stores.MarketplaceCredentials;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.stores.TrendyolCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for fetching historical order data from Trendyol Settlements API.
 *
 * This service is used during store onboarding to fetch ALL orders (2 years ago to today)
 * from Settlements API. During initial sync, this replaces Orders API to avoid duplicates
 * and provides REAL commission rates from the start.
 *
 * IMPORTANT: We fetch data from 2 years ago (not store.createdAt) because:
 * - store.createdAt = when the store was added to SellerX (recent)
 * - Actual Trendyol store creation = could be years ago with historical data
 *
 * Data Flow:
 * - Initial sync: 2 years ago → today: Fetch ALL orders from Settlements API
 * - Scheduled jobs: Use Orders API for new orders (duplicate control prevents conflicts)
 *
 * Settlement data provides:
 * - orderNumber, orderDate, barcode
 * - Real commission rates (not estimated!)
 * - sellerRevenue (net amount after commission)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrendyolHistoricalSettlementService {

    private static final String TRENDYOL_BASE_URL = "https://apigw.trendyol.com";
    private static final String SETTLEMENT_ENDPOINT = "/integration/finance/che/sellers/{sellerId}/settlements";

    // Trendyol API limits
    private static final int CHUNK_DAYS = 14; // Max 15 days per request, use 14 to be safe
    private static final int PAGE_SIZE = 1000;
    private static final int HISTORICAL_MONTHS_BACK = 24; // Go back 2 years to cover all historical data

    private final TrendyolOrderRepository orderRepository;
    private final StoreRepository storeRepository;
    private final TrendyolProductRepository productRepository;
    private final RestTemplate restTemplate;
    private final TrendyolRateLimiter rateLimiter;
    private final HistoricalSyncFailedChunkRepository failedChunkRepository;
    
    private static final int MAX_CHUNK_RETRIES = 5; // Poisonous chunk threshold

    /**
     * Sync historical settlements for a store.
     * This creates TrendyolOrder entities from Settlement data for ALL orders (2 years ago to today).
     * Used during initial sync to fetch all orders with real commission rates.
     *
     * @param storeId The store ID
     * @return HistoricalSyncResult with statistics about the sync operation
     */
    public HistoricalSyncResult syncHistoricalSettlementsForStore(UUID storeId) {
        log.info("Starting historical settlement sync for store: {}", storeId);

        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));

        if (!"trendyol".equalsIgnoreCase(store.getMarketplace())) {
            throw InvalidStoreConfigurationException.notTrendyolStore(storeId.toString());
        }

        // Acquire sync lock to prevent concurrent sync operations
        if (!acquireSyncLock(store)) {
            String errorMsg = String.format("Sync already in progress for store %s (locked by thread: %s)",
                storeId, store.getSyncLockThreadId());
            log.warn(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        try {
            // Refresh store entity to get latest data
            store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));

            TrendyolCredentials credentials = extractTrendyolCredentials(store);
            if (credentials == null || credentials.getSellerId() == null) {
                throw InvalidStoreConfigurationException.missingCredentials(storeId.toString());
            }

            // Create HTTP headers for API calls
            String auth = credentials.getApiKey() + ":" + credentials.getApiSecret();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + encodedAuth);
            headers.set("User-Agent", credentials.getSellerId() + " - SelfIntegration");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Resumable Sync: Check if there's a checkpoint to resume from
            LocalDateTime syncFrom;
            LocalDateTime syncTo = LocalDateTime.now();
            
            if (store.getHistoricalSyncCheckpointDate() != null) {
                // Resume from checkpoint
                syncFrom = store.getHistoricalSyncCheckpointDate().plusDays(1);
                log.info("Resuming historical sync for store {} from checkpoint: {} (previous sync started: {})",
                    storeId, syncFrom, store.getHistoricalSyncStartDate());
            } else {
                // First time sync - find first order date using Binary Search
                LocalDate firstOrderDate = findFirstOrderDate(store, credentials, entity);
                syncFrom = firstOrderDate.atStartOfDay();
                store.setHistoricalSyncStartDate(syncFrom);
                storeRepository.save(store);
                log.info("Starting new historical sync for store {}: {} to {} ({} days) - First order date found: {}",
                    storeId, syncFrom, syncTo, ChronoUnit.DAYS.between(syncFrom, syncTo), firstOrderDate);
            }

            return fetchAndCreateHistoricalOrders(store, credentials, syncFrom, syncTo);
        } catch (Exception e) {
            log.error("Historical sync failed for store {}: {}", storeId, e.getMessage(), e);
            return HistoricalSyncResult.failed(e.getMessage());
        } finally {
            // Always release lock, even if sync fails
            releaseSyncLock(store);
        }
    }

    /**
     * Extract TrendyolCredentials from Store
     */
    private TrendyolCredentials extractTrendyolCredentials(Store store) {
        MarketplaceCredentials credentials = store.getCredentials();
        if (credentials instanceof TrendyolCredentials) {
            return (TrendyolCredentials) credentials;
        }
        return null;
    }

    /**
     * Acquire sync lock for a store to prevent concurrent sync operations.
     * If a lock exists and is older than 2 hours, it's considered stale and will be cleared.
     * 
     * @param store The store to acquire lock for
     * @return true if lock was acquired, false if lock already exists and is not stale
     */
    private boolean acquireSyncLock(Store store) {
        LocalDateTime now = LocalDateTime.now();
        
        // Check if lock exists
        if (store.getSyncLockAcquiredAt() != null) {
            LocalDateTime lockTime = store.getSyncLockAcquiredAt();
            
            // Check if lock is stale (older than 2 hours)
            if (lockTime.isBefore(now.minusHours(2))) {
                log.warn("Clearing stale sync lock for store {} (acquired at: {}, thread: {})",
                    store.getId(), lockTime, store.getSyncLockThreadId());
                releaseSyncLock(store);
            } else {
                // Lock is active
                log.warn("Sync lock already exists for store {} (acquired at: {}, thread: {})",
                    store.getId(), lockTime, store.getSyncLockThreadId());
                return false;
            }
        }
        
        // Acquire lock
        store.setSyncLockAcquiredAt(now);
        store.setSyncLockThreadId(Thread.currentThread().getName());
        storeRepository.save(store);
        
        log.info("Sync lock acquired for store {} by thread {}", store.getId(), store.getSyncLockThreadId());
        return true;
    }

    /**
     * Release sync lock for a store.
     * 
     * @param store The store to release lock for
     */
    private void releaseSyncLock(Store store) {
        if (store.getSyncLockAcquiredAt() != null) {
            log.debug("Releasing sync lock for store {} (was held by thread: {})",
                store.getId(), store.getSyncLockThreadId());
        }
        
        store.setSyncLockAcquiredAt(null);
        store.setSyncLockThreadId(null);
        storeRepository.save(store);
    }

    /**
     * Check if a store has an active sync lock.
     * 
     * @param store The store to check
     * @return true if lock exists and is not stale, false otherwise
     */
    private boolean isSyncLocked(Store store) {
        if (store.getSyncLockAcquiredAt() == null) {
            return false;
        }
        
        LocalDateTime lockTime = store.getSyncLockAcquiredAt();
        // Lock is considered active if it's less than 2 hours old
        return lockTime.isAfter(LocalDateTime.now().minusHours(2));
    }

    /**
     * Find the first order date using Binary Search algorithm.
     * Searches from 1 October 2017 (Trendyol marketplace start) to today.
     * 
     * @param store The store
     * @param credentials Trendyol credentials
     * @param entity HTTP entity for API calls
     * @return LocalDate of the first order, or 1 October 2017 if no orders found
     */
    private LocalDate findFirstOrderDate(Store store, TrendyolCredentials credentials, HttpEntity<String> entity) {
        LocalDate low = LocalDate.of(2017, 10, 1); // Trendyol marketplace start date
        LocalDate high = LocalDate.now();
        
        log.info("Starting Binary Search for first order date for store {}: range {} to {}", 
            store.getId(), low, high);
        
        int step = 0;
        while (ChronoUnit.DAYS.between(low, high) >= 15) {
            step++;
            long daysBetween = ChronoUnit.DAYS.between(low, high);
            LocalDate mid = low.plusDays(daysBetween / 2);
            LocalDate midEnd = mid.plusDays(14); // 15 days inclusive (day 0 to day 14 = 15 days)

            log.debug("Binary Search step {}: low={}, high={}, mid={}, checking range {} to {}",
                step, low, high, mid, mid, midEnd);
            
            boolean dataExists = checkDataExists(store, credentials, entity, mid, midEnd);
            
            String result = dataExists ? "VAR" : "YOK";
            log.info("[BINARY-SEARCH] [ADIM {}] {} - {} taranıyor. Orta Nokta: {} -> Sonuç: {}", 
                step, low, high, mid, result);
            
            if (dataExists) {
                // Data exists in this window, first order is at or before mid
                high = mid;
                log.debug("Step {}: Data EXISTS → adjusting high to {}", step, high);
            } else {
                // No data in this window, first order is after mid+14 (the 15-day window)
                low = mid.plusDays(15); // Skip past the checked 15-day window
                log.debug("Step {}: Data NOT EXISTS → adjusting low to {}", step, low);
            }
        }
        
        log.info("[BINARY-SEARCH] [SUCCESS] Mağaza doğum günü tespit edildi: {} (Toplam {} adımda bulundu).", 
            low, step);
        log.info("Binary Search completed for store {} in {} steps. First order date: {}", 
            store.getId(), step, low);
        
        // Validation: Check if there's data before the found date (handles data gaps)
        // This prevents missing orders if the store had inactive periods
        LocalDate validationStart = low.minusDays(30);
        if (checkDataExists(store, credentials, entity, validationStart, low)) {
            log.warn("Found data before firstOrderDate ({}), refining search in range {} to {}",
                low, validationStart, low);
            // Refine search in the smaller range
            int refinementSteps = 0;
            LocalDate refinedDate = refineFirstOrderDate(store, credentials, entity, validationStart, low);
            if (!refinedDate.equals(low)) {
                refinementSteps = (int) Math.ceil(Math.log(30.0 / 15.0) / Math.log(2.0));
            }
            low = refinedDate;
            log.info("[BINARY-SEARCH] [REFINED] Doğrulama sonrası güncellenen tarih: {} ({} ek adım)", 
                low, refinementSteps);
            log.info("Refined first order date for store {}: {}", store.getId(), low);
        }
        
        return low;
    }

    /**
     * Refine the first order date search in a smaller date range.
     * Used when validation detects data before the initially found date.
     * 
     * @param store The store
     * @param credentials Trendyol credentials
     * @param entity HTTP entity for API calls
     * @param searchStart Start date for refined search
     * @param searchEnd End date for refined search
     * @return LocalDate of the first order in the refined range
     */
    private LocalDate refineFirstOrderDate(Store store, TrendyolCredentials credentials, HttpEntity<String> entity,
                                          LocalDate searchStart, LocalDate searchEnd) {
        LocalDate low = searchStart;
        LocalDate high = searchEnd;
        
        log.debug("Refining first order date search for store {}: range {} to {}",
            store.getId(), low, high);
        
        int step = 0;
        while (ChronoUnit.DAYS.between(low, high) >= 15) {
            step++;
            long daysBetween = ChronoUnit.DAYS.between(low, high);
            LocalDate mid = low.plusDays(daysBetween / 2);
            LocalDate midEnd = mid.plusDays(14); // 15 days inclusive (day 0 to day 14 = 15 days)

            log.debug("Refinement step {}: low={}, high={}, mid={}, checking range {} to {}",
                step, low, high, mid, mid, midEnd);

            boolean dataExists = checkDataExists(store, credentials, entity, mid, midEnd);

            if (dataExists) {
                high = mid;
                log.debug("Refinement step {}: Data EXISTS → adjusting high to {}", step, high);
            } else {
                low = mid.plusDays(15); // Skip past the checked 15-day window
                log.debug("Refinement step {}: Data NOT EXISTS → adjusting low to {}", step, low);
            }
        }
        
        log.info("Refinement completed for store {} in {} steps. Refined first order date: {}",
            store.getId(), step, low);
        
        return low;
    }

    /**
     * Check if settlement data exists for the given date range.
     * Checks all transaction types (Sale, Return, Discount, Coupon) to ensure
     * we don't miss the first order if it's a return or discount transaction.
     * 
     * @param store The store
     * @param credentials Trendyol credentials
     * @param entity HTTP entity for API calls
     * @param start Start date (inclusive)
     * @param end End date (inclusive, but limited to 15 days from start)
     * @return true if data exists in any transaction type, false otherwise
     */
    private boolean checkDataExists(Store store, TrendyolCredentials credentials, HttpEntity<String> entity,
                                   LocalDate start, LocalDate end) {
        // Check all transaction types to ensure we don't miss the first order
        // (e.g., if first transaction is a Return or Discount)
        String[] transactionTypes = {"Sale", "Return", "Discount", "Coupon", "EarlyPayment"};
        
        for (String transactionType : transactionTypes) {
            if (checkTransactionTypeExists(store, credentials, entity, start, end, transactionType)) {
                return true; // Data exists in at least one transaction type
            }
        }
        
        return false; // No data found in any transaction type
    }

    /**
     * Check if settlement data exists for a specific transaction type in the given date range.
     * 
     * @param store The store
     * @param credentials Trendyol credentials
     * @param entity HTTP entity for API calls
     * @param start Start date (inclusive)
     * @param end End date (inclusive, but limited to 15 days from start)
     * @param transactionType Transaction type to check (Sale, Return, Discount, Coupon)
     * @return true if data exists for this transaction type, false otherwise
     */
    private boolean checkTransactionTypeExists(Store store, TrendyolCredentials credentials, HttpEntity<String> entity,
                                              LocalDate start, LocalDate end, String transactionType) {
        try {
            // Ensure end date is not more than 15 days from start (API limit: max 15 days inclusive)
            LocalDate actualEnd = end.isAfter(start.plusDays(14)) ? start.plusDays(14) : end;
            
            long startTimestamp = start.atStartOfDay()
                .atZone(ZoneId.of("Europe/Istanbul"))
                .toInstant()
                .toEpochMilli();
            long endTimestamp = actualEnd.atTime(23, 59, 59)
                .atZone(ZoneId.of("Europe/Istanbul"))
                .toInstant()
                .toEpochMilli();
            
            String url = TRENDYOL_BASE_URL + SETTLEMENT_ENDPOINT +
                        "?transactionType=" + transactionType +
                        "&startDate=" + startTimestamp +
                        "&endDate=" + endTimestamp +
                        "&page=0" +
                        "&size=1000"; // Trendyol API requires specific size values (1000 works like Financial service)
            
            // Use retry mechanism for resilience against timeouts and transient errors
            ResponseEntity<TrendyolFinancialSettlementResponse> response =
                fetchSettlementWithRetry(url, entity, credentials.getSellerId().toString(), 3, store.getId());

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                TrendyolFinancialSettlementResponse settlementResponse = response.getBody();

                // Check if data exists
                boolean hasData = (settlementResponse.getTotalElements() != null &&
                                 settlementResponse.getTotalElements() > 0) ||
                                (settlementResponse.getContent() != null &&
                                 !settlementResponse.getContent().isEmpty());

                log.info("[BINARY-CHECK] {} type for {} to {}: totalElements={}, hasData={}",
                    transactionType, start, end,
                    settlementResponse.getTotalElements(), hasData);

                return hasData;
            }

            log.warn("[BINARY-CHECK] Non-OK response for {} type in {} to {}: status={}",
                transactionType, start, end, response.getStatusCode());
            return false;
            
        } catch (Exception e) {
            log.debug("Error checking {} transaction type for store {} in range {} to {}: {}", 
                transactionType, store.getId(), start, end, e.getMessage());
            return false;
        }
    }

    /**
     * Fetch settlement data with retry mechanism for handling timeouts and transient errors.
     * Uses exponential backoff strategy.
     * 
     * @param url The API URL
     * @param entity HTTP entity with headers
     * @param sellerId Trendyol seller ID
     * @param maxRetries Maximum number of retry attempts (default: 3)
     * @return ResponseEntity with settlement data
     * @throws RuntimeException if max retries exceeded
     */
    private ResponseEntity<TrendyolFinancialSettlementResponse> fetchSettlementWithRetry(
            String url, HttpEntity<String> entity, String sellerId, int maxRetries, UUID storeId) {

        int retryCount = 0;
        Exception lastException = null;

        while (retryCount < maxRetries) {
            try {
                rateLimiter.acquire(storeId);
                
                ResponseEntity<TrendyolFinancialSettlementResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    TrendyolFinancialSettlementResponse.class,
                    sellerId
                );
                
                // Success - return response
                return response;
                
            } catch (HttpClientErrorException.Unauthorized e) {
                // 401 Unauthorized - token might be expired
                lastException = e;
                log.warn("Unauthorized (401) for seller {} on attempt {}/{}. This may indicate expired credentials.",
                    sellerId, retryCount + 1, maxRetries);
                
                if (retryCount < maxRetries - 1) {
                    retryCount++;
                    long sleepTime = 1000L * retryCount; // Exponential backoff: 1s, 2s, 3s
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                } else {
                    break; // Max retries reached
                }
                
            } catch (ResourceAccessException e) {
                // Connection timeout or network error
                lastException = e;
                log.warn("Connection timeout/error for seller {} on attempt {}/{}. Retrying...",
                    sellerId, retryCount + 1, maxRetries);
                
                if (retryCount < maxRetries - 1) {
                    retryCount++;
                    long sleepTime = 1000L * retryCount; // Exponential backoff: 1s, 2s, 3s
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                } else {
                    break; // Max retries reached
                }
                
            } catch (HttpServerErrorException e) {
                // 5xx server errors - retry
                lastException = e;
                log.warn("Server error ({} {}) for seller {} on attempt {}/{}. Retrying...",
                    e.getStatusCode().value(), e.getStatusCode().toString(), sellerId, retryCount + 1, maxRetries);
                
                if (retryCount < maxRetries - 1) {
                    retryCount++;
                    long sleepTime = 1000L * retryCount; // Exponential backoff: 1s, 2s, 3s
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                } else {
                    break; // Max retries reached
                }
                
            } catch (Exception e) {
                // Other exceptions - don't retry, throw immediately
                log.error("Unexpected error fetching settlement data for seller {}: {}", sellerId, e.getMessage());
                throw new RuntimeException("Failed to fetch settlement data", e);
            }
        }
        
        // Max retries exceeded
        String errorMsg = String.format("Max retries (%d) exceeded for seller %s. Last error: %s",
            maxRetries, sellerId, lastException != null ? lastException.getMessage() : "Unknown");
        log.error(errorMsg);
        throw new RuntimeException(errorMsg, lastException);
    }

    /**
     * Fetch settlements in chunks and create historical orders
     */
    private HistoricalSyncResult fetchAndCreateHistoricalOrders(
            Store store,
            TrendyolCredentials credentials,
            LocalDateTime syncFrom,
            LocalDateTime syncTo) {

        int totalChunks = 0;
        int failedChunks = 0;
        int skippedChunks = 0; // Poisonous chunks
        int ordersCreated = 0;
        int settlementsProcessed = 0;

        // Create HTTP headers (same pattern as TrendyolFinancialSettlementService)
        String auth = credentials.getApiKey() + ":" + credentials.getApiSecret();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.set("User-Agent", credentials.getSellerId() + " - SelfIntegration");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Calculate total chunks for progress tracking
        long totalDays = ChronoUnit.DAYS.between(syncFrom, syncTo);
        int estimatedTotalChunks = (int) Math.ceil((double) totalDays / CHUNK_DAYS);
        
        // Initialize progress tracking if not already set
        if (store.getHistoricalSyncTotalChunks() == null || store.getHistoricalSyncTotalChunks() == 0) {
            store.setHistoricalSyncTotalChunks(estimatedTotalChunks);
            store.setHistoricalSyncCompletedChunks(0);
        }
        if (store.getHistoricalSyncStartDate() == null) {
            store.setHistoricalSyncStartDate(syncFrom);
        }
        storeRepository.save(store);
        
        log.info("[HISTORICAL] {} paketlik büyük senkronizasyon başladı. Hedef: {} -> {}.", 
                estimatedTotalChunks, syncFrom.toLocalDate(), syncTo.toLocalDate());

        // Process in CHUNK_DAYS intervals - Memory efficient: save each chunk immediately
        LocalDateTime currentStart = syncFrom;

        while (currentStart.isBefore(syncTo)) {
            LocalDateTime currentEnd = currentStart.plusDays(CHUNK_DAYS);
            if (currentEnd.isAfter(syncTo)) {
                currentEnd = syncTo;
            }

            totalChunks++;
            
            // Update current processing date for frontend progress
            store.setHistoricalSyncCurrentProcessingDate(currentStart);
            storeRepository.save(store);
            
            log.info("Processing chunk {}/{} for store {}: {} to {}",
                totalChunks, estimatedTotalChunks, store.getId(), currentStart, currentEnd);

            // Poisonous Chunk Protection: Check if this chunk is permanently failed
            Optional<HistoricalSyncFailedChunk> failedChunk = failedChunkRepository
                .findByStoreIdAndChunkStartDateAndChunkEndDate(
                    store.getId(), currentStart, currentEnd);
            
            if (failedChunk.isPresent() && failedChunk.get().getRetryCount() >= MAX_CHUNK_RETRIES) {
                skippedChunks++;
                log.warn("[KRİTİK] {} paketi 5 kez başarısız oldu. 'failed_chunks' tablosuna taşındı, sonraki pakete zıplanıyor.",
                    currentStart.toLocalDate());
                log.warn("Skipping permanently failed chunk for store {}: {} to {} (retry count: {})",
                    store.getId(), currentStart, currentEnd, failedChunk.get().getRetryCount());
                currentStart = currentEnd;
                continue;
            }

            // Each chunk uses its own Map to avoid memory issues
            Map<String, List<TrendyolFinancialSettlementItem>> chunkSettlementsByOrder = new HashMap<>();

            try {
                // Fetch all transaction types for this chunk
                int chunkSettlements = fetchSettlementsForChunk(
                    store, credentials, entity, currentStart, currentEnd, chunkSettlementsByOrder);
                settlementsProcessed += chunkSettlements;

                // Immediately create and save orders from this chunk (memory efficient)
                int chunkOrders = createOrdersFromSettlements(store, chunkSettlementsByOrder);
                ordersCreated += chunkOrders;

                // Flush to database and clear memory
                orderRepository.flush();
                chunkSettlementsByOrder.clear();

                // SUCCESS: Update checkpoint and progress
                store.setHistoricalSyncCheckpointDate(currentEnd);
                int completedChunks = (store.getHistoricalSyncCompletedChunks() != null ? 
                     store.getHistoricalSyncCompletedChunks() : 0) + 1;
                store.setHistoricalSyncCompletedChunks(completedChunks);
                storeRepository.save(store);
                
                // Calculate percentage
                double percentage = estimatedTotalChunks > 0 ? 
                    ((double) completedChunks / estimatedTotalChunks) * 100 : 0;
                
                // Remove failed chunk record if it exists (chunk succeeded after retry)
                failedChunk.ifPresent(failedChunkRepository::delete);
                
                log.info("[HISTORICAL] [%{}] tamamlandı. Paket {}/{} bitti. Şu anki odak: {}. Bu pakette {} sipariş işlendi.",
                    String.format("%.1f", percentage), completedChunks, estimatedTotalChunks, 
                    currentStart.toLocalDate(), chunkOrders);
                log.info("Chunk completed for store {}: {} to {} (orders: {})",
                    store.getId(), currentStart, currentEnd, chunkOrders);

                // Respect rate limiting between chunks (per-store)
                rateLimiter.acquire(store.getId());
                Thread.sleep(300);

            } catch (Exception e) {
                failedChunks++;
                String errorMessage = e.getMessage();
                
                // Get retry count for this chunk
                int retryCount = failedChunk.map(HistoricalSyncFailedChunk::getRetryCount).orElse(0) + 1;
                int maxRetries = 3; // fetchSettlementWithRetry uses 3 retries
                
                if (retryCount <= maxRetries) {
                    log.warn("[UYARI] {} paketinde hata! Deneme {}/{} yapılıyor... Sebep: {}",
                        currentStart.toLocalDate(), retryCount, maxRetries, errorMessage);
                }
                
                log.error("Failed to fetch historical settlements for store {} in chunk {} - {}: {}",
                    store.getId(), currentStart, currentEnd, errorMessage);

                // Track failed chunk (Poisonous Chunk Protection)
                if (failedChunk.isPresent()) {
                    // Increment retry count
                    HistoricalSyncFailedChunk chunk = failedChunk.get();
                    chunk.setRetryCount(chunk.getRetryCount() + 1);
                    chunk.setLastErrorMessage(errorMessage);
                    chunk.setFailedAt(LocalDateTime.now());
                    failedChunkRepository.save(chunk);
                    
                    log.warn("Chunk retry count increased to {} for store {}: {} to {}",
                        chunk.getRetryCount(), store.getId(), currentStart, currentEnd);
                } else {
                    // Create new failed chunk record
                    HistoricalSyncFailedChunk newFailedChunk = HistoricalSyncFailedChunk.builder()
                        .storeId(store.getId())
                        .chunkStartDate(currentStart)
                        .chunkEndDate(currentEnd)
                        .retryCount(1)
                        .lastErrorMessage(errorMessage)
                        .failedAt(LocalDateTime.now())
                        .build();
                    failedChunkRepository.save(newFailedChunk);
                }
            }

            currentStart = currentEnd;
        }

        // Determine final status
        String status;
        if (failedChunks == 0 && skippedChunks == 0) {
            status = "COMPLETED";
            // Clear checkpoint on completion
            store.setHistoricalSyncCheckpointDate(null);
            store.setHistoricalSyncCurrentProcessingDate(null);
            // Clean up failed chunks
            failedChunkRepository.deleteByStoreId(store.getId());
        } else if (failedChunks + skippedChunks < totalChunks) {
            status = "PARTIAL";
        } else {
            status = "FAILED";
        }

        HistoricalSyncResult result = HistoricalSyncResult.builder()
            .status(status)
            .ordersCreated(ordersCreated)
            .settlementsProcessed(settlementsProcessed)
            .failedChunks(failedChunks)
            .skippedChunks(skippedChunks)
            .totalChunks(totalChunks)
            .syncedFrom(syncFrom)
            .syncedTo(syncTo)
            .build();

        if (status.equals("COMPLETED")) {
            log.info("[HISTORICAL] [COMPLETED] Tüm geçmiş siparişler çekildi. Toplam: {} sipariş, {} paket.",
                ordersCreated, totalChunks);
        }
        
        log.info("Historical sync completed for store {}: status={}, orders={}, settlements={}, chunks={}/{}, failed={}, skipped={}",
            store.getId(), status, ordersCreated, settlementsProcessed, 
            store.getHistoricalSyncCompletedChunks(), totalChunks, failedChunks, skippedChunks);

        return result;
    }

    /**
     * Fetch settlements for a single chunk (date range)
     */
    private int fetchSettlementsForChunk(
            Store store,
            TrendyolCredentials credentials,
            HttpEntity<String> entity,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Map<String, List<TrendyolFinancialSettlementItem>> allSettlementsByOrder) {

        long startTimestamp = startDate.atZone(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();
        long endTimestamp = endDate.atZone(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();

        String[] transactionTypes = {"Sale", "Return", "Discount", "Coupon", "EarlyPayment"};
        int totalSettlements = 0;

        for (String transactionType : transactionTypes) {
            int typeSettlements = fetchSettlementsByType(
                store, credentials, entity, startTimestamp, endTimestamp, transactionType, allSettlementsByOrder);
            totalSettlements += typeSettlements;

            // Small delay between transaction types
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return totalSettlements;
    }

    /**
     * Fetch settlements for a specific transaction type with pagination
     */
    private int fetchSettlementsByType(
            Store store,
            TrendyolCredentials credentials,
            HttpEntity<String> entity,
            long startTimestamp,
            long endTimestamp,
            String transactionType,
            Map<String, List<TrendyolFinancialSettlementItem>> allSettlementsByOrder) {

        int currentPage = 0;
        int totalPages = 1;
        int totalProcessed = 0;

        while (currentPage < totalPages) {
            String url = TRENDYOL_BASE_URL + SETTLEMENT_ENDPOINT +
                        "?transactionType=" + transactionType +
                        "&startDate=" + startTimestamp +
                        "&endDate=" + endTimestamp +
                        "&page=" + currentPage +
                        "&size=" + PAGE_SIZE;

            try {
                // Use retry mechanism for resilience against timeouts and transient errors
                ResponseEntity<TrendyolFinancialSettlementResponse> response =
                    fetchSettlementWithRetry(url, entity, credentials.getSellerId().toString(), 3, store.getId());

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    TrendyolFinancialSettlementResponse settlementResponse = response.getBody();

                    // Update totalPages from first response
                    if (currentPage == 0) {
                        totalPages = settlementResponse.getTotalPages() != null ?
                                   settlementResponse.getTotalPages() : 1;
                    }

                    // Process settlements
                    if (settlementResponse.getContent() != null && !settlementResponse.getContent().isEmpty()) {
                        for (TrendyolFinancialSettlementItem item : settlementResponse.getContent()) {
                            // Skip if missing required fields
                            if (item.getOrderNumber() == null || item.getShipmentPackageId() == null) {
                                continue;
                            }

                            String key = item.getOrderNumber() + "_" + item.getShipmentPackageId();
                            allSettlementsByOrder.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
                            totalProcessed++;
                        }
                    }
                } else {
                    log.warn("Failed to fetch {} settlements for store {}: Status {}",
                        transactionType, store.getId(), response.getStatusCode());
                    break;
                }

                currentPage++;

                if (currentPage < totalPages) {
                    Thread.sleep(100);
                }

            } catch (Exception e) {
                log.error("Error fetching {} settlements page {} for store {}: {}",
                    transactionType, currentPage, store.getId(), e.getMessage());
                break;
            }
        }

        return totalProcessed;
    }

    /**
     * Create TrendyolOrder entities from grouped settlement data
     */
    private int createOrdersFromSettlements(
            Store store,
            Map<String, List<TrendyolFinancialSettlementItem>> settlementsByOrder) {

        int ordersCreated = 0;

        for (Map.Entry<String, List<TrendyolFinancialSettlementItem>> entry : settlementsByOrder.entrySet()) {
            String[] parts = entry.getKey().split("_");
            String orderNumber = parts[0];
            Long packageId = Long.valueOf(parts[1]);
            List<TrendyolFinancialSettlementItem> settlements = entry.getValue();

            try {
                // Check if order already exists
                Optional<TrendyolOrder> existingOrder = orderRepository
                    .findByTyOrderNumberAndPackageNoAndStore(orderNumber, packageId, store);

                if (existingOrder.isPresent()) {
                    TrendyolOrder order = existingOrder.get();
                    
                    // Race Condition Protection: If order came from webhook/Orders API (has operational data),
                    // only merge financial data. Otherwise, update completely.
                    if ("ORDER_API".equals(order.getDataSource()) || 
                        order.getShipmentCity() != null) {
                        // Webhook/Orders API'den gelmiş - detaylı operasyonel veriler var
                        // Sadece finansal verileri merge et, operasyonel verileri koru
                        mergeFinancialDataOnly(order, settlements);
                        orderRepository.save(order);
                        log.debug("Merged financial data into existing webhook/Orders API order: orderNumber={}, packageId={}",
                            orderNumber, packageId);
                    } else {
                        // SETTLEMENT_API'den gelmiş veya başka kaynak - tamamen güncelle
                        updateOrderFromSettlements(order, settlements);
                        orderRepository.save(order);
                        log.debug("Updated existing order from settlements: orderNumber={}, packageId={}",
                            orderNumber, packageId);
                    }
                    continue; // Order already processed
                }

                // Create new order from settlement data
                TrendyolOrder order = createOrderFromSettlements(store, orderNumber, packageId, settlements);
                if (order != null) {
                    orderRepository.save(order);
                    ordersCreated++;

                    log.debug("Created historical order: orderNumber={}, packageId={}, items={}",
                        orderNumber, packageId, order.getOrderItems().size());
                }

            } catch (Exception e) {
                log.error("Failed to create order from settlements: orderNumber={}, packageId={}: {}",
                    orderNumber, packageId, e.getMessage());
            }
        }

        log.info("Created {} historical orders for store {} from {} settlement groups",
            ordersCreated, store.getId(), settlementsByOrder.size());

        return ordersCreated;
    }

    /**
     * Create a TrendyolOrder entity from settlement items
     */
    private TrendyolOrder createOrderFromSettlements(
            Store store,
            String orderNumber,
            Long packageId,
            List<TrendyolFinancialSettlementItem> settlements) {

        if (settlements.isEmpty()) {
            return null;
        }

        // Separate settlements by type
        List<TrendyolFinancialSettlementItem> sales = new ArrayList<>();
        List<TrendyolFinancialSettlementItem> returns = new ArrayList<>();
        List<TrendyolFinancialSettlementItem> discounts = new ArrayList<>();
        List<TrendyolFinancialSettlementItem> coupons = new ArrayList<>();
        List<TrendyolFinancialSettlementItem> earlyPayments = new ArrayList<>();

        for (TrendyolFinancialSettlementItem item : settlements) {
            String type = item.getTransactionType();
            if ("Satış".equals(type) || "Sale".equals(type)) {
                sales.add(item);
            } else if ("İade".equals(type) || "Return".equals(type)) {
                returns.add(item);
            } else if ("İndirim".equals(type) || "Discount".equals(type)) {
                discounts.add(item);
            } else if ("Kupon".equals(type) || "Coupon".equals(type)) {
                coupons.add(item);
            } else if ("EarlyPayment".equals(type) || "ErkenOdeme".equals(type)) {
                earlyPayments.add(item);
            }
        }

        // Need at least one sale to create an order
        if (sales.isEmpty()) {
            log.debug("No sale settlements for order {} package {}, skipping", orderNumber, packageId);
            return null;
        }

        // Get order date from first sale
        TrendyolFinancialSettlementItem firstSale = sales.get(0);
        LocalDateTime orderDate = convertTimestampToLocalDateTime(firstSale.getOrderDate());
        if (orderDate == null) {
            orderDate = convertTimestampToLocalDateTime(firstSale.getTransactionDate());
        }

        // Calculate gross amount (sum of all sale credits)
        BigDecimal grossAmount = sales.stream()
            .map(TrendyolFinancialSettlementItem::getCredit)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate total discount
        BigDecimal totalDiscount = discounts.stream()
            .map(TrendyolFinancialSettlementItem::getDebt)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate total coupon discount (treated as TY discount)
        BigDecimal totalTyDiscount = coupons.stream()
            .map(TrendyolFinancialSettlementItem::getDebt)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate coupon discount amount (for couponDiscount field)
        BigDecimal couponDiscount = coupons.stream()
            .map(TrendyolFinancialSettlementItem::getDebt)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate early payment fee
        BigDecimal earlyPaymentFee = earlyPayments.stream()
            .map(TrendyolFinancialSettlementItem::getDebt)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate total commission
        BigDecimal totalCommission = sales.stream()
            .map(TrendyolFinancialSettlementItem::getCommissionAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Create order items from sales (grouped by barcode)
        List<OrderItem> orderItems = createOrderItemsFromSales(store, sales);

        // Determine order status based on returns
        String status = returns.isEmpty() ? "Delivered" : "PartiallyReturned";
        if (returns.size() >= sales.size()) {
            status = "Returned";
        }

        // Build the order
        TrendyolOrder order = TrendyolOrder.builder()
            .store(store)
            .tyOrderNumber(orderNumber)
            .packageNo(packageId)
            .orderDate(orderDate)
            .grossAmount(grossAmount)
            .totalDiscount(totalDiscount)
            .totalTyDiscount(totalTyDiscount)
            .couponDiscount(couponDiscount)
            .earlyPaymentFee(earlyPaymentFee)
            .orderItems(orderItems)
            .status(status)
            .shipmentPackageStatus("Delivered") // Historical orders are delivered
            .totalPrice(grossAmount.subtract(totalDiscount).subtract(totalTyDiscount))
            .estimatedCommission(totalCommission)
            .isCommissionEstimated(false) // Commission is REAL from Settlement API!
            .transactionStatus("SETTLED")
            .transactionDate(convertTimestampToLocalDateTime(firstSale.getTransactionDate()))
            .dataSource("SETTLEMENT_API") // Mark as historical data
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        // Update product commission rates from this data
        updateProductCommissionRates(store, sales);

        return order;
    }

    /**
     * Create OrderItem list from sale settlements
     */
    private List<OrderItem> createOrderItemsFromSales(Store store, List<TrendyolFinancialSettlementItem> sales) {
        // Group sales by barcode
        Map<String, List<TrendyolFinancialSettlementItem>> salesByBarcode = sales.stream()
            .collect(Collectors.groupingBy(TrendyolFinancialSettlementItem::getBarcode));

        List<OrderItem> orderItems = new ArrayList<>();

        for (Map.Entry<String, List<TrendyolFinancialSettlementItem>> entry : salesByBarcode.entrySet()) {
            String barcode = entry.getKey();
            List<TrendyolFinancialSettlementItem> barcodeSales = entry.getValue();

            // Calculate totals for this barcode
            int quantity = barcodeSales.size();
            BigDecimal totalPrice = barcodeSales.stream()
                .map(TrendyolFinancialSettlementItem::getCredit)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal unitPrice = quantity > 0 ?
                totalPrice.divide(BigDecimal.valueOf(quantity), 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;

            // Get commission rate from first sale
            BigDecimal commissionRate = barcodeSales.get(0).getCommissionRate();

            // Try to find product info from database
            String productName = "Unknown Product";

            Optional<TrendyolProduct> product = productRepository.findByStoreIdAndBarcode(store.getId(), barcode);
            if (product.isPresent()) {
                productName = product.get().getTitle();
            }

            OrderItem orderItem = OrderItem.builder()
                .barcode(barcode)
                .productName(productName)
                .quantity(quantity)
                .price(unitPrice)
                .vatBaseAmount(totalPrice) // Settlement credit is usually VAT inclusive
                .estimatedCommissionRate(commissionRate)
                .build();

            orderItems.add(orderItem);
        }

        return orderItems;
    }

    /**
     * Update product commission rates from settlement data
     */
    private void updateProductCommissionRates(Store store, List<TrendyolFinancialSettlementItem> settlements) {
        // Group by barcode and find most recent
        Map<String, TrendyolFinancialSettlementItem> latestByBarcode = new HashMap<>();

        for (TrendyolFinancialSettlementItem item : settlements) {
            if (item.getBarcode() == null || item.getCommissionRate() == null) {
                continue;
            }

            String barcode = item.getBarcode();
            TrendyolFinancialSettlementItem existing = latestByBarcode.get(barcode);

            if (existing == null ||
                (item.getTransactionDate() != null &&
                 (existing.getTransactionDate() == null || item.getTransactionDate() > existing.getTransactionDate()))) {
                latestByBarcode.put(barcode, item);
            }
        }

        // Update products
        List<TrendyolProduct> productsToUpdate = new ArrayList<>();

        for (Map.Entry<String, TrendyolFinancialSettlementItem> entry : latestByBarcode.entrySet()) {
            String barcode = entry.getKey();
            TrendyolFinancialSettlementItem settlement = entry.getValue();

            Optional<TrendyolProduct> productOpt = productRepository.findByStoreIdAndBarcode(store.getId(), barcode);
            if (productOpt.isPresent()) {
                TrendyolProduct product = productOpt.get();
                LocalDateTime settlementDate = convertTimestampToLocalDateTime(settlement.getTransactionDate());

                // Only update if this is more recent or if no commission date exists
                if (product.getLastCommissionDate() == null ||
                    (settlementDate != null && settlementDate.isAfter(product.getLastCommissionDate()))) {

                    product.setLastCommissionRate(settlement.getCommissionRate());
                    product.setLastCommissionDate(settlementDate);
                    productsToUpdate.add(product);
                }
            }
        }

        if (!productsToUpdate.isEmpty()) {
            productRepository.saveAll(productsToUpdate);
            log.debug("Updated commission rates for {} products from historical settlements", productsToUpdate.size());
        }
    }

    /**
     * Converts milliseconds timestamp to LocalDateTime
     */
    private LocalDateTime convertTimestampToLocalDateTime(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.of("Europe/Istanbul"))
                .toLocalDateTime();
    }

    /*
     * BINARY SEARCH ALGORITHM SIMULATION - Haziran 2019 Senaryosu
     * 
     * Senaryo: Mağaza Haziran 2019'da açıldı (ilk sipariş: 15 Haziran 2019)
     * Arama aralığı: 1 Ekim 2017 - 20 Ocak 2025 (bugün)
     * 
     * Adım 1: low=2017-10-01, high=2025-01-20, daysBetween=2682
     *         mid = 2017-10-01 + (2682/2) = 2017-10-01 + 1341 = 2021-05-25
     *         Sorgu: 2021-05-25 to 2021-06-09 (15 gün)
     *         Sonuç: Veri VAR (mağaza 2021'de aktif)
     *         → high = 2021-05-25
     * 
     * Adım 2: low=2017-10-01, high=2021-05-25, daysBetween=1332
     *         mid = 2017-10-01 + (1332/2) = 2017-10-01 + 666 = 2019-07-13
     *         Sorgu: 2019-07-13 to 2019-07-28 (15 gün)
     *         Sonuç: Veri VAR (mağaza Haziran 2019'da açıldı, Temmuz'da sipariş var)
     *         → high = 2019-07-13
     * 
     * Adım 3: low=2017-10-01, high=2019-07-13, daysBetween=650
     *         mid = 2017-10-01 + (650/2) = 2017-10-01 + 325 = 2018-08-22
     *         Sorgu: 2018-08-22 to 2018-09-06 (15 gün)
     *         Sonuç: Veri YOK (mağaza henüz açılmamış)
     *         → low = 2018-09-06 (mid + 16 gün)
     * 
     * Adım 4: low=2018-09-06, high=2019-07-13, daysBetween=310
     *         mid = 2018-09-06 + (310/2) = 2018-09-06 + 155 = 2019-02-08
     *         Sorgu: 2019-02-08 to 2019-02-23 (15 gün)
     *         Sonuç: Veri YOK
     *         → low = 2019-02-24
     * 
     * Adım 5: low=2019-02-24, high=2019-07-13, daysBetween=139
     *         mid = 2019-02-24 + (139/2) = 2019-02-24 + 69 = 2019-05-04
     *         Sorgu: 2019-05-04 to 2019-05-19 (15 gün)
     *         Sonuç: Veri YOK
     *         → low = 2019-05-20
     * 
     * Adım 6: low=2019-05-20, high=2019-07-13, daysBetween=54
     *         mid = 2019-05-20 + (54/2) = 2019-05-20 + 27 = 2019-06-16
     *         Sorgu: 2019-06-16 to 2019-07-01 (15 gün)
     *         Sonuç: Veri VAR (ilk sipariş 15 Haziran'da, 16 Haziran'da da sipariş var)
     *         → high = 2019-06-16
     * 
     * Adım 7: low=2019-05-20, high=2019-06-16, daysBetween=27
     *         mid = 2019-05-20 + (27/2) = 2019-05-20 + 13 = 2019-06-02
     *         Sorgu: 2019-06-02 to 2019-06-17 (15 gün)
     *         Sonuç: Veri VAR (15 Haziran'da sipariş var)
     *         → high = 2019-06-02
     * 
     * Adım 8: low=2019-05-20, high=2019-06-02, daysBetween=13
     *         daysBetween < 15 → DÖNGÜ BİTER
     * 
     * Sonuç: firstOrderDate = low = 2019-05-20
     * 
     * Toplam API İsteği: 7 adım (planlanan maksimum 12'nin altında)
     * 
     * Not: Gerçek ilk sipariş 15 Haziran 2019, ama algoritma 15 günlük pencere limiti nedeniyle
     * 2019-05-20'yi bulur. Bu yeterlidir çünkü 15 Haziran'dan önceki 15 günlük pencerede
     * veri yok, bu yüzden algoritma doğru çalışır.
     */

    /**
     * Merge only financial data from settlements into an existing order.
     * Used when order already has operational data from webhook/Orders API.
     * Preserves operational fields (address, phone, cargo tracking) while updating financial data.
     * 
     * @param order Existing order with operational data
     * @param settlements Settlement items to merge
     */
    private void mergeFinancialDataOnly(
            TrendyolOrder order,
            List<TrendyolFinancialSettlementItem> settlements) {

        // Separate settlements by type
        List<TrendyolFinancialSettlementItem> sales = new ArrayList<>();
        List<TrendyolFinancialSettlementItem> returns = new ArrayList<>();
        List<TrendyolFinancialSettlementItem> discounts = new ArrayList<>();
        List<TrendyolFinancialSettlementItem> coupons = new ArrayList<>();
        List<TrendyolFinancialSettlementItem> earlyPayments = new ArrayList<>();

        for (TrendyolFinancialSettlementItem item : settlements) {
            String type = item.getTransactionType();
            if ("Satış".equals(type) || "Sale".equals(type)) {
                sales.add(item);
            } else if ("İade".equals(type) || "Return".equals(type)) {
                returns.add(item);
            } else if ("İndirim".equals(type) || "Discount".equals(type)) {
                discounts.add(item);
            } else if ("Kupon".equals(type) || "Coupon".equals(type)) {
                coupons.add(item);
            } else if ("EarlyPayment".equals(type) || "ErkenOdeme".equals(type)) {
                earlyPayments.add(item);
            }
        }

        if (sales.isEmpty()) {
            log.debug("No sale settlements to merge for order {}", order.getTyOrderNumber());
            return;
        }

        // Update financial fields only
        BigDecimal grossAmount = sales.stream()
            .map(TrendyolFinancialSettlementItem::getCredit)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDiscount = discounts.stream()
            .map(TrendyolFinancialSettlementItem::getDebt)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTyDiscount = coupons.stream()
            .map(TrendyolFinancialSettlementItem::getDebt)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate coupon discount
        BigDecimal couponDiscount = coupons.stream()
            .map(TrendyolFinancialSettlementItem::getDebt)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate early payment fee
        BigDecimal earlyPaymentFee = earlyPayments.stream()
            .map(TrendyolFinancialSettlementItem::getDebt)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCommission = sales.stream()
            .map(TrendyolFinancialSettlementItem::getCommissionAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Update order financial fields
        order.setGrossAmount(grossAmount);
        order.setTotalDiscount(totalDiscount);
        order.setTotalTyDiscount(totalTyDiscount);
        order.setCouponDiscount(couponDiscount);
        order.setEarlyPaymentFee(earlyPaymentFee);
        order.setTotalPrice(grossAmount.subtract(totalDiscount).subtract(totalTyDiscount));
        order.setEstimatedCommission(totalCommission);
        order.setIsCommissionEstimated(false); // Real commission from Settlement API
        
        // Update transaction status and date
        TrendyolFinancialSettlementItem firstSale = sales.get(0);
        order.setTransactionStatus("SETTLED");
        order.setTransactionDate(convertTimestampToLocalDateTime(firstSale.getTransactionDate()));
        
        // Update financial transactions (if needed)
        List<FinancialOrderItemData> financialTransactions = createFinancialTransactionsFromSettlements(settlements);
        if (!financialTransactions.isEmpty()) {
            order.setFinancialTransactions(financialTransactions);
        }
        
        // Update status based on returns (if applicable)
        if (!returns.isEmpty()) {
            if (returns.size() >= sales.size()) {
                order.setStatus("Returned");
            } else {
                order.setStatus("PartiallyReturned");
            }
        }
        
        log.debug("Merged financial data for order {}: grossAmount={}, commission={}",
            order.getTyOrderNumber(), grossAmount, totalCommission);
    }

    /**
     * Update an existing order completely from settlements.
     * Used when order came from SETTLEMENT_API source and needs full update.
     * 
     * @param order Existing order to update
     * @param settlements Settlement items to use for update
     */
    private void updateOrderFromSettlements(
            TrendyolOrder order,
            List<TrendyolFinancialSettlementItem> settlements) {

        // Separate settlements by type
        List<TrendyolFinancialSettlementItem> sales = new ArrayList<>();
        List<TrendyolFinancialSettlementItem> returns = new ArrayList<>();
        List<TrendyolFinancialSettlementItem> discounts = new ArrayList<>();
        List<TrendyolFinancialSettlementItem> coupons = new ArrayList<>();
        List<TrendyolFinancialSettlementItem> earlyPayments = new ArrayList<>();

        for (TrendyolFinancialSettlementItem item : settlements) {
            String type = item.getTransactionType();
            if ("Satış".equals(type) || "Sale".equals(type)) {
                sales.add(item);
            } else if ("İade".equals(type) || "Return".equals(type)) {
                returns.add(item);
            } else if ("İndirim".equals(type) || "Discount".equals(type)) {
                discounts.add(item);
            } else if ("Kupon".equals(type) || "Coupon".equals(type)) {
                coupons.add(item);
            } else if ("EarlyPayment".equals(type) || "ErkenOdeme".equals(type)) {
                earlyPayments.add(item);
            }
        }

        if (sales.isEmpty()) {
            log.debug("No sale settlements to update order {}", order.getTyOrderNumber());
            return;
        }

        // Get order date from first sale
        TrendyolFinancialSettlementItem firstSale = sales.get(0);
        LocalDateTime orderDate = convertTimestampToLocalDateTime(firstSale.getOrderDate());
        if (orderDate == null) {
            orderDate = convertTimestampToLocalDateTime(firstSale.getTransactionDate());
        }

        // Calculate amounts
        BigDecimal grossAmount = sales.stream()
            .map(TrendyolFinancialSettlementItem::getCredit)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDiscount = discounts.stream()
            .map(TrendyolFinancialSettlementItem::getDebt)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTyDiscount = coupons.stream()
            .map(TrendyolFinancialSettlementItem::getDebt)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate coupon discount
        BigDecimal couponDiscount = coupons.stream()
            .map(TrendyolFinancialSettlementItem::getDebt)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate early payment fee
        BigDecimal earlyPaymentFee = earlyPayments.stream()
            .map(TrendyolFinancialSettlementItem::getDebt)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCommission = sales.stream()
            .map(TrendyolFinancialSettlementItem::getCommissionAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Update order items from sales
        List<OrderItem> orderItems = createOrderItemsFromSales(order.getStore(), sales);

        // Determine status
        String status = returns.isEmpty() ? "Delivered" : "PartiallyReturned";
        if (returns.size() >= sales.size()) {
            status = "Returned";
        }

        // Update all fields
        order.setOrderDate(orderDate);
        order.setGrossAmount(grossAmount);
        order.setTotalDiscount(totalDiscount);
        order.setTotalTyDiscount(totalTyDiscount);
        order.setCouponDiscount(couponDiscount);
        order.setEarlyPaymentFee(earlyPaymentFee);
        order.setOrderItems(orderItems);
        order.setStatus(status);
        order.setShipmentPackageStatus("Delivered");
        order.setTotalPrice(grossAmount.subtract(totalDiscount).subtract(totalTyDiscount));
        order.setEstimatedCommission(totalCommission);
        order.setIsCommissionEstimated(false);
        order.setTransactionStatus("SETTLED");
        order.setTransactionDate(convertTimestampToLocalDateTime(firstSale.getTransactionDate()));
        order.setDataSource("SETTLEMENT_API");
        
        // Update financial transactions
        List<FinancialOrderItemData> financialTransactions = createFinancialTransactionsFromSettlements(settlements);
        if (!financialTransactions.isEmpty()) {
            order.setFinancialTransactions(financialTransactions);
        }
        
        log.debug("Updated order {} completely from settlements: grossAmount={}, commission={}",
            order.getTyOrderNumber(), grossAmount, totalCommission);
    }

    /**
     * Create financial transactions list from settlement items.
     * Helper method for merge operations.
     * Note: This is a simplified implementation. Full implementation would require
     * converting TrendyolFinancialSettlementItem to FinancialSettlement objects.
     */
    private List<FinancialOrderItemData> createFinancialTransactionsFromSettlements(
            List<TrendyolFinancialSettlementItem> settlements) {
        // For now, return empty list as financial transactions are typically
        // populated during Financial Sync step, not during Historical Sync.
        // This method is kept for future enhancement.
        return new ArrayList<>();
    }
}
