package com.ecommerce.sellerx.purchasing;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierDto {
    private Long id;
    private String name;
    private String contactPerson;
    private String email;
    private String phone;
    private String address;
    private String country;
    private String currency;
    private Integer paymentTermsDays;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
