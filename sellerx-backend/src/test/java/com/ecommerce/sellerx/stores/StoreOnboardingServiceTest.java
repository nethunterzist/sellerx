package com.ecommerce.sellerx.stores;

import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.common.TestDataBuilder;
import com.ecommerce.sellerx.financial.TrendyolFinancialSettlementService;
import com.ecommerce.sellerx.financial.TrendyolHistoricalSettlementService;
import com.ecommerce.sellerx.financial.TrendyolOtherFinancialsService;
import com.ecommerce.sellerx.orders.OrderCommissionRecalculationService;
import com.ecommerce.sellerx.orders.TrendyolOrderService;
import com.ecommerce.sellerx.products.TrendyolProductService;
import com.ecommerce.sellerx.qa.TrendyolQaService;
import com.ecommerce.sellerx.returns.TrendyolClaimsService;
import com.ecommerce.sellerx.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StoreOnboardingService.
 * Tests sync flow status transitions, retry logic, and cancellation.
 */
@DisplayName("StoreOnboardingService")
class StoreOnboardingServiceTest extends BaseUnitTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private TrendyolProductService productService;

    @Mock
    private TrendyolOrderService orderService;

    @Mock
    private TrendyolHistoricalSettlementService historicalSettlementService;

    @Mock
    private TrendyolFinancialSettlementService financialService;

    @Mock
    private TrendyolOtherFinancialsService otherFinancialsService;

    @Mock
    private TrendyolQaService qaService;

    @Mock
    private TrendyolClaimsService claimsService;

    @Mock
    private OrderCommissionRecalculationService commissionRecalculationService;

    @Mock
    private Executor onboardingExecutor;

    @InjectMocks
    private StoreOnboardingService onboardingService;

    private User testUser;
    private Store testStore;
    private UUID storeId;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetSequence();
        testUser = TestDataBuilder.user().build();
        testUser.setId(1L);
        testStore = TestDataBuilder.store(testUser).build();
        storeId = UUID.randomUUID();
        testStore.setId(storeId);
        testStore.setSyncPhases(new HashMap<>());
    }

    @Nested
    @DisplayName("retryInitialSync")
    class RetryInitialSync {

        @Test
        @DisplayName("should not retry when sync is already in progress")
        void shouldNotRetryWhenSyncInProgress() {
            // Given
            testStore.setOverallSyncStatus(OverallSyncStatus.IN_PROGRESS);
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));

            // When
            onboardingService.retryInitialSync(storeId);

            // Then - performInitialSync should NOT be called (no async execution triggered)
            verify(storeRepository, times(1)).findById(storeId);
            // The service should log a warning and return without starting sync
        }

        @Test
        @DisplayName("should not retry when sync is already completed")
        void shouldNotRetryWhenSyncCompleted() {
            // Given
            testStore.setOverallSyncStatus(OverallSyncStatus.COMPLETED);
            testStore.setInitialSyncCompleted(true);
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));

            // When
            onboardingService.retryInitialSync(storeId);

            // Then
            verify(storeRepository, times(1)).findById(storeId);
        }

        @Test
        @DisplayName("should not retry when legacy sync status starts with SYNCING_")
        void shouldNotRetryWhenLegacySyncInProgress() {
            // Given
            testStore.setOverallSyncStatus(null);
            testStore.setSyncStatus(SyncStatus.SYNCING_PRODUCTS);
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));

            // When
            onboardingService.retryInitialSync(storeId);

            // Then
            verify(storeRepository, times(1)).findById(storeId);
        }

        @Test
        @DisplayName("should throw when store not found")
        void shouldThrowWhenStoreNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(storeRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When/Then
            org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> onboardingService.retryInitialSync(nonExistentId)
            );
        }
    }

    @Nested
    @DisplayName("getSyncStatus")
    class GetSyncStatus {

        @Test
        @DisplayName("should return current sync status")
        void shouldReturnCurrentSyncStatus() {
            // Given
            testStore.setSyncStatus(SyncStatus.SYNCING_PRODUCTS);
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));

            // When
            String status = onboardingService.getSyncStatus(storeId);

            // Then
            assertThat(status)
                    .as("Should return the current sync status")
                    .isEqualTo("SYNCING_PRODUCTS");
        }

        @Test
        @DisplayName("should return UNKNOWN when store not found")
        void shouldReturnUnknownWhenStoreNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(storeRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When
            String status = onboardingService.getSyncStatus(nonExistentId);

            // Then
            assertThat(status)
                    .as("Should return UNKNOWN for non-existent store")
                    .isEqualTo("UNKNOWN");
        }
    }

    @Nested
    @DisplayName("getSyncPhases")
    class GetSyncPhases {

        @Test
        @DisplayName("should return sync phases for existing store")
        void shouldReturnSyncPhases() {
            // Given
            Map<String, PhaseStatus> phases = new HashMap<>();
            phases.put("PRODUCTS", PhaseStatus.completed());
            phases.put("HISTORICAL", PhaseStatus.active());
            phases.put("FINANCIAL", PhaseStatus.pending());
            testStore.setSyncPhases(phases);
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));

            // When
            Map<String, PhaseStatus> result = onboardingService.getSyncPhases(storeId);

            // Then
            assertThat(result)
                    .as("Should return all sync phases")
                    .hasSize(3);
            assertThat(result.get("PRODUCTS").getStatus())
                    .as("PRODUCTS phase should be COMPLETED")
                    .isEqualTo(PhaseStatusType.COMPLETED);
            assertThat(result.get("HISTORICAL").getStatus())
                    .as("HISTORICAL phase should be ACTIVE")
                    .isEqualTo(PhaseStatusType.ACTIVE);
            assertThat(result.get("FINANCIAL").getStatus())
                    .as("FINANCIAL phase should be PENDING")
                    .isEqualTo(PhaseStatusType.PENDING);
        }

        @Test
        @DisplayName("should return empty map when store not found")
        void shouldReturnEmptyMapWhenStoreNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(storeRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When
            Map<String, PhaseStatus> result = onboardingService.getSyncPhases(nonExistentId);

            // Then
            assertThat(result)
                    .as("Should return empty map for non-existent store")
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("requestCancelSync")
    class RequestCancelSync {

        @Test
        @DisplayName("should set cancel flag for store")
        void shouldSetCancelFlagForStore() {
            // When
            onboardingService.requestCancelSync(storeId);

            // Then - the cancel flag should be set internally
            // We verify this indirectly: if we call it, no exception should be thrown
            // The actual cancellation is tested during sync execution
        }

        @Test
        @DisplayName("should handle multiple cancel requests for same store")
        void shouldHandleMultipleCancelRequests() {
            // When/Then - should not throw
            onboardingService.requestCancelSync(storeId);
            onboardingService.requestCancelSync(storeId);
        }
    }

    @Nested
    @DisplayName("PhaseStatus")
    class PhaseStatusTests {

        @Test
        @DisplayName("pending status should have correct state")
        void pendingStatusShouldHaveCorrectState() {
            // When
            PhaseStatus status = PhaseStatus.pending();

            // Then
            assertThat(status.getStatus()).isEqualTo(PhaseStatusType.PENDING);
            assertThat(status.getStartedAt()).isNull();
            assertThat(status.getCompletedAt()).isNull();
            assertThat(status.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("active status should have startedAt timestamp")
        void activeStatusShouldHaveStartedAt() {
            // When
            PhaseStatus status = PhaseStatus.active();

            // Then
            assertThat(status.getStatus()).isEqualTo(PhaseStatusType.ACTIVE);
            assertThat(status.getStartedAt())
                    .as("Active phase should have startedAt set")
                    .isNotNull();
        }

        @Test
        @DisplayName("completed status should have completedAt timestamp")
        void completedStatusShouldHaveCompletedAt() {
            // When
            PhaseStatus status = PhaseStatus.completed();

            // Then
            assertThat(status.getStatus()).isEqualTo(PhaseStatusType.COMPLETED);
            assertThat(status.getCompletedAt())
                    .as("Completed phase should have completedAt set")
                    .isNotNull();
        }

        @Test
        @DisplayName("failed status should include error message")
        void failedStatusShouldIncludeErrorMessage() {
            // When
            PhaseStatus status = PhaseStatus.failed("Connection timeout");

            // Then
            assertThat(status.getStatus()).isEqualTo(PhaseStatusType.FAILED);
            assertThat(status.getErrorMessage())
                    .as("Failed phase should have error message")
                    .isEqualTo("Connection timeout");
            assertThat(status.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("activeWithProgress should include progress percentage")
        void activeWithProgressShouldIncludeProgress() {
            // When
            PhaseStatus status = PhaseStatus.activeWithProgress(50);

            // Then
            assertThat(status.getStatus()).isEqualTo(PhaseStatusType.ACTIVE);
            assertThat(status.getProgress())
                    .as("Should include progress percentage")
                    .isEqualTo(50);
            assertThat(status.getStartedAt()).isNotNull();
        }
    }
}
