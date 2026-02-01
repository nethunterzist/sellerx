package com.ecommerce.sellerx.common;

import com.ecommerce.sellerx.expenses.StoreExpense;
import com.ecommerce.sellerx.orders.OrderItem;
import com.ecommerce.sellerx.orders.TrendyolOrder;
import com.ecommerce.sellerx.products.CostAndStockInfo;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.SyncStatus;
import com.ecommerce.sellerx.stores.TrendyolCredentials;
import com.ecommerce.sellerx.stores.WebhookStatus;
import com.ecommerce.sellerx.users.Role;
import com.ecommerce.sellerx.users.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Factory class for creating test entities with sensible defaults.
 * All methods return builders to allow customization.
 *
 * Usage examples:
 * <pre>
 * // Create a simple user
 * User user = TestDataBuilder.user().build();
 *
 * // Create a user with custom email
 * User user = TestDataBuilder.user()
 *     .email("custom@example.com")
 *     .role(Role.ADMIN)
 *     .build();
 *
 * // Create a store for a user
 * Store store = TestDataBuilder.store(user).build();
 *
 * // Create a product with cost info
 * TrendyolProduct product = TestDataBuilder.product(store)
 *     .salePrice(new BigDecimal("199.99"))
 *     .costAndStockInfo(List.of(
 *         TestDataBuilder.costAndStockInfo(100.0, 10).build()
 *     ))
 *     .build();
 * </pre>
 */
