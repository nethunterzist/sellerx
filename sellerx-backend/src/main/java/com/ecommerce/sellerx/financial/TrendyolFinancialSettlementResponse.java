package com.ecommerce.sellerx.financial;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendyolFinancialSettlementResponse {
    
    @JsonProperty("page")
    private Integer page;
    
    @JsonProperty("size")
    private Integer size;
    
    @JsonProperty("totalPages")
    private Integer totalPages;
    
    @JsonProperty("totalElements")
    private Long totalElements;
    
    @JsonProperty("content")
    private List<TrendyolFinancialSettlementItem> content;
}
