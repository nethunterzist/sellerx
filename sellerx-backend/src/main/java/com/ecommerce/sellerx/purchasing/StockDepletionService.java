package com.ecommerce.sellerx.purchasing;

import com.ecommerce.sellerx.products.CostAndStockInfo;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import com.ecommerce.sellerx.purchasing.dto.DepletedProductDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockDepletionService {

    private final TrendyolProductRepository productRepository;

    /**
     * Get all products with depleted stock for a store.
     */
    @Transactional(readOnly = true)
    public List<DepletedProductDto> getDepletedProducts(UUID storeId) {
        List<TrendyolProduct> depletedProducts = productRepository.findByStoreIdAndStockDepletedTrue(storeId);

        return depletedProducts.stream()
                .map(product -> DepletedProductDto.builder()
                        .productId(product.getId())
                        .productName(product.getTitle())
                        .barcode(product.getBarcode())
                        .productImage(product.getImage())
                        .lastStockDate(getLastStockDate(product))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get count of depleted products for a store.
     */
    @Transactional(readOnly = true)
    public int getDepletedProductCount(UUID storeId) {
        return productRepository.findByStoreIdAndStockDepletedTrue(storeId).size();
    }

    private LocalDate getLastStockDate(TrendyolProduct product) {
        if (product.getCostAndStockInfo() == null || product.getCostAndStockInfo().isEmpty()) {
            return null;
        }
        return product.getCostAndStockInfo().stream()
                .filter(s -> s.getStockDate() != null)
                .map(CostAndStockInfo::getStockDate)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }
}
