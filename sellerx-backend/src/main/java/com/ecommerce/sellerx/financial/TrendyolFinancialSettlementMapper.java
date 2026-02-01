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
     * Determine status based on transaction type
     */
    private String determineStatus(String transactionType) {
        if ("Satış".equals(transactionType) || "Sale".equals(transactionType)) {
            return "SOLD";
        } else if ("İade".equals(transactionType) || "Return".equals(transactionType)) {
            return "RETURNED";
        } else if ("İptal".equals(transactionType) || "Cancel".equals(transactionType)) {
            return "CANCELLED";
        } else if ("İndirim".equals(transactionType) || "Discount".equals(transactionType)) {
            return "DISCOUNT";
        } else if ("Kupon".equals(transactionType) || "Coupon".equals(transactionType)) {
            return "COUPON";
        } else {
            return "UNKNOWN";
        }
    }
}
