package com.ecommerce.sellerx.alerts;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for AlertRule entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertRuleDto {

    private UUID id;
    private UUID storeId;
    private String storeName;

    private String name;
    private AlertType alertType;
    private AlertConditionType conditionType;
    private BigDecimal threshold;

    private String productBarcode;
    private String categoryName;

    private Boolean emailEnabled;
    private Boolean pushEnabled;
    private Boolean inAppEnabled;
    private Boolean active;

    private Integer cooldownMinutes;
    private LocalDateTime lastTriggeredAt;
    private Integer triggerCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
