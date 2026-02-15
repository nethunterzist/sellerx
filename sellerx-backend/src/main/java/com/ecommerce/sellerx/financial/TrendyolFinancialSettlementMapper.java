package com.ecommerce.sellerx.financial;

import org.springframework.stereotype.Component;

@Component
public class TrendyolFinancialSettlementMapper {
    
    /**
     * Maps Trendyol API settlement item to our internal OrderItemSettlement DTO
     */
    public FinancialSettlement mapToOrderItemSettlement(TrendyolFinancialSettlementItem apiItem) {
        if (apiItem == null) {
            return null;
        }
        
        // Determine status based on transaction type
        String status = determineStatus(apiItem.getTransactionType());
        
        return FinancialSettlement.builder()
                .id(apiItem.getId())
                .barcode(apiItem.getBarcode())
                .transactionType(apiItem.getTransactionType())
                .status(status)
                .receiptId(apiItem.getReceiptId())
                .debt(apiItem.getDebt())
                .credit(apiItem.getCredit())
                .paymentPeriod(apiItem.getPaymentPeriod())
                .commissionRate(apiItem.getCommissionRate())
                .commissionAmount(apiItem.getCommissionAmount())
                .commissionInvoiceSerialNumber(apiItem.getCommissionInvoiceSerialNumber())
                .sellerRevenue(apiItem.getSellerRevenue())
                .paymentOrderId(apiItem.getPaymentOrderId())
                .shipmentPackageId(apiItem.getShipmentPackageId())
                .transactionDate(apiItem.getTransactionDate())
                .paymentDate(apiItem.getPaymentDate())
                .build();
    }
    
    /**
     * Determine status based on transaction type.
     * Supports all 17 Trendyol settlement transaction types.
     */
    private String determineStatus(String transactionType) {
        if (transactionType == null) {
            return "UNKNOWN";
        }
        return switch (transactionType) {
            case "Satış", "Sale" -> "SOLD";
            case "İade", "Return" -> "RETURNED";
            case "İptal", "Cancel" -> "CANCELLED";
            case "İndirim", "Discount" -> "DISCOUNT";
            case "Kupon", "Coupon" -> "COUPON";
            case "ErkenÖdeme", "EarlyPayment" -> "EARLY_PAYMENT";
            case "DiscountCancel" -> "DISCOUNT_CANCEL";
            case "CouponCancel" -> "COUPON_CANCEL";
            case "ManualRefund" -> "MANUAL_REFUND";
            case "ManualRefundCancel" -> "MANUAL_REFUND_CANCEL";
            case "TYDiscount" -> "TY_DISCOUNT";
            case "TYDiscountCancel" -> "TY_DISCOUNT_CANCEL";
            case "TYCoupon" -> "TY_COUPON";
            case "TYCouponCancel" -> "TY_COUPON_CANCEL";
            case "ProvisionPositive" -> "PROVISION_POSITIVE";
            case "ProvisionNegative" -> "PROVISION_NEGATIVE";
            case "CommissionPositive" -> "COMMISSION_POSITIVE";
            case "CommissionNegative" -> "COMMISSION_NEGATIVE";
            default -> "UNKNOWN";
        };
    }
}
