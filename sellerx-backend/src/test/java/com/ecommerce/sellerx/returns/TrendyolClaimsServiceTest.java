package com.ecommerce.sellerx.returns;

import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.common.TestDataBuilder;
import com.ecommerce.sellerx.orders.TrendyolOrderRepository;
import com.ecommerce.sellerx.returns.dto.*;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.users.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("TrendyolClaimsService")
class TrendyolClaimsServiceTest extends BaseUnitTest {

    @Mock
    private TrendyolClaimRepository claimRepository;

    @Mock
    private TrendyolOrderRepository orderRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ReturnRecordSyncService returnRecordSyncService;

    private TrendyolClaimsService service;

    private User testUser;
    private Store testStore;
    private UUID storeId;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetSequence();
        ObjectMapper objectMapper = new ObjectMapper();
        service = new TrendyolClaimsService(claimRepository, orderRepository, storeRepository, restTemplate, objectMapper, returnRecordSyncService);

        testUser = TestDataBuilder.user().build();
        testStore = TestDataBuilder.completedStore(testUser).build();
        storeId = testStore.getId();
    }

    @Nested
    @DisplayName("getClaims")
    class GetClaims {

        @Test
        @DisplayName("should return paginated claims without status filter")
        void shouldReturnPaginatedClaimsNoStatusFilter() {
            Pageable pageable = PageRequest.of(0, 20);
            TrendyolClaim claim = createTestClaim("CLM-001", "WaitingInAction");
            Page<TrendyolClaim> page = new PageImpl<>(List.of(claim));

            when(claimRepository.findByStoreIdOrderByClaimDateDesc(storeId, pageable))
                    .thenReturn(page);

            Page<ClaimDto> result = service.getClaims(storeId, null, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getClaimId()).isEqualTo("CLM-001");
            verify(claimRepository).findByStoreIdOrderByClaimDateDesc(storeId, pageable);
            verify(claimRepository, never()).findByStoreIdAndStatusOrderByClaimDateDesc(any(), any(), any());
        }

        @Test
        @DisplayName("should return paginated claims with status filter")
        void shouldReturnPaginatedClaimsWithStatusFilter() {
            Pageable pageable = PageRequest.of(0, 20);
            TrendyolClaim claim = createTestClaim("CLM-002", "Accepted");
            Page<TrendyolClaim> page = new PageImpl<>(List.of(claim));

            when(claimRepository.findByStoreIdAndStatusOrderByClaimDateDesc(storeId, "Accepted", pageable))
                    .thenReturn(page);

            Page<ClaimDto> result = service.getClaims(storeId, "Accepted", pageable);

            assertThat(result.getContent().get(0).getStatus()).isEqualTo("Accepted");
            verify(claimRepository).findByStoreIdAndStatusOrderByClaimDateDesc(storeId, "Accepted", pageable);
        }

        @Test
        @DisplayName("should return empty page when no claims exist")
        void shouldReturnEmptyPageWhenNoClaims() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<TrendyolClaim> emptyPage = new PageImpl<>(Collections.emptyList());

            when(claimRepository.findByStoreIdOrderByClaimDateDesc(storeId, pageable))
                    .thenReturn(emptyPage);

            Page<ClaimDto> result = service.getClaims(storeId, null, pageable);

            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("should handle empty string status as no filter")
        void shouldHandleEmptyStringStatusAsNoFilter() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<TrendyolClaim> page = new PageImpl<>(Collections.emptyList());

            when(claimRepository.findByStoreIdOrderByClaimDateDesc(storeId, pageable))
                    .thenReturn(page);

            service.getClaims(storeId, "", pageable);

            verify(claimRepository).findByStoreIdOrderByClaimDateDesc(storeId, pageable);
        }
    }

    @Nested
    @DisplayName("getClaimsByStatuses")
    class GetClaimsByStatuses {

        @Test
        @DisplayName("should return claims matching multiple statuses")
        void shouldReturnClaimsByMultipleStatuses() {
            Pageable pageable = PageRequest.of(0, 20);
            List<String> statuses = List.of("WaitingInAction", "Created");
            TrendyolClaim claim1 = createTestClaim("CLM-010", "WaitingInAction");
            TrendyolClaim claim2 = createTestClaim("CLM-011", "Created");
            Page<TrendyolClaim> page = new PageImpl<>(List.of(claim1, claim2));

            when(claimRepository.findByStoreIdAndStatusInOrderByClaimDateDesc(storeId, statuses, pageable))
                    .thenReturn(page);

            Page<ClaimDto> result = service.getClaimsByStatuses(storeId, statuses, pageable);

            assertThat(result.getTotalElements()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("getClaim")
    class GetClaim {

        @Test
        @DisplayName("should return single claim by ID")
        void shouldReturnSingleClaim() {
            TrendyolClaim claim = createTestClaim("CLM-100", "WaitingInAction");
            when(claimRepository.findByStoreIdAndClaimId(storeId, "CLM-100"))
                    .thenReturn(Optional.of(claim));

            ClaimDto result = service.getClaim(storeId, "CLM-100");

            assertThat(result.getClaimId()).isEqualTo("CLM-100");
            assertThat(result.getStatus()).isEqualTo("WaitingInAction");
        }

        @Test
        @DisplayName("should throw exception when claim not found")
        void shouldThrowWhenClaimNotFound() {
            when(claimRepository.findByStoreIdAndClaimId(storeId, "NONEXISTENT"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getClaim(storeId, "NONEXISTENT"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Claim not found");
        }
    }

    @Nested
    @DisplayName("getStats")
    class GetStats {

        @Test
        @DisplayName("should aggregate claims statistics correctly")
        void shouldAggregateClaimsStats() {
            when(claimRepository.countByStoreId(storeId)).thenReturn(50L);
            when(claimRepository.countByStoreIdAndStatus(storeId, "WaitingInAction")).thenReturn(10L);
            when(claimRepository.countByStoreIdAndStatus(storeId, "Accepted")).thenReturn(25L);
            when(claimRepository.countByStoreIdAndStatus(storeId, "Rejected")).thenReturn(8L);
            when(claimRepository.countByStoreIdAndStatus(storeId, "Unresolved")).thenReturn(7L);

            ClaimsStatsDto stats = service.getStats(storeId);

            assertThat(stats.getTotalClaims()).isEqualTo(50);
            assertThat(stats.getPendingClaims()).isEqualTo(10);
            assertThat(stats.getAcceptedClaims()).isEqualTo(25);
            assertThat(stats.getRejectedClaims()).isEqualTo(8);
            assertThat(stats.getUnresolvedClaims()).isEqualTo(7);
        }

        @Test
        @DisplayName("should return zero stats when no claims exist")
        void shouldReturnZeroStats() {
            when(claimRepository.countByStoreId(storeId)).thenReturn(0L);
            when(claimRepository.countByStoreIdAndStatus(eq(storeId), anyString())).thenReturn(0L);

            ClaimsStatsDto stats = service.getStats(storeId);

            assertThat(stats.getTotalClaims()).isZero();
            assertThat(stats.getPendingClaims()).isZero();
            assertThat(stats.getAcceptedClaims()).isZero();
        }
    }

    @Nested
    @DisplayName("getClaimIssueReasons")
    class GetClaimIssueReasons {

        @Test
        @DisplayName("should return dynamic reasons from Trendyol API")
        void shouldReturnDynamicReasonsFromApi() throws Exception {
            // Setup: provide a Trendyol store
            when(storeRepository.findByMarketplaceIgnoreCase("trendyol"))
                    .thenReturn(List.of(testStore));

            // Mock API response
            String apiResponse = """
                    {
                      "content": [
                        {"id": 1, "name": "İade gelen ürün sahte"},
                        {"id": 51, "name": "İade gelen ürün kullanılmış"},
                        {"id": 9999, "name": "Yeni Sebep"}
                      ]
                    }
                    """;

            when(restTemplate.exchange(
                    contains("claim-issue-reasons"),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(ResponseEntity.ok(apiResponse));

            List<ClaimIssueReasonDto> reasons = service.getClaimIssueReasons();

            assertThat(reasons).hasSize(3);
            assertThat(reasons.get(0).getId()).isEqualTo(1);
            assertThat(reasons.get(0).isRequiresFile()).isTrue(); // Known ID
            assertThat(reasons.get(2).getId()).isEqualTo(9999);
            assertThat(reasons.get(2).getName()).isEqualTo("Yeni Sebep");
            assertThat(reasons.get(2).isRequiresFile()).isFalse(); // Unknown ID defaults to false
        }

        @Test
        @DisplayName("should return fallback when no Trendyol credentials found")
        void shouldReturnFallbackWhenNoCredentials() {
            when(storeRepository.findByMarketplaceIgnoreCase("trendyol"))
                    .thenReturn(Collections.emptyList());

            List<ClaimIssueReasonDto> reasons = service.getClaimIssueReasons();

            assertThat(reasons).hasSize(13);
            // Verify it's the fallback list
            assertThat(reasons.stream().map(ClaimIssueReasonDto::getId))
                    .contains(1, 51, 201, 251, 401, 101, 151, 301, 351, 1651, 1701, 1751, 451);
            verify(restTemplate, never()).exchange(anyString(), any(), any(HttpEntity.class), eq(String.class));
        }

        @Test
        @DisplayName("should return fallback when API throws exception")
        void shouldReturnFallbackOnApiError() {
            when(storeRepository.findByMarketplaceIgnoreCase("trendyol"))
                    .thenReturn(List.of(testStore));
            when(restTemplate.exchange(
                    anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)
            )).thenThrow(new RuntimeException("API unavailable"));

            List<ClaimIssueReasonDto> reasons = service.getClaimIssueReasons();

            assertThat(reasons).hasSize(13); // Fallback list
        }

        @Test
        @DisplayName("unknown reason ID should default requiresFile to false")
        void unknownReasonIdShouldDefaultRequiresFileFalse() throws Exception {
            when(storeRepository.findByMarketplaceIgnoreCase("trendyol"))
                    .thenReturn(List.of(testStore));

            String apiResponse = """
                    {
                      "content": [
                        {"id": 77777, "name": "Bilinmeyen Sebep"}
                      ]
                    }
                    """;

            when(restTemplate.exchange(
                    contains("claim-issue-reasons"),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(ResponseEntity.ok(apiResponse));

            List<ClaimIssueReasonDto> reasons = service.getClaimIssueReasons();

            assertThat(reasons).hasSize(1);
            assertThat(reasons.get(0).isRequiresFile()).isFalse();
        }
    }

    @Nested
    @DisplayName("approveClaim")
    class ApproveClaim {

        @Test
        @DisplayName("should reject approval when claim status is not WaitingInAction")
        void shouldRejectApprovalForWrongStatus() {
            TrendyolClaim claim = createTestClaim("CLM-200", "Accepted");

            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(claimRepository.findByStoreIdAndClaimId(storeId, "CLM-200"))
                    .thenReturn(Optional.of(claim));

            ClaimActionResponse response = service.approveClaim(storeId, "CLM-200", List.of("item1"));

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).contains("WaitingInAction");
        }

        @Test
        @DisplayName("should fail when store not found")
        void shouldFailWhenStoreNotFound() {
            when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.approveClaim(storeId, "CLM-300", List.of("item1")))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Store not found");
        }
    }

    @Nested
    @DisplayName("rejectClaim")
    class RejectClaim {

        @Test
        @DisplayName("should reject rejection when claim status is not WaitingInAction")
        void shouldRejectRejectionForWrongStatus() {
            TrendyolClaim claim = createTestClaim("CLM-400", "Accepted");

            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(claimRepository.findByStoreIdAndClaimId(storeId, "CLM-400"))
                    .thenReturn(Optional.of(claim));

            ClaimActionResponse response = service.rejectClaim(storeId, "CLM-400", 1, List.of("item1"), "test");

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).contains("WaitingInAction");
        }
    }

    @Nested
    @DisplayName("getClaimItemAudit")
    class GetClaimItemAudit {

        @Test
        @DisplayName("should return audit trail from Trendyol API")
        void shouldReturnAuditTrail() throws Exception {
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));

            String apiResponse = """
                    {
                      "content": [
                        {
                          "claimId": "CLM-001",
                          "claimItemId": "ITEM-001",
                          "previousStatus": "Created",
                          "newStatus": "WaitingInAction",
                          "userInfoDocument": {
                            "id": "sys-1",
                            "app": "Trendyol",
                            "user": null
                          },
                          "date": 1700000000000
                        },
                        {
                          "claimId": "CLM-001",
                          "claimItemId": "ITEM-001",
                          "previousStatus": "WaitingInAction",
                          "newStatus": "Accepted",
                          "userInfoDocument": {
                            "id": "seller-1",
                            "app": "SellerCenter",
                            "user": "seller@test.com"
                          },
                          "date": 1700100000000
                        }
                      ]
                    }
                    """;

            when(restTemplate.exchange(
                    contains("/claims/items/ITEM-001/audit"),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(ResponseEntity.ok(apiResponse));

            List<ClaimItemAuditDto> audit = service.getClaimItemAudit(storeId, "ITEM-001");

            assertThat(audit).hasSize(2);
            assertThat(audit.get(0).getPreviousStatus()).isEqualTo("Created");
            assertThat(audit.get(0).getNewStatus()).isEqualTo("WaitingInAction");
            assertThat(audit.get(0).getExecutorApp()).isEqualTo("Trendyol");
            assertThat(audit.get(1).getNewStatus()).isEqualTo("Accepted");
            assertThat(audit.get(1).getExecutorUser()).isEqualTo("seller@test.com");
        }

        @Test
        @DisplayName("should throw when store not found")
        void shouldThrowWhenStoreNotFound() {
            when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getClaimItemAudit(storeId, "ITEM-001"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Store not found");
        }

        @Test
        @DisplayName("should throw when no Trendyol credentials")
        void shouldThrowWhenNoCredentials() {
            Store storeWithoutCreds = Store.builder()
                    .id(storeId)
                    .storeName("No Creds Store")
                    .marketplace("other")
                    .build();
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(storeWithoutCreds));

            assertThatThrownBy(() -> service.getClaimItemAudit(storeId, "ITEM-001"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("credentials not found");
        }

        @Test
        @DisplayName("should throw when API returns error")
        void shouldThrowOnApiError() {
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(restTemplate.exchange(
                    anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)
            )).thenThrow(new RuntimeException("Connection refused"));

            assertThatThrownBy(() -> service.getClaimItemAudit(storeId, "ITEM-001"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to fetch");
        }
    }

    // Helper methods

    private TrendyolClaim createTestClaim(String claimId, String status) {
        ClaimItem item = ClaimItem.builder()
                .claimItemId("item-" + claimId)
                .barcode("BARCODE-001")
                .productName("Test Product")
                .price(new BigDecimal("199.99"))
                .quantity(BigDecimal.ONE)
                .status(status)
                .build();

        return TrendyolClaim.builder()
                .store(testStore)
                .claimId(claimId)
                .orderNumber("TY-ORDER-" + claimId)
                .customerFirstName("Test")
                .customerLastName("Customer")
                .claimDate(LocalDateTime.now())
                .status(status)
                .items(List.of(item))
                .lastModifiedDate(LocalDateTime.now())
                .syncedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
