package com.ecommerce.sellerx.admin.dto;

import com.ecommerce.sellerx.users.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserListDto {
    private Long id;
    private String name;
    private String email;
    private Role role;
    private int storeCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    // Subscription info
    private String subscriptionStatus;
    private String planName;
}
