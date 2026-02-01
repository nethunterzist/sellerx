package com.ecommerce.sellerx.alerts;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for creating a new alert rule.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAlertRuleRequest {

    /**
     * Store ID - null means applies to all user's stores.
     */
    private UUID storeId;

    @NotBlank(message = "Rule name is required")
    @Size(max = 200, message = "Rule name must be at most 200 characters")
    private String name;

    @NotNull(message = "Alert type is required")
    private AlertType alertType;

    @NotNull(message = "Condition type is required")
    private AlertConditionType conditionType;

    /**
     * Threshold value for the condition.
     * Required for BELOW, ABOVE, EQUALS conditions.
     */
    private BigDecimal threshold;

    /**
     * Product barcode - null means all products.
     */
    @Size(max = 100, message = "Product barcode must be at most 100 characters")
    private String productBarcode;

    /**
     * Category name - null means all categories.
     */
    @Size(max = 200, message = "Category name must be at most 200 characters")
    private String categoryName;

    /**
     * Enable email notifications.
     */
    @Builder.Default
    private Boolean emailEnabled = true;

    /**
     * Enable push notifications.
     */
    @Builder.Default
    private Boolean pushEnabled = false;

    /**
     * Enable in-app notifications.
     */
    @Builder.Default
    private Boolean inAppEnabled = true;

    /**
     * Cooldown in minutes between triggers.
     */
    @Min(value = 1, message = "Cooldown must be at least 1 minute")
    @Builder.Default
    private Integer cooldownMinutes = 60;
}
