package com.ecommerce.sellerx.financial;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * DTO for Trendyol cargo-invoice API paginated response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrendyolCargoInvoiceResponse {

    @JsonProperty("content")
    private List<TrendyolCargoInvoiceItem> content;

    @JsonProperty("totalElements")
    private Integer totalElements;

    @JsonProperty("totalPages")
    private Integer totalPages;

    @JsonProperty("size")
    private Integer size;

    @JsonProperty("number")
    private Integer number;

    @JsonProperty("first")
    private Boolean first;

    @JsonProperty("last")
    private Boolean last;

    @JsonProperty("empty")
    private Boolean empty;
}
