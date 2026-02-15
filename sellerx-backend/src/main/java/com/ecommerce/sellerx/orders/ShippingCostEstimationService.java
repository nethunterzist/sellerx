package com.ecommerce.sellerx.orders;

import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for estimating shipping costs on orders that come from Orders API.
 *
 * Orders API does not provide shipping cost data - that only comes from Cargo Invoice API
 * (typically 1-3 days after shipment). This service estimates shipping cost using reference data
 * from previously processed cargo invoices via the barcode-based lookup system.
 *
 * Shipping Estimation Logic:
 * - Uses lastShippingCostPerUnit from TrendyolProduct (set by TrendyolOtherFinancialsService when cargo invoice arrives)
 * - If no shipping data available, returns 0 (cannot estimate)
 *
 * Formula: totalShipping = MAX(lastShippingCostPerUnit) across all items in the order
 * (Trendyol charges shipping per PACKAGE, not per item — one order = one package = one shipping fee)
 *
 * When real cargo invoice arrives:
 * - TrendyolOtherFinancialsService updates order.estimatedShippingCost with real value
 * - Sets order.isShippingEstimated = false
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingCostEstimationService {

    private final TrendyolProductRepository productRepository;
    private final OrderCostCalculator orderCostCalculator;

    /**
     * Calculate estimated shipping cost for an entire order.
     * Uses MAX(lastShippingCostPerUnit) across all items because Trendyol charges
     * shipping per PACKAGE (not per item). One order = one package = one shipping fee.
     *
     * @param order The order to estimate shipping for
     * @return Estimated shipping cost for the order (package-level)
     */
    public BigDecimal calculateOrderEstimatedShipping(TrendyolOrder order) {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            return BigDecimal.ZERO;
        }

        UUID storeId = order.getStore().getId();

        // Build product cache for all barcodes in the order
        Map<String, TrendyolProduct> productCache = buildProductCache(order, storeId);

        // Trendyol charges shipping per PACKAGE, not per item.
        // Use MAX across all items as the package shipping cost estimate.
        BigDecimal maxShippingCost = BigDecimal.ZERO;

        for (OrderItem item : order.getOrderItems()) {
            if (item.getBarcode() == null || item.getBarcode().isEmpty()) {
                continue;
            }

            TrendyolProduct product = productCache.get(item.getBarcode());
            if (product == null) {
                continue;
            }

            BigDecimal cost = orderCostCalculator.getEffectiveShippingCostPerUnit(product);
            if (cost.compareTo(maxShippingCost) > 0) {
                maxShippingCost = cost;
            }
        }

        log.debug("Order {} estimated package shipping (MAX): {}", order.getTyOrderNumber(), maxShippingCost);
        return maxShippingCost;
    }

    /**
     * Calculate estimated shipping cost for a single order item using barcode lookup.
     *
     * @param item The order item
     * @param productCache Cache of products by barcode
     * @return Estimated shipping cost for the item
     */
    public BigDecimal calculateItemEstimatedShipping(OrderItem item, Map<String, TrendyolProduct> productCache) {
        if (item.getBarcode() == null || item.getBarcode().isEmpty()) {
            log.debug("No barcode for order item, cannot estimate shipping");
            return BigDecimal.ZERO;
        }

        TrendyolProduct product = productCache.get(item.getBarcode());
        if (product == null) {
            log.debug("Product not found for barcode: {}, cannot estimate shipping", item.getBarcode());
            return BigDecimal.ZERO;
        }

        // Get effective shipping cost per unit using lookup
        BigDecimal shippingCostPerUnit = orderCostCalculator.getEffectiveShippingCostPerUnit(product);
        if (shippingCostPerUnit.compareTo(BigDecimal.ZERO) == 0) {
            log.debug("No shipping cost data available for barcode: {}", item.getBarcode());
            return BigDecimal.ZERO;
        }

        // Calculate shipping: shippingCostPerUnit × quantity
        Integer quantity = item.getQuantity() != null ? item.getQuantity() : 1;
        BigDecimal totalItemShipping = shippingCostPerUnit.multiply(BigDecimal.valueOf(quantity));

        log.debug("Item barcode={}, shippingCostPerUnit={}, quantity={}, totalShipping={}",
                item.getBarcode(), shippingCostPerUnit, quantity, totalItemShipping);

        return totalItemShipping;
    }

    /**
     * Estimate shipping cost for an order and update the order entity.
     * Sets estimatedShippingCost and marks isShippingEstimated as true.
     *
     * @param order The order to update with estimated shipping cost
     */
    public void estimateAndSetOrderShipping(TrendyolOrder order) {
        BigDecimal estimatedShipping = calculateOrderEstimatedShipping(order);

        order.setEstimatedShippingCost(estimatedShipping);
        order.setIsShippingEstimated(true);

        log.info("Set estimated shipping {} for order {} (dataSource={})",
                estimatedShipping, order.getTyOrderNumber(), order.getDataSource());
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
     * Check if an order has estimated shipping that needs reconciliation.
     *
     * @param order The order to check
     * @return true if the order has estimated shipping
     */
    public boolean needsShippingReconciliation(TrendyolOrder order) {
        return Boolean.TRUE.equals(order.getIsShippingEstimated())
                && "ORDER_API".equals(order.getDataSource());
    }
}
