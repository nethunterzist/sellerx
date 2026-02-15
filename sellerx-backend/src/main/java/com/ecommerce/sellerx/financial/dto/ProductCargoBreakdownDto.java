package com.ecommerce.sellerx.financial.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO for product cargo breakdown response.
 * Shows cargo cost breakdown by shipments for a specific product (barcode).
 * Used in the "Detay" panel for KARGO Ürünler tab.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCargoBreakdownDto {

    /**
     * Product barcode (SKU)
     */
    private String barcode;

    /**
     * Product name
     */
    private String productName;

    /**
     * Product image URL
     */
    private String productImageUrl;

    /**
     * Product URL on Trendyol
     */
    private String productUrl;

    // ================================================================================
    // Toplam Değerler (Total Values)
    // ================================================================================

    /**
     * Total cargo cost amount for this product
     */
    private BigDecimal totalAmount;

    /**
     * Total VAT amount for this product's cargo
     */
    private BigDecimal totalVatAmount;

    /**
     * Total desi (volumetric weight) for all shipments
     */
    private BigDecimal totalDesi;

    /**
     * Total number of shipments/cargo invoices for this product
     */
    private int totalShipmentCount;

    /**
     * Number of distinct orders for this product
     */
    private int orderCount;

    // ================================================================================
    // Ortalama Değerler (Average Values)
    // ================================================================================

    /**
     * Average desi per shipment
     */
    private BigDecimal averageDesi;

    /**
     * Average cost per shipment
     */
    private BigDecimal averageCostPerShipment;

    // ================================================================================
    // Gönderi Listesi (Shipment List)
    // ================================================================================

    /**
     * List of shipments (paginated)
     */
    private List<CargoShipmentDetailDto> shipments;

    // ================================================================================
    // Sayfalama Bilgisi (Pagination Info)
    // ================================================================================

    /**
     * Current page number (0-based)
     */
    @Builder.Default
    private int currentPage = 0;

    /**
     * Total number of pages
     */
    @Builder.Default
    private int totalPages = 1;

    /**
     * Whether there are more pages to load
     */
    @Builder.Default
    private boolean hasMore = false;

    /**
     * DTO for individual cargo shipment details
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CargoShipmentDetailDto {
        /**
         * Trendyol order number
         */
        private String orderNumber;

        /**
         * Shipment package ID from Trendyol
         */
        private Long shipmentPackageId;

        /**
         * Invoice serial number
         */
        private String invoiceSerialNumber;

        /**
         * Cargo cost amount
         */
        private BigDecimal amount;

        /**
         * VAT amount
         */
        private BigDecimal vatAmount;

        /**
         * Desi (volumetric weight)
         */
        private Integer desi;

        /**
         * Invoice date
         */
        private LocalDate invoiceDate;

        /**
         * Cargo company name (e.g., Trendyol Express, Yurtiçi, MNG)
         */
        private String cargoCompany;
    }
}
