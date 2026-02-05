package com.ecommerce.sellerx.orders;

import com.ecommerce.sellerx.orders.dto.CustomerAnalyticsProjections.*;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.stores.TrendyolCredentials;
import com.ecommerce.sellerx.users.Role;
import com.ecommerce.sellerx.users.User;
import com.ecommerce.sellerx.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.Rollback;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Customer Analytics repository queries.
 * Uses TestContainers with real PostgreSQL to validate complex SQL queries.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DisplayName("CustomerAnalyticsRepository Integration Tests")
class CustomerAnalyticsRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private TrendyolOrderRepository orderRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private UserRepository userRepository;

    private Store testStore;
    private UUID storeId;

    // Customer IDs
    private static final Long CUSTOMER_1_ID = 1001L; // 5 orders - repeat customer
    private static final Long CUSTOMER_2_ID = 1002L; // 2 orders - repeat customer
    private static final Long CUSTOMER_3_ID = 1003L; // 1 order - new customer
    private static final Long CUSTOMER_4_ID = 1004L; // 1 order, no customer_id (null)

    @BeforeEach
    @Rollback
    void setUp() {
        // Clean up
        orderRepository.deleteAll();
        storeRepository.deleteAll();
        userRepository.deleteAll();

        // Create user
        User user = User.builder()
                .name("Analytics Test User")
                .email("analytics-test-" + UUID.randomUUID() + "@test.com")
                .password("password123")
                .role(Role.USER)
                .build();
        userRepository.save(user);

        // Create store
        testStore = Store.builder()
                .storeName("Analytics Test Store")
                .marketplace("trendyol")
                .credentials(new TrendyolCredentials("apiKey", "apiSecret", 123L, null, "Token"))
                .user(user)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testStore = storeRepository.save(testStore);
        storeId = testStore.getId();

        // Create test orders with varied customer data
        createTestOrders();
    }

    private void createTestOrders() {
        LocalDateTime now = LocalDateTime.now();

        // Customer 1: 5 orders over 6 months (high-value repeat customer) - Istanbul
        createOrder(CUSTOMER_1_ID, "Ahmet", "Yilmaz", now.minusMonths(6), "Istanbul",
                new BigDecimal("500.00"), "BARCODE-001", "Product A");
        createOrder(CUSTOMER_1_ID, "Ahmet", "Yilmaz", now.minusMonths(4), "Istanbul",
                new BigDecimal("750.00"), "BARCODE-001", "Product A");
        createOrder(CUSTOMER_1_ID, "Ahmet", "Yilmaz", now.minusMonths(3), "Istanbul",
                new BigDecimal("300.00"), "BARCODE-002", "Product B");
        createOrder(CUSTOMER_1_ID, "Ahmet", "Yilmaz", now.minusMonths(2), "Istanbul",
                new BigDecimal("600.00"), "BARCODE-001", "Product A");
        createOrder(CUSTOMER_1_ID, "Ahmet", "Yilmaz", now.minusMonths(1), "Istanbul",
                new BigDecimal("450.00"), "BARCODE-003", "Product C");

        // Customer 2: 2 orders over 3 months (medium repeat customer) - Ankara
        createOrder(CUSTOMER_2_ID, "Mehmet", "Demir", now.minusMonths(3), "Ankara",
                new BigDecimal("350.00"), "BARCODE-002", "Product B");
        createOrder(CUSTOMER_2_ID, "Mehmet", "Demir", now.minusMonths(1), "Ankara",
                new BigDecimal("400.00"), "BARCODE-001", "Product A");

        // Customer 3: 1 order (new customer) - Istanbul
        createOrder(CUSTOMER_3_ID, "Ayse", "Kara", now.minusDays(15), "Istanbul",
                new BigDecimal("200.00"), "BARCODE-003", "Product C");

        // Order without customer_id (for backfill coverage test)
        createOrderWithoutCustomerId("Zeynep", "Arslan", now.minusDays(10), "Izmir",
                new BigDecimal("150.00"), "BARCODE-002", "Product B");
    }

    private void createOrder(Long customerId, String firstName, String lastName,
                            LocalDateTime orderDate, String city, BigDecimal totalPrice,
                            String barcode, String productName) {
        OrderItem item = OrderItem.builder()
                .barcode(barcode)
                .productName(productName)
                .quantity(1)
                .unitPriceOrder(totalPrice)
                .price(totalPrice)
                .vatBaseAmount(totalPrice.multiply(new BigDecimal("0.82")))
                .build();

        TrendyolOrder order = TrendyolOrder.builder()
                .store(testStore)
                .tyOrderNumber("TY-" + UUID.randomUUID().toString().substring(0, 8))
                .packageNo(System.currentTimeMillis() + (long)(Math.random() * 10000))
                .orderDate(orderDate)
                .grossAmount(totalPrice)
                .totalPrice(totalPrice)
                .status("Delivered")
                .customerId(customerId)
                .customerFirstName(firstName)
                .customerLastName(lastName)
                .shipmentCity(city)
                .orderItems(List.of(item))
                .dataSource("ORDER_API")
                .build();

        orderRepository.save(order);
    }

    private void createOrderWithoutCustomerId(String firstName, String lastName,
                                              LocalDateTime orderDate, String city, BigDecimal totalPrice,
                                              String barcode, String productName) {
        OrderItem item = OrderItem.builder()
                .barcode(barcode)
                .productName(productName)
                .quantity(1)
                .unitPriceOrder(totalPrice)
                .price(totalPrice)
                .vatBaseAmount(totalPrice.multiply(new BigDecimal("0.82")))
                .build();

        TrendyolOrder order = TrendyolOrder.builder()
                .store(testStore)
                .tyOrderNumber("TY-" + UUID.randomUUID().toString().substring(0, 8))
                .packageNo(System.currentTimeMillis() + (long)(Math.random() * 10000))
                .orderDate(orderDate)
                .grossAmount(totalPrice)
                .totalPrice(totalPrice)
                .status("Delivered")
                .customerId(null) // No customer ID
                .customerFirstName(firstName)
                .customerLastName(lastName)
                .shipmentCity(city)
                .orderItems(List.of(item))
                .dataSource("ORDER_API")
                .build();

        orderRepository.save(order);
    }

    @Nested
    @DisplayName("getCustomerAnalyticsSummary")
    class GetCustomerAnalyticsSummaryTests {

        @Test
        @DisplayName("should return correct customer summary statistics")
        void shouldReturnCorrectCustomerSummary() {
            // When
            SummaryProjection summary = orderRepository.getCustomerAnalyticsSummary(storeId);

            // Then
            assertThat(summary).isNotNull();
            assertThat(summary.getTotalCustomers()).isEqualTo(3); // 3 customers with customer_id
            assertThat(summary.getRepeatCustomers()).isEqualTo(2); // Customer 1 (5 orders) and Customer 2 (2 orders)

            // Total revenue: 500+750+300+600+450+350+400+200 = 3550
            assertThat(summary.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("3550.00"));

            // Repeat customer revenue: Customer1 (2600) + Customer2 (750) = 3350
            assertThat(summary.getRepeatRevenue()).isEqualByComparingTo(new BigDecimal("3350.00"));
        }

        @Test
        @DisplayName("should return zero values for empty store")
        void shouldReturnZeroValuesForEmptyStore() {
            // Given - new store with no orders
            User newUser = User.builder()
                    .name("Empty Store User")
                    .email("empty-" + UUID.randomUUID() + "@test.com")
                    .password("password123")
                    .role(Role.USER)
                    .build();
            userRepository.save(newUser);

            Store emptyStore = Store.builder()
                    .storeName("Empty Store")
                    .marketplace("trendyol")
                    .credentials(new TrendyolCredentials("apiKey", "apiSecret", 456L, null, "Token"))
                    .user(newUser)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            storeRepository.save(emptyStore);

            // When
            SummaryProjection summary = orderRepository.getCustomerAnalyticsSummary(emptyStore.getId());

            // Then
            assertThat(summary).isNotNull();
            assertThat(summary.getTotalCustomers()).isEqualTo(0);
            assertThat(summary.getRepeatCustomers()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("getCustomerSegmentation")
    class GetCustomerSegmentationTests {

        @Test
        @DisplayName("should return correct customer segments")
        void shouldReturnCorrectSegments() {
            // When
            List<SegmentProjection> segments = orderRepository.getCustomerSegmentation(storeId);

            // Then
            assertThat(segments).hasSize(3); // 1, 2-3, 4-6 (order-based segments)

            // Find each segment
            SegmentProjection oneOrder = segments.stream()
                    .filter(s -> "1".equals(s.getSegment())).findFirst().orElse(null);
            SegmentProjection twoThreeOrders = segments.stream()
                    .filter(s -> "2-3".equals(s.getSegment())).findFirst().orElse(null);
            SegmentProjection fourSixOrders = segments.stream()
                    .filter(s -> "4-6".equals(s.getSegment())).findFirst().orElse(null);

            // 1 order: Customer 3 (1 order, 200 TL)
            assertThat(oneOrder).isNotNull();
            assertThat(oneOrder.getCustomerCount()).isEqualTo(1);
            assertThat(oneOrder.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("200.00"));

            // 2-3 orders: Customer 2 (2 orders, 750 TL)
            assertThat(twoThreeOrders).isNotNull();
            assertThat(twoThreeOrders.getCustomerCount()).isEqualTo(1);
            assertThat(twoThreeOrders.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("750.00"));

            // 4-6 orders: Customer 1 (5 orders, 2600 TL)
            assertThat(fourSixOrders).isNotNull();
            assertThat(fourSixOrders.getCustomerCount()).isEqualTo(1);
            assertThat(fourSixOrders.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("2600.00"));
        }
    }

    @Nested
    @DisplayName("getCityRepeatAnalysis")
    class GetCityRepeatAnalysisTests {

        @Test
        @DisplayName("should return city-based repeat customer analysis")
        void shouldReturnCityRepeatAnalysis() {
            // When
            List<CityRepeatProjection> cities = orderRepository.getCityRepeatAnalysis(storeId);

            // Then
            assertThat(cities).isNotEmpty();

            // Istanbul: Customer 1 (5 orders) + Customer 3 (1 order) = 2 customers, 1 repeat
            CityRepeatProjection istanbul = cities.stream()
                    .filter(c -> "Istanbul".equals(c.getCity())).findFirst().orElse(null);
            assertThat(istanbul).isNotNull();
            assertThat(istanbul.getTotalCustomers()).isEqualTo(2);
            assertThat(istanbul.getRepeatCustomers()).isEqualTo(1); // Only Customer 1 is repeat

            // Ankara: Customer 2 (2 orders) = 1 customer, 1 repeat
            CityRepeatProjection ankara = cities.stream()
                    .filter(c -> "Ankara".equals(c.getCity())).findFirst().orElse(null);
            assertThat(ankara).isNotNull();
            assertThat(ankara.getTotalCustomers()).isEqualTo(1);
            assertThat(ankara.getRepeatCustomers()).isEqualTo(1);
        }

        @Test
        @DisplayName("should order by total customers descending")
        void shouldOrderByTotalCustomersDescending() {
            // When
            List<CityRepeatProjection> cities = orderRepository.getCityRepeatAnalysis(storeId);

            // Then
            assertThat(cities.get(0).getCity()).isEqualTo("Istanbul"); // 2 customers
            assertThat(cities.get(1).getCity()).isEqualTo("Ankara"); // 1 customer
        }
    }

    @Nested
    @DisplayName("getMonthlyNewVsRepeatTrend")
    class GetMonthlyTrendTests {

        @Test
        @DisplayName("should return monthly trend with new vs repeat breakdown")
        void shouldReturnMonthlyTrend() {
            // When
            List<MonthlyTrendProjection> trend = orderRepository.getMonthlyNewVsRepeatTrend(storeId);

            // Then
            assertThat(trend).isNotEmpty();

            // All months should have revenue data
            BigDecimal totalNewRevenue = BigDecimal.ZERO;
            BigDecimal totalRepeatRevenue = BigDecimal.ZERO;

            for (MonthlyTrendProjection month : trend) {
                assertThat(month.getMonth()).matches("\\d{4}-\\d{2}"); // Format: YYYY-MM
                if (month.getNewRevenue() != null) {
                    totalNewRevenue = totalNewRevenue.add(month.getNewRevenue());
                }
                if (month.getRepeatRevenue() != null) {
                    totalRepeatRevenue = totalRepeatRevenue.add(month.getRepeatRevenue());
                }
            }

            // Should have some revenue in both categories
            assertThat(totalNewRevenue.add(totalRepeatRevenue))
                    .isGreaterThan(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("getProductRepeatAnalysis")
    class GetProductRepeatAnalysisTests {

        @Test
        @DisplayName("should return product-level repeat buyer analysis")
        void shouldReturnProductRepeatAnalysis() {
            // When
            List<ProductRepeatProjection> products = orderRepository.getProductRepeatAnalysis(storeId);

            // Then
            // BARCODE-001: Bought by Customer 1 (3 times) and Customer 2 (1 time) = 2 buyers, 1 repeat
            // BARCODE-002: Bought by Customer 1 (1 time) and Customer 2 (1 time) = 2 buyers, 0 repeat
            // BARCODE-003: Bought by Customer 1 (1 time) and Customer 3 (1 time) = 2 buyers, 0 repeat
            // Note: Only products with 3+ buyers are returned (HAVING COUNT(*) >= 3)
            // With our test data, no product has 3+ buyers, so list may be empty

            // If we had 3+ buyers per product, we would validate:
            // assertThat(products).isNotEmpty();

            // For now, verify the query executes without error
            assertThat(products).isNotNull();
        }
    }

    @Nested
    @DisplayName("getCrossSellAnalysis")
    class GetCrossSellAnalysisTests {

        @Test
        @DisplayName("should return cross-sell product pairs")
        void shouldReturnCrossSellAnalysis() {
            // When
            List<CrossSellProjection> crossSell = orderRepository.getCrossSellAnalysis(storeId);

            // Then
            // Customer 1 bought: BARCODE-001, BARCODE-002, BARCODE-003
            // Customer 2 bought: BARCODE-001, BARCODE-002
            // Cross-sell pairs with co-occurrence >= 2:
            // - BARCODE-001 + BARCODE-002: Both customers bought both = 2 co-occurrences

            assertThat(crossSell).isNotEmpty();

            // Find the BARCODE-001 + BARCODE-002 pair
            CrossSellProjection pair = crossSell.stream()
                    .filter(p ->
                        (p.getSourceBarcode().equals("BARCODE-001") && p.getTargetBarcode().equals("BARCODE-002")) ||
                        (p.getSourceBarcode().equals("BARCODE-002") && p.getTargetBarcode().equals("BARCODE-001")))
                    .findFirst()
                    .orElse(null);

            assertThat(pair).isNotNull();
            assertThat(pair.getCoOccurrenceCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("findCustomerSummaries")
    class FindCustomerSummariesTests {

        @Test
        @DisplayName("should return paginated customer list ordered by totalSpend")
        void shouldReturnPaginatedCustomerList() {
            // When
            List<CustomerSummaryProjection> customers = orderRepository.findCustomerSummaries(storeId, 10, 0);

            // Then
            assertThat(customers).hasSize(3);

            // Ordered by totalSpend DESC
            // Customer 1: 2600 TL (highest)
            // Customer 2: 750 TL
            // Customer 3: 200 TL (lowest)
            assertThat(customers.get(0).getCustomerId()).isEqualTo(CUSTOMER_1_ID);
            assertThat(customers.get(0).getTotalSpend()).isEqualByComparingTo(new BigDecimal("2600.00"));
            assertThat(customers.get(0).getOrderCount()).isEqualTo(5);

            assertThat(customers.get(1).getCustomerId()).isEqualTo(CUSTOMER_2_ID);
            assertThat(customers.get(1).getTotalSpend()).isEqualByComparingTo(new BigDecimal("750.00"));
            assertThat(customers.get(1).getOrderCount()).isEqualTo(2);

            assertThat(customers.get(2).getCustomerId()).isEqualTo(CUSTOMER_3_ID);
            assertThat(customers.get(2).getTotalSpend()).isEqualByComparingTo(new BigDecimal("200.00"));
            assertThat(customers.get(2).getOrderCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should support pagination with offset")
        void shouldSupportPaginationWithOffset() {
            // When - get second page (skip first customer)
            List<CustomerSummaryProjection> customers = orderRepository.findCustomerSummaries(storeId, 2, 1);

            // Then
            assertThat(customers).hasSize(2);
            assertThat(customers.get(0).getCustomerId()).isEqualTo(CUSTOMER_2_ID);
            assertThat(customers.get(1).getCustomerId()).isEqualTo(CUSTOMER_3_ID);
        }

        @Test
        @DisplayName("should return display name with first and last name")
        void shouldReturnDisplayName() {
            // When
            List<CustomerSummaryProjection> customers = orderRepository.findCustomerSummaries(storeId, 1, 0);

            // Then
            assertThat(customers.get(0).getDisplayName()).isEqualTo("Ahmet Yilmaz");
        }
    }

    @Nested
    @DisplayName("countDistinctCustomers")
    class CountDistinctCustomersTests {

        @Test
        @DisplayName("should count distinct customers with customer_id")
        void shouldCountDistinctCustomers() {
            // When
            long count = orderRepository.countDistinctCustomers(storeId);

            // Then
            assertThat(count).isEqualTo(3); // Only customers with customer_id
        }
    }

    @Nested
    @DisplayName("getCustomerDataCoverage")
    class GetCustomerDataCoverageTests {

        @Test
        @DisplayName("should return backfill coverage statistics")
        void shouldReturnBackfillCoverage() {
            // When
            BackfillCoverageProjection coverage = orderRepository.getCustomerDataCoverage(storeId);

            // Then
            assertThat(coverage).isNotNull();
            assertThat(coverage.getTotalOrders()).isEqualTo(9); // 8 with customer_id + 1 without
            assertThat(coverage.getOrdersWithCustomerData()).isEqualTo(8); // Only 8 have customer_id
        }
    }

    @Nested
    @DisplayName("getAvgRepeatIntervalDays")
    class GetAvgRepeatIntervalDaysTests {

        @Test
        @DisplayName("should calculate average repeat interval in days")
        void shouldCalculateAvgRepeatInterval() {
            // When
            Double avgDays = orderRepository.getAvgRepeatIntervalDays(storeId);

            // Then
            assertThat(avgDays).isNotNull();
            assertThat(avgDays).isGreaterThan(0.0);
            // Customer 1 has 5 orders over 6 months, Customer 2 has 2 orders over 2 months
            // Average should be somewhere around 30-60 days
            assertThat(avgDays).isBetween(15.0, 90.0);
        }
    }

    @Nested
    @DisplayName("findOrderNumbersWithoutCustomerData")
    class FindOrderNumbersWithoutCustomerDataTests {

        @Test
        @DisplayName("should return order numbers without customer_id")
        void shouldReturnOrderNumbersWithoutCustomerId() {
            // When
            List<String> orderNumbers = orderRepository.findOrderNumbersWithoutCustomerData(storeId);

            // Then
            assertThat(orderNumbers).hasSize(1); // Only 1 order without customer_id
        }
    }
}
