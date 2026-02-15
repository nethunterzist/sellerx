package com.ecommerce.sellerx.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Sandbox faturası için response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SandboxInvoiceDto {

    private UUID id;
    private String trendyolId;
    private LocalDateTime transactionDate;
    private String transactionType;
    private String description;
    private BigDecimal debt;
    private BigDecimal credit;
    private String invoiceSerialNumber;
    private String orderNumber;
    private LocalDateTime createdAt;
}
