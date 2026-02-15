package com.ecommerce.sellerx.orders.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Event published when stock/cost information is changed (add, update, delete).
 * Used to trigger async stock-order synchronization after transaction commits.
 */
@Getter
public class StockChangedEvent extends ApplicationEvent {

    private final UUID storeId;
    private final UUID productId;
    private final LocalDate stockDate;

    public StockChangedEvent(Object source, UUID storeId, UUID productId, LocalDate stockDate) {
        super(source);
        this.storeId = storeId;
        this.productId = productId;
        this.stockDate = stockDate;
    }
}
