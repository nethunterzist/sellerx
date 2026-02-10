package com.ecommerce.sellerx.products;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddStockInfoRequest {
    private Integer quantity;
    private Double unitCost;
    private Integer costVatRate;
    private LocalDate stockDate;

    // ============== Döviz Kuru Desteği (Excel F1, F2, F4) ==============
    private String currency; // "TRY", "USD", "EUR" (null = TRY)
    private Double exchangeRate; // Döviz kuru (örn: 44.0 TL/$)
    private Double foreignCost; // Yabancı para cinsinden maliyet (örn: 10 $)

    // ============== ÖTV Desteği (Excel F5) ==============
    private Double otvRate; // Özel Tüketim Vergisi oranı (örn: 0.2 = %20)

    // ============== Reklam Metrikleri (Excel C23, C24) ==============
    private Double cpc; // Cost Per Click (TL)
    private Double cvr; // Conversion Rate (örn: 0.018 = %1.8)
}
