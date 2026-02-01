package com.ecommerce.sellerx.buybox.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Buybox alert ayarları güncelleme isteği DTO'su.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAlertSettingsRequest {

    private Boolean alertOnLoss;

    private Boolean alertOnNewCompetitor;

    @DecimalMin(value = "0.01", message = "Fiyat eşiği en az 0.01 TL olmalıdır")
    private BigDecimal alertPriceThreshold;

    private Boolean isActive;
}
