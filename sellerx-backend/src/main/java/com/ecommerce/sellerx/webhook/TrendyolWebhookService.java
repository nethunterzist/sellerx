package com.ecommerce.sellerx.webhook;

import com.ecommerce.sellerx.config.FinancialConstants;
import com.ecommerce.sellerx.orders.OrderCostCalculator;
import com.ecommerce.sellerx.orders.OrderItem;
import com.ecommerce.sellerx.orders.TrendyolOrder;
import com.ecommerce.sellerx.orders.TrendyolOrderRepository;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendyolWebhookService {
    
    private final TrendyolOrderRepository orderRepository;
    private final StoreRepository storeRepository;
    private final OrderCostCalculator costCalculator;
    
    /**
     * Process incoming webhook order data
     */
    public void processWebhookOrder(TrendyolWebhookPayload payload, String sellerId) {
        try {
            log.info("Processing webhook order: {} for seller: {}", payload.getOrderNumber(), sellerId);
            
            // Find the store by seller ID
            Optional<Store> storeOpt = storeRepository.findBySellerId(sellerId);
            if (storeOpt.isEmpty()) {
                log.warn("Store not found for sellerId: {}", sellerId);
                return;
            }
            
            Store store = storeOpt.get();
            
            // Check if order already exists
            Optional<TrendyolOrder> existingOrder = orderRepository.findByStoreIdAndPackageNo(store.getId(), payload.getId());
            
            if (existingOrder.isPresent()) {
                // Update existing order
                TrendyolOrder order = existingOrder.get();
                updateOrderFromWebhook(order, payload, store);
                orderRepository.save(order);
                log.info("Updated existing order: {} with new status: {}", payload.getOrderNumber(), payload.getStatus());
            } else {
                // Create new order
                TrendyolOrder newOrder = createOrderFromWebhook(payload, store);
                orderRepository.save(newOrder);
                log.info("Created new order: {} with status: {}", payload.getOrderNumber(), payload.getStatus());
            }
            
        } catch (Exception e) {
            log.error("Error processing webhook order {}: {}", payload.getOrderNumber(), e.getMessage(), e);
            // Don't rethrow - we don't want to return error to Trendyol for processing issues
        }
    }
    
    /**
     * Update existing order with webhook data
     */
    private void updateOrderFromWebhook(TrendyolOrder existingOrder, TrendyolWebhookPayload payload, Store store) {
        // Update status and shipment package status
        existingOrder.setStatus(payload.getStatus());
        existingOrder.setShipmentPackageStatus(payload.getShipmentPackageStatus());
        
        // Update last modified date if provided
        if (payload.getLastModifiedDate() != null) {
            LocalDateTime lastModified = Instant.ofEpochMilli(payload.getLastModifiedDate())
                    .atZone(ZoneId.of("Europe/Istanbul"))
                    .toLocalDateTime();
            existingOrder.setUpdatedAt(lastModified);
        }
        
        // Update cargo deci if provided
        if (payload.getCargoDeci() != null) {
            existingOrder.setCargoDeci(payload.getCargoDeci().intValue());
        }
        
        log.debug("Updated order {} from {} to {}", payload.getOrderNumber(), 
                 existingOrder.getStatus(), payload.getStatus());
    }
    
    /**
     * Create new order from webhook data
     */
    private TrendyolOrder createOrderFromWebhook(TrendyolWebhookPayload payload, Store store) {
        // Convert order date
        LocalDateTime orderDate = Instant.ofEpochMilli(payload.getOrderDate())
                .atZone(ZoneId.of("Europe/Istanbul"))
                .toLocalDateTime();
        
        // Convert order lines to order items
        List<OrderItem> orderItems = payload.getLines().stream()
                .map(line -> convertWebhookLineToOrderItem(line, store.getId(), orderDate))
                .collect(Collectors.toList());
        
        // Use total price from webhook payload (Trendyol provides this)
        java.math.BigDecimal totalPrice = payload.getTotalPrice() != null ? 
                                          payload.getTotalPrice() : java.math.BigDecimal.ZERO;
        
        // Calculate stoppage (withholding tax) as totalPrice * stoppage rate
        java.math.BigDecimal stoppage = totalPrice.multiply(FinancialConstants.STOPPAGE_RATE_DECIMAL);
        
        return TrendyolOrder.builder()
                .store(store)
                .tyOrderNumber(payload.getOrderNumber())
                .packageNo(payload.getId())
                .orderDate(orderDate)
                .grossAmount(payload.getGrossAmount())
                .totalDiscount(payload.getTotalDiscount())
                .totalTyDiscount(payload.getTotalTyDiscount())
                .totalPrice(totalPrice)
                .stoppage(stoppage)
                .orderItems(orderItems)
                .shipmentPackageStatus(payload.getShipmentPackageStatus())
                .status(payload.getStatus())
                .cargoDeci(payload.getCargoDeci() != null ? payload.getCargoDeci().intValue() : 0)
                .build();
    }
    
    /**
     * Convert webhook order line to OrderItem
     */
    private OrderItem convertWebhookLineToOrderItem(TrendyolWebhookPayload.OrderLine line, UUID storeId, LocalDateTime orderDate) {
        OrderItem.OrderItemBuilder itemBuilder = OrderItem.builder()
                .barcode(line.getBarcode())
                .productName(line.getProductName())
                .quantity(line.getQuantity())
                .unitPriceOrder(line.getAmount())
                .unitPriceDiscount(line.getDiscount())
                .unitPriceTyDiscount(line.getTyDiscount())
                .vatBaseAmount(line.getVatBaseAmount())
                .price(line.getPrice());
        
        // Use the cost calculator to set cost information
        costCalculator.setCostInfo(itemBuilder, line.getBarcode(), storeId, orderDate);
        
        return itemBuilder.build();
    }
}
