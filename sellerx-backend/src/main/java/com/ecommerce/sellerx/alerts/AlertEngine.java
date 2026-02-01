package com.ecommerce.sellerx.alerts;

import com.ecommerce.sellerx.email.EmailService;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.users.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Alert Engine - Evaluates alert rules and triggers alerts when conditions are met.
 * This is the core service that connects rule definitions with actual events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertEngine {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertHistoryRepository alertHistoryRepository;
    private final EmailService emailService;

    /**
     * Check stock alerts for a list of products.
     * Called after product sync to detect stock-related conditions.
     */
    @Async
    @Transactional
    public void checkStockAlerts(Store store, List<TrendyolProduct> products) {
        log.debug("Checking stock alerts for store: {} with {} products", store.getId(), products.size());

        User user = store.getUser();
        List<AlertRule> stockRules = alertRuleRepository.findActiveRulesByType(
                user.getId(), store.getId(), AlertType.STOCK);

        if (stockRules.isEmpty()) {
            log.debug("No active stock rules found for user: {}", user.getId());
            return;
        }

        for (TrendyolProduct product : products) {
            for (AlertRule rule : stockRules) {
                try {
                    evaluateStockRule(rule, product, store, user);
                } catch (Exception e) {
                    log.error("Error evaluating stock rule {} for product {}: {}",
                            rule.getId(), product.getBarcode(), e.getMessage());
                }
            }
        }
    }

    /**
     * Check stock alert for a single product.
     */
    @Transactional
    public void checkStockAlertForProduct(Store store, TrendyolProduct product) {
        User user = store.getUser();
        List<AlertRule> stockRules = alertRuleRepository.findActiveRulesByType(
                user.getId(), store.getId(), AlertType.STOCK);

        for (AlertRule rule : stockRules) {
            try {
                evaluateStockRule(rule, product, store, user);
            } catch (Exception e) {
                log.error("Error evaluating stock rule {} for product {}: {}",
                        rule.getId(), product.getBarcode(), e.getMessage());
            }
        }
    }

    /**
     * Evaluate a single stock rule against a product.
     */
    private void evaluateStockRule(AlertRule rule, TrendyolProduct product, Store store, User user) {
        // Check if rule applies to this product
        if (!ruleAppliesToProduct(rule, product)) {
            return;
        }

        // Check if rule is in cooldown
        if (!rule.canTrigger()) {
            log.debug("Rule {} is in cooldown, skipping", rule.getId());
            return;
        }

        Integer stock = product.getTrendyolQuantity();
        if (stock == null) {
            stock = 0;
        }

        boolean shouldTrigger = evaluateCondition(
                rule.getConditionType(),
                BigDecimal.valueOf(stock),
                rule.getThreshold()
        );

        if (shouldTrigger) {
            triggerStockAlert(rule, product, stock, store, user);
        }
    }

    /**
     * Check if a rule applies to a specific product.
     */
    private boolean ruleAppliesToProduct(AlertRule rule, TrendyolProduct product) {
        // Check product barcode filter
        if (rule.getProductBarcode() != null && !rule.getProductBarcode().isEmpty()) {
            if (!rule.getProductBarcode().equals(product.getBarcode())) {
                return false;
            }
        }

        // Check category filter
        if (rule.getCategoryName() != null && !rule.getCategoryName().isEmpty()) {
            if (product.getCategoryName() == null ||
                !product.getCategoryName().toLowerCase().contains(rule.getCategoryName().toLowerCase())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Evaluate a condition against a value and threshold.
     */
    private boolean evaluateCondition(AlertConditionType conditionType, BigDecimal value, BigDecimal threshold) {
        switch (conditionType) {
            case BELOW:
                return threshold != null && value.compareTo(threshold) < 0;
            case ABOVE:
                return threshold != null && value.compareTo(threshold) > 0;
            case EQUALS:
                return threshold != null && value.compareTo(threshold) == 0;
            case ZERO:
                return value.compareTo(BigDecimal.ZERO) == 0;
            case CHANGED:
                // For CHANGED, we need historical data - handled separately
                return false;
            default:
                return false;
        }
    }

    /**
     * Trigger a stock alert.
     */
    private void triggerStockAlert(AlertRule rule, TrendyolProduct product, Integer stock, Store store, User user) {
        String title = generateStockAlertTitle(rule, product, stock);
        String message = generateStockAlertMessage(rule, product, stock);
        AlertSeverity severity = determineStockAlertSeverity(rule, stock);

        Map<String, Object> data = new HashMap<>();
        data.put("productId", product.getId());
        data.put("barcode", product.getBarcode());
        data.put("productName", product.getTitle());
        data.put("currentStock", stock);
        data.put("threshold", rule.getThreshold());
        data.put("imageUrl", product.getImage());

        AlertHistory alert = createAlert(rule, user, store, title, message, severity, data);

        // Send email notification if enabled
        if (Boolean.TRUE.equals(rule.getEmailEnabled()) && user.getEmail() != null) {
            sendStockAlertEmail(alert, rule, product, stock, store, user, severity);
        }

        log.info("Stock alert triggered: {} for product {} (stock: {})",
                rule.getName(), product.getBarcode(), stock);
    }

    /**
     * Send stock alert email.
     */
    private void sendStockAlertEmail(AlertHistory alert, AlertRule rule, TrendyolProduct product,
                                     Integer stock, Store store, User user, AlertSeverity severity) {
        try {
            int threshold = rule.getThreshold() != null ? rule.getThreshold().intValue() : 0;
            emailService.sendStockAlertEmail(
                    user.getEmail(),
                    truncate(product.getTitle(), 100),
                    product.getBarcode(),
                    stock,
                    threshold,
                    store.getStoreName(),
                    severity.name()
            );

            // Update alert to mark email as sent
            alert.setEmailSent(true);
            alertHistoryRepository.save(alert);

            log.debug("Stock alert email sent to {} for product {}", user.getEmail(), product.getBarcode());
        } catch (Exception e) {
            log.error("Failed to send stock alert email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    /**
     * Generate alert title for stock alerts.
     */
    private String generateStockAlertTitle(AlertRule rule, TrendyolProduct product, Integer stock) {
        String productName = truncate(product.getTitle(), 50);

        switch (rule.getConditionType()) {
            case BELOW:
                return String.format("Stok Uyarısı: %s - %d adet kaldı", productName, stock);
            case ZERO:
                return String.format("Stok Tükendi: %s", productName);
            case ABOVE:
                return String.format("Yüksek Stok: %s - %d adet", productName, stock);
            default:
                return String.format("Stok Değişikliği: %s", productName);
        }
    }

    /**
     * Generate alert message for stock alerts.
     */
    private String generateStockAlertMessage(AlertRule rule, TrendyolProduct product, Integer stock) {
        switch (rule.getConditionType()) {
            case BELOW:
                return String.format(
                        "Ürün '%s' (Barkod: %s) stoğu %d adede düştü. Belirlediğiniz eşik: %s adet.",
                        product.getTitle(), product.getBarcode(), stock, rule.getThreshold());
            case ZERO:
                return String.format(
                        "Ürün '%s' (Barkod: %s) stoğu tükendi! Acil stok girişi yapılması önerilir.",
                        product.getTitle(), product.getBarcode());
            case ABOVE:
                return String.format(
                        "Ürün '%s' (Barkod: %s) stoğu %d adede yükseldi. Belirlediğiniz eşik: %s adet.",
                        product.getTitle(), product.getBarcode(), stock, rule.getThreshold());
            default:
                return String.format(
                        "Ürün '%s' (Barkod: %s) stoğunda değişiklik tespit edildi. Mevcut stok: %d adet.",
                        product.getTitle(), product.getBarcode(), stock);
        }
    }

    /**
     * Determine severity based on stock level.
     */
    private AlertSeverity determineStockAlertSeverity(AlertRule rule, Integer stock) {
        if (stock == 0) {
            return AlertSeverity.CRITICAL;
        }
        if (rule.getThreshold() != null) {
            BigDecimal threshold = rule.getThreshold();
            BigDecimal stockValue = BigDecimal.valueOf(stock);
            // If stock is less than 50% of threshold, it's HIGH severity
            if (stockValue.compareTo(threshold.multiply(BigDecimal.valueOf(0.5))) < 0) {
                return AlertSeverity.HIGH;
            }
        }
        return AlertSeverity.MEDIUM;
    }

    /**
     * Create and save an alert.
     */
    @Transactional
    public AlertHistory createAlert(
            AlertRule rule,
            User user,
            Store store,
            String title,
            String message,
            AlertSeverity severity,
            Map<String, Object> data) {

        AlertHistory alert = AlertHistory.builder()
                .rule(rule)
                .user(user)
                .store(store)
                .alertType(rule.getAlertType())
                .title(title)
                .message(message)
                .severity(severity)
                .data(data)
                .emailSent(false)
                .pushSent(false)
                .inAppSent(true)
                .build();

        AlertHistory saved = alertHistoryRepository.save(alert);

        // Update rule trigger info
        rule.recordTrigger();
        alertRuleRepository.save(rule);

        log.debug("Created alert: {} for user: {}", saved.getId(), user.getId());

        return saved;
    }

    /**
     * Create a system alert (not tied to a rule).
     */
    @Transactional
    public AlertHistory createSystemAlert(
            User user,
            Store store,
            String title,
            String message,
            AlertSeverity severity,
            Map<String, Object> data) {

        return createSystemAlert(user, store, title, message, severity, data, false);
    }

    /**
     * Create a system alert with optional email notification.
     */
    @Transactional
    public AlertHistory createSystemAlert(
            User user,
            Store store,
            String title,
            String message,
            AlertSeverity severity,
            Map<String, Object> data,
            boolean sendEmail) {

        AlertHistory alert = AlertHistory.builder()
                .user(user)
                .store(store)
                .alertType(AlertType.SYSTEM)
                .title(title)
                .message(message)
                .severity(severity)
                .data(data)
                .emailSent(false)
                .pushSent(false)
                .inAppSent(true)
                .build();

        AlertHistory saved = alertHistoryRepository.save(alert);

        // Send email if requested
        if (sendEmail && user.getEmail() != null) {
            try {
                String storeName = store != null ? store.getStoreName() : null;
                emailService.sendAlertEmail(
                        user.getEmail(),
                        title,
                        message,
                        storeName,
                        severity.name(),
                        AlertType.SYSTEM.name()
                );
                saved.setEmailSent(true);
                alertHistoryRepository.save(saved);
            } catch (Exception e) {
                log.error("Failed to send system alert email: {}", e.getMessage());
            }
        }

        return saved;
    }

    /**
     * Utility method to truncate strings.
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }
}
