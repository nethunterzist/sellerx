package com.ecommerce.sellerx.returns.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClaimItemDto {
    private String claimItemId;
    private String barcode;
    private String productName;
    private String productSize;
    private String productColor;
    private BigDecimal price;
    private BigDecimal quantity;
    private String reasonName;
    private String reasonCode;
    private String status;
    private String customerNote;
    private Boolean autoAccepted;
    private Boolean acceptedBySeller;
    private String imageUrl;
}
