package com.ecommerce.sellerx.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductListResponse<T> {
    private Long totalElements;
    private Integer totalPages;
    private Integer currentPage;
    private Integer pageSize;
    private Boolean isFirst;
    private Boolean isLast;
    private Boolean hasNext;
    private Boolean hasPrevious;
    private List<T> products;
}
