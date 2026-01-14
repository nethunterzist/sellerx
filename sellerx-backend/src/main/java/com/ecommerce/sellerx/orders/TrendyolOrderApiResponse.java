package com.ecommerce.sellerx.orders;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for representing Trendyol API response structure
 */
@Data
public class TrendyolOrderApiResponse {
    
    @JsonProperty("totalElements")
    private Integer totalElements;
    
    @JsonProperty("totalPages")
    private Integer totalPages;
    
    @JsonProperty("page")
    private Integer page;
    
    @JsonProperty("size")
    private Integer size;
    
    @JsonProperty("content")
    private List<TrendyolOrderContent> content;
    
    @Data
    public static class TrendyolOrderContent {
        
        @JsonProperty("orderNumber")
        private String orderNumber;
        
        @JsonProperty("id")
        private Long id; // This is the package number
        
        @JsonProperty("grossAmount")
        private BigDecimal grossAmount;
        
        @JsonProperty("totalDiscount")
        private BigDecimal totalDiscount;
        
        @JsonProperty("totalTyDiscount")
        private BigDecimal totalTyDiscount;
        
        @JsonProperty("totalPrice")
        private BigDecimal totalPrice;
        
        @JsonProperty("lines")
        private List<TrendyolOrderLine> lines;
        
        @JsonProperty("originShipmentDate")
        private Long originShipmentDate; // This will be converted to LocalDateTime
        
        @JsonProperty("shipmentPackageStatus")
        private String shipmentPackageStatus;
        
        @JsonProperty("status")
        private String status;
        
        @JsonProperty("cargoDeci")
        private Integer cargoDeci;
        
        @JsonProperty("cargoTrackingNumber")
        private Long cargoTrackingNumber; // We need this to filter out orders without package numbers
    }
    
    @Data
    public static class TrendyolOrderLine {
        
        @JsonProperty("barcode")
        private String barcode;
        
        @JsonProperty("productName")
        private String productName;
        
        @JsonProperty("quantity")
        private Integer quantity;
        
        @JsonProperty("amount")
        private BigDecimal amount; // This becomes unitPriceOrder
        
        @JsonProperty("discount")
        private BigDecimal discount; // This becomes unitPriceDiscount
        
        @JsonProperty("tyDiscount")
        private BigDecimal tyDiscount; // This becomes unitPriceTyDiscount
        
        @JsonProperty("vatBaseAmount")
        private BigDecimal vatBaseAmount;
        
        @JsonProperty("price")
        private BigDecimal price; // Final price after discounts
    }
}