public final class TestDataBuilder {

    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);

    private TestDataBuilder() {
        // Utility class
    }

    /**
     * Generates a unique sequence number for test data.
     */
    public static int nextSequence() {
        return SEQUENCE.incrementAndGet();
    }

    /**
     * Resets the sequence counter (call in @BeforeEach if needed).
     */
    public static void resetSequence() {
        SEQUENCE.set(0);
    }

    // ========== USER ==========

    /**
     * Creates a User builder with sensible defaults.
     * Email: test{sequence}@example.com
     * Password: BCrypt hash of "password123"
     */
    public static User.UserBuilder user() {
        int seq = nextSequence();
        return User.builder()
                .name("Test User " + seq)
                .email("test" + seq + "@example.com")
                .password("$2a$10$pbhA2lvr7KXc4gxwcrYbIu6rlsyi5IpASgzxG6Wcco0/VSGwR1g.K") // "password123"
                .role(Role.USER);
    }

    /**
     * Creates an admin user builder.
     */
    public static User.UserBuilder adminUser() {
        return user().role(Role.ADMIN);
    }

    // ========== STORE ==========

    /**
     * Creates a Store builder with sensible defaults.
     * Uses Trendyol marketplace with test credentials.
     */
    public static Store.StoreBuilder store(User owner) {
        int seq = nextSequence();
        return Store.builder()
                .user(owner)
                .storeName("Test Store " + seq)
                .marketplace("trendyol")
                .credentials(testTrendyolCredentials(seq))
                .syncStatus(SyncStatus.PENDING)
                .webhookStatus(WebhookStatus.PENDING)
                .initialSyncCompleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now());
    }

    /**
     * Creates a Store builder with completed sync status.
     */
    public static Store.StoreBuilder completedStore(User owner) {
        return store(owner)
                .syncStatus(SyncStatus.COMPLETED)
                .initialSyncCompleted(true);
    }

    /**
     * Creates test Trendyol credentials.
     */
    public static TrendyolCredentials testTrendyolCredentials(int sequence) {
        return TrendyolCredentials.builder()
                .apiKey("test-api-key-" + sequence)
                .apiSecret("test-api-secret-" + sequence)
                .sellerId((long) (100000 + sequence))
                .integrationCode("test-integration-" + sequence)
                .Token("test-token-" + sequence)
                .build();
    }

    // ========== PRODUCT ==========

    /**
     * Creates a TrendyolProduct builder with sensible defaults.
     */
    public static TrendyolProduct.TrendyolProductBuilder product(Store store) {
        int seq = nextSequence();
        return TrendyolProduct.builder()
                .store(store)
                .productId("PRODUCT-" + seq)
                .barcode("BARCODE-" + seq)
                .title("Test Product " + seq)
                .categoryName("Test Category")
                .brand("Test Brand")
                .brandId((long) (1000 + seq))
                .salePrice(new BigDecimal("199.99"))
                .vatRate(20)
                .commissionRate(new BigDecimal("15.00"))
                .trendyolQuantity(100)
                .approved(true)
                .onSale(true)
                .archived(false)
                .rejected(false)
                .blacklisted(false)
                .hasActiveCampaign(false)
                .costAndStockInfo(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now());
    }

    /**
     * Creates a product with cost and stock information.
     */
    public static TrendyolProduct.TrendyolProductBuilder productWithCost(Store store, double unitCost, int quantity) {
        List<CostAndStockInfo> costInfo = new ArrayList<>();
        costInfo.add(costAndStockInfo(unitCost, quantity).build());
        return product(store).costAndStockInfo(costInfo);
    }

    // ========== COST AND STOCK INFO ==========

    /**
     * Creates a CostAndStockInfo builder with defaults.
     */
    public static CostAndStockInfo.CostAndStockInfoBuilder costAndStockInfo(double unitCost, int quantity) {
        return CostAndStockInfo.builder()
                .unitCost(unitCost)
                .quantity(quantity)
                .costVatRate(20)
                .stockDate(LocalDate.now())
                .usedQuantity(0);
    }

    /**
     * Creates multiple stock lots for FIFO testing.
     * Each lot has different unit costs and dates.
     */
    public static List<CostAndStockInfo> createFifoStockLots(double... unitCostsAndQuantities) {
        if (unitCostsAndQuantities.length % 2 != 0) {
            throw new IllegalArgumentException("Arguments must be pairs of (unitCost, quantity)");
        }

        List<CostAndStockInfo> lots = new ArrayList<>();
        LocalDate baseDate = LocalDate.now().minusDays(unitCostsAndQuantities.length / 2);

        for (int i = 0; i < unitCostsAndQuantities.length; i += 2) {
            double unitCost = unitCostsAndQuantities[i];
            int quantity = (int) unitCostsAndQuantities[i + 1];

            lots.add(CostAndStockInfo.builder()
                    .unitCost(unitCost)
                    .quantity(quantity)
                    .costVatRate(20)
                    .stockDate(baseDate.plusDays(i / 2))
                    .usedQuantity(0)
                    .build());
        }
        return lots;
    }

    // ========== ORDER ==========

    /**
     * Creates a TrendyolOrder builder with sensible defaults.
     */
    public static TrendyolOrder.TrendyolOrderBuilder order(Store store) {
        int seq = nextSequence();
        return TrendyolOrder.builder()
                .store(store)
                .tyOrderNumber("TY-ORDER-" + seq)
                .packageNo((long) (1000000 + seq))
                .orderDate(LocalDateTime.now())
                .grossAmount(new BigDecimal("299.99"))
                .totalDiscount(BigDecimal.ZERO)
                .totalTyDiscount(BigDecimal.ZERO)
                .totalPrice(new BigDecimal("299.99"))
                .shipmentPackageStatus("Delivered")
                .status("Delivered")
                .stoppage(BigDecimal.ZERO)
                .estimatedCommission(new BigDecimal("45.00"))
                .isCommissionEstimated(true)
                .cargoDeci(1)
                .transactionStatus("NOT_SETTLED")
                .dataSource("ORDER_API")
                .orderItems(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now());
    }

    /**
     * Creates an order with items.
     */
    public static TrendyolOrder.TrendyolOrderBuilder orderWithItems(Store store, List<OrderItem> items) {
        BigDecimal totalPrice = items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return order(store)
                .orderItems(items)
                .grossAmount(totalPrice)
                .totalPrice(totalPrice);
    }

    // ========== ORDER ITEM ==========

    /**
     * Creates an OrderItem builder with sensible defaults.
     */
    public static OrderItem.OrderItemBuilder orderItem() {
        int seq = nextSequence();
        return OrderItem.builder()
                .barcode("BARCODE-" + seq)
                .productName("Test Product " + seq)
                .quantity(1)
                .unitPriceOrder(new BigDecimal("199.99"))
                .unitPriceDiscount(BigDecimal.ZERO)
                .unitPriceTyDiscount(BigDecimal.ZERO)
                .vatBaseAmount(new BigDecimal("166.66"))
                .saleVatRate(20)
                .saleVatAmount(new BigDecimal("33.33"))
                .price(new BigDecimal("199.99"))
                .cost(new BigDecimal("100.00"))
                .costVat(20)
                .stockDate(LocalDate.now())
                .estimatedCommissionRate(new BigDecimal("15.00"))
                .unitEstimatedCommission(new BigDecimal("30.00"));
    }

    /**
     * Creates an order item for a specific product.
     */
    public static OrderItem.OrderItemBuilder orderItem(TrendyolProduct product, int quantity) {
        BigDecimal price = product.getSalePrice();
        BigDecimal vatAmount = price.multiply(BigDecimal.valueOf(product.getVatRate()))
                .divide(BigDecimal.valueOf(100 + product.getVatRate()), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal vatBase = price.subtract(vatAmount);

        return OrderItem.builder()
                .barcode(product.getBarcode())
                .productName(product.getTitle())
                .quantity(quantity)
                .unitPriceOrder(price)
                .unitPriceDiscount(BigDecimal.ZERO)
                .unitPriceTyDiscount(BigDecimal.ZERO)
                .vatBaseAmount(vatBase)
                .saleVatRate(product.getVatRate())
                .saleVatAmount(vatAmount)
                .price(price)
                .costVat(20)
                .stockDate(LocalDate.now())
                .estimatedCommissionRate(product.getCommissionRate())
                .unitEstimatedCommission(
                        vatBase.multiply(product.getCommissionRate())
                                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP)
                );
    }

    // ========== STORE EXPENSE ==========

    /**
     * Creates a StoreExpense builder with sensible defaults.
     * Note: expenseCategory and store must be set by the caller.
     */
    public static StoreExpense.StoreExpenseBuilder storeExpense(com.ecommerce.sellerx.stores.Store store,
                                                                 com.ecommerce.sellerx.expenses.ExpenseCategory category) {
        int seq = nextSequence();
        return StoreExpense.builder()
                .store(store)
                .expenseCategory(category)
                .name("Test Expense " + seq)
                .amount(new BigDecimal("150.00"))
                .frequency(com.ecommerce.sellerx.expenses.ExpenseFrequency.MONTHLY)
                .date(LocalDateTime.now())
                .vatRate(20)
                .vatAmount(new BigDecimal("30.00"))
                .netAmount(new BigDecimal("150.00"))
                .isVatDeductible(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now());
    }

    /**
     * Creates a StoreExpense builder with no VAT.
     */
    public static StoreExpense.StoreExpenseBuilder storeExpenseNoVat(com.ecommerce.sellerx.stores.Store store,
                                                                      com.ecommerce.sellerx.expenses.ExpenseCategory category) {
        return storeExpense(store, category)
                .vatRate(null)
                .vatAmount(BigDecimal.ZERO)
                .netAmount(new BigDecimal("150.00"));
    }

    // ========== EXPENSE CATEGORY ==========

    /**
     * Creates an ExpenseCategory builder with sensible defaults.
     */
    public static com.ecommerce.sellerx.expenses.ExpenseCategory.ExpenseCategoryBuilder expenseCategory() {
        int seq = nextSequence();
        return com.ecommerce.sellerx.expenses.ExpenseCategory.builder()
                .name("Test Category " + seq)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now());
    }

    // ========== WEBHOOK EVENT ==========

    /**
     * Creates a WebhookEvent builder with sensible defaults.
     */
    public static com.ecommerce.sellerx.webhook.WebhookEvent.WebhookEventBuilder webhookEvent(
            java.util.UUID storeId, String sellerId) {
        int seq = nextSequence();
        return com.ecommerce.sellerx.webhook.WebhookEvent.builder()
                .eventId("evt-" + UUID.randomUUID().toString())
                .storeId(storeId)
                .sellerId(sellerId)
                .eventType("order.status.changed")
                .orderNumber("TY-WH-ORDER-" + seq)
                .status("Delivered")
                .payload("{\"orderNumber\":\"TY-WH-ORDER-" + seq + "\"}")
                .processingStatus(com.ecommerce.sellerx.webhook.WebhookEvent.ProcessingStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .processingTimeMs(50L);
    }

    /**
     * Creates a duplicate webhook event for idempotency testing.
     */
    public static com.ecommerce.sellerx.webhook.WebhookEvent.WebhookEventBuilder duplicateWebhookEvent(
            java.util.UUID storeId, String sellerId, String existingEventId) {
        return webhookEvent(storeId, sellerId)
                .eventId(existingEventId)
                .processingStatus(com.ecommerce.sellerx.webhook.WebhookEvent.ProcessingStatus.DUPLICATE);
    }

    // ========== UTILITY METHODS ==========

    /**
     * Creates a random UUID string.
     */
    public static String randomUuidString() {
        return UUID.randomUUID().toString();
    }

    /**
     * Creates a BigDecimal from double with proper scale.
     */
    public static BigDecimal money(double value) {
        return BigDecimal.valueOf(value).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Creates a date in the past relative to today.
     */
    public static LocalDate daysAgo(int days) {
        return LocalDate.now().minusDays(days);
    }

    /**
     * Creates a datetime in the past relative to now.
     */
    public static LocalDateTime hoursAgo(int hours) {
        return LocalDateTime.now().minusHours(hours);
    }
}
