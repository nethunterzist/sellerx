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
}
