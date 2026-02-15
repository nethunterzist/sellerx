package com.ecommerce.sellerx.alerts;

import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.common.TestDataBuilder;
import com.ecommerce.sellerx.email.EmailService;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.returns.ReturnRecordRepository;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("AlertEngine")
class AlertEngineTest extends BaseUnitTest {

    @Mock
    private AlertRuleRepository alertRuleRepository;

    @Mock
    private AlertHistoryRepository alertHistoryRepository;

    @Mock
    private ReturnRecordRepository returnRecordRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AlertEngine alertEngine;

    private User testUser;
    private Store testStore;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetSequence();
        alertEngine = new AlertEngine(alertRuleRepository, alertHistoryRepository, returnRecordRepository, emailService, eventPublisher);

        testUser = TestDataBuilder.user().build();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        testStore = TestDataBuilder.completedStore(testUser).build();
        testStore.setId(UUID.randomUUID());
    }

    @Nested
    @DisplayName("checkStockAlertForProduct")
    class CheckStockAlertForProduct {

        @Test
        @DisplayName("should trigger alert when stock is below threshold")
        void shouldTriggerWhenStockBelowThreshold() {
            // Given
            TrendyolProduct product = buildProduct("BARCODE-1", "Test Product", 5);

            AlertRule rule = buildStockRule(AlertConditionType.BELOW, BigDecimal.TEN, null, null);
            when(alertRuleRepository.findActiveRulesByType(testUser.getId(), testStore.getId(), AlertType.STOCK))
                    .thenReturn(List.of(rule));
            when(alertHistoryRepository.save(any(AlertHistory.class))).thenAnswer(i -> {
                AlertHistory h = i.getArgument(0);
                h.setId(UUID.randomUUID());
                return h;
            });
            when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(i -> i.getArgument(0));

            // When
            alertEngine.checkStockAlertForProduct(testStore, product);

            // Then
            verify(alertHistoryRepository).save(any(AlertHistory.class));
            verify(alertRuleRepository).save(rule); // rule trigger recorded
        }

        @Test
        @DisplayName("should not trigger alert when stock is above threshold")
        void shouldNotTriggerWhenStockAboveThreshold() {
            // Given
            TrendyolProduct product = buildProduct("BARCODE-1", "Test Product", 15);

            AlertRule rule = buildStockRule(AlertConditionType.BELOW, BigDecimal.TEN, null, null);
            when(alertRuleRepository.findActiveRulesByType(testUser.getId(), testStore.getId(), AlertType.STOCK))
                    .thenReturn(List.of(rule));

            // When
            alertEngine.checkStockAlertForProduct(testStore, product);

            // Then
            verify(alertHistoryRepository, never()).save(any(AlertHistory.class));
        }

        @Test
        @DisplayName("should trigger alert when stock is zero with ZERO condition")
        void shouldTriggerWhenStockIsZero() {
            // Given
            TrendyolProduct product = buildProduct("BARCODE-1", "Test Product", 0);

            AlertRule rule = buildStockRule(AlertConditionType.ZERO, null, null, null);
            when(alertRuleRepository.findActiveRulesByType(testUser.getId(), testStore.getId(), AlertType.STOCK))
                    .thenReturn(List.of(rule));
            when(alertHistoryRepository.save(any(AlertHistory.class))).thenAnswer(i -> {
                AlertHistory h = i.getArgument(0);
                h.setId(UUID.randomUUID());
                return h;
            });
            when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(i -> i.getArgument(0));

            // When
            alertEngine.checkStockAlertForProduct(testStore, product);

            // Then
            verify(alertHistoryRepository).save(any(AlertHistory.class));
        }

        @Test
        @DisplayName("should not trigger when stock is non-zero with ZERO condition")
        void shouldNotTriggerWhenStockNonZeroWithZeroCondition() {
            // Given
            TrendyolProduct product = buildProduct("BARCODE-1", "Test Product", 3);

            AlertRule rule = buildStockRule(AlertConditionType.ZERO, null, null, null);
            when(alertRuleRepository.findActiveRulesByType(testUser.getId(), testStore.getId(), AlertType.STOCK))
                    .thenReturn(List.of(rule));

            // When
            alertEngine.checkStockAlertForProduct(testStore, product);

            // Then
            verify(alertHistoryRepository, never()).save(any(AlertHistory.class));
        }

        @Test
        @DisplayName("should skip product when barcode filter does not match")
        void shouldSkipWhenBarcodeDoesNotMatch() {
            // Given
            TrendyolProduct product = buildProduct("BARCODE-OTHER", "Test Product", 0);

            AlertRule rule = buildStockRule(AlertConditionType.ZERO, null, "BARCODE-1", null);
            when(alertRuleRepository.findActiveRulesByType(testUser.getId(), testStore.getId(), AlertType.STOCK))
                    .thenReturn(List.of(rule));

            // When
            alertEngine.checkStockAlertForProduct(testStore, product);

            // Then
            verify(alertHistoryRepository, never()).save(any(AlertHistory.class));
        }

        @Test
        @DisplayName("should match product when barcode filter matches")
        void shouldMatchWhenBarcodeMatches() {
            // Given
            TrendyolProduct product = buildProduct("BARCODE-1", "Test Product", 0);

            AlertRule rule = buildStockRule(AlertConditionType.ZERO, null, "BARCODE-1", null);
            when(alertRuleRepository.findActiveRulesByType(testUser.getId(), testStore.getId(), AlertType.STOCK))
                    .thenReturn(List.of(rule));
            when(alertHistoryRepository.save(any(AlertHistory.class))).thenAnswer(i -> {
                AlertHistory h = i.getArgument(0);
                h.setId(UUID.randomUUID());
                return h;
            });
            when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(i -> i.getArgument(0));

            // When
            alertEngine.checkStockAlertForProduct(testStore, product);

            // Then
            verify(alertHistoryRepository).save(any(AlertHistory.class));
        }

        @Test
        @DisplayName("should skip when rule is in cooldown")
        void shouldSkipWhenRuleInCooldown() {
            // Given
            TrendyolProduct product = buildProduct("BARCODE-1", "Test Product", 0);

            AlertRule rule = buildStockRule(AlertConditionType.ZERO, null, null, null);
            rule.setLastTriggeredAt(LocalDateTime.now().minusMinutes(10)); // recently triggered
            rule.setCooldownMinutes(60); // 60 min cooldown

            when(alertRuleRepository.findActiveRulesByType(testUser.getId(), testStore.getId(), AlertType.STOCK))
                    .thenReturn(List.of(rule));

            // When
            alertEngine.checkStockAlertForProduct(testStore, product);

            // Then
            verify(alertHistoryRepository, never()).save(any(AlertHistory.class));
        }

        @Test
        @DisplayName("should filter by category when category filter is set")
        void shouldFilterByCategory() {
            // Given
            TrendyolProduct product = buildProduct("BARCODE-1", "Test Product", 0);
            product.setCategoryName("Electronics");

            AlertRule rule = buildStockRule(AlertConditionType.ZERO, null, null, "Fashion");
            when(alertRuleRepository.findActiveRulesByType(testUser.getId(), testStore.getId(), AlertType.STOCK))
                    .thenReturn(List.of(rule));

            // When
            alertEngine.checkStockAlertForProduct(testStore, product);

            // Then
            verify(alertHistoryRepository, never()).save(any(AlertHistory.class));
        }

        @Test
        @DisplayName("should match category filter case-insensitively with contains")
        void shouldMatchCategoryCaseInsensitive() {
            // Given
            TrendyolProduct product = buildProduct("BARCODE-1", "Test Product", 0);
            product.setCategoryName("Fashion & Accessories");

            AlertRule rule = buildStockRule(AlertConditionType.ZERO, null, null, "fashion");
            when(alertRuleRepository.findActiveRulesByType(testUser.getId(), testStore.getId(), AlertType.STOCK))
                    .thenReturn(List.of(rule));
            when(alertHistoryRepository.save(any(AlertHistory.class))).thenAnswer(i -> {
                AlertHistory h = i.getArgument(0);
                h.setId(UUID.randomUUID());
                return h;
            });
            when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(i -> i.getArgument(0));

            // When
            alertEngine.checkStockAlertForProduct(testStore, product);

            // Then
            verify(alertHistoryRepository).save(any(AlertHistory.class));
        }

        @Test
        @DisplayName("should handle null stock quantity as zero")
        void shouldHandleNullStockAsZero() {
            // Given
            TrendyolProduct product = buildProduct("BARCODE-1", "Test Product", null);

            AlertRule rule = buildStockRule(AlertConditionType.ZERO, null, null, null);
            when(alertRuleRepository.findActiveRulesByType(testUser.getId(), testStore.getId(), AlertType.STOCK))
                    .thenReturn(List.of(rule));
            when(alertHistoryRepository.save(any(AlertHistory.class))).thenAnswer(i -> {
                AlertHistory h = i.getArgument(0);
                h.setId(UUID.randomUUID());
                return h;
            });
            when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(i -> i.getArgument(0));

            // When
            alertEngine.checkStockAlertForProduct(testStore, product);

            // Then
            verify(alertHistoryRepository).save(any(AlertHistory.class));
        }

        @Test
        @DisplayName("should not process when no active stock rules exist")
        void shouldNotProcessWhenNoRules() {
            // Given
            TrendyolProduct product = buildProduct("BARCODE-1", "Test Product", 0);
            when(alertRuleRepository.findActiveRulesByType(testUser.getId(), testStore.getId(), AlertType.STOCK))
                    .thenReturn(List.of());

            // When
            alertEngine.checkStockAlertForProduct(testStore, product);

            // Then
            verify(alertHistoryRepository, never()).save(any(AlertHistory.class));
        }
    }

    @Nested
    @DisplayName("createAlert")
    class CreateAlert {

        @Test
        @DisplayName("should create alert and update rule trigger info")
        void shouldCreateAlertAndUpdateTriggerInfo() {
            // Given
            AlertRule rule = buildStockRule(AlertConditionType.BELOW, BigDecimal.TEN, null, null);
            rule.setTriggerCount(2);

            Map<String, Object> data = Map.of("productId", UUID.randomUUID());

            when(alertHistoryRepository.save(any(AlertHistory.class))).thenAnswer(i -> {
                AlertHistory h = i.getArgument(0);
                h.setId(UUID.randomUUID());
                return h;
            });
            when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(i -> i.getArgument(0));

            // When
            AlertHistory result = alertEngine.createAlert(
                    rule, testUser, testStore, "Test Title", "Test Message",
                    AlertSeverity.MEDIUM, data);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("Test Title");
            assertThat(result.getSeverity()).isEqualTo(AlertSeverity.MEDIUM);
            assertThat(rule.getTriggerCount()).isEqualTo(3);
            assertThat(rule.getLastTriggeredAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("createSystemAlert")
    class CreateSystemAlert {

        @Test
        @DisplayName("should create system alert without email")
        void shouldCreateSystemAlertWithoutEmail() {
            // Given
            Map<String, Object> data = Map.of("key", "value");
            when(alertHistoryRepository.save(any(AlertHistory.class))).thenAnswer(i -> {
                AlertHistory h = i.getArgument(0);
                h.setId(UUID.randomUUID());
                return h;
            });

            // When
            AlertHistory result = alertEngine.createSystemAlert(
                    testUser, testStore, "System Alert", "Something happened",
                    AlertSeverity.HIGH, data);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getAlertType()).isEqualTo(AlertType.SYSTEM);
            verify(emailService, never()).sendAlertEmail(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("should create system alert with email when requested")
        void shouldCreateSystemAlertWithEmail() {
            // Given
            Map<String, Object> data = Map.of("key", "value");
            when(alertHistoryRepository.save(any(AlertHistory.class))).thenAnswer(i -> {
                AlertHistory h = i.getArgument(0);
                h.setId(UUID.randomUUID());
                return h;
            });

            // When
            AlertHistory result = alertEngine.createSystemAlert(
                    testUser, testStore, "System Alert", "Something happened",
                    AlertSeverity.HIGH, data, true);

            // Then
            assertThat(result).isNotNull();
            verify(emailService).sendAlertEmail(
                    eq("test@example.com"),
                    eq("System Alert"),
                    eq("Something happened"),
                    eq(testStore.getStoreName()),
                    eq("HIGH"),
                    eq("SYSTEM")
            );
        }
    }

    // Helper methods

    private TrendyolProduct buildProduct(String barcode, String title, Integer quantity) {
        TrendyolProduct product = TestDataBuilder.product(testStore).build();
        product.setBarcode(barcode);
        product.setTitle(title);
        product.setTrendyolQuantity(quantity);
        return product;
    }

    private AlertRule buildStockRule(AlertConditionType conditionType, BigDecimal threshold,
                                     String barcode, String category) {
        return AlertRule.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .store(testStore)
                .name("Stock Rule")
                .alertType(AlertType.STOCK)
                .conditionType(conditionType)
                .threshold(threshold)
                .productBarcode(barcode)
                .categoryName(category)
                .active(true)
                .emailEnabled(false)
                .pushEnabled(false)
                .inAppEnabled(true)
                .cooldownMinutes(60)
                .triggerCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
