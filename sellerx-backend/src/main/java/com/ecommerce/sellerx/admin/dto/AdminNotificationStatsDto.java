package com.ecommerce.sellerx.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminNotificationStatsDto {
    private long total;
    private long totalUnread;
    private Map<String, Long> byType;
}
