package com.ecommerce.sellerx.alerts;

import com.ecommerce.sellerx.auth.AuthService;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.users.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing alert rules.
 * Provides CRUD operations for user-defined alert rules.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertRuleService {

    private final AlertRuleRepository alertRuleRepository;
    private final StoreRepository storeRepository;
    private final AuthService authService;

    /**
     * Get all alert rules for the current user.
     */
    @Transactional(readOnly = true)
    public List<AlertRuleDto> getRulesForCurrentUser() {
        User user = authService.getCurrentUser();
        return alertRuleRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get paginated alert rules for the current user.
     */
    @Transactional(readOnly = true)
    public Page<AlertRuleDto> getRulesForCurrentUser(Pageable pageable) {
        User user = authService.getCurrentUser();
        return alertRuleRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(this::toDto);
    }

    /**
     * Get a specific rule by ID.
     */
    @Transactional(readOnly = true)
    public AlertRuleDto getRuleById(UUID ruleId) {
        User user = authService.getCurrentUser();
        AlertRule rule = alertRuleRepository.findByIdAndUserId(ruleId, user.getId())
                .orElseThrow(() -> new AlertRuleNotFoundException("Alert rule not found: " + ruleId));
        return toDto(rule);
    }

    /**
     * Create a new alert rule.
     */
    @Transactional
    public AlertRuleDto createRule(CreateAlertRuleRequest request) {
        User user = authService.getCurrentUser();

        // Check for duplicate name
        if (alertRuleRepository.existsByUserIdAndName(user.getId(), request.getName())) {
            throw new AlertRuleValidationException("A rule with this name already exists");
        }

        // Validate threshold for certain conditions
        validateThreshold(request.getConditionType(), request.getThreshold());

        // Get store if specified
        Store store = null;
        if (request.getStoreId() != null) {
            store = storeRepository.findById(request.getStoreId())
                    .filter(s -> s.getUser().getId().equals(user.getId()))
                    .orElseThrow(() -> new AlertRuleValidationException("Store not found: " + request.getStoreId()));
        }

        AlertRule rule = AlertRule.builder()
                .user(user)
                .store(store)
                .name(request.getName())
                .alertType(request.getAlertType())
                .conditionType(request.getConditionType())
                .threshold(request.getThreshold())
                .productBarcode(request.getProductBarcode())
                .categoryName(request.getCategoryName())
                .emailEnabled(request.getEmailEnabled())
                .pushEnabled(request.getPushEnabled())
                .inAppEnabled(request.getInAppEnabled())
                .active(true)
                .cooldownMinutes(request.getCooldownMinutes())
                .triggerCount(0)
                .build();

        AlertRule saved = alertRuleRepository.save(rule);
        log.info("Created alert rule: {} for user: {}", saved.getId(), user.getId());

        return toDto(saved);
    }

    /**
     * Update an existing alert rule.
     */
    @Transactional
    public AlertRuleDto updateRule(UUID ruleId, UpdateAlertRuleRequest request) {
        User user = authService.getCurrentUser();

        AlertRule rule = alertRuleRepository.findByIdAndUserId(ruleId, user.getId())
                .orElseThrow(() -> new AlertRuleNotFoundException("Alert rule not found: " + ruleId));

        // Check for duplicate name if name is being changed
        if (request.getName() != null && !request.getName().equals(rule.getName())) {
            if (alertRuleRepository.existsByUserIdAndName(user.getId(), request.getName())) {
                throw new AlertRuleValidationException("A rule with this name already exists");
            }
            rule.setName(request.getName());
        }

        // Update store if specified
        if (request.getStoreId() != null) {
            Store store = storeRepository.findById(request.getStoreId())
                    .filter(s -> s.getUser().getId().equals(user.getId()))
                    .orElseThrow(() -> new AlertRuleValidationException("Store not found: " + request.getStoreId()));
            rule.setStore(store);
        }

        // Update other fields if provided
        if (request.getAlertType() != null) {
            rule.setAlertType(request.getAlertType());
        }
        if (request.getConditionType() != null) {
            rule.setConditionType(request.getConditionType());
        }
        if (request.getThreshold() != null) {
            rule.setThreshold(request.getThreshold());
        }
        if (request.getProductBarcode() != null) {
            rule.setProductBarcode(request.getProductBarcode());
        }
        if (request.getCategoryName() != null) {
            rule.setCategoryName(request.getCategoryName());
        }
        if (request.getEmailEnabled() != null) {
            rule.setEmailEnabled(request.getEmailEnabled());
        }
        if (request.getPushEnabled() != null) {
            rule.setPushEnabled(request.getPushEnabled());
        }
        if (request.getInAppEnabled() != null) {
            rule.setInAppEnabled(request.getInAppEnabled());
        }
        if (request.getActive() != null) {
            rule.setActive(request.getActive());
        }
        if (request.getCooldownMinutes() != null) {
            rule.setCooldownMinutes(request.getCooldownMinutes());
        }

        // Validate threshold for the current condition
        validateThreshold(rule.getConditionType(), rule.getThreshold());

        AlertRule saved = alertRuleRepository.save(rule);
        log.info("Updated alert rule: {} for user: {}", saved.getId(), user.getId());

        return toDto(saved);
    }

    /**
     * Toggle active status of a rule.
     */
    @Transactional
    public AlertRuleDto toggleRuleActive(UUID ruleId) {
        User user = authService.getCurrentUser();

        AlertRule rule = alertRuleRepository.findByIdAndUserId(ruleId, user.getId())
                .orElseThrow(() -> new AlertRuleNotFoundException("Alert rule not found: " + ruleId));

        rule.setActive(!rule.getActive());
        AlertRule saved = alertRuleRepository.save(rule);

        log.info("Toggled alert rule {} to active={} for user: {}", ruleId, saved.getActive(), user.getId());
        return toDto(saved);
    }

    /**
     * Delete an alert rule.
     */
    @Transactional
    public void deleteRule(UUID ruleId) {
        User user = authService.getCurrentUser();

        AlertRule rule = alertRuleRepository.findByIdAndUserId(ruleId, user.getId())
                .orElseThrow(() -> new AlertRuleNotFoundException("Alert rule not found: " + ruleId));

        alertRuleRepository.delete(rule);
        log.info("Deleted alert rule: {} for user: {}", ruleId, user.getId());
    }

    /**
     * Get rule count for current user.
     */
    @Transactional(readOnly = true)
    public long getRuleCount() {
        User user = authService.getCurrentUser();
        return alertRuleRepository.countByUserId(user.getId());
    }

    /**
     * Get active rule count for current user.
     */
    @Transactional(readOnly = true)
    public long getActiveRuleCount() {
        User user = authService.getCurrentUser();
        return alertRuleRepository.countByUserIdAndActive(user.getId(), true);
    }

    /**
     * Validate that threshold is provided for conditions that require it.
     */
    private void validateThreshold(AlertConditionType conditionType, java.math.BigDecimal threshold) {
        if (conditionType == AlertConditionType.BELOW ||
            conditionType == AlertConditionType.ABOVE ||
            conditionType == AlertConditionType.EQUALS) {
            if (threshold == null) {
                throw new AlertRuleValidationException("Threshold is required for " + conditionType + " condition");
            }
        }
    }

    /**
     * Convert entity to DTO.
     */
    private AlertRuleDto toDto(AlertRule rule) {
        return AlertRuleDto.builder()
                .id(rule.getId())
                .storeId(rule.getStore() != null ? rule.getStore().getId() : null)
                .storeName(rule.getStore() != null ? rule.getStore().getStoreName() : null)
                .name(rule.getName())
                .alertType(rule.getAlertType())
                .conditionType(rule.getConditionType())
                .threshold(rule.getThreshold())
                .productBarcode(rule.getProductBarcode())
                .categoryName(rule.getCategoryName())
                .emailEnabled(rule.getEmailEnabled())
                .pushEnabled(rule.getPushEnabled())
                .inAppEnabled(rule.getInAppEnabled())
                .active(rule.getActive())
                .cooldownMinutes(rule.getCooldownMinutes())
                .lastTriggeredAt(rule.getLastTriggeredAt())
                .triggerCount(rule.getTriggerCount())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }
}
