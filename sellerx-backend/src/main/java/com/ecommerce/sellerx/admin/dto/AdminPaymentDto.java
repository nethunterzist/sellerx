package com.ecommerce.sellerx.admin.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPaymentDto {
    private Long id;
    private String userEmail;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;
}
