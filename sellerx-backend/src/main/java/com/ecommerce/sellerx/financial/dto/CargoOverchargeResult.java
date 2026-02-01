package com.ecommerce.sellerx.financial.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for Cargo Overcharge Detection result (Fazla Kargo Faturası)
 * Detects when Trendyol bills more cargo than expected based on desi weight
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CargoOverchargeResult {

    private UUID cargoInvoiceId;
    private String invoiceSerialNumber;
    private Long shipmentPackageId;
    private String orderNumber;
    private LocalDate invoiceDate;

    // Weight-based calculation
    private Integer desi;                  // Weight in desi
    private BigDecimal expectedAmount;     // desi * standard rate
    private BigDecimal actualAmount;       // Charged amount from Trendyol
    private BigDecimal overchargeAmount;   // actual - expected (if positive)

    // Status: OVERCHARGED, NORMAL
    private String status;

    // Additional info
    private String shipmentPackageType;
    private BigDecimal vatAmount;
    private Integer vatRate;
}
