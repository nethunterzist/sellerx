package com.ecommerce.sellerx.buybox;

import com.ecommerce.sellerx.buybox.dto.*;
import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.stores.TrendyolCredentials;
import com.ecommerce.sellerx.users.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class BuyboxServiceTest extends BaseUnitTest {

    @Mock
    private BuyboxTrackedProductRepository trackedProductRepository;

    @Mock
    private BuyboxSnapshotRepository snapshotRepository;

    @Mock
    private BuyboxAlertRepository alertRepository;

    @Mock
    private TrendyolProductRepository productRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private TrendyolBuyboxClient buyboxClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private BuyboxService buyboxService;

    private User testUser;
    private User otherUser;
    private Store testStore;
    private TrendyolProduct testProduct;
    private UUID storeId;
    private UUID productId;
    private UUID trackedProductId;

    @BeforeEach
    void setUp() {
        buyboxService = new BuyboxService(
                trackedProductRepository, snapshotRepository, alertRepository,
                productRepository, storeRepository, buyboxClient, objectMapper
        );

        testUser = User.builder()
                .name("Test User")
                .email("test@test.com")
                .build();
        testUser.setId(1L);

        otherUser = User.builder()
                .name("Other User")
                .email("other@test.com")
                .build();
        otherUser.setId(2L);

        storeId = UUID.randomUUID();
        TrendyolCredentials credentials = TrendyolCredentials.builder()
                .apiKey("key")
                .apiSecret("secret")
                .sellerId(12345L)
                .build();

        testStore = Store.builder()
                .storeName("Test Store")
                .marketplace("trendyol")
                .user(testUser)
                .credentials(credentials)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testStore.setId(storeId);

        productId = UUID.randomUUID();
        testProduct = TrendyolProduct.builder()
                .store(testStore)
                .productId("TRENDYOL-123")
                .title("Test Product")
                .barcode("BARCODE123")
                .image("https://example.com/img.jpg")
                .salePrice(new BigDecimal("100.00"))
                .build();
        testProduct.setId(productId);

        trackedProductId = UUID.randomUUID();
    }

    private BuyboxTrackedProduct createTrackedProduct() {
        BuyboxTrackedProduct tracked = BuyboxTrackedProduct.builder()
                .store(testStore)
                .product(testProduct)
                .isActive(true)
                .alertOnLoss(true)
                .alertOnNewCompetitor(true)
                .alertPriceThreshold(new BigDecimal("10.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        tracked.setId(trackedProductId);
        return tracked;
    }

    @Nested
    @DisplayName("addProductToTrack")
    class AddProductToTrack {

        @Test
        @DisplayName("should add product to tracking successfully")
        void shouldAddProductToTracking() {
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
            when(trackedProductRepository.existsByStoreIdAndProductId(storeId, productId)).thenReturn(false);
            when(trackedProductRepository.countByStoreIdAndIsActiveTrue(storeId)).thenReturn(0);
            when(trackedProductRepository.save(any(BuyboxTrackedProduct.class))).thenAnswer(invocation -> {
                BuyboxTrackedProduct saved = invocation.getArgument(0);
                saved.setId(trackedProductId);
                return saved;
            });
            // For initial buybox check - return failed response to avoid complex chain
            when(buyboxClient.fetchBuyboxData(any(), any())).thenReturn(
                    BuyboxApiResponse.builder().success(false).errorMessage("Initial check skipped").build()
            );
            when(snapshotRepository.findTopByTrackedProductIdOrderByCheckedAtDesc(any()))
                    .thenReturn(Optional.empty());

            BuyboxTrackedProductDto result = buyboxService.addProductToTrack(storeId, productId, testUser);

            assertThat(result).isNotNull();
            assertThat(result.getProductTitle()).isEqualTo("Test Product");
            assertThat(result.isActive()).isTrue();
            verify(trackedProductRepository).save(any(BuyboxTrackedProduct.class));
        }

        @Test
        @DisplayName("should throw when store not found")
        void shouldThrowWhenStoreNotFound() {
            UUID unknownStoreId = UUID.randomUUID();
            when(storeRepository.findById(unknownStoreId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> buyboxService.addProductToTrack(unknownStoreId, productId, testUser))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(unknownStoreId.toString());
        }

        @Test
        @DisplayName("should throw when user does not own the store")
        void shouldThrowWhenUserDoesNotOwnStore() {
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));

            assertThatThrownBy(() -> buyboxService.addProductToTrack(storeId, productId, otherUser))
                    .isInstanceOf(SecurityException.class);
        }

        @Test
        @DisplayName("should throw when product does not belong to store")
        void shouldThrowWhenProductNotInStore() {
            Store otherStore = Store.builder()
                    .storeName("Other Store")
                    .marketplace("trendyol")
                    .user(testUser)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            otherStore.setId(UUID.randomUUID());

            TrendyolProduct otherProduct = TrendyolProduct.builder()
                    .store(otherStore) // different store
                    .productId("OTHER-123")
                    .title("Other Product")
                    .build();
            otherProduct.setId(productId);

            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(productRepository.findById(productId)).thenReturn(Optional.of(otherProduct));

            assertThatThrownBy(() -> buyboxService.addProductToTrack(storeId, productId, testUser))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ait de");
        }

        @Test
        @DisplayName("should throw when product already tracked")
        void shouldThrowWhenAlreadyTracked() {
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
            when(trackedProductRepository.existsByStoreIdAndProductId(storeId, productId)).thenReturn(true);

            assertThatThrownBy(() -> buyboxService.addProductToTrack(storeId, productId, testUser))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("zaten");
        }

        @Test
        @DisplayName("should throw when max tracked products limit reached")
        void shouldThrowWhenLimitReached() {
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
            when(trackedProductRepository.existsByStoreIdAndProductId(storeId, productId)).thenReturn(false);
            when(trackedProductRepository.countByStoreIdAndIsActiveTrue(storeId)).thenReturn(10);

            assertThatThrownBy(() -> buyboxService.addProductToTrack(storeId, productId, testUser))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("10");
        }
    }

    @Nested
    @DisplayName("removeProductFromTrack")
    class RemoveProductFromTrack {

        @Test
        @DisplayName("should remove tracked product")
        void shouldRemoveTrackedProduct() {
            BuyboxTrackedProduct tracked = createTrackedProduct();
            when(trackedProductRepository.findById(trackedProductId)).thenReturn(Optional.of(tracked));

            buyboxService.removeProductFromTrack(storeId, trackedProductId, testUser);

            verify(trackedProductRepository).delete(tracked);
        }

        @Test
        @DisplayName("should throw when tracked product not found")
        void shouldThrowWhenNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(trackedProductRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> buyboxService.removeProductFromTrack(storeId, unknownId, testUser))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw when store mismatch")
        void shouldThrowWhenStoreMismatch() {
            BuyboxTrackedProduct tracked = createTrackedProduct();
            when(trackedProductRepository.findById(trackedProductId)).thenReturn(Optional.of(tracked));

            UUID wrongStoreId = UUID.randomUUID();
            assertThatThrownBy(() -> buyboxService.removeProductFromTrack(wrongStoreId, trackedProductId, testUser))
                    .isInstanceOf(SecurityException.class);
        }
    }

    @Nested
    @DisplayName("getTrackedProducts")
    class GetTrackedProducts {

        @Test
        @DisplayName("should return tracked products for store")
        void shouldReturnTrackedProducts() {
            BuyboxTrackedProduct tracked = createTrackedProduct();
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(trackedProductRepository.findByStoreIdAndIsActiveTrue(storeId)).thenReturn(List.of(tracked));
            when(snapshotRepository.findTopByTrackedProductIdOrderByCheckedAtDesc(trackedProductId))
                    .thenReturn(Optional.empty());

            List<BuyboxTrackedProductDto> result = buyboxService.getTrackedProducts(storeId, testUser);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getProductTitle()).isEqualTo("Test Product");
        }

        @Test
        @DisplayName("should throw when user does not own the store")
        void shouldThrowWhenUnauthorized() {
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));

            assertThatThrownBy(() -> buyboxService.getTrackedProducts(storeId, otherUser))
                    .isInstanceOf(SecurityException.class);
        }
    }

    @Nested
    @DisplayName("updateAlertSettings")
    class UpdateAlertSettings {

        @Test
        @DisplayName("should update alert settings")
        void shouldUpdateAlertSettings() {
            BuyboxTrackedProduct tracked = createTrackedProduct();
            when(trackedProductRepository.findById(trackedProductId)).thenReturn(Optional.of(tracked));
            when(trackedProductRepository.save(any(BuyboxTrackedProduct.class))).thenAnswer(i -> i.getArgument(0));
            when(snapshotRepository.findTopByTrackedProductIdOrderByCheckedAtDesc(trackedProductId))
                    .thenReturn(Optional.empty());

            UpdateAlertSettingsRequest request = UpdateAlertSettingsRequest.builder()
                    .alertOnLoss(false)
                    .alertOnNewCompetitor(false)
                    .alertPriceThreshold(new BigDecimal("5.00"))
                    .isActive(false)
                    .build();

            BuyboxTrackedProductDto result = buyboxService.updateAlertSettings(trackedProductId, request, testUser);

            assertThat(result).isNotNull();
            assertThat(tracked.isAlertOnLoss()).isFalse();
            assertThat(tracked.isAlertOnNewCompetitor()).isFalse();
            assertThat(tracked.getAlertPriceThreshold()).isEqualByComparingTo(new BigDecimal("5.00"));
            assertThat(tracked.isActive()).isFalse();
        }

        @Test
        @DisplayName("should only update non-null fields")
        void shouldOnlyUpdateNonNullFields() {
            BuyboxTrackedProduct tracked = createTrackedProduct();
            when(trackedProductRepository.findById(trackedProductId)).thenReturn(Optional.of(tracked));
            when(trackedProductRepository.save(any(BuyboxTrackedProduct.class))).thenAnswer(i -> i.getArgument(0));
            when(snapshotRepository.findTopByTrackedProductIdOrderByCheckedAtDesc(trackedProductId))
                    .thenReturn(Optional.empty());

            UpdateAlertSettingsRequest request = UpdateAlertSettingsRequest.builder()
                    .alertOnLoss(null) // should not change
                    .alertOnNewCompetitor(null) // should not change
                    .alertPriceThreshold(new BigDecimal("20.00"))
                    .isActive(null) // should not change
                    .build();

            buyboxService.updateAlertSettings(trackedProductId, request, testUser);

            assertThat(tracked.isAlertOnLoss()).isTrue(); // unchanged
            assertThat(tracked.isAlertOnNewCompetitor()).isTrue(); // unchanged
            assertThat(tracked.getAlertPriceThreshold()).isEqualByComparingTo(new BigDecimal("20.00"));
            assertThat(tracked.isActive()).isTrue(); // unchanged
        }
    }

    @Nested
    @DisplayName("getDashboard")
    class GetDashboard {

        @Test
        @DisplayName("should return dashboard with status counts")
        void shouldReturnDashboardWithStatusCounts() {
            BuyboxTrackedProduct tracked = createTrackedProduct();
            BuyboxSnapshot wonSnapshot = BuyboxSnapshot.builder()
                    .trackedProduct(tracked)
                    .buyboxStatus(BuyboxStatus.WON)
                    .checkedAt(LocalDateTime.now())
                    .build();
            wonSnapshot.setId(UUID.randomUUID());

            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(trackedProductRepository.findByStoreIdAndIsActiveTrue(storeId)).thenReturn(List.of(tracked));
            when(snapshotRepository.findTopByTrackedProductIdOrderByCheckedAtDesc(trackedProductId))
                    .thenReturn(Optional.of(wonSnapshot));
            when(alertRepository.countByStoreIdAndIsReadFalse(storeId)).thenReturn(3);
            when(alertRepository.findByStoreIdAndIsReadFalseOrderByCreatedAtDesc(storeId))
                    .thenReturn(List.of());

            BuyboxDashboardDto result = buyboxService.getDashboard(storeId, testUser);

            assertThat(result.getStoreId()).isEqualTo(storeId);
            assertThat(result.getTotalTrackedProducts()).isEqualTo(1);
            assertThat(result.getWonCount()).isEqualTo(1);
            assertThat(result.getLostCount()).isEqualTo(0);
            assertThat(result.getUnreadAlertCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("should throw when user does not own the store")
        void shouldThrowWhenUnauthorized() {
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));

            assertThatThrownBy(() -> buyboxService.getDashboard(storeId, otherUser))
                    .isInstanceOf(SecurityException.class);
        }
    }

    @Nested
    @DisplayName("markAlertsAsRead")
    class MarkAlertsAsRead {

        @Test
        @DisplayName("should mark all alerts as read for store")
        void shouldMarkAlertsAsRead() {
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));

            buyboxService.markAlertsAsRead(storeId, testUser);

            verify(alertRepository).markAllAsReadByStoreId(eq(storeId), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("should throw when user does not own the store")
        void shouldThrowWhenUnauthorized() {
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));

            assertThatThrownBy(() -> buyboxService.markAlertsAsRead(storeId, otherUser))
                    .isInstanceOf(SecurityException.class);
        }
    }
}
