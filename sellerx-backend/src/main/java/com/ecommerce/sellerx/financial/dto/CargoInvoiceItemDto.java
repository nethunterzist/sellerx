package com.ecommerce.sellerx.financial.dto;

import com.ecommerce.sellerx.financial.TrendyolCargoInvoice;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for Cargo Invoice Item
 * Shows individual shipment details within a cargo invoice
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CargoInvoiceItemDto {

    private UUID id;
    private String invoiceSerialNumber;
    private String orderNumber;
    private Long shipmentPackageId;
    private BigDecimal amount;
    private Integer desi;
    private String shipmentPackageType;
    private Integer vatRate;
    private BigDecimal vatAmount;
    private LocalDate invoiceDate;

    // Additional fields from rawData (if available) or enriched from TrendyolProduct
    private String barcode;
    private String productName;
    private String productImageUrl;
    private String cargoCompany;

    private LocalDateTime createdAt;

    /**
     * Convert from entity to DTO
     */
    public static CargoInvoiceItemDto fromEntity(TrendyolCargoInvoice cargoInvoice) {
        CargoInvoiceItemDto.CargoInvoiceItemDtoBuilder builder = CargoInvoiceItemDto.builder()
                .id(cargoInvoice.getId())
                .invoiceSerialNumber(cargoInvoice.getInvoiceSerialNumber())
                .orderNumber(cargoInvoice.getOrderNumber())
                .shipmentPackageId(cargoInvoice.getShipmentPackageId())
                .amount(cargoInvoice.getAmount())
                .desi(cargoInvoice.getDesi())
                .shipmentPackageType(cargoInvoice.getShipmentPackageType())
                .vatRate(cargoInvoice.getVatRate())
                .vatAmount(cargoInvoice.getVatAmount())
                .invoiceDate(cargoInvoice.getInvoiceDate())
                .createdAt(cargoInvoice.getCreatedAt());

        // Extract additional fields from rawData if available (use safe type conversion)
        Map<String, Object> rawData = cargoInvoice.getRawData();
        if (rawData != null) {
            Object barcode = rawData.get("barcode");
            if (barcode != null) {
                builder.barcode(barcode.toString());
            }
            Object productName = rawData.get("productName");
            if (productName != null) {
                builder.productName(productName.toString());
            }
            Object cargoCompany = rawData.get("cargoCompany");
            if (cargoCompany != null) {
                builder.cargoCompany(cargoCompany.toString());
            }
        }

        return builder.build();
    }
}
