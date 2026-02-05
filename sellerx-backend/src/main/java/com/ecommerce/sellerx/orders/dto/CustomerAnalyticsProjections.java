package com.ecommerce.sellerx.orders.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Projection interfaces for customer analytics native SQL queries.
 */
public class CustomerAnalyticsProjections {

    public interface SummaryProjection {
        Integer getTotalCustomers();
        Integer getRepeatCustomers();
        BigDecimal getTotalRevenue();
        BigDecimal getRepeatRevenue();
        Double getAvgOrdersPerCustomer();
        Double getAvgItemsPerCustomer();
    }

    public interface SegmentProjection {
        String getSegment();
        Integer getCustomerCount();
        BigDecimal getTotalRevenue();
    }

    public interface CityRepeatProjection {
        String getCity();
        Integer getTotalCustomers();
        Integer getRepeatCustomers();
        BigDecimal getTotalRevenue();
    }

    public interface MonthlyTrendProjection {
        String getMonth();
        Integer getNewCustomers();
        Integer getRepeatCustomers();
        BigDecimal getNewRevenue();
        BigDecimal getRepeatRevenue();
    }

    public interface ProductRepeatProjection {
        String getBarcode();
        String getProductName();
        Integer getTotalBuyers();
        Integer getRepeatBuyers();
        Integer getTotalQuantitySold();
        String getImage();
        String getProductUrl();
    }

    public interface CrossSellProjection {
        String getSourceBarcode();
        String getSourceProductName();
        String getTargetBarcode();
        String getTargetProductName();
        Integer getCoOccurrenceCount();
        String getSourceImage();
        String getSourceProductUrl();
        String getTargetImage();
        String getTargetProductUrl();
    }

    public interface CustomerSummaryProjection {
        Long getCustomerId();
        String getDisplayName();
        String getCity();
        Integer getOrderCount();
        Integer getItemCount();
        BigDecimal getTotalSpend();
        LocalDateTime getFirstOrderDate();
        LocalDateTime getLastOrderDate();
    }

    public interface BackfillCoverageProjection {
        Long getTotalOrders();
        Long getOrdersWithCustomerData();
    }

    /**
     * Lifecycle stage projection: customers grouped by lifecycle stage.
     */
    public interface LifecycleStageProjection {
        String getLifecycleStage();
        Integer getCustomerCount();
        BigDecimal getTotalRevenue();
    }

    /**
     * Cohort analysis projection: monthly cohort retention data.
     */
    public interface CohortProjection {
        String getCohortMonth();
        String getOrderMonth();
        Integer getActiveCustomers();
    }

    /**
     * Purchase frequency distribution projection.
     */
    public interface FrequencyDistributionProjection {
        String getFrequencyBucket();
        Integer getCustomerCount();
        BigDecimal getTotalRevenue();
        Integer getTotalOrders();
    }

    /**
     * CLV (Customer Lifetime Value) summary projection.
     */
    public interface ClvSummaryProjection {
        BigDecimal getAvgClv();
        BigDecimal getMedianClv();
        BigDecimal getTop10PercentClv();
        BigDecimal getTop10PercentRevenueShare();
    }
}
