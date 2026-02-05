package com.ecommerce.sellerx.purchasing;

import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.products.CostAndStockInfo;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import com.ecommerce.sellerx.purchasing.dto.DepletedProductDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("StockDepletionService")
class StockDepletionServiceTest extends BaseUnitTest {

    @Mock
    private TrendyolProductRepository productRepository;

    private StockDepletionService service;

    private UUID storeId;

    @BeforeEach
    void setUp() {
        service = new StockDepletionService(productRepository);
        storeId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("getDepletedProducts")
    class GetDepletedProducts {

        @Test
        @DisplayName("should return list of depleted products with correct fields")
        void shouldReturnDepletedProducts() {
            // Given
            TrendyolProduct product = TrendyolProduct.builder()
                    .id(UUID.randomUUID())
                    .title("Test Product")
                    .barcode("BARCODE-001")
                    .image("https://example.com/image.jpg")
                    .stockDepleted(true)
                    .costAndStockInfo(List.of(
                            CostAndStockInfo.builder()
                                    .stockDate(LocalDate.of(2025, 1, 15))
                                    .quantity(10)
                                    .unitCost(20.0)
                                    .usedQuantity(10)
                                    .build(),
                            CostAndStockInfo.builder()
                                    .stockDate(LocalDate.of(2025, 2, 1))
                                    .quantity(5)
                                    .unitCost(25.0)
                                    .usedQuantity(5)
                                    .build()
                    ))
                    .build();

            when(productRepository.findByStoreIdAndStockDepletedTrue(storeId))
                    .thenReturn(List.of(product));

            // When
            List<DepletedProductDto> result = service.getDepletedProducts(storeId);

            // Then
            assertThat(result).hasSize(1);
            DepletedProductDto dto = result.get(0);
            assertThat(dto.getProductId()).isEqualTo(product.getId());
            assertThat(dto.getProductName()).isEqualTo("Test Product");
            assertThat(dto.getBarcode()).isEqualTo("BARCODE-001");
            assertThat(dto.getProductImage()).isEqualTo("https://example.com/image.jpg");
            assertThat(dto.getLastStockDate()).isEqualTo(LocalDate.of(2025, 2, 1)); // Most recent
        }

        @Test
        @DisplayName("should return empty list when no depleted products exist")
        void shouldReturnEmptyListWhenNoDepleted() {
            when(productRepository.findByStoreIdAndStockDepletedTrue(storeId))
                    .thenReturn(Collections.emptyList());

            List<DepletedProductDto> result = service.getDepletedProducts(storeId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return multiple depleted products")
        void shouldReturnMultipleDepletedProducts() {
            TrendyolProduct product1 = TrendyolProduct.builder()
                    .id(UUID.randomUUID())
                    .title("Product A")
                    .barcode("BC-001")
                    .stockDepleted(true)
                    .costAndStockInfo(List.of(
                            CostAndStockInfo.builder()
                                    .stockDate(LocalDate.of(2025, 1, 1))
                                    .quantity(10)
                                    .unitCost(10.0)
                                    .usedQuantity(10)
                                    .build()
                    ))
                    .build();

            TrendyolProduct product2 = TrendyolProduct.builder()
                    .id(UUID.randomUUID())
                    .title("Product B")
                    .barcode("BC-002")
                    .stockDepleted(true)
                    .costAndStockInfo(List.of(
                            CostAndStockInfo.builder()
                                    .stockDate(LocalDate.of(2025, 3, 15))
                                    .quantity(20)
                                    .unitCost(30.0)
                                    .usedQuantity(20)
                                    .build()
                    ))
                    .build();

            when(productRepository.findByStoreIdAndStockDepletedTrue(storeId))
                    .thenReturn(List.of(product1, product2));

            List<DepletedProductDto> result = service.getDepletedProducts(storeId);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(DepletedProductDto::getBarcode)
                    .containsExactly("BC-001", "BC-002");
        }

        @Test
        @DisplayName("should return null lastStockDate when product has no stock info")
        void shouldReturnNullLastStockDateWhenNoStockInfo() {
            TrendyolProduct product = TrendyolProduct.builder()
                    .id(UUID.randomUUID())
                    .title("No Stock Product")
                    .barcode("BC-EMPTY")
                    .stockDepleted(true)
                    .costAndStockInfo(Collections.emptyList())
                    .build();

            when(productRepository.findByStoreIdAndStockDepletedTrue(storeId))
                    .thenReturn(List.of(product));

            List<DepletedProductDto> result = service.getDepletedProducts(storeId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getLastStockDate()).isNull();
        }
    }

    @Nested
    @DisplayName("getDepletedProductCount")
    class GetDepletedProductCount {

        @Test
        @DisplayName("should return count of depleted products")
        void shouldReturnCount() {
            TrendyolProduct p1 = TrendyolProduct.builder().id(UUID.randomUUID()).stockDepleted(true).build();
            TrendyolProduct p2 = TrendyolProduct.builder().id(UUID.randomUUID()).stockDepleted(true).build();

            when(productRepository.findByStoreIdAndStockDepletedTrue(storeId))
                    .thenReturn(List.of(p1, p2));

            int count = service.getDepletedProductCount(storeId);

            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("should return zero when no depleted products")
        void shouldReturnZeroWhenNone() {
            when(productRepository.findByStoreIdAndStockDepletedTrue(storeId))
                    .thenReturn(Collections.emptyList());

            int count = service.getDepletedProductCount(storeId);

            assertThat(count).isZero();
        }
    }
}
