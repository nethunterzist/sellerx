package com.ecommerce.sellerx.products;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // Ignore unknown fields during deserialization
public class CostAndStockInfo {
    private Integer quantity; // Original stock quantity
    private Double unitCost;
    private Integer costVatRate;
    private LocalDate stockDate;

    private String costSource; // "AUTO_DETECTED", "MANUAL", "PURCHASE_ORDER" (null = legacy/manual)

    // ============== Döviz Kuru Desteği (Excel F1, F2, F4) ==============
    private String currency; // "TRY", "USD", "EUR" (null = TRY)
    private Double exchangeRate; // Döviz kuru (örn: 44.0 TL/$)
    private Double foreignCost; // Yabancı para cinsinden maliyet (örn: 10 $)
    // NOT: unitCost = foreignCost * exchangeRate olarak hesaplanır

    // ============== ÖTV Desteği (Excel F5) ==============
    private Double otvRate; // Özel Tüketim Vergisi oranı (örn: 0.2 = %20)
    // NOT: Fatura maliyet = unitCost * (1 + otvRate) * (1 + vatRate/100)

    // ============== Reklam Metrikleri (Excel C23, C24) ==============
    private Double cpc; // Cost Per Click (TL) - tıklama başı maliyet
    private Double cvr; // Conversion Rate (örn: 0.018 = %1.8)
    // NOT: Reklam maliyeti = cpc / cvr (1 satış için gereken reklam harcaması)

    // Stock usage tracking - default to 0 if null
    @JsonProperty(value = "usedQuantity", defaultValue = "0")
    private Integer usedQuantity; // How much has been used from this stock
    
    // Helper method to get remaining quantity
    public Integer getRemainingQuantity() {
        Integer used = getUsedQuantity(); // Use the getter which handles null
        Integer total = quantity != null ? quantity : 0;
        return total - used;
    }
    
    // Getter for usedQuantity with default handling
    public Integer getUsedQuantity() {
        return usedQuantity != null ? usedQuantity : 0;
    }
    
    // Setter for usedQuantity
    public void setUsedQuantity(Integer usedQuantity) {
        this.usedQuantity = usedQuantity;
    }

    // ============== Helper Methods ==============

    /**
     * Döviz kurlu maliyet hesabı.
     * Eğer foreignCost ve exchangeRate varsa: foreignCost * exchangeRate
     * Yoksa: unitCost
     */
    public Double getEffectiveUnitCost() {
        if (foreignCost != null && exchangeRate != null && exchangeRate > 0) {
            return foreignCost * exchangeRate;
        }
        return unitCost;
    }

    /**
     * ÖTV dahil fatura maliyeti (KDV hariç).
     * Formül: unitCost * (1 + otvRate)
     */
    public Double getCostWithOtv() {
        Double baseCost = getEffectiveUnitCost();
        if (baseCost == null) return null;
        double otv = otvRate != null ? otvRate : 0.0;
        return baseCost * (1 + otv);
    }

    /**
     * ÖTV ve KDV dahil tam fatura maliyeti.
     * Formül: unitCost * (1 + otvRate) * (1 + vatRate/100)
     * Excel formülü: C11 = F4 × (1 + F5) × (1 + F6)
     */
    public Double getFullInvoiceCost() {
        Double costWithOtv = getCostWithOtv();
        if (costWithOtv == null) return null;
        double vat = costVatRate != null ? costVatRate / 100.0 : 0.0;
        return costWithOtv * (1 + vat);
    }

    /**
     * 1 satış için gereken reklam maliyeti.
     * Formül: cpc / cvr
     * Excel formülü: C19 = C23 / C24
     */
    public Double getAdvertisingCostPerSale() {
        if (cpc != null && cvr != null && cvr > 0) {
            return cpc / cvr;
        }
        return null;
    }

    /**
     * ACOS (Advertising Cost of Sale) - Satış başına reklam maliyeti oranı.
     * Formül: (cpc / cvr) / salePrice
     * Excel formülü: C22 = C19 / C14
     */
    public Double calculateAcos(Double salePrice) {
        Double adCost = getAdvertisingCostPerSale();
        if (adCost != null && salePrice != null && salePrice > 0) {
            return adCost / salePrice;
        }
        return null;
    }

    /**
     * Para birimi gösterimi için helper.
     */
    public String getCurrencyDisplay() {
        if (currency == null || "TRY".equals(currency)) {
            return "₺";
        }
        switch (currency) {
            case "USD": return "$";
            case "EUR": return "€";
            default: return currency;
        }
    }
}
