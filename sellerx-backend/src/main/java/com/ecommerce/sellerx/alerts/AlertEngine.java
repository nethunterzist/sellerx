package com.ecommerce.sellerx.alerts;

import com.ecommerce.sellerx.email.EmailService;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.returns.ReturnRecord;
import com.ecommerce.sellerx.returns.ReturnRecordRepository;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.users.User;
import com.ecommerce.sellerx.websocket.event.AlertCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final ReturnRecordRepository returnRecordRepository;
    private final EmailService emailService;
    private final ApplicationEventPublisher eventPublisher;

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
        // Check product barcode filter (supports comma-separated multiple barcodes)
        if (rule.getProductBarcode() != null && !rule.getProductBarcode().isEmpty()) {
            String[] barcodes = rule.getProductBarcode().split(",");
            boolean matches = false;
            for (String barcode : barcodes) {
                if (barcode.trim().equals(product.getBarcode())) {
                    matches = true;
                    break;
                }
            }
            if (!matches) {
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
     * Check return alerts for a store.
     * Evaluates return count and return rate per barcode over the last 7 days.
     */
    @Async
    @Transactional
    public void checkReturnAlerts(Store store) {
        log.debug("Checking return alerts for store: {}", store.getId());

        User user = store.getUser();
        List<AlertRule> returnRules = alertRuleRepository.findActiveRulesByType(
                user.getId(), store.getId(), AlertType.RETURN);

        if (returnRules.isEmpty()) {
            log.debug("No active return rules found for user: {}", user.getId());
            return;
        }

        // Fetch last 7 days of returns
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(7);
        List<ReturnRecord> recentReturns = returnRecordRepository.findByStoreAndDateRange(
                store.getId(), startDate, endDate);

        if (recentReturns.isEmpty()) {
            log.debug("No recent returns found for store: {}", store.getId());
            return;
        }

        // Group returns by barcode
        Map<String, List<ReturnRecord>> returnsByBarcode = recentReturns.stream()
                .collect(Collectors.groupingBy(ReturnRecord::getBarcode));

        for (AlertRule rule : returnRules) {
            try {
                evaluateReturnRule(rule, returnsByBarcode, store, user);
            } catch (Exception e) {
                log.error("Error evaluating return rule {} for store {}: {}",
                        rule.getId(), store.getId(), e.getMessage());
            }
        }
    }

    /**
     * Evaluate a single return rule against grouped return data.
     */
    private void evaluateReturnRule(AlertRule rule, Map<String, List<ReturnRecord>> returnsByBarcode,
                                     Store store, User user) {
        if (!rule.canTrigger()) {
            log.debug("Return rule {} is in cooldown, skipping", rule.getId());
            return;
        }

        for (Map.Entry<String, List<ReturnRecord>> entry : returnsByBarcode.entrySet()) {
            String barcode = entry.getKey();
            List<ReturnRecord> returns = entry.getValue();

            // Check if rule applies to this barcode
            if (rule.getProductBarcode() != null && !rule.getProductBarcode().isEmpty()) {
                String[] targetBarcodes = rule.getProductBarcode().split(",");
                boolean matches = false;
                for (String target : targetBarcodes) {
                    if (target.trim().equals(barcode)) {
                        matches = true;
                        break;
                    }
                }
                if (!matches) continue;
            }

            int returnCount = returns.stream()
                    .mapToInt(r -> r.getQuantity() != null ? r.getQuantity() : 1)
                    .sum();

            BigDecimal returnCountValue = BigDecimal.valueOf(returnCount);

            boolean shouldTrigger = evaluateCondition(
                    rule.getConditionType(), returnCountValue, rule.getThreshold());

            if (shouldTrigger) {
                String productName = returns.get(0).getProductName();
                if (productName == null) productName = barcode;
                triggerReturnAlert(rule, barcode, truncate(productName, 50), returnCount, store, user);
                // Only trigger once per rule evaluation to avoid spam
                return;
            }
        }
    }

    /**
     * Trigger a return alert.
     */
    private void triggerReturnAlert(AlertRule rule, String barcode, String productName,
                                     int returnCount, Store store, User user) {
        String title = String.format("Iade Uyarisi: %s - Son 7 gunde %d iade", productName, returnCount);
        String message = String.format(
                "Urun '%s' (Barkod: %s) son 7 gunde %d adet iade aldı. Belirlediginiz esik: %s.",
                productName, barcode, returnCount,
                rule.getThreshold() != null ? rule.getThreshold().intValue() + " adet" : "-");

        AlertSeverity severity = returnCount >= 10 ? AlertSeverity.CRITICAL
                : returnCount >= 5 ? AlertSeverity.HIGH : AlertSeverity.MEDIUM;

        Map<String, Object> data = new HashMap<>();
        data.put("barcode", barcode);
        data.put("productName", productName);
        data.put("returnCount", returnCount);
        data.put("threshold", rule.getThreshold());
        data.put("period", "7d");

        AlertHistory alert = createAlert(rule, user, store, title, message, severity, data);

        // Send email notification if enabled
        if (Boolean.TRUE.equals(rule.getEmailEnabled()) && user.getEmail() != null) {
            try {
                emailService.sendAlertEmail(
                        user.getEmail(), title, message,
                        store.getStoreName(), severity.name(), AlertType.RETURN.name());
                alert.setEmailSent(true);
                alertHistoryRepository.save(alert);
            } catch (Exception e) {
                log.error("Failed to send return alert email to {}: {}", user.getEmail(), e.getMessage());
            }
        }

        log.info("Return alert triggered: {} for barcode {} (returns: {})",
                rule.getName(), barcode, returnCount);
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

        // Publish event for WebSocket notification
        eventPublisher.publishEvent(new AlertCreatedEvent(this, saved));

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

        // Publish event for WebSocket notification
        eventPublisher.publishEvent(new AlertCreatedEvent(this, saved));

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
