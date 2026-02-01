package com.ecommerce.sellerx.financial.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO for Invoice Type Statistics
 * Shows summary for each invoice type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceTypeStatsDto {

    private String invoiceTypeCode;
    private String invoiceType;
    private String invoiceCategory;
    @JsonProperty("isDeduction")
    private boolean isDeduction;
    private long invoiceCount;
    private BigDecimal totalAmount;
    private BigDecimal totalVatAmount;
    private BigDecimal vatRate;
    private String icon;
    private String color;

    /**
     * Get icon based on category
     */
    public String getIcon() {
        if (icon != null) return icon;
        return switch (invoiceCategory) {
            case "KOMISYON" -> "receipt";
            case "KARGO" -> "truck";
            case "ULUSLARARASI" -> "globe";
            case "CEZA" -> "alert-triangle";
            case "REKLAM" -> "megaphone";
            case "IADE" -> "refresh-ccw";
            default -> "file-text";
        };
    }

    /**
     * Get color based on deduction/refund status
     */
    public String getColor() {
        if (color != null) return color;
        return isDeduction ? "red" : "green";
    }
}
