package com.ecommerce.sellerx.maintenance;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Repository for accessing pre-computed dashboard statistics from materialized views.
 * These views are refreshed every 15 minutes by DataMaintenanceScheduler.
 */
@Repository
@RequiredArgsConstructor
public class DashboardStatsRepository {

    private final JdbcTemplate jdbcTemplate;

    // ============================================
    // DAILY STATS QUERIES
    // ============================================

    /**
     * Get daily order stats for a store within a date range.
     * Uses mv_daily_order_stats materialized view.
     */
    public List<DailyOrderStats> getDailyStats(UUID storeId, LocalDate startDate, LocalDate endDate) {
        return jdbcTemplate.query(
            """
            SELECT period_date, order_count, unique_customers, total_revenue,
                   gross_amount, total_discount, total_ty_discount, total_commission,
                   total_coupon_discount, total_early_payment_fee,
                   delivered_count, cancelled_count, returned_count
            FROM mv_daily_order_stats
            WHERE store_id = ? AND period_date BETWEEN ? AND ?
            ORDER BY period_date DESC
            """,
            (rs, rowNum) -> new DailyOrderStats(
                rs.getDate("period_date").toLocalDate(),
                rs.getLong("order_count"),
                rs.getLong("unique_customers"),
                rs.getBigDecimal("total_revenue"),
                rs.getBigDecimal("gross_amount"),
                rs.getBigDecimal("total_discount"),
                rs.getBigDecimal("total_ty_discount"),
                rs.getBigDecimal("total_commission"),
                rs.getBigDecimal("total_coupon_discount"),
                rs.getBigDecimal("total_early_payment_fee"),
                rs.getLong("delivered_count"),
                rs.getLong("cancelled_count"),
                rs.getLong("returned_count")
            ),
            storeId, startDate, endDate
        );
    }

    /**
     * Get today's stats for a store.
     */
    public DailyOrderStats getTodayStats(UUID storeId) {
        List<DailyOrderStats> stats = getDailyStats(storeId, LocalDate.now(), LocalDate.now());
        return stats.isEmpty() ? DailyOrderStats.empty(LocalDate.now()) : stats.get(0);
    }

    // ============================================
    // MONTHLY STATS QUERIES
    // ============================================

    /**
     * Get monthly order stats for a store.
     * Uses mv_monthly_order_stats materialized view.
     */
    public List<MonthlyOrderStats> getMonthlyStats(UUID storeId, int months) {
        LocalDate startMonth = LocalDate.now().withDayOfMonth(1).minusMonths(months - 1);
        return jdbcTemplate.query(
            """
            SELECT period_month, order_count, unique_customers, total_revenue,
                   gross_amount, total_commission, delivered_count, cancelled_count, returned_count
            FROM mv_monthly_order_stats
            WHERE store_id = ? AND period_month >= ?
            ORDER BY period_month DESC
            """,
            (rs, rowNum) -> new MonthlyOrderStats(
                rs.getDate("period_month").toLocalDate(),
                rs.getLong("order_count"),
                rs.getLong("unique_customers"),
                rs.getBigDecimal("total_revenue"),
                rs.getBigDecimal("gross_amount"),
                rs.getBigDecimal("total_commission"),
                rs.getLong("delivered_count"),
                rs.getLong("cancelled_count"),
                rs.getLong("returned_count")
            ),
            storeId, startMonth
        );
    }

    // ============================================
    // CITY STATS QUERIES
    // ============================================

    /**
     * Get city-based sales stats for a store.
     * Uses mv_city_sales_stats materialized view.
     */
    public List<CitySalesStats> getCityStats(UUID storeId, LocalDate month) {
        return jdbcTemplate.query(
            """
            SELECT shipment_city, order_count, unique_customers, total_revenue
            FROM mv_city_sales_stats
            WHERE store_id = ? AND period_month = ?
            ORDER BY total_revenue DESC
            """,
            (rs, rowNum) -> new CitySalesStats(
                rs.getString("shipment_city"),
                rs.getLong("order_count"),
                rs.getLong("unique_customers"),
                rs.getBigDecimal("total_revenue")
            ),
            storeId, month
        );
    }

