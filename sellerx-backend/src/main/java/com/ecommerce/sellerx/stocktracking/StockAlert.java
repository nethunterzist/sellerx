package com.ecommerce.sellerx.stocktracking;

import com.ecommerce.sellerx.stores.Store;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an alert triggered by a stock change.
 * Similar to BuyboxAlert but for stock-related events.
 */
@Entity
@Table(name = "stock_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tracked_product_id", nullable = false)
    private StockTrackedProduct trackedProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    // Alert details
    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 50)
    private StockAlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private StockAlertSeverity severity;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    // Stock data at alert time
    @Column(name = "old_quantity")
    private Integer oldQuantity;

    @Column(name = "new_quantity")
    private Integer newQuantity;

    @Column(name = "threshold")
    private Integer threshold;

    // Notification status
    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "email_sent")
    @Builder.Default
    private Boolean emailSent = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Factory methods for creating alerts
    public static StockAlert outOfStock(StockTrackedProduct product, int oldQty) {
        return StockAlert.builder()
                .trackedProduct(product)
                .store(product.getStore())
                .alertType(StockAlertType.OUT_OF_STOCK)
                .severity(StockAlertSeverity.CRITICAL)
                .title("Rakip Stok Tükendi: " + product.getProductName())
                .message(String.format("%s ürününün stoğu tükendi! Önceki stok: %d adet",
                        product.getProductName(), oldQty))
                .oldQuantity(oldQty)
                .newQuantity(0)
                .build();
    }

    public static StockAlert lowStock(StockTrackedProduct product, int oldQty, int newQty) {
        return StockAlert.builder()
                .trackedProduct(product)
                .store(product.getStore())
                .alertType(StockAlertType.LOW_STOCK)
                .severity(StockAlertSeverity.HIGH)
                .title("Rakip Düşük Stok: " + product.getProductName())
                .message(String.format("%s ürününün stoğu %d adede düştü (eşik: %d)",
                        product.getProductName(), newQty, product.getLowStockThreshold()))
                .oldQuantity(oldQty)
                .newQuantity(newQty)
                .threshold(product.getLowStockThreshold())
                .build();
    }

    public static StockAlert backInStock(StockTrackedProduct product, int newQty) {
        return StockAlert.builder()
                .trackedProduct(product)
                .store(product.getStore())
                .alertType(StockAlertType.BACK_IN_STOCK)
                .severity(StockAlertSeverity.MEDIUM)
                .title("Rakip Stoğa Geri Döndü: " + product.getProductName())
                .message(String.format("%s ürünü tekrar stoğa girdi! Yeni stok: %d adet",
                        product.getProductName(), newQty))
                .oldQuantity(0)
                .newQuantity(newQty)
                .build();
    }

    public static StockAlert stockIncreased(StockTrackedProduct product, int oldQty, int newQty) {
        int increase = newQty - oldQty;
        int percentIncrease = oldQty > 0 ? (increase * 100 / oldQty) : 100;

        return StockAlert.builder()
                .trackedProduct(product)
                .store(product.getStore())
                .alertType(StockAlertType.STOCK_INCREASED)
                .severity(StockAlertSeverity.LOW)
                .title("Rakip Stok Artışı: " + product.getProductName())
                .message(String.format("%s ürününde stok artışı: %d → %d (+%d%%, +%d adet)",
                        product.getProductName(), oldQty, newQty, percentIncrease, increase))
                .oldQuantity(oldQty)
                .newQuantity(newQty)
                .build();
    }

    // Mark as read
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
}
