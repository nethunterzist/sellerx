package com.ecommerce.sellerx.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Trendyol webhook payload model - represents incoming order data from Trendyol webhooks
 */
@Data
public class TrendyolWebhookPayload {
    
    @JsonProperty("shipmentAddress")
    private Address shipmentAddress;
    
    @JsonProperty("orderNumber")
    private String orderNumber;
    
    @JsonProperty("grossAmount")
    private BigDecimal grossAmount;
    
    @JsonProperty("totalDiscount")
    private BigDecimal totalDiscount;
    
    @JsonProperty("totalTyDiscount")
    private BigDecimal totalTyDiscount;
    
    @JsonProperty("taxNumber")
    private String taxNumber;
    
    @JsonProperty("invoiceAddress")
    private Address invoiceAddress;
    
    @JsonProperty("customerFirstName")
    private String customerFirstName;
    
    @JsonProperty("customerEmail")
    private String customerEmail;
    
    @JsonProperty("customerId")
    private Long customerId;
    
    @JsonProperty("customerLastName")
    private String customerLastName;
    
    @JsonProperty("id")
    private Long id; // This is the package number
    
    @JsonProperty("cargoTrackingNumber")
    private Long cargoTrackingNumber;
    
    @JsonProperty("cargoProviderName")
    private String cargoProviderName;
    
    @JsonProperty("cargoSenderNumber")
    private String cargoSenderNumber;
    
    @JsonProperty("lines")
    private List<OrderLine> lines;
    
    @JsonProperty("orderDate")
    private Long orderDate; // Timestamp in milliseconds
    
    @JsonProperty("identityNumber")
    private String identityNumber;
    
    @JsonProperty("currencyCode")
    private String currencyCode;
    
    @JsonProperty("packageHistories")
    private List<PackageHistory> packageHistories;
    
    @JsonProperty("shipmentPackageStatus")
    private String shipmentPackageStatus;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("whoPays")
    private Integer whoPays;
    
    @JsonProperty("deliveryType")
    private String deliveryType;
    
    @JsonProperty("timeSlotId")
    private Integer timeSlotId;
    
    @JsonProperty("estimatedDeliveryStartDate")
    private Long estimatedDeliveryStartDate;
    
    @JsonProperty("estimatedDeliveryEndDate")
    private Long estimatedDeliveryEndDate;
    
    @JsonProperty("totalPrice")
    private BigDecimal totalPrice;
    
    @JsonProperty("deliveryAddressType")
    private String deliveryAddressType;
    
    @JsonProperty("agreedDeliveryDate")
    private Long agreedDeliveryDate;
    
    @JsonProperty("fastDelivery")
    private Boolean fastDelivery;
    
    @JsonProperty("originShipmentDate")
    private Long originShipmentDate;
    
    @JsonProperty("lastModifiedDate")
    private Long lastModifiedDate;
    
    @JsonProperty("commercial")
    private Boolean commercial;
    
    @JsonProperty("fastDeliveryType")
    private String fastDeliveryType;
    
    @JsonProperty("deliveredByService")
    private Boolean deliveredByService;
    
    @JsonProperty("agreedDeliveryDateExtendible")
    private Boolean agreedDeliveryDateExtendible;
    
    @JsonProperty("extendedAgreedDeliveryDate")
    private Long extendedAgreedDeliveryDate;
    
    @JsonProperty("agreedDeliveryExtensionEndDate")
    private Long agreedDeliveryExtensionEndDate;
    
    @JsonProperty("agreedDeliveryExtensionStartDate")
    private Long agreedDeliveryExtensionStartDate;
    
    @JsonProperty("warehouseId")
    private Long warehouseId;
    
    @JsonProperty("groupDeal")
    private Boolean groupDeal;
    
    @JsonProperty("invoiceLink")
    private String invoiceLink;
    
    @JsonProperty("micro")
    private Boolean micro;
    
    @JsonProperty("giftBoxRequested")
    private Boolean giftBoxRequested;
    
    @JsonProperty("3pByTrendyol")
    private Boolean thirdPartyByTrendyol;
    
    @JsonProperty("containsDangerousProduct")
    private Boolean containsDangerousProduct;
    
    @JsonProperty("cargoDeci")
    private BigDecimal cargoDeci;
    
    @JsonProperty("isCod")
    private Boolean isCod;
    
