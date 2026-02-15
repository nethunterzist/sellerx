package com.ecommerce.sellerx.crosssell;

import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.common.TestDataBuilder;
import com.ecommerce.sellerx.qa.TrendyolQuestion;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CrossSellMatcher")
class CrossSellMatcherTest extends BaseUnitTest {

    private CrossSellMatcher matcher;
    private User testUser;
    private Store testStore;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetSequence();
        matcher = new CrossSellMatcher();

        testUser = TestDataBuilder.user().build();
        testUser.setId(1L);

        testStore = TestDataBuilder.completedStore(testUser).build();
        testStore.setId(UUID.randomUUID());
    }

    @Nested
    @DisplayName("ALL_QUESTIONS trigger")
    class AllQuestionsTrigger {

        @Test
        @DisplayName("should always match ALL_QUESTIONS trigger type")
        void shouldAlwaysMatch() {
            CrossSellRule rule = buildRule(TriggerType.ALL_QUESTIONS, Map.of());
            TrendyolQuestion question = buildQuestion("Bu ürün ne zaman kargoya verilir?", null, null);

            assertThat(matcher.matches(rule, question)).isTrue();
        }
    }

    @Nested
    @DisplayName("KEYWORD trigger")
    class KeywordTrigger {

        @Test
        @DisplayName("should match when keyword found in question text")
        void shouldMatchKeywordInQuestion() {
            CrossSellRule rule = buildRule(TriggerType.KEYWORD, Map.of("keywords", List.of("kargo", "teslimat")));
            TrendyolQuestion question = buildQuestion("Bu ürün kargo ile ne zaman gelir?", null, null);

            assertThat(matcher.matches(rule, question)).isTrue();
        }

        @Test
        @DisplayName("should match keyword case-insensitively with Turkish locale")
        void shouldMatchCaseInsensitive() {
            CrossSellRule rule = buildRule(TriggerType.KEYWORD, Map.of("keywords", List.of("KARGO")));
            TrendyolQuestion question = buildQuestion("kargo süresi nedir?", null, null);

            assertThat(matcher.matches(rule, question)).isTrue();
        }

        @Test
        @DisplayName("should match keyword found in product title")
        void shouldMatchKeywordInProductTitle() {
            CrossSellRule rule = buildRule(TriggerType.KEYWORD, Map.of("keywords", List.of("telefon")));
            TrendyolQuestion question = buildQuestion("Renk seçenekleri var mı?", null, "Samsung Telefon Kılıfı");

            assertThat(matcher.matches(rule, question)).isTrue();
        }

        @Test
        @DisplayName("should not match when no keywords found")
        void shouldNotMatchWhenNoKeywords() {
            CrossSellRule rule = buildRule(TriggerType.KEYWORD, Map.of("keywords", List.of("kargo", "teslimat")));
            TrendyolQuestion question = buildQuestion("Ürün garantisi var mı?", null, null);

            assertThat(matcher.matches(rule, question)).isFalse();
        }

        @Test
        @DisplayName("should not match when keywords list is empty")
        void shouldNotMatchEmptyKeywords() {
            CrossSellRule rule = buildRule(TriggerType.KEYWORD, Map.of("keywords", List.of()));
            TrendyolQuestion question = buildQuestion("herhangi bir soru", null, null);

            assertThat(matcher.matches(rule, question)).isFalse();
        }

        @Test
        @DisplayName("should not match when keywords key is missing")
        void shouldNotMatchMissingKeywords() {
            CrossSellRule rule = buildRule(TriggerType.KEYWORD, Map.of());
            TrendyolQuestion question = buildQuestion("herhangi bir soru", null, null);

            assertThat(matcher.matches(rule, question)).isFalse();
        }
    }

    @Nested
    @DisplayName("CATEGORY trigger")
    class CategoryTrigger {

        @Test
        @DisplayName("should match when category name found in product title")
        void shouldMatchCategory() {
            CrossSellRule rule = buildRule(TriggerType.CATEGORY, Map.of("categoryNames", List.of("Elektronik")));
            TrendyolQuestion question = buildQuestion("Pil ömrü nedir?", null, "Elektronik Saat");

            assertThat(matcher.matches(rule, question)).isTrue();
        }

        @Test
        @DisplayName("should not match when product title is null")
        void shouldNotMatchNullTitle() {
            CrossSellRule rule = buildRule(TriggerType.CATEGORY, Map.of("categoryNames", List.of("Elektronik")));
            TrendyolQuestion question = buildQuestion("Pil ömrü nedir?", null, null);

            assertThat(matcher.matches(rule, question)).isFalse();
        }

        @Test
        @DisplayName("should not match when category not in product title")
        void shouldNotMatchWrongCategory() {
            CrossSellRule rule = buildRule(TriggerType.CATEGORY, Map.of("categoryNames", List.of("Elektronik")));
            TrendyolQuestion question = buildQuestion("Beden seçimi nasıl?", null, "Yazlık Elbise");

            assertThat(matcher.matches(rule, question)).isFalse();
        }
    }

    @Nested
    @DisplayName("PRODUCT trigger")
    class ProductTrigger {

        @Test
        @DisplayName("should match when question barcode is in trigger barcodes")
        void shouldMatchBarcode() {
            CrossSellRule rule = buildRule(TriggerType.PRODUCT, Map.of("productBarcodes", List.of("ABC123", "DEF456")));
            TrendyolQuestion question = buildQuestion("Bu ürün orijinal mi?", "ABC123", "Test Ürün");

            assertThat(matcher.matches(rule, question)).isTrue();
        }

        @Test
        @DisplayName("should match barcode case-insensitively")
        void shouldMatchBarcodeCaseInsensitive() {
            CrossSellRule rule = buildRule(TriggerType.PRODUCT, Map.of("productBarcodes", List.of("abc123")));
            TrendyolQuestion question = buildQuestion("Bu ürün orijinal mi?", "ABC123", "Test Ürün");

            assertThat(matcher.matches(rule, question)).isTrue();
        }

        @Test
        @DisplayName("should not match when barcode not in list")
        void shouldNotMatchWrongBarcode() {
            CrossSellRule rule = buildRule(TriggerType.PRODUCT, Map.of("productBarcodes", List.of("ABC123")));
            TrendyolQuestion question = buildQuestion("Bu ürün orijinal mi?", "XYZ789", "Test Ürün");

            assertThat(matcher.matches(rule, question)).isFalse();
        }

        @Test
        @DisplayName("should not match when question has no barcode")
        void shouldNotMatchNullBarcode() {
            CrossSellRule rule = buildRule(TriggerType.PRODUCT, Map.of("productBarcodes", List.of("ABC123")));
            TrendyolQuestion question = buildQuestion("Bu ürün orijinal mi?", null, "Test Ürün");

            assertThat(matcher.matches(rule, question)).isFalse();
        }
    }

    @Nested
    @DisplayName("findMatchingRule")
    class FindMatchingRule {

        @Test
        @DisplayName("should return first matching rule by priority")
        void shouldReturnHighestPriorityMatch() {
            CrossSellRule lowPriority = buildRule(TriggerType.ALL_QUESTIONS, Map.of());
            lowPriority.setPriority(1);
            lowPriority.setName("Low Priority");

            CrossSellRule highPriority = buildRule(TriggerType.KEYWORD, Map.of("keywords", List.of("kargo")));
            highPriority.setPriority(10);
            highPriority.setName("High Priority");

            TrendyolQuestion question = buildQuestion("kargo ne zaman gelir?", null, null);

            // Rules already sorted by priority desc
            List<CrossSellRule> rules = List.of(highPriority, lowPriority);
            CrossSellRule result = matcher.findMatchingRule(rules, question);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("High Priority");
        }

        @Test
        @DisplayName("should return null when no rules match")
        void shouldReturnNullWhenNoMatch() {
            CrossSellRule rule = buildRule(TriggerType.KEYWORD, Map.of("keywords", List.of("kargo")));

            TrendyolQuestion question = buildQuestion("garanti süresi nedir?", null, null);

            CrossSellRule result = matcher.findMatchingRule(List.of(rule), question);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null for empty rules list")
        void shouldReturnNullForEmptyRules() {
            TrendyolQuestion question = buildQuestion("herhangi bir soru", null, null);

            CrossSellRule result = matcher.findMatchingRule(List.of(), question);
            assertThat(result).isNull();
        }
    }

    // ==================== Helpers ====================

    private CrossSellRule buildRule(TriggerType triggerType, Map<String, Object> triggerConditions) {
        return CrossSellRule.builder()
                .id(UUID.randomUUID())
                .store(testStore)
                .name("Test Rule")
                .triggerType(triggerType)
                .triggerConditions(triggerConditions)
                .recommendationType(RecommendationType.SPECIFIC_PRODUCTS)
                .priority(0)
                .maxProducts(3)
                .active(true)
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
