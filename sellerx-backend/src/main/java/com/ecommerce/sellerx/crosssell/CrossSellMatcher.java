package com.ecommerce.sellerx.crosssell;

import com.ecommerce.sellerx.qa.TrendyolQuestion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Matches cross-sell rules against incoming questions.
 * Rules are evaluated by priority (highest first). First matching rule wins.
 */
@Slf4j
@Component
public class CrossSellMatcher {

    /**
     * Find the first matching rule for a question from a list of active rules (already sorted by priority desc).
     */
    public CrossSellRule findMatchingRule(List<CrossSellRule> activeRules, TrendyolQuestion question) {
        for (CrossSellRule rule : activeRules) {
            if (matches(rule, question)) {
                log.debug("Cross-sell rule '{}' matched for question {}", rule.getName(), question.getId());
                return rule;
            }
        }
        return null;
    }

    /**
     * Check if a single rule matches a given question.
     */
    public boolean matches(CrossSellRule rule, TrendyolQuestion question) {
        return switch (rule.getTriggerType()) {
            case ALL_QUESTIONS -> true;
            case KEYWORD -> matchesKeyword(rule.getTriggerConditions(), question);
            case CATEGORY -> matchesCategory(rule.getTriggerConditions(), question);
            case PRODUCT -> matchesProduct(rule.getTriggerConditions(), question);
        };
    }

    private boolean matchesKeyword(Map<String, Object> conditions, TrendyolQuestion question) {
        List<String> keywords = getStringList(conditions, "keywords");
        if (keywords.isEmpty()) {
            return false;
        }

        String questionText = normalizeText(question.getCustomerQuestion());
        String productTitle = question.getProductTitle() != null ? normalizeText(question.getProductTitle()) : "";

        for (String keyword : keywords) {
            String normalizedKeyword = normalizeText(keyword);
            if (questionText.contains(normalizedKeyword) || productTitle.contains(normalizedKeyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesCategory(Map<String, Object> conditions, TrendyolQuestion question) {
        List<String> categoryNames = getStringList(conditions, "categoryNames");
        if (categoryNames.isEmpty()) {
            return false;
        }

        // We don't have category directly on question, but productTitle may contain category hints.
        // For a more accurate match, we'd need to look up the product by barcode.
        // For now, match against productTitle as a reasonable approximation.
        String productTitle = question.getProductTitle() != null ? normalizeText(question.getProductTitle()) : "";
        if (productTitle.isEmpty()) {
            return false;
        }

        for (String category : categoryNames) {
            if (productTitle.contains(normalizeText(category))) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesProduct(Map<String, Object> conditions, TrendyolQuestion question) {
        List<String> productBarcodes = getStringList(conditions, "productBarcodes");
        if (productBarcodes.isEmpty()) {
            return false;
        }

        String questionBarcode = question.getBarcode();
        if (questionBarcode == null || questionBarcode.isBlank()) {
            return false;
        }

        return productBarcodes.stream()
                .anyMatch(barcode -> barcode.equalsIgnoreCase(questionBarcode));
    }

    @SuppressWarnings("unchecked")
    private List<String> getStringList(Map<String, Object> conditions, String key) {
        if (conditions == null || !conditions.containsKey(key)) {
            return Collections.emptyList();
        }
        Object value = conditions.get(key);
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }
        return Collections.emptyList();
    }

    private String normalizeText(String text) {
        if (text == null) return "";
        // Turkish-aware lowercase
        return text.toLowerCase(new Locale("tr", "TR")).trim();
    }
}
