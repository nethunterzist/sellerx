package com.ecommerce.sellerx.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminActivityLogDto {
    private Long id;
    private String email;
    private String action;
    private String ipAddress;
    private String device;
    private String browser;
    private Boolean success;
    private LocalDateTime createdAt;
}
