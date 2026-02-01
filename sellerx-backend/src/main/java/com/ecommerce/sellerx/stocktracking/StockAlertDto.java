package com.ecommerce.sellerx.stocktracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAlertDto {
    private UUID id;
    private UUID trackedProductId;
    private String productName;
    private String productImageUrl;

    private StockAlertType alertType;
    private StockAlertSeverity severity;
    private String title;
    private String message;

    private Integer oldQuantity;
    private Integer newQuantity;
    private Integer threshold;

    private Boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    public static StockAlertDto fromEntity(StockAlert entity) {
        return StockAlertDto.builder()
                .id(entity.getId())
                .trackedProductId(entity.getTrackedProduct().getId())
                .productName(entity.getTrackedProduct().getProductName())
                .productImageUrl(entity.getTrackedProduct().getImageUrl())
                .alertType(entity.getAlertType())
                .severity(entity.getSeverity())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .oldQuantity(entity.getOldQuantity())
                .newQuantity(entity.getNewQuantity())
                .threshold(entity.getThreshold())
                .isRead(entity.getIsRead())
                .readAt(entity.getReadAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