    @JsonProperty("createdBy")
    private String createdBy; // "order-creation", "split", "cancel", "transfer"
    
    @JsonProperty("originPackageIds")
    private List<Long> originPackageIds;
    
    @Data
    public static class Address {
        @JsonProperty("id")
        private Long id;
        
        @JsonProperty("firstName")
        private String firstName;
        
        @JsonProperty("lastName")
        private String lastName;
        
        @JsonProperty("company")
        private String company;
        
        @JsonProperty("address1")
        private String address1;
        
        @JsonProperty("address2")
        private String address2;
        
        @JsonProperty("addressLines")
        private AddressLines addressLines;
        
        @JsonProperty("city")
        private String city;
        
        @JsonProperty("cityCode")
        private Integer cityCode;
        
        @JsonProperty("district")
        private String district;
        
        @JsonProperty("districtId")
        private Integer districtId;
        
        @JsonProperty("countyId")
        private Integer countyId;
        
        @JsonProperty("countyName")
        private String countyName;
        
        @JsonProperty("shortAddress")
        private String shortAddress;
        
        @JsonProperty("stateName")
        private String stateName;
        
        @JsonProperty("postalCode")
        private String postalCode;
        
        @JsonProperty("countryCode")
        private String countryCode;
        
        @JsonProperty("neighborhoodId")
        private Integer neighborhoodId;
        
        @JsonProperty("neighborhood")
        private String neighborhood;
        
        @JsonProperty("phone")
        private String phone;
        
        @JsonProperty("latitude")
        private String latitude;
        
        @JsonProperty("longitude")
        private String longitude;
        
        @JsonProperty("fullAddress")
        private String fullAddress;
        
        @JsonProperty("fullName")
        private String fullName;
        
        @Data
        public static class AddressLines {
            @JsonProperty("addressLine1")
            private String addressLine1;
            
            @JsonProperty("addressLine2")
            private String addressLine2;
        }
    }
    
    @Data
    public static class OrderLine {
        @JsonProperty("quantity")
        private Integer quantity;
        
        @JsonProperty("salesCampaignId")
        private Long salesCampaignId;
        
        @JsonProperty("productSize")
        private String productSize;
        
        @JsonProperty("merchantSku")
        private String merchantSku;
        
        @JsonProperty("productName")
        private String productName;
        
        @JsonProperty("productCode")
        private Long productCode;
        
        @JsonProperty("productOrigin")
        private String productOrigin;
        
        @JsonProperty("merchantId")
        private Long merchantId;
        
        @JsonProperty("amount")
        private BigDecimal amount;
        
        @JsonProperty("discount")
        private BigDecimal discount;
        
        @JsonProperty("tyDiscount")
        private BigDecimal tyDiscount;
        
        @JsonProperty("discountDetails")
        private List<DiscountDetail> discountDetails;
        
        @JsonProperty("currencyCode")
        private String currencyCode;
        
        @JsonProperty("productColor")
        private String productColor;
        
        @JsonProperty("id")
        private Long id;
        
        @JsonProperty("sku")
        private String sku;
        
        @JsonProperty("vatBaseAmount")
        private BigDecimal vatBaseAmount;
        
        @JsonProperty("barcode")
        private String barcode;
        
        @JsonProperty("orderLineItemStatusName")
        private String orderLineItemStatusName;
        
        @JsonProperty("price")
        private BigDecimal price;
        
        @JsonProperty("fastDeliveryOptions")
        private List<FastDeliveryOption> fastDeliveryOptions;
        
        @JsonProperty("productCategoryId")
        private Long productCategoryId;
        
        @JsonProperty("laborCost")
        private BigDecimal laborCost;
        
        @Data
        public static class DiscountDetail {
            @JsonProperty("lineItemPrice")
            private BigDecimal lineItemPrice;
            
            @JsonProperty("lineItemDiscount")
            private BigDecimal lineItemDiscount;
            
            @JsonProperty("lineItemTyDiscount")
            private BigDecimal lineItemTyDiscount;
        }
        
        @Data
        public static class FastDeliveryOption {
            @JsonProperty("type")
            private String type;
        }
    }
    
    @Data
    public static class PackageHistory {
        @JsonProperty("createdDate")
        private Long createdDate;
        
        @JsonProperty("status")
        private String status;
    }
}
