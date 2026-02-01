package com.ecommerce.sellerx.alerts;

import com.ecommerce.sellerx.auth.AuthService;
import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.common.TestDataBuilder;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("AlertRuleService")
class AlertRuleServiceTest extends BaseUnitTest {

    @Mock
    private AlertRuleRepository alertRuleRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private AuthService authService;

    private AlertRuleService alertRuleService;

    private User testUser;
    private Store testStore;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetSequence();
        alertRuleService = new AlertRuleService(alertRuleRepository, storeRepository, authService);

        testUser = TestDataBuilder.user().build();
        testUser.setId(1L);

        testStore = TestDataBuilder.store(testUser).build();
        testStore.setId(UUID.randomUUID());
    }

    @Nested
    @DisplayName("getRulesForCurrentUser")
    class GetRulesForCurrentUser {

        @Test
        @DisplayName("should return all rules for authenticated user")
        void shouldReturnAllRulesForUser() {
            // Given
            when(authService.getCurrentUser()).thenReturn(testUser);

            AlertRule rule1 = buildAlertRule("Low Stock Alert", AlertType.STOCK, AlertConditionType.BELOW);
            AlertRule rule2 = buildAlertRule("High Profit Alert", AlertType.PROFIT, AlertConditionType.ABOVE);

            when(alertRuleRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId()))
                    .thenReturn(List.of(rule1, rule2));

            // When
            List<AlertRuleDto> result = alertRuleService.getRulesForCurrentUser();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("Low Stock Alert");
            assertThat(result.get(1).getName()).isEqualTo("High Profit Alert");
            verify(alertRuleRepository).findByUserIdOrderByCreatedAtDesc(testUser.getId());
        }

        @Test
        @DisplayName("should return empty list when no rules exist")
        void shouldReturnEmptyListWhenNoRules() {
            // Given
            when(authService.getCurrentUser()).thenReturn(testUser);
            when(alertRuleRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId()))
                    .thenReturn(List.of());

            // When
            List<AlertRuleDto> result = alertRuleService.getRulesForCurrentUser();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getRuleById")
    class GetRuleById {

        @Test
        @DisplayName("should return rule when found for user")
        void shouldReturnRuleWhenFound() {
            // Given
            UUID ruleId = UUID.randomUUID();
            when(authService.getCurrentUser()).thenReturn(testUser);

            AlertRule rule = buildAlertRule("Test Rule", AlertType.STOCK, AlertConditionType.BELOW);
            rule.setId(ruleId);
            when(alertRuleRepository.findByIdAndUserId(ruleId, testUser.getId()))
                    .thenReturn(Optional.of(rule));

            // When
            AlertRuleDto result = alertRuleService.getRuleById(ruleId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(ruleId);
            assertThat(result.getName()).isEqualTo("Test Rule");
        }

        @Test
        @DisplayName("should throw AlertRuleNotFoundException when rule not found")
        void shouldThrowWhenRuleNotFound() {
            // Given
            UUID ruleId = UUID.randomUUID();
            when(authService.getCurrentUser()).thenReturn(testUser);
            when(alertRuleRepository.findByIdAndUserId(ruleId, testUser.getId()))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> alertRuleService.getRuleById(ruleId))
                    .isInstanceOf(AlertRuleNotFoundException.class)
                    .hasMessageContaining(ruleId.toString());
        }
    }

    @Nested
    @DisplayName("createRule")
    class CreateRule {

        @Test
        @DisplayName("should create rule successfully without store")
        void shouldCreateRuleWithoutStore() {
            // Given
            when(authService.getCurrentUser()).thenReturn(testUser);
            when(alertRuleRepository.existsByUserIdAndName(testUser.getId(), "Stock Alert"))
                    .thenReturn(false);

            CreateAlertRuleRequest request = CreateAlertRuleRequest.builder()
                    .name("Stock Alert")
                    .alertType(AlertType.STOCK)
                    .conditionType(AlertConditionType.BELOW)
                    .threshold(BigDecimal.TEN)
                    .build();

            when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(invocation -> {
                AlertRule saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                saved.setCreatedAt(LocalDateTime.now());
                return saved;
            });

            // When
            AlertRuleDto result = alertRuleService.createRule(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Stock Alert");
            assertThat(result.getAlertType()).isEqualTo(AlertType.STOCK);
            assertThat(result.getActive()).isTrue();
            verify(alertRuleRepository).save(any(AlertRule.class));
        }

        @Test
        @DisplayName("should create rule with store association")
        void shouldCreateRuleWithStore() {
            // Given
            when(authService.getCurrentUser()).thenReturn(testUser);
            when(alertRuleRepository.existsByUserIdAndName(testUser.getId(), "Store Alert"))
                    .thenReturn(false);
            when(storeRepository.findById(testStore.getId()))
                    .thenReturn(Optional.of(testStore));

            CreateAlertRuleRequest request = CreateAlertRuleRequest.builder()
                    .name("Store Alert")
                    .storeId(testStore.getId())
                    .alertType(AlertType.STOCK)
                    .conditionType(AlertConditionType.ZERO)
                    .build();

            when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(invocation -> {
                AlertRule saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                saved.setCreatedAt(LocalDateTime.now());
                return saved;
            });

            // When
            AlertRuleDto result = alertRuleService.createRule(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStoreId()).isEqualTo(testStore.getId());
        }

        @Test
        @DisplayName("should throw when duplicate rule name exists")
        void shouldThrowWhenDuplicateName() {
            // Given
            when(authService.getCurrentUser()).thenReturn(testUser);
            when(alertRuleRepository.existsByUserIdAndName(testUser.getId(), "Existing Rule"))
                    .thenReturn(true);

            CreateAlertRuleRequest request = CreateAlertRuleRequest.builder()
                    .name("Existing Rule")
                    .alertType(AlertType.STOCK)
                    .conditionType(AlertConditionType.BELOW)
                    .threshold(BigDecimal.TEN)
                    .build();

            // When/Then
            assertThatThrownBy(() -> alertRuleService.createRule(request))
                    .isInstanceOf(AlertRuleValidationException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("should throw when BELOW condition has no threshold")
        void shouldThrowWhenBelowConditionMissingThreshold() {
            // Given
            when(authService.getCurrentUser()).thenReturn(testUser);
            when(alertRuleRepository.existsByUserIdAndName(any(), any())).thenReturn(false);

            CreateAlertRuleRequest request = CreateAlertRuleRequest.builder()
                    .name("No Threshold Rule")
                    .alertType(AlertType.STOCK)
                    .conditionType(AlertConditionType.BELOW)
                    .threshold(null)
                    .build();

            // When/Then
            assertThatThrownBy(() -> alertRuleService.createRule(request))
                    .isInstanceOf(AlertRuleValidationException.class)
                    .hasMessageContaining("Threshold is required");
        }

        @Test
        @DisplayName("should throw when ABOVE condition has no threshold")
        void shouldThrowWhenAboveConditionMissingThreshold() {
            // Given
            when(authService.getCurrentUser()).thenReturn(testUser);
            when(alertRuleRepository.existsByUserIdAndName(any(), any())).thenReturn(false);

            CreateAlertRuleRequest request = CreateAlertRuleRequest.builder()
                    .name("No Threshold Rule")
                    .alertType(AlertType.PROFIT)
                    .conditionType(AlertConditionType.ABOVE)
                    .threshold(null)
                    .build();

            // When/Then
            assertThatThrownBy(() -> alertRuleService.createRule(request))
                    .isInstanceOf(AlertRuleValidationException.class)
                    .hasMessageContaining("Threshold is required");
        }

        @Test
        @DisplayName("should allow ZERO condition without threshold")
        void shouldAllowZeroConditionWithoutThreshold() {
            // Given
            when(authService.getCurrentUser()).thenReturn(testUser);
            when(alertRuleRepository.existsByUserIdAndName(any(), any())).thenReturn(false);

            CreateAlertRuleRequest request = CreateAlertRuleRequest.builder()
                    .name("Zero Stock Alert")
                    .alertType(AlertType.STOCK)
                    .conditionType(AlertConditionType.ZERO)
                    .threshold(null)
                    .build();

            when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(invocation -> {
                AlertRule saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                saved.setCreatedAt(LocalDateTime.now());
                return saved;
            });

            // When
            AlertRuleDto result = alertRuleService.createRule(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getConditionType()).isEqualTo(AlertConditionType.ZERO);
        }

        @Test
        @DisplayName("should throw when store not found")
        void shouldThrowWhenStoreNotFound() {
            // Given
            UUID invalidStoreId = UUID.randomUUID();
            when(authService.getCurrentUser()).thenReturn(testUser);
            when(alertRuleRepository.existsByUserIdAndName(any(), any())).thenReturn(false);
            when(storeRepository.findById(invalidStoreId)).thenReturn(Optional.empty());

            CreateAlertRuleRequest request = CreateAlertRuleRequest.builder()
                    .name("Invalid Store Rule")
                    .storeId(invalidStoreId)
                    .alertType(AlertType.STOCK)
                    .conditionType(AlertConditionType.ZERO)
                    .build();

            // When/Then
            assertThatThrownBy(() -> alertRuleService.createRule(request))
                    .isInstanceOf(AlertRuleValidationException.class)
                    .hasMessageContaining("Store not found");
        }
    }

    @Nested
    @DisplayName("toggleRuleActive")
    class ToggleRuleActive {

        @Test
        @DisplayName("should toggle active rule to inactive")
        void shouldToggleActiveToInactive() {
            // Given
            UUID ruleId = UUID.randomUUID();
            when(authService.getCurrentUser()).thenReturn(testUser);

            AlertRule rule = buildAlertRule("Test Rule", AlertType.STOCK, AlertConditionType.BELOW);
            rule.setId(ruleId);
            rule.setActive(true);

            when(alertRuleRepository.findByIdAndUserId(ruleId, testUser.getId()))
                    .thenReturn(Optional.of(rule));
            when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(i -> i.getArgument(0));

            // When
            AlertRuleDto result = alertRuleService.toggleRuleActive(ruleId);

            // Then
            assertThat(result.getActive()).isFalse();
        }

        @Test
        @DisplayName("should toggle inactive rule to active")
        void shouldToggleInactiveToActive() {
            // Given
            UUID ruleId = UUID.randomUUID();
            when(authService.getCurrentUser()).thenReturn(testUser);

            AlertRule rule = buildAlertRule("Test Rule", AlertType.STOCK, AlertConditionType.BELOW);
            rule.setId(ruleId);
            rule.setActive(false);

            when(alertRuleRepository.findByIdAndUserId(ruleId, testUser.getId()))
                    .thenReturn(Optional.of(rule));
            when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(i -> i.getArgument(0));

            // When
            AlertRuleDto result = alertRuleService.toggleRuleActive(ruleId);

            // Then
            assertThat(result.getActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("deleteRule")
    class DeleteRule {

        @Test
        @DisplayName("should delete rule successfully")
        void shouldDeleteRule() {
            // Given
            UUID ruleId = UUID.randomUUID();
            when(authService.getCurrentUser()).thenReturn(testUser);

            AlertRule rule = buildAlertRule("Delete Me", AlertType.STOCK, AlertConditionType.BELOW);
            rule.setId(ruleId);

            when(alertRuleRepository.findByIdAndUserId(ruleId, testUser.getId()))
                    .thenReturn(Optional.of(rule));

            // When
            alertRuleService.deleteRule(ruleId);

            // Then
            verify(alertRuleRepository).delete(rule);
        }

        @Test
        @DisplayName("should throw when deleting non-existent rule")
        void shouldThrowWhenDeletingNonExistent() {
            // Given
            UUID ruleId = UUID.randomUUID();
            when(authService.getCurrentUser()).thenReturn(testUser);
            when(alertRuleRepository.findByIdAndUserId(ruleId, testUser.getId()))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> alertRuleService.deleteRule(ruleId))
                    .isInstanceOf(AlertRuleNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getRuleCount and getActiveRuleCount")
    class RuleCounts {

        @Test
        @DisplayName("should return total rule count")
        void shouldReturnTotalRuleCount() {
            when(authService.getCurrentUser()).thenReturn(testUser);
            when(alertRuleRepository.countByUserId(testUser.getId())).thenReturn(5L);

            assertThat(alertRuleService.getRuleCount()).isEqualTo(5L);
        }

        @Test
        @DisplayName("should return active rule count")
        void shouldReturnActiveRuleCount() {
            when(authService.getCurrentUser()).thenReturn(testUser);
            when(alertRuleRepository.countByUserIdAndActive(testUser.getId(), true)).thenReturn(3L);

            assertThat(alertRuleService.getActiveRuleCount()).isEqualTo(3L);
        }
    }

    // Helper method
    private AlertRule buildAlertRule(String name, AlertType type, AlertConditionType conditionType) {
        return AlertRule.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .store(testStore)
                .name(name)
                .alertType(type)
                .conditionType(conditionType)
                .threshold(BigDecimal.TEN)
                .active(true)
                .emailEnabled(true)
                .pushEnabled(false)
                .inAppEnabled(true)
                .cooldownMinutes(60)
                .triggerCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
