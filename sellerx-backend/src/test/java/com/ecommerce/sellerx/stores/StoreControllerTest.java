package com.ecommerce.sellerx.stores;

import com.ecommerce.sellerx.common.BaseControllerTest;
import com.ecommerce.sellerx.common.TestDataBuilder;
import com.ecommerce.sellerx.stores.SyncStatus;
import com.ecommerce.sellerx.stores.WebhookStatus;
import com.ecommerce.sellerx.users.User;
import com.ecommerce.sellerx.webhook.TrendyolWebhookManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for StoreController.
 * Tests store CRUD operations, sync management, and authorization.
 */
@DisplayName("StoreController")
class StoreControllerTest extends BaseControllerTest {

    @Autowired
    private StoreRepository storeRepository;

    // Mock external services to prevent API calls during tests
    @MockBean
    private StoreOnboardingService storeOnboardingService;

    @MockBean
    private TrendyolWebhookManagementService webhookManagementService;

    private static final String STORES_URL = "/stores";
    private static final String MY_STORES_URL = "/stores/my";

    @BeforeEach
    void setUpStores() {
        storeRepository.deleteAll();
        cleanUpUsers();
        TestDataBuilder.resetSequence();

        // Configure mocks to prevent external API calls
        doNothing().when(storeOnboardingService).performInitialSync(any(Store.class));

        // Return a disabled webhook result to skip webhook creation
        TrendyolWebhookManagementService.WebhookResult disabledResult =
                TrendyolWebhookManagementService.WebhookResult.disabled();
        when(webhookManagementService.createWebhookForStore(any(TrendyolCredentials.class)))
                .thenReturn(disabledResult);
        when(webhookManagementService.deleteWebhookForStore(any(TrendyolCredentials.class), any()))
                .thenReturn(disabledResult);
    }

    @Nested
    @DisplayName("GET /stores/my")
    class GetMyStores {

        @Test
        @DisplayName("should return empty list when user has no stores")
        void shouldReturnEmptyListWhenUserHasNoStores() throws Exception {
            // Given
            User user = createAndSaveTestUser("store-test@example.com");

            // When/Then
            performWithAuth(get(MY_STORES_URL), user)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("should return user's stores")
        void shouldReturnUsersStores() throws Exception {
            // Given
            User user = createAndSaveTestUser("store-test@example.com");
            Store store = createAndSaveStore(user, "My Test Store");

            // When/Then
            performWithAuth(get(MY_STORES_URL), user)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].storeName").value("My Test Store"))
                    .andExpect(jsonPath("$[0].marketplace").value("trendyol"));
        }

        @Test
        @DisplayName("should not return other user's stores")
        void shouldNotReturnOtherUsersStores() throws Exception {
            // Given
            User user1 = createAndSaveTestUser("user1@example.com");
            User user2 = createAndSaveTestUser("user2@example.com");
            createAndSaveStore(user1, "User1 Store");
            createAndSaveStore(user2, "User2 Store");

            // When/Then - user1 should only see their own store
            performWithAuth(get(MY_STORES_URL), user1)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].storeName").value("User1 Store"));
        }

