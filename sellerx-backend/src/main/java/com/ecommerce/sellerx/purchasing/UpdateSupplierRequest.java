package com.ecommerce.sellerx.purchasing;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSupplierRequest {
    private String name;
    private String contactPerson;
    private String email;
    private String phone;
    private String address;
    private String country;
    private String currency;
    private Integer paymentTermsDays;
    private String notes;
}
