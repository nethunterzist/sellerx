package com.ecommerce.sellerx.orders;

import com.ecommerce.sellerx.common.BaseControllerTest;
import com.ecommerce.sellerx.common.TestDataBuilder;
import com.ecommerce.sellerx.orders.dto.*;
import com.ecommerce.sellerx.stores.*;
import com.ecommerce.sellerx.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CustomerAnalyticsController.
 * Tests all customer analytics endpoints including authorization.
 */
@DisplayName("CustomerAnalyticsController")
class CustomerAnalyticsControllerTest extends BaseControllerTest {

    @Autowired
    private StoreRepository storeRepository;

    @MockBean
    private CustomerAnalyticsService analyticsService;

    private static final String BASE_URL = "/api/stores/{storeId}/customer-analytics";

    private User testUser;
    private Store testStore;

    @BeforeEach
    void setUpTest() {
        storeRepository.deleteAll();
        cleanUpUsers();
        TestDataBuilder.resetSequence();

        testUser = createAndSaveTestUser("customer-analytics-test@example.com");
        testStore = createAndSaveStore(testUser, "Analytics Test Store");
    }

    @Nested
    @DisplayName("GET /api/stores/{storeId}/customer-analytics/summary")
    class GetSummary {

        @Test
        @DisplayName("should return analytics summary for store owner")
        void shouldReturnAnalyticsSummaryForStoreOwner() throws Exception {
            // Given
            CustomerAnalyticsResponse response = createMockAnalyticsResponse();
            when(analyticsService.getAnalytics(testStore.getId())).thenReturn(response);

            // When/Then
            performWithAuth(get(BASE_URL + "/summary", testStore.getId()), testUser)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.summary.totalCustomers").value(100))
                    .andExpect(jsonPath("$.summary.repeatCustomers").value(30))
                    .andExpect(jsonPath("$.summary.repeatRate").value(30.0))
                    .andExpect(jsonPath("$.segmentation", hasSize(3)))
                    .andExpect(jsonPath("$.cityAnalysis", hasSize(2)))
                    .andExpect(jsonPath("$.monthlyTrend", hasSize(1)));

            verify(analyticsService).getAnalytics(testStore.getId());
        }

