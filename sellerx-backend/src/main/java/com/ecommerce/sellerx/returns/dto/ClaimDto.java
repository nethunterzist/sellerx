package com.ecommerce.sellerx.returns.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClaimDto {
    private UUID id;
    private String claimId;
    private String orderNumber;
    private String customerFirstName;
    private String customerLastName;
    private String customerFullName;
    private LocalDateTime claimDate;
    private String cargoTrackingNumber;
    private String cargoTrackingLink;
    private String cargoProviderName;
    private String status;
    private List<ClaimItemDto> items;
    private int totalItemCount;
    private LocalDateTime lastModifiedDate;
    private LocalDateTime syncedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
