package com.ecommerce.sellerx.financial;

import com.ecommerce.sellerx.common.TrendyolRateLimiter;
import com.ecommerce.sellerx.common.exception.InvalidStoreConfigurationException;
import com.ecommerce.sellerx.common.exception.ResourceNotFoundException;
import com.ecommerce.sellerx.orders.TrendyolOrderRepository;
import com.ecommerce.sellerx.stores.MarketplaceCredentials;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.stores.TrendyolCredentials;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Locale;

/**
 * Service for fetching Trendyol OtherFinancials API data:
 * - Stoppage (Tevkifat)
 * - PaymentOrder (Hak Edis)
 * - DeductionInvoices (for Cargo Invoice serial numbers)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrendyolOtherFinancialsService {

    private static final String TRENDYOL_BASE_URL = "https://apigw.trendyol.com";
    private static final String OTHER_FINANCIALS_ENDPOINT = "/integration/finance/che/sellers/{sellerId}/otherfinancials";
    private static final String CARGO_INVOICE_ENDPOINT = "/integration/finance/che/sellers/{sellerId}/cargo-invoice/{invoiceSerialNumber}/items";

    private final StoreRepository storeRepository;
    private final TrendyolStoppageRepository stoppageRepository;
    private final TrendyolCargoInvoiceRepository cargoInvoiceRepository;
    private final TrendyolPaymentOrderRepository paymentOrderRepository;
    private final TrendyolDeductionInvoiceRepository deductionInvoiceRepository;
    private final com.ecommerce.sellerx.products.TrendyolProductRepository productRepository;
    private final TrendyolOrderRepository orderRepository;
    private final RestTemplate restTemplate;
    private final TrendyolRateLimiter rateLimiter;

    /**
     * Sync Stoppage (Tevkifat) transactions
     */
    @Transactional
    public void syncStoppages(Store store, TrendyolCredentials credentials, LocalDate startDate, LocalDate endDate) {
        log.info("Syncing stoppages for store: {} from {} to {}", store.getId(), startDate, endDate);

        HttpEntity<String> entity = createHttpEntity(credentials);
        long startTimestamp = startDate.atStartOfDay(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();
        long endTimestamp = endDate.plusDays(1).atStartOfDay(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();

        int currentPage = 0;
        int totalPages = 1;
        int savedCount = 0;

        while (currentPage < totalPages) {
            rateLimiter.acquire(store.getId());

            String url = TRENDYOL_BASE_URL + OTHER_FINANCIALS_ENDPOINT +
                    "?transactionType=Stoppage" +
                    "&startDate=" + startTimestamp +
                    "&endDate=" + endTimestamp +
                    "&page=" + currentPage +
                    "&size=1000";

            try {
                ResponseEntity<TrendyolOtherFinancialsResponse> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        TrendyolOtherFinancialsResponse.class,
                        credentials.getSellerId()
                );

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    TrendyolOtherFinancialsResponse data = response.getBody();

                    if (currentPage == 0) {
                        totalPages = data.getTotalPages() != null ? data.getTotalPages() : 1;
                        log.info("Found {} total Stoppage records across {} pages for store: {}",
                                data.getTotalElements(), totalPages, store.getId());
                    }

                    if (data.getContent() != null) {
                        for (TrendyolOtherFinancialsItem item : data.getContent()) {
                            savedCount += saveStoppage(store, item);
                        }
                    }
                } else {
                    log.warn("Failed to fetch stoppages page {} for store: {}", currentPage, store.getId());
                    break;
                }

                currentPage++;
                Thread.sleep(200);

            } catch (Exception e) {
                log.error("Error fetching stoppages for store: {}", store.getId(), e);
                break;
            }
        }

        log.info("Saved {} stoppage records for store: {}", savedCount, store.getId());
    }

    /**
     * Sync PaymentOrder transactions for Hak Edis Kontrolu
     */
    @Transactional
    public void syncPaymentOrders(Store store, TrendyolCredentials credentials, LocalDate startDate, LocalDate endDate) {
        log.info("Syncing payment orders for store: {} from {} to {}", store.getId(), startDate, endDate);

        HttpEntity<String> entity = createHttpEntity(credentials);
        long startTimestamp = startDate.atStartOfDay(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();
        long endTimestamp = endDate.plusDays(1).atStartOfDay(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();

        int currentPage = 0;
        int totalPages = 1;
        int savedCount = 0;

        while (currentPage < totalPages) {
            rateLimiter.acquire(store.getId());

            String url = TRENDYOL_BASE_URL + OTHER_FINANCIALS_ENDPOINT +
                    "?transactionType=PaymentOrder" +
                    "&startDate=" + startTimestamp +
                    "&endDate=" + endTimestamp +
                    "&page=" + currentPage +
                    "&size=1000";

            try {
                ResponseEntity<TrendyolOtherFinancialsResponse> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        TrendyolOtherFinancialsResponse.class,
                        credentials.getSellerId()
                );

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    TrendyolOtherFinancialsResponse data = response.getBody();

                    if (currentPage == 0) {
                        totalPages = data.getTotalPages() != null ? data.getTotalPages() : 1;
                        log.info("Found {} total PaymentOrder records across {} pages for store: {}",
                                data.getTotalElements(), totalPages, store.getId());
                    }

                    if (data.getContent() != null) {
                        for (TrendyolOtherFinancialsItem item : data.getContent()) {
                            savedCount += savePaymentOrder(store, item);
                        }
                    }
                } else {
                    log.warn("Failed to fetch payment orders page {} for store: {}", currentPage, store.getId());
                    break;
                }

                currentPage++;
                Thread.sleep(200);

            } catch (Exception e) {
                log.error("Error fetching payment orders for store: {}", store.getId(), e);
                break;
            }
        }

        log.info("Saved {} payment order records for store: {}", savedCount, store.getId());
    }

    /**
     * Sync Cargo Invoices - First fetch DeductionInvoices to get serial numbers, then fetch cargo details
     */
    @Transactional
    public void syncCargoInvoices(Store store, TrendyolCredentials credentials, LocalDate startDate, LocalDate endDate) {
        log.info("Syncing cargo invoices for store: {} from {} to {}", store.getId(), startDate, endDate);

        HttpEntity<String> entity = createHttpEntity(credentials);

        // Step 1: Get DeductionInvoices to find invoice serial numbers
        Set<String> invoiceSerialNumbers = fetchDeductionInvoiceSerialNumbers(store, credentials, entity, startDate, endDate);
        log.info("Found {} unique invoice serial numbers for store: {}", invoiceSerialNumbers.size(), store.getId());

        // Step 2: For each invoice serial number, fetch cargo invoice details
        int savedCount = 0;
        for (String serialNumber : invoiceSerialNumbers) {
            savedCount += fetchAndSaveCargoInvoiceDetails(store, credentials, entity, serialNumber, startDate);
        }

        log.info("Saved {} cargo invoice records for store: {}", savedCount, store.getId());
    }

    private Set<String> fetchDeductionInvoiceSerialNumbers(Store store, TrendyolCredentials credentials,
                                                            HttpEntity<String> entity, LocalDate startDate, LocalDate endDate) {
        Set<String> serialNumbers = new HashSet<>();

        // Trendyol API has a 15-day limit on date range
        // Split into 14-day chunks to stay within the limit
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween > 14) {
            log.info("Date range {} days exceeds 15-day limit for deduction invoices, splitting into chunks", daysBetween);
            LocalDate chunkStart = startDate;

            while (chunkStart.isBefore(endDate) || chunkStart.isEqual(endDate)) {
                LocalDate chunkEnd = chunkStart.plusDays(14);
                if (chunkEnd.isAfter(endDate)) {
                    chunkEnd = endDate;
                }

                log.info("Processing deduction invoices chunk: {} to {}", chunkStart, chunkEnd);
                serialNumbers.addAll(fetchDeductionInvoiceSerialNumbersChunk(store, credentials, entity, chunkStart, chunkEnd));

                chunkStart = chunkEnd.plusDays(1);
            }

            log.info("Total found {} unique invoice serial numbers across all chunks for store: {}", serialNumbers.size(), store.getId());
            return serialNumbers;
        }

        return fetchDeductionInvoiceSerialNumbersChunk(store, credentials, entity, startDate, endDate);
    }

    private Set<String> fetchDeductionInvoiceSerialNumbersChunk(Store store, TrendyolCredentials credentials,
                                                                  HttpEntity<String> entity, LocalDate startDate, LocalDate endDate) {
        Set<String> serialNumbers = new HashSet<>();

        long startTimestamp = startDate.atStartOfDay(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();
        long endTimestamp = endDate.plusDays(1).atStartOfDay(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();

        int currentPage = 0;
        int totalPages = 1;

        while (currentPage < totalPages) {
            rateLimiter.acquire(store.getId());

            String url = TRENDYOL_BASE_URL + OTHER_FINANCIALS_ENDPOINT +
                    "?transactionType=DeductionInvoices" +
                    "&startDate=" + startTimestamp +
                    "&endDate=" + endTimestamp +
                    "&page=" + currentPage +
                    "&size=1000";

            try {
                ResponseEntity<TrendyolOtherFinancialsResponse> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        TrendyolOtherFinancialsResponse.class,
                        credentials.getSellerId()
                );

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    TrendyolOtherFinancialsResponse data = response.getBody();

                    if (currentPage == 0) {
                        totalPages = data.getTotalPages() != null ? data.getTotalPages() : 1;
                    }

                    if (data.getContent() != null) {
                        for (TrendyolOtherFinancialsItem item : data.getContent()) {
                            // According to Trendyol documentation:
                            // For "Kargo Fatura" or "Kargo Faturası" records, the 'id' field IS the invoiceSerialNumber
                            // https://partner.trendyol.com/help/developer/accounting-finance-integration
                            if (isCargoInvoiceType(item.getTransactionType())) {
                                if (item.getId() != null && !item.getId().isEmpty()) {
                                    serialNumbers.add(item.getId());
                                    log.debug("Found cargo invoice serial number from id: {} for transactionType: {}",
                                            item.getId(), item.getTransactionType());
                                }
                            } else if (item.getInvoiceSerialNumber() != null && !item.getInvoiceSerialNumber().isEmpty()) {
                                serialNumbers.add(item.getInvoiceSerialNumber());
                            }
                        }
                    }
                }

                currentPage++;
                Thread.sleep(200);

            } catch (Exception e) {
                log.error("Error fetching deduction invoices for store: {} - chunk {} to {}: {}",
                        store.getId(), startDate, endDate, e.getMessage());
                break;
            }
        }

        return serialNumbers;
    }

    private int fetchAndSaveCargoInvoiceDetails(Store store, TrendyolCredentials credentials,
                                                 HttpEntity<String> entity, String serialNumber, LocalDate invoiceDate) {
        int savedCount = 0;
        int currentPage = 0;
        int totalPages = 1;

        while (currentPage < totalPages) {
            rateLimiter.acquire(store.getId());

            String url = TRENDYOL_BASE_URL + CARGO_INVOICE_ENDPOINT +
                    "?page=" + currentPage +
                    "&size=1000";

            try {
                ResponseEntity<TrendyolCargoInvoiceResponse> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        TrendyolCargoInvoiceResponse.class,
                        credentials.getSellerId(),
                        serialNumber
                );

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    TrendyolCargoInvoiceResponse data = response.getBody();

                    if (currentPage == 0) {
                        totalPages = data.getTotalPages() != null ? data.getTotalPages() : 1;
                    }

                    if (data.getContent() != null) {
                        for (TrendyolCargoInvoiceItem item : data.getContent()) {
                            savedCount += saveCargoInvoice(store, serialNumber, item, invoiceDate);
                        }
                    }
                }

                currentPage++;
                Thread.sleep(200);

            } catch (Exception e) {
                log.error("Error fetching cargo invoice {} for store: {}", serialNumber, store.getId(), e);
                break;
            }
        }

        return savedCount;
    }

    private int saveStoppage(Store store, TrendyolOtherFinancialsItem item) {
        LocalDateTime transactionDate = item.getTransactionDate() != null
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(item.getTransactionDate()), ZoneId.of("Europe/Istanbul"))
                : LocalDateTime.now();

        // Check if already exists
        if (stoppageRepository.existsByStoreIdAndTransactionIdAndTransactionDate(
                store.getId(), item.getId(), transactionDate)) {
            return 0;
        }

        BigDecimal amount = item.getDebt() != null ? item.getDebt() : BigDecimal.ZERO;
        if (item.getCredit() != null) {
            amount = amount.subtract(item.getCredit());
        }

        BigDecimal finalAmount = amount.abs();

        TrendyolStoppage stoppage = TrendyolStoppage.builder()
                .store(store)
                .transactionId(item.getId())
                .transactionDate(transactionDate)
                .amount(finalAmount)
                .invoiceSerialNumber(item.getInvoiceSerialNumber())
                .paymentOrderId(item.getPaymentOrderId())
                .receiptId(item.getReceiptId())
                .description(item.getDescription())
                // Order matching fields from OtherFinancials API
                .orderNumber(item.getOrderNumber())
                .shipmentPackageId(item.getShipmentPackageId())
                .rawData(Map.of(
                        "debt", item.getDebt() != null ? item.getDebt() : BigDecimal.ZERO,
                        "credit", item.getCredit() != null ? item.getCredit() : BigDecimal.ZERO,
                        "transactionType", item.getTransactionType() != null ? item.getTransactionType() : ""
                ))
                .build();

        stoppageRepository.save(stoppage);

        return 1;
    }

    private int savePaymentOrder(Store store, TrendyolOtherFinancialsItem item) {
        if (item.getPaymentOrderId() == null) {
            return 0;
        }

        // Check if already exists
        Optional<TrendyolPaymentOrder> existing = paymentOrderRepository
                .findByStoreIdAndPaymentOrderId(store.getId(), item.getPaymentOrderId());

        LocalDateTime paymentDate = item.getPaymentDate() != null
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(item.getPaymentDate()), ZoneId.of("Europe/Istanbul"))
                : LocalDateTime.now();

        // PaymentOrder amounts come in the 'debt' field from Trendyol API
        BigDecimal totalAmount = item.getDebt() != null ? item.getDebt() : BigDecimal.ZERO;

        if (existing.isPresent()) {
            // Update existing record
            TrendyolPaymentOrder paymentOrder = existing.get();
            paymentOrder.setTotalAmount(totalAmount);
            paymentOrder.setPaymentDate(paymentDate);
            paymentOrderRepository.save(paymentOrder);
            return 0; // Not a new record
        }

        TrendyolPaymentOrder paymentOrder = TrendyolPaymentOrder.builder()
                .store(store)
                .paymentOrderId(item.getPaymentOrderId())
                .paymentDate(paymentDate)
                .totalAmount(totalAmount)
                .rawData(Map.of(
                        "debt", item.getDebt() != null ? item.getDebt() : BigDecimal.ZERO,
                        "credit", item.getCredit() != null ? item.getCredit() : BigDecimal.ZERO,
                        "description", item.getDescription() != null ? item.getDescription() : ""
                ))
                .build();

        paymentOrderRepository.save(paymentOrder);
        return 1;
    }

    private int saveCargoInvoice(Store store, String serialNumber, TrendyolCargoInvoiceItem item, LocalDate invoiceDate) {
        // Check if already exists
        if (cargoInvoiceRepository.existsByStoreIdAndInvoiceSerialNumberAndShipmentPackageId(
                store.getId(), serialNumber, item.getShipmentPackageId())) {
            return 0;
        }

        BigDecimal shippingAmount = item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO;

        // Resolve barcode: Trendyol cargo invoice API returns empty barcode,
        // so look it up from the order via order_number
        String barcode = item.getBarcode();
        if ((barcode == null || barcode.isEmpty()) && item.getOrderNumber() != null) {
            var orders = orderRepository.findByStoreIdAndTyOrderNumber(
                    store.getId(), item.getOrderNumber());
            if (!orders.isEmpty()) {
                var order = orders.get(0);
                if (order.getOrderItems() != null && order.getOrderItems().size() == 1) {
                    barcode = order.getOrderItems().get(0).getBarcode();
                }
            }
        }

        String resolvedBarcode = barcode != null ? barcode : "";

        TrendyolCargoInvoice cargoInvoice = TrendyolCargoInvoice.builder()
                .store(store)
                .invoiceSerialNumber(serialNumber)
                .orderNumber(item.getOrderNumber())
                .shipmentPackageId(item.getShipmentPackageId())
                .amount(shippingAmount)
                .desi(item.getDesi())
                .shipmentPackageType(item.getShipmentPackageType())
                .vatRate(20) // Cargo VAT is typically 20%
                .invoiceDate(invoiceDate)
                .rawData(Map.of(
                        "barcode", resolvedBarcode,
                        "productName", item.getProductName() != null ? item.getProductName() : ""
                ))
                .build();

        cargoInvoice.calculateVatAmount();
        cargoInvoiceRepository.save(cargoInvoice);

        // Update product's lastShippingCostPerUnit for future shipping estimations
        if (!resolvedBarcode.isEmpty() && shippingAmount.compareTo(BigDecimal.ZERO) > 0) {
            productRepository.findByStoreIdAndBarcode(store.getId(), resolvedBarcode)
                    .ifPresent(product -> {
                        product.setLastShippingCostPerUnit(shippingAmount);
                        product.setLastShippingCostDate(LocalDateTime.now());
                        productRepository.save(product);
                        log.debug("Updated product {} lastShippingCostPerUnit to {} from cargo invoice {}",
                                resolvedBarcode, shippingAmount, serialNumber);
                    });
        }

        return 1;
    }

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

    /**
     * Check if the transaction type is a cargo invoice type.
     * According to Trendyol documentation, for "Kargo Fatura" or "Kargo Faturası" records,
     * the 'id' field is the invoiceSerialNumber to use for the cargo invoice detail API.
     */
    private boolean isCargoInvoiceType(String transactionType) {
        if (transactionType == null) {
            return false;
        }
        return transactionType.equals("Kargo Fatura") ||
               transactionType.equals("Kargo Faturası") ||
               transactionType.equalsIgnoreCase("Kargo Fatura") ||
               transactionType.contains("Kargo Fatura");
    }

    /**
     * Check if the transaction type is a commission invoice type.
     * For "Komisyon Faturası" and "Platform Hizmet Bedeli" records,
     * the 'id' field contains the invoiceSerialNumber (similar to Kargo Fatura behavior).
     * This covers all KOMISYON category invoice types.
     */
    private boolean isCommissionInvoiceType(String transactionType) {
        if (transactionType == null) {
            return false;
        }
        String lower = transactionType.toLowerCase();
        // Komisyon Faturası
        if (lower.contains("komisyon") && lower.contains("fatura")) {
            return true;
        }
        // Platform Hizmet Bedeli
        if (lower.contains("platform") && lower.contains("hizmet")) {
            return true;
        }
        return false;
    }

    /**
     * Check if the transaction type is a penalty (CEZA) invoice type.
     * For penalty invoices, the 'id' field contains the invoiceSerialNumber
     * when invoiceSerialNumber is not provided by Trendyol API.
     * Handles variations like "TEDARIK EDEMEME FATURASI" vs "Tedarik Edememe".
     */
    private boolean isPenaltyInvoiceType(String transactionType) {
        if (transactionType == null) {
            return false;
        }
        // Use uppercase for case-insensitive matching
        // Also handle ASCII variations of Turkish characters (I vs İ, S vs Ş)
        String upper = transactionType.toUpperCase(Locale.ENGLISH);
        // Penalty types from TYPE_TO_CATEGORY mapping
        return upper.contains("TEDARIK EDEMEME") ||
               upper.contains("TERMIN GECIKME") ||
               upper.contains("EKSIK URUN") || upper.contains("EKS") && upper.contains("URUN") ||
               upper.contains("YANLIS URUN") || upper.contains("YANLI") && upper.contains("URUN") ||
               upper.contains("KUSURLU URUN") || upper.contains("KUSUR") && upper.contains("URUN");
    }

    /**
     * Check if the transaction type is a deduction (KESINTI/DIGER) invoice type.
     * For deduction invoices, the 'id' field may contain the invoiceSerialNumber
     * when invoiceSerialNumber is not provided by Trendyol API.
     */
    private boolean isDeductionInvoiceType(String transactionType) {
        if (transactionType == null) {
            return false;
        }
        // Deduction types from TYPE_TO_CATEGORY mapping (DIGER category)
        return transactionType.equals("Kurumsal Kampanya Yansıtma Bedeli") ||
               transactionType.equals("Erken Ödeme Kesinti Faturası") ||
               transactionType.equals("Fatura Kontör Satış Bedeli") ||
               transactionType.equals("Müşteri Duyuruları Faturası") ||
               transactionType.contains("TEX Tazmin");
    }

    /**
     * Check if the transaction type is an advertising (REKLAM) invoice type.
     * For "Reklam Bedeli" and "Komisyonlu İnfluencer Reklam Bedeli" records,
     * the 'id' field contains the invoiceSerialNumber when invoiceSerialNumber is not provided.
     */
    private boolean isAdInvoiceType(String transactionType) {
        if (transactionType == null) {
            return false;
        }
        // Ad types from TYPE_TO_CATEGORY mapping (REKLAM category)
        return transactionType.equals("Reklam Bedeli") ||
               transactionType.equals("Komisyonlu İnfluencer Reklam Bedeli") ||
               transactionType.equals("Sabit Bütçeli Influencer Reklam Bedeli");
    }

    /**
     * Check if the transaction type is an international operation (ULUSLARARASI/IADE) invoice type.
     * For "Yurtdışı Operasyon Iade Bedeli", "AZ-Yurtdışı Operasyon Bedeli" etc.,
     * the 'id' field contains the invoiceSerialNumber when invoiceSerialNumber is not provided.
     * Also handles corrupted Turkish characters like "AZ-YURTDÕ_Õ OPERASYON BEDELI %18".
     */
    private boolean isInternationalOperationInvoiceType(String transactionType) {
        if (transactionType == null) {
            return false;
        }
        String upper = transactionType.toUpperCase(Locale.ENGLISH);
        // Check for various patterns:
        // - "Yurtdışı Operasyon Iade Bedeli"
        // - "AZ-Yurtdışı Operasyon Bedeli"
        // - "AZ-YURTDÕ_Õ OPERASYON BEDELI %18" (corrupted version)
        // - "Yurt Dışı Operasyon Bedeli"
        if (upper.contains("YURT") && upper.contains("OPERASYON")) {
            return true;
        }
        // Handle corrupted Turkish Õ → I mapping
        if (upper.contains("YURT") && (upper.contains("OPERAS") || upper.contains("OPERATION"))) {
            return true;
        }
        return false;
    }

    /**
     * Check if the transaction type is an international service fee (ULUSLARARASI HIZMET) invoice type.
     * For "Uluslararası Hizmet Bedeli" and "AZ-Uluslararası Hizmet Bedeli" records,
     * the 'id' field contains the invoiceSerialNumber when invoiceSerialNumber is not provided.
     * Note: This is different from isInternationalOperationInvoiceType() which handles "Yurtdışı Operasyon" types.
     */
    private boolean isInternationalServiceFeeType(String transactionType) {
        if (transactionType == null) {
            return false;
        }
        // Use Turkish locale for proper uppercase conversion of Turkish characters
        String upper = transactionType.toUpperCase(Locale.forLanguageTag("tr-TR"));
        // Check for "Uluslararası Hizmet Bedeli" and "AZ-Uluslararası Hizmet Bedeli"
        // Turkish İ becomes I in uppercase, so we check for both patterns
        return (upper.contains("ULUSLARARASI") || upper.contains("ULUSLARARASI")) && upper.contains("HİZMET");
    }

    /**
     * Correct transaction type names that Trendyol API returns incorrectly.
     * Trendyol API returns different names than what they show in their Excel export.
     */
    private String correctTransactionType(String transactionType) {
        if (transactionType == null) {
            return null;
        }
        // Trendyol API returns "Yurt Dışı Operasyon Bedeli" but Excel shows "Yurtdışı Operasyon Iade Bedeli"
        if (transactionType.equals("Yurt Dışı Operasyon Bedeli")) {
            return "Yurtdışı Operasyon Iade Bedeli";
        }
        return transactionType;
    }

    /**
     * Normalizes corrupted Turkish characters in transaction types.
     * Trendyol API started returning corrupted encoding after Dec 30, 2025 update.
     * First affected records appeared on Jan 21, 2026.
     *
     * Known corrupted patterns are mapped to correct Turkish equivalents.
     */
    private String normalizeTransactionType(String transactionType) {
        if (transactionType == null) {
            return null;
        }

        // Known corrupted patterns → correct Turkish
        return switch (transactionType) {
            case "AZ-YURTDÕ_Õ OPERASYON BEDELI %18" -> "AZ-Yurtdışı Operasyon Bedeli %18";
            case "TEDARIK EDEMEME FATURASI" -> "Tedarik Edememe";
            case "YANLIS URUN FATURASI" -> "Yanlış Ürün Faturası";
            case "EKSIK URUN FATURASI" -> "Eksik Ürün Faturası";
            case "KUSURLU URUN FATURASI" -> "Kusurlu Ürün Faturası";
            default -> transactionType;
        };
    }

    // ============== Wrapper methods for controller usage ==============

    /**
     * Sync all other financials and return result with statistics.
     * This is a wrapper that returns SyncResult for controller use.
     */
    @Transactional
    public SyncResult syncAllOtherFinancials(UUID storeId, LocalDate startDate, LocalDate endDate) {
        log.info("Starting to sync other financials for store: {} from {} to {}", storeId, startDate, endDate);

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));

        if (!"trendyol".equalsIgnoreCase(store.getMarketplace())) {
            throw InvalidStoreConfigurationException.notTrendyolStore(storeId.toString());
        }

        TrendyolCredentials credentials = extractTrendyolCredentials(store);
        if (credentials == null || credentials.getSellerId() == null) {
            throw InvalidStoreConfigurationException.missingCredentials(storeId.toString());
        }

        int stoppagesCount = syncStoppagesInternal(store, credentials, startDate, endDate);
        int paymentOrdersCount = syncPaymentOrdersInternal(store, credentials, startDate, endDate);
        int cargoInvoicesCount = syncCargoInvoicesInternal(store, credentials, startDate, endDate);
        int deductionInvoicesCount = syncDeductionInvoicesInternal(store, credentials, startDate, endDate);
        int returnInvoicesCount = syncReturnInvoicesInternal(store, credentials, startDate, endDate);

        // Sync additional OtherFinancials transaction types
        int cashAdvanceCount = syncGenericOtherFinancialsInternal(store, credentials, startDate, endDate, "CashAdvance");
        int wireTransferCount = syncGenericOtherFinancialsInternal(store, credentials, startDate, endDate, "WireTransfer");
        int incomingTransferCount = syncGenericOtherFinancialsInternal(store, credentials, startDate, endDate, "IncomingTransfer");
        int commissionAgreementCount = syncGenericOtherFinancialsInternal(store, credentials, startDate, endDate, "CommissionAgreementInvoice");
        int financialItemCount = syncGenericOtherFinancialsInternal(store, credentials, startDate, endDate, "FinancialItem");

        log.info("Completed syncing other financials for store: {} - stoppages: {}, paymentOrders: {}, cargoInvoices: {}, " +
                "deductionInvoices: {}, returnInvoices: {}, cashAdvance: {}, wireTransfer: {}, incomingTransfer: {}, " +
                "commissionAgreement: {}, financialItem: {}",
                storeId, stoppagesCount, paymentOrdersCount, cargoInvoicesCount, deductionInvoicesCount, returnInvoicesCount,
                cashAdvanceCount, wireTransferCount, incomingTransferCount, commissionAgreementCount, financialItemCount);

        return SyncResult.builder()
                .stoppagesCount(stoppagesCount)
                .paymentOrdersCount(paymentOrdersCount)
                .cargoInvoicesCount(cargoInvoicesCount)
                .deductionInvoicesCount(deductionInvoicesCount)
                .returnInvoicesCount(returnInvoicesCount)
                .cashAdvanceCount(cashAdvanceCount)
                .wireTransferCount(wireTransferCount)
                .incomingTransferCount(incomingTransferCount)
                .commissionAgreementCount(commissionAgreementCount)
                .financialItemCount(financialItemCount)
                .success(true)
                .build();
    }

    /**
     * Sync stoppages by storeId (wrapper for controller)
     */
    @Transactional
    public int syncStoppages(UUID storeId, LocalDate startDate, LocalDate endDate) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));

        TrendyolCredentials credentials = extractTrendyolCredentials(store);
        if (credentials == null || credentials.getSellerId() == null) {
            throw InvalidStoreConfigurationException.missingCredentials(storeId.toString());
        }

        return syncStoppagesInternal(store, credentials, startDate, endDate);
    }

    /**
     * Sync payment orders by storeId (wrapper for controller)
     */
    @Transactional
    public int syncPaymentOrders(UUID storeId, LocalDate startDate, LocalDate endDate) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));

        TrendyolCredentials credentials = extractTrendyolCredentials(store);
        if (credentials == null || credentials.getSellerId() == null) {
            throw InvalidStoreConfigurationException.missingCredentials(storeId.toString());
        }

        return syncPaymentOrdersInternal(store, credentials, startDate, endDate);
    }

    /**
     * Sync cargo invoices by storeId (wrapper for controller)
     */
    @Transactional
    public int syncCargoInvoices(UUID storeId, LocalDate startDate, LocalDate endDate) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));

        TrendyolCredentials credentials = extractTrendyolCredentials(store);
        if (credentials == null || credentials.getSellerId() == null) {
            throw InvalidStoreConfigurationException.missingCredentials(storeId.toString());
        }

        int savedCount = syncCargoInvoicesInternal(store, credentials, startDate, endDate);

        // Update orders with real shipping costs from cargo invoices
        int updatedOrders = updateOrdersWithRealShipping(storeId);
        log.info("Updated {} orders with real shipping costs for store: {}", updatedOrders, storeId);

        return savedCount;
    }

    /**
     * Sync ALL historical cargo invoices for a store.
     * Fetches cargo invoices from the earliest order date to today.
     * Used for:
     * 1. Store onboarding - to fetch all historical shipping data
     * 2. Manual sync - to fix missing cargo invoice data
     *
     * @param storeId The store ID to sync historical cargo invoices for
     * @return HistoricalCargoSyncResult with sync statistics
     */
    @Transactional
    public HistoricalCargoSyncResult syncHistoricalCargoInvoices(UUID storeId) {
        log.info("Starting historical cargo invoice sync for store: {}", storeId);

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));

        if (!"trendyol".equalsIgnoreCase(store.getMarketplace())) {
            throw InvalidStoreConfigurationException.notTrendyolStore(storeId.toString());
        }

        TrendyolCredentials credentials = extractTrendyolCredentials(store);
        if (credentials == null || credentials.getSellerId() == null) {
            throw InvalidStoreConfigurationException.missingCredentials(storeId.toString());
        }

        // Find the earliest order date for this store
        Optional<LocalDate> earliestOrderDateOpt = orderRepository.findEarliestOrderDateByStoreId(storeId);

        if (earliestOrderDateOpt.isEmpty()) {
            log.info("No orders found for store {}, skipping historical cargo sync", storeId);
            return HistoricalCargoSyncResult.builder()
                    .storeId(storeId)
                    .syncedCargoInvoices(0)
                    .updatedOrders(0)
                    .startDate(null)
                    .endDate(null)
                    .message("No orders found for this store")
                    .success(true)
                    .build();
        }

        LocalDate startDate = earliestOrderDateOpt.get();
        LocalDate endDate = LocalDate.now();
        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);

        log.info("Syncing historical cargo invoices for store {} from {} to {} ({} days)",
                storeId, startDate, endDate, totalDays);

        // Sync cargo invoices
        int savedCount = syncCargoInvoicesInternal(store, credentials, startDate, endDate);

        // Update orders with real shipping costs
        int updatedOrders = updateOrdersWithRealShipping(storeId);

        log.info("Historical cargo sync completed for store {}: {} invoices synced, {} orders updated",
                storeId, savedCount, updatedOrders);

        return HistoricalCargoSyncResult.builder()
                .storeId(storeId)
                .syncedCargoInvoices(savedCount)
                .updatedOrders(updatedOrders)
                .startDate(startDate)
                .endDate(endDate)
                .totalDays(totalDays)
                .message("Historical cargo invoice sync completed successfully")
                .success(true)
                .build();
    }

    /**
     * Result DTO for historical cargo invoice sync operation
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoricalCargoSyncResult {
        private UUID storeId;
        private int syncedCargoInvoices;
        private int updatedOrders;
        private LocalDate startDate;
        private LocalDate endDate;
        private long totalDays;
        private String message;
        private boolean success;
    }

    // Internal methods that do the actual work (renamed from public methods)
    private int syncStoppagesInternal(Store store, TrendyolCredentials credentials, LocalDate startDate, LocalDate endDate) {
        log.info("Syncing stoppages for store: {} from {} to {}", store.getId(), startDate, endDate);

        // Trendyol API has a 15-day limit on date range
        // Split into 14-day chunks to stay within the limit
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween > 14) {
            log.info("Date range {} days exceeds 15-day limit, splitting into chunks", daysBetween);
            int totalSavedCount = 0;
            LocalDate chunkStart = startDate;

            while (chunkStart.isBefore(endDate) || chunkStart.isEqual(endDate)) {
                LocalDate chunkEnd = chunkStart.plusDays(14);
                if (chunkEnd.isAfter(endDate)) {
                    chunkEnd = endDate;
                }

                log.info("Processing stoppages chunk: {} to {}", chunkStart, chunkEnd);
                totalSavedCount += syncStoppagesChunk(store, credentials, chunkStart, chunkEnd);

                chunkStart = chunkEnd.plusDays(1);
            }

            log.info("Total saved {} stoppage records across all chunks for store: {}", totalSavedCount, store.getId());
            return totalSavedCount;
        }

        return syncStoppagesChunk(store, credentials, startDate, endDate);
    }

    private int syncStoppagesChunk(Store store, TrendyolCredentials credentials, LocalDate startDate, LocalDate endDate) {
        HttpEntity<String> entity = createHttpEntity(credentials);
        long startTimestamp = startDate.atStartOfDay(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();
        long endTimestamp = endDate.plusDays(1).atStartOfDay(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();

        int currentPage = 0;
        int totalPages = 1;
        int savedCount = 0;

        while (currentPage < totalPages) {
            rateLimiter.acquire(store.getId());

            String url = TRENDYOL_BASE_URL + OTHER_FINANCIALS_ENDPOINT +
                    "?transactionType=Stoppage" +
                    "&startDate=" + startTimestamp +
                    "&endDate=" + endTimestamp +
                    "&page=" + currentPage +
                    "&size=1000";

            try {
                ResponseEntity<TrendyolOtherFinancialsResponse> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        TrendyolOtherFinancialsResponse.class,
                        credentials.getSellerId()
                );

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    TrendyolOtherFinancialsResponse data = response.getBody();

                    if (currentPage == 0) {
                        totalPages = data.getTotalPages() != null ? data.getTotalPages() : 1;
                        log.info("Found {} total Stoppage records across {} pages for store: {}",
                                data.getTotalElements(), totalPages, store.getId());
                    }

                    if (data.getContent() != null) {
                        for (TrendyolOtherFinancialsItem item : data.getContent()) {
                            savedCount += saveStoppage(store, item);
                        }
                    }
                } else {
                    log.warn("Failed to fetch stoppages page {} for store: {}", currentPage, store.getId());
                    break;
                }

                currentPage++;
                Thread.sleep(200);

            } catch (Exception e) {
                log.error("Error fetching stoppages for store: {} - chunk {} to {}: {}",
                        store.getId(), startDate, endDate, e.getMessage());
                break;
            }
        }

        log.info("Saved {} stoppage records for chunk {} to {} for store: {}",
                savedCount, startDate, endDate, store.getId());
        return savedCount;
    }

    private int syncPaymentOrdersInternal(Store store, TrendyolCredentials credentials, LocalDate startDate, LocalDate endDate) {
        log.info("Syncing payment orders for store: {} from {} to {}", store.getId(), startDate, endDate);

        // Trendyol API has a 15-day limit on date range
        // Split into 14-day chunks to stay within the limit
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween > 14) {
            log.info("Date range {} days exceeds 15-day limit, splitting into chunks", daysBetween);
            int totalSavedCount = 0;
            LocalDate chunkStart = startDate;

            while (chunkStart.isBefore(endDate) || chunkStart.isEqual(endDate)) {
                LocalDate chunkEnd = chunkStart.plusDays(14);
                if (chunkEnd.isAfter(endDate)) {
                    chunkEnd = endDate;
                }

                log.info("Processing chunk: {} to {}", chunkStart, chunkEnd);
                totalSavedCount += syncPaymentOrdersChunk(store, credentials, chunkStart, chunkEnd);

                chunkStart = chunkEnd.plusDays(1);
            }

            log.info("Total saved {} payment order records across all chunks for store: {}", totalSavedCount, store.getId());
            return totalSavedCount;
        }

        return syncPaymentOrdersChunk(store, credentials, startDate, endDate);
    }

    private int syncPaymentOrdersChunk(Store store, TrendyolCredentials credentials, LocalDate startDate, LocalDate endDate) {
        HttpEntity<String> entity = createHttpEntity(credentials);
        long startTimestamp = startDate.atStartOfDay(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();
        long endTimestamp = endDate.plusDays(1).atStartOfDay(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();

        int currentPage = 0;
        int totalPages = 1;
        int savedCount = 0;

        while (currentPage < totalPages) {
            rateLimiter.acquire(store.getId());

            String url = TRENDYOL_BASE_URL + OTHER_FINANCIALS_ENDPOINT +
                    "?transactionType=PaymentOrder" +
                    "&startDate=" + startTimestamp +
                    "&endDate=" + endTimestamp +
                    "&page=" + currentPage +
                    "&size=1000";

            String resolvedUrl = url.replace("{sellerId}", String.valueOf(credentials.getSellerId()));
            log.info("Calling Trendyol OtherFinancials API - URL: {}", resolvedUrl);
            log.info("Date range - startDate: {} ({}), endDate: {} ({})",
                    startTimestamp, startDate, endTimestamp, endDate);

            try {
                ResponseEntity<TrendyolOtherFinancialsResponse> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        TrendyolOtherFinancialsResponse.class,
                        credentials.getSellerId()
                );

                TrendyolOtherFinancialsResponse data = response.getBody();
                if (response.getStatusCode() == HttpStatus.OK && data != null) {
                    log.info("API Response - totalElements: {}, totalPages: {}, content size: {}",
                            data.getTotalElements(), data.getTotalPages(),
                            data.getContent() != null ? data.getContent().size() : 0);

                    if (currentPage == 0) {
                        totalPages = data.getTotalPages() != null ? data.getTotalPages() : 1;
                        log.info("Found {} total PaymentOrder records across {} pages for store: {}",
                                data.getTotalElements(), totalPages, store.getId());
                    }

                    if (data.getContent() != null) {
                        for (TrendyolOtherFinancialsItem item : data.getContent()) {
                            savedCount += savePaymentOrder(store, item);
                        }
                    }
                } else {
                    log.warn("Failed to fetch payment orders page {} for store: {}", currentPage, store.getId());
                    break;
                }

                currentPage++;
                Thread.sleep(200);

            } catch (Exception e) {
                log.error("Error fetching payment orders for store: {} - chunk {} to {}: {}",
                        store.getId(), startDate, endDate, e.getMessage());
                break;
            }
        }

        log.info("Saved {} payment order records for chunk {} to {} for store: {}",
                savedCount, startDate, endDate, store.getId());
        return savedCount;
    }

    private int syncCargoInvoicesInternal(Store store, TrendyolCredentials credentials, LocalDate startDate, LocalDate endDate) {
        log.info("Syncing cargo invoices for store: {} from {} to {}", store.getId(), startDate, endDate);

        HttpEntity<String> entity = createHttpEntity(credentials);

        Set<String> invoiceSerialNumbers = fetchDeductionInvoiceSerialNumbers(store, credentials, entity, startDate, endDate);
        log.info("Found {} unique invoice serial numbers for store: {}", invoiceSerialNumbers.size(), store.getId());

        int savedCount = 0;
        for (String serialNumber : invoiceSerialNumbers) {
            savedCount += fetchAndSaveCargoInvoiceDetails(store, credentials, entity, serialNumber, startDate);
        }

        log.info("Saved {} cargo invoice records for store: {}", savedCount, store.getId());
        return savedCount;
    }

    // ============== DeductionInvoices Sync Methods ==============

    /**
     * Sync DeductionInvoices by storeId (wrapper for controller).
     * This fetches ALL deduction invoice types including platform fees, cargo fees, ad fees, etc.
     */
    @Transactional
    public int syncDeductionInvoices(UUID storeId, LocalDate startDate, LocalDate endDate) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));

        TrendyolCredentials credentials = extractTrendyolCredentials(store);
        if (credentials == null || credentials.getSellerId() == null) {
            throw InvalidStoreConfigurationException.missingCredentials(storeId.toString());
        }

        return syncDeductionInvoicesInternal(store, credentials, startDate, endDate);
    }

    private int syncDeductionInvoicesInternal(Store store, TrendyolCredentials credentials, LocalDate startDate, LocalDate endDate) {
        log.info("Syncing deduction invoices for store: {} from {} to {}", store.getId(), startDate, endDate);

        // Trendyol API has a 15-day limit on date range
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween > 14) {
            log.info("Date range {} days exceeds 15-day limit for deduction invoices, splitting into chunks", daysBetween);
            int totalSavedCount = 0;
            LocalDate chunkStart = startDate;

            while (chunkStart.isBefore(endDate) || chunkStart.isEqual(endDate)) {
                LocalDate chunkEnd = chunkStart.plusDays(14);
                if (chunkEnd.isAfter(endDate)) {
                    chunkEnd = endDate;
                }

                log.info("Processing deduction invoices chunk: {} to {}", chunkStart, chunkEnd);
                totalSavedCount += syncDeductionInvoicesChunk(store, credentials, chunkStart, chunkEnd);

                chunkStart = chunkEnd.plusDays(1);
            }

            log.info("Total saved {} deduction invoice records across all chunks for store: {}", totalSavedCount, store.getId());
            return totalSavedCount;
        }

        return syncDeductionInvoicesChunk(store, credentials, startDate, endDate);
    }

    private int syncDeductionInvoicesChunk(Store store, TrendyolCredentials credentials, LocalDate startDate, LocalDate endDate) {
        HttpEntity<String> entity = createHttpEntity(credentials);
        long startTimestamp = startDate.atStartOfDay(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();
        long endTimestamp = endDate.plusDays(1).atStartOfDay(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();

        int currentPage = 0;
        int totalPages = 1;
        int savedCount = 0;

        while (currentPage < totalPages) {
            rateLimiter.acquire(store.getId());

            String url = TRENDYOL_BASE_URL + OTHER_FINANCIALS_ENDPOINT +
                    "?transactionType=DeductionInvoices" +
                    "&startDate=" + startTimestamp +
                    "&endDate=" + endTimestamp +
                    "&page=" + currentPage +
                    "&size=1000";

            try {
                ResponseEntity<TrendyolOtherFinancialsResponse> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        TrendyolOtherFinancialsResponse.class,
                        credentials.getSellerId()
                );

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    TrendyolOtherFinancialsResponse data = response.getBody();

                    if (currentPage == 0) {
                        totalPages = data.getTotalPages() != null ? data.getTotalPages() : 1;
                        log.info("Found {} total DeductionInvoice records across {} pages for store: {}",
                                data.getTotalElements(), totalPages, store.getId());
                    }

                    if (data.getContent() != null) {
                        for (TrendyolOtherFinancialsItem item : data.getContent()) {
                            savedCount += saveDeductionInvoice(store, item);
                        }
                    }
                } else {
                    log.warn("Failed to fetch deduction invoices page {} for store: {}", currentPage, store.getId());
                    break;
                }

                currentPage++;
                Thread.sleep(200);

            } catch (Exception e) {
                log.error("Error fetching deduction invoices for store: {} - chunk {} to {}: {}",
                        store.getId(), startDate, endDate, e.getMessage());
                break;
            }
        }

        log.info("Saved {} deduction invoice records for chunk {} to {} for store: {}",
                savedCount, startDate, endDate, store.getId());
        return savedCount;
    }

    private int saveDeductionInvoice(Store store, TrendyolOtherFinancialsItem item) {
        if (item.getId() == null || item.getId().isEmpty()) {
            return 0;
        }

        // Check if already exists by Trendyol ID
        if (deductionInvoiceRepository.existsByStoreIdAndTrendyolId(store.getId(), item.getId())) {
            return 0;
        }

        LocalDateTime transactionDate = item.getTransactionDate() != null
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(item.getTransactionDate()), ZoneId.of("Europe/Istanbul"))
                : LocalDateTime.now();

        // Additional check: Prevent duplicates when Trendyol returns different IDs for same invoice
        // (e.g., first sync returns numeric ID, later sync returns DDF/AZD format)
        BigDecimal debt = item.getDebt() != null ? item.getDebt() : BigDecimal.ZERO;
        if (deductionInvoiceRepository.existsByStoreIdAndDebtAndTransactionDateAndTransactionType(
                store.getId(), debt, transactionDate, item.getTransactionType())) {
            log.debug("Skipping duplicate invoice by content match: {} (debt={}, date={}, type={})",
                    item.getId(), debt, transactionDate, item.getTransactionType());
            return 0;
        }

        LocalDateTime paymentDate = item.getPaymentDate() != null
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(item.getPaymentDate()), ZoneId.of("Europe/Istanbul"))
                : null;

        // For "Kargo Fatura" records, the 'id' field IS the invoiceSerialNumber
        // according to Trendyol API documentation
        String invoiceSerialNum = item.getInvoiceSerialNumber();
        if (isCargoInvoiceType(item.getTransactionType()) && item.getId() != null) {
            invoiceSerialNum = item.getId();
            log.debug("Kargo Fatura record: using id '{}' as invoiceSerialNumber", item.getId());
        }

        // For "Komisyon Faturası" records, the 'id' field also contains the invoiceSerialNumber
        // when invoiceSerialNumber is empty (similar to Kargo Fatura behavior)
        if (isCommissionInvoiceType(item.getTransactionType()) && item.getId() != null
                && (invoiceSerialNum == null || invoiceSerialNum.isEmpty())) {
            invoiceSerialNum = item.getId();
            log.debug("Komisyon Faturası record: using id '{}' as invoiceSerialNumber", item.getId());
        }

        // For CEZA (penalty) records, use 'id' as invoiceSerialNumber when not provided
        if (isPenaltyInvoiceType(item.getTransactionType()) && item.getId() != null
                && (invoiceSerialNum == null || invoiceSerialNum.isEmpty())) {
            invoiceSerialNum = item.getId();
            log.debug("CEZA record: using id '{}' as invoiceSerialNumber for type '{}'",
                    item.getId(), item.getTransactionType());
        }

        // For KESINTI/DIGER (deduction) records, use 'id' as invoiceSerialNumber when not provided
        if (isDeductionInvoiceType(item.getTransactionType()) && item.getId() != null
                && (invoiceSerialNum == null || invoiceSerialNum.isEmpty())) {
            invoiceSerialNum = item.getId();
            log.debug("KESINTI record: using id '{}' as invoiceSerialNumber for type '{}'",
                    item.getId(), item.getTransactionType());
        }

        // For REKLAM (ad) records, use 'id' as invoiceSerialNumber when not provided
        // Covers: "Reklam Bedeli", "Komisyonlu İnfluencer Reklam Bedeli"
        if (isAdInvoiceType(item.getTransactionType()) && item.getId() != null
                && (invoiceSerialNum == null || invoiceSerialNum.isEmpty())) {
            invoiceSerialNum = item.getId();
            log.debug("REKLAM record: using id '{}' as invoiceSerialNumber for type '{}'",
                    item.getId(), item.getTransactionType());
        }

        // For international operation (ULUSLARARASI/IADE) records, use 'id' as invoiceSerialNumber when not provided
        // Covers: "Yurtdışı Operasyon Iade Bedeli", "AZ-Yurtdışı Operasyon Bedeli", corrupted variants
        if (isInternationalOperationInvoiceType(item.getTransactionType()) && item.getId() != null
                && (invoiceSerialNum == null || invoiceSerialNum.isEmpty())) {
            invoiceSerialNum = item.getId();
            log.debug("ULUSLARARASI/IADE record: using id '{}' as invoiceSerialNumber for type '{}'",
                    item.getId(), item.getTransactionType());
        }

        // For international service fee (ULUSLARARASI HIZMET) records, use 'id' as invoiceSerialNumber when not provided
        // Covers: "Uluslararası Hizmet Bedeli", "AZ-Uluslararası Hizmet Bedeli"
        // Note: This is different from international operation types above
        if (isInternationalServiceFeeType(item.getTransactionType()) && item.getId() != null
                && (invoiceSerialNum == null || invoiceSerialNum.isEmpty())) {
            invoiceSerialNum = item.getId();
            log.debug("ULUSLARARASI HIZMET record: using id '{}' as invoiceSerialNumber for type '{}'",
                    item.getId(), item.getTransactionType());
        }

        // UNIVERSAL FALLBACK: For any remaining types where invoiceSerialNumber is still null/empty,
        // use the Trendyol ID as fallback. This prevents NULL invoice_serial_number in database
        // and ensures all invoice types are properly tracked.
        // Examples: "Eksik Ürün Faturası", "Yanlış Ürün Faturası", and any future types
        if ((invoiceSerialNum == null || invoiceSerialNum.isEmpty()) && item.getId() != null) {
            invoiceSerialNum = item.getId();
            log.debug("FALLBACK: using id '{}' as invoiceSerialNumber for unhandled type '{}'",
                    item.getId(), item.getTransactionType());
        }

        // Fix transaction type: Trendyol API returns "Yurt Dışı Operasyon Bedeli"
        // but it should be "Yurtdışı Operasyon Iade Bedeli" (as shown in Trendyol Excel export)
        // Also normalize corrupted Turkish characters from API (since Dec 30, 2025 update)
        String correctedTransactionType = normalizeTransactionType(
                correctTransactionType(item.getTransactionType())
        );

        TrendyolDeductionInvoice invoice = TrendyolDeductionInvoice.builder()
                .storeId(store.getId())
                .trendyolId(item.getId())
                .transactionDate(transactionDate)
                .transactionType(correctedTransactionType)
                .description(item.getDescription())
                .debt(item.getDebt() != null ? item.getDebt() : BigDecimal.ZERO)
                .credit(item.getCredit() != null ? item.getCredit() : BigDecimal.ZERO)
                .invoiceSerialNumber(invoiceSerialNum)
                .officialSerialNumber(item.getInvoiceSerialNumber())
                .paymentOrderId(item.getPaymentOrderId())
                .paymentDate(paymentDate)
                .orderNumber(item.getOrderNumber())
                .shipmentPackageId(item.getShipmentPackageId())
                .sellerId(item.getSellerId())
                .currency("TRY")
                .affiliate("TRENDYOLTR")
                .build();

        deductionInvoiceRepository.save(invoice);

        log.debug("Saved deduction invoice: {} - type: {} - amount: {}",
                item.getId(), item.getTransactionType(), item.getDebt());

        return 1;
    }

    /**
     * Updates orders with real shipping costs from cargo invoices.
     * Filters by 'Gönderi Kargo Bedeli' for outbound shipping cost.
     * Also updates return shipping costs from 'İade Kargo Bedeli' invoices.
     *
     * @param storeId The store ID
     * @return Number of orders updated (outbound shipping)
     */
    @Transactional
    public int updateOrdersWithRealShipping(UUID storeId) {
        log.info("Updating orders with real shipping costs for store: {}", storeId);

        // Update outbound shipping costs (Gönderi Kargo Bedeli)
        int outboundCount = orderRepository.updateOrdersWithCargoInvoices(storeId);
        log.info("Updated {} orders with real outbound shipping costs for store: {}", outboundCount, storeId);

        // Update return shipping costs (İade Kargo Bedeli)
        int returnCount = orderRepository.updateOrdersWithReturnCargoInvoices(storeId);
        log.info("Updated {} orders with real return shipping costs for store: {}", returnCount, storeId);

        // Recalculate estimates for remaining orders using product reference data
        int estimatedCount = orderRepository.recalculateEstimatedShipping(storeId);
        log.info("Recalculated estimated shipping for {} orders using product reference data for store: {}", estimatedCount, storeId);

        return outboundCount;
    }

    /**
     * Sync result DTO for returning sync statistics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncResult {
        private int stoppagesCount;
        private int paymentOrdersCount;
        private int cargoInvoicesCount;
        private int deductionInvoicesCount;
        private int returnInvoicesCount;
        private int cashAdvanceCount;
        private int wireTransferCount;
        private int incomingTransferCount;
        private int commissionAgreementCount;
        private int financialItemCount;
        private boolean success;
        private String errorMessage;
    }

    // ============== Generic OtherFinancials Sync Methods ==============
    // For CashAdvance, WireTransfer, IncomingTransfer, CommissionAgreementInvoice, FinancialItem

    /**
     * Generic sync method for additional OtherFinancials transaction types.
     * Stores records in trendyol_deduction_invoices table with their transactionType preserved.
     * Supports: CashAdvance, WireTransfer, IncomingTransfer, CommissionAgreementInvoice, FinancialItem
     */
    private int syncGenericOtherFinancialsInternal(Store store, TrendyolCredentials credentials,
                                                    LocalDate startDate, LocalDate endDate, String transactionType) {
        log.info("Syncing {} for store: {} from {} to {}", transactionType, store.getId(), startDate, endDate);

        // Trendyol API has a 15-day limit on date range
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween > 14) {
            int totalSavedCount = 0;
            LocalDate chunkStart = startDate;

            while (chunkStart.isBefore(endDate) || chunkStart.isEqual(endDate)) {
                LocalDate chunkEnd = chunkStart.plusDays(14);
                if (chunkEnd.isAfter(endDate)) {
                    chunkEnd = endDate;
                }

                totalSavedCount += syncGenericOtherFinancialsChunk(store, credentials, chunkStart, chunkEnd, transactionType);
                chunkStart = chunkEnd.plusDays(1);
            }

            log.info("Total saved {} {} records across all chunks for store: {}", totalSavedCount, transactionType, store.getId());
            return totalSavedCount;
        }

        return syncGenericOtherFinancialsChunk(store, credentials, startDate, endDate, transactionType);
    }

    private int syncGenericOtherFinancialsChunk(Store store, TrendyolCredentials credentials,
                                                 LocalDate startDate, LocalDate endDate, String transactionType) {
        HttpEntity<String> entity = createHttpEntity(credentials);
        long startTimestamp = startDate.atStartOfDay(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();
        long endTimestamp = endDate.plusDays(1).atStartOfDay(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();

        int currentPage = 0;
        int totalPages = 1;
        int savedCount = 0;

        while (currentPage < totalPages) {
            rateLimiter.acquire(store.getId());

            String url = TRENDYOL_BASE_URL + OTHER_FINANCIALS_ENDPOINT +
                    "?transactionType=" + transactionType +
                    "&startDate=" + startTimestamp +
                    "&endDate=" + endTimestamp +
                    "&page=" + currentPage +
                    "&size=1000";

            try {
                ResponseEntity<TrendyolOtherFinancialsResponse> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        TrendyolOtherFinancialsResponse.class,
                        credentials.getSellerId()
                );

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    TrendyolOtherFinancialsResponse data = response.getBody();

                    if (currentPage == 0) {
                        totalPages = data.getTotalPages() != null ? data.getTotalPages() : 1;
                        if (data.getTotalElements() != null && data.getTotalElements() > 0) {
                            log.info("Found {} total {} records across {} pages for store: {}",
                                    data.getTotalElements(), transactionType, totalPages, store.getId());
                        }
                    }

                    if (data.getContent() != null) {
                        for (TrendyolOtherFinancialsItem item : data.getContent()) {
                            savedCount += saveDeductionInvoice(store, item);
                        }
                    }
                } else {
                    log.warn("Failed to fetch {} page {} for store: {}", transactionType, currentPage, store.getId());
                    break;
                }

                currentPage++;
                Thread.sleep(200);

            } catch (Exception e) {
                log.error("Error fetching {} for store: {} - chunk {} to {}: {}",
                        transactionType, store.getId(), startDate, endDate, e.getMessage());
                break;
            }
        }

        if (savedCount > 0) {
            log.info("Saved {} {} records for chunk {} to {} for store: {}",
                    savedCount, transactionType, startDate, endDate, store.getId());
        }
        return savedCount;
    }

    // ============== ReturnInvoices Sync Methods ==============
    // These fetch TYE-prefixed credit invoices (iade/alacak faturaları)
    // Examples: Kurumsal Kampanya Yansıtma, MP Kargo İtiraz İade, Tedarikçi Faturası

    /**
     * Sync ReturnInvoices by storeId (wrapper for controller).
     * This fetches TYE-prefixed credit invoices (iade faturalar) - money returned TO the seller.
     * Unlike DeductionInvoices which are debits (borç), these are credits (alacak).
     */
    @Transactional
    public int syncReturnInvoices(UUID storeId, LocalDate startDate, LocalDate endDate) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));

        TrendyolCredentials credentials = extractTrendyolCredentials(store);
        if (credentials == null || credentials.getSellerId() == null) {
            throw InvalidStoreConfigurationException.missingCredentials(storeId.toString());
        }

        return syncReturnInvoicesInternal(store, credentials, startDate, endDate);
    }

    private int syncReturnInvoicesInternal(Store store, TrendyolCredentials credentials, LocalDate startDate, LocalDate endDate) {
        log.info("Syncing return invoices (TYE credits) for store: {} from {} to {}", store.getId(), startDate, endDate);

        // Trendyol API has a 15-day limit on date range
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween > 14) {
            log.info("Date range {} days exceeds 15-day limit for return invoices, splitting into chunks", daysBetween);
            int totalSavedCount = 0;
            LocalDate chunkStart = startDate;

            while (chunkStart.isBefore(endDate) || chunkStart.isEqual(endDate)) {
                LocalDate chunkEnd = chunkStart.plusDays(14);
                if (chunkEnd.isAfter(endDate)) {
                    chunkEnd = endDate;
                }

                log.info("Processing return invoices chunk: {} to {}", chunkStart, chunkEnd);
                totalSavedCount += syncReturnInvoicesChunk(store, credentials, chunkStart, chunkEnd);

                chunkStart = chunkEnd.plusDays(1);
            }

            log.info("Total saved {} return invoice records across all chunks for store: {}", totalSavedCount, store.getId());
            return totalSavedCount;
        }

        return syncReturnInvoicesChunk(store, credentials, startDate, endDate);
    }

    private int syncReturnInvoicesChunk(Store store, TrendyolCredentials credentials, LocalDate startDate, LocalDate endDate) {
        HttpEntity<String> entity = createHttpEntity(credentials);
        long startTimestamp = startDate.atStartOfDay(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();
        long endTimestamp = endDate.plusDays(1).atStartOfDay(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();

        int currentPage = 0;
        int totalPages = 1;
        int savedCount = 0;

        while (currentPage < totalPages) {
            rateLimiter.acquire(store.getId());

            // Use transactionType=ReturnInvoice for TYE-prefixed credit invoices
            // Size must be 500 or 1000 according to Trendyol API
            String url = TRENDYOL_BASE_URL + OTHER_FINANCIALS_ENDPOINT +
                    "?transactionType=ReturnInvoice" +
                    "&startDate=" + startTimestamp +
                    "&endDate=" + endTimestamp +
                    "&page=" + currentPage +
                    "&size=500";

            try {
                ResponseEntity<TrendyolOtherFinancialsResponse> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        TrendyolOtherFinancialsResponse.class,
                        credentials.getSellerId()
                );

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    TrendyolOtherFinancialsResponse data = response.getBody();

                    if (currentPage == 0) {
                        totalPages = data.getTotalPages() != null ? data.getTotalPages() : 1;
                        log.info("Found {} total ReturnInvoice records across {} pages for store: {}",
                                data.getTotalElements(), totalPages, store.getId());
                    }

                    if (data.getContent() != null) {
                        for (TrendyolOtherFinancialsItem item : data.getContent()) {
                            savedCount += saveReturnInvoice(store, item);
                        }
                    }
                } else {
                    log.warn("Failed to fetch return invoices page {} for store: {}", currentPage, store.getId());
                    break;
                }

                currentPage++;
                Thread.sleep(200);

            } catch (Exception e) {
                log.error("Error fetching return invoices for store: {} - chunk {} to {}: {}",
                        store.getId(), startDate, endDate, e.getMessage());
                break;
            }
        }

        log.info("Saved {} return invoice records for chunk {} to {} for store: {}",
                savedCount, startDate, endDate, store.getId());
        return savedCount;
    }

    /**
     * Save a return invoice (TYE credit) to the deduction_invoices table.
     * ReturnInvoices are credit transactions (alacak) vs DeductionInvoices which are debit (borç).
     */
    private int saveReturnInvoice(Store store, TrendyolOtherFinancialsItem item) {
        if (item.getId() == null || item.getId().isEmpty()) {
            return 0;
        }

        // Check if already exists by Trendyol ID
        if (deductionInvoiceRepository.existsByStoreIdAndTrendyolId(store.getId(), item.getId())) {
            return 0;
        }

        LocalDateTime transactionDate = item.getTransactionDate() != null
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(item.getTransactionDate()), ZoneId.of("Europe/Istanbul"))
                : LocalDateTime.now();

        // Additional check: Prevent duplicates when Trendyol returns different IDs for same invoice
        BigDecimal credit = item.getCredit() != null ? item.getCredit() : BigDecimal.ZERO;
        if (deductionInvoiceRepository.existsByStoreIdAndCreditAndTransactionDateAndTransactionType(
                store.getId(), credit, transactionDate, item.getTransactionType())) {
            log.debug("Skipping duplicate return invoice by content match: {} (credit={}, date={}, type={})",
                    item.getId(), credit, transactionDate, item.getTransactionType());
            return 0;
        }

        LocalDateTime paymentDate = item.getPaymentDate() != null
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(item.getPaymentDate()), ZoneId.of("Europe/Istanbul"))
                : null;

        // For ReturnInvoice, the 'id' IS the invoiceSerialNumber (TYE prefix)
        // Also available in commissionInvoiceSerialNumber field
        String invoiceSerialNum = item.getId();
        if (item.getInvoiceSerialNumber() != null && !item.getInvoiceSerialNumber().isEmpty()) {
            invoiceSerialNum = item.getInvoiceSerialNumber();
        }

        // Map generic "Tedarikçi Faturası" to more specific types based on description
        String transactionType = mapReturnInvoiceTransactionType(item.getTransactionType(), item.getDescription());

        TrendyolDeductionInvoice invoice = TrendyolDeductionInvoice.builder()
                .storeId(store.getId())
                .trendyolId(item.getId())
                .transactionDate(transactionDate)
                .transactionType(transactionType)
                .description(item.getDescription())
                .debt(item.getDebt() != null ? item.getDebt() : BigDecimal.ZERO)
                .credit(item.getCredit() != null ? item.getCredit() : BigDecimal.ZERO)
                .invoiceSerialNumber(invoiceSerialNum)
                .paymentOrderId(item.getPaymentOrderId())
                .paymentDate(paymentDate)
                .orderNumber(item.getOrderNumber())
                .shipmentPackageId(item.getShipmentPackageId())
                .sellerId(item.getSellerId())
                .currency("TRY")
                .affiliate("TRENDYOLTR")
                .build();

        deductionInvoiceRepository.save(invoice);

        log.debug("Saved return invoice: {} - type: {} - credit: {}",
                item.getId(), transactionType, item.getCredit());

        return 1;
    }

    /**
     * Map generic "Tedarikçi Faturası" transaction type to more specific types based on description.
     * This helps categorize TYE invoices properly for reporting.
     */
    private String mapReturnInvoiceTransactionType(String transactionType, String description) {
        if (description == null) {
            return transactionType;
        }

        String lowerDesc = description.toLowerCase(Locale.forLanguageTag("tr-TR"));

        // Kurumsal Kampanya Yansıtma Bedeli
        if (lowerDesc.contains("kurumsal") && (lowerDesc.contains("kampanya") || lowerDesc.contains("fatura"))) {
            return "Kurumsal Kampanya Yansıtma Bedeli";
        }

        // MP Kargo İtiraz İade Faturası
        if (lowerDesc.contains("kargo") && lowerDesc.contains("itiraz")) {
            return "MP Kargo İtiraz İade Faturası";
        }

        // TZM Tazmin Faturası
        if (lowerDesc.contains("tazmin") || lowerDesc.contains("tzm")) {
            return "Tazmin Faturası";
        }

        // Default to original transaction type
        return transactionType;
    }
}
