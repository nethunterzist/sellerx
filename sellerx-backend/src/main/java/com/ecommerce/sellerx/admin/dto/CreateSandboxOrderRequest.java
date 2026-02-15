package com.ecommerce.sellerx.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Sandbox siparişi oluşturmak için request DTO.
 * Frontend hesaplamalarını test etmek için kullanılır.
 *
 * NOT: Komisyon ve kargo bilgileri SORULMAZ - canlı sistemle aynı algoritma kullanılır:
 * - Komisyon: product.lastCommissionRate → product.commissionRate → 0
 * - Kargo: product.lastShippingCostPerUnit → 0
 * Bu değerler fatura eklendiğinde ürüne yazılır.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSandboxOrderRequest {

    // Ürün bilgisi (zorunlu)
    private String barcode;

    // Satış bilgileri (zorunlu)
    private Integer quantity;
    private BigDecimal unitPrice;

    // Sipariş tarihi (opsiyonel - default: bugün)
    private LocalDate orderDate;

    // Sipariş durumu (opsiyonel - default: "Delivered")
    private String status;

    // Müşteri bilgileri (opsiyonel - test için)
    private String customerName;
    private String city;
}
