package com.ecommerce.sellerx.categories;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryBulkInsertRequest {
    private List<CategoryInsertDto> result;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryInsertDto {
        @JsonProperty("category_id")
        private Long categoryId;
        
        @JsonProperty("category_name")
        private String categoryName;
        
        @JsonProperty("parent_category")
        private String parentCategory;
        
        @JsonProperty("commission_rate")
        private String commissionRate;
        
        @JsonProperty("average_shipment_size")
        private String averageShipmentSize;
    }
}
