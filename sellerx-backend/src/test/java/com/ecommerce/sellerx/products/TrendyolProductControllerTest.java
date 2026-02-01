package com.ecommerce.sellerx.products;

import com.ecommerce.sellerx.common.BaseControllerTest;
import com.ecommerce.sellerx.common.TestDataBuilder;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.stores.SyncStatus;
import com.ecommerce.sellerx.stores.TrendyolCredentials;
import com.ecommerce.sellerx.stores.WebhookStatus;
import com.ecommerce.sellerx.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for TrendyolProductController.
 * Tests product listing, cost/stock updates, and authorization.
 */
@DisplayName("TrendyolProductController")
class TrendyolProductControllerTest extends BaseControllerTest {

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private TrendyolProductRepository productRepository;

    private static final String PRODUCTS_URL = "/products";

    @BeforeEach
    void setUpProducts() {
        productRepository.deleteAll();
        storeRepository.deleteAll();
        cleanUpUsers();
        TestDataBuilder.resetSequence();
    }

    @Nested
    @DisplayName("GET /products/store/{storeId}")
    class GetProductsByStore {

        @Test
        @DisplayName("should return products for store owner")
        void shouldReturnProductsForStoreOwner() throws Exception {
            // Given
            User user = createAndSaveTestUser("product-owner@example.com");
            Store store = createAndSaveStore(user);
            createAndSaveProduct(store, "Product 1", "BARCODE-001");
            createAndSaveProduct(store, "Product 2", "BARCODE-002");

            // When/Then
            performWithAuth(get(PRODUCTS_URL + "/store/" + store.getId()), user)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.products", hasSize(2)))
                    .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        @DisplayName("should return empty list when store has no products")
        void shouldReturnEmptyListWhenStoreHasNoProducts() throws Exception {
            // Given
            User user = createAndSaveTestUser("product-owner@example.com");
            Store store = createAndSaveStore(user);

            // When/Then
            performWithAuth(get(PRODUCTS_URL + "/store/" + store.getId()), user)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.products", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("should return 403 when user is not store owner")
        void shouldReturn403WhenUserIsNotStoreOwner() throws Exception {
            // Given
            User owner = createAndSaveTestUser("owner@example.com");
            User otherUser = createAndSaveTestUser("other@example.com");
            Store store = createAndSaveStore(owner);

            // When/Then
            performWithAuth(get(PRODUCTS_URL + "/store/" + store.getId()), otherUser)
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticatedRequest() throws Exception {
            // When/Then
            performWithoutAuth(get(PRODUCTS_URL + "/store/" + UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should support pagination parameters")
        void shouldSupportPaginationParameters() throws Exception {
            // Given
            User user = createAndSaveTestUser("product-owner@example.com");
            Store store = createAndSaveStore(user);

            // Create 15 products
            for (int i = 1; i <= 15; i++) {
                createAndSaveProduct(store, "Product " + i, "BARCODE-" + String.format("%03d", i));
            }

            // When/Then - request page 0 with size 5
            performWithAuth(get(PRODUCTS_URL + "/store/" + store.getId() + "?page=0&size=5"), user)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.products", hasSize(5)))
                    .andExpect(jsonPath("$.totalElements").value(15))
                    .andExpect(jsonPath("$.totalPages").value(3));
        }

        @Test
        @DisplayName("should support search parameter")
        void shouldSupportSearchParameter() throws Exception {
            // Given
            User user = createAndSaveTestUser("product-owner@example.com");
            Store store = createAndSaveStore(user);
            createAndSaveProduct(store, "Apple iPhone 15", "BARCODE-001");
            createAndSaveProduct(store, "Samsung Galaxy S24", "BARCODE-002");
            createAndSaveProduct(store, "Apple MacBook Pro", "BARCODE-003");

            // When/Then - search for "Apple"
            performWithAuth(get(PRODUCTS_URL + "/store/" + store.getId() + "?search=Apple"), user)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.products", hasSize(2)));
        }
    }

    @Nested
    @DisplayName("GET /products/store/{storeId}/all")
    class GetAllProductsByStore {

        @Test
        @DisplayName("should return all products for store owner")
        void shouldReturnAllProductsForStoreOwner() throws Exception {
            // Given
            User user = createAndSaveTestUser("product-owner@example.com");
            Store store = createAndSaveStore(user);
            createAndSaveProduct(store, "Product 1", "BARCODE-001");
            createAndSaveProduct(store, "Product 2", "BARCODE-002");

            // When/Then
            performWithAuth(get(PRODUCTS_URL + "/store/" + store.getId() + "/all"), user)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.products", hasSize(2)));
        }

        @Test
        @DisplayName("should return 403 when user is not store owner")
        void shouldReturn403WhenUserIsNotStoreOwner() throws Exception {
            // Given
            User owner = createAndSaveTestUser("owner@example.com");
            User otherUser = createAndSaveTestUser("other@example.com");
            Store store = createAndSaveStore(owner);

            // When/Then
            performWithAuth(get(PRODUCTS_URL + "/store/" + store.getId() + "/all"), otherUser)
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /products/{productId}/cost-and-stock")
    class UpdateCostAndStock {

        @Test
        @DisplayName("should update cost and stock for product owner")
        void shouldUpdateCostAndStockForProductOwner() throws Exception {
            // Given
            User user = createAndSaveTestUser("product-owner@example.com");
            Store store = createAndSaveStore(user);
            TrendyolProduct product = createAndSaveProduct(store, "Test Product", "BARCODE-001");

            String requestBody = """
                {
                    "unitCost": 100.50,
                    "quantity": 50,
                    "costVatRate": 20,
                    "stockDate": "2024-01-15"
                }
                """;

            // When/Then
            performWithAuth(put(PRODUCTS_URL + "/" + product.getId() + "/cost-and-stock").content(requestBody), user)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.costAndStockInfo", hasSize(1)))
                    .andExpect(jsonPath("$.costAndStockInfo[0].unitCost").value(100.50))
                    .andExpect(jsonPath("$.costAndStockInfo[0].quantity").value(50));
        }

        @Test
        @DisplayName("should return 403 when user is not product owner")
        void shouldReturn403WhenUserIsNotProductOwner() throws Exception {
            // Given
            User owner = createAndSaveTestUser("owner@example.com");
            User otherUser = createAndSaveTestUser("other@example.com");
            Store store = createAndSaveStore(owner);
            TrendyolProduct product = createAndSaveProduct(store, "Test Product", "BARCODE-001");

            String requestBody = """
                {
                    "unitCost": 100.50,
                    "quantity": 50,
                    "costVatRate": 20,
                    "stockDate": "2024-01-15"
                }
                """;

            // When/Then
            performWithAuth(put(PRODUCTS_URL + "/" + product.getId() + "/cost-and-stock").content(requestBody), otherUser)
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticatedRequest() throws Exception {
            // Given
            String requestBody = """
                {
                    "unitCost": 100.50,
                    "quantity": 50,
                    "costVatRate": 20,
                    "stockDate": "2024-01-15"
                }
                """;

            // When/Then
            performWithoutAuth(put(PRODUCTS_URL + "/" + UUID.randomUUID() + "/cost-and-stock").content(requestBody))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /products/{productId}/stock-info")
    class AddStockInfo {

        @Test
        @DisplayName("should add stock info for product owner")
        void shouldAddStockInfoForProductOwner() throws Exception {
            // Given
            User user = createAndSaveTestUser("product-owner@example.com");
            Store store = createAndSaveStore(user);
            TrendyolProduct product = createAndSaveProduct(store, "Test Product", "BARCODE-001");

            String requestBody = """
                {
                    "unitCost": 75.00,
                    "quantity": 30,
                    "costVatRate": 20,
                    "stockDate": "2024-02-01"
                }
                """;

            // When/Then
            performWithAuth(post(PRODUCTS_URL + "/" + product.getId() + "/stock-info").content(requestBody), user)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.costAndStockInfo").isArray());
        }

        @Test
        @DisplayName("should return 403 when user is not product owner")
        void shouldReturn403WhenUserIsNotProductOwner() throws Exception {
            // Given
            User owner = createAndSaveTestUser("owner@example.com");
            User otherUser = createAndSaveTestUser("other@example.com");
            Store store = createAndSaveStore(owner);
            TrendyolProduct product = createAndSaveProduct(store, "Test Product", "BARCODE-001");

            String requestBody = """
                {
                    "unitCost": 75.00,
                    "quantity": 30,
                    "costVatRate": 20,
                    "stockDate": "2024-02-01"
                }
                """;

            // When/Then
            performWithAuth(post(PRODUCTS_URL + "/" + product.getId() + "/stock-info").content(requestBody), otherUser)
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /products/{productId}/stock-info/{stockDate}")
    class DeleteStockInfo {

        @Test
        @DisplayName("should delete stock info for product owner")
        void shouldDeleteStockInfoForProductOwner() throws Exception {
            // Given
            User user = createAndSaveTestUser("product-owner@example.com");
            Store store = createAndSaveStore(user);
            TrendyolProduct product = createAndSaveProductWithCostInfo(store, "Test Product", "BARCODE-001");
            LocalDate stockDate = product.getCostAndStockInfo().get(0).getStockDate();

            // When/Then
            performWithAuth(delete(PRODUCTS_URL + "/" + product.getId() + "/stock-info/" + stockDate), user)
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 403 when user is not product owner")
        void shouldReturn403WhenUserIsNotProductOwner() throws Exception {
            // Given
            User owner = createAndSaveTestUser("owner@example.com");
            User otherUser = createAndSaveTestUser("other@example.com");
            Store store = createAndSaveStore(owner);
            TrendyolProduct product = createAndSaveProductWithCostInfo(store, "Test Product", "BARCODE-001");
            LocalDate stockDate = product.getCostAndStockInfo().get(0).getStockDate();

            // When/Then
            performWithAuth(delete(PRODUCTS_URL + "/" + product.getId() + "/stock-info/" + stockDate), otherUser)
                    .andExpect(status().isForbidden());
        }
    }

    // Helper methods

    private Store createAndSaveStore(User user) {
        TrendyolCredentials credentials = TrendyolCredentials.builder()
                .apiKey("test-api-key")
                .apiSecret("test-api-secret")
                .sellerId(123456L)
                .integrationCode("test-integration")
                .build();

        Store store = Store.builder()
                .user(user)
                .storeName("Test Store")
                .marketplace("trendyol")
                .credentials(credentials)
                .syncStatus(SyncStatus.COMPLETED)
                .webhookStatus(WebhookStatus.PENDING)
                .initialSyncCompleted(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return storeRepository.save(store);
    }

    private TrendyolProduct createAndSaveProduct(Store store, String title, String barcode) {
        TrendyolProduct product = TrendyolProduct.builder()
                .store(store)
                .productId("PROD-" + barcode)
                .barcode(barcode)
                .title(title)
                .categoryName("Test Category")
                .brand("Test Brand")
                .brandId(1000L)
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
                .build();

        return productRepository.save(product);
    }

    private TrendyolProduct createAndSaveProductWithCostInfo(Store store, String title, String barcode) {
        List<CostAndStockInfo> costInfo = new ArrayList<>();
        costInfo.add(CostAndStockInfo.builder()
                .unitCost(100.0)
                .quantity(50)
                .costVatRate(20)
                .stockDate(LocalDate.now())
                .usedQuantity(0)
                .build());

        TrendyolProduct product = TrendyolProduct.builder()
                .store(store)
                .productId("PROD-" + barcode)
                .barcode(barcode)
                .title(title)
                .categoryName("Test Category")
                .brand("Test Brand")
                .brandId(1000L)
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
                .costAndStockInfo(costInfo)
                .build();

        return productRepository.save(product);
    }
}
