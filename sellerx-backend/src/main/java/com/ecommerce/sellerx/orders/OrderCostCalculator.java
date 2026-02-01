package com.ecommerce.sellerx.orders;

import com.ecommerce.sellerx.products.CostAndStockInfo;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCostCalculator {
    
    private final TrendyolProductRepository productRepository;
    
    /**
     * Calculate and set cost information for an OrderItem builder using FIFO allocation
     */
    public void setCostInfo(OrderItem.OrderItemBuilder itemBuilder, String barcode, UUID storeId, 
                           LocalDateTime orderDate, Map<String, TrendyolProduct> productCache) {
        if (barcode == null || barcode.isEmpty()) {
            return;
        }
        
        TrendyolProduct product = null;
        
        // Use cache if available, otherwise query database
        if (productCache != null) {
            product = productCache.get(barcode);
        } else {
            Optional<TrendyolProduct> productOpt = productRepository.findByStoreIdAndBarcode(storeId, barcode);
            product = productOpt.orElse(null);
        }
        
        if (product != null) {
            // Find the first available stock entry using FIFO logic
            CostAndStockInfo appropriateCost = findFirstAvailableStockForOrder(product, orderDate.toLocalDate());
            
            if (appropriateCost != null) {
                itemBuilder.cost(appropriateCost.getUnitCost() != null ? 
                                BigDecimal.valueOf(appropriateCost.getUnitCost()) : null)
                          .costVat(appropriateCost.getCostVatRate())
                          .stockDate(appropriateCost.getStockDate()); // Set the stock date for tracking
                
                log.debug("Found cost {} from stock date {} for product {} on order date {}", 
                        appropriateCost.getUnitCost(), appropriateCost.getStockDate(), barcode, orderDate);
            } else {
                log.debug("No available stock found for product {} on order date {}", 
                        barcode, orderDate);
            }
            
            // Set commission information from product
            setCommissionInfo(itemBuilder, product);
        } else {
            log.debug("Product not found in trendyol_products for barcode: {}", barcode);
        }
    }
    
    /**
     * Set commission information for an OrderItem builder (as estimated values)
     * Uses fallback logic: lastCommissionRate → commissionRate → 0
     */
    public void setCommissionInfo(OrderItem.OrderItemBuilder itemBuilder, TrendyolProduct product) {
        // Commission rate fallback: lastCommissionRate → commissionRate → 0
        BigDecimal commissionRate = getEffectiveCommissionRate(product);

        if (commissionRate.compareTo(BigDecimal.ZERO) > 0) {
            itemBuilder.estimatedCommissionRate(commissionRate);
        }

        if (product.getShippingVolumeWeight() != null) {
            itemBuilder.estimatedShippingVolumeWeight(product.getShippingVolumeWeight());
        }
    }

    /**
     * Get effective commission rate with fallback logic.
     * Priority: lastCommissionRate (from Financial API) → commissionRate (from Product API) → 0
     *
     * @param product The product to get commission rate from
     * @return The effective commission rate
     */
    public BigDecimal getEffectiveCommissionRate(TrendyolProduct product) {
        if (product.getLastCommissionRate() != null) {
            return product.getLastCommissionRate();
        } else if (product.getCommissionRate() != null) {
            return product.getCommissionRate();
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get effective shipping cost per unit.
     * Returns lastShippingCostPerUnit from most recent cargo invoice, or 0 if not available.
     *
     * @param product The product to get shipping cost from
     * @return The effective shipping cost per unit (TL/adet)
     */
    public BigDecimal getEffectiveShippingCostPerUnit(TrendyolProduct product) {
        if (product.getLastShippingCostPerUnit() != null) {
            return product.getLastShippingCostPerUnit();
        }
        return BigDecimal.ZERO;
    }

    /**
     * Calculate unit estimated commission for an OrderItem
     *
     * IMPORTANT: In Trendyol API, the field named "vatBaseAmount" is actually the VAT RATE (e.g., 20 for 20%),
     * NOT the VAT base amount. This is a naming issue in Trendyol's API.
     *
     * Formula: (price / (1 + vatRate/100)) * commissionRate / 100
     *
     * Example:
     * - price: 799.40 TL (VAT included)
     * - vatRate: 20 (meaning 20%)
     * - actualVatBase: 799.40 / 1.20 = 666.17 TL
     * - commissionRate: 19%
     * - commission: 666.17 * 0.19 = 126.57 TL
     *
     * This is an ESTIMATED commission. Real commission is set by Financial API
     * and will update the order's isCommissionEstimated flag to false.
     *
     * @param price The item price (VAT included)
     * @param vatRate The VAT rate (e.g., 20 for 20%) - NOTE: This comes from vatBaseAmount field
     * @param commissionRate The commission rate (percentage)
     * @return The calculated commission amount
     */
    public BigDecimal calculateUnitEstimatedCommission(BigDecimal price,
                                                      BigDecimal vatRate,
                                                      BigDecimal commissionRate) {
        if (price == null || commissionRate == null) {
            return BigDecimal.ZERO;
        }

        // Default VAT rate to 20% if not provided (cosmetics category default in Turkey)
        BigDecimal effectiveVatRate = vatRate != null ? vatRate : BigDecimal.valueOf(20);

        // Calculate actual VAT base: price / (1 + vatRate/100)
        BigDecimal divisor = BigDecimal.ONE.add(effectiveVatRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        BigDecimal actualVatBase = price.divide(divisor, 2, RoundingMode.HALF_UP);

        // Calculate commission: actualVatBase * commissionRate / 100
        return actualVatBase.multiply(commissionRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate and set cost information for an OrderItem builder (without cache)
     */
    public void setCostInfo(OrderItem.OrderItemBuilder itemBuilder, String barcode, UUID storeId, 
                           LocalDateTime orderDate) {
        setCostInfo(itemBuilder, barcode, storeId, orderDate, null);
    }
    
    /**
     * Find the most appropriate cost for a product on a given order date
     */
    public CostAndStockInfo findAppropriateCostForProduct(TrendyolProduct product, LocalDate orderDate) {
        if (product.getCostAndStockInfo().isEmpty()) {
            log.debug("No cost information found for product with barcode: {}", product.getBarcode());
            return null;
        }
        
        // Sort cost and stock info by date (earliest first)
        List<CostAndStockInfo> sortedCosts = product.getCostAndStockInfo().stream()
                .filter(cost -> cost.getStockDate() != null)
                .sorted((c1, c2) -> c1.getStockDate().compareTo(c2.getStockDate()))
                .collect(Collectors.toList());
        
        if (sortedCosts.isEmpty()) {
            return null;
        }
        
        return findAppropriateCost(sortedCosts, orderDate);
    }
    
    /**
     * Find the most appropriate cost for the given order date
     * Logic: Use the latest cost entry that is on or before the order date
     */
    private CostAndStockInfo findAppropriateCost(List<CostAndStockInfo> sortedCosts, LocalDate orderDate) {
        CostAndStockInfo appropriateCost = null;
        
        for (CostAndStockInfo cost : sortedCosts) {
            // If cost date is after order date, break (since list is sorted)
            if (cost.getStockDate().isAfter(orderDate)) {
                break;
            }
            // This cost is on or before the order date, so it's a candidate
            appropriateCost = cost;
        }
        
        // If no cost found before or on order date, use the first available cost
        if (appropriateCost == null && !sortedCosts.isEmpty()) {
            appropriateCost = sortedCosts.get(0);
            log.debug("No cost found before order date, using earliest available cost from: {}", 
                     appropriateCost.getStockDate());
        }
        
        return appropriateCost;
    }
    
    /**
     * Find the first available stock entry for an order using FIFO logic
     * This method should ideally coordinate with StockOrderSynchronizationService
     * but for now provides basic FIFO selection
     */
    private CostAndStockInfo findFirstAvailableStockForOrder(TrendyolProduct product, LocalDate orderDate) {
        if (product.getCostAndStockInfo() == null || product.getCostAndStockInfo().isEmpty()) {
            return null;
        }
        
        // Get stock entries sorted by date (FIFO)
        return product.getCostAndStockInfo().stream()
                .filter(stock -> stock.getStockDate() != null)
                .filter(stock -> !stock.getStockDate().isAfter(orderDate)) // Stock must exist before order
                .filter(stock -> stock.getRemainingQuantity() > 0) // Must have remaining stock
                .sorted((s1, s2) -> s1.getStockDate().compareTo(s2.getStockDate())) // FIFO order
                .findFirst()
                .orElse(null);
    }
}