        @Test
        @DisplayName("should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticatedRequest() throws Exception {
            // When/Then
            performWithoutAuth(get(BASE_URL + "/summary", testStore.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 403 for non-owner user")
        void shouldReturn403ForNonOwnerUser() throws Exception {
            // Given
            User otherUser = createAndSaveTestUser("other-summary@example.com");

            // When/Then
            performWithAuth(get(BASE_URL + "/summary", testStore.getId()), otherUser)
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/stores/{storeId}/customer-analytics/customers")
    class GetCustomers {

        @Test
        @DisplayName("should return paginated customer list with default params")
        void shouldReturnPaginatedCustomerListWithDefaultParams() throws Exception {
            // Given
            Map<String, Object> response = createMockCustomerListResponse();
            when(analyticsService.getCustomerList(eq(testStore.getId()), eq(0), eq(20), eq("totalSpend"), eq("desc"), isNull(), any()))
                    .thenReturn(response);

            // When/Then
            performWithAuth(get(BASE_URL + "/customers", testStore.getId()), testUser)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(50))
                    .andExpect(jsonPath("$.totalPages").value(3))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.content", hasSize(2)));

            verify(analyticsService).getCustomerList(eq(testStore.getId()), eq(0), eq(20), eq("totalSpend"), eq("desc"), isNull(), any());
        }

        @Test
        @DisplayName("should accept custom pagination params")
        void shouldAcceptCustomPaginationParams() throws Exception {
            // Given
            Map<String, Object> response = createMockCustomerListResponse();
            when(analyticsService.getCustomerList(eq(testStore.getId()), eq(2), eq(10), eq("orderCount"), eq("desc"), isNull(), any()))
                    .thenReturn(response);

            // When/Then
            performWithAuth(get(BASE_URL + "/customers?page=2&size=10&sortBy=orderCount", testStore.getId()), testUser)
                    .andExpect(status().isOk());

            verify(analyticsService).getCustomerList(eq(testStore.getId()), eq(2), eq(10), eq("orderCount"), eq("desc"), isNull(), any());
        }

        @Test
        @DisplayName("should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticatedRequest() throws Exception {
            // When/Then
            performWithoutAuth(get(BASE_URL + "/customers", testStore.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 403 for non-owner user")
        void shouldReturn403ForNonOwnerUser() throws Exception {
            // Given
            User otherUser = createAndSaveTestUser("other-customers@example.com");

            // When/Then
            performWithAuth(get(BASE_URL + "/customers", testStore.getId()), otherUser)
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/stores/{storeId}/customer-analytics/product-repeat")
    class GetProductRepeat {

        @Test
        @DisplayName("should return product repeat analysis")
        void shouldReturnProductRepeatAnalysis() throws Exception {
            // Given
            List<ProductRepeatData> response = createMockProductRepeatData();
            when(analyticsService.getProductRepeatAnalysis(testStore.getId())).thenReturn(response);

            // When/Then
            performWithAuth(get(BASE_URL + "/product-repeat", testStore.getId()), testUser)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].barcode").value("BARCODE-001"))
                    .andExpect(jsonPath("$[0].productName").value("Product A"))
                    .andExpect(jsonPath("$[0].repeatRate").value(30.0));

            verify(analyticsService).getProductRepeatAnalysis(testStore.getId());
        }

        @Test
        @DisplayName("should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticatedRequest() throws Exception {
            // When/Then
            performWithoutAuth(get(BASE_URL + "/product-repeat", testStore.getId()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/stores/{storeId}/customer-analytics/cross-sell")
    class GetCrossSell {

        @Test
        @DisplayName("should return cross-sell analysis")
        void shouldReturnCrossSellAnalysis() throws Exception {
            // Given
            List<CrossSellData> response = createMockCrossSellData();
            when(analyticsService.getCrossSellAnalysis(testStore.getId())).thenReturn(response);

            // When/Then
            performWithAuth(get(BASE_URL + "/cross-sell", testStore.getId()), testUser)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].sourceBarcode").value("BARCODE-A"))
                    .andExpect(jsonPath("$[0].targetBarcode").value("BARCODE-B"))
                    .andExpect(jsonPath("$[0].coOccurrenceCount").value(25));

            verify(analyticsService).getCrossSellAnalysis(testStore.getId());
        }

        @Test
        @DisplayName("should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticatedRequest() throws Exception {
            // When/Then
            performWithoutAuth(get(BASE_URL + "/cross-sell", testStore.getId()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/stores/{storeId}/customer-analytics/backfill-status")
    class GetBackfillStatus {

        @Test
        @DisplayName("should return backfill coverage status")
        void shouldReturnBackfillCoverageStatus() throws Exception {
            // Given
            Map<String, Object> response = createMockBackfillCoverageResponse();
            when(analyticsService.getBackfillCoverage(testStore.getId())).thenReturn(response);

            // When/Then
            performWithAuth(get(BASE_URL + "/backfill-status", testStore.getId()), testUser)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalOrders").value(1000))
                    .andExpect(jsonPath("$.ordersWithCustomerData").value(750))
                    .andExpect(jsonPath("$.coveragePercent").value(75.0))
                    .andExpect(jsonPath("$.ordersWithoutCustomerData").value(250));

            verify(analyticsService).getBackfillCoverage(testStore.getId());
        }

        @Test
        @DisplayName("should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticatedRequest() throws Exception {
            // When/Then
            performWithoutAuth(get(BASE_URL + "/backfill-status", testStore.getId()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/stores/{storeId}/customer-analytics/trigger-backfill")
    class TriggerBackfill {

        @Test
        @DisplayName("should trigger backfill for store owner")
        void shouldTriggerBackfillForStoreOwner() throws Exception {
            // Given
            doNothing().when(analyticsService).triggerCustomerDataBackfill(testStore.getId());

            // When/Then
            performWithAuth(post(BASE_URL + "/trigger-backfill", testStore.getId()), testUser)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("started"))
                    .andExpect(jsonPath("$.message").exists());

            verify(analyticsService).triggerCustomerDataBackfill(testStore.getId());
        }

        @Test
        @DisplayName("should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticatedRequest() throws Exception {
            // When/Then
            performWithoutAuth(post(BASE_URL + "/trigger-backfill", testStore.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 403 for non-owner user")
        void shouldReturn403ForNonOwnerUser() throws Exception {
            // Given
            User otherUser = createAndSaveTestUser("other-backfill@example.com");

            // When/Then
            performWithAuth(post(BASE_URL + "/trigger-backfill", testStore.getId()), otherUser)
                    .andExpect(status().isForbidden());
        }
    }

    // ========== Helper Methods ==========

    private Store createAndSaveStore(User user, String storeName) {
        TrendyolCredentials credentials = TrendyolCredentials.builder()
                .apiKey("test-api-key")
                .apiSecret("test-api-secret")
                .sellerId(123456L)
                .integrationCode("test-integration")
                .build();

        Store store = Store.builder()
                .user(user)
                .storeName(storeName)
                .marketplace("trendyol")
                .credentials(credentials)
                .syncStatus(SyncStatus.COMPLETED)
                .webhookStatus(WebhookStatus.ACTIVE)
                .initialSyncCompleted(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return storeRepository.save(store);
    }

    private CustomerAnalyticsResponse createMockAnalyticsResponse() {
        CustomerAnalyticsSummary summary = CustomerAnalyticsSummary.builder()
                .totalCustomers(100)
                .repeatCustomers(30)
                .repeatRate(30.0)
                .totalRevenue(new BigDecimal("50000.00"))
                .repeatCustomerRevenue(new BigDecimal("20000.00"))
                .avgOrdersPerCustomer(2.5)
                .avgRepeatIntervalDays(45.0)
                .repeatRevenueShare(40.0)
                .build();

        List<SegmentData> segmentation = List.of(
                SegmentData.builder().segment("1").customerCount(70).totalRevenue(new BigDecimal("15000.00")).build(),
                SegmentData.builder().segment("2-3").customerCount(20).totalRevenue(new BigDecimal("18000.00")).build(),
                SegmentData.builder().segment("4-6").customerCount(10).totalRevenue(new BigDecimal("17000.00")).build()
        );

        List<CityRepeatData> cityAnalysis = List.of(
                CityRepeatData.builder().city("Istanbul").totalCustomers(40).repeatCustomers(15).repeatRate(37.5).totalRevenue(new BigDecimal("20000.00")).build(),
                CityRepeatData.builder().city("Ankara").totalCustomers(25).repeatCustomers(8).repeatRate(32.0).totalRevenue(new BigDecimal("12000.00")).build()
        );

        List<MonthlyTrend> monthlyTrend = List.of(
                MonthlyTrend.builder().month("2025-01").newCustomers(10).repeatCustomers(5).newRevenue(new BigDecimal("5000.00")).repeatRevenue(new BigDecimal("3000.00")).build()
        );

        return CustomerAnalyticsResponse.builder()
                .summary(summary)
                .segmentation(segmentation)
                .cityAnalysis(cityAnalysis)
                .monthlyTrend(monthlyTrend)
                .build();
    }

    private Map<String, Object> createMockCustomerListResponse() {
        List<CustomerListItem> content = List.of(
                CustomerListItem.builder()
                        .customerKey("1001")
                        .displayName("John Doe")
                        .city("Istanbul")
                        .orderCount(5)
                        .totalSpend(new BigDecimal("2500.00"))
                        .avgOrderValue(500.0)
                        .recencyScore(5)
                        .frequencyScore(4)
                        .monetaryScore(4)
                        .rfmSegment("Şampiyonlar")
                        .build(),
                CustomerListItem.builder()
                        .customerKey("1002")
                        .displayName("Jane Smith")
                        .city("Ankara")
                        .orderCount(2)
                        .totalSpend(new BigDecimal("800.00"))
                        .avgOrderValue(400.0)
                        .recencyScore(3)
                        .frequencyScore(2)
                        .monetaryScore(2)
                        .rfmSegment("Potansiyel Sadıklar")
                        .build()
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", content);
        response.put("totalElements", 50L);
        response.put("totalPages", 3);
        response.put("page", 0);
        response.put("size", 20);
        return response;
    }

    private List<ProductRepeatData> createMockProductRepeatData() {
        return List.of(
                ProductRepeatData.builder()
                        .barcode("BARCODE-001")
                        .productName("Product A")
                        .totalBuyers(50)
                        .repeatBuyers(15)
                        .repeatRate(30.0)
                        .avgDaysBetweenRepurchase(30.5)
                        .totalQuantitySold(120)
                        .build(),
                ProductRepeatData.builder()
                        .barcode("BARCODE-002")
                        .productName("Product B")
                        .totalBuyers(30)
                        .repeatBuyers(6)
                        .repeatRate(20.0)
                        .avgDaysBetweenRepurchase(45.0)
                        .totalQuantitySold(80)
                        .build()
        );
    }

    private List<CrossSellData> createMockCrossSellData() {
        return List.of(
                CrossSellData.builder()
                        .sourceBarcode("BARCODE-A")
                        .sourceProductName("Product A")
                        .targetBarcode("BARCODE-B")
                        .targetProductName("Product B")
                        .coOccurrenceCount(25)
                        .confidence(25.0)
                        .build()
        );
    }

    private Map<String, Object> createMockBackfillCoverageResponse() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalOrders", 1000L);
        response.put("ordersWithCustomerData", 750L);
        response.put("coveragePercent", 75.0);
        response.put("ordersWithoutCustomerData", 250L);
        response.put("note", "Coverage indicates orders with customer_id data available.");
        return response;
    }
}
