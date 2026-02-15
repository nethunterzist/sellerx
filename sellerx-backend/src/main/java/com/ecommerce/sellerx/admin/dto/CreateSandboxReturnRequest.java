package com.ecommerce.sellerx.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Sandbox iadesi oluşturmak için request DTO.
 * İade maliyetlerini ve dashboard hesaplamalarını test etmek için kullanılır.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSandboxReturnRequest {

    // Sipariş bilgisi (zorunlu - mevcut siparişten)
    private String orderNumber;

    // Ürün bilgisi (zorunlu)
    private String barcode;

    // Ürün adı (opsiyonel - siparişteki üründen alınır)
    private String productName;

    // İade miktarı (zorunlu)
    private Integer quantity;

    // Ürün maliyeti (opsiyonel - FIFO'dan hesaplanır)
    private BigDecimal productCost;

    // Gidiş kargo maliyeti (opsiyonel - default: 25 TL)
    private BigDecimal shippingCostOut;

    // Dönüş kargo maliyeti (opsiyonel - default: 25 TL)
    private BigDecimal shippingCostReturn;

    // Komisyon kaybı (opsiyonel - siparişten hesaplanır)
    private BigDecimal commissionLoss;

    // Paketleme maliyeti (opsiyonel - default: 5 TL)
    private BigDecimal packagingCost;

    // İade sebebi (opsiyonel)
    private String returnReason;

    // İade tarihi (opsiyonel - default: bugün)
    private LocalDate returnDate;
}
