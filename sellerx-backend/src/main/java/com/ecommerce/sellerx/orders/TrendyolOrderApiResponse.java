package com.ecommerce.sellerx.orders;

import com.fasterxml.jackson.annotation.JsonAlias;
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
        @JsonAlias("shipmentPackageId") // Trendyol API migration: id -> shipmentPackageId (08.12.2025)
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

        @JsonProperty("shipmentAddress")
        private ShipmentAddress shipmentAddress; // Shipment address with city information

        @JsonProperty("packageHistories")
        private List<PackageHistory> packageHistories;

        @JsonProperty("estimatedDeliveryEndDate")
        private Long estimatedDeliveryEndDate;

        @JsonProperty("lastModifiedDate")
        private Long lastModifiedDate;

        @JsonProperty("customerFirstName")
        private String customerFirstName;

        @JsonProperty("customerLastName")
        private String customerLastName;

        @JsonProperty("customerEmail")
        private String customerEmail;

        @JsonProperty("customerId")
        private Long customerId;

        // New fields from Trendyol API changelog (08.12.2025)
        @JsonProperty("cancelledBy")
        private String cancelledBy;

        @JsonProperty("cancelReason")
        private String cancelReason;

        @JsonProperty("cancelReasonCode")
        private String cancelReasonCode;

        @JsonProperty("packageTotalDiscount")
        private BigDecimal packageTotalDiscount;
    }

    @Data
    public static class PackageHistory {
        @JsonProperty("createdDate")
        private Long createdDate;

        @JsonProperty("status")
        private String status;
    }

    @Data
    public static class ShipmentAddress {
        @JsonProperty("city")
        private String city;

        @JsonProperty("cityCode")
        private Integer cityCode;

        @JsonProperty("district")
        private String district;

        @JsonProperty("districtId")
        private Integer districtId;

        @JsonProperty("fullName")
        private String fullName;

        @JsonProperty("fullAddress")
        private String fullAddress;
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
        @JsonAlias("lineGrossAmount") // Trendyol API migration: amount -> lineGrossAmount (08.12.2025)
        private BigDecimal amount; // This becomes unitPriceOrder
        
        @JsonProperty("discount")
        private BigDecimal discount; // This becomes unitPriceDiscount
        
        @JsonProperty("tyDiscount")
        private BigDecimal tyDiscount; // This becomes unitPriceTyDiscount
        
        @JsonProperty("vatBaseAmount")
        @JsonAlias("vatRate") // Trendyol API migration: vatBaseAmount -> vatRate (08.12.2025)
        private BigDecimal vatBaseAmount;

        @JsonProperty("price")
        private BigDecimal price; // Final price after discounts

        // New fields from Trendyol API changelog (08.12.2025, 11.04.2025)
        @JsonProperty("lineTotalDiscount")
        private BigDecimal lineTotalDiscount;

        @JsonProperty("productCategoryId")
        private Long productCategoryId;

        @JsonProperty("laborCost")
        private BigDecimal laborCost;
    }
}
