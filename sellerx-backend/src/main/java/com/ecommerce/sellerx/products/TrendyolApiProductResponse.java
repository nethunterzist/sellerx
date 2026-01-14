package com.ecommerce.sellerx.products;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrendyolApiProductResponse {
    
    @JsonProperty("totalElements")
    private Long totalElements;
    
    @JsonProperty("totalPages")
    private Integer totalPages;
    
    @JsonProperty("page")
    private Integer page;
    
    @JsonProperty("size")
    private Integer size;
    
    @JsonProperty("content")
    private List<TrendyolApiProduct> content;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TrendyolApiProduct {
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("approved")
        private Boolean approved;
        
        @JsonProperty("archived")
        private Boolean archived;
        
        @JsonProperty("productCode")
        private Long productCode;
        
        @JsonProperty("batchRequestId")
        private String batchRequestId;
        
        @JsonProperty("supplierId")
        private Long supplierId;
        
        @JsonProperty("createDateTime")
        private Long createDateTime;
        
        @JsonProperty("lastUpdateDate")
        private Long lastUpdateDate;
        
        @JsonProperty("gender")
        private String gender;
        
        @JsonProperty("brand")
        private String brand;
        
        @JsonProperty("barcode")
        private String barcode;
        
        @JsonProperty("title")
        private String title;
        
        @JsonProperty("categoryName")
        private String categoryName;
        
        @JsonProperty("productMainId")
        private String productMainId;
        
        @JsonProperty("description")
        private String description;
        
        @JsonProperty("stockUnitType")
        private String stockUnitType;
        
        @JsonProperty("quantity")
        private Integer quantity;
        
        @JsonProperty("listPrice")
        private BigDecimal listPrice;
        
        @JsonProperty("salePrice")
        private BigDecimal salePrice;
        
        @JsonProperty("vatRate")
        private Integer vatRate;
        
        @JsonProperty("dimensionalWeight")
        private BigDecimal dimensionalWeight;
        
        @JsonProperty("stockCode")
        private String stockCode;
        
        @JsonProperty("deliveryOption")
        private DeliveryOption deliveryOption;
        
        @JsonProperty("images")
        private List<ProductImage> images;
        
        @JsonProperty("attributes")
        private List<ProductAttribute> attributes;
        
        @JsonProperty("platformListingId")
        private String platformListingId;
        
        @JsonProperty("stockId")
        private String stockId;
        
        @JsonProperty("hasActiveCampaign")
        private Boolean hasActiveCampaign;
        
        @JsonProperty("locked")
        private Boolean locked;
        
        @JsonProperty("productContentId")
        private Long productContentId;
        
        @JsonProperty("pimCategoryId")
        private Long pimCategoryId;
        
        @JsonProperty("brandId")
        private Long brandId;
        
        @JsonProperty("version")
        private Integer version;
        
        @JsonProperty("color")
        private String color;
        
        @JsonProperty("size")
        private String size;
        
        @JsonProperty("lockedByUnSuppliedReason")
        private Boolean lockedByUnSuppliedReason;
        
        @JsonProperty("onSale")
        private Boolean onsale;
        
        @JsonProperty("blacklisted")
        private Boolean blacklisted;
        
        @JsonProperty("rejected")
        private Boolean rejected;
        
        @JsonProperty("productUrl")
        private String productUrl;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DeliveryOption {
        @JsonProperty("deliveryDuration")
        private Integer deliveryDuration;
        
        @JsonProperty("fastDeliveryType")
        private String fastDeliveryType;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductImage {
        @JsonProperty("url")
        private String url;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductAttribute {
        @JsonProperty("attributeId")
        private Long attributeId;
        
        @JsonProperty("attributeName")
        private String attributeName;
        
        @JsonProperty("attributeValueId")
        private Long attributeValueId;
        
        @JsonProperty("attributeValue")
        private String attributeValue;
    }
}
