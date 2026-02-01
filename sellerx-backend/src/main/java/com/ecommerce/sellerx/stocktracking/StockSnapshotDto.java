package com.ecommerce.sellerx.stocktracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockSnapshotDto {
    private UUID id;
    private Integer quantity;
    private Boolean inStock;
    private BigDecimal price;
    private Integer previousQuantity;
    private Integer quantityChange;
    private LocalDateTime checkedAt;

    public static StockSnapshotDto fromEntity(StockSnapshot entity) {
        return StockSnapshotDto.builder()
                .id(entity.getId())
                .quantity(entity.getQuantity())
                .inStock(entity.getInStock())
                .price(entity.getPrice())
                .previousQuantity(entity.getPreviousQuantity())
                .quantityChange(entity.getQuantityChange())
                .checkedAt(entity.getCheckedAt())
                .build();
    }
}
