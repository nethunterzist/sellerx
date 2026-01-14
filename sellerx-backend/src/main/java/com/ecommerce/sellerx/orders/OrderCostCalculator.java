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
     */
    public void setCommissionInfo(OrderItem.OrderItemBuilder itemBuilder, TrendyolProduct product) {
        if (product.getCommissionRate() != null) {
            itemBuilder.estimatedCommissionRate(product.getCommissionRate());
        }
        
        if (product.getShippingVolumeWeight() != null) {
            itemBuilder.estimatedShippingVolumeWeight(product.getShippingVolumeWeight());
        }
    }
    
    /**
     * Calculate unit estimated commission for an OrderItem
     * Formula: (unitPriceOrder - unitPriceDiscount) * commissionRate / 100
     */
    public BigDecimal calculateUnitEstimatedCommission(BigDecimal unitPriceOrder, 
                                                      BigDecimal unitPriceDiscount, 
                                                      BigDecimal commissionRate) {
        if (unitPriceOrder == null || commissionRate == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal discount = unitPriceDiscount != null ? unitPriceDiscount : BigDecimal.ZERO;
        BigDecimal netAmount = unitPriceOrder.subtract(discount);
        
        return netAmount.multiply(commissionRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
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
