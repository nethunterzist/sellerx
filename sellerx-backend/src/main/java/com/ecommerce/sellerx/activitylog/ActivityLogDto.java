package com.ecommerce.sellerx.activitylog;

import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActivityLogDto {
    private Long id;
    private String action;
    private String device;
    private String browser;
    private String ipAddress;
    private Boolean success;
    private LocalDateTime createdAt;
}
