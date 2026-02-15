package com.ecommerce.sellerx.crosssell;

import com.ecommerce.sellerx.crosssell.dto.*;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import com.ecommerce.sellerx.qa.TrendyolQuestion;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrossSellService {

    private final CrossSellRuleRepository ruleRepository;
    private final CrossSellRuleProductRepository ruleProductRepository;
    private final CrossSellSettingsRepository settingsRepository;
    private final CrossSellAnalyticsRepository analyticsRepository;
    private final CrossSellMatcher matcher;
    private final TrendyolProductRepository productRepository;
    private final StoreRepository storeRepository;

    // ==================== Settings ====================

    public CrossSellSettingsDto getSettings(UUID storeId) {
        CrossSellSettings settings = settingsRepository.findByStoreId(storeId)
                .orElse(null);
        if (settings == null) {
            return CrossSellSettingsDto.builder()
                    .storeId(storeId)
                    .enabled(false)
                    .defaultMaxProducts(3)
                    .includeInAnswer(true)
                    .showProductImage(true)
                    .showProductPrice(true)
                    .build();
        }
        return toSettingsDto(settings);
    }

    @Transactional
    public CrossSellSettingsDto updateSettings(UUID storeId, UpdateCrossSellSettingsRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Mağaza bulunamadı: " + storeId));

        CrossSellSettings settings = settingsRepository.findByStoreId(storeId)
                .orElseGet(() -> CrossSellSettings.builder().store(store).build());

        if (request.getEnabled() != null) settings.setEnabled(request.getEnabled());
        if (request.getDefaultMaxProducts() != null) settings.setDefaultMaxProducts(request.getDefaultMaxProducts());
        if (request.getIncludeInAnswer() != null) settings.setIncludeInAnswer(request.getIncludeInAnswer());
        if (request.getShowProductImage() != null) settings.setShowProductImage(request.getShowProductImage());
        if (request.getShowProductPrice() != null) settings.setShowProductPrice(request.getShowProductPrice());

        settings = settingsRepository.save(settings);
        log.info("Cross-sell settings updated for store {}", storeId);
        return toSettingsDto(settings);
    }

    // ==================== Rules CRUD ====================

    public List<CrossSellRuleDto> getRules(UUID storeId) {
        return ruleRepository.findByStoreIdOrderByPriorityDesc(storeId)
                .stream()
                .map(this::toRuleDto)
                .toList();
    }

    public CrossSellRuleDto getRule(UUID ruleId) {
        CrossSellRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Çapraz satış kuralı bulunamadı: " + ruleId));
        return toRuleDto(rule);
    }

    @Transactional
    public CrossSellRuleDto createRule(UUID storeId, CreateCrossSellRuleRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Mağaza bulunamadı: " + storeId));

        CrossSellRule rule = CrossSellRule.builder()
                .store(store)
                .name(request.getName())
                .triggerType(request.getTriggerType())
                .triggerConditions(request.getTriggerConditions() != null ? request.getTriggerConditions() : Map.of())
                .recommendationType(request.getRecommendationType())
                .recommendationText(request.getRecommendationText())
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .maxProducts(request.getMaxProducts() != null ? request.getMaxProducts() : 3)
                .active(true)
                .build();

        rule = ruleRepository.save(rule);

        if (request.getProducts() != null && !request.getProducts().isEmpty()) {
            saveRuleProducts(rule, request.getProducts());
        }

        log.info("Cross-sell rule '{}' created for store {}", rule.getName(), storeId);
        return toRuleDto(rule);
    }

    @Transactional
    public CrossSellRuleDto updateRule(UUID ruleId, UpdateCrossSellRuleRequest request) {
        CrossSellRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Çapraz satış kuralı bulunamadı: " + ruleId));

        if (request.getName() != null) rule.setName(request.getName());
        if (request.getTriggerType() != null) rule.setTriggerType(request.getTriggerType());
        if (request.getTriggerConditions() != null) rule.setTriggerConditions(request.getTriggerConditions());
        if (request.getRecommendationType() != null) rule.setRecommendationType(request.getRecommendationType());
        if (request.getRecommendationText() != null) rule.setRecommendationText(request.getRecommendationText());
        if (request.getPriority() != null) rule.setPriority(request.getPriority());
        if (request.getMaxProducts() != null) rule.setMaxProducts(request.getMaxProducts());
        if (request.getActive() != null) rule.setActive(request.getActive());

        rule = ruleRepository.save(rule);

        if (request.getProducts() != null) {
            ruleProductRepository.deleteByRuleId(ruleId);
            if (!request.getProducts().isEmpty()) {
                saveRuleProducts(rule, request.getProducts());
            }
        }

        log.info("Cross-sell rule '{}' updated", rule.getName());
        return toRuleDto(rule);
    }

    @Transactional
    public void deleteRule(UUID ruleId) {
        ruleRepository.deleteById(ruleId);
        log.info("Cross-sell rule {} deleted", ruleId);
    }

    @Transactional
    public void toggleRule(UUID ruleId, boolean active) {
        CrossSellRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Çapraz satış kuralı bulunamadı: " + ruleId));
        rule.setActive(active);
        ruleRepository.save(rule);
    }

    // ==================== Recommendation Engine ====================

    /**
     * Get cross-sell recommendations for a given question.
     * Called from AiAnswerService after generating an AI answer.
     */
    @Transactional
    public List<CrossSellRecommendationDto> getRecommendations(TrendyolQuestion question) {
        UUID storeId = question.getStore().getId();

        // Check if cross-sell is enabled for this store
        CrossSellSettings settings = settingsRepository.findByStoreId(storeId).orElse(null);
        if (settings == null || !settings.getEnabled()) {
            return Collections.emptyList();
        }

        // Get active rules sorted by priority
        List<CrossSellRule> activeRules = ruleRepository.findActiveRulesByStoreId(storeId);
        if (activeRules.isEmpty()) {
            return Collections.emptyList();
        }

        // Find the first matching rule
        CrossSellRule matchedRule = matcher.findMatchingRule(activeRules, question);
        if (matchedRule == null) {
            return Collections.emptyList();
        }

        // Resolve recommended products
        List<String> barcodes = resolveProductBarcodes(matchedRule, question, storeId);
        if (barcodes.isEmpty()) {
            return Collections.emptyList();
        }

        // Limit to max products
        int limit = matchedRule.getMaxProducts() != null ? matchedRule.getMaxProducts() : settings.getDefaultMaxProducts();
        if (barcodes.size() > limit) {
            barcodes = barcodes.subList(0, limit);
        }

        // Fetch product details
        List<TrendyolProduct> products = productRepository.findByStoreIdAndBarcodeIn(storeId, barcodes);
        Map<String, TrendyolProduct> productMap = products.stream()
                .collect(Collectors.toMap(TrendyolProduct::getBarcode, p -> p, (a, b) -> a));

        // Build recommendations
        List<CrossSellRecommendationDto> recommendations = new ArrayList<>();
        for (String barcode : barcodes) {
            TrendyolProduct product = productMap.get(barcode);
            if (product != null) {
                recommendations.add(CrossSellRecommendationDto.builder()
                        .ruleId(matchedRule.getId())
                        .ruleName(matchedRule.getName())
                        .recommendationText(matchedRule.getRecommendationText())
                        .productBarcode(product.getBarcode())
                        .productTitle(product.getTitle())
                        .productImage(product.getImage())
                        .productPrice(settings.getShowProductPrice() ? product.getSalePrice() : null)
                        .productUrl(product.getProductUrl())
                        .build());

                // Track analytics
                analyticsRepository.save(CrossSellAnalytics.builder()
                        .store(question.getStore())
                        .rule(matchedRule)
                        .question(question)
                        .recommendedBarcode(barcode)
                        .wasIncludedInAnswer(settings.getIncludeInAnswer())
                        .build());
            }
        }

        log.info("Generated {} cross-sell recommendations for question {} using rule '{}'",
                recommendations.size(), question.getId(), matchedRule.getName());
        return recommendations;
    }

    // ==================== Analytics ====================

    public CrossSellAnalyticsDto getAnalytics(UUID storeId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long totalRules = ruleRepository.countByStoreId(storeId);
        long activeRules = ruleRepository.findActiveRulesByStoreId(storeId).size();
        long totalRecommendations = analyticsRepository.countByStoreIdSince(storeId, thirtyDaysAgo);
        long includedInAnswers = analyticsRepository.countIncludedByStoreIdSince(storeId, thirtyDaysAgo);

        return CrossSellAnalyticsDto.builder()
                .totalRules(totalRules)
                .activeRules(activeRules)
                .totalRecommendations(totalRecommendations)
                .includedInAnswers(includedInAnswers)
                .build();
    }

    // ==================== Product Search ====================

    /**
     * Search products for cross-sell rule builder.
     * Returns only products belonging to the specified store (user's inventory).
     * Searches by title, barcode, brand, and category name.
     */
    public List<ProductSearchResultDto> searchProducts(UUID storeId, String query) {
        log.debug("Searching products for store {} with query: {}", storeId, query);

        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        // Use existing repository method that searches by title, barcode, brand, categoryName
        Page<TrendyolProduct> products = productRepository.findByStoreIdAndSearch(
            storeId,
            query.trim(),
            PageRequest.of(0, 20) // Limit to 20 results for dropdown
        );

        return products.stream()
            .map(p -> ProductSearchResultDto.builder()
                .barcode(p.getBarcode())
                .title(p.getTitle())
                .image(p.getImage())
                .salePrice(p.getSalePrice())
                .onSale(p.getOnSale())
                .trendyolQuantity(p.getTrendyolQuantity())
                .build())
            .toList();
    }

    // ==================== Private Helpers ====================

    private List<String> resolveProductBarcodes(CrossSellRule rule, TrendyolQuestion question, UUID storeId) {
        return switch (rule.getRecommendationType()) {
            case SPECIFIC_PRODUCTS -> {
                List<CrossSellRuleProduct> ruleProducts = ruleProductRepository.findByRuleIdOrderByDisplayOrderAsc(rule.getId());
                yield ruleProducts.stream()
                        .map(CrossSellRuleProduct::getProductBarcode)
                        .toList();
            }
            case SAME_CATEGORY -> {
                // Get the product category from the question's associated product
                if (question.getBarcode() == null) {
                    yield Collections.emptyList();
                }
                Optional<TrendyolProduct> questionProduct = productRepository.findByStoreIdAndBarcode(storeId, question.getBarcode());
                if (questionProduct.isEmpty() || questionProduct.get().getCategoryName() == null) {
                    yield Collections.emptyList();
                }
                // Find other products in the same category, excluding the question's product
                String category = questionProduct.get().getCategoryName();
                List<TrendyolProduct> categoryProducts = productRepository.findByStoreId(storeId).stream()
                        .filter(p -> category.equalsIgnoreCase(p.getCategoryName()))
                        .filter(p -> !p.getBarcode().equals(question.getBarcode()))
                        .filter(p -> Boolean.TRUE.equals(p.getOnSale()))
                        .filter(p -> p.getTrendyolQuantity() != null && p.getTrendyolQuantity() > 0)
                        .limit(rule.getMaxProducts())
                        .toList();
                yield categoryProducts.stream().map(TrendyolProduct::getBarcode).toList();
            }
            case BESTSELLERS -> {
                // Return on-sale products with stock, sorted by quantity as a proxy for popularity
                List<TrendyolProduct> bestsellers = productRepository.findByStoreId(storeId).stream()
                        .filter(p -> Boolean.TRUE.equals(p.getOnSale()))
                        .filter(p -> p.getTrendyolQuantity() != null && p.getTrendyolQuantity() > 0)
                        .filter(p -> question.getBarcode() == null || !p.getBarcode().equals(question.getBarcode()))
                        .sorted(Comparator.comparing(
                                (TrendyolProduct p) -> p.getTrendyolQuantity() != null ? p.getTrendyolQuantity() : 0
                        ).reversed())
                        .limit(rule.getMaxProducts())
                        .toList();
                yield bestsellers.stream().map(TrendyolProduct::getBarcode).toList();
            }
        };
    }

    private void saveRuleProducts(CrossSellRule rule, List<CreateCrossSellRuleRequest.RuleProductRequest> productRequests) {
        for (int i = 0; i < productRequests.size(); i++) {
            CreateCrossSellRuleRequest.RuleProductRequest req = productRequests.get(i);
            CrossSellRuleProduct ruleProduct = CrossSellRuleProduct.builder()
                    .rule(rule)
                    .productBarcode(req.getProductBarcode())
                    .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : i)
                    .build();
            ruleProductRepository.save(ruleProduct);
        }
    }

    // ==================== DTO Mapping ====================

    private CrossSellRuleDto toRuleDto(CrossSellRule rule) {
        List<CrossSellRuleProductDto> productDtos = Collections.emptyList();
        if (rule.getProducts() != null && !rule.getProducts().isEmpty()) {
            productDtos = rule.getProducts().stream()
                    .map(p -> CrossSellRuleProductDto.builder()
                            .id(p.getId())
                            .productBarcode(p.getProductBarcode())
                            .displayOrder(p.getDisplayOrder())
                            .build())
                    .sorted(Comparator.comparing(CrossSellRuleProductDto::getDisplayOrder))
                    .toList();
        }

        return CrossSellRuleDto.builder()
                .id(rule.getId())
                .storeId(rule.getStore().getId())
                .name(rule.getName())
                .triggerType(rule.getTriggerType())
                .triggerConditions(rule.getTriggerConditions())
                .recommendationType(rule.getRecommendationType())
                .recommendationText(rule.getRecommendationText())
                .priority(rule.getPriority())
                .maxProducts(rule.getMaxProducts())
                .active(rule.getActive())
                .products(productDtos)
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }

    private CrossSellSettingsDto toSettingsDto(CrossSellSettings settings) {
        return CrossSellSettingsDto.builder()
                .id(settings.getId())
                .storeId(settings.getStore().getId())
                .enabled(settings.getEnabled())
                .defaultMaxProducts(settings.getDefaultMaxProducts())
                .includeInAnswer(settings.getIncludeInAnswer())
                .showProductImage(settings.getShowProductImage())
                .showProductPrice(settings.getShowProductPrice())
                .build();
    }
}
