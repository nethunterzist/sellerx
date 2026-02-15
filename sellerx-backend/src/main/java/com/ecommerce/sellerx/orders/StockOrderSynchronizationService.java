package com.ecommerce.sellerx.orders;

import com.ecommerce.sellerx.orders.event.StockChangedEvent;
import com.ecommerce.sellerx.products.CostAndStockInfo;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
     * Handles stock change events after transaction commits.
     * Runs asynchronously to not block the calling thread.
     */
    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStockChangedEvent(StockChangedEvent event) {
        log.info("Received stock change event for store {} product {} date {}",
            event.getStoreId(), event.getProductId(), event.getStockDate());
        synchronizeOrdersAfterStockChange(event.getStoreId(), event.getStockDate());
    }

    /**
     * Synchronize orders after stock changes (add/update/delete)
     */
    @Transactional
    public void synchronizeOrdersAfterStockChange(UUID storeId, LocalDate changedStockDate) {
        log.info("Starting stock-order synchronization for store {} from date {}", storeId, changedStockDate);
        redistributeStockFIFO(storeId, changedStockDate);
        log.info("Completed stock-order synchronization for store {}", storeId);
    }

    /**
     * Redistribute stock using FIFO algorithm for a specific store.
     * Handles split allocation across multiple lots and last-known-cost fallback.
     */
    @Transactional
    public void redistributeStockFIFO(UUID storeId, LocalDate fromDate) {
        log.info("Starting FIFO stock redistribution for store {} from date {}", storeId, fromDate);

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

            // Redistribute stock using FIFO algorithm with split allocation
            boolean hasDepletedAllocation = redistributeStockForProduct(product, orderItems);

            // Update stockDepleted flag
            product.setStockDepleted(hasDepletedAllocation);

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

        orderItems.sort((a, b) -> a.getOrder().getOrderDate().compareTo(b.getOrder().getOrderDate()));
        return orderItems;
    }

    /**
     * Redistribute stock for a product using FIFO algorithm with split allocation.
     * Returns true if any order item used LAST_KNOWN cost (stock depleted).
     */
    private boolean redistributeStockForProduct(TrendyolProduct product, List<OrderItemWithOrder> orderItems) {
        // Sort stock entries by date (FIFO)
        List<CostAndStockInfo> sortedStock = product.getCostAndStockInfo().stream()
                .filter(stock -> stock.getStockDate() != null)
                .sorted(Comparator.comparing(CostAndStockInfo::getStockDate))
                .collect(Collectors.toList());

        boolean hasDepletedAllocation = false;

        for (OrderItemWithOrder orderItemWithOrder : orderItems) {
            OrderItem orderItem = orderItemWithOrder.getOrderItem();
            TrendyolOrder order = orderItemWithOrder.getOrder();

            AllocationResult result = allocateStockForOrderItem(
                    sortedStock, orderItem, order.getOrderDate().toLocalDate());

            if (result != null) {
                orderItem.setCost(result.weightedAverageCost);
                orderItem.setCostVat(result.costVatRate);
                orderItem.setStockDate(result.earliestStockDate);
                orderItem.setCostSource(result.costSource);

                if ("LAST_KNOWN".equals(result.costSource)) {
                    hasDepletedAllocation = true;
                }

                log.debug("Allocated stock for order item: {} units, cost={}, source={}",
                        orderItem.getQuantity(), result.weightedAverageCost, result.costSource);
            } else {
                orderItem.setCost(null);
                orderItem.setCostVat(null);
                orderItem.setStockDate(null);
                orderItem.setCostSource(null);

                log.debug("No stock available for order item: {} units", orderItem.getQuantity());
            }
        }

        return hasDepletedAllocation;
    }

    /**
     * Allocate stock for a single order item using FIFO logic with SPLIT ALLOCATION.
     *
     * When an order spans multiple lots, calculates weighted average cost:
     * e.g., 15 units needed, Lot1 has 10 at 20TL, Lot2 has 50 at 22TL
     * → (10×20 + 5×22) / 15 = 20.67 TL
     *
     * Falls back to last known cost when all lots are depleted.
     */
    private AllocationResult allocateStockForOrderItem(
            List<CostAndStockInfo> sortedStock,
            OrderItem orderItem,
            LocalDate orderDate) {

        int neededQuantity = orderItem.getQuantity();
        int totalAllocated = 0;
        BigDecimal totalCostWeighted = BigDecimal.ZERO;
        Integer vatRate = null;
        LocalDate earliestDate = null;

        for (CostAndStockInfo stockInfo : sortedStock) {
            if (neededQuantity <= 0) break;

            // FIFO rule: stock must exist before or on order date
            if (stockInfo.getStockDate().isAfter(orderDate)) {
                continue;
            }

            int remainingStock = stockInfo.getRemainingQuantity();
            if (remainingStock > 0) {
                int usedFromThisStock = Math.min(neededQuantity, remainingStock);
                stockInfo.setUsedQuantity(stockInfo.getUsedQuantity() + usedFromThisStock);

                BigDecimal lotCost = stockInfo.getUnitCost() != null
                    ? BigDecimal.valueOf(stockInfo.getUnitCost())
                    : BigDecimal.ZERO;
                totalCostWeighted = totalCostWeighted.add(
                    lotCost.multiply(BigDecimal.valueOf(usedFromThisStock)));

                if (vatRate == null) {
                    vatRate = stockInfo.getCostVatRate();
                }
                if (earliestDate == null) {
                    earliestDate = stockInfo.getStockDate();
                }

                totalAllocated += usedFromThisStock;
                neededQuantity -= usedFromThisStock;
            }
        }

        // Successful FIFO allocation (full or partial)
        if (totalAllocated > 0) {
            BigDecimal weightedAvgCost = totalCostWeighted.divide(
                BigDecimal.valueOf(totalAllocated), 2, RoundingMode.HALF_UP);

            return new AllocationResult(weightedAvgCost, vatRate, earliestDate, "FIFO");
        }

        // LAST KNOWN COST FALLBACK: find the most recent lot by stockDate
        // regardless of remaining quantity
        CostAndStockInfo lastKnownLot = sortedStock.stream()
                .filter(s -> s.getStockDate() != null && !s.getStockDate().isAfter(orderDate))
                .reduce((first, second) -> second) // last element = most recent date (already sorted ASC)
                .orElse(null);

        if (lastKnownLot != null && lastKnownLot.getUnitCost() != null) {
            return new AllocationResult(
                BigDecimal.valueOf(lastKnownLot.getUnitCost()),
                lastKnownLot.getCostVatRate(),
                lastKnownLot.getStockDate(),
                "LAST_KNOWN"
            );
        }

        return null; // No cost data at all
    }

    /**
     * Result of a FIFO stock allocation for an order item
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class AllocationResult {
        private BigDecimal weightedAverageCost;
        private Integer costVatRate;
        private LocalDate earliestStockDate;
        private String costSource; // "FIFO" or "LAST_KNOWN"
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
