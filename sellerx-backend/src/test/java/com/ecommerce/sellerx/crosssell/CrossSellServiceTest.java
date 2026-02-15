package com.ecommerce.sellerx.crosssell;

import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.common.TestDataBuilder;
import com.ecommerce.sellerx.crosssell.dto.*;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import com.ecommerce.sellerx.qa.TrendyolQuestion;
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
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("CrossSellService")
class CrossSellServiceTest extends BaseUnitTest {

    @Mock private CrossSellRuleRepository ruleRepository;
    @Mock private CrossSellRuleProductRepository ruleProductRepository;
    @Mock private CrossSellSettingsRepository settingsRepository;
    @Mock private CrossSellAnalyticsRepository analyticsRepository;
    @Mock private CrossSellMatcher matcher;
    @Mock private TrendyolProductRepository productRepository;
    @Mock private StoreRepository storeRepository;

    private CrossSellService service;

    private User testUser;
    private Store testStore;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetSequence();
        service = new CrossSellService(
                ruleRepository, ruleProductRepository, settingsRepository,
                analyticsRepository, matcher, productRepository, storeRepository
        );

        testUser = TestDataBuilder.user().build();
        testUser.setId(1L);

        testStore = TestDataBuilder.completedStore(testUser).build();
        testStore.setId(UUID.randomUUID());
    }

    @Nested
    @DisplayName("getSettings")
    class GetSettings {

        @Test
        @DisplayName("should return default settings when none exist")
        void shouldReturnDefaultSettingsWhenNoneExist() {
            when(settingsRepository.findByStoreId(testStore.getId())).thenReturn(Optional.empty());

            CrossSellSettingsDto result = service.getSettings(testStore.getId());

            assertThat(result.getStoreId()).isEqualTo(testStore.getId());
            assertThat(result.getEnabled()).isFalse();
            assertThat(result.getDefaultMaxProducts()).isEqualTo(3);
            assertThat(result.getIncludeInAnswer()).isTrue();
        }

        @Test
        @DisplayName("should return stored settings when they exist")
        void shouldReturnStoredSettings() {
            CrossSellSettings settings = CrossSellSettings.builder()
                    .id(UUID.randomUUID())
                    .store(testStore)
                    .enabled(true)
                    .defaultMaxProducts(5)
                    .includeInAnswer(false)
                    .showProductImage(true)
                    .showProductPrice(false)
                    .build();
            when(settingsRepository.findByStoreId(testStore.getId())).thenReturn(Optional.of(settings));

            CrossSellSettingsDto result = service.getSettings(testStore.getId());

            assertThat(result.getEnabled()).isTrue();
            assertThat(result.getDefaultMaxProducts()).isEqualTo(5);
            assertThat(result.getIncludeInAnswer()).isFalse();
            assertThat(result.getShowProductPrice()).isFalse();
        }
    }

    @Nested
    @DisplayName("createRule")
    class CreateRule {

        @Test
        @DisplayName("should create rule with products")
        void shouldCreateRuleWithProducts() {
            when(storeRepository.findById(testStore.getId())).thenReturn(Optional.of(testStore));
            when(ruleRepository.save(any(CrossSellRule.class))).thenAnswer(i -> {
                CrossSellRule r = i.getArgument(0);
                r.setId(UUID.randomUUID());
                r.setCreatedAt(LocalDateTime.now());
                r.setUpdatedAt(LocalDateTime.now());
                return r;
            });
            when(ruleProductRepository.save(any(CrossSellRuleProduct.class))).thenAnswer(i -> {
                CrossSellRuleProduct p = i.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });

            CreateCrossSellRuleRequest request = CreateCrossSellRuleRequest.builder()
                    .name("Test Rule")
                    .triggerType(TriggerType.KEYWORD)
                    .triggerConditions(Map.of("keywords", List.of("kılıf")))
                    .recommendationType(RecommendationType.SPECIFIC_PRODUCTS)
                    .maxProducts(2)
                    .products(List.of(
                            CreateCrossSellRuleRequest.RuleProductRequest.builder()
                                    .productBarcode("BARCODE-1")
                                    .displayOrder(0)
                                    .build()
                    ))
                    .build();

            CrossSellRuleDto result = service.createRule(testStore.getId(), request);

            assertThat(result.getName()).isEqualTo("Test Rule");
            assertThat(result.getTriggerType()).isEqualTo(TriggerType.KEYWORD);
            assertThat(result.getMaxProducts()).isEqualTo(2);
            verify(ruleProductRepository).save(any(CrossSellRuleProduct.class));
        }

        @Test
        @DisplayName("should throw when store not found")
        void shouldThrowWhenStoreNotFound() {
            when(storeRepository.findById(testStore.getId())).thenReturn(Optional.empty());

            CreateCrossSellRuleRequest request = CreateCrossSellRuleRequest.builder()
                    .name("Test Rule")
                    .triggerType(TriggerType.ALL_QUESTIONS)
                    .recommendationType(RecommendationType.BESTSELLERS)
                    .build();

            assertThatThrownBy(() -> service.createRule(testStore.getId(), request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Mağaza bulunamadı");
        }
    }

    @Nested
    @DisplayName("getRecommendations")
    class GetRecommendations {

        @Test
        @DisplayName("should return empty list when cross-sell is disabled")
        void shouldReturnEmptyWhenDisabled() {
            TrendyolQuestion question = buildQuestion("test soru", "BARCODE-1", "Test Ürün");
            when(settingsRepository.findByStoreId(testStore.getId())).thenReturn(Optional.empty());

            List<CrossSellRecommendationDto> result = service.getRecommendations(question);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when no active rules exist")
        void shouldReturnEmptyWhenNoRules() {
            TrendyolQuestion question = buildQuestion("test soru", "BARCODE-1", "Test Ürün");
            CrossSellSettings settings = CrossSellSettings.builder()
                    .store(testStore).enabled(true).defaultMaxProducts(3)
                    .includeInAnswer(true).showProductImage(true).showProductPrice(true)
                    .build();
            when(settingsRepository.findByStoreId(testStore.getId())).thenReturn(Optional.of(settings));
            when(ruleRepository.findActiveRulesByStoreId(testStore.getId())).thenReturn(List.of());

            List<CrossSellRecommendationDto> result = service.getRecommendations(question);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when no rule matches")
        void shouldReturnEmptyWhenNoMatch() {
            TrendyolQuestion question = buildQuestion("test soru", "BARCODE-1", "Test Ürün");
            CrossSellSettings settings = CrossSellSettings.builder()
                    .store(testStore).enabled(true).defaultMaxProducts(3)
                    .includeInAnswer(true).showProductImage(true).showProductPrice(true)
                    .build();
            CrossSellRule rule = buildRule(TriggerType.KEYWORD, Map.of("keywords", List.of("kargo")));

            when(settingsRepository.findByStoreId(testStore.getId())).thenReturn(Optional.of(settings));
            when(ruleRepository.findActiveRulesByStoreId(testStore.getId())).thenReturn(List.of(rule));
            when(matcher.findMatchingRule(any(), any())).thenReturn(null);

            List<CrossSellRecommendationDto> result = service.getRecommendations(question);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return recommendations when rule matches with SPECIFIC_PRODUCTS")
        void shouldReturnRecommendationsForSpecificProducts() {
            TrendyolQuestion question = buildQuestion("kılıf önerisi var mı?", "BARCODE-Q", "Test Ürün");
            CrossSellSettings settings = CrossSellSettings.builder()
                    .store(testStore).enabled(true).defaultMaxProducts(3)
                    .includeInAnswer(true).showProductImage(true).showProductPrice(true)
                    .build();
            CrossSellRule rule = buildRule(TriggerType.KEYWORD, Map.of("keywords", List.of("kılıf")));
            rule.setRecommendationType(RecommendationType.SPECIFIC_PRODUCTS);
            rule.setRecommendationText("Bu ürünlerle birlikte kullanabilirsiniz:");

            CrossSellRuleProduct ruleProduct = CrossSellRuleProduct.builder()
                    .id(UUID.randomUUID())
                    .rule(rule)
                    .productBarcode("REC-BARCODE-1")
                    .displayOrder(0)
                    .build();

            TrendyolProduct recProduct = TestDataBuilder.product(testStore).build();
            recProduct.setBarcode("REC-BARCODE-1");
            recProduct.setTitle("Önerilen Ürün");
            recProduct.setSalePrice(new BigDecimal("49.99"));

            when(settingsRepository.findByStoreId(testStore.getId())).thenReturn(Optional.of(settings));
            when(ruleRepository.findActiveRulesByStoreId(testStore.getId())).thenReturn(List.of(rule));
            when(matcher.findMatchingRule(any(), any())).thenReturn(rule);
            when(ruleProductRepository.findByRuleIdOrderByDisplayOrderAsc(rule.getId())).thenReturn(List.of(ruleProduct));
            when(productRepository.findByStoreIdAndBarcodeIn(testStore.getId(), List.of("REC-BARCODE-1")))
                    .thenReturn(List.of(recProduct));
            when(analyticsRepository.save(any(CrossSellAnalytics.class))).thenAnswer(i -> {
                CrossSellAnalytics a = i.getArgument(0);
                a.setId(UUID.randomUUID());
                return a;
            });

            List<CrossSellRecommendationDto> result = service.getRecommendations(question);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getProductBarcode()).isEqualTo("REC-BARCODE-1");
            assertThat(result.get(0).getProductTitle()).isEqualTo("Önerilen Ürün");
            assertThat(result.get(0).getRecommendationText()).isEqualTo("Bu ürünlerle birlikte kullanabilirsiniz:");
            assertThat(result.get(0).getProductPrice()).isEqualTo(new BigDecimal("49.99"));
            verify(analyticsRepository).save(any(CrossSellAnalytics.class));
        }

        @Test
        @DisplayName("should limit recommendations to max_products")
        void shouldLimitToMaxProducts() {
            TrendyolQuestion question = buildQuestion("soru", "BARCODE-Q", "Test");
            CrossSellSettings settings = CrossSellSettings.builder()
                    .store(testStore).enabled(true).defaultMaxProducts(3)
                    .includeInAnswer(true).showProductImage(true).showProductPrice(true)
                    .build();
            CrossSellRule rule = buildRule(TriggerType.ALL_QUESTIONS, Map.of());
            rule.setRecommendationType(RecommendationType.SPECIFIC_PRODUCTS);
            rule.setMaxProducts(1);

            List<CrossSellRuleProduct> ruleProducts = List.of(
                    CrossSellRuleProduct.builder().id(UUID.randomUUID()).rule(rule)
                            .productBarcode("B1").displayOrder(0).build(),
                    CrossSellRuleProduct.builder().id(UUID.randomUUID()).rule(rule)
                            .productBarcode("B2").displayOrder(1).build()
            );

            TrendyolProduct p1 = TestDataBuilder.product(testStore).build();
            p1.setBarcode("B1");
            p1.setTitle("Product 1");

            when(settingsRepository.findByStoreId(testStore.getId())).thenReturn(Optional.of(settings));
            when(ruleRepository.findActiveRulesByStoreId(testStore.getId())).thenReturn(List.of(rule));
            when(matcher.findMatchingRule(any(), any())).thenReturn(rule);
            when(ruleProductRepository.findByRuleIdOrderByDisplayOrderAsc(rule.getId())).thenReturn(ruleProducts);
            when(productRepository.findByStoreIdAndBarcodeIn(eq(testStore.getId()), any())).thenReturn(List.of(p1));
            when(analyticsRepository.save(any(CrossSellAnalytics.class))).thenAnswer(i -> {
                CrossSellAnalytics a = i.getArgument(0);
                a.setId(UUID.randomUUID());
                return a;
            });

            List<CrossSellRecommendationDto> result = service.getRecommendations(question);

            // Should be limited to 1 (maxProducts)
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("should not include price when showProductPrice is false")
        void shouldNotIncludePriceWhenDisabled() {
            TrendyolQuestion question = buildQuestion("soru", "BARCODE-Q", "Test");
            CrossSellSettings settings = CrossSellSettings.builder()
                    .store(testStore).enabled(true).defaultMaxProducts(3)
                    .includeInAnswer(true).showProductImage(true).showProductPrice(false)
                    .build();
            CrossSellRule rule = buildRule(TriggerType.ALL_QUESTIONS, Map.of());
            rule.setRecommendationType(RecommendationType.SPECIFIC_PRODUCTS);

            CrossSellRuleProduct ruleProduct = CrossSellRuleProduct.builder()
                    .id(UUID.randomUUID()).rule(rule).productBarcode("B1").displayOrder(0).build();

            TrendyolProduct product = TestDataBuilder.product(testStore).build();
            product.setBarcode("B1");
            product.setSalePrice(new BigDecimal("199.99"));

            when(settingsRepository.findByStoreId(testStore.getId())).thenReturn(Optional.of(settings));
            when(ruleRepository.findActiveRulesByStoreId(testStore.getId())).thenReturn(List.of(rule));
            when(matcher.findMatchingRule(any(), any())).thenReturn(rule);
            when(ruleProductRepository.findByRuleIdOrderByDisplayOrderAsc(rule.getId())).thenReturn(List.of(ruleProduct));
            when(productRepository.findByStoreIdAndBarcodeIn(testStore.getId(), List.of("B1"))).thenReturn(List.of(product));
            when(analyticsRepository.save(any(CrossSellAnalytics.class))).thenAnswer(i -> {
                CrossSellAnalytics a = i.getArgument(0);
                a.setId(UUID.randomUUID());
                return a;
            });

            List<CrossSellRecommendationDto> result = service.getRecommendations(question);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getProductPrice()).isNull();
        }
    }

    @Nested
    @DisplayName("deleteRule")
    class DeleteRule {

        @Test
        @DisplayName("should delete rule by id")
        void shouldDeleteRule() {
            UUID ruleId = UUID.randomUUID();

            service.deleteRule(ruleId);

            verify(ruleRepository).deleteById(ruleId);
        }
    }

    @Nested
    @DisplayName("toggleRule")
    class ToggleRule {

        @Test
        @DisplayName("should toggle rule active status")
        void shouldToggleRuleActive() {
            CrossSellRule rule = buildRule(TriggerType.ALL_QUESTIONS, Map.of());
            rule.setActive(true);

            when(ruleRepository.findById(rule.getId())).thenReturn(Optional.of(rule));
            when(ruleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.toggleRule(rule.getId(), false);

            assertThat(rule.getActive()).isFalse();
            verify(ruleRepository).save(rule);
        }
    }

    // ==================== Helpers ====================

    private CrossSellRule buildRule(TriggerType triggerType, Map<String, Object> conditions) {
        return CrossSellRule.builder()
                .id(UUID.randomUUID())
                .store(testStore)
                .name("Test Rule")
                .triggerType(triggerType)
                .triggerConditions(conditions)
                .recommendationType(RecommendationType.SPECIFIC_PRODUCTS)
                .priority(0)
                .maxProducts(3)
                .active(true)
                .products(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private TrendyolQuestion buildQuestion(String customerQuestion, String barcode, String productTitle) {
        return TrendyolQuestion.builder()
                .id(UUID.randomUUID())
                .store(testStore)
                .questionId("Q-" + UUID.randomUUID())
                .customerQuestion(customerQuestion)
                .barcode(barcode)
                .productTitle(productTitle)
                .questionDate(LocalDateTime.now())
                .status("PENDING")
                .build();
    }
}
