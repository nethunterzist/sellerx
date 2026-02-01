package com.ecommerce.sellerx.financial;

import com.ecommerce.sellerx.common.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TrendyolFinancialSettlementMapper.
 * Tests mapping from Trendyol API settlement items to internal FinancialSettlement DTOs.
 */
@DisplayName("TrendyolFinancialSettlementMapper")
class TrendyolFinancialSettlementMapperTest extends BaseUnitTest {

    private TrendyolFinancialSettlementMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TrendyolFinancialSettlementMapper();
    }

    @Nested
    @DisplayName("mapToOrderItemSettlement")
    class MapToOrderItemSettlement {

        @Test
        @DisplayName("should return null for null input")
        void shouldReturnNullForNullInput() {
            // When
            FinancialSettlement result = mapper.mapToOrderItemSettlement(null);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should map Sale transaction with SOLD status")
        void shouldMapSaleTransactionWithSoldStatus() {
            // Given
            TrendyolFinancialSettlementItem item = TrendyolFinancialSettlementItem.builder()
                    .id("settlement-123")
                    .barcode("BARCODE-001")
                    .transactionType("Satış")
                    .receiptId(100001L)
                    .debt(BigDecimal.ZERO)
                    .credit(new BigDecimal("199.99"))
                    .paymentPeriod(14)
                    .commissionRate(new BigDecimal("15.00"))
                    .commissionAmount(new BigDecimal("30.00"))
                    .commissionInvoiceSerialNumber("INV-001")
                    .sellerRevenue(new BigDecimal("169.99"))
                    .paymentOrderId(500001L)
                    .country("TR")
                    .shipmentPackageId(1000001L)
                    .build();

            // When
            FinancialSettlement result = mapper.mapToOrderItemSettlement(item);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("settlement-123");
            assertThat(result.getBarcode()).isEqualTo("BARCODE-001");
            assertThat(result.getTransactionType()).isEqualTo("Satış");
            assertThat(result.getStatus()).isEqualTo("SOLD");
            assertThat(result.getCredit()).isEqualByComparingTo("199.99");
            assertThat(result.getCommissionRate()).isEqualByComparingTo("15.00");
            assertThat(result.getCommissionAmount()).isEqualByComparingTo("30.00");
            assertThat(result.getSellerRevenue()).isEqualByComparingTo("169.99");
        }

        @Test
        @DisplayName("should map English Sale transaction with SOLD status")
        void shouldMapEnglishSaleTransactionWithSoldStatus() {
            // Given
            TrendyolFinancialSettlementItem item = TrendyolFinancialSettlementItem.builder()
                    .id("settlement-124")
                    .barcode("BARCODE-002")
                    .transactionType("Sale")
                    .credit(new BigDecimal("299.99"))
                    .build();

            // When
            FinancialSettlement result = mapper.mapToOrderItemSettlement(item);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("SOLD");
        }

        @Test
        @DisplayName("should map Return transaction with RETURNED status")
        void shouldMapReturnTransactionWithReturnedStatus() {
            // Given
            TrendyolFinancialSettlementItem item = TrendyolFinancialSettlementItem.builder()
                    .id("settlement-125")
                    .barcode("BARCODE-003")
                    .transactionType("İade")
                    .debt(new BigDecimal("199.99"))
                    .credit(BigDecimal.ZERO)
                    .build();

            // When
            FinancialSettlement result = mapper.mapToOrderItemSettlement(item);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("RETURNED");
            assertThat(result.getTransactionType()).isEqualTo("İade");
        }

        @Test
        @DisplayName("should map English Return transaction with RETURNED status")
        void shouldMapEnglishReturnTransactionWithReturnedStatus() {
            // Given
            TrendyolFinancialSettlementItem item = TrendyolFinancialSettlementItem.builder()
                    .id("settlement-126")
                    .barcode("BARCODE-004")
                    .transactionType("Return")
                    .build();

            // When
            FinancialSettlement result = mapper.mapToOrderItemSettlement(item);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("RETURNED");
        }

        @Test
        @DisplayName("should map Discount transaction with DISCOUNT status")
        void shouldMapDiscountTransactionWithDiscountStatus() {
            // Given
            TrendyolFinancialSettlementItem item = TrendyolFinancialSettlementItem.builder()
                    .id("settlement-127")
                    .barcode("BARCODE-005")
                    .transactionType("İndirim")
                    .debt(new BigDecimal("20.00"))
                    .credit(BigDecimal.ZERO)
                    .commissionAmount(new BigDecimal("-3.00"))
                    .build();

            // When
            FinancialSettlement result = mapper.mapToOrderItemSettlement(item);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("DISCOUNT");
            assertThat(result.getDebt()).isEqualByComparingTo("20.00");
        }

        @Test
        @DisplayName("should map English Discount transaction with DISCOUNT status")
        void shouldMapEnglishDiscountTransactionWithDiscountStatus() {
            // Given
            TrendyolFinancialSettlementItem item = TrendyolFinancialSettlementItem.builder()
                    .id("settlement-128")
                    .barcode("BARCODE-006")
                    .transactionType("Discount")
                    .build();

            // When
            FinancialSettlement result = mapper.mapToOrderItemSettlement(item);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("DISCOUNT");
        }

        @Test
        @DisplayName("should map Coupon transaction with COUPON status")
        void shouldMapCouponTransactionWithCouponStatus() {
            // Given
            TrendyolFinancialSettlementItem item = TrendyolFinancialSettlementItem.builder()
                    .id("settlement-129")
                    .barcode("BARCODE-007")
                    .transactionType("Kupon")
                    .debt(new BigDecimal("50.00"))
                    .build();

            // When
            FinancialSettlement result = mapper.mapToOrderItemSettlement(item);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("COUPON");
        }

        @Test
        @DisplayName("should map English Coupon transaction with COUPON status")
        void shouldMapEnglishCouponTransactionWithCouponStatus() {
            // Given
            TrendyolFinancialSettlementItem item = TrendyolFinancialSettlementItem.builder()
                    .id("settlement-130")
                    .barcode("BARCODE-008")
                    .transactionType("Coupon")
                    .build();

            // When
            FinancialSettlement result = mapper.mapToOrderItemSettlement(item);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("COUPON");
        }

        @Test
        @DisplayName("should map Cancel transaction with CANCELLED status")
        void shouldMapCancelTransactionWithCancelledStatus() {
            // Given
            TrendyolFinancialSettlementItem item = TrendyolFinancialSettlementItem.builder()
                    .id("settlement-131")
                    .barcode("BARCODE-009")
                    .transactionType("İptal")
                    .build();

            // When
            FinancialSettlement result = mapper.mapToOrderItemSettlement(item);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("should map unknown transaction type with UNKNOWN status")
        void shouldMapUnknownTransactionTypeWithUnknownStatus() {
            // Given
            TrendyolFinancialSettlementItem item = TrendyolFinancialSettlementItem.builder()
                    .id("settlement-132")
                    .barcode("BARCODE-010")
                    .transactionType("Bilinmeyen")
                    .build();

            // When
            FinancialSettlement result = mapper.mapToOrderItemSettlement(item);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("UNKNOWN");
        }

        @Test
        @DisplayName("should map all fields correctly")
        void shouldMapAllFieldsCorrectly() {
            // Given
            TrendyolFinancialSettlementItem item = TrendyolFinancialSettlementItem.builder()
                    .id("settlement-full")
                    .barcode("BARCODE-FULL")
                    .transactionType("Satış")
                    .receiptId(999999L)
                    .debt(new BigDecimal("10.00"))
                    .credit(new BigDecimal("500.00"))
                    .paymentPeriod(7)
                    .commissionRate(new BigDecimal("12.50"))
                    .commissionAmount(new BigDecimal("62.50"))
                    .commissionInvoiceSerialNumber("INV-FULL-001")
                    .sellerRevenue(new BigDecimal("437.50"))
                    .paymentOrderId(888888L)
                    .country("TR")
                    .shipmentPackageId(777777L)
                    .build();

            // When
            FinancialSettlement result = mapper.mapToOrderItemSettlement(item);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("settlement-full");
            assertThat(result.getBarcode()).isEqualTo("BARCODE-FULL");
            assertThat(result.getTransactionType()).isEqualTo("Satış");
            assertThat(result.getStatus()).isEqualTo("SOLD");
            assertThat(result.getReceiptId()).isEqualTo(999999L);
            assertThat(result.getDebt()).isEqualByComparingTo("10.00");
            assertThat(result.getCredit()).isEqualByComparingTo("500.00");
            assertThat(result.getPaymentPeriod()).isEqualTo(7);
            assertThat(result.getCommissionRate()).isEqualByComparingTo("12.50");
            assertThat(result.getCommissionAmount()).isEqualByComparingTo("62.50");
            assertThat(result.getCommissionInvoiceSerialNumber()).isEqualTo("INV-FULL-001");
            assertThat(result.getSellerRevenue()).isEqualByComparingTo("437.50");
            assertThat(result.getPaymentOrderId()).isEqualTo(888888L);
            assertThat(result.getShipmentPackageId()).isEqualTo(777777L);
        }
    }
}
