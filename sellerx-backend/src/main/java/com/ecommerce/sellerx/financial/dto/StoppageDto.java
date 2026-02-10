package com.ecommerce.sellerx.financial.dto;

import com.ecommerce.sellerx.financial.TrendyolStoppage;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for Stoppage (Withholding Tax) data.
 * Represents individual stopaj/tevkifat records from Trendyol.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoppageDto {

    private UUID id;
    private LocalDateTime transactionDate;
    private BigDecimal amount;
    private String invoiceSerialNumber;
    private Long paymentOrderId;
    private Long receiptId;
    private String description;

    /**
     * Convert from entity to DTO.
     */
    public static StoppageDto fromEntity(TrendyolStoppage entity) {
        if (entity == null) return null;

        return StoppageDto.builder()
                .id(entity.getId())
                .transactionDate(entity.getTransactionDate())
                .amount(entity.getAmount())
                .invoiceSerialNumber(entity.getInvoiceSerialNumber())
                .paymentOrderId(entity.getPaymentOrderId())
                .receiptId(entity.getReceiptId())
                .description(entity.getDescription())
                .build();
    }
}