        @Test
        @DisplayName("should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticatedRequest() throws Exception {
            // When/Then
            performWithoutAuth(get(MY_STORES_URL))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /stores/{id}")
    class GetStore {

        @Test
        @DisplayName("should return store for owner")
        void shouldReturnStoreForOwner() throws Exception {
            // Given
            User user = createAndSaveTestUser("store-owner@example.com");
            Store store = createAndSaveStore(user, "Owner's Store");

            // When/Then
            performWithAuth(get(STORES_URL + "/" + store.getId()), user)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.storeName").value("Owner's Store"))
                    .andExpect(jsonPath("$.marketplace").value("trendyol"))
                    .andExpect(jsonPath("$.id").value(store.getId().toString()));
        }

        @Test
        @DisplayName("should return 403 when user is not owner")
        void shouldReturn403WhenUserIsNotOwner() throws Exception {
            // Given
            User owner = createAndSaveTestUser("owner@example.com");
            User otherUser = createAndSaveTestUser("other@example.com");
            Store store = createAndSaveStore(owner, "Owner's Store");

            // When/Then
            performWithAuth(get(STORES_URL + "/" + store.getId()), otherUser)
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 when store not found")
        void shouldReturn404WhenStoreNotFound() throws Exception {
            // Given
            User user = createAndSaveTestUser("user@example.com");
            UUID nonExistentId = UUID.randomUUID();

            // When/Then
            performWithAuth(get(STORES_URL + "/" + nonExistentId), user)
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticatedRequest() throws Exception {
            // When/Then
            performWithoutAuth(get(STORES_URL + "/" + UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /stores")
    class CreateStore {

        @Test
        @DisplayName("should create store with valid request")
        void shouldCreateStoreWithValidRequest() throws Exception {
            // Given
            User user = createAndSaveTestUser("creator@example.com");
            String requestBody = """
                {
                    "storeName": "New Store",
                    "marketplace": "trendyol",
                    "credentials": {
                        "type": "trendyol",
                        "apiKey": "test-api-key",
                        "apiSecret": "test-api-secret",
                        "sellerId": 123456,
                        "integrationCode": "test-integration"
                    }
                }
                """;

            // When/Then
            performWithAuth(post(STORES_URL).content(requestBody), user)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.storeName").value("New Store"))
                    .andExpect(jsonPath("$.marketplace").value("trendyol"))
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(header().exists("Location"));
        }

        @Test
        @DisplayName("should return 400 for missing store name")
        void shouldReturn400ForMissingStoreName() throws Exception {
            // Given
            User user = createAndSaveTestUser("creator@example.com");
            String requestBody = """
                {
                    "marketplace": "trendyol",
                    "credentials": {
                        "type": "trendyol",
                        "apiKey": "test-api-key",
                        "apiSecret": "test-api-secret",
                        "sellerId": 123456
                    }
                }
                """;

            // When/Then
            performWithAuth(post(STORES_URL).content(requestBody), user)
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 for missing marketplace")
        void shouldReturn400ForMissingMarketplace() throws Exception {
            // Given
            User user = createAndSaveTestUser("creator@example.com");
            String requestBody = """
                {
                    "storeName": "New Store",
                    "credentials": {
                        "type": "trendyol",
                        "apiKey": "test-api-key",
                        "apiSecret": "test-api-secret",
                        "sellerId": 123456
                    }
                }
                """;

            // When/Then
            performWithAuth(post(STORES_URL).content(requestBody), user)
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticatedRequest() throws Exception {
            // Given
            String requestBody = """
                {
                    "storeName": "New Store",
                    "marketplace": "trendyol",
                    "credentials": {
                        "type": "trendyol",
                        "apiKey": "test-api-key",
                        "apiSecret": "test-api-secret",
                        "sellerId": 123456
                    }
                }
                """;

            // When/Then
            performWithoutAuth(post(STORES_URL).content(requestBody))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("DELETE /stores/{id}")
    class DeleteStore {

        @Test
        @DisplayName("should delete store for owner")
        void shouldDeleteStoreForOwner() throws Exception {
            // Given
            User user = createAndSaveTestUser("owner@example.com");
            Store store = createAndSaveStore(user, "Store to Delete");

            // When/Then
            performWithAuth(delete(STORES_URL + "/" + store.getId()), user)
                    .andExpect(status().isNoContent());

            // Verify store was deleted
            performWithAuth(get(STORES_URL + "/" + store.getId()), user)
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 403 when user is not owner")
        void shouldReturn403WhenUserIsNotOwner() throws Exception {
            // Given
            User owner = createAndSaveTestUser("owner@example.com");
            User otherUser = createAndSaveTestUser("other@example.com");
            Store store = createAndSaveStore(owner, "Owner's Store");

            // When/Then
            performWithAuth(delete(STORES_URL + "/" + store.getId()), otherUser)
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticatedRequest() throws Exception {
            // When/Then
            performWithoutAuth(delete(STORES_URL + "/" + UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /stores/{id}/sync-status")
    class GetSyncStatus {

        @Test
        @DisplayName("should return sync status for owner")
        void shouldReturnSyncStatusForOwner() throws Exception {
            // Given
            User user = createAndSaveTestUser("owner@example.com");
            Store store = createAndSaveStore(user, "Test Store");

            // When/Then
            performWithAuth(get(STORES_URL + "/" + store.getId() + "/sync-status"), user)
                    .andExpect(status().isOk())
                    .andExpect(content().string(notNullValue()));
        }

        @Test
        @DisplayName("should return 403 when user is not owner")
        void shouldReturn403WhenUserIsNotOwner() throws Exception {
            // Given
            User owner = createAndSaveTestUser("owner@example.com");
            User otherUser = createAndSaveTestUser("other@example.com");
            Store store = createAndSaveStore(owner, "Owner's Store");

            // When/Then
            performWithAuth(get(STORES_URL + "/" + store.getId() + "/sync-status"), otherUser)
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /stores/{id}/sync-progress")
    class GetSyncProgress {

        @Test
        @DisplayName("should return sync progress for owner")
        void shouldReturnSyncProgressForOwner() throws Exception {
            // Given - Use UUID in email to ensure uniqueness across test runs
            String uniqueEmail = "sync-progress-owner-" + UUID.randomUUID() + "@example.com";
            User user = createAndSaveTestUser(uniqueEmail);
            Store store = createAndSaveStore(user, "Test Store");

            // When/Then
            performWithAuth(get(STORES_URL + "/" + store.getId() + "/sync-progress"), user)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.syncStatus").exists())
                    .andExpect(jsonPath("$.percentage").exists());
        }

        @Test
        @DisplayName("should return 403 when user is not owner")
        void shouldReturn403WhenUserIsNotOwner() throws Exception {
            // Given - Use UUID in email to ensure uniqueness across test runs
            String ownerEmail = "sync-progress-owner-" + UUID.randomUUID() + "@example.com";
            String otherEmail = "sync-progress-other-" + UUID.randomUUID() + "@example.com";
            User owner = createAndSaveTestUser(ownerEmail);
            User otherUser = createAndSaveTestUser(otherEmail);
            Store store = createAndSaveStore(owner, "Owner's Store");

            // When/Then
            performWithAuth(get(STORES_URL + "/" + store.getId() + "/sync-progress"), otherUser)
                    .andExpect(status().isForbidden());
        }
    }

    // Helper methods

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
                .syncStatus(SyncStatus.PENDING)
                .webhookStatus(WebhookStatus.PENDING)
                .initialSyncCompleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return storeRepository.save(store);
    }
}
