package com.ecommerce.sellerx.alerts;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for updating an existing alert rule.
 * All fields are optional - only non-null fields will be updated.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAlertRuleRequest {

    /**
     * Store ID - null means applies to all user's stores.
     */
    private UUID storeId;

    @Size(max = 200, message = "Rule name must be at most 200 characters")
    private String name;

    private AlertType alertType;

    private AlertConditionType conditionType;

    private BigDecimal threshold;

    @Size(max = 100, message = "Product barcode must be at most 100 characters")
    private String productBarcode;

    @Size(max = 200, message = "Category name must be at most 200 characters")
    private String categoryName;

    private Boolean emailEnabled;

    private Boolean pushEnabled;

    private Boolean inAppEnabled;

    private Boolean active;

    @Min(value = 1, message = "Cooldown must be at least 1 minute")
    private Integer cooldownMinutes;
}
