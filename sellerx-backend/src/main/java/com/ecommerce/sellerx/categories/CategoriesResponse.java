package com.ecommerce.sellerx.categories;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoriesResponse {
    private List<TrendyolCategoryDto> result;
}
