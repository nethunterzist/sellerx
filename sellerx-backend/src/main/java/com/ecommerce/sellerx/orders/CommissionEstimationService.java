package com.ecommerce.sellerx.orders;

import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for estimating commission on orders that come from Orders API.
 *
 * Orders API does not provide commission data - that only comes from Settlement API
 * (typically 3-7 days later). This service estimates commission using reference data
 * from previously settled orders via the barcode-based lookup system.
 *
 * Commission Estimation Priority Chain:
 * 1. lastCommissionRate (from Financial/Settlement API - most accurate)
 * 2. commissionRate (from Product API - category default)
 * 3. 0 (no commission data available)
 *
 * Formula: commission = (price / (1 + vatRate/100)) * commissionRate / 100
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommissionEstimationService {

    private final TrendyolProductRepository productRepository;
    private final OrderCostCalculator orderCostCalculator;

    /**
     * Calculate estimated commission for an entire order.
     * Sums up the estimated commission for all order items.
     *
     * @param order The order to estimate commission for
     * @return Total estimated commission for the order
     */
    public BigDecimal calculateOrderEstimatedCommission(TrendyolOrder order) {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            return BigDecimal.ZERO;
        }

        UUID storeId = order.getStore().getId();

        // Build product cache for all barcodes in the order
        Map<String, TrendyolProduct> productCache = buildProductCache(order, storeId);

        BigDecimal totalCommission = BigDecimal.ZERO;

        for (OrderItem item : order.getOrderItems()) {
            BigDecimal itemCommission = calculateItemEstimatedCommission(item, productCache);
            totalCommission = totalCommission.add(itemCommission);
        }

        log.debug("Order {} total estimated commission: {}", order.getTyOrderNumber(), totalCommission);
        return totalCommission;
    }

    /**
     * Calculate estimated commission for a single order item using barcode lookup.
     *
     * @param item The order item
     * @param productCache Cache of products by barcode
     * @return Estimated commission for the item
     */
    public BigDecimal calculateItemEstimatedCommission(OrderItem item, Map<String, TrendyolProduct> productCache) {
        if (item.getBarcode() == null || item.getBarcode().isEmpty()) {
            log.debug("No barcode for order item, cannot estimate commission");
            return BigDecimal.ZERO;
        }

        TrendyolProduct product = productCache.get(item.getBarcode());
        if (product == null) {
            log.debug("Product not found for barcode: {}, cannot estimate commission", item.getBarcode());
            return BigDecimal.ZERO;
        }

        // Get effective commission rate using fallback chain
        BigDecimal commissionRate = orderCostCalculator.getEffectiveCommissionRate(product);
        if (commissionRate.compareTo(BigDecimal.ZERO) == 0) {
            log.debug("No commission rate available for barcode: {}", item.getBarcode());
            return BigDecimal.ZERO;
        }

        // Calculate commission: (price / (1 + vatRate/100)) * commissionRate / 100
        BigDecimal commission = orderCostCalculator.calculateUnitEstimatedCommission(
                item.getPrice(),
                item.getVatBaseAmount(), // Note: This is actually VAT rate, not VAT base (Trendyol naming issue)
                commissionRate
        );

        // Multiply by quantity if applicable
        Integer quantity = item.getQuantity() != null ? item.getQuantity() : 1;
        BigDecimal totalItemCommission = commission.multiply(BigDecimal.valueOf(quantity));

        log.debug("Item barcode={}, price={}, vatRate={}, commissionRate={}%, quantity={}, commission={}",
                item.getBarcode(), item.getPrice(), item.getVatBaseAmount(),
                commissionRate, quantity, totalItemCommission);

        return totalItemCommission;
    }

    /**
     * Estimate commission for an order and update the order entity.
     * Sets estimatedCommission and marks isCommissionEstimated as true.
     *
     * @param order The order to update with estimated commission
     */
    public void estimateAndSetOrderCommission(TrendyolOrder order) {
        BigDecimal estimatedCommission = calculateOrderEstimatedCommission(order);

        order.setEstimatedCommission(estimatedCommission);
        order.setIsCommissionEstimated(true);

        log.info("Set estimated commission {} for order {} (dataSource={})",
                estimatedCommission, order.getTyOrderNumber(), order.getDataSource());
    }

    /**
     * Estimate commission for an order item and update the item.
     * Uses the product's lastCommissionRate from Settlement API when available.
     *
     * @param item The order item to update
     * @param storeId The store ID for product lookup
     */
    public void estimateAndSetItemCommission(OrderItem item, UUID storeId) {
        if (item.getBarcode() == null || item.getBarcode().isEmpty()) {
            return;
        }

        Optional<TrendyolProduct> productOpt = productRepository.findByStoreIdAndBarcode(storeId, item.getBarcode());
        if (productOpt.isEmpty()) {
            log.debug("Product not found for barcode: {}", item.getBarcode());
            return;
        }

        TrendyolProduct product = productOpt.get();
        BigDecimal commissionRate = orderCostCalculator.getEffectiveCommissionRate(product);

        if (commissionRate.compareTo(BigDecimal.ZERO) > 0) {
            item.setEstimatedCommissionRate(commissionRate);

            BigDecimal unitCommission = orderCostCalculator.calculateUnitEstimatedCommission(
                    item.getPrice(),
                    item.getVatBaseAmount(),
                    commissionRate
            );
            item.setUnitEstimatedCommission(unitCommission);

            log.debug("Set item commission: barcode={}, rate={}%, unit={}",
                    item.getBarcode(), commissionRate, unitCommission);
        }
    }

    /**
     * Build a product cache for all barcodes in an order.
     * This reduces database queries by batch-loading all products at once.
     *
     * @param order The order containing items with barcodes
     * @param storeId The store ID for product lookup
     * @return Map of barcode to TrendyolProduct
     */
    private Map<String, TrendyolProduct> buildProductCache(TrendyolOrder order, UUID storeId) {
        // Extract all unique barcodes from order items
        var barcodes = order.getOrderItems().stream()
                .map(OrderItem::getBarcode)
                .filter(barcode -> barcode != null && !barcode.isEmpty())
                .distinct()
                .toList();

        if (barcodes.isEmpty()) {
            return Map.of();
        }

        // Batch load all products by barcodes
        return productRepository.findByStoreIdAndBarcodeIn(storeId, barcodes)
                .stream()
                .collect(Collectors.toMap(
                        TrendyolProduct::getBarcode,
                        Function.identity(),
                        (existing, replacement) -> existing // Keep first in case of duplicates
                ));
    }

    /**
     * Check if an order has estimated commission that needs reconciliation.
     *
     * @param order The order to check
     * @return true if the order has estimated commission
     */
    public boolean needsReconciliation(TrendyolOrder order) {
        return Boolean.TRUE.equals(order.getIsCommissionEstimated())
                && "ORDER_API".equals(order.getDataSource());
    }

    /**
     * Get commission estimation accuracy statistics for a store.
     * Compares estimated vs real commission from reconciled orders.
     *
     * @param storeId The store ID
     * @return Statistics object with accuracy metrics
     */
    public CommissionEstimationStats getEstimationStats(UUID storeId) {
        // This will be implemented when we add the reconciliation tracking
        // For now, return empty stats
        return new CommissionEstimationStats();
    }

    /**
     * Statistics class for commission estimation accuracy.
     */
    public static class CommissionEstimationStats {
        private int totalReconciled = 0;
        private BigDecimal totalEstimated = BigDecimal.ZERO;
        private BigDecimal totalReal = BigDecimal.ZERO;
        private BigDecimal averageAccuracy = BigDecimal.ZERO;

        // Getters
        public int getTotalReconciled() { return totalReconciled; }
        public BigDecimal getTotalEstimated() { return totalEstimated; }
        public BigDecimal getTotalReal() { return totalReal; }
        public BigDecimal getAverageAccuracy() { return averageAccuracy; }
    }
}
