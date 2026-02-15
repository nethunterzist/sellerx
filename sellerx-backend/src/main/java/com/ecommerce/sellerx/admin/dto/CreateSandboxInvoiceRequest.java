package com.ecommerce.sellerx.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Sandbox faturası oluşturmak için request DTO.
 * Dashboard ve finansal hesaplamaları test etmek için kullanılır.
 *
 * Komisyon ve Kargo faturaları için özel alanlar:
 * - barcode: Hangi ürün için fatura (komisyon/kargo faturası)
 * - commissionRate: Komisyon oranı (%) - Komisyon Faturası için
 * - shippingCostPerUnit: Birim kargo maliyeti (TL) - Kargo Fatura için
 *
 * Bu alanlar fatura eklendiğinde ilgili ürünün lastCommissionRate / lastShippingCostPerUnit
 * değerlerini günceller, böylece yeni siparişler bu değerleri referans alır.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSandboxInvoiceRequest {

    // Fatura türü (zorunlu)
    // Örnek: "Komisyon Faturası", "Kargo Fatura", "Platform Hizmet Bedeli", "Reklam Bedeli"
    private String transactionType;

    // Tutar (zorunlu)
    // Pozitif değer = borç (debt), Negatif değer = alacak (credit/iade)
    private BigDecimal amount;

    // Fatura tarihi (opsiyonel - default: bugün)
    private LocalDate transactionDate;

    // Açıklama (opsiyonel)
    private String description;

    // İlişkili sipariş numarası (opsiyonel - komisyon faturalarında kullanılır)
    private String orderNumber;

    // Fatura seri numarası (opsiyonel - default: otomatik oluşturulur)
    private String invoiceSerialNumber;

    // === Ürün İlişkilendirme (Komisyon/Kargo Faturaları İçin) ===

    // Hangi ürün için fatura (Komisyon Faturası veya Kargo Fatura için zorunlu)
    private String barcode;

    // Komisyon oranı (%) - Komisyon Faturası için
    // Bu değer product.lastCommissionRate'i günceller
    private BigDecimal commissionRate;

    // Birim kargo maliyeti (TL/adet) - Kargo Fatura için
    // Bu değer product.lastShippingCostPerUnit'i günceller
    private BigDecimal shippingCostPerUnit;
}
