package com.ecommerce.sellerx.orders;

import com.ecommerce.sellerx.orders.dto.*;
import com.ecommerce.sellerx.orders.dto.CustomerAnalyticsProjections.*;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
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
    private final TrendyolProductRepository productRepository;
    private final EntityManager entityManager;

    /**
     * Get full analytics summary: summary stats + segmentation + city analysis + monthly trend.
     */
    public CustomerAnalyticsResponse getAnalytics(UUID storeId) {
        SummaryWithOrderCountProjection summaryRaw = orderRepository.getCustomerAnalyticsSummaryWithOrderCount(storeId);
        Double avgRepeatInterval = orderRepository.getAvgRepeatIntervalDays(storeId);

        int totalCustomers = summaryRaw.getTotalCustomers() != null ? summaryRaw.getTotalCustomers() : 0;
        int repeatCustomers = summaryRaw.getRepeatCustomers() != null ? summaryRaw.getRepeatCustomers() : 0;
        BigDecimal totalRevenue = summaryRaw.getTotalRevenue() != null ? summaryRaw.getTotalRevenue() : BigDecimal.ZERO;
        BigDecimal repeatRevenue = summaryRaw.getRepeatRevenue() != null ? summaryRaw.getRepeatRevenue() : BigDecimal.ZERO;
        long totalOrders = summaryRaw.getTotalOrders() != null ? summaryRaw.getTotalOrders() : 0;

        // Calculate average order value
        BigDecimal avgOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        CustomerAnalyticsSummary summary = CustomerAnalyticsSummary.builder()
                .totalCustomers(totalCustomers)
                .repeatCustomers(repeatCustomers)
                .repeatRate(totalCustomers > 0 ? (double) repeatCustomers / totalCustomers * 100 : 0)
                .avgOrdersPerCustomer(summaryRaw.getAvgOrdersPerCustomer() != null ? summaryRaw.getAvgOrdersPerCustomer() : 0)
                .avgItemsPerCustomer(summaryRaw.getAvgItemsPerCustomer() != null ? summaryRaw.getAvgItemsPerCustomer() : 0)
                .avgItemsPerOrder(summaryRaw.getAvgItemsPerOrder() != null ? summaryRaw.getAvgItemsPerOrder() : 0)
                .avgRepeatIntervalDays(avgRepeatInterval != null ? avgRepeatInterval : 0)
                .totalRevenue(totalRevenue)
                .repeatCustomerRevenue(repeatRevenue)
                .repeatRevenueShare(totalRevenue.compareTo(BigDecimal.ZERO) > 0
                        ? repeatRevenue.multiply(BigDecimal.valueOf(100))
                        .divide(totalRevenue, 2, RoundingMode.HALF_UP).doubleValue()
                        : 0)
                .avgOrderValue(avgOrderValue)
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
     * Get paginated customer list with RFM scoring, filtering, and sorting.
     */
    public Map<String, Object> getCustomerList(UUID storeId, int page, int size, String sortBy, String sortDir, String search, CustomerListFilter filter) {
        // Fetch per-customer repeat interval data (needed for filtering)
        List<CustomerRepeatIntervalProjection> repeatIntervals = orderRepository.getPerCustomerRepeatIntervalDays(storeId);
        Map<Long, Double> repeatIntervalMap = repeatIntervals.stream()
                .filter(r -> r.getCustomerId() != null && r.getAvgDays() != null)
                .collect(Collectors.toMap(
                        CustomerRepeatIntervalProjection::getCustomerId,
                        CustomerRepeatIntervalProjection::getAvgDays,
                        (a, b) -> a
                ));

        // Build dynamic query
        String validSortDir = "asc".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";
        String sortColumn = mapSortField(sortBy);

        // Build the base query with CTE for repeat interval
        StringBuilder sql = new StringBuilder();
        sql.append("""
            WITH repeat_intervals AS (
                SELECT customer_id, AVG(days_diff) as avg_days
                FROM (
                    SELECT customer_id,
                           EXTRACT(EPOCH FROM (order_date - LAG(order_date) OVER (PARTITION BY customer_id ORDER BY order_date))) / 86400 as days_diff
                    FROM trendyol_orders
                    WHERE store_id = :storeId
                    AND customer_id IS NOT NULL
                    AND order_items IS NOT NULL
                    AND status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')
                ) sub
                WHERE days_diff IS NOT NULL
                GROUP BY customer_id
            ),
            customer_stats AS (
                SELECT
                    o.customer_id as customerId,
                    CONCAT(MAX(o.customer_first_name), ' ', MAX(o.customer_last_name)) as displayName,
                    MAX(o.shipment_city) as city,
                    COUNT(*) as orderCount,
                    COALESCE(SUM(
                        (SELECT COALESCE(SUM((item->>'quantity')::int), 0)
                         FROM jsonb_array_elements(o.order_items) AS item)
                    ), 0) as itemCount,
                    COALESCE(SUM(o.total_price), 0) as totalSpend,
                    MIN(o.order_date) as firstOrderDate,
                    MAX(o.order_date) as lastOrderDate,
                    ri.avg_days as avgRepeatDays
                FROM trendyol_orders o
                LEFT JOIN repeat_intervals ri ON o.customer_id = ri.customer_id
                WHERE o.store_id = :storeId
                AND o.customer_id IS NOT NULL
                AND o.order_items IS NOT NULL
                AND o.status IN ('Created', 'Picking', 'Invoiced', 'Shipped', 'Delivered', 'AtCollectionPoint', 'UnPacked')
                GROUP BY o.customer_id, ri.avg_days
            )
            SELECT * FROM customer_stats cs
            WHERE 1=1
            """);

        // Add search filter
        if (search != null && !search.isBlank()) {
            sql.append(" AND (LOWER(cs.displayName) LIKE LOWER(:search) OR LOWER(cs.city) LIKE LOWER(:search))");
        }

        // Add filters
        if (filter != null) {
            if (filter.getMinOrderCount() != null) {
                sql.append(" AND cs.orderCount >= :minOrderCount");
            }
            if (filter.getMaxOrderCount() != null) {
                sql.append(" AND cs.orderCount <= :maxOrderCount");
            }
            if (filter.getMinItemCount() != null) {
                sql.append(" AND cs.itemCount >= :minItemCount");
            }
            if (filter.getMaxItemCount() != null) {
                sql.append(" AND cs.itemCount <= :maxItemCount");
            }
            if (filter.getMinTotalSpend() != null) {
                sql.append(" AND cs.totalSpend >= :minTotalSpend");
            }
            if (filter.getMaxTotalSpend() != null) {
                sql.append(" AND cs.totalSpend <= :maxTotalSpend");
            }
            if (filter.getMinAvgOrderValue() != null) {
                sql.append(" AND (cs.totalSpend / NULLIF(cs.orderCount, 0)) >= :minAvgOrderValue");
            }
            if (filter.getMaxAvgOrderValue() != null) {
                sql.append(" AND (cs.totalSpend / NULLIF(cs.orderCount, 0)) <= :maxAvgOrderValue");
            }
            if (filter.getMinRepeatInterval() != null) {
                sql.append(" AND cs.avgRepeatDays >= :minRepeatInterval");
            }
            if (filter.getMaxRepeatInterval() != null) {
                sql.append(" AND cs.avgRepeatDays <= :maxRepeatInterval");
            }
        }

        // Add sorting
        if ("avgOrderValue".equals(sortBy)) {
            sql.append(" ORDER BY (cs.totalSpend / NULLIF(cs.orderCount, 0)) ").append(validSortDir).append(" NULLS LAST");
        } else if ("avgRepeatIntervalDays".equals(sortBy)) {
            sql.append(" ORDER BY cs.avgRepeatDays ").append(validSortDir).append(" NULLS LAST");
        } else {
            sql.append(" ORDER BY cs.").append(sortColumn).append(" ").append(validSortDir).append(" NULLS LAST");
        }

        // Count query (same filters, no pagination)
        String countSql = sql.toString().replaceFirst("SELECT \\* FROM customer_stats cs", "SELECT COUNT(*) FROM customer_stats cs");
        // Handle text block whitespace - try again with flexible whitespace if first replace didn't work
        if (countSql.contains("SELECT *")) {
            countSql = countSql.replaceFirst("\\s*SELECT \\* FROM customer_stats cs", " SELECT COUNT(*) FROM customer_stats cs");
        }
        // Remove ORDER BY - use specific pattern to avoid matching ORDER BY inside OVER clause
        int orderByIndex = countSql.lastIndexOf(" ORDER BY ");
        if (orderByIndex > 0) {
            countSql = countSql.substring(0, orderByIndex);
        }

        // Add pagination to main query
        sql.append(" LIMIT :limit OFFSET :offset");

        // Execute count query
        log.debug("Customer list count SQL: {}", countSql);
        Query countQuery = entityManager.createNativeQuery(countSql);
        countQuery.setParameter("storeId", storeId);
        if (search != null && !search.isBlank()) {
            countQuery.setParameter("search", "%" + search.trim() + "%");
        }
        setFilterParameters(countQuery, filter);
        long totalCount;
        try {
            totalCount = ((Number) countQuery.getSingleResult()).longValue();
            log.debug("Customer list count result: {}", totalCount);
        } catch (Exception e) {
            log.error("Error executing customer list count query for store {}: {}", storeId, e.getMessage(), e);
            throw e;
        }

        // Execute main query
        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("storeId", storeId);
        query.setParameter("limit", size);
        query.setParameter("offset", page * size);
        if (search != null && !search.isBlank()) {
            query.setParameter("search", "%" + search.trim() + "%");
        }
        setFilterParameters(query, filter);

        @SuppressWarnings("unchecked")
        List<Object[]> results;
        try {
            results = query.getResultList();
            log.debug("Customer list query returned {} results", results.size());
        } catch (Exception e) {
            log.error("Error executing customer list query for store {}: {}", storeId, e.getMessage(), e);
            throw e;
        }

        // Map results to CustomerListItem
        List<CustomerListItem> items = results.stream().map(row -> {
            Long customerId = row[0] != null ? ((Number) row[0]).longValue() : null;
            String displayName = (String) row[1];
            String city = (String) row[2];
            int orderCount = row[3] != null ? ((Number) row[3]).intValue() : 0;
            int itemCount = row[4] != null ? ((Number) row[4]).intValue() : 0;
            BigDecimal totalSpend = row[5] != null ? new BigDecimal(row[5].toString()) : BigDecimal.ZERO;
            LocalDateTime firstOrderDate = row[6] != null ? ((java.sql.Timestamp) row[6]).toLocalDateTime() : null;
            LocalDateTime lastOrderDate = row[7] != null ? ((java.sql.Timestamp) row[7]).toLocalDateTime() : null;
            Double avgRepeatDays = row[8] != null ? ((Number) row[8]).doubleValue() : null;

            // Calculate RFM scores
            int recencyScore = calculateRecencyScore(lastOrderDate);
            int frequencyScore = calculateFrequencyScore(orderCount);
            int monetaryScore = calculateMonetaryScore(totalSpend);
            String rfmSegment = determineRfmSegment(recencyScore, frequencyScore, monetaryScore);

            return CustomerListItem.builder()
                    .customerKey(customerId != null ? String.valueOf(customerId) : "")
                    .displayName(displayName != null ? displayName.trim() : "")
                    .city(city)
                    .orderCount(orderCount)
                    .itemCount(itemCount)
                    .totalSpend(totalSpend)
                    .firstOrderDate(firstOrderDate)
                    .lastOrderDate(lastOrderDate)
                    .avgOrderValue(orderCount > 0
                            ? totalSpend.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP).doubleValue()
                            : 0)
                    .rfmSegment(rfmSegment)
                    .recencyScore(recencyScore)
                    .frequencyScore(frequencyScore)
                    .monetaryScore(monetaryScore)
                    .avgRepeatIntervalDays(avgRepeatDays)
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

    private String mapSortField(String sortBy) {
        return switch (sortBy) {
            case "orderCount" -> "orderCount";
            case "itemCount" -> "itemCount";
            case "totalSpend" -> "totalSpend";
            case "avgOrderValue" -> "totalSpend"; // Will be handled specially
            case "avgRepeatIntervalDays" -> "avgRepeatDays"; // Will be handled specially
            default -> "totalSpend";
        };
    }

    private void setFilterParameters(Query query, CustomerListFilter filter) {
        if (filter == null) return;
        if (filter.getMinOrderCount() != null) {
            query.setParameter("minOrderCount", filter.getMinOrderCount());
        }
        if (filter.getMaxOrderCount() != null) {
            query.setParameter("maxOrderCount", filter.getMaxOrderCount());
        }
        if (filter.getMinItemCount() != null) {
            query.setParameter("minItemCount", filter.getMinItemCount());
        }
        if (filter.getMaxItemCount() != null) {
            query.setParameter("maxItemCount", filter.getMaxItemCount());
        }
        if (filter.getMinTotalSpend() != null) {
            query.setParameter("minTotalSpend", filter.getMinTotalSpend());
        }
        if (filter.getMaxTotalSpend() != null) {
            query.setParameter("maxTotalSpend", filter.getMaxTotalSpend());
        }
        if (filter.getMinAvgOrderValue() != null) {
            query.setParameter("minAvgOrderValue", filter.getMinAvgOrderValue());
        }
        if (filter.getMaxAvgOrderValue() != null) {
            query.setParameter("maxAvgOrderValue", filter.getMaxAvgOrderValue());
        }
        if (filter.getMinRepeatInterval() != null) {
            query.setParameter("minRepeatInterval", filter.getMinRepeatInterval());
        }
        if (filter.getMaxRepeatInterval() != null) {
            query.setParameter("maxRepeatInterval", filter.getMaxRepeatInterval());
        }
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

    /**
     * Get all orders for a specific customer.
     */
    public List<CustomerOrderDto> getCustomerOrders(UUID storeId, Long customerId) {
        List<TrendyolOrder> orders = orderRepository.findByStoreIdAndCustomerIdOrderByOrderDateDesc(storeId, customerId);

        // Collect all unique barcodes from order items
        List<String> allBarcodes = orders.stream()
                .filter(o -> o.getOrderItems() != null)
                .flatMap(o -> o.getOrderItems().stream())
                .map(OrderItem::getBarcode)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // Batch fetch products by barcode for image and URL
        Map<String, TrendyolProduct> productMap = new HashMap<>();
        if (!allBarcodes.isEmpty()) {
            productRepository.findByStoreIdAndBarcodeIn(storeId, allBarcodes)
                    .forEach(p -> productMap.put(p.getBarcode(), p));
        }

        return orders.stream().map(order -> mapOrderToDto(order, productMap)).collect(Collectors.toList());
    }

    /**
     * Get paginated orders for a specific customer.
     * Used for lazy loading in customer detail panel.
     */
    public CustomerOrdersPageDto getCustomerOrdersPaginated(UUID storeId, Long customerId, int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        List<TrendyolOrder> orders = orderRepository.findByStoreIdAndCustomerIdPaginated(storeId, customerId, pageable);
        long totalCount = orderRepository.countByStoreIdAndCustomerId(storeId, customerId);

        // Collect all unique barcodes from order items
        List<String> allBarcodes = orders.stream()
                .filter(o -> o.getOrderItems() != null)
                .flatMap(o -> o.getOrderItems().stream())
                .map(OrderItem::getBarcode)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // Batch fetch products by barcode for image and URL
        Map<String, TrendyolProduct> productMap = new HashMap<>();
        if (!allBarcodes.isEmpty()) {
            productRepository.findByStoreIdAndBarcodeIn(storeId, allBarcodes)
                    .forEach(p -> productMap.put(p.getBarcode(), p));
        }

        List<CustomerOrderDto> content = orders.stream().map(order -> mapOrderToDto(order, productMap)).collect(Collectors.toList());

        return CustomerOrdersPageDto.builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalCount)
                .totalPages((int) Math.ceil((double) totalCount / size))
                .hasNext((page + 1) * size < totalCount)
                .build();
    }

    /**
     * Map TrendyolOrder entity to CustomerOrderDto.
     */
    private CustomerOrderDto mapOrderToDto(TrendyolOrder order, Map<String, TrendyolProduct> productMap) {
        // Parse order items from JSONB
        List<CustomerOrderDto.CustomerOrderItemDto> items = order.getOrderItems() != null
                ? order.getOrderItems().stream().map(item -> {
                    // Calculate discount: unitPriceDiscount + unitPriceTyDiscount
                    BigDecimal discount = BigDecimal.ZERO;
                    if (item.getUnitPriceDiscount() != null) {
                        discount = discount.add(item.getUnitPriceDiscount());
                    }
                    if (item.getUnitPriceTyDiscount() != null) {
                        discount = discount.add(item.getUnitPriceTyDiscount());
                    }

                    // Look up product for image and URL
                    TrendyolProduct product = productMap.get(item.getBarcode());

                    return CustomerOrderDto.CustomerOrderItemDto.builder()
                            .barcode(item.getBarcode())
                            .productName(item.getProductName())
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPriceOrder())
                            .discount(discount)
                            .price(item.getPrice())
                            .image(product != null ? product.getImage() : null)
                            .productUrl(product != null ? product.getProductUrl() : null)
                            .build();
                }).collect(Collectors.toList())
                : Collections.emptyList();

        // Calculate total price and discount from items
        BigDecimal totalPrice = items.stream()
                .map(item -> item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDiscount = items.stream()
                .map(item -> item.getDiscount() != null ? item.getDiscount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CustomerOrderDto.builder()
                .orderId(order.getId().toString())
                .tyOrderNumber(order.getTyOrderNumber())
                .orderDate(order.getOrderDate())
                .status(order.getStatus())
                .totalPrice(totalPrice)
                .totalDiscount(totalDiscount)
                .shipmentCity(order.getShipmentCity())
                .items(items)
                .build();
    }

    /**
     * Get detailed product analytics for a specific product by barcode.
     * Returns product info + list of buyers who purchased this product.
     */
    public ProductDetailDto getProductDetail(UUID storeId, String barcode) {
        // Get product info from products table
        Optional<TrendyolProduct> productOpt = productRepository.findByStoreIdAndBarcode(storeId, barcode);
        String productName = productOpt.map(TrendyolProduct::getTitle).orElse("");
        String image = productOpt.map(TrendyolProduct::getImage).orElse(null);
        String productUrl = productOpt.map(TrendyolProduct::getProductUrl).orElse(null);

        // Get repeat stats for this product
        Object[] statsRaw = orderRepository.getProductRepeatStatsByBarcode(storeId, barcode);
        int totalBuyers = 0;
        int repeatBuyers = 0;
        int totalQuantitySold = 0;

        if (statsRaw != null && statsRaw.length > 0 && statsRaw[0] != null) {
            Object[] stats = (Object[]) statsRaw[0];
            if (stats.length >= 3) {
                totalBuyers = stats[0] != null ? ((Number) stats[0]).intValue() : 0;
                repeatBuyers = stats[1] != null ? ((Number) stats[1]).intValue() : 0;
                totalQuantitySold = stats[2] != null ? ((Number) stats[2]).intValue() : 0;
            }
        }

        // Get average repeat interval
        Double avgDays = orderRepository.getProductAvgRepeatIntervalDaysByBarcode(storeId, barcode);
        double avgDaysBetweenRepurchase = avgDays != null ? avgDays : 0.0;

        // Calculate repeat rate
        double repeatRate = totalBuyers > 0 ? (double) repeatBuyers / totalBuyers * 100 : 0;

        // Get list of buyers
        List<ProductBuyerProjection> buyersRaw = orderRepository.findProductBuyersByBarcode(storeId, barcode);
        List<ProductBuyerDto> buyers = buyersRaw.stream().map(b -> ProductBuyerDto.builder()
                .customerId(b.getCustomerId())
                .customerName(b.getCustomerName() != null ? b.getCustomerName().trim() : "")
                .city(b.getCity())
                .purchaseCount(b.getPurchaseCount() != null ? b.getPurchaseCount() : 0)
                .totalSpend(b.getTotalSpend() != null ? b.getTotalSpend() : BigDecimal.ZERO)
                .build()
        ).collect(Collectors.toList());

        return ProductDetailDto.builder()
                .barcode(barcode)
                .productName(productName)
                .totalBuyers(totalBuyers)
                .repeatBuyers(repeatBuyers)
                .repeatRate(Math.round(repeatRate * 10.0) / 10.0)
                .avgDaysBetweenRepurchase(Math.round(avgDaysBetweenRepurchase * 10.0) / 10.0)
                .totalQuantitySold(totalQuantitySold)
                .image(image)
                .productUrl(productUrl)
                .buyers(buyers)
                .build();
    }

    /**
     * Get paginated list of buyers for a specific product.
     * Used for lazy loading in product detail panel.
     */
    public ProductBuyersPageDto getProductBuyers(UUID storeId, String barcode, int page, int size) {
        int offset = page * size;
        List<ProductBuyerProjection> buyersRaw = orderRepository.findProductBuyersByBarcodePaginated(storeId, barcode, size, offset);
        long totalCount = orderRepository.countProductBuyersByBarcode(storeId, barcode);

        List<ProductBuyerDto> buyers = buyersRaw.stream().map(b -> ProductBuyerDto.builder()
                .customerId(b.getCustomerId())
                .customerName(b.getCustomerName() != null ? b.getCustomerName().trim() : "")
                .city(b.getCity())
                .purchaseCount(b.getPurchaseCount() != null ? b.getPurchaseCount() : 0)
                .totalSpend(b.getTotalSpend() != null ? b.getTotalSpend() : BigDecimal.ZERO)
                .build()
        ).collect(Collectors.toList());

        return ProductBuyersPageDto.builder()
                .content(buyers)
                .page(page)
                .size(size)
                .totalElements(totalCount)
                .totalPages((int) Math.ceil((double) totalCount / size))
                .hasNext((page + 1) * size < totalCount)
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
