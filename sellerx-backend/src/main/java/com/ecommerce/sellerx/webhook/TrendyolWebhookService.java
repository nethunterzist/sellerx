package com.ecommerce.sellerx.webhook;

import com.ecommerce.sellerx.config.FinancialConstants;
import com.ecommerce.sellerx.orders.OrderCostCalculator;
import com.ecommerce.sellerx.orders.OrderItem;
import com.ecommerce.sellerx.orders.TrendyolOrder;
import com.ecommerce.sellerx.orders.TrendyolOrderRepository;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TrendyolWebhookService {

    private final TrendyolOrderRepository orderRepository;
    private final StoreRepository storeRepository;
    private final OrderCostCalculator costCalculator;
    private final MeterRegistry meterRegistry;

    public TrendyolWebhookService(
            TrendyolOrderRepository orderRepository,
            StoreRepository storeRepository,
            OrderCostCalculator costCalculator,
            MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.storeRepository = storeRepository;
        this.costCalculator = costCalculator;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Process incoming webhook order data
     */
    public void processWebhookOrder(TrendyolWebhookPayload payload, String sellerId) {
        try {
            log.info("Processing webhook order: {} for seller: {}", payload.getOrderNumber(), sellerId);
            incrementWebhookCounter("order_received");

            // Find the store by seller ID
            Optional<Store> storeOpt = storeRepository.findBySellerId(sellerId);
            if (storeOpt.isEmpty()) {
                log.warn("Store not found for sellerId: {}", sellerId);
                incrementWebhookCounter("store_not_found");
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
                incrementWebhookCounter("order_updated");
            } else {
                // Create new order
                TrendyolOrder newOrder = createOrderFromWebhook(payload, store);
                orderRepository.save(newOrder);
                log.info("Created new order: {} with status: {}", payload.getOrderNumber(), payload.getStatus());
                incrementWebhookCounter("order_created");
            }

        } catch (Exception e) {
            log.error("Error processing webhook order {}: {}", payload.getOrderNumber(), e.getMessage(), e);
            incrementWebhookCounter("processing_error");
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

        // Update shipment city information if not already set
        if (existingOrder.getShipmentCity() == null && payload.getShipmentAddress() != null) {
            TrendyolWebhookPayload.Address address = payload.getShipmentAddress();
            existingOrder.setShipmentCity(address.getCity());
            existingOrder.setShipmentCityCode(address.getCityCode());
            existingOrder.setShipmentDistrict(address.getDistrict());
            existingOrder.setShipmentDistrictId(address.getDistrictId());
        }

        // Update customer info if not already set
        if (existingOrder.getCustomerEmail() == null && payload.getCustomerEmail() != null) {
            existingOrder.setCustomerFirstName(payload.getCustomerFirstName());
            existingOrder.setCustomerLastName(payload.getCustomerLastName());
            existingOrder.setCustomerEmail(payload.getCustomerEmail());
            existingOrder.setCustomerId(payload.getCustomerId());
        }

        // Extract delivery date from packageHistories when status is Delivered
        if ("Delivered".equals(payload.getStatus()) && existingOrder.getDeliveryDate() == null) {
            extractAndSetDeliveryDate(existingOrder, payload);
        }

        // Update cancellation info if provided
        if (payload.getCancelledBy() != null && existingOrder.getCancelledBy() == null) {
            existingOrder.setCancelledBy(payload.getCancelledBy());
            existingOrder.setCancelReason(payload.getCancelReason());
            existingOrder.setCancelReasonCode(payload.getCancelReasonCode());
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

        // Extract shipment address city information
        String shipmentCity = null;
        Integer shipmentCityCode = null;
        String shipmentDistrict = null;
        Integer shipmentDistrictId = null;
        if (payload.getShipmentAddress() != null) {
            TrendyolWebhookPayload.Address address = payload.getShipmentAddress();
            shipmentCity = address.getCity();
            shipmentCityCode = address.getCityCode();
            shipmentDistrict = address.getDistrict();
            shipmentDistrictId = address.getDistrictId();
        }

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
                .shipmentCity(shipmentCity)
                .shipmentCityCode(shipmentCityCode)
                .shipmentDistrict(shipmentDistrict)
                .shipmentDistrictId(shipmentDistrictId)
                .customerFirstName(payload.getCustomerFirstName())
                .customerLastName(payload.getCustomerLastName())
                .customerEmail(payload.getCustomerEmail())
                .customerId(payload.getCustomerId())
                .cancelledBy(payload.getCancelledBy())
                .cancelReason(payload.getCancelReason())
                .cancelReasonCode(payload.getCancelReasonCode())
                .dataSource("ORDER_API") // Mark as webhook/Orders API source (has operational data)
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

    /**
     * Extract delivery date from webhook packageHistories and calculate hakediş date.
     * Looks for the "Delivered" status entry in packageHistories to get the actual delivery timestamp.
     * Falls back to lastModifiedDate if packageHistories doesn't contain delivery info.
     */
    private void extractAndSetDeliveryDate(TrendyolOrder order, TrendyolWebhookPayload payload) {
        LocalDateTime deliveryDate = null;

        // Try to extract from packageHistories
        if (payload.getPackageHistories() != null) {
            for (TrendyolWebhookPayload.PackageHistory history : payload.getPackageHistories()) {
                if ("Delivered".equals(history.getStatus()) && history.getCreatedDate() != null) {
                    deliveryDate = Instant.ofEpochMilli(history.getCreatedDate())
                            .atZone(ZoneId.of("Europe/Istanbul"))
                            .toLocalDateTime();
                    break;
                }
            }
        }

        // Fallback to lastModifiedDate
        if (deliveryDate == null && payload.getLastModifiedDate() != null) {
            deliveryDate = Instant.ofEpochMilli(payload.getLastModifiedDate())
                    .atZone(ZoneId.of("Europe/Istanbul"))
                    .toLocalDateTime();
        }

        if (deliveryDate != null) {
            order.setDeliveryDate(deliveryDate);
            log.debug("Set delivery date {} for order {}", deliveryDate, order.getTyOrderNumber());
        }
    }

    private void incrementWebhookCounter(String eventType) {
        Counter.builder("sellerx.webhook.events")
                .tag("type", eventType)
                .description("Number of webhook events processed")
                .register(meterRegistry)
                .increment();
    }
}
