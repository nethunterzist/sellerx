package com.ecommerce.sellerx.orders;

import com.ecommerce.sellerx.products.CostAndStockInfo;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockOrderSynchronizationService {
    
    private final TrendyolOrderRepository orderRepository;
    private final TrendyolProductRepository productRepository;

    /**
     * Synchronize orders after stock changes (add/update/delete)
     * This method is called when stock is added, updated, or deleted
     */
    @Transactional
    public void synchronizeOrdersAfterStockChange(UUID storeId, LocalDate changedStockDate) {
        log.info("Starting stock-order synchronization for store {} from date {}", storeId, changedStockDate);
        
        // Redistribute stock using FIFO for this store from the changed date onwards
        redistributeStockFIFO(storeId, changedStockDate);
        
        log.info("Completed stock-order synchronization for store {}", storeId);
    }

    /**
     * Redistribute stock using FIFO algorithm for a specific store
     */
    @Transactional
    public void redistributeStockFIFO(UUID storeId, LocalDate fromDate) {
        log.info("Starting FIFO stock redistribution for store {} from date {}", storeId, fromDate);
        
        // Get all products for this store
        List<TrendyolProduct> products = productRepository.findByStoreId(storeId);
        
        for (TrendyolProduct product : products) {
            if (product.getCostAndStockInfo() == null || product.getCostAndStockInfo().isEmpty()) {
                continue;
            }
            
            // Reset usage counters for stock entries from the changed date onwards
            resetStockUsageFromDate(product, fromDate);
            
            // Get all orders for this product from the specified date onwards
            List<TrendyolOrder> ordersToUpdate = orderRepository.findOrdersWithProductFromDate(
                    storeId, 
                    product.getBarcode(), 
                    fromDate.atStartOfDay()
            );
            
            if (ordersToUpdate.isEmpty()) {
                continue;
            }
            
            // Sort orders by date (FIFO)
            List<OrderItemWithOrder> orderItems = extractAndSortOrderItems(ordersToUpdate, product.getBarcode());
            
            // Redistribute stock using FIFO algorithm
            redistributeStockFIFO(product, orderItems);
            
            // Save updated product with new usage counts
            productRepository.save(product);
            
            // Save updated orders
            orderRepository.saveAll(ordersToUpdate);
        }
        
        log.info("Completed FIFO stock redistribution for store {}", storeId);
    }

    /**
     * Reset usage counters for stock entries from a specific date onwards
     */
    private void resetStockUsageFromDate(TrendyolProduct product, LocalDate fromDate) {
        if (product.getCostAndStockInfo() == null) {
            return;
        }
        
        for (CostAndStockInfo stockInfo : product.getCostAndStockInfo()) {
            if (stockInfo.getStockDate() != null && !stockInfo.getStockDate().isBefore(fromDate)) {
                stockInfo.setUsedQuantity(0);
            }
        }
    }

    /**
     * Extract and sort order items by date (FIFO)
     */
    private List<OrderItemWithOrder> extractAndSortOrderItems(List<TrendyolOrder> orders, String barcode) {
        List<OrderItemWithOrder> orderItems = new ArrayList<>();
        
        for (TrendyolOrder order : orders) {
            if (order.getOrderItems() != null) {
                for (OrderItem item : order.getOrderItems()) {
                    if (barcode.equals(item.getBarcode())) {
                        orderItems.add(new OrderItemWithOrder(item, order));
                    }
                }
            }
        }
        
        // Sort by order date (FIFO)
        orderItems.sort((a, b) -> a.getOrder().getOrderDate().compareTo(b.getOrder().getOrderDate()));
        
        return orderItems;
    }

    /**
     * Redistribute stock for a product using FIFO algorithm
     */
    private void redistributeStockFIFO(TrendyolProduct product, List<OrderItemWithOrder> orderItems) {
        // Sort stock entries by date (FIFO)
        List<CostAndStockInfo> sortedStock = product.getCostAndStockInfo().stream()
                .filter(stock -> stock.getStockDate() != null)
                .sorted((s1, s2) -> s1.getStockDate().compareTo(s2.getStockDate()))
                .collect(Collectors.toList());
        
        // Allocate stock to order items using FIFO
        for (OrderItemWithOrder orderItemWithOrder : orderItems) {
            OrderItem orderItem = orderItemWithOrder.getOrderItem();
            TrendyolOrder order = orderItemWithOrder.getOrder();
            
            // Find appropriate stock entry for this order
            CostAndStockInfo allocatedStock = allocateStockForOrderItem(sortedStock, orderItem, order.getOrderDate().toLocalDate());
            
            if (allocatedStock != null) {
                // Update order item with cost information
                orderItem.setCost(allocatedStock.getUnitCost() != null ? 
                                BigDecimal.valueOf(allocatedStock.getUnitCost()) : null);
                orderItem.setCostVat(allocatedStock.getCostVatRate());
                orderItem.setStockDate(allocatedStock.getStockDate());
                
                log.debug("Allocated stock from {} for order item: {} units at cost {}", 
                        allocatedStock.getStockDate(), orderItem.getQuantity(), allocatedStock.getUnitCost());
            } else {
                // No stock available, clear cost information
                orderItem.setCost(null);
                orderItem.setCostVat(null);
                orderItem.setStockDate(null);
                
                log.debug("No stock available for order item: {} units", orderItem.getQuantity());
            }
        }
    }

    /**
     * Allocate stock for a single order item using FIFO logic
     */
    private CostAndStockInfo allocateStockForOrderItem(List<CostAndStockInfo> sortedStock, OrderItem orderItem, LocalDate orderDate) {
        int neededQuantity = orderItem.getQuantity();
        
        for (CostAndStockInfo stockInfo : sortedStock) {
            // Stock must exist before or on order date
            if (stockInfo.getStockDate().isAfter(orderDate)) {
                continue;
            }
            
            int remainingStock = stockInfo.getRemainingQuantity();
            if (remainingStock > 0) {
                // Use available stock
                int usedFromThisStock = Math.min(neededQuantity, remainingStock);
                stockInfo.setUsedQuantity(stockInfo.getUsedQuantity() + usedFromThisStock);
                neededQuantity -= usedFromThisStock;
                
                // Return this stock info as the source for this order item
                return stockInfo;
            }
        }
        
        // No stock available
        return null;
    }

    /**
     * Helper class to pair OrderItem with its TrendyolOrder
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class OrderItemWithOrder {
        private OrderItem orderItem;
        private TrendyolOrder order;
    }
}
