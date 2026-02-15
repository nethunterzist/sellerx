package com.ecommerce.sellerx.dashboard;

import com.ecommerce.sellerx.expenses.ExpenseFrequency;
import com.ecommerce.sellerx.expenses.StoreExpense;
import com.ecommerce.sellerx.expenses.StoreExpenseRepository;
import com.ecommerce.sellerx.financial.TrendyolCargoInvoice;
import com.ecommerce.sellerx.financial.TrendyolCargoInvoiceRepository;
import com.ecommerce.sellerx.financial.TrendyolDeductionInvoiceRepository;
import com.ecommerce.sellerx.financial.TrendyolInvoice;
import com.ecommerce.sellerx.financial.TrendyolInvoiceRepository;
import com.ecommerce.sellerx.orders.OrderItem;
import com.ecommerce.sellerx.orders.TrendyolOrder;
import com.ecommerce.sellerx.orders.TrendyolOrderRepository;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import com.ecommerce.sellerx.returns.ReturnRecordRepository;
import com.ecommerce.sellerx.returns.TrendyolClaim;
import com.ecommerce.sellerx.returns.TrendyolClaimRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardStatsService {

    private final TrendyolOrderRepository orderRepository;
    private final StoreExpenseRepository storeExpenseRepository;
    private final TrendyolProductRepository productRepository;
    private final ReturnRecordRepository returnRecordRepository;
    private final TrendyolDeductionInvoiceRepository deductionInvoiceRepository;
    private final TrendyolCargoInvoiceRepository cargoInvoiceRepository;
    private final TrendyolInvoiceRepository invoiceRepository;
    private final TrendyolClaimRepository claimRepository;

    // Turkey timezone
    private static final ZoneId TURKEY_ZONE = ZoneId.of("Europe/Istanbul");

    // Platform fee keywords for TrendyolStoppage description parsing (Turkish)
    private static final String KEYWORD_INTERNATIONAL = "uluslararası";
    private static final String KEYWORD_OVERSEAS_OPERATION = "yurt dışı operasyon";
    private static final String KEYWORD_TERMIN = "termin";
    private static final String KEYWORD_PLATFORM_SERVICE = "platform hizmet";
    private static final String KEYWORD_INVOICE_CREDIT = "fatura kontör";
    private static final String KEYWORD_UNSUPPLIED = "tedarik edememe";
    private static final String KEYWORD_AZ_OVERSEAS = "az-yurtdışı";
    private static final String KEYWORD_AZ_PLATFORM = "az-platform";
    private static final String KEYWORD_PACKAGING = "paketleme";
    private static final String KEYWORD_WAREHOUSE = "depo hizmet";
    private static final String KEYWORD_CALL_CENTER = "çağrı merkezi";
    private static final String KEYWORD_PHOTO = "fotoğraf";
    private static final String KEYWORD_INTEGRATION = "entegrasyon";
    private static final String KEYWORD_STORAGE = "depolama";
    private static final String KEYWORD_STOPPAGE = "stopaj";

    // Expense category keywords (Turkish)
    private static final String EXPENSE_KEYWORD_OFFICE = "ofis";
    private static final String EXPENSE_KEYWORD_PACKAGING = "ambalaj";
    private static final String EXPENSE_KEYWORD_ACCOUNTING = "muhasebe";
    private static final String EXPENSE_KEYWORD_ADVERTISING = "reklam";
    
    public DashboardStatsResponse getStatsForStore(UUID storeId) {
        log.info("Calculating dashboard stats for store: {}", storeId);
        
        LocalDate today = LocalDate.now(TURKEY_ZONE);
        LocalDate yesterday = today.minusDays(1);
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDate firstDayOfLastMonth = today.minusMonths(1).withDayOfMonth(1);
        LocalDate lastDayOfLastMonth = today.withDayOfMonth(1).minusDays(1);
        
        log.info("Current Turkey date: {}", today);
        
        DashboardStatsDto todayStats = calculateStatsForPeriod(storeId, "today", today, today);
        DashboardStatsDto yesterdayStats = calculateStatsForPeriod(storeId, "yesterday", yesterday, yesterday);
        DashboardStatsDto thisMonthStats = calculateStatsForPeriod(storeId, "thisMonth", firstDayOfMonth, today);
        DashboardStatsDto lastMonthStats = calculateStatsForPeriod(storeId, "lastMonth", firstDayOfLastMonth, lastDayOfLastMonth);
        
        return DashboardStatsResponse.builder()
                .today(todayStats)
                .yesterday(yesterdayStats)
                .thisMonth(thisMonthStats)
                .lastMonth(lastMonthStats)
                .storeId(storeId.toString())
                .calculatedAt(LocalDateTime.now(TURKEY_ZONE).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }
    
    /**
     * Calculate stats for a custom date range.
     * This is a public method that can be called from the controller for custom date range queries.
     */
    public DashboardStatsDto getStatsForDateRange(UUID storeId, LocalDate startDate, LocalDate endDate, String periodLabel) {
        log.info("Calculating stats for store {} with custom date range: {} to {}", storeId, startDate, endDate);
        return calculateStatsForPeriod(storeId, periodLabel != null ? periodLabel : "custom", startDate, endDate);
    }

    /**
     * Get city statistics for map visualization.
     * Groups orders by city and calculates totals for each city.
     */
    public CityStatsResponse getCityStats(UUID storeId, LocalDate startDate, LocalDate endDate, String productBarcode) {
        log.info("Calculating city stats for store {} from {} to {}, product: {}", storeId, startDate, endDate, productBarcode);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // Get city statistics from repository
        var rawStats = productBarcode != null && !productBarcode.isEmpty()
                ? orderRepository.findCityStatsByStoreAndDateRangeAndProduct(storeId, startDateTime, endDateTime, productBarcode)
                : orderRepository.findCityStatsByStoreAndDateRange(storeId, startDateTime, endDateTime);

        // Convert raw results to DTOs
        List<CityStatsDto> cities = rawStats.stream()
                .map(row -> {
                    String cityName = row.getCity();
                    Integer cityCode = row.getCityCode();
                    Long orderCount = row.getOrderCount() != null ? row.getOrderCount() : 0L;
                    BigDecimal totalRevenue = row.getTotalRevenue() != null ? row.getTotalRevenue() : BigDecimal.ZERO;
                    Long totalQuantity = row.getTotalQuantity() != null ? row.getTotalQuantity() : 0L;

                    BigDecimal avgOrderValue = orderCount > 0
                            ? totalRevenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    return CityStatsDto.builder()
                            .cityName(cityName)
                            .cityCode(cityCode)
                            .totalOrders(orderCount)
                            .totalRevenue(totalRevenue)
                            .totalQuantity(totalQuantity)
                            .averageOrderValue(avgOrderValue)
                            .build();
                })
                .toList();

        // Count orders without city information
        Long ordersWithoutCity = orderRepository.countOrdersWithoutCity(storeId, startDateTime, endDateTime);

        return CityStatsResponse.builder()
                .cities(cities)
                .totalCities(cities.size())
                .ordersWithoutCity(ordersWithoutCity)
                .build();
    }

    /**
     * Get multi-period statistics for P&L breakdown.
     * Returns stats for multiple periods (months, weeks, or days) with horizontal scroll table data.
     *
     * @param storeId Store ID
     * @param periodType "monthly", "weekly", or "daily"
     * @param periodCount Number of periods to include (e.g., 12 for last 12 months)
     * @param productBarcode Optional: filter by product barcode
     * @return MultiPeriodStatsResponse with stats for each period and total
     */
    public MultiPeriodStatsResponse getMultiPeriodStats(UUID storeId, String periodType, int periodCount, String productBarcode) {
        log.info("Calculating multi-period stats for store {}: periodType={}, periodCount={}, productBarcode={}",
                storeId, periodType, periodCount, productBarcode);

        LocalDate today = LocalDate.now(TURKEY_ZONE);
        List<PeriodStatsDto> periods = new ArrayList<>();

        // Turkish locale for month names
        Locale turkishLocale = new Locale("tr", "TR");

        // Generate periods based on type
        switch (periodType.toLowerCase()) {
            case "monthly" -> generateMonthlyPeriods(storeId, today, periodCount, periods, turkishLocale, productBarcode);
            case "weekly" -> generateWeeklyPeriods(storeId, today, periodCount, periods, turkishLocale, productBarcode);
            case "daily" -> generateDailyPeriods(storeId, today, periodCount, periods, turkishLocale, productBarcode);
            default -> throw new IllegalArgumentException("Invalid periodType: " + periodType + ". Use 'monthly', 'weekly', or 'daily'.");
        }

        // Calculate totals from all periods
        PeriodStatsDto total = calculateTotalFromPeriods(periods);

        return MultiPeriodStatsResponse.builder()
                .periods(periods)
                .total(total)
                .storeId(storeId.toString())
                .periodType(periodType)
                .periodCount(periodCount)
                .calculatedAt(LocalDateTime.now(TURKEY_ZONE).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    private void generateMonthlyPeriods(UUID storeId, LocalDate today, int periodCount,
                                        List<PeriodStatsDto> periods, Locale locale, String productBarcode) {
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM yyyy", locale);

        for (int i = 0; i < periodCount; i++) {
            LocalDate monthStart = today.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

            // If it's current month, end date is today
            if (i == 0) {
                monthEnd = today;
            }

            String label = monthStart.format(monthFormatter);
            // Capitalize first letter (e.g., "ara 2025" -> "Ara 2025")
            label = label.substring(0, 1).toUpperCase() + label.substring(1);

            PeriodStatsDto periodStats = calculatePeriodStatsDto(storeId, label, monthStart, monthEnd, productBarcode);
            periods.add(periodStats);
        }
    }

    private void generateWeeklyPeriods(UUID storeId, LocalDate today, int periodCount,
                                       List<PeriodStatsDto> periods, Locale locale, String productBarcode) {
        WeekFields weekFields = WeekFields.of(DayOfWeek.MONDAY, 1);

        for (int i = 0; i < periodCount; i++) {
            LocalDate weekEnd = today.minusWeeks(i);
            LocalDate weekStart = weekEnd.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

            // If it's current week, start from Monday but end is today
            if (i == 0) {
                weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                weekEnd = today;
            } else {
                weekEnd = weekStart.plusDays(6); // Sunday
            }

            int weekNumber = weekStart.get(weekFields.weekOfWeekBasedYear());
            String label = "Hafta " + weekNumber;

            PeriodStatsDto periodStats = calculatePeriodStatsDto(storeId, label, weekStart, weekEnd, productBarcode);
            periods.add(periodStats);
        }
    }

    private void generateDailyPeriods(UUID storeId, LocalDate today, int periodCount,
                                      List<PeriodStatsDto> periods, Locale locale, String productBarcode) {
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("d MMM", locale);

        for (int i = 0; i < periodCount; i++) {
            LocalDate date = today.minusDays(i);
            String label = date.format(dayFormatter);
            // Capitalize first letter of month (e.g., "17 oca" -> "17 Oca")
            int spaceIndex = label.indexOf(' ');
            if (spaceIndex > 0 && spaceIndex < label.length() - 1) {
                label = label.substring(0, spaceIndex + 1) +
                        label.substring(spaceIndex + 1, spaceIndex + 2).toUpperCase() +
                        label.substring(spaceIndex + 2);
            }

            PeriodStatsDto periodStats = calculatePeriodStatsDto(storeId, label, date, date, productBarcode);
            periods.add(periodStats);
        }
    }

    private PeriodStatsDto calculatePeriodStatsDto(UUID storeId, String label, LocalDate startDate,
                                                    LocalDate endDate, String productBarcode) {
        // Use existing method to get base stats
        DashboardStatsDto baseStats = calculateStatsForPeriod(storeId, label, startDate, endDate);

        // TODO: If productBarcode is specified, filter the stats (requires additional implementation)
        // For now, we return all stats without product filtering

        // Use the same values already calculated by calculateStatsForPeriod()
        // (which includes platformFees, advertising cost, etc.)
        BigDecimal netProfit = baseStats.getNetProfit();
        BigDecimal profitMargin = baseStats.getProfitMargin();
        BigDecimal roi = baseStats.getRoi();

        return PeriodStatsDto.builder()
                .periodLabel(label)
                .startDate(startDate.toString())
                .endDate(endDate.toString())
                .totalOrders(baseStats.getTotalOrders())
                .totalProductsSold(baseStats.getTotalProductsSold())
                .totalRevenue(baseStats.getTotalRevenue())
                .returnCount(baseStats.getReturnCount())
                .returnCost(baseStats.getReturnCost())
                .totalProductCosts(baseStats.getTotalProductCosts())
                .grossProfit(baseStats.getGrossProfit())
                .vatDifference(baseStats.getVatDifference())
                .totalStoppage(baseStats.getTotalStoppage())
                .totalEstimatedCommission(baseStats.getTotalEstimatedCommission())
                .netProfit(netProfit)
                .profitMargin(profitMargin)
                .roi(roi)
                .itemsWithoutCost(baseStats.getItemsWithoutCost())
                .totalExpenseNumber(baseStats.getTotalExpenseNumber())
                .totalExpenseAmount(baseStats.getTotalExpenseAmount())
                .totalShippingCost(baseStats.getTotalShippingCost())
                // Kesilen Faturalar (Invoiced Fees)
                .platformServiceFee(baseStats.getPlatformServiceFee())
                .azPlatformServiceFee(baseStats.getAzPlatformServiceFee())
                .invoicedAdvertisingFees(baseStats.getInvoicedAdvertisingFees())
                .invoicedPenaltyFees(baseStats.getInvoicedPenaltyFees())
                .invoicedInternationalFees(baseStats.getInvoicedInternationalFees())
                .invoicedOtherFees(baseStats.getInvoicedOtherFees())
                .invoicedRefunds(baseStats.getInvoicedRefunds())
                // Gider Kategorileri
                .expensesByCategory(baseStats.getExpensesByCategory())
                .build();
    }

    private BigDecimal calculateProfitMargin(BigDecimal grossProfit, BigDecimal revenue) {
        if (revenue == null || revenue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        // Margin = (Gross Profit / Revenue) * 100
        return grossProfit
                .divide(revenue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateROI(BigDecimal netProfit, BigDecimal productCosts) {
        if (productCosts == null || productCosts.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        // ROI = (Net Profit / Product Costs) * 100
        return netProfit
                .divide(productCosts, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private PeriodStatsDto calculateTotalFromPeriods(List<PeriodStatsDto> periods) {
        if (periods.isEmpty()) {
            return PeriodStatsDto.builder()
                    .periodLabel("Toplam")
                    .totalOrders(0)
                    .totalProductsSold(0)
                    .totalRevenue(BigDecimal.ZERO)
                    .returnCount(0)
                    .returnCost(BigDecimal.ZERO)
                    .totalProductCosts(BigDecimal.ZERO)
                    .grossProfit(BigDecimal.ZERO)
                    .vatDifference(BigDecimal.ZERO)
                    .totalStoppage(BigDecimal.ZERO)
                    .totalEstimatedCommission(BigDecimal.ZERO)
                    .netProfit(BigDecimal.ZERO)
                    .profitMargin(BigDecimal.ZERO)
                    .roi(BigDecimal.ZERO)
                    .itemsWithoutCost(0)
                    .totalExpenseNumber(0)
                    .totalExpenseAmount(BigDecimal.ZERO)
                    .totalShippingCost(BigDecimal.ZERO)
                    // Kesilen Faturalar
                    .platformServiceFee(BigDecimal.ZERO)
                    .azPlatformServiceFee(BigDecimal.ZERO)
                    .invoicedAdvertisingFees(BigDecimal.ZERO)
                    .invoicedPenaltyFees(BigDecimal.ZERO)
                    .invoicedInternationalFees(BigDecimal.ZERO)
                    .invoicedOtherFees(BigDecimal.ZERO)
                    .invoicedRefunds(BigDecimal.ZERO)
                    // Gider Kategorileri
                    .expensesByCategory(new java.util.HashMap<>())
                    .build();
        }

        // Sum all numeric values
        int totalOrders = periods.stream().mapToInt(p -> p.getTotalOrders() != null ? p.getTotalOrders() : 0).sum();
        int totalProductsSold = periods.stream().mapToInt(p -> p.getTotalProductsSold() != null ? p.getTotalProductsSold() : 0).sum();
        int returnCount = periods.stream().mapToInt(p -> p.getReturnCount() != null ? p.getReturnCount() : 0).sum();
        int itemsWithoutCost = periods.stream().mapToInt(p -> p.getItemsWithoutCost() != null ? p.getItemsWithoutCost() : 0).sum();
        int totalExpenseNumber = periods.stream().mapToInt(p -> p.getTotalExpenseNumber() != null ? p.getTotalExpenseNumber() : 0).sum();

        BigDecimal totalRevenue = sumBigDecimals(periods, PeriodStatsDto::getTotalRevenue);
        BigDecimal totalReturnCost = sumBigDecimals(periods, PeriodStatsDto::getReturnCost);
        BigDecimal totalProductCosts = sumBigDecimals(periods, PeriodStatsDto::getTotalProductCosts);
        BigDecimal totalGrossProfit = sumBigDecimals(periods, PeriodStatsDto::getGrossProfit);
        BigDecimal totalVatDifference = sumBigDecimals(periods, PeriodStatsDto::getVatDifference);
        BigDecimal totalStoppage = sumBigDecimals(periods, PeriodStatsDto::getTotalStoppage);
        BigDecimal totalCommission = sumBigDecimals(periods, PeriodStatsDto::getTotalEstimatedCommission);
        BigDecimal totalNetProfit = sumBigDecimals(periods, PeriodStatsDto::getNetProfit);
        BigDecimal totalExpenseAmount = sumBigDecimals(periods, PeriodStatsDto::getTotalExpenseAmount);
        BigDecimal totalShippingCost = sumBigDecimals(periods, PeriodStatsDto::getTotalShippingCost);

        // Sum invoiced fees
        BigDecimal totalPlatformServiceFee = sumBigDecimals(periods, PeriodStatsDto::getPlatformServiceFee);
        BigDecimal totalAzPlatformServiceFee = sumBigDecimals(periods, PeriodStatsDto::getAzPlatformServiceFee);
        BigDecimal totalInvoicedAdvertisingFees = sumBigDecimals(periods, PeriodStatsDto::getInvoicedAdvertisingFees);
        BigDecimal totalInvoicedPenaltyFees = sumBigDecimals(periods, PeriodStatsDto::getInvoicedPenaltyFees);
        BigDecimal totalInvoicedInternationalFees = sumBigDecimals(periods, PeriodStatsDto::getInvoicedInternationalFees);
        BigDecimal totalInvoicedOtherFees = sumBigDecimals(periods, PeriodStatsDto::getInvoicedOtherFees);
        BigDecimal totalInvoicedRefunds = sumBigDecimals(periods, PeriodStatsDto::getInvoicedRefunds);

        // Merge expense categories from all periods
        java.util.Map<String, BigDecimal> mergedExpensesByCategory = new java.util.HashMap<>();
        for (PeriodStatsDto period : periods) {
            if (period.getExpensesByCategory() != null) {
                period.getExpensesByCategory().forEach((category, amount) -> {
                    mergedExpensesByCategory.merge(category, amount != null ? amount : BigDecimal.ZERO, BigDecimal::add);
                });
            }
        }

        // Calculate derived metrics from totals
        BigDecimal profitMargin = calculateProfitMargin(totalGrossProfit, totalRevenue);
        BigDecimal roi = calculateROI(totalNetProfit, totalProductCosts);

        // Find date range from first and last period
        String startDate = periods.get(periods.size() - 1).getStartDate();
        String endDate = periods.get(0).getEndDate();

        return PeriodStatsDto.builder()
                .periodLabel("Toplam")
                .startDate(startDate)
                .endDate(endDate)
                .totalOrders(totalOrders)
                .totalProductsSold(totalProductsSold)
                .totalRevenue(totalRevenue)
                .returnCount(returnCount)
                .returnCost(totalReturnCost)
                .totalProductCosts(totalProductCosts)
                .grossProfit(totalGrossProfit)
                .vatDifference(totalVatDifference)
                .totalStoppage(totalStoppage)
                .totalEstimatedCommission(totalCommission)
                .netProfit(totalNetProfit)
                .profitMargin(profitMargin)
                .roi(roi)
                .itemsWithoutCost(itemsWithoutCost)
                .totalExpenseNumber(totalExpenseNumber)
                .totalExpenseAmount(totalExpenseAmount)
                .totalShippingCost(totalShippingCost)
                // Kesilen Faturalar
                .platformServiceFee(totalPlatformServiceFee)
                .azPlatformServiceFee(totalAzPlatformServiceFee)
                .invoicedAdvertisingFees(totalInvoicedAdvertisingFees)
                .invoicedPenaltyFees(totalInvoicedPenaltyFees)
                .invoicedInternationalFees(totalInvoicedInternationalFees)
                .invoicedOtherFees(totalInvoicedOtherFees)
                .invoicedRefunds(totalInvoicedRefunds)
                // Gider Kategorileri
                .expensesByCategory(mergedExpensesByCategory)
                .build();
    }

    private BigDecimal sumBigDecimals(List<PeriodStatsDto> periods, java.util.function.Function<PeriodStatsDto, BigDecimal> getter) {
        return periods.stream()
                .map(getter)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private DashboardStatsDto calculateStatsForPeriod(UUID storeId, String period, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        log.debug("Calculating stats for period {} from {} to {}", period, startDateTime, endDateTime);

        // Get revenue orders (not cancelled, returned etc.)
        List<TrendyolOrder> revenueOrders = orderRepository.findRevenueOrdersByStoreAndDateRange(
                storeId, startDateTime, endDateTime);

        // Get returned orders via claims (matches Trendyol's "İade Talep Tarihi" filter)
        // Previously used orderRepository.findReturnedOrdersByStoreAndDateRange which filtered by orderDate
        // (order creation date), not by return date — causing mismatches with Trendyol counts.
        List<TrendyolClaim> claims = claimRepository.findByStoreIdAndDateRange(storeId, startDateTime, endDateTime);
        Set<String> returnedOrderNumbers = claims.stream()
                .map(TrendyolClaim::getOrderNumber)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Fetch actual order objects for cost calculation
        List<TrendyolOrder> returnedOrders = new ArrayList<>();
        for (String orderNumber : returnedOrderNumbers) {
            List<TrendyolOrder> orders = orderRepository.findByStoreIdAndTyOrderNumber(storeId, orderNumber);
            if (!orders.isEmpty()) {
                returnedOrders.add(orders.get(0));
            }
        }

        // Calculate basic stats
        int totalOrders = revenueOrders.size();
        int totalProductsSold = calculateTotalProductsSold(revenueOrders);
        int returnCount = returnedOrders.size();
        int returnQuantity = calculateTotalProductsSold(returnedOrders); // İade edilen ürün adedi
        int netUnitsSold = totalProductsSold - returnQuantity; // Net Satış Adedi

        // Calculate revenue (Ciro = total_price, matches Trendyol panel)
        BigDecimal totalRevenue = calculateTotalRevenue(revenueOrders);

        // Calculate actual return cost from ReturnRecord table (dynamic, not hardcoded)
        BigDecimal returnCost = calculateActualReturnCost(storeId, startDateTime, endDateTime, returnCount, returnedOrders);

        // Calculate product costs and items without cost
        ProductCostResult productCostResult = calculateProductCosts(revenueOrders);
        BigDecimal totalProductCosts = productCostResult.getTotalCosts();
        int itemsWithoutCost = productCostResult.getItemsWithoutCost();

        // Calculate gross profit
        BigDecimal grossProfit = totalRevenue.subtract(totalProductCosts);

        // Calculate VAT difference
        BigDecimal vatDifference = calculateVatDifference(revenueOrders);

        // Calculate total stoppage (from orders - legacy method)
        BigDecimal totalStoppage = calculateTotalStoppage(revenueOrders);

        // Calculate invoiced commission from TrendyolDeductionInvoice (matches Invoices page)
        // Includes: Komisyon Faturası, AZ - Komisyon Faturası, AZ-Komisyon Geliri
        BigDecimal totalEstimatedCommission = deductionInvoiceRepository.sumCommissionFeesByStoreIdAndDateRange(
                storeId, startDateTime, endDateTime);
        if (totalEstimatedCommission == null || totalEstimatedCommission.compareTo(BigDecimal.ZERO) == 0) {
            // Fatura komisyonu yoksa, siparişlerin estimatedCommission toplamını kullan (Sandbox için)
            totalEstimatedCommission = calculateTotalEstimatedCommissionFromOrders(revenueOrders);
        }

        // Calculate period expenses (legacy method)
        List<PeriodExpenseDto> expenses = calculatePeriodExpenses(storeId, startDate, endDate);

        // Calculate expense summary
        int totalExpenseNumber = expenses.size();
        BigDecimal totalExpenseAmount = expenses.stream()
                .map(PeriodExpenseDto::expenseTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ============== YENİ HESAPLAMALAR (32 Metrik) ==============

        // Platform ücretleri (9 kategori) - TrendyolStoppage tablosundan
        PlatformFeesResult platformFees = calculatePlatformFees(storeId, startDateTime, endDateTime);

        // Kategori bazlı giderler - StoreExpense tablosundan
        CategorizedExpensesResult categorizedExpenses = calculateCategorizedExpenses(storeId, startDateTime, endDateTime);

        // İndirimler (Satıcı, Platform, Kupon) - TrendyolOrder tablosundan
        DiscountsResult discounts = calculateDiscounts(storeId, startDateTime, endDateTime);

        // Kargo maliyetleri (dönem seviyesi) - TrendyolDeductionInvoice tablosundan
        ShippingCostsResult shippingCosts = calculateShippingCosts(storeId, startDate, endDate);

        // Erken ödeme ücreti - Settlement API'den (TrendyolOrder tablosundan)
        BigDecimal earlyPaymentFee = orderRepository.sumEarlyPaymentFee(storeId, startDateTime, endDateTime);
        if (earlyPaymentFee == null) {
            earlyPaymentFee = BigDecimal.ZERO;
        }

        // İade oranı (%)
        BigDecimal refundRate = calculateRefundRate(returnCount, totalOrders);

        // Net Ciro = Brüt Ciro - İndirimler
        BigDecimal netRevenue = totalRevenue.subtract(discounts.getTotalDiscount());

        // Faturalı Reklam (Trendyol DeductionInvoices API'den - "Reklam Bedeli" transaction type)
        BigDecimal invoicedAdvertisingCost = deductionInvoiceRepository.sumAdvertisingFeesByStoreIdAndDateRange(
                storeId, startDateTime, endDateTime);
        if (invoicedAdvertisingCost == null) {
            invoicedAdvertisingCost = BigDecimal.ZERO;
        }

        // ============== KESİLEN FATURALAR (Dashboard Kartları için) ==============
        // Kategori bazlı fatura toplamları - TrendyolDeductionInvoice tablosundan
        // NOT: KOMİSYON ve KARGO artık ayrı satırlarda gösteriliyor (fatura bazlı hesaplanıyor)
        // Bu bölüm sadece REKLAM, CEZA, ULUSLARARASI, DİĞER ve İADE kategorilerini içerir
        BigDecimal invoicedAdvertisingFees = deductionInvoiceRepository.sumInvoicedAdvertisingFees(
                storeId, startDateTime, endDateTime);
        BigDecimal invoicedPenaltyFees = deductionInvoiceRepository.sumInvoicedPenaltyFees(
                storeId, startDateTime, endDateTime);
        BigDecimal invoicedInternationalFees = deductionInvoiceRepository.sumInvoicedInternationalFees(
                storeId, startDateTime, endDateTime);
        BigDecimal invoicedOtherFees = deductionInvoiceRepository.sumInvoicedOtherFees(
                storeId, startDateTime, endDateTime);
        BigDecimal invoicedRefunds = deductionInvoiceRepository.sumInvoicedRefunds(
                storeId, startDateTime, endDateTime);

        // Null kontrolü (COALESCE kullanıldığı için normalde null gelmez ama yine de kontrol)
        if (invoicedAdvertisingFees == null) invoicedAdvertisingFees = BigDecimal.ZERO;
        if (invoicedPenaltyFees == null) invoicedPenaltyFees = BigDecimal.ZERO;
        if (invoicedInternationalFees == null) invoicedInternationalFees = BigDecimal.ZERO;
        if (invoicedOtherFees == null) invoicedOtherFees = BigDecimal.ZERO;
        if (invoicedRefunds == null) invoicedRefunds = BigDecimal.ZERO;

        // Toplam kesilen faturalar: REKLAM + CEZA + ULUSLARARASI + DIGER - IADE
        BigDecimal invoicedDeductions = invoicedAdvertisingFees
                .add(invoicedPenaltyFees)
                .add(invoicedInternationalFees)
                .add(invoicedOtherFees)
                .subtract(invoicedRefunds);

        log.info("Period {} invoiced deductions: ads={}, penalty={}, international={}, other={}, refunds={}, total={}",
                period, invoicedAdvertisingFees, invoicedPenaltyFees, invoicedInternationalFees,
                invoicedOtherFees, invoicedRefunds, invoicedDeductions);

        // ============== ÜRÜN DETAYLARI VE REKLAM MALİYETİ ==============
        // Önce ürün detaylarını hesapla (reklam maliyetini netProfit'ten düşmek için)
        // Toplam giderleri de geçir (ürün bazlı net kar tutarlılığı için)
        List<ProductDetailDto> products = calculateProductDetails(
                storeId, revenueOrders, returnedOrders, startDateTime, endDateTime,
                totalRevenue, platformFees.getTotal(), totalExpenseAmount, invoicedDeductions);

        // Tüm ürünlerin toplam reklam maliyetini hesapla
        BigDecimal totalAdvertisingCostSum = products.stream()
                .map(ProductDetailDto::getTotalAdvertisingCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Period {} total advertising cost from products: {}", period, totalAdvertisingCostSum);

        // ============== NET KAR HESAPLAMASI (Güncellenmiş) ==============
        // Net Kar = Brüt Kar - Komisyon - Platform Ücretleri - İade Maliyeti - Giderler - Kargo - Kesilen Faturalar - Reklam Maliyeti
        BigDecimal netProfit = grossProfit
                .subtract(totalEstimatedCommission)
                .subtract(platformFees.getTotal())
                .subtract(returnCost)
                .subtract(totalExpenseAmount)
                .subtract(shippingCosts.getShippingCost())
                .subtract(invoicedDeductions)
                .subtract(totalAdvertisingCostSum);

        // Calculate Profit Margin (%)
        BigDecimal profitMargin = BigDecimal.ZERO;
        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            profitMargin = grossProfit
                    .divide(totalRevenue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // Calculate ROI (%) = (Net Profit / Product Costs) * 100
        BigDecimal roi = BigDecimal.ZERO;
        if (totalProductCosts.compareTo(BigDecimal.ZERO) > 0) {
            roi = netProfit
                    .divide(totalProductCosts, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        log.info("Period {} stats: revenue={}, grossProfit={}, commission={}, platformFees={}, returnCost={}, expenses={}, shipping={}, invoicedDeductions={}, adsCost={}, netProfit={}",
                period, totalRevenue, grossProfit, totalEstimatedCommission, platformFees.getTotal(), returnCost, totalExpenseAmount, shippingCosts.getShippingCost(), invoicedDeductions, totalAdvertisingCostSum, netProfit);
        log.info("Period {} discounts: seller={}, platform={}, coupon={}, total={}",
                period, discounts.getSellerDiscount(), discounts.getPlatformDiscount(), discounts.getCouponDiscount(), discounts.getTotalDiscount());

        return DashboardStatsDto.builder()
                .period(period)
                .totalOrders(totalOrders)
                .totalProductsSold(totalProductsSold)
                .netUnitsSold(netUnitsSold)
                .totalRevenue(totalRevenue)
                .returnCount(returnCount)
                .returnCost(returnCost)
                .totalProductCosts(totalProductCosts)
                .grossProfit(grossProfit)
                .netProfit(netProfit)
                .profitMargin(profitMargin)
                .vatDifference(vatDifference)
                .totalStoppage(totalStoppage) // Siparişlerden hesaplanan %1 stopaj
                .totalEstimatedCommission(totalEstimatedCommission)
                .itemsWithoutCost(itemsWithoutCost)
                .totalExpenseNumber(totalExpenseNumber)
                .totalExpenseAmount(totalExpenseAmount)
                // ============== YENİ ALANLAR ==============
                // Kesilen Faturalar (Dashboard Kartları için)
                .invoicedDeductions(invoicedDeductions)
                .invoicedAdvertisingFees(invoicedAdvertisingFees)
                .invoicedPenaltyFees(invoicedPenaltyFees)
                .invoicedInternationalFees(invoicedInternationalFees)
                .invoicedOtherFees(invoicedOtherFees)
                .invoicedRefunds(invoicedRefunds)
                // Eski alan - uyumluluk için korunuyor
                .invoicedAdvertisingCost(invoicedAdvertisingCost)
                // İndirimler & Kuponlar
                .totalSellerDiscount(discounts.getSellerDiscount())
                .totalPlatformDiscount(discounts.getPlatformDiscount())
                .totalCouponDiscount(discounts.getCouponDiscount())
                // Kargo
                .totalShippingCost(shippingCosts.getShippingCost())
                .totalShippingIncome(shippingCosts.getShippingIncome())
                // Platform Ücretleri (15 kategori)
                .internationalServiceFee(platformFees.getInternationalServiceFee())
                .overseasOperationFee(platformFees.getOverseasOperationFee())
                .terminDelayFee(platformFees.getTerminDelayFee())
                .platformServiceFee(platformFees.getPlatformServiceFee())
                .invoiceCreditFee(platformFees.getInvoiceCreditFee())
                .unsuppliedFee(platformFees.getUnsuppliedFee())
                .azOverseasOperationFee(platformFees.getAzOverseasOperationFee())
                .azPlatformServiceFee(platformFees.getAzPlatformServiceFee())
                .packagingServiceFee(platformFees.getPackagingServiceFee())
                .warehouseServiceFee(platformFees.getWarehouseServiceFee())
                .callCenterFee(platformFees.getCallCenterFee())
                .photoShootingFee(platformFees.getPhotoShootingFee())
                .integrationFee(platformFees.getIntegrationFee())
                .storageServiceFee(platformFees.getStorageServiceFee())
                .otherPlatformFees(platformFees.getOtherPlatformFees())
                // Erken Ödeme (Settlement API'den)
                .earlyPaymentFee(earlyPaymentFee)
                // Gider Kategorileri (eski hardcoded + yeni dinamik)
                .officeExpenses(categorizedExpenses.getOfficeExpenses())
                .packagingExpenses(categorizedExpenses.getPackagingExpenses())
                .accountingExpenses(categorizedExpenses.getAccountingExpenses())
                .otherExpenses(categorizedExpenses.getOtherExpenses())
                .expensesByCategory(categorizedExpenses.getExpensesByCategory())
                // İade Detayları
                .refundRate(refundRate)
                // Net Ciro
                .netRevenue(netRevenue)
                // ROI
                .roi(roi)
                // Detay listeleri
                .orders(calculateOrderDetails(storeId, revenueOrders, returnedOrders))
                .products(products)
                .expenses(expenses)
                .build();
    }
    
    private int calculateTotalProductsSold(List<TrendyolOrder> orders) {
        return orders.stream()
                .flatMap(order -> order.getOrderItems().stream())
                .mapToInt(OrderItem::getQuantity)
                .sum();
    }
    
    private BigDecimal calculateTotalRevenue(List<TrendyolOrder> orders) {
        return orders.stream()
                .map(order -> order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate actual return cost from ReturnRecord table.
     * Falls back to estimating from returned orders if no ReturnRecord data exists.
     * Fallback includes:
     * - Product cost (alış maliyeti)
     * - Shipping cost (outbound - gönderi kargo)
     * - Shipping cost (return - iade kargo)
     * NOT: Komisyon dahil edilmez - Trendyol iade durumunda komisyonu iptal eder.
     *
     * @param storeId Store ID
     * @param startDateTime Period start date/time
     * @param endDateTime Period end date/time
     * @param returnCount Number of returned orders (for fallback calculation)
     * @param returnedOrders List of returned orders for fallback estimation
     * @return Actual return cost from ReturnRecord or estimated from orders
     */
    private BigDecimal calculateActualReturnCost(UUID storeId, LocalDateTime startDateTime,
                                                   LocalDateTime endDateTime, int returnCount,
                                                   List<TrendyolOrder> returnedOrders) {
        // 1. Try return_records table first (most accurate data)
        try {
            BigDecimal actualReturnCost = returnRecordRepository.sumTotalLossByStoreAndDateRange(
                    storeId, startDateTime, endDateTime);

            if (actualReturnCost != null && actualReturnCost.compareTo(BigDecimal.ZERO) > 0) {
                log.debug("Using actual return cost from ReturnRecord: {} for period {} to {}",
                        actualReturnCost, startDateTime, endDateTime);
                return actualReturnCost;
            }
        } catch (Exception e) {
            log.warn("Error fetching return cost from ReturnRecord: {}", e.getMessage());
        }

        // 2. Fallback: Estimate from returned orders
        if (returnedOrders != null && !returnedOrders.isEmpty()) {
            // 2a. Kargo faturalarından gerçek gönderi ve iade kargo maliyetlerini al
            List<String> orderNumbers = returnedOrders.stream()
                    .map(TrendyolOrder::getTyOrderNumber)
                    .filter(Objects::nonNull)
                    .toList();

            Map<String, BigDecimal> outboundShippingMap = new HashMap<>();
            Map<String, BigDecimal> returnShippingMap = new HashMap<>();
            if (!orderNumbers.isEmpty()) {
                cargoInvoiceRepository.sumOutboundShippingByOrderNumbers(storeId, orderNumbers)
                        .forEach(p -> outboundShippingMap.put(p.getOrderNumber(), p.getTotalAmount()));
                cargoInvoiceRepository.sumReturnShippingByOrderNumbers(storeId, orderNumbers)
                        .forEach(p -> returnShippingMap.put(p.getOrderNumber(), p.getTotalAmount()));
            }

            // 2b. Kargo faturası olmayan barkodlar için son bilinen kargo maliyetini bul
            Set<String> barcodesNeedingFallback = new HashSet<>();
            for (TrendyolOrder order : returnedOrders) {
                String orderNum = order.getTyOrderNumber();
                if (!outboundShippingMap.containsKey(orderNum) && !returnShippingMap.containsKey(orderNum)) {
                    // Bu sipariş için hiç kargo faturası yok - barkodlardan referans al
                    if (order.getOrderItems() != null) {
                        for (OrderItem item : order.getOrderItems()) {
                            if (item.getBarcode() != null) {
                                barcodesNeedingFallback.add(item.getBarcode());
                            }
                        }
                    }
                }
            }

            Map<String, BigDecimal> barcodeShippingFallback = new HashMap<>();
            if (!barcodesNeedingFallback.isEmpty()) {
                cargoInvoiceRepository.findLatestShippingShareByBarcodes(storeId, new ArrayList<>(barcodesNeedingFallback))
                        .forEach(p -> {
                            if (p.getShippingShare() != null && p.getShippingShare().compareTo(BigDecimal.ZERO) > 0) {
                                barcodeShippingFallback.put(p.getBarcode(), p.getShippingShare());
                            }
                        });
                if (!barcodeShippingFallback.isEmpty()) {
                    log.info("RETURN_SHIPPING_FALLBACK: Found shipping reference for {} of {} barcodes",
                            barcodeShippingFallback.size(), barcodesNeedingFallback.size());
                }
            }

            BigDecimal estimatedCost = BigDecimal.ZERO;
            for (TrendyolOrder order : returnedOrders) {
                String orderNum = order.getTyOrderNumber();

                // Product cost - sadece satılamaz (isResalable == false) ise dahil et
                BigDecimal productCost = BigDecimal.ZERO;
                if (Boolean.FALSE.equals(order.getIsResalable())) {
                    if (order.getOrderItems() != null) {
                        for (OrderItem item : order.getOrderItems()) {
                            if (item.getCost() != null && item.getCost().compareTo(BigDecimal.ZERO) > 0) {
                                int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                                productCost = productCost.add(
                                        item.getCost().multiply(BigDecimal.valueOf(qty)));
                            }
                        }
                    }
                }

                // Outbound shipping cost: 1) Kargo faturası, 2) Sipariş alanı, 3) Barkod referansı
                BigDecimal outboundShipping = outboundShippingMap.getOrDefault(orderNum, BigDecimal.ZERO);
                if (outboundShipping.compareTo(BigDecimal.ZERO) == 0) {
                    outboundShipping = order.getEstimatedShippingCost() != null
                            ? order.getEstimatedShippingCost() : BigDecimal.ZERO;
                }
                if (outboundShipping.compareTo(BigDecimal.ZERO) == 0 && order.getOrderItems() != null) {
                    // Barkod bazlı son bilinen kargo maliyetinden referans al
                    for (OrderItem item : order.getOrderItems()) {
                        BigDecimal ref = barcodeShippingFallback.getOrDefault(item.getBarcode(), BigDecimal.ZERO);
                        if (ref.compareTo(BigDecimal.ZERO) > 0) {
                            outboundShipping = outboundShipping.add(ref);
                        }
                    }
                }

                // Return shipping cost: 1) İade kargo faturası, 2) Sipariş alanı, 3) Gönderi kargosundan tahmin
                BigDecimal returnShipping = returnShippingMap.getOrDefault(orderNum, BigDecimal.ZERO);
                if (returnShipping.compareTo(BigDecimal.ZERO) == 0) {
                    if (order.getReturnShippingCost() != null && order.getReturnShippingCost().compareTo(BigDecimal.ZERO) > 0) {
                        returnShipping = order.getReturnShippingCost();
                    } else if (outboundShipping.compareTo(BigDecimal.ZERO) > 0) {
                        returnShipping = outboundShipping; // İade kargo ≈ gönderi kargo
                    }
                }

                // NOT: Komisyon iade maliyetine dahil edilmez.
                // Trendyol iade durumunda komisyonu iptal eder, gerçek bir kayıp değildir.

                estimatedCost = estimatedCost.add(productCost)
                        .add(outboundShipping)
                        .add(returnShipping);

                log.debug("RETURN_COST order={}: product={}, outbound={}, return={}",
                        orderNum, productCost, outboundShipping, returnShipping);
            }
            log.info("Estimated return cost from orders: {} for {} returns", estimatedCost, returnedOrders.size());
            return estimatedCost;
        }

        return BigDecimal.ZERO;
    }

    private ProductCostResult calculateProductCosts(List<TrendyolOrder> orders) {
        BigDecimal totalCosts = BigDecimal.ZERO;
        int itemsWithoutCost = 0;
        
        for (TrendyolOrder order : orders) {
            for (OrderItem item : order.getOrderItems()) {
                if (item.getCost() != null && item.getCost().compareTo(BigDecimal.ZERO) > 0) {
                    // Item has cost - add to total
                    BigDecimal itemTotalCost = item.getCost().multiply(BigDecimal.valueOf(item.getQuantity()));
                    totalCosts = totalCosts.add(itemTotalCost);
                } else {
                    // Item doesn't have cost
                    itemsWithoutCost += item.getQuantity();
                }
            }
        }
        
        return new ProductCostResult(totalCosts, itemsWithoutCost);
    }
    
    private BigDecimal calculateVatDifference(List<TrendyolOrder> orders) {
        BigDecimal totalVatDifference = BigDecimal.ZERO;
        
        for (TrendyolOrder order : orders) {
            for (OrderItem item : order.getOrderItems()) {
                // Only calculate VAT difference if both cost and price exist
                if (item.getPrice() != null && item.getCost() != null && 
                    item.getPrice().compareTo(BigDecimal.ZERO) > 0 && 
                    item.getCost().compareTo(BigDecimal.ZERO) > 0) {
                    
                    // Sales VAT (Tahsil Edilen KDV) - Calculate from price using vatBaseAmount as rate
                    BigDecimal salesVat;
                    if (item.getVatBaseAmount() != null && item.getVatBaseAmount().compareTo(BigDecimal.ZERO) > 0) {
                        // vatBaseAmount is actually the VAT rate (e.g., 20 for 20%)
                        BigDecimal salesVatRate = item.getVatBaseAmount().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                        BigDecimal salesPriceIncludingVat = item.getPrice();
                        
                        // Formula: Sales VAT = (Sales Price (VAT Included) / (1 + VAT Rate)) × VAT Rate
                        BigDecimal salesPriceExcludingVat = salesPriceIncludingVat.divide(
                                BigDecimal.ONE.add(salesVatRate), 4, RoundingMode.HALF_UP);
                        salesVat = salesPriceExcludingVat.multiply(salesVatRate);
                    } else {
                        // Fallback: Calculate from price assuming 20% VAT
                        BigDecimal salesPrice = item.getPrice();
                        salesVat = salesPrice.multiply(BigDecimal.valueOf(0.20))
                                .divide(BigDecimal.valueOf(1.20), 2, RoundingMode.HALF_UP);
                    }
                    
                    // Cost VAT (Ödenen KDV) - Calculate using actual costVat rate
                    BigDecimal costVat = BigDecimal.ZERO;
                    if (item.getCostVat() != null && item.getCostVat() > 0) {
                        // costVat is the VAT rate (e.g., 20 for 20%)
                        BigDecimal costVatRate = BigDecimal.valueOf(item.getCostVat()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                        BigDecimal costIncludingVat = item.getCost();
                        
                        // Formula: Cost VAT = (Cost (VAT Included) / (1 + VAT Rate)) × VAT Rate
                        BigDecimal costExcludingVat = costIncludingVat.divide(
                                BigDecimal.ONE.add(costVatRate), 4, RoundingMode.HALF_UP);
                        costVat = costExcludingVat.multiply(costVatRate);
                    }
                    
                    // VAT difference per item (Net KDV Farkı)
                    // KDV Farkı = Satış KDV − Alış KDV
                    BigDecimal vatDifferencePerItem = salesVat.subtract(costVat);
                    
                    // Total VAT difference for this item (multiply by quantity)
                    BigDecimal itemVatDifference = vatDifferencePerItem.multiply(BigDecimal.valueOf(item.getQuantity()));
                    
                    totalVatDifference = totalVatDifference.add(itemVatDifference);
                    
                    log.debug("VAT calculation for {}: Sales VAT={}, Cost VAT={}, Difference={}, Quantity={}, Total Diff={}", 
                            item.getBarcode(), salesVat, costVat, vatDifferencePerItem, item.getQuantity(), itemVatDifference);
                }
            }
        }
        
        return totalVatDifference;
    }
    
    private BigDecimal calculateTotalStoppage(List<TrendyolOrder> orders) {
        return orders.stream()
                .map(order -> order.getStoppage() != null ? order.getStoppage() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Siparişlerin estimatedCommission toplamını hesaplar.
     * Fatura komisyonu yoksa (Sandbox vb. durumlar) bu değer kullanılır.
     */
    private BigDecimal calculateTotalEstimatedCommissionFromOrders(List<TrendyolOrder> orders) {
        return orders.stream()
                .map(order -> order.getEstimatedCommission() != null ? order.getEstimatedCommission() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Helper class for product cost calculation result
    private static class ProductCostResult {
        private final BigDecimal totalCosts;
        private final int itemsWithoutCost;
        
        public ProductCostResult(BigDecimal totalCosts, int itemsWithoutCost) {
            this.totalCosts = totalCosts;
            this.itemsWithoutCost = itemsWithoutCost;
        }
        
        public BigDecimal getTotalCosts() {
            return totalCosts;
        }
        
        public int getItemsWithoutCost() {
            return itemsWithoutCost;
        }
    }
    
    private List<OrderDetailDto> calculateOrderDetails(UUID storeId, List<TrendyolOrder> revenueOrders, List<TrendyolOrder> returnedOrders) {
        // Build barcodeToCommissionRate map for NOT_SETTLED orders
        Set<String> allBarcodes = new HashSet<>();
        for (TrendyolOrder order : revenueOrders) {
            for (OrderItem item : order.getOrderItems()) {
                if (item.getBarcode() != null) {
                    allBarcodes.add(item.getBarcode());
                }
            }
        }

        Map<String, BigDecimal> barcodeToCommissionRate = new HashMap<>();
        if (!allBarcodes.isEmpty()) {
            List<TrendyolProduct> products = productRepository.findByStoreIdAndBarcodeIn(storeId, new ArrayList<>(allBarcodes));
            for (TrendyolProduct product : products) {
                if (product.getLastCommissionRate() != null) {
                    barcodeToCommissionRate.put(product.getBarcode(), product.getLastCommissionRate());
                }
            }
        }

        return revenueOrders.stream()
                .map(order -> {
                    // Siparişin ürünlerini listele
                    List<OrderProductDetailDto> products = order.getOrderItems().stream()
                            .map(item -> {
                                BigDecimal itemPrice = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
                                BigDecimal itemCost = item.getCost() != null ? item.getCost() : BigDecimal.ZERO;
                                int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                                BigDecimal totalCost = itemCost.multiply(BigDecimal.valueOf(qty));
                                // Get actual commission from Settlement API if available, otherwise estimate from lastCommissionRate
                                BigDecimal commission = getActualItemCommission(order, item, barcodeToCommissionRate);
                                BigDecimal profit = itemPrice.subtract(totalCost).subtract(commission);

                                return OrderProductDetailDto.builder()
                                        .barcode(item.getBarcode())
                                        .productName(item.getProductName())
                                        .quantity(qty)
                                        .unitPrice(qty > 0 ? itemPrice.divide(BigDecimal.valueOf(qty), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO)
                                        .totalPrice(itemPrice)
                                        .cost(itemCost)
                                        .totalCost(totalCost)
                                        .commission(commission)
                                        .profit(profit)
                                        .build();
                            })
                            .toList();

                    // Sipariş için ciro hesaplama (gross_amount - total_discount)
                    BigDecimal grossAmount = order.getGrossAmount() != null ? order.getGrossAmount() : BigDecimal.ZERO;
                    BigDecimal totalDiscount = order.getTotalDiscount() != null ? order.getTotalDiscount() : BigDecimal.ZERO;
                    BigDecimal orderRevenue = grossAmount.subtract(totalDiscount);

                    // Sipariş için toplam maliyet hesaplama
                    BigDecimal orderTotalCost = order.getOrderItems().stream()
                            .filter(item -> item.getCost() != null && item.getCost().compareTo(BigDecimal.ZERO) > 0)
                            .map(item -> item.getCost().multiply(BigDecimal.valueOf(item.getQuantity())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    // Brüt kar hesaplama
                    BigDecimal orderGrossProfit = orderRevenue.subtract(orderTotalCost);

                    // İade fiyatı (şimdilik 0, gerekirse daha sonra hesaplanabilir)
                    BigDecimal returnPrice = BigDecimal.ZERO;

                    // Stoppage amount
                    BigDecimal stoppage = order.getStoppage() != null ? order.getStoppage() : BigDecimal.ZERO;

                    // Commission (actual from Settlement API if SETTLED, otherwise estimated from lastCommissionRate)
                    BigDecimal estimatedCommission = getActualCommission(order, barcodeToCommissionRate);

                    // Shipping cost (from cargo invoice or estimated)
                    BigDecimal shippingCost = order.getEstimatedShippingCost() != null ? order.getEstimatedShippingCost() : BigDecimal.ZERO;

                    return OrderDetailDto.builder()
                            .orderNumber(order.getTyOrderNumber())
                            .orderDate(order.getOrderDate())
                            .products(products)
                            .totalPrice(grossAmount)
                            .returnPrice(returnPrice)
                            .revenue(orderRevenue)
                            .grossProfit(orderGrossProfit)
                            .stoppage(stoppage)
                            .estimatedCommission(estimatedCommission)
                            .estimatedShippingCost(shippingCost)
                            .build();
                })
                .toList();
    }
    
    private List<ProductDetailDto> calculateProductDetails(
            UUID storeId, List<TrendyolOrder> revenueOrders, List<TrendyolOrder> returnedOrders,
            LocalDateTime startDateTime, LocalDateTime endDateTime,
            BigDecimal totalRevenue, BigDecimal totalPlatformFees, BigDecimal totalExpenses, BigDecimal totalInvoicedDeductions) {
        // Per-order kargo maliyetlerini cargo invoices tablosundan al
        LocalDate startDate = startDateTime.toLocalDate();
        LocalDate endDate = endDateTime.toLocalDate();

        // Build orderNumber → shipping cost map from cargo invoices
        Map<String, BigDecimal> orderShippingCostMap = new HashMap<>();
        List<TrendyolCargoInvoice> cargoInvoices = cargoInvoiceRepository
                .findByStoreIdAndInvoiceDateBetweenOrderByInvoiceDateDesc(storeId, startDate, endDate);

        for (TrendyolCargoInvoice invoice : cargoInvoices) {
            if (invoice.getOrderNumber() != null && invoice.getAmount() != null) {
                orderShippingCostMap.merge(invoice.getOrderNumber(), invoice.getAmount(), BigDecimal::add);
            }
        }

        BigDecimal totalCargoFees = orderShippingCostMap.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("PRODUCT_DETAILS: Total cargo fees from {} cargo invoices: {} for store {} from {} to {}",
                cargoInvoices.size(), totalCargoFees, storeId, startDate, endDate);

        // ============== FALLBACK: Kargo faturası olmayan siparişler için ==============
        // Barcode bazlı EN SON kargo payını al (en güncel faturadan)
        // Bu, sipariş bazında kargo faturası olmadığında fallback olarak kullanılır
        Map<String, BigDecimal> barcodeLatestShippingMap = new HashMap<>();
        try {
            var latestShippingResults = cargoInvoiceRepository.findLatestShippingShareByBarcode(storeId);
            for (var row : latestShippingResults) {
                String barcode = row.getBarcode();
                BigDecimal latestShipping = row.getShippingShare() != null ? row.getShippingShare() : BigDecimal.ZERO;
                if (barcode != null && latestShipping.compareTo(BigDecimal.ZERO) > 0) {
                    barcodeLatestShippingMap.put(barcode, latestShipping);
                }
            }
            log.info("PRODUCT_DETAILS: Loaded {} barcode latest shipping costs for fallback", barcodeLatestShippingMap.size());
        } catch (Exception e) {
            log.warn("PRODUCT_DETAILS: Failed to load barcode latest shipping costs for fallback: {}", e.getMessage());
        }

        // Ürün bazlı metric tracking için helper class
        class ProductMetrics {
            String productName;
            String barcode;
            String brand;
            String image;
            String productUrl;
            Integer stock;
            int totalSoldQuantity = 0;
            int returnQuantity = 0;
            BigDecimal revenue = BigDecimal.ZERO;
            BigDecimal grossProfit = BigDecimal.ZERO;
            BigDecimal estimatedCommission = BigDecimal.ZERO;
            BigDecimal productCost = BigDecimal.ZERO;
            BigDecimal sellerDiscount = BigDecimal.ZERO;
            BigDecimal platformDiscount = BigDecimal.ZERO;
            BigDecimal couponDiscount = BigDecimal.ZERO;
            BigDecimal refundCost = BigDecimal.ZERO;
            BigDecimal shippingCost = BigDecimal.ZERO; // Kargo maliyeti (sipariş bazlı dağıtım)
            Set<String> orderNumbers = new HashSet<>(); // Bu ürünü içeren benzersiz sipariş numaraları
            // Reklam metrikleri (CPC/CVR)
            BigDecimal cpc = null; // Cost Per Click (TL)
            BigDecimal cvr = null; // Conversion Rate (örn: 0.018 = %1.8)
            BigDecimal salePrice = BigDecimal.ZERO; // Ürünün satış fiyatı (ACOS hesabı için)
        }

        Map<String, ProductMetrics> productMap = new HashMap<>();

        // PERFORMANCE FIX: Collect all barcodes first, then batch fetch products
        Set<String> allBarcodes = new HashSet<>();
        for (TrendyolOrder order : revenueOrders) {
            for (OrderItem item : order.getOrderItems()) {
                if (item.getBarcode() != null) {
                    allBarcodes.add(item.getBarcode());
                }
            }
        }
        for (TrendyolOrder order : returnedOrders) {
            for (OrderItem item : order.getOrderItems()) {
                if (item.getBarcode() != null) {
                    allBarcodes.add(item.getBarcode());
                }
            }
        }

        // Single batch query instead of N+1 queries
        Map<String, TrendyolProduct> barcodeToProductMap = new HashMap<>();
        Map<String, BigDecimal> barcodeToCommissionRate = new HashMap<>();
        if (!allBarcodes.isEmpty()) {
            List<TrendyolProduct> products = productRepository.findByStoreIdAndBarcodeIn(storeId, new ArrayList<>(allBarcodes));
            for (TrendyolProduct product : products) {
                barcodeToProductMap.put(product.getBarcode(), product);
                if (product.getLastCommissionRate() != null) {
                    barcodeToCommissionRate.put(product.getBarcode(), product.getLastCommissionRate());
                }
            }
        }

        // Revenue siparişlerinden ürün bilgilerini topla
        for (TrendyolOrder order : revenueOrders) {
            // Sipariş bazında indirim oranı hesapla (ürünlere dağıtmak için)
            BigDecimal orderGrossAmount = order.getGrossAmount() != null ? order.getGrossAmount() : BigDecimal.ZERO;
            BigDecimal orderTotalDiscount = order.getTotalDiscount() != null ? order.getTotalDiscount() : BigDecimal.ZERO;
            BigDecimal orderTyDiscount = order.getTotalTyDiscount() != null ? order.getTotalTyDiscount() : BigDecimal.ZERO;
            BigDecimal orderCouponDiscount = order.getCouponDiscount() != null ? order.getCouponDiscount() : BigDecimal.ZERO;
            BigDecimal orderSellerDiscount = orderTotalDiscount.subtract(orderTyDiscount).subtract(orderCouponDiscount);

            for (OrderItem item : order.getOrderItems()) {
                String barcode = item.getBarcode();
                if (barcode == null) continue;

                String productName = item.getProductName();

                // Get product from pre-fetched map (no DB call here)
                TrendyolProduct trendyolProduct = barcodeToProductMap.get(barcode);

                String imageUrl = trendyolProduct != null ? trendyolProduct.getImage() : null;
                Integer stock = trendyolProduct != null ? trendyolProduct.getTrendyolQuantity() : 0;
                String brand = trendyolProduct != null ? trendyolProduct.getBrand() : null;
                String productUrl = trendyolProduct != null ? trendyolProduct.getProductUrl() : null;

                ProductMetrics metrics = productMap.computeIfAbsent(barcode, k -> {
                    ProductMetrics m = new ProductMetrics();
                    m.productName = productName;
                    m.barcode = barcode;
                    m.brand = brand;
                    m.image = imageUrl;
                    m.productUrl = productUrl;
                    m.stock = stock;
                    // Reklam metrikleri (CPC/CVR) - ürün bazlı
                    if (trendyolProduct != null) {
                        m.cpc = trendyolProduct.getCpc();
                        m.cvr = trendyolProduct.getCvr();
                        m.salePrice = trendyolProduct.getSalePrice() != null ? trendyolProduct.getSalePrice() : BigDecimal.ZERO;
                    }
                    return m;
                });

                // Sipariş numarasını takip et (benzersiz sipariş sayısı için)
                if (order.getTyOrderNumber() != null) {
                    metrics.orderNumbers.add(order.getTyOrderNumber());
                }

                // Satış miktarını ekle
                int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                metrics.totalSoldQuantity += qty;

                // Ürün için ciro hesaplama (price zaten qty ile çarpılmış gelir Trendyol'dan)
                BigDecimal itemRevenue = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
                metrics.revenue = metrics.revenue.add(itemRevenue);

                // Ürün için maliyet hesaplama
                BigDecimal itemCost = BigDecimal.ZERO;
                if (item.getCost() != null && item.getCost().compareTo(BigDecimal.ZERO) > 0) {
                    itemCost = item.getCost().multiply(BigDecimal.valueOf(qty));
                }
                metrics.productCost = metrics.productCost.add(itemCost);

                // Brüt kar
                BigDecimal itemGrossProfit = itemRevenue.subtract(itemCost);
                metrics.grossProfit = metrics.grossProfit.add(itemGrossProfit);

                // Komisyon hesaplama
                BigDecimal itemCommission = getActualItemCommission(order, item, barcodeToCommissionRate);
                metrics.estimatedCommission = metrics.estimatedCommission.add(itemCommission);

                // İndirim dağıtımı (ürün cirosu oranında)
                if (orderGrossAmount.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal itemRatio = itemRevenue.divide(orderGrossAmount, 4, RoundingMode.HALF_UP);
                    // Satıcı indirimi payı
                    if (orderSellerDiscount.compareTo(BigDecimal.ZERO) > 0) {
                        metrics.sellerDiscount = metrics.sellerDiscount.add(orderSellerDiscount.multiply(itemRatio));
                    }
                    // Platform indirimi payı
                    if (orderTyDiscount.compareTo(BigDecimal.ZERO) > 0) {
                        metrics.platformDiscount = metrics.platformDiscount.add(orderTyDiscount.multiply(itemRatio));
                    }
                    // Kupon indirimi payı
                    if (orderCouponDiscount.compareTo(BigDecimal.ZERO) > 0) {
                        metrics.couponDiscount = metrics.couponDiscount.add(orderCouponDiscount.multiply(itemRatio));
                    }
                    // Kargo maliyeti dağıtımı (cargo invoices'dan gerçek kargo maliyeti)
                    BigDecimal orderShippingCost = orderShippingCostMap.getOrDefault(
                            order.getTyOrderNumber(), BigDecimal.ZERO);
                    if (orderShippingCost.compareTo(BigDecimal.ZERO) > 0) {
                        // GERÇEK KARGO MALİYETİ (kargo faturasından)
                        metrics.shippingCost = metrics.shippingCost.add(orderShippingCost.multiply(itemRatio));
                    } else {
                        // FALLBACK: Kargo faturası yoksa, barcode'un EN SON kargo faturasındaki payını kullan
                        // Bu, Trendyol'un henüz kargo faturası kesmediği siparişler için referans değer sağlar
                        BigDecimal fallbackShipping = barcodeLatestShippingMap.getOrDefault(barcode, BigDecimal.ZERO);
                        if (fallbackShipping.compareTo(BigDecimal.ZERO) > 0) {
                            // En son kargo faturasındaki payı itemRatio ile dağıt (diğer maliyetler gibi)
                            metrics.shippingCost = metrics.shippingCost.add(fallbackShipping.multiply(itemRatio));
                        }
                    }
                }
            }
        }

        // İade siparişlerinden iade miktarlarını ekle
        for (TrendyolOrder order : returnedOrders) {
            for (OrderItem item : order.getOrderItems()) {
                String barcode = item.getBarcode();
                if (barcode == null) continue;

                ProductMetrics metrics = productMap.get(barcode);
                if (metrics != null) {
                    int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                    metrics.returnQuantity += qty;

                    // İade maliyeti tahmini (ürün maliyeti + kargo)
                    BigDecimal itemCost = BigDecimal.ZERO;
                    if (item.getCost() != null) {
                        itemCost = item.getCost().multiply(BigDecimal.valueOf(qty));
                    }
                    metrics.refundCost = metrics.refundCost.add(itemCost);
                }
            }
        }

        // Ürün bazlı kargo maliyeti artık sipariş seviyesinde hesaplanıyor.
        // Her siparişin estimatedShippingCost değeri ürünlerin ciro oranına göre dağıtılıyor.

        // Convert to DTOs with calculated metrics
        return productMap.values().stream()
                .map(m -> {
                    // Toplam indirim
                    BigDecimal totalDiscount = m.sellerDiscount.add(m.platformDiscount).add(m.couponDiscount);

                    // Net ciro = brüt ciro - indirimler
                    BigDecimal netRevenue = m.revenue.subtract(totalDiscount);

                    // İade oranı (%)
                    BigDecimal refundRate = BigDecimal.ZERO;
                    if (m.totalSoldQuantity > 0) {
                        refundRate = BigDecimal.valueOf(m.returnQuantity)
                                .divide(BigDecimal.valueOf(m.totalSoldQuantity), 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .setScale(2, RoundingMode.HALF_UP);
                    }

                    // ============== REKLAM METRİKLERİ HESAPLAMA (NET KAR'DAN ÖNCE) ==============
                    // Reklam Maliyeti (satış başına) = CPC / CVR
                    // Örnek: CPC=3 TL, CVR=0.018 → 3 / 0.018 = 166.67 TL/satış
                    BigDecimal advertisingCostPerSale = null;
                    BigDecimal acos = null;
                    BigDecimal totalAdvertisingCost = null;

                    if (m.cpc != null && m.cvr != null && m.cvr.compareTo(BigDecimal.ZERO) > 0) {
                        // Satış başına reklam maliyeti
                        advertisingCostPerSale = m.cpc.divide(m.cvr, 2, RoundingMode.HALF_UP);

                        // ACOS = (reklamMaliyeti / satışFiyatı) * 100
                        if (m.salePrice.compareTo(BigDecimal.ZERO) > 0) {
                            acos = advertisingCostPerSale
                                    .divide(m.salePrice, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100))
                                    .setScale(2, RoundingMode.HALF_UP);
                        }

                        // Toplam reklam maliyeti = satış başına maliyet × satış adedi
                        if (m.totalSoldQuantity > 0) {
                            totalAdvertisingCost = advertisingCostPerSale
                                    .multiply(BigDecimal.valueOf(m.totalSoldQuantity))
                                    .setScale(2, RoundingMode.HALF_UP);
                        }
                    }

                    // ============== NET KAR HESAPLAMASI (Tutarlılık için güncellenmiş) ==============
                    // Net kar = brüt kar - komisyon - kargo - iade maliyeti - platform fees - giderler - faturalı kesintiler - reklam
                    // Ürün bazlı gider dağıtımı: (ürün geliri / toplam gelir) × toplam gider
                    BigDecimal revenueRatio = BigDecimal.ZERO;
                    if (totalRevenue != null && totalRevenue.compareTo(BigDecimal.ZERO) > 0 && m.revenue.compareTo(BigDecimal.ZERO) > 0) {
                        revenueRatio = m.revenue.divide(totalRevenue, 6, RoundingMode.HALF_UP);
                    }

                    // Ürün bazlı dağıtılmış giderler
                    BigDecimal allocatedPlatformFees = totalPlatformFees != null
                            ? totalPlatformFees.multiply(revenueRatio).setScale(2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    BigDecimal allocatedExpenses = totalExpenses != null
                            ? totalExpenses.multiply(revenueRatio).setScale(2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    BigDecimal allocatedInvoicedDeductions = totalInvoicedDeductions != null
                            ? totalInvoicedDeductions.multiply(revenueRatio).setScale(2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    // Net kar hesaplaması (dashboard ile tutarlı)
                    BigDecimal netProfit = m.grossProfit
                            .subtract(m.estimatedCommission)
                            .subtract(m.shippingCost)
                            .subtract(m.refundCost)
                            .subtract(allocatedPlatformFees)
                            .subtract(allocatedExpenses)
                            .subtract(allocatedInvoicedDeductions);
                    // Reklam maliyetini çıkar (eğer varsa)
                    if (totalAdvertisingCost != null) {
                        netProfit = netProfit.subtract(totalAdvertisingCost);
                    }

                    // Kar marjı (%)
                    BigDecimal profitMargin = BigDecimal.ZERO;
                    if (m.revenue.compareTo(BigDecimal.ZERO) > 0) {
                        profitMargin = m.grossProfit
                                .divide(m.revenue, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .setScale(2, RoundingMode.HALF_UP);
                    }

                    // ROI (%)
                    BigDecimal roi = BigDecimal.ZERO;
                    if (m.productCost.compareTo(BigDecimal.ZERO) > 0) {
                        roi = netProfit
                                .divide(m.productCost, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .setScale(2, RoundingMode.HALF_UP);
                    }

                    return ProductDetailDto.builder()
                            .productName(m.productName)
                            .barcode(m.barcode)
                            .brand(m.brand)
                            .image(m.image)
                            .productUrl(m.productUrl)
                            .stock(m.stock)
                            .totalSoldQuantity(m.totalSoldQuantity)
                            .returnQuantity(m.returnQuantity)
                            .revenue(m.revenue)
                            .grossProfit(m.grossProfit)
                            .estimatedCommission(m.estimatedCommission)
                            // Yeni alanlar
                            .sellerDiscount(m.sellerDiscount.setScale(2, RoundingMode.HALF_UP))
                            .platformDiscount(m.platformDiscount.setScale(2, RoundingMode.HALF_UP))
                            .couponDiscount(m.couponDiscount.setScale(2, RoundingMode.HALF_UP))
                            .totalDiscount(totalDiscount.setScale(2, RoundingMode.HALF_UP))
                            .netRevenue(netRevenue.setScale(2, RoundingMode.HALF_UP))
                            .productCost(m.productCost.setScale(2, RoundingMode.HALF_UP))
                            .shippingCost(m.shippingCost.setScale(2, RoundingMode.HALF_UP))
                            .refundCost(m.refundCost.setScale(2, RoundingMode.HALF_UP))
                            .refundRate(refundRate)
                            .netProfit(netProfit.setScale(2, RoundingMode.HALF_UP))
                            .profitMargin(profitMargin)
                            .roi(roi)
                            .orderCount(m.orderNumbers.size())
                            // Reklam metrikleri
                            .cpc(m.cpc)
                            .cvr(m.cvr)
                            .advertisingCostPerSale(advertisingCostPerSale)
                            .acos(acos)
                            .totalAdvertisingCost(totalAdvertisingCost)
                            .build();
                })
                .toList();
    }
    
    private List<PeriodExpenseDto> calculatePeriodExpenses(UUID storeId, LocalDate startDate, LocalDate endDate) {
        log.debug("Calculating expenses for store {} from {} to {}", storeId, startDate, endDate);
        
        // Get all expenses for the store
        List<StoreExpense> allExpenses = storeExpenseRepository.findByStoreIdOrderByDateDesc(storeId);
        
        Map<String, PeriodExpenseDto> expenseMap = new HashMap<>();
        
        for (StoreExpense expense : allExpenses) {
            int quantity = calculateExpenseQuantityForPeriod(expense, startDate, endDate);
            
            if (quantity > 0) {
                String key = expense.getName() + "_" + expense.getFrequency();
                BigDecimal totalAmount = expense.getAmount().multiply(BigDecimal.valueOf(quantity));
                
                expenseMap.merge(key, 
                    new PeriodExpenseDto(
                        expense.getName(),
                        quantity,
                        totalAmount,
                        expense.getFrequency()
                    ),
                    (existing, replacement) -> new PeriodExpenseDto(
                        existing.expenseName(),
                        existing.expenseQuantity() + replacement.expenseQuantity(),
                        existing.expenseTotal().add(replacement.expenseTotal()),
                        existing.expenseFrequency()
                    )
                );
            }
        }
        
        return expenseMap.values().stream().toList();
    }
    
    private int calculateExpenseQuantityForPeriod(StoreExpense expense, LocalDate startDate, LocalDate endDate) {
        LocalDate expenseDate = expense.getDate().toLocalDate();
        
        // If it's a one-time expense, check if it falls within the period
        if (expense.getFrequency() == ExpenseFrequency.ONE_TIME) {
            return (expenseDate.isEqual(startDate) || expenseDate.isAfter(startDate)) && 
                   (expenseDate.isEqual(endDate) || expenseDate.isBefore(endDate)) ? 1 : 0;
        }
        
        return switch (expense.getFrequency()) {
            case DAILY -> calculateDailyExpenseQuantity(expenseDate, startDate, endDate);
            case WEEKLY -> calculateWeeklyExpenseQuantity(expenseDate, startDate, endDate);
            case MONTHLY -> calculateMonthlyExpenseQuantity(expenseDate, startDate, endDate);
            case YEARLY -> calculateYearlyExpenseQuantity(expenseDate, startDate, endDate);
            default -> 0;
        };
    }
    
    private int calculateDailyExpenseQuantity(LocalDate expenseDate, LocalDate startDate, LocalDate endDate) {
        // For daily expenses, count the number of days in the period
        // Only count if expense was created before or during the period
        if (expenseDate.isAfter(endDate)) {
            return 0;
        }
        
        LocalDate effectiveStart = expenseDate.isAfter(startDate) ? expenseDate : startDate;
        
        return (int) (endDate.toEpochDay() - effectiveStart.toEpochDay() + 1);
    }
    
    private int calculateWeeklyExpenseQuantity(LocalDate expenseDate, LocalDate startDate, LocalDate endDate) {
        // For weekly expenses, count Mondays in the period (or the day of week when expense was created)
        if (expenseDate.isAfter(endDate)) {
            return 0;
        }
        
        int dayOfWeek = expenseDate.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday
        LocalDate current = startDate;
        int count = 0;
        
        while (!current.isAfter(endDate)) {
            if (current.getDayOfWeek().getValue() == dayOfWeek && !current.isBefore(expenseDate)) {
                count++;
            }
            current = current.plusDays(1);
        }
        
        return count;
    }
    
    private int calculateMonthlyExpenseQuantity(LocalDate expenseDate, LocalDate startDate, LocalDate endDate) {
        // For monthly expenses, count if the first day of any month in the period falls within our range
        // and the expense was created before that month
        if (expenseDate.isAfter(endDate)) {
            return 0;
        }
        
        LocalDate current = startDate.withDayOfMonth(1); // Start from first day of start month
        int count = 0;
        
        while (!current.isAfter(endDate)) {
            // Check if this month's first day is within our period and after expense creation
            if (!current.isBefore(startDate) && 
                !current.isAfter(endDate) && 
                !current.isBefore(expenseDate)) {
                count++;
            }
            
            current = current.plusMonths(1).withDayOfMonth(1); // Move to first day of next month
        }
        
        return count;
    }
    
    private int calculateYearlyExpenseQuantity(LocalDate expenseDate, LocalDate startDate, LocalDate endDate) {
        // For yearly expenses, count if January 1st of any year in the period falls within our range
        // and the expense was created before that year
        if (expenseDate.isAfter(endDate)) {
            return 0;
        }
        
        int startYear = startDate.getYear();
        int endYear = endDate.getYear();
        int count = 0;
        
        for (int year = startYear; year <= endYear; year++) {
            LocalDate yearlyExpenseDate = LocalDate.of(year, 1, 1); // January 1st
            
            if (!yearlyExpenseDate.isBefore(startDate) && 
                !yearlyExpenseDate.isAfter(endDate) && 
                !yearlyExpenseDate.isBefore(expenseDate)) {
                count++;
            }
        }
        
        return count;
    }
    
    private BigDecimal calculateTotalEstimatedCommission(List<TrendyolOrder> orders, UUID storeId) {
        // Batch fetch all products for commission rate lookup
        Set<String> allBarcodes = new HashSet<>();
        for (TrendyolOrder order : orders) {
            for (OrderItem item : order.getOrderItems()) {
                if (item.getBarcode() != null) {
                    allBarcodes.add(item.getBarcode());
                }
            }
        }

        Map<String, BigDecimal> barcodeToCommissionRate = new HashMap<>();
        if (!allBarcodes.isEmpty()) {
            List<TrendyolProduct> products = productRepository.findByStoreIdAndBarcodeIn(storeId, new ArrayList<>(allBarcodes));
            for (TrendyolProduct product : products) {
                if (product.getLastCommissionRate() != null) {
                    barcodeToCommissionRate.put(product.getBarcode(), product.getLastCommissionRate());
                }
            }
        }

        return orders.stream()
                .map(order -> getActualCommission(order, barcodeToCommissionRate))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ============== YENİ HESAPLAMA METODLARI (32 Metrik) ==============

    /**
     * Platform ücretleri hesaplamaları (15 kategori).
     * TrendyolDeductionInvoice tablosundaki transactionType alanına göre kategorize eder.
     * Stopaj (vergi kesintisi) hala TrendyolStoppage tablosundan gelir.
     */
    private PlatformFeesResult calculatePlatformFees(UUID storeId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        // Platform ücretleri DeductionInvoices tablosundan (OtherFinancials API)
        BigDecimal internationalServiceFee = deductionInvoiceRepository.sumByTransactionType(
                storeId, startDateTime, endDateTime, "Uluslararası Hizmet Bedeli");
        BigDecimal overseasOperationFee = deductionInvoiceRepository.sumByTransactionType(
                storeId, startDateTime, endDateTime, "Yurt Dışı Operasyon Bedeli");
        BigDecimal platformServiceFee = deductionInvoiceRepository.sumByTransactionType(
                storeId, startDateTime, endDateTime, "Platform Hizmet Bedeli");
        // AZ-Uluslararası Hizmet Bedeli: invoicedInternationalFees içinde zaten sayılıyor, çift sayımı önle
        BigDecimal azOverseasOperationFee = BigDecimal.ZERO;
        BigDecimal azPlatformServiceFee = deductionInvoiceRepository.sumByTransactionType(
                storeId, startDateTime, endDateTime, "AZ-Platform Hizmet Bedeli");

        // Diğer platform ücretleri: invoiced* sorgularıyla %100 çakışıyor, çift sayımı önlemek için kaldırıldı
        BigDecimal otherPlatformFees = BigDecimal.ZERO;

        // Bu türler DeductionInvoices'ta yok, sıfır olarak döndür
        BigDecimal terminDelayFee = BigDecimal.ZERO;
        BigDecimal invoiceCreditFee = BigDecimal.ZERO;
        BigDecimal unsuppliedFee = BigDecimal.ZERO;
        BigDecimal packagingServiceFee = BigDecimal.ZERO;
        BigDecimal warehouseServiceFee = BigDecimal.ZERO;
        BigDecimal callCenterFee = BigDecimal.ZERO;
        BigDecimal photoShootingFee = BigDecimal.ZERO;
        BigDecimal integrationFee = BigDecimal.ZERO;
        BigDecimal storageServiceFee = BigDecimal.ZERO;

        return new PlatformFeesResult(
                internationalServiceFee != null ? internationalServiceFee : BigDecimal.ZERO,
                overseasOperationFee != null ? overseasOperationFee : BigDecimal.ZERO,
                terminDelayFee,
                platformServiceFee != null ? platformServiceFee : BigDecimal.ZERO,
                invoiceCreditFee,
                unsuppliedFee,
                azOverseasOperationFee != null ? azOverseasOperationFee : BigDecimal.ZERO,
                azPlatformServiceFee != null ? azPlatformServiceFee : BigDecimal.ZERO,
                packagingServiceFee,
                warehouseServiceFee,
                callCenterFee,
                photoShootingFee,
                integrationFee,
                storageServiceFee,
                otherPlatformFees != null ? otherPlatformFees : BigDecimal.ZERO
        );
    }

    /**
     * Kategori bazlı gider hesaplamaları.
     * StoreExpense tablosundaki expenseCategory.name alanına göre kategorize eder.
     * Dinamik kategori desteği: Tüm kategoriler Map olarak döner, yeni kategoriler otomatik desteklenir.
     *
     * NOT: Bu metot artık frequency-aware hesaplama yapıyor (calculatePeriodExpenses ile aynı mantık).
     * Recurring expenses (DAILY, WEEKLY, MONTHLY, YEARLY) dönem içinde kaç kez uygulanacağına göre hesaplanır.
     */
    private CategorizedExpensesResult calculateCategorizedExpenses(UUID storeId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        LocalDate startDate = startDateTime.toLocalDate();
        LocalDate endDate = endDateTime.toLocalDate();

        // TÜM giderleri al (calculatePeriodExpenses ile aynı)
        List<StoreExpense> allExpenses = storeExpenseRepository.findByStoreIdWithRelations(storeId);

        // Kategori bazlı toplam hesapla (frequency-aware)
        Map<String, BigDecimal> expensesByCategory = new LinkedHashMap<>();

        // Hardcoded alanlar için de frequency-aware hesaplama
        BigDecimal officeExpenses = BigDecimal.ZERO;
        BigDecimal packagingExpenses = BigDecimal.ZERO;
        BigDecimal accountingExpenses = BigDecimal.ZERO;
        BigDecimal advertisingExpenses = BigDecimal.ZERO;
        BigDecimal otherExpenses = BigDecimal.ZERO;

        for (StoreExpense expense : allExpenses) {
            int quantity = calculateExpenseQuantityForPeriod(expense, startDate, endDate);

            if (quantity > 0) {
                BigDecimal totalAmount = expense.getAmount().multiply(BigDecimal.valueOf(quantity));

                // Dinamik kategori hesaplaması
                if (expense.getExpenseCategory() != null) {
                    String categoryName = expense.getExpenseCategory().getName();
                    expensesByCategory.merge(categoryName, totalAmount, BigDecimal::add);

                    // Hardcoded alanlar için keyword eşleştirmesi (geriye uyumluluk)
                    String lowerName = categoryName.toLowerCase();
                    if (lowerName.contains(EXPENSE_KEYWORD_OFFICE)) {
                        officeExpenses = officeExpenses.add(totalAmount);
                    } else if (lowerName.contains(EXPENSE_KEYWORD_PACKAGING) || lowerName.contains("paketleme")) {
                        packagingExpenses = packagingExpenses.add(totalAmount);
                    } else if (lowerName.contains(EXPENSE_KEYWORD_ACCOUNTING)) {
                        accountingExpenses = accountingExpenses.add(totalAmount);
                    } else if (lowerName.contains("reklam")) {
                        advertisingExpenses = advertisingExpenses.add(totalAmount);
                    } else {
                        // Diğer tüm kategoriler
                        otherExpenses = otherExpenses.add(totalAmount);
                    }
                }
            }
        }

        // Kategori toplamlarını büyükten küçüğe sırala
        Map<String, BigDecimal> sortedExpensesByCategory = expensesByCategory.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        log.debug("Calculated expenses by category for store {}: {}", storeId, sortedExpensesByCategory);

        return new CategorizedExpensesResult(
                officeExpenses,
                packagingExpenses,
                accountingExpenses,
                advertisingExpenses,
                otherExpenses,
                sortedExpensesByCategory
        );
    }

    /**
     * İndirim hesaplamaları (Satıcı, Platform, Kupon).
     * TrendyolOrder tablosundaki discount alanlarından hesaplanır.
     * Kupon indirimi Settlement API'den gelen transactionType=Coupon verilerinden alınır.
     */
    private DiscountsResult calculateDiscounts(UUID storeId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        log.info("Calculating discounts for store {} from {} to {}", storeId, startDateTime, endDateTime);

        BigDecimal sellerDiscount = orderRepository.sumSellerDiscount(storeId, startDateTime, endDateTime);
        BigDecimal platformDiscount = orderRepository.sumPlatformDiscount(storeId, startDateTime, endDateTime);
        BigDecimal couponDiscount = orderRepository.sumCouponDiscount(storeId, startDateTime, endDateTime);

        log.info("Raw discount values: sellerDiscount={}, platformDiscount={}, couponDiscount={}",
                sellerDiscount, platformDiscount, couponDiscount);

        // Toplam indirim hesaplama (kupon dahil)
        BigDecimal totalDiscount = (sellerDiscount != null ? sellerDiscount : BigDecimal.ZERO)
                .add(platformDiscount != null ? platformDiscount : BigDecimal.ZERO)
                .add(couponDiscount != null ? couponDiscount : BigDecimal.ZERO);

        log.info("Calculated discounts: seller={}, platform={}, coupon={}, total={}",
                sellerDiscount != null ? sellerDiscount : BigDecimal.ZERO,
                platformDiscount != null ? platformDiscount : BigDecimal.ZERO,
                couponDiscount != null ? couponDiscount : BigDecimal.ZERO,
                totalDiscount);

        return new DiscountsResult(
                sellerDiscount != null ? sellerDiscount : BigDecimal.ZERO,
                platformDiscount != null ? platformDiscount : BigDecimal.ZERO,
                couponDiscount != null ? couponDiscount : BigDecimal.ZERO,
                totalDiscount
        );
    }

    /**
     * İade oranı hesaplama.
     * İade oranı (%) = (İade sayısı / Toplam sipariş sayısı) * 100
     */
    private BigDecimal calculateRefundRate(int returnCount, int totalOrders) {
        if (totalOrders == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(returnCount)
                .divide(BigDecimal.valueOf(totalOrders), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Kargo maliyetleri hesaplama (dönem seviyesi).
     * TrendyolDeductionInvoice tablosundan fatura bazlı toplam (Faturalar sayfasıyla tutarlı).
     * Dahil edilen fatura tipleri: 'Kargo Fatura', 'Kargo Faturası', 'AZ - Kargo Fatura'
     *
     * NOT: Bu hesaplama Faturalar sayfasındaki KARGO kategorisiyle aynı kaynağı kullanır.
     * Böylece Dashboard ve Faturalar sayfası tutarlı değerler gösterir.
     *
     * @param storeId Mağaza ID
     * @param startDate Başlangıç tarihi
     * @param endDate Bitiş tarihi
     * @return ShippingCostsResult (shippingCost ve shippingIncome)
     */
    private ShippingCostsResult calculateShippingCosts(UUID storeId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        // CRITICAL FIX: Cargo invoices from Trendyol arrive 1-4 weeks after orders are placed.
        // Using invoice data for recent periods causes wrong calculations because:
        // - Invoice transaction_date = when invoice was issued (e.g., today)
        // - But the invoice covers orders from 1-4 weeks ago
        // Solution: Only use invoices for periods that ended at least 7 days ago
        LocalDate today = LocalDate.now(TURKEY_ZONE);
        boolean isRecentPeriod = endDate.isAfter(today.minusDays(7));

        BigDecimal cargoAmount;

        if (isRecentPeriod) {
            // Recent period (< 7 days old): ALWAYS use estimated shipping from orders
            // Invoices don't exist yet or would be for wrong period
            cargoAmount = orderRepository.sumEstimatedShippingCostByStoreAndDateRange(
                    storeId, startDateTime, endDateTime);
            if (cargoAmount == null) {
                cargoAmount = BigDecimal.ZERO;
            }
            log.debug("Shipping costs for store {} [{} to {}]: using estimated from orders (recent period)",
                    storeId, startDate, endDate);
        } else {
            // Older period (>= 7 days): Try invoices first, fallback to estimates
            BigDecimal invoicedAmount = deductionInvoiceRepository.sumCargoFeesByStoreIdAndDateRange(
                    storeId, startDateTime, endDateTime);
            if (invoicedAmount == null) {
                invoicedAmount = BigDecimal.ZERO;
            }

            if (invoicedAmount.compareTo(BigDecimal.ZERO) > 0) {
                cargoAmount = invoicedAmount;
                log.debug("Shipping costs for store {} [{} to {}]: invoiced={}",
                        storeId, startDate, endDate, cargoAmount);
            } else {
                cargoAmount = orderRepository.sumEstimatedShippingCostByStoreAndDateRange(
                        storeId, startDateTime, endDateTime);
                if (cargoAmount == null) {
                    cargoAmount = BigDecimal.ZERO;
                }
                log.debug("Shipping costs for store {} [{} to {}]: estimated from orders (no invoice)",
                        storeId, startDate, endDate);
            }
        }

        log.info("✅ Shipping costs for store {} [{} to {}]: {} TL ({})",
                storeId, startDate, endDate, cargoAmount,
                isRecentPeriod ? "ESTIMATED" : "INVOICED");

        return new ShippingCostsResult(cargoAmount, BigDecimal.ZERO);
    }

    // ============== RESULT HOLDER CLASSES ==============

    /**
     * Platform ücretleri sonuç sınıfı (15 kategori).
     */
    private static class PlatformFeesResult {
        private final BigDecimal internationalServiceFee;
        private final BigDecimal overseasOperationFee;      // Yurt Dışı Operasyon Bedeli
        private final BigDecimal terminDelayFee;
        private final BigDecimal platformServiceFee;        // Platform Hizmet Bedeli
        private final BigDecimal invoiceCreditFee;          // Fatura Kontör Satış Bedeli
        private final BigDecimal unsuppliedFee;             // Tedarik Edememe
        private final BigDecimal azOverseasOperationFee;    // AZ-Yurtdışı Operasyon Bedeli
        private final BigDecimal azPlatformServiceFee;      // AZ-Platform Hizmet Bedeli
        private final BigDecimal packagingServiceFee;
        private final BigDecimal warehouseServiceFee;
        private final BigDecimal callCenterFee;
        private final BigDecimal photoShootingFee;
        private final BigDecimal integrationFee;
        private final BigDecimal storageServiceFee;
        private final BigDecimal otherPlatformFees;

        public PlatformFeesResult(BigDecimal internationalServiceFee, BigDecimal overseasOperationFee,
                                   BigDecimal terminDelayFee, BigDecimal platformServiceFee,
                                   BigDecimal invoiceCreditFee, BigDecimal unsuppliedFee,
                                   BigDecimal azOverseasOperationFee, BigDecimal azPlatformServiceFee,
                                   BigDecimal packagingServiceFee, BigDecimal warehouseServiceFee,
                                   BigDecimal callCenterFee, BigDecimal photoShootingFee,
                                   BigDecimal integrationFee, BigDecimal storageServiceFee,
                                   BigDecimal otherPlatformFees) {
            this.internationalServiceFee = internationalServiceFee;
            this.overseasOperationFee = overseasOperationFee;
            this.terminDelayFee = terminDelayFee;
            this.platformServiceFee = platformServiceFee;
            this.invoiceCreditFee = invoiceCreditFee;
            this.unsuppliedFee = unsuppliedFee;
            this.azOverseasOperationFee = azOverseasOperationFee;
            this.azPlatformServiceFee = azPlatformServiceFee;
            this.packagingServiceFee = packagingServiceFee;
            this.warehouseServiceFee = warehouseServiceFee;
            this.callCenterFee = callCenterFee;
            this.photoShootingFee = photoShootingFee;
            this.integrationFee = integrationFee;
            this.storageServiceFee = storageServiceFee;
            this.otherPlatformFees = otherPlatformFees;
        }

        public BigDecimal getInternationalServiceFee() { return internationalServiceFee; }
        public BigDecimal getOverseasOperationFee() { return overseasOperationFee; }
        public BigDecimal getTerminDelayFee() { return terminDelayFee; }
        public BigDecimal getPlatformServiceFee() { return platformServiceFee; }
        public BigDecimal getInvoiceCreditFee() { return invoiceCreditFee; }
        public BigDecimal getUnsuppliedFee() { return unsuppliedFee; }
        public BigDecimal getAzOverseasOperationFee() { return azOverseasOperationFee; }
        public BigDecimal getAzPlatformServiceFee() { return azPlatformServiceFee; }
        public BigDecimal getPackagingServiceFee() { return packagingServiceFee; }
        public BigDecimal getWarehouseServiceFee() { return warehouseServiceFee; }
        public BigDecimal getCallCenterFee() { return callCenterFee; }
        public BigDecimal getPhotoShootingFee() { return photoShootingFee; }
        public BigDecimal getIntegrationFee() { return integrationFee; }
        public BigDecimal getStorageServiceFee() { return storageServiceFee; }
        public BigDecimal getOtherPlatformFees() { return otherPlatformFees; }

        public BigDecimal getTotal() {
            return internationalServiceFee.add(overseasOperationFee).add(terminDelayFee)
                    .add(platformServiceFee).add(invoiceCreditFee).add(unsuppliedFee)
                    .add(azOverseasOperationFee).add(azPlatformServiceFee)
                    .add(packagingServiceFee).add(warehouseServiceFee).add(callCenterFee)
                    .add(photoShootingFee).add(integrationFee).add(storageServiceFee)
                    .add(otherPlatformFees);
        }
    }

    /**
     * Kategori bazlı giderler sonuç sınıfı.
     * Hem eski hardcoded alanlar (geriye uyumluluk) hem de dinamik Map (yeni kategoriler) içerir.
     */
    private static class CategorizedExpensesResult {
        private final BigDecimal officeExpenses;
        private final BigDecimal packagingExpenses;
        private final BigDecimal accountingExpenses;
        private final BigDecimal advertisingExpenses;
        private final BigDecimal otherExpenses;
        private final Map<String, BigDecimal> expensesByCategory; // Dinamik kategoriler

        public CategorizedExpensesResult(BigDecimal officeExpenses, BigDecimal packagingExpenses,
                                          BigDecimal accountingExpenses, BigDecimal advertisingExpenses,
                                          BigDecimal otherExpenses, Map<String, BigDecimal> expensesByCategory) {
            this.officeExpenses = officeExpenses;
            this.packagingExpenses = packagingExpenses;
            this.accountingExpenses = accountingExpenses;
            this.advertisingExpenses = advertisingExpenses;
            this.otherExpenses = otherExpenses;
            this.expensesByCategory = expensesByCategory != null ? expensesByCategory : new HashMap<>();
        }

        public BigDecimal getOfficeExpenses() { return officeExpenses; }
        public BigDecimal getPackagingExpenses() { return packagingExpenses; }
        public BigDecimal getAccountingExpenses() { return accountingExpenses; }
        public BigDecimal getAdvertisingExpenses() { return advertisingExpenses; }
        public BigDecimal getOtherExpenses() { return otherExpenses; }
        public Map<String, BigDecimal> getExpensesByCategory() { return expensesByCategory; }

        public BigDecimal getTotal() {
            // Dinamik Map'ten toplam hesapla (daha doğru)
            if (!expensesByCategory.isEmpty()) {
                return expensesByCategory.values().stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            // Fallback: eski hardcoded alanlardan
            return officeExpenses.add(packagingExpenses).add(accountingExpenses)
                    .add(advertisingExpenses).add(otherExpenses);
        }
    }

    /**
     * İndirimler sonuç sınıfı.
     */
    private static class DiscountsResult {
        private final BigDecimal sellerDiscount;
        private final BigDecimal platformDiscount;
        private final BigDecimal couponDiscount;
        private final BigDecimal totalDiscount;

        public DiscountsResult(BigDecimal sellerDiscount, BigDecimal platformDiscount,
                                BigDecimal couponDiscount, BigDecimal totalDiscount) {
            this.sellerDiscount = sellerDiscount;
            this.platformDiscount = platformDiscount;
            this.couponDiscount = couponDiscount;
            this.totalDiscount = totalDiscount;
        }

        public BigDecimal getSellerDiscount() { return sellerDiscount; }
        public BigDecimal getPlatformDiscount() { return platformDiscount; }
        public BigDecimal getCouponDiscount() { return couponDiscount; }
        public BigDecimal getTotalDiscount() { return totalDiscount; }
    }

    /**
     * Kargo maliyetleri sonuç sınıfı.
     */
    private static class ShippingCostsResult {
        private final BigDecimal shippingCost;
        private final BigDecimal shippingIncome;

        public ShippingCostsResult(BigDecimal shippingCost, BigDecimal shippingIncome) {
            this.shippingCost = shippingCost;
            this.shippingIncome = shippingIncome;
        }

        public BigDecimal getShippingCost() { return shippingCost; }
        public BigDecimal getShippingIncome() { return shippingIncome; }
    }

    /**
     * Get actual commission for a specific item in an order.
     * Priority:
     * 1. If order is SETTLED → use real commission from financialTransactions
     * 2. If NOT_SETTLED → use product's lastCommissionRate to estimate
     * 3. Fall back to item's unitEstimatedCommission or zero
     */
    private BigDecimal getActualItemCommission(TrendyolOrder order, OrderItem item, Map<String, BigDecimal> barcodeToCommissionRate) {
        // Priority 1: If order is SETTLED and has financial transactions, use real commission
        if ("SETTLED".equals(order.getTransactionStatus()) &&
            order.getFinancialTransactions() != null &&
            !order.getFinancialTransactions().isEmpty() &&
            item.getBarcode() != null) {

            for (var itemData : order.getFinancialTransactions()) {
                if (item.getBarcode().equals(itemData.getBarcode()) &&
                    itemData.getTransactionSummary() != null &&
                    itemData.getTransactionSummary().getTotalCommission() != null) {
                    return itemData.getTransactionSummary().getTotalCommission();
                }
            }
        }

        // Priority 2: If NOT_SETTLED, use product's lastCommissionRate to estimate
        // Commission is calculated on item price (not vatBaseAmount which is just the VAT portion)
        if (item.getBarcode() != null && barcodeToCommissionRate != null) {
            BigDecimal commissionRate = barcodeToCommissionRate.get(item.getBarcode());
            if (commissionRate != null && commissionRate.compareTo(BigDecimal.ZERO) > 0) {
                // Calculate: price * (commissionRate / 100)
                // Note: price already includes quantity in Trendyol's order_items
                BigDecimal price = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
                if (price.compareTo(BigDecimal.ZERO) > 0) {
                    return price
                            .multiply(commissionRate)
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                }
            }
        }

        // Priority 3: Fall back to item's unitEstimatedCommission
        if (item.getUnitEstimatedCommission() != null &&
            item.getUnitEstimatedCommission().compareTo(BigDecimal.ZERO) > 0) {
            return item.getUnitEstimatedCommission().multiply(BigDecimal.valueOf(item.getQuantity()));
        }

        return BigDecimal.ZERO;
    }

    /**
     * Overloaded method for backward compatibility (without commission rate map)
     */
    private BigDecimal getActualItemCommission(TrendyolOrder order, OrderItem item) {
        return getActualItemCommission(order, item, null);
    }

    /**
     * Get actual commission for an order.
     * Priority:
     * 1. If order is SETTLED → use real commission from financialTransactions
     * 2. If NOT_SETTLED → calculate from each item using product's lastCommissionRate
     * 3. Fall back to order's estimatedCommission or zero
     */
    private BigDecimal getActualCommission(TrendyolOrder order, Map<String, BigDecimal> barcodeToCommissionRate) {
        // Priority 1: If order is SETTLED and has financial transactions, use real commission
        if ("SETTLED".equals(order.getTransactionStatus()) &&
            order.getFinancialTransactions() != null &&
            !order.getFinancialTransactions().isEmpty()) {

            BigDecimal totalCommission = BigDecimal.ZERO;
            for (var itemData : order.getFinancialTransactions()) {
                if (itemData.getTransactionSummary() != null &&
                    itemData.getTransactionSummary().getTotalCommission() != null) {
                    totalCommission = totalCommission.add(itemData.getTransactionSummary().getTotalCommission());
                }
            }

            if (totalCommission.compareTo(BigDecimal.ZERO) > 0) {
                return totalCommission;
            }
        }

        // Priority 2: If NOT_SETTLED, calculate from each item using product's lastCommissionRate
        if (barcodeToCommissionRate != null && !barcodeToCommissionRate.isEmpty()) {
            BigDecimal totalCommission = BigDecimal.ZERO;
            for (OrderItem item : order.getOrderItems()) {
                totalCommission = totalCommission.add(getActualItemCommission(order, item, barcodeToCommissionRate));
            }
            if (totalCommission.compareTo(BigDecimal.ZERO) > 0) {
                return totalCommission;
            }
        }

        // Priority 3: Fall back to order's estimatedCommission
        return order.getEstimatedCommission() != null ? order.getEstimatedCommission() : BigDecimal.ZERO;
    }

    /**
     * Overloaded method for backward compatibility (without commission rate map)
     */
    private BigDecimal getActualCommission(TrendyolOrder order) {
        return getActualCommission(order, null);
    }

    /**
     * Get deduction invoice breakdown by transaction type for a date range.
     * Used for dashboard detail panel to show all invoice types individually instead of grouped categories.
     *
     * @param storeId Store UUID
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of deduction breakdown by transaction type with debt and credit totals
     */
    public List<com.ecommerce.sellerx.dashboard.dto.DeductionBreakdownDto> getDeductionBreakdown(
            UUID storeId, LocalDate startDate, LocalDate endDate) {
        log.info("Getting deduction breakdown for store {} from {} to {}", storeId, startDate, endDate);

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        var projections = deductionInvoiceRepository.getDeductionBreakdownByStoreIdAndDateRange(storeId, start, end);

        return projections.stream()
                .map(p -> new com.ecommerce.sellerx.dashboard.dto.DeductionBreakdownDto(
                        p.getTransactionType(),
                        p.getTotalDebt() != null ? p.getTotalDebt() : BigDecimal.ZERO,
                        p.getTotalCredit() != null ? p.getTotalCredit() : BigDecimal.ZERO
                ))
                .toList();
    }
}
