package com.ecommerce.sellerx.orders;

import com.ecommerce.sellerx.orders.dto.*;
import com.ecommerce.sellerx.orders.dto.CustomerAnalyticsProjections.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Lifecycle stage labels, icons, and colors mapping.
 */
class LifecycleStageConfig {
    static final Map<String, String> LABELS = Map.of(
            "new", "Yeni",
            "active", "Aktif",
            "loyal", "Sadık",
            "at_risk", "Risk Altında",
            "dormant", "Uyuyan",
            "lost", "Kayıp",
            "other", "Diğer"
    );

    static final Map<String, String> ICONS = Map.of(
            "new", "sparkles",
            "active", "flame",
            "loyal", "gem",
            "at_risk", "alert-triangle",
            "dormant", "moon",
            "lost", "x-circle",
            "other", "help-circle"
    );

    static final Map<String, String> COLORS = Map.of(
            "new", "#22c55e",
            "active", "#3b82f6",
            "loyal", "#8b5cf6",
            "at_risk", "#eab308",
            "dormant", "#f97316",
            "lost", "#ef4444",
            "other", "#6b7280"
    );
}

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerAnalyticsService {

    private final TrendyolOrderRepository orderRepository;
    private final TrendyolOrderService orderService;

    /**
     * Get full analytics summary: summary stats + segmentation + city analysis + monthly trend.
     */
    public CustomerAnalyticsResponse getAnalytics(UUID storeId) {
        SummaryProjection summaryRaw = orderRepository.getCustomerAnalyticsSummary(storeId);
        Double avgRepeatInterval = orderRepository.getAvgRepeatIntervalDays(storeId);

        int totalCustomers = summaryRaw.getTotalCustomers() != null ? summaryRaw.getTotalCustomers() : 0;
        int repeatCustomers = summaryRaw.getRepeatCustomers() != null ? summaryRaw.getRepeatCustomers() : 0;
        BigDecimal totalRevenue = summaryRaw.getTotalRevenue() != null ? summaryRaw.getTotalRevenue() : BigDecimal.ZERO;
        BigDecimal repeatRevenue = summaryRaw.getRepeatRevenue() != null ? summaryRaw.getRepeatRevenue() : BigDecimal.ZERO;

        CustomerAnalyticsSummary summary = CustomerAnalyticsSummary.builder()
                .totalCustomers(totalCustomers)
                .repeatCustomers(repeatCustomers)
                .repeatRate(totalCustomers > 0 ? (double) repeatCustomers / totalCustomers * 100 : 0)
                .avgOrdersPerCustomer(summaryRaw.getAvgOrdersPerCustomer() != null ? summaryRaw.getAvgOrdersPerCustomer() : 0)
                .avgItemsPerCustomer(summaryRaw.getAvgItemsPerCustomer() != null ? summaryRaw.getAvgItemsPerCustomer() : 0)
                .avgRepeatIntervalDays(avgRepeatInterval != null ? avgRepeatInterval : 0)
                .totalRevenue(totalRevenue)
                .repeatCustomerRevenue(repeatRevenue)
                .repeatRevenueShare(totalRevenue.compareTo(BigDecimal.ZERO) > 0
                        ? repeatRevenue.multiply(BigDecimal.valueOf(100))
                        .divide(totalRevenue, 2, RoundingMode.HALF_UP).doubleValue()
                        : 0)
                .build();

        // Segmentation
        List<SegmentProjection> segmentRaw = orderRepository.getCustomerSegmentation(storeId);
        List<SegmentData> segmentation = segmentRaw.stream().map(s -> SegmentData.builder()
                .segment(s.getSegment())
                .customerCount(s.getCustomerCount() != null ? s.getCustomerCount() : 0)
                .totalRevenue(s.getTotalRevenue() != null ? s.getTotalRevenue() : BigDecimal.ZERO)
                .percentage(totalCustomers > 0
                        ? (double) (s.getCustomerCount() != null ? s.getCustomerCount() : 0) / totalCustomers * 100
                        : 0)
                .build()).collect(Collectors.toList());

        // City analysis
        List<CityRepeatProjection> cityRaw = orderRepository.getCityRepeatAnalysis(storeId);
        List<CityRepeatData> cityAnalysis = cityRaw.stream().map(c -> {
            int cityTotal = c.getTotalCustomers() != null ? c.getTotalCustomers() : 0;
            int cityRepeat = c.getRepeatCustomers() != null ? c.getRepeatCustomers() : 0;
            return CityRepeatData.builder()
                    .city(c.getCity())
                    .totalCustomers(cityTotal)
                    .repeatCustomers(cityRepeat)
                    .repeatRate(cityTotal > 0 ? (double) cityRepeat / cityTotal * 100 : 0)
                    .totalRevenue(c.getTotalRevenue() != null ? c.getTotalRevenue() : BigDecimal.ZERO)
                    .build();
        }).collect(Collectors.toList());

        // Monthly trend
        List<MonthlyTrendProjection> trendRaw = orderRepository.getMonthlyNewVsRepeatTrend(storeId);
        List<MonthlyTrend> monthlyTrend = trendRaw.stream().map(t -> MonthlyTrend.builder()
                .month(t.getMonth())
                .newCustomers(t.getNewCustomers() != null ? t.getNewCustomers() : 0)
                .repeatCustomers(t.getRepeatCustomers() != null ? t.getRepeatCustomers() : 0)
                .newRevenue(t.getNewRevenue() != null ? t.getNewRevenue() : BigDecimal.ZERO)
                .repeatRevenue(t.getRepeatRevenue() != null ? t.getRepeatRevenue() : BigDecimal.ZERO)
                .build()).collect(Collectors.toList());

        return CustomerAnalyticsResponse.builder()
                .summary(summary)
                .segmentation(segmentation)
                .cityAnalysis(cityAnalysis)
                .monthlyTrend(monthlyTrend)
                .build();
    }

    /**
     * Get paginated customer list with RFM scoring and optional search.
     */
    public Map<String, Object> getCustomerList(UUID storeId, int page, int size, String sort, String search) {
        int offset = page * size;
        List<CustomerSummaryProjection> customers;
        long totalCount;

        if (search != null && !search.isBlank()) {
            customers = orderRepository.findCustomerSummariesWithSearch(storeId, search.trim(), size, offset);
            totalCount = orderRepository.countDistinctCustomersWithSearch(storeId, search.trim());
        } else {
            customers = orderRepository.findCustomerSummaries(storeId, size, offset);
            totalCount = orderRepository.countDistinctCustomers(storeId);
        }

        // Calculate RFM scores for this page
        // RFM Frequency uses orderCount (number of orders placed) to measure return behavior
        List<CustomerListItem> items = customers.stream().map(c -> {
            int orderCount = c.getOrderCount() != null ? c.getOrderCount() : 0;
            int itemCount = c.getItemCount() != null ? c.getItemCount() : 0;
            BigDecimal totalSpend = c.getTotalSpend() != null ? c.getTotalSpend() : BigDecimal.ZERO;
            LocalDateTime lastOrder = c.getLastOrderDate();

            // Simple RFM scoring (1-5 scale) - Frequency uses orderCount (return behavior)
            int recencyScore = calculateRecencyScore(lastOrder);
            int frequencyScore = calculateFrequencyScore(orderCount);
            int monetaryScore = calculateMonetaryScore(totalSpend);
            String rfmSegment = determineRfmSegment(recencyScore, frequencyScore, monetaryScore);

            return CustomerListItem.builder()
                    .customerKey(c.getCustomerId() != null ? String.valueOf(c.getCustomerId()) : "")
                    .displayName(c.getDisplayName() != null ? c.getDisplayName().trim() : "")
                    .city(c.getCity())
                    .orderCount(orderCount)
                    .itemCount(itemCount)
                    .totalSpend(totalSpend)
                    .firstOrderDate(c.getFirstOrderDate())
                    .lastOrderDate(lastOrder)
                    .avgOrderValue(orderCount > 0
                            ? totalSpend.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP).doubleValue()
                            : 0)
                    .rfmSegment(rfmSegment)
                    .recencyScore(recencyScore)
                    .frequencyScore(frequencyScore)
                    .monetaryScore(monetaryScore)
                    .build();
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("content", items);
        result.put("totalElements", totalCount);
        result.put("totalPages", (int) Math.ceil((double) totalCount / size));
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    /**
     * Get product repeat buyer analysis with avg days between repurchase.
     */
    public List<ProductRepeatData> getProductRepeatAnalysis(UUID storeId) {
        List<ProductRepeatProjection> products = orderRepository.getProductRepeatAnalysis(storeId);
        List<Object[]> avgDaysRaw = orderRepository.getProductAvgRepeatIntervalDays(storeId);

        // Build barcode -> avgDays map
        Map<String, Double> avgDaysMap = new HashMap<>();
        for (Object[] row : avgDaysRaw) {
            String barcode = (String) row[0];
            Double avgDays = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            avgDaysMap.put(barcode, avgDays);
        }

        return products.stream().map(p -> {
            int totalBuyers = p.getTotalBuyers() != null ? p.getTotalBuyers() : 0;
            int repeatBuyers = p.getRepeatBuyers() != null ? p.getRepeatBuyers() : 0;
            return ProductRepeatData.builder()
                    .barcode(p.getBarcode())
                    .productName(p.getProductName())
                    .totalBuyers(totalBuyers)
                    .repeatBuyers(repeatBuyers)
                    .repeatRate(totalBuyers > 0 ? (double) repeatBuyers / totalBuyers * 100 : 0)
                    .avgDaysBetweenRepurchase(avgDaysMap.getOrDefault(p.getBarcode(), 0.0))
                    .totalQuantitySold(p.getTotalQuantitySold() != null ? p.getTotalQuantitySold() : 0)
                    .image(p.getImage())
                    .productUrl(p.getProductUrl())
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * Get cross-sell analysis.
     */
    public List<CrossSellData> getCrossSellAnalysis(UUID storeId) {
        List<CrossSellProjection> pairs = orderRepository.getCrossSellAnalysis(storeId);
        long totalCustomers = orderRepository.countDistinctCustomers(storeId);

        return pairs.stream().map(p -> CrossSellData.builder()
                .sourceBarcode(p.getSourceBarcode())
                .sourceProductName(p.getSourceProductName())
                .targetBarcode(p.getTargetBarcode())
                .targetProductName(p.getTargetProductName())
                .coOccurrenceCount(p.getCoOccurrenceCount() != null ? p.getCoOccurrenceCount() : 0)
                .confidence(totalCustomers > 0
                        ? (double) (p.getCoOccurrenceCount() != null ? p.getCoOccurrenceCount() : 0) / totalCustomers * 100
                        : 0)
                .sourceImage(p.getSourceImage())
                .sourceProductUrl(p.getSourceProductUrl())
                .targetImage(p.getTargetImage())
                .targetProductUrl(p.getTargetProductUrl())
                .build()).collect(Collectors.toList());
    }

    /**
     * Get backfill coverage stats.
     */
    public Map<String, Object> getBackfillCoverage(UUID storeId) {
        BackfillCoverageProjection coverage = orderRepository.getCustomerDataCoverage(storeId);
        long total = coverage.getTotalOrders() != null ? coverage.getTotalOrders() : 0;
        long withData = coverage.getOrdersWithCustomerData() != null ? coverage.getOrdersWithCustomerData() : 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalOrders", total);
        result.put("ordersWithCustomerData", withData);
        result.put("coveragePercent", total > 0 ? Math.round((double) withData / total * 1000.0) / 10.0 : 0);
        result.put("ordersWithoutCustomerData", total - withData);
        result.put("note", "Müşteri analitiği artık customer_id (sabit numerik kimlik) bazlıdır. " +
                "Trendyol Order API sadece son ~3 aylık siparişlerin müşteri bilgisini sağlar. " +
                "Daha eski siparişler hakediş verisinden geldiği için customer_id içermez. " +
                "Yeni siparişler her senkronizasyonda otomatik olarak customer_id ile kaydedilir.");
        return result;
    }

    /**
     * Trigger async backfill of customer data for existing orders.
     * Fetches all available orders from Trendyol API (without date filter)
     * to maximize customer data coverage.
     */
    @Async
    public void triggerCustomerDataBackfill(UUID storeId) {
        log.info("Starting customer data backfill for store {}", storeId);
        try {
            orderService.fetchAllOrdersForCustomerBackfill(storeId);
            log.info("Customer data backfill completed for store {}", storeId);
        } catch (Exception e) {
            log.error("Customer data backfill failed for store {}", storeId, e);
        }
    }

    /**
     * Get customer lifecycle stages distribution.
     */
    public List<LifecycleStageData> getLifecycleStages(UUID storeId) {
        List<LifecycleStageProjection> raw = orderRepository.getCustomerLifecycleStages(storeId);

        // Calculate total customers for percentage
        int totalCustomers = raw.stream()
                .mapToInt(r -> r.getCustomerCount() != null ? r.getCustomerCount() : 0)
                .sum();

        return raw.stream().map(r -> {
            String stage = r.getLifecycleStage();
            int count = r.getCustomerCount() != null ? r.getCustomerCount() : 0;
            BigDecimal revenue = r.getTotalRevenue() != null ? r.getTotalRevenue() : BigDecimal.ZERO;

            return LifecycleStageData.builder()
                    .stage(stage)
                    .label(LifecycleStageConfig.LABELS.getOrDefault(stage, stage))
                    .icon(LifecycleStageConfig.ICONS.getOrDefault(stage, "help-circle"))
                    .color(LifecycleStageConfig.COLORS.getOrDefault(stage, "#6b7280"))
                    .customerCount(count)
                    .totalRevenue(revenue)
                    .percentage(totalCustomers > 0 ? (double) count / totalCustomers * 100 : 0)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * Get cohort retention analysis.
     */
    public List<CohortData> getCohortAnalysis(UUID storeId) {
        List<CohortProjection> raw = orderRepository.getCohortAnalysis(storeId);

        // Group by cohort month
        Map<String, List<CohortProjection>> byCohort = raw.stream()
                .collect(Collectors.groupingBy(CohortProjection::getCohortMonth));

        List<CohortData> result = new ArrayList<>();

        for (Map.Entry<String, List<CohortProjection>> entry : byCohort.entrySet()) {
            String cohortMonth = entry.getKey();
            List<CohortProjection> monthData = entry.getValue();

            // Find cohort size (customers in the cohort month itself)
            int cohortSize = monthData.stream()
                    .filter(m -> m.getOrderMonth().equals(cohortMonth))
                    .mapToInt(m -> m.getActiveCustomers() != null ? m.getActiveCustomers() : 0)
                    .findFirst()
                    .orElse(0);

            if (cohortSize == 0) continue;

            // Sort month data by order month
            monthData.sort(Comparator.comparing(CohortProjection::getOrderMonth));

            // Calculate retention rates with M1, M2, M3... keys (relative months from cohort)
            Map<String, Double> retentionRates = new LinkedHashMap<>();
            int monthIndex = 1;
            for (CohortProjection m : monthData) {
                int active = m.getActiveCustomers() != null ? m.getActiveCustomers() : 0;
                double rate = (double) active / cohortSize * 100;
                // Use M1, M2, M3... format (M1 is the cohort month itself)
                retentionRates.put("M" + monthIndex, Math.round(rate * 10.0) / 10.0);
                monthIndex++;
            }

            result.add(CohortData.builder()
                    .cohortMonth(cohortMonth)
                    .cohortSize(cohortSize)
                    .retentionRates(retentionRates)
                    .build());
        }

        // Sort by cohort month
        result.sort(Comparator.comparing(CohortData::getCohortMonth));
        return result;
    }

    /**
     * Get purchase frequency distribution.
     */
    public List<FrequencyDistributionData> getFrequencyDistribution(UUID storeId) {
        List<FrequencyDistributionProjection> raw = orderRepository.getPurchaseFrequencyDistribution(storeId);

        // Calculate totals
        int totalCustomers = raw.stream()
                .mapToInt(r -> r.getCustomerCount() != null ? r.getCustomerCount() : 0)
                .sum();
        BigDecimal totalRevenue = raw.stream()
                .map(r -> r.getTotalRevenue() != null ? r.getTotalRevenue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return raw.stream().map(r -> {
            int count = r.getCustomerCount() != null ? r.getCustomerCount() : 0;
            BigDecimal revenue = r.getTotalRevenue() != null ? r.getTotalRevenue() : BigDecimal.ZERO;
            int orders = r.getTotalOrders() != null ? r.getTotalOrders() : 0;

            double percentage = totalCustomers > 0 ? (double) count / totalCustomers * 100 : 0;
            double revenueShare = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                    ? revenue.multiply(BigDecimal.valueOf(100))
                    .divide(totalRevenue, 2, RoundingMode.HALF_UP).doubleValue()
                    : 0;

            return FrequencyDistributionData.builder()
                    .bucket(r.getFrequencyBucket())
                    .customerCount(count)
                    .totalRevenue(revenue)
                    .totalOrders(orders)
                    .percentage(Math.round(percentage * 10.0) / 10.0)
                    .revenueShare(Math.round(revenueShare * 10.0) / 10.0)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * Get CLV summary statistics.
     */
    public ClvSummaryData getClvSummary(UUID storeId) {
        ClvSummaryProjection raw = orderRepository.getClvSummary(storeId);

        return ClvSummaryData.builder()
                .avgClv(raw.getAvgClv() != null ? raw.getAvgClv().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .medianClv(raw.getMedianClv() != null ? raw.getMedianClv().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .top10PercentClv(raw.getTop10PercentClv() != null ? raw.getTop10PercentClv().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .top10PercentRevenueShare(raw.getTop10PercentRevenueShare() != null
                        ? Math.round(raw.getTop10PercentRevenueShare().doubleValue() * 10.0) / 10.0
                        : 0)
                .build();
    }

    // ============== RFM Scoring Helpers ==============

    private int calculateRecencyScore(LocalDateTime lastOrderDate) {
        if (lastOrderDate == null) return 1;
        long daysSince = java.time.Duration.between(lastOrderDate, LocalDateTime.now()).toDays();
        if (daysSince <= 7) return 5;
        if (daysSince <= 30) return 4;
        if (daysSince <= 60) return 3;
        if (daysSince <= 90) return 2;
        return 1;
    }

    /**
     * Calculate RFM Frequency score based on number of orders placed (return behavior).
     * Thresholds: 10+orders=5, 6+orders=4, 4+orders=3, 2+orders=2, 1order=1
     */
    private int calculateFrequencyScore(int orderCount) {
        if (orderCount >= 10) return 5;  // Very loyal - 10+ orders
        if (orderCount >= 6) return 4;   // Loyal - 6-9 orders
        if (orderCount >= 4) return 3;   // Regular - 4-5 orders
        if (orderCount >= 2) return 2;   // Returning - 2-3 orders
        return 1;                         // New - 1 order
    }

    private int calculateMonetaryScore(BigDecimal totalSpend) {
        if (totalSpend == null) return 1;
        double amount = totalSpend.doubleValue();
        if (amount >= 5000) return 5;
        if (amount >= 2000) return 4;
        if (amount >= 1000) return 3;
        if (amount >= 500) return 2;
        return 1;
    }

    private String determineRfmSegment(int r, int f, int m) {
        double avg = (r + f + m) / 3.0;
        if (r >= 4 && f >= 4 && m >= 4) return "Şampiyonlar";
        if (r >= 4 && f >= 3) return "Sadık Müşteriler";
        if (r >= 4 && f <= 2) return "Yeni Müşteriler";
        if (r >= 3 && f >= 3) return "Potansiyel Sadık";
        if (r <= 2 && f >= 3) return "Risk Altında";
        if (r <= 2 && f <= 2 && m >= 3) return "Kaybedilmek Üzere";
        if (r <= 2 && f <= 2) return "Kayıp";
        return "Diğer";
    }
}
