package com.ecommerce.sellerx.alerts;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for AlertHistory entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertHistoryDto {

    private UUID id;
    private UUID ruleId;
    private String ruleName;
    private UUID storeId;
    private String storeName;

    private AlertType alertType;
    private String title;
    private String message;
    private AlertSeverity severity;

    private Map<String, Object> data;

    private Boolean emailSent;
    private Boolean pushSent;
    private Boolean inAppSent;

    private String status;

    private Boolean read;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
