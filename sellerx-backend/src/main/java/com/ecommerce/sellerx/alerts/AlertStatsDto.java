package com.ecommerce.sellerx.alerts;

import lombok.*;

/**
 * DTO for alert statistics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertStatsDto {

    private long unreadCount;
    private long stockAlertsLast24h;
    private long profitAlertsLast24h;
    private long orderAlertsLast24h;
    private long totalAlertsLast7Days;
}