    /**
     * Get top N cities by revenue for a store in current month.
     */
    public List<CitySalesStats> getTopCities(UUID storeId, int limit) {
        LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);
        return jdbcTemplate.query(
            """
            SELECT shipment_city, order_count, unique_customers, total_revenue
            FROM mv_city_sales_stats
            WHERE store_id = ? AND period_month = ?
            ORDER BY total_revenue DESC
            LIMIT ?
            """,
            (rs, rowNum) -> new CitySalesStats(
                rs.getString("shipment_city"),
                rs.getLong("order_count"),
                rs.getLong("unique_customers"),
                rs.getBigDecimal("total_revenue")
            ),
            storeId, currentMonth, limit
        );
    }

    // ============================================
    // PRODUCT PERFORMANCE QUERIES
    // ============================================

    /**
     * Get top selling products for a store.
     * Uses mv_product_performance materialized view.
     */
    public List<ProductPerformance> getTopProducts(UUID storeId, int limit) {
        LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);
        return jdbcTemplate.query(
            """
            SELECT barcode, sale_count, total_quantity, total_revenue, total_commission
            FROM mv_product_performance
            WHERE store_id = ? AND period_month = ?
            ORDER BY total_revenue DESC
            LIMIT ?
            """,
            (rs, rowNum) -> new ProductPerformance(
                rs.getString("barcode"),
                rs.getLong("sale_count"),
                rs.getLong("total_quantity"),
                rs.getBigDecimal("total_revenue"),
                rs.getBigDecimal("total_commission")
            ),
            storeId, currentMonth, limit
        );
    }

    // ============================================
    // QUICK STATS (for dashboard overview)
    // ============================================

    /**
     * Get quick comparison stats using v_store_quick_stats view.
     */
    public QuickStats getQuickStats(UUID storeId) {
        return jdbcTemplate.queryForObject(
            """
            SELECT today_orders, today_revenue, yesterday_orders, yesterday_revenue,
                   month_orders, month_revenue
            FROM v_store_quick_stats
            WHERE store_id = ?
            """,
            (rs, rowNum) -> new QuickStats(
                rs.getLong("today_orders"),
                rs.getBigDecimal("today_revenue"),
                rs.getLong("yesterday_orders"),
                rs.getBigDecimal("yesterday_revenue"),
                rs.getLong("month_orders"),
                rs.getBigDecimal("month_revenue")
            ),
            storeId
        );
    }

    // ============================================
    // DTO RECORDS
    // ============================================

    public record DailyOrderStats(
        LocalDate date,
        long orderCount,
        long uniqueCustomers,
        BigDecimal totalRevenue,
        BigDecimal grossAmount,
        BigDecimal totalDiscount,
        BigDecimal totalTyDiscount,
        BigDecimal totalCommission,
        BigDecimal totalCouponDiscount,
        BigDecimal totalEarlyPaymentFee,
        long deliveredCount,
        long cancelledCount,
        long returnedCount
    ) {
        public static DailyOrderStats empty(LocalDate date) {
            return new DailyOrderStats(date, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, 0, 0, 0);
        }
    }

    public record MonthlyOrderStats(
        LocalDate month,
        long orderCount,
        long uniqueCustomers,
        BigDecimal totalRevenue,
        BigDecimal grossAmount,
        BigDecimal totalCommission,
        long deliveredCount,
        long cancelledCount,
        long returnedCount
    ) {}

    public record CitySalesStats(
        String city,
        long orderCount,
        long uniqueCustomers,
        BigDecimal totalRevenue
    ) {}

    public record ProductPerformance(
        String barcode,
        long saleCount,
        long totalQuantity,
        BigDecimal totalRevenue,
        BigDecimal totalCommission
    ) {}

    public record QuickStats(
        long todayOrders,
        BigDecimal todayRevenue,
        long yesterdayOrders,
        BigDecimal yesterdayRevenue,
        long monthOrders,
        BigDecimal monthRevenue
    ) {}
}
