package com.ecommerce.sellerx.categories;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TrendyolCategoryMapper {
    
    public TrendyolCategoryDto toDto(TrendyolCategory category) {
        if (category == null) {
            return null;
        }
        
        return TrendyolCategoryDto.builder()
                .id(category.getId())
                .categoryId(category.getCategoryId())
                .categoryName(category.getCategoryName())
                .parentCategory(category.getParentCategory())
                .commissionRate(category.getCommissionRate() != null ? category.getCommissionRate().toString() : null)
                .averageShipmentSize(category.getAverageShipmentSize() != null ? category.getAverageShipmentSize().toString() : null)
                .build();
    }
    
    public List<TrendyolCategoryDto> toDtoList(List<TrendyolCategory> categories) {
        return categories.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    public TrendyolCategory fromInsertDto(CategoryBulkInsertRequest.CategoryInsertDto insertDto) {
        if (insertDto == null) {
            return null;
        }
        
        return TrendyolCategory.builder()
                .categoryId(insertDto.getCategoryId())
                .categoryName(insertDto.getCategoryName())
                .parentCategory(insertDto.getParentCategory())
                .commissionRate(insertDto.getCommissionRate() != null ? new BigDecimal(insertDto.getCommissionRate()) : null)
                .averageShipmentSize(insertDto.getAverageShipmentSize() != null ? new BigDecimal(insertDto.getAverageShipmentSize()) : null)
                .build();
    }
}
