package com.ecommerce.sellerx.financial;

import com.ecommerce.sellerx.common.exception.ResourceNotFoundException;
import com.ecommerce.sellerx.financial.dto.AggregatedProductDto;
import com.ecommerce.sellerx.financial.dto.PurchaseVatDto;
import com.ecommerce.sellerx.financial.dto.SalesVatDto;
import com.ecommerce.sellerx.financial.dto.CargoInvoiceItemDto;
import com.ecommerce.sellerx.financial.dto.CargoInvoiceItemsPageDto;
import com.ecommerce.sellerx.financial.dto.CommissionInvoiceItemDto;
import com.ecommerce.sellerx.financial.dto.CommissionInvoiceItemsPageDto;
import com.ecommerce.sellerx.financial.dto.InvoiceDetailDto;
import com.ecommerce.sellerx.financial.dto.InvoiceItemDto;
import com.ecommerce.sellerx.financial.dto.InvoiceItemsPageDto;
import com.ecommerce.sellerx.financial.dto.InvoiceSummaryDto;
import com.ecommerce.sellerx.financial.dto.InvoiceTypeStatsDto;
import com.ecommerce.sellerx.financial.dto.OrderInvoiceItemsDto;
import com.ecommerce.sellerx.financial.dto.ProductCargoBreakdownDto;
import com.ecommerce.sellerx.financial.dto.ProductCommissionBreakdownDto;
import com.ecommerce.sellerx.financial.dto.ProductExpenseBreakdownDto;
import com.ecommerce.sellerx.financial.dto.TransactionTypeBreakdownDto;
import com.ecommerce.sellerx.financial.dto.StoppageDto;
import com.ecommerce.sellerx.financial.dto.StoppageSummaryDto;
import com.ecommerce.sellerx.orders.OrderItem;
import com.ecommerce.sellerx.orders.TrendyolOrder;
import com.ecommerce.sellerx.orders.TrendyolOrderRepository;
import com.ecommerce.sellerx.purchasing.PurchaseOrder;
import com.ecommerce.sellerx.purchasing.PurchaseOrderItem;
import com.ecommerce.sellerx.purchasing.PurchaseOrderRepository;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing and querying invoice data.
 * Provides summary statistics and detailed invoice information for the dashboard.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrendyolInvoiceService {

    private final TrendyolDeductionInvoiceRepository deductionInvoiceRepository;
    private final TrendyolInvoiceRepository invoiceRepository;
    private final TrendyolCargoInvoiceRepository cargoInvoiceRepository;
    private final TrendyolStoppageRepository stoppageRepository;
    private final TrendyolOrderRepository orderRepository;
    private final TrendyolProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    // Invoice type to category mapping
    private static final Map<String, String> TYPE_TO_CATEGORY = Map.ofEntries(
            // Platform Ücretleri (Platform Hizmet Bedelleri)
            Map.entry("Platform Hizmet Bedeli", "PLATFORM_UCRETLERI"),
            Map.entry("AZ-Platform Hizmet Bedeli", "PLATFORM_UCRETLERI"),

            // Komisyon
            Map.entry("AZ - Komisyon Faturası", "KOMISYON"),
            Map.entry("AZ-Komisyon Geliri", "KOMISYON"),
            Map.entry("Komisyon Faturası", "KOMISYON"),

            // Kargo
            Map.entry("Kargo Fatura", "KARGO"),
            Map.entry("Kargo Faturası", "KARGO"),
            Map.entry("AZ - Kargo Fatura", "KARGO"),
            Map.entry("MP Kargo İtiraz İade Faturası", "KARGO"),

            // Uluslararasi (kesinti olarak faturalandırılan uluslararası hizmetler)
            Map.entry("Uluslararası Hizmet Bedeli", "ULUSLARARASI"),
            Map.entry("AZ-Uluslararası Hizmet Bedeli", "ULUSLARARASI"),
            Map.entry("Yurt Dışı Operasyon Bedeli", "ULUSLARARASI"),

            // Uluslararasi - AZ-Yurtdışı Operasyon (kesinti olarak faturalandırılan)
            Map.entry("AZ-Yurtdışı Operasyon Bedeli", "ULUSLARARASI"),
            Map.entry("AZ-YURTDÕ_Õ OPERASYON BEDELI %18", "ULUSLARARASI"),

            // Iade - Geri yatan ödemeler (refund/compensation to seller)
            Map.entry("Yurtdışı Operasyon Iade Bedeli", "IADE"),

            // Ceza
            Map.entry("Tedarik Edememe", "CEZA"),
            Map.entry("TEDARIK EDEMEME FATURASI", "CEZA"),
            Map.entry("Termin Gecikme Bedeli", "CEZA"),
            Map.entry("Eksik Ürün Faturası", "CEZA"),
            Map.entry("Yanlış Ürün Faturası", "CEZA"),
            Map.entry("YANLIS URUN FATURASI", "CEZA"),
            Map.entry("Kusurlu Ürün Faturası", "CEZA"),
            Map.entry("Teslim Kontrol Faturası", "CEZA"),

            // Reklam
            Map.entry("Reklam Bedeli", "REKLAM"),
            Map.entry("Sabit Bütçeli Influencer Reklam Bedeli", "REKLAM"),
            Map.entry("Komisyonlu İnfluencer Reklam Bedeli", "REKLAM"),
            Map.entry("Komisyonlu Influencer Reklam Bedeli", "REKLAM"),  // ASCII 'I' variant

            // Iade (Geri yatan ödeme - refund/compensation)
            Map.entry("Tazmin Faturası", "IADE"),

            // Hizmet Bedeli (Service Fees - Depo, Paketleme, Çağrı Merkezi, etc.)
            Map.entry("Depo Hizmet Bedeli", "HIZMET_BEDELI"),
            Map.entry("Paketleme Hizmet Bedeli", "HIZMET_BEDELI"),
            Map.entry("Çağrı Merkezi Hizmet Bedeli", "HIZMET_BEDELI"),
            Map.entry("Fotoğraf Hizmet Bedeli", "HIZMET_BEDELI"),
            Map.entry("Entegrasyon Hizmet Bedeli", "HIZMET_BEDELI"),
            Map.entry("Depolama Hizmet Bedeli", "HIZMET_BEDELI"),
            Map.entry("Fulfilment Hizmet Bedeli", "HIZMET_BEDELI"),
            Map.entry("Paketleme Hizmeti Bedeli", "HIZMET_BEDELI"),
            Map.entry("Depo Hizmeti Bedeli", "HIZMET_BEDELI"),

            // Reklam/Kampanya
            Map.entry("Kurumsal Kampanya Yansıtma Bedeli", "REKLAM"),

            // Diger
            Map.entry("Erken Ödeme Kesinti Faturası", "DIGER"),
            Map.entry("Fatura Kontör Satış Bedeli", "DIGER"),
            Map.entry("Müşteri Duyuruları Faturası", "DIGER"),
            Map.entry("TEX Tazmin - İşleme- %0", "DIGER")
    );

    // Invoice type to code mapping
    private static final Map<String, String> TYPE_TO_CODE = Map.ofEntries(
            Map.entry("Platform Hizmet Bedeli", "PLATFORM_HIZMET"),
            Map.entry("AZ - Komisyon Faturası", "AZ_KOMISYON"),
            Map.entry("AZ-Komisyon Geliri", "AZ_KOMISYON_GELIRI"),
            Map.entry("Komisyon Faturası", "KOMISYON_FATURASI"),
            Map.entry("Kargo Fatura", "KARGO_FATURA"),
            Map.entry("Kargo Faturası", "KARGO_FATURA"),
            Map.entry("AZ - Kargo Fatura", "AZ_KARGO"),
            Map.entry("MP Kargo İtiraz İade Faturası", "KARGO_ITIRAZ_IADE"),
            Map.entry("Uluslararası Hizmet Bedeli", "ULUSLARARASI_HIZMET"),
            Map.entry("AZ-Yurtdışı Operasyon Bedeli", "AZ_YURTDISI_OPERASYON"),
            Map.entry("AZ-YURTDÕ_Õ OPERASYON BEDELI %18", "AZ_YURTDISI_OPERASYON"),
            Map.entry("AZ-Platform Hizmet Bedeli", "AZ_PLATFORM_HIZMET"),
            Map.entry("AZ-Uluslararası Hizmet Bedeli", "AZ_ULUSLARARASI_HIZMET"),
            Map.entry("Yurtdışı Operasyon Iade Bedeli", "YURTDISI_OPERASYON_IADE"),
            Map.entry("Yurt Dışı Operasyon Bedeli", "YURTDISI_OPERASYON"),
            Map.entry("Tedarik Edememe", "TEDARIK_EDEMEME"),
            Map.entry("TEDARIK EDEMEME FATURASI", "TEDARIK_EDEMEME"),
            Map.entry("Termin Gecikme Bedeli", "TERMIN_GECIKME"),
            Map.entry("Eksik Ürün Faturası", "EKSIK_URUN"),
            Map.entry("Yanlış Ürün Faturası", "YANLIS_URUN"),
            Map.entry("YANLIS URUN FATURASI", "YANLIS_URUN"),
            Map.entry("Kusurlu Ürün Faturası", "KUSURLU_URUN"),
            Map.entry("Reklam Bedeli", "REKLAM_BEDELI"),
            Map.entry("Sabit Bütçeli Influencer Reklam Bedeli", "INFLUENCER_SABIT"),
            Map.entry("Komisyonlu İnfluencer Reklam Bedeli", "INFLUENCER_KOMISYON"),
            Map.entry("Komisyonlu Influencer Reklam Bedeli", "INFLUENCER_KOMISYON"),  // ASCII 'I' variant
            Map.entry("Teslim Kontrol Faturası", "TESLIM_KONTROL"),
            Map.entry("Kurumsal Kampanya Yansıtma Bedeli", "KURUMSAL_KAMPANYA"),
            Map.entry("Erken Ödeme Kesinti Faturası", "ERKEN_ODEME_KESINTI"),
            Map.entry("Fatura Kontör Satış Bedeli", "KONTOR_SATIS"),
            Map.entry("Müşteri Duyuruları Faturası", "MUSTERI_DUYURU"),
            Map.entry("TEX Tazmin - İşleme- %0", "TEX_TAZMIN"),
            Map.entry("Tazmin Faturası", "TAZMIN_FATURASI"),

            // Hizmet Bedeli (Service Fees)
            Map.entry("Depo Hizmet Bedeli", "DEPO_HIZMET"),
            Map.entry("Paketleme Hizmet Bedeli", "PAKETLEME_HIZMET"),
            Map.entry("Çağrı Merkezi Hizmet Bedeli", "CAGRI_MERKEZI_HIZMET"),
            Map.entry("Fotoğraf Hizmet Bedeli", "FOTOGRAF_HIZMET"),
            Map.entry("Entegrasyon Hizmet Bedeli", "ENTEGRASYON_HIZMET"),
            Map.entry("Depolama Hizmet Bedeli", "DEPOLAMA_HIZMET"),
            Map.entry("Fulfilment Hizmet Bedeli", "FULFILMENT_HIZMET"),
            Map.entry("Paketleme Hizmeti Bedeli", "PAKETLEME_HIZMETI"),
            Map.entry("Depo Hizmeti Bedeli", "DEPO_HIZMETI")
    );

    /**
     * Get invoice summary for a store within a date range.
     * Groups invoices by type and provides totals.
     */
    @Transactional(readOnly = true)
    public InvoiceSummaryDto getInvoiceSummary(UUID storeId, LocalDate startDate, LocalDate endDate) {
        log.info("Getting invoice summary for store: {} from {} to {}", storeId, startDate, endDate);

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        // Get all deduction invoices for the period
        List<TrendyolDeductionInvoice> invoices = deductionInvoiceRepository
                .findByStoreIdAndTransactionDateBetween(storeId, startDateTime, endDateTime);

        // Group by transaction type and calculate stats
        Map<String, InvoiceTypeStatsDto> typeStats = new LinkedHashMap<>();
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal totalRefunds = BigDecimal.ZERO;

        for (TrendyolDeductionInvoice invoice : invoices) {
            String typeName = invoice.getTransactionType();
            if (typeName == null) continue;

            String typeCode = TYPE_TO_CODE.getOrDefault(typeName, normalizeTypeCode(typeName));
            String category = TYPE_TO_CATEGORY.getOrDefault(typeName, "DIGER");

            // Check if this is a refund:
            // 1. credit > 0 means direct refund from Trendyol
            // 2. transaction type containing "iade" or "İade" is also a refund
            //    (Trendyol sends these as debt but they're actually refunds to seller)
            boolean hasCreditValue = invoice.getCredit() != null && invoice.getCredit().compareTo(BigDecimal.ZERO) > 0;
            boolean isRefundByType = isRefundTransactionType(invoice.getTransactionType());
            boolean isRefund = hasCreditValue || isRefundByType;

            BigDecimal amount;
            if (isRefund) {
                // For refunds: prefer credit if available, otherwise use debt
                // (Trendyol sends some refunds as positive debt instead of credit)
                if (hasCreditValue) {
                    amount = invoice.getCredit();
                } else {
                    amount = invoice.getDebt() != null ? invoice.getDebt() : BigDecimal.ZERO;
                }
                totalRefunds = totalRefunds.add(amount);
            } else {
                amount = invoice.getDebt() != null ? invoice.getDebt() : BigDecimal.ZERO;
                totalDeductions = totalDeductions.add(amount);
            }

            // Create final copies for use in lambda expression
            final BigDecimal finalAmount = amount;
            // Override category to IADE for refund types (same as convertToDetailDto)
            final String finalCategory = isRefund ? "IADE" : category;
            final String finalTransactionType = typeName;
            final boolean finalIsDeduction = !isRefund;

            // Use composite key to separate deductions and refunds for the same type
            String groupKey = typeCode + (isRefund ? "_REFUND" : "_DEDUCTION");

            // Create final copy of typeCode for use in lambda
            final String finalTypeCode = typeCode;

            typeStats.compute(groupKey, (k, existing) -> {
                if (existing == null) {
                    return InvoiceTypeStatsDto.builder()
                            .invoiceTypeCode(finalTypeCode)
                            .invoiceType(typeName)
                            .invoiceCategory(finalCategory)
                            .isDeduction(finalIsDeduction)
                            .invoiceCount(1)
                            .totalAmount(finalAmount)
                            .totalVatAmount(calculateVat(finalAmount, finalCategory, finalTransactionType, finalTypeCode))
                            .vatRate(getVatRate(finalCategory, finalTransactionType, finalTypeCode))
                            .build();
                } else {
                    existing.setInvoiceCount(existing.getInvoiceCount() + 1);
                    existing.setTotalAmount(existing.getTotalAmount().add(finalAmount));
                    existing.setTotalVatAmount(existing.getTotalVatAmount().add(calculateVat(finalAmount, finalCategory, finalTransactionType, finalTypeCode)));
                    return existing;
                }
            });
        }

        // Sort by total amount descending
        List<InvoiceTypeStatsDto> sortedStats = typeStats.values().stream()
                .sorted((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()))
                .collect(Collectors.toList());

        // Calculate category summaries
        Map<String, InvoiceSummaryDto.CategorySummaryDto> categorySummaries = new LinkedHashMap<>();
        for (InvoiceTypeStatsDto stat : sortedStats) {
            categorySummaries.compute(stat.getInvoiceCategory(), (k, existing) -> {
                if (existing == null) {
                    return InvoiceSummaryDto.CategorySummaryDto.builder()
                            .category(stat.getInvoiceCategory())
                            .invoiceCount(stat.getInvoiceCount())
                            .totalAmount(stat.getTotalAmount())
                            .totalVatAmount(stat.getTotalVatAmount())
                            .build();
                } else {
                    existing.setInvoiceCount(existing.getInvoiceCount() + stat.getInvoiceCount());
                    existing.setTotalAmount(existing.getTotalAmount().add(stat.getTotalAmount()));
                    existing.setTotalVatAmount(existing.getTotalVatAmount().add(stat.getTotalVatAmount()));
                    return existing;
                }
            });
        }

        // Calculate Purchase VAT from CLOSED purchase orders by stockEntryDate
        // Per Turkish VAT law, input VAT is deductible in the month of purchase/stock entry
        List<PurchaseOrder> closedPOs = purchaseOrderRepository.findClosedWithItemsByStoreId(storeId);

        BigDecimal totalPurchaseCostExclVat = BigDecimal.ZERO;
        BigDecimal totalPurchaseVat = BigDecimal.ZERO;
        int totalItemsPurchased = 0;
        Map<Integer, BigDecimal[]> purchaseVatByRateMap = new LinkedHashMap<>(); // vatRate -> [costAmount, vatAmount, itemCount]

        for (PurchaseOrder po : closedPOs) {
            if (po.getItems() == null) continue;
            for (PurchaseOrderItem item : po.getItems()) {
                // Determine effective stock entry date: item > PO > poDate
                LocalDate effectiveDate = item.getStockEntryDate();
                if (effectiveDate == null) effectiveDate = po.getStockEntryDate();
                if (effectiveDate == null) effectiveDate = po.getPoDate();

                // Filter by period
                if (effectiveDate.isBefore(startDate) || effectiveDate.isAfter(endDate)) continue;

                int units = item.getUnitsOrdered() != null ? item.getUnitsOrdered() : 0;
                if (units <= 0) continue;

                // totalCostPerUnit is a computed DB column (manufacturing + transportation), KDV-exclusive
                BigDecimal costPerUnit = item.getTotalCostPerUnit();
                if (costPerUnit == null || costPerUnit.compareTo(BigDecimal.ZERO) <= 0) continue;

                BigDecimal lineCost = costPerUnit.multiply(BigDecimal.valueOf(units));
                int vatRate = item.getCostVatRate() != null ? item.getCostVatRate() : 0;
                BigDecimal lineVat = vatRate > 0
                        ? lineCost.multiply(BigDecimal.valueOf(vatRate))
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                totalPurchaseCostExclVat = totalPurchaseCostExclVat.add(lineCost);
                totalPurchaseVat = totalPurchaseVat.add(lineVat);
                totalItemsPurchased += units;

                // Accumulate by rate
                purchaseVatByRateMap.compute(vatRate, (k, arr) -> {
                    if (arr == null) arr = new BigDecimal[]{ BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO };
                    arr[0] = arr[0].add(lineCost);
                    arr[1] = arr[1].add(lineVat);
                    arr[2] = arr[2].add(BigDecimal.valueOf(units));
                    return arr;
                });
            }
        }

        List<PurchaseVatDto.PurchaseVatByRate> purchaseVatByRate = purchaseVatByRateMap.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .map(e -> PurchaseVatDto.PurchaseVatByRate.builder()
                        .vatRate(e.getKey())
                        .costAmount(e.getValue()[0])
                        .vatAmount(e.getValue()[1])
                        .itemCount(e.getValue()[2].intValue())
                        .build())
                .collect(Collectors.toList());

        PurchaseVatDto purchaseVatDto = PurchaseVatDto.builder()
                .totalPurchaseCostExclVat(totalPurchaseCostExclVat)
                .totalPurchaseVatAmount(totalPurchaseVat)
                .totalItemsPurchased(totalItemsPurchased)
                .byRate(purchaseVatByRate)
                .build();

        // Calculate Sales VAT (Satış KDV'si) from delivered orders
        List<TrendyolOrder> deliveredOrders = orderRepository
                .findByStoreIdAndOrderDateBetweenAndStatusIn(
                        storeId, startDateTime, endDateTime,
                        List.of("Delivered"));

        // Build barcode -> product map (full object instead of just VAT rate)
        Map<String, TrendyolProduct> barcodeToProduct = productRepository.findByStoreId(storeId).stream()
                .filter(p -> p.getBarcode() != null)
                .collect(Collectors.toMap(TrendyolProduct::getBarcode, p -> p, (a, b) -> a));

        Map<Integer, BigDecimal[]> salesVatByRateMap = new LinkedHashMap<>(); // vatRate -> [salesAmount, vatAmount, itemCount]
        BigDecimal totalSalesAmount = BigDecimal.ZERO;
        BigDecimal totalSalesVat = BigDecimal.ZERO;
        int totalSalesItemCount = 0;
        int salesItemsWithoutVatRate = 0;

        for (TrendyolOrder order : deliveredOrders) {
            if (order.getOrderItems() == null) continue;
            for (OrderItem item : order.getOrderItems()) {
                int qty = item.getQuantity() != null ? item.getQuantity() : 1;

                // Look up VAT rate from product catalog by barcode
                TrendyolProduct product = (item.getBarcode() != null) ? barcodeToProduct.get(item.getBarcode()) : null;
                Integer vatRate = (product != null && product.getVatRate() != null) ? product.getVatRate() : null;
                if (vatRate == null || vatRate <= 0) {
                    salesItemsWithoutVatRate += qty;
                    continue;
                }

                // Calculate sales amount from vatBaseAmount (already includes qty, no need to multiply)
                // Note: vatBaseAmount is always the TOTAL amount for all items in the line
                BigDecimal itemSalesAmount = (item.getVatBaseAmount() != null)
                        ? item.getVatBaseAmount()
                        : BigDecimal.ZERO;

                // VAT = vatBaseAmount * vatRate / 100
                // Note: DO NOT multiply by qty - vatBaseAmount already contains the total
                BigDecimal itemVatAmount = BigDecimal.ZERO;
                if (item.getVatBaseAmount() != null && vatRate > 0) {
                    itemVatAmount = item.getVatBaseAmount()
                            .multiply(BigDecimal.valueOf(vatRate))
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                }

                totalSalesAmount = totalSalesAmount.add(itemSalesAmount);
                totalSalesVat = totalSalesVat.add(itemVatAmount);
                totalSalesItemCount += qty;

                salesVatByRateMap.merge(vatRate, new BigDecimal[]{itemSalesAmount, itemVatAmount, BigDecimal.valueOf(qty)},
                        (existing, newVal) -> new BigDecimal[]{
                                existing[0].add(newVal[0]),
                                existing[1].add(newVal[1]),
                                existing[2].add(newVal[2])
                        });
            }
        }

        // Product-based grouping for detailed breakdown - UPDATED to include image, brand, productUrl
        Map<String, Object[]> productSalesVatMap = new LinkedHashMap<>();
        // barcode -> [productName, quantity, salesAmount, vatAmount, vatRate, image, brand, productUrl]

        for (TrendyolOrder order : deliveredOrders) {
            if (order.getOrderItems() == null) continue;
            for (OrderItem item : order.getOrderItems()) {
                String barcode = item.getBarcode();
                if (barcode == null) continue;

                int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                TrendyolProduct product = barcodeToProduct.get(barcode);  // CHANGED: get full product
                Integer vatRate = (product != null && product.getVatRate() != null) ? product.getVatRate() : null;
                if (vatRate == null || vatRate <= 0) continue;

                BigDecimal itemSalesAmount = (item.getVatBaseAmount() != null)
                        ? item.getVatBaseAmount()
                        : BigDecimal.ZERO;

                BigDecimal itemVatAmount = BigDecimal.ZERO;
                if (item.getVatBaseAmount() != null && vatRate > 0) {
                    itemVatAmount = item.getVatBaseAmount()
                            .multiply(BigDecimal.valueOf(vatRate))
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                }

                String productName = item.getProductName() != null ? item.getProductName() : barcode;
                // NEW: Extract image, brand, productUrl from product
                String image = (product != null) ? product.getImage() : null;
                String brand = (product != null) ? product.getBrand() : null;
                String productUrl = (product != null) ? product.getProductUrl() : null;

                productSalesVatMap.merge(barcode,
                        new Object[]{productName, qty, itemSalesAmount, itemVatAmount, vatRate, image, brand, productUrl},
                        (existing, newVal) -> new Object[]{
                                existing[0], // keep first productName
                                (int)existing[1] + (int)newVal[1], // sum qty
                                ((BigDecimal)existing[2]).add((BigDecimal)newVal[2]), // sum salesAmount
                                ((BigDecimal)existing[3]).add((BigDecimal)newVal[3]), // sum vatAmount
                                existing[4], // keep vatRate
                                existing[5], // keep image
                                existing[6], // keep brand
                                existing[7]  // keep productUrl
                        });
            }
        }

        // Convert to DTO list, sorted by salesAmount descending - UPDATED with new fields
        List<SalesVatDto.ProductSalesVatDto> productSalesVatList = productSalesVatMap.entrySet().stream()
                .sorted((a, b) -> ((BigDecimal)b.getValue()[2]).compareTo((BigDecimal)a.getValue()[2]))
                .map(entry -> SalesVatDto.ProductSalesVatDto.builder()
                        .barcode(entry.getKey())
                        .productName((String)entry.getValue()[0])
                        .quantity((Integer)entry.getValue()[1])
                        .salesAmount((BigDecimal)entry.getValue()[2])
                        .vatAmount((BigDecimal)entry.getValue()[3])
                        .vatRate((Integer)entry.getValue()[4])
                        .image((String)entry.getValue()[5])        // NEW
                        .brand((String)entry.getValue()[6])        // NEW
                        .productUrl((String)entry.getValue()[7])   // NEW
                        .build())
                .collect(Collectors.toList());

        List<SalesVatDto.SalesVatByRateDto> salesVatByRateList = salesVatByRateMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> SalesVatDto.SalesVatByRateDto.builder()
                        .vatRate(entry.getKey())
                        .salesAmount(entry.getValue()[0])
                        .vatAmount(entry.getValue()[1])
                        .itemCount(entry.getValue()[2].intValue())
                        .build())
                .collect(Collectors.toList());

        SalesVatDto salesVatDto = SalesVatDto.builder()
                .totalSalesAmount(totalSalesAmount)
                .totalVatAmount(totalSalesVat)
                .totalItemsSold(totalSalesItemCount)
                .itemsWithoutVatRate(salesItemsWithoutVatRate)
                .byRate(salesVatByRateList)
                .byProduct(productSalesVatList)
                .build();

        // Calculate Stoppage (Stopaj/Tevkifat) summary - siparişlerden hesaplanan %1 değer
        BigDecimal totalStoppageAmount = orderRepository.sumStoppageByStoreAndDateRange(storeId, startDateTime, endDateTime);
        int stoppageCount = orderRepository.countOrdersWithStoppage(storeId, startDateTime, endDateTime);

        return InvoiceSummaryDto.builder()
                .storeId(storeId.toString())
                .periodStart(startDate)
                .periodEnd(endDate)
                .totalDeductions(totalDeductions)
                .totalRefunds(totalRefunds)
                .netAmount(totalRefunds.subtract(totalDeductions))
                .totalInvoiceCount(invoices.size())
                .invoicesByType(sortedStats)
                .invoicesByCategory(new ArrayList<>(categorySummaries.values()))
                .purchaseVat(purchaseVatDto)
                .salesVat(salesVatDto)
                .totalStoppageAmount(totalStoppageAmount != null ? totalStoppageAmount : BigDecimal.ZERO)
                .stoppageCount(stoppageCount)
                .build();
    }

    /**
     * Get invoices by type code with pagination.
     */
    @Transactional(readOnly = true)
    public Page<InvoiceDetailDto> getInvoicesByType(UUID storeId, String typeCode,
                                                     LocalDate startDate, LocalDate endDate,
                                                     Pageable pageable) {
        log.info("Getting invoices by type {} for store: {} from {} to {}", typeCode, storeId, startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        // Get the original type name for the code
        String typeName = getTypeNameFromCode(typeCode);

        // Fetch from deduction invoices
        List<TrendyolDeductionInvoice> allInvoices = deductionInvoiceRepository
                .findByStoreIdAndTransactionDateBetween(storeId, startDateTime, endDateTime);

        // Filter by type
        List<TrendyolDeductionInvoice> filteredInvoices = allInvoices.stream()
                .filter(inv -> {
                    String invTypeCode = TYPE_TO_CODE.getOrDefault(inv.getTransactionType(),
                            normalizeTypeCode(inv.getTransactionType()));
                    return typeCode.equals(invTypeCode);
                })
                .sorted((a, b) -> b.getTransactionDate().compareTo(a.getTransactionDate()))
                .collect(Collectors.toList());

        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filteredInvoices.size());

        List<InvoiceDetailDto> pageContent = start < filteredInvoices.size()
                ? filteredInvoices.subList(start, end).stream()
                    .map(this::convertToDetailDto)
                    .collect(Collectors.toList())
                : Collections.emptyList();

        return new PageImpl<>(pageContent, pageable, filteredInvoices.size());
    }

    /**
     * Get invoices by category with pagination.
     */
    @Transactional(readOnly = true)
    public Page<InvoiceDetailDto> getInvoicesByCategory(UUID storeId, String category,
                                                         LocalDate startDate, LocalDate endDate,
                                                         Pageable pageable) {
        log.info("Getting invoices by category {} for store: {} from {} to {}", category, storeId, startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        List<TrendyolDeductionInvoice> allInvoices = deductionInvoiceRepository
                .findByStoreIdAndTransactionDateBetween(storeId, startDateTime, endDateTime);

        // Filter by category
        // Special handling for "IADE" category - use isRefundTransactionType() logic
        // because refund types may be mapped to different categories in TYPE_TO_CATEGORY
        // For non-IADE categories, exclude refunds (they should only appear in IADE)
        List<TrendyolDeductionInvoice> filteredInvoices = allInvoices.stream()
                .filter(inv -> {
                    // Check if this invoice is a refund
                    boolean hasCreditValue = inv.getCredit() != null && inv.getCredit().compareTo(BigDecimal.ZERO) > 0;
                    boolean isRefundByType = isRefundTransactionType(inv.getTransactionType());
                    boolean isRefund = hasCreditValue || isRefundByType;

                    if ("IADE".equals(category)) {
                        // For IADE category, include only refunds
                        return isRefund;
                    } else {
                        // For other categories:
                        // 1. Exclude refunds (they belong to IADE)
                        // 2. Check if category matches
                        if (isRefund) {
                            return false; // Refunds should only appear in IADE
                        }
                        String invCategory = TYPE_TO_CATEGORY.getOrDefault(inv.getTransactionType(), "DIGER");
                        return category.equals(invCategory);
                    }
                })
                .sorted((a, b) -> b.getTransactionDate().compareTo(a.getTransactionDate()))
                .collect(Collectors.toList());

        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filteredInvoices.size());

        List<InvoiceDetailDto> pageContent = start < filteredInvoices.size()
                ? filteredInvoices.subList(start, end).stream()
                    .map(this::convertToDetailDto)
                    .collect(Collectors.toList())
                : Collections.emptyList();

        return new PageImpl<>(pageContent, pageable, filteredInvoices.size());
    }

    /**
     * Get all invoices with pagination (for overview).
     */
    @Transactional(readOnly = true)
    public Page<InvoiceDetailDto> getAllInvoices(UUID storeId, LocalDate startDate, LocalDate endDate,
                                                  Pageable pageable) {
        log.info("Getting all invoices for store: {} from {} to {}", storeId, startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        List<TrendyolDeductionInvoice> allInvoices = deductionInvoiceRepository
                .findByStoreIdAndTransactionDateBetween(storeId, startDateTime, endDateTime);

        // Sort by date descending
        List<TrendyolDeductionInvoice> sortedInvoices = allInvoices.stream()
                .sorted((a, b) -> b.getTransactionDate().compareTo(a.getTransactionDate()))
                .collect(Collectors.toList());

        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), sortedInvoices.size());

        List<InvoiceDetailDto> pageContent = start < sortedInvoices.size()
                ? sortedInvoices.subList(start, end).stream()
                    .map(this::convertToDetailDto)
                    .collect(Collectors.toList())
                : Collections.emptyList();

        return new PageImpl<>(pageContent, pageable, sortedInvoices.size());
    }

    /**
     * Get cargo invoice items by invoice serial number.
     * Returns all shipments/orders within a specific cargo invoice.
     * Enriches items with product name and image from TrendyolProduct table.
     */
    @Transactional(readOnly = true)
    public List<CargoInvoiceItemDto> getCargoInvoiceItems(UUID storeId, String invoiceSerialNumber) {
        log.info("Getting cargo invoice items for store: {}, invoice: {}", storeId, invoiceSerialNumber);

        // Verify store exists
        storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));

        List<TrendyolCargoInvoice> items = cargoInvoiceRepository
                .findByStoreIdAndInvoiceSerialNumberOrderByInvoiceDateDescCreatedAtDesc(storeId, invoiceSerialNumber);

        log.info("Found {} cargo invoice items for invoice: {}", items.size(), invoiceSerialNumber);

        // Convert to DTOs first
        List<CargoInvoiceItemDto> dtos = items.stream()
                .map(CargoInvoiceItemDto::fromEntity)
                .collect(Collectors.toList());

        // Collect all unique barcodes to fetch product info
        Set<String> barcodes = dtos.stream()
                .map(CargoInvoiceItemDto::getBarcode)
                .filter(b -> b != null && !b.isEmpty())
                .collect(Collectors.toSet());

        if (!barcodes.isEmpty()) {
            // Fetch products by barcodes
            List<TrendyolProduct> products = productRepository.findByStoreIdAndBarcodeIn(storeId, new ArrayList<>(barcodes));
            Map<String, TrendyolProduct> productMap = products.stream()
                    .collect(Collectors.toMap(TrendyolProduct::getBarcode, p -> p, (a, b) -> a));

            // Enrich DTOs with product info
            for (CargoInvoiceItemDto dto : dtos) {
                if (dto.getBarcode() != null) {
                    TrendyolProduct product = productMap.get(dto.getBarcode());
                    if (product != null) {
                        dto.setProductName(product.getTitle());
                        dto.setProductImageUrl(product.getImage());
                    }
                }
            }
        }

        return dtos;
    }

    /**
     * Get cargo invoice item count by invoice serial number.
     */
    @Transactional(readOnly = true)
    public long getCargoInvoiceItemCount(UUID storeId, String invoiceSerialNumber) {
        return cargoInvoiceRepository.countByStoreIdAndInvoiceSerialNumber(storeId, invoiceSerialNumber);
    }

    /**
     * Get cargo invoice items by invoice serial number with pagination.
     * Used for lazy loading in invoice detail panel.
     */
    @Transactional(readOnly = true)
    public CargoInvoiceItemsPageDto getCargoInvoiceItemsPaginated(UUID storeId, String invoiceSerialNumber, int page, int size) {
        log.info("Getting paginated cargo invoice items for store: {}, invoice: {}, page: {}, size: {}",
                storeId, invoiceSerialNumber, page, size);

        // Verify store exists
        storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));

        Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        Page<TrendyolCargoInvoice> itemsPage = cargoInvoiceRepository
                .findByStoreIdAndInvoiceSerialNumberOrderByInvoiceDateDescCreatedAtDesc(storeId, invoiceSerialNumber, pageable);

        log.info("Found {} cargo invoice items (page {} of {})", itemsPage.getNumberOfElements(), page, itemsPage.getTotalPages());

        // Convert to DTOs
        List<CargoInvoiceItemDto> dtos = itemsPage.getContent().stream()
                .map(CargoInvoiceItemDto::fromEntity)
                .collect(Collectors.toList());

        // Collect all unique barcodes to fetch product info
        Set<String> barcodes = dtos.stream()
                .map(CargoInvoiceItemDto::getBarcode)
                .filter(b -> b != null && !b.isEmpty())
                .collect(Collectors.toSet());

        if (!barcodes.isEmpty()) {
            // Fetch products by barcodes
            List<TrendyolProduct> products = productRepository.findByStoreIdAndBarcodeIn(storeId, new ArrayList<>(barcodes));
            Map<String, TrendyolProduct> productMap = products.stream()
                    .collect(Collectors.toMap(TrendyolProduct::getBarcode, p -> p, (a, b) -> a));

            // Enrich DTOs with product info
            for (CargoInvoiceItemDto dto : dtos) {
                if (dto.getBarcode() != null) {
                    TrendyolProduct product = productMap.get(dto.getBarcode());
                    if (product != null) {
                        dto.setProductName(product.getTitle());
                        dto.setProductImageUrl(product.getImage());
                    }
                }
            }
        }

        return CargoInvoiceItemsPageDto.builder()
                .content(dtos)
                .page(page)
                .size(size)
                .totalElements(itemsPage.getTotalElements())
                .totalPages(itemsPage.getTotalPages())
                .hasNext(itemsPage.hasNext())
                .build();
    }

    /**
     * Get generic invoice items by invoice serial number.
     * Returns all orders/items within a specific invoice (for CEZA, KOMISYON, etc. types).
     * This is similar to getCargoInvoiceItems but for non-cargo invoices.
     */
    @Transactional(readOnly = true)
    public List<InvoiceItemDto> getInvoiceItems(UUID storeId, String invoiceSerialNumber) {
        log.info("Getting invoice items for store: {}, invoice: {}", storeId, invoiceSerialNumber);

        // Verify store exists
        storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));

        List<TrendyolDeductionInvoice> items = deductionInvoiceRepository
                .findByStoreIdAndInvoiceSerialNumberOrderByTransactionDateDesc(storeId, invoiceSerialNumber);

        log.info("Found {} invoice items for invoice: {}", items.size(), invoiceSerialNumber);

        return items.stream()
                .map(InvoiceItemDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get invoice item count by invoice serial number.
     */
    @Transactional(readOnly = true)
    public long getInvoiceItemCount(UUID storeId, String invoiceSerialNumber) {
        return deductionInvoiceRepository.countByStoreIdAndInvoiceSerialNumber(storeId, invoiceSerialNumber);
    }

    /**
     * Get generic invoice items by invoice serial number with pagination.
     * Used for lazy loading in invoice detail panel.
     */
    @Transactional(readOnly = true)
    public InvoiceItemsPageDto getInvoiceItemsPaginated(UUID storeId, String invoiceSerialNumber, int page, int size) {
        log.info("Getting paginated invoice items for store: {}, invoice: {}, page: {}, size: {}",
                storeId, invoiceSerialNumber, page, size);

        // Verify store exists
        storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));

        Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        Page<TrendyolDeductionInvoice> itemsPage = deductionInvoiceRepository
                .findByStoreIdAndInvoiceSerialNumberOrderByTransactionDateDesc(storeId, invoiceSerialNumber, pageable);

        log.info("Found {} invoice items (page {} of {})", itemsPage.getNumberOfElements(), page, itemsPage.getTotalPages());

        List<InvoiceItemDto> dtos = itemsPage.getContent().stream()
                .map(InvoiceItemDto::fromEntity)
                .collect(Collectors.toList());

        return InvoiceItemsPageDto.builder()
                .content(dtos)
                .page(page)
                .size(size)
                .totalElements(itemsPage.getTotalElements())
                .totalPages(itemsPage.getTotalPages())
                .hasNext(itemsPage.hasNext())
                .build();
    }

    /**
     * Get commission invoice items by invoice serial number.
     * Queries trendyol_orders.financial_transactions JSONB to find orders
     * linked to a specific commission invoice.
     * Returns order-level commission breakdown (similar to Trendyol Excel export).
     */
    @Transactional(readOnly = true)
    public List<CommissionInvoiceItemDto> getCommissionInvoiceItems(UUID storeId, String invoiceSerialNumber) {
        log.info("Getting commission invoice items for store: {}, invoice: {}", storeId, invoiceSerialNumber);

        // Verify store exists
        storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));

        // Find orders with this commission invoice serial number in financial_transactions
        List<TrendyolOrder> orders = orderRepository.findByStoreIdAndCommissionInvoiceSerialNumber(storeId, invoiceSerialNumber);

        log.info("Found {} orders for commission invoice: {}", orders.size(), invoiceSerialNumber);

        // Extract commission details from each order's financial_transactions
        List<CommissionInvoiceItemDto> items = new ArrayList<>();

        for (TrendyolOrder order : orders) {
            if (order.getFinancialTransactions() == null) continue;

            for (FinancialOrderItemData itemData : order.getFinancialTransactions()) {
                if (itemData.getTransactions() == null) continue;

                for (FinancialSettlement settlement : itemData.getTransactions()) {
                    // Only include items from this specific invoice
                    if (invoiceSerialNumber.equals(settlement.getCommissionInvoiceSerialNumber())) {
                        items.add(CommissionInvoiceItemDto.builder()
                                .orderNumber(order.getTyOrderNumber())
                                .orderDate(order.getOrderDate())
                                .barcode(settlement.getBarcode() != null ? settlement.getBarcode() : itemData.getBarcode())
                                .commissionRate(settlement.getCommissionRate())
                                .commissionAmount(settlement.getCommissionAmount())
                                .sellerRevenue(settlement.getSellerRevenue())
                                .trendyolRevenue(settlement.getCredit())
                                .transactionType(settlement.getTransactionType())
                                // Yeni alanlar - Trendyol Excel export uyumu
                                .recordId(settlement.getId() != null ? settlement.getId() :
                                        (settlement.getReceiptId() != null ? settlement.getReceiptId().toString() : null))
                                .transactionDate(settlement.getTransactionDate() != null ?
                                        LocalDateTime.ofInstant(Instant.ofEpochMilli(settlement.getTransactionDate()),
                                                ZoneId.of("Europe/Istanbul")) : null)
                                .paymentPeriod(settlement.getPaymentPeriod())
                                .paymentDate(settlement.getPaymentDate() != null ?
                                        LocalDateTime.ofInstant(Instant.ofEpochMilli(settlement.getPaymentDate()),
                                                ZoneId.of("Europe/Istanbul")) : null)
                                .totalAmount(settlement.getCredit())
                                .build());
                    }
                }
            }
        }

        log.info("Extracted {} commission items from {} orders", items.size(), orders.size());
        return items;
    }

    /**
     * Get commission invoice items by invoice serial number with pagination.
     * Used for lazy loading in invoice detail panel.
     * Note: Commission items are extracted from orders' financial_transactions JSONB,
     * so pagination is done in-memory after extraction.
     */
    @Transactional(readOnly = true)
    public CommissionInvoiceItemsPageDto getCommissionInvoiceItemsPaginated(UUID storeId, String invoiceSerialNumber, int page, int size) {
        log.info("Getting paginated commission invoice items for store: {}, invoice: {}, page: {}, size: {}",
                storeId, invoiceSerialNumber, page, size);

        // Get all items first (extracted from JSONB)
        List<CommissionInvoiceItemDto> allItems = getCommissionInvoiceItems(storeId, invoiceSerialNumber);

        // Manual pagination
        int start = page * size;
        int end = Math.min(start + size, allItems.size());

        List<CommissionInvoiceItemDto> pageContent = start < allItems.size()
                ? allItems.subList(start, end)
                : Collections.emptyList();

        int totalPages = (int) Math.ceil((double) allItems.size() / size);
        boolean hasNext = page < totalPages - 1;

        log.info("Returning {} commission items (page {} of {})", pageContent.size(), page, totalPages);

        return CommissionInvoiceItemsPageDto.builder()
                .content(pageContent)
                .page(page)
                .size(size)
                .totalElements(allItems.size())
                .totalPages(totalPages)
                .hasNext(hasNext)
                .build();
    }

    /**
     * Convert TrendyolDeductionInvoice to InvoiceDetailDto.
     */
    private InvoiceDetailDto convertToDetailDto(TrendyolDeductionInvoice invoice) {
        String typeCode = TYPE_TO_CODE.getOrDefault(invoice.getTransactionType(),
                normalizeTypeCode(invoice.getTransactionType()));
        String category = TYPE_TO_CATEGORY.getOrDefault(invoice.getTransactionType(), "DIGER");

        // Determine if this is a refund:
        // 1. credit > 0 (direct refund from Trendyol)
        // 2. transaction type is a refund type (iade or AZ-yurtdışı operasyon patterns)
        boolean hasCreditValue = invoice.getCredit() != null && invoice.getCredit().compareTo(BigDecimal.ZERO) > 0;
        boolean isRefundByType = isRefundTransactionType(invoice.getTransactionType());
        boolean isRefund = hasCreditValue || isRefundByType;

        BigDecimal amount;
        boolean isDeduction;
        if (isRefund) {
            // For refunds: prefer credit if available, otherwise use debt
            // (Trendyol sends some refunds as positive debt instead of credit)
            if (hasCreditValue) {
                amount = invoice.getCredit();
            } else {
                amount = invoice.getDebt() != null ? invoice.getDebt() : BigDecimal.ZERO;
            }
            isDeduction = false;
            // Override category to IADE for refund types
            category = "IADE";
        } else {
            amount = invoice.getDebt() != null ? invoice.getDebt() : BigDecimal.ZERO;
            isDeduction = true;
        }

        String transactionType = invoice.getTransactionType();
        BigDecimal vatAmount = calculateVat(amount, category, transactionType, typeCode);
        BigDecimal vatRate = getVatRate(category, transactionType, typeCode);
        BigDecimal baseAmount = amount.subtract(vatAmount);

        return InvoiceDetailDto.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceSerialNumber())
                .invoiceType(invoice.getTransactionType())
                .invoiceTypeCode(typeCode)
                .invoiceCategory(category)
                .invoiceDate(invoice.getTransactionDate())
                .amount(amount)
                .vatAmount(vatAmount)
                .vatRate(vatRate)
                .baseAmount(baseAmount)
                .isDeduction(isDeduction)
                .orderNumber(invoice.getOrderNumber())
                .shipmentPackageId(invoice.getShipmentPackageId())
                .paymentOrderId(invoice.getPaymentOrderId())
                .description(invoice.getDescription())
                .build();
    }

    /**
     * Normalize a transaction type to a type code.
     */
    private String normalizeTypeCode(String typeName) {
        if (typeName == null) return "DIGER";
        return typeName.toUpperCase()
                .replace(" ", "_")
                .replace("-", "_")
                .replace("İ", "I")
                .replace("Ş", "S")
                .replace("Ğ", "G")
                .replace("Ü", "U")
                .replace("Ö", "O")
                .replace("Ç", "C");
    }

    /**
     * Get type name from type code.
     */
    private String getTypeNameFromCode(String typeCode) {
        return TYPE_TO_CODE.entrySet().stream()
                .filter(e -> e.getValue().equals(typeCode))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(typeCode);
    }

    /**
     * Calculate VAT amount based on category, transaction type, and type code.
     * Most deductions have 20% VAT, AZ-Yurtdışı Operasyon has 18%.
     */
    private BigDecimal calculateVat(BigDecimal amount, String category, String transactionType, String typeCode) {
        BigDecimal vatRate = getVatRate(category, transactionType, typeCode);
        if (vatRate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        // amount = base + vat, vat = amount * vatRate / (100 + vatRate)
        return amount.multiply(vatRate)
                .divide(BigDecimal.valueOf(100).add(vatRate), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate VAT amount (without typeCode, for backward compatibility).
     */
    private BigDecimal calculateVat(BigDecimal amount, String category, String transactionType) {
        return calculateVat(amount, category, transactionType, null);
    }

    /**
     * Get VAT rate for a category and transaction type.
     * Most invoice types use 20% VAT, but some international operations use 18%.
     */
    private BigDecimal getVatRate(String category) {
        return getVatRate(category, null, null);
    }

    /**
     * Get VAT rate based on transaction type (backward compatibility).
     */
    private BigDecimal getVatRate(String category, String transactionType) {
        return getVatRate(category, transactionType, null);
    }

    /**
     * Get VAT rate based on type code and transaction type.
     * Uses typeCode first (reliable, resolved via TYPE_TO_CODE map which handles all variants),
     * then falls back to string matching on transactionType.
     *
     * - Tedarik Edememe: 0% VAT (penalty, no VAT)
     * - Termin Gecikme Bedeli: 0% VAT (penalty, no VAT)
     * - Eksik/Yanlış/Kusurlu Ürün: 0% VAT (penalties, no VAT)
     * - AZ-Platform/Uluslararası Hizmet: 0% VAT
     * - TEX Tazmin: 0% VAT
     * - Yurtdışı Operasyon Iade: 0% VAT
     * - AZ-Yurtdışı Operasyon Bedeli: 18% VAT
     * - All other types: 20% VAT
     */
    private BigDecimal getVatRate(String category, String transactionType, String typeCode) {
        // typeCode-based matching (reliable - TYPE_TO_CODE map handles all Trendyol API variants)
        if (typeCode != null) {
            if ("AZ_YURTDISI_OPERASYON".equals(typeCode)) {
                return BigDecimal.valueOf(18);
            }
            if ("TEDARIK_EDEMEME".equals(typeCode) ||
                "TERMIN_GECIKME".equals(typeCode) ||
                "EKSIK_URUN".equals(typeCode) ||
                "YANLIS_URUN".equals(typeCode) ||
                "KUSURLU_URUN".equals(typeCode) ||
                "AZ_PLATFORM_HIZMET".equals(typeCode) ||
                "AZ_ULUSLARARASI_HIZMET".equals(typeCode) ||
                "TEX_TAZMIN".equals(typeCode) ||
                "YURTDISI_OPERASYON_IADE".equals(typeCode)) {
                return BigDecimal.ZERO;
            }
        }

        if (transactionType == null) {
            return BigDecimal.valueOf(20);
        }

        // Fallback: Normalize to uppercase AND convert Turkish special characters to ASCII
        String normalizedType = normalizeTurkishToAscii(transactionType.toUpperCase(Locale.forLanguageTag("tr-TR")));

        // Penalties, product issues, AZ services, TEX refunds, and international operation returns have 0% VAT
        if (normalizedType.contains("TEDARIK EDEMEME") ||
            normalizedType.contains("TERMIN GECIKME") ||
            normalizedType.contains("EKSIK URUN") ||
            normalizedType.contains("YANLIS URUN") ||
            normalizedType.contains("KUSURLU URUN") ||
            normalizedType.contains("AZ-PLATFORM HIZMET") ||
            normalizedType.contains("AZ-ULUSLARARASI HIZMET") ||
            normalizedType.contains("TEX TAZMIN") ||
            normalizedType.contains("YURTDISI OPERASYON IADE")) {
            return BigDecimal.ZERO;
        }

        // AZ-Yurtdışı Operasyon Bedeli uses 18% VAT (but not the "Iade" variant which is 0%)
        if (normalizedType.contains("AZ-YURTDISI OPERASYON")) {
            return BigDecimal.valueOf(18);
        }

        // All other categories have 20% VAT in Turkey
        return BigDecimal.valueOf(20);
    }

    /**
     * Normalize Turkish special characters to ASCII equivalents.
     * İ → I, Ş → S, Ğ → G, Ü → U, Ö → O, Ç → C, ı → I
     * This ensures consistent string matching regardless of Turkish locale variations.
     */
    private String normalizeTurkishToAscii(String input) {
        if (input == null) return null;
        return input
                .replace("İ", "I")  // Turkish dotted I uppercase
                .replace("ı", "I")  // Turkish dotless i lowercase
                .replace("Ş", "S")
                .replace("ş", "S")
                .replace("Ğ", "G")
                .replace("ğ", "G")
                .replace("Ü", "U")
                .replace("ü", "U")
                .replace("Ö", "O")
                .replace("ö", "O")
                .replace("Ç", "C")
                .replace("ç", "C");
    }

    /**
     * Check if transaction type indicates a refund (iade).
     * Trendyol sends some refunds with debt > 0 instead of credit > 0,
     * but we can identify them by the transaction type.
     *
     * Refund types include:
     * - "Yurtdışı Operasyon Iade Bedeli" (contains "iade")
     * - "AZ-Yurtdışı Operasyon Bedeli" (AZ overseas operation - shown as negative in Trendyol UI)
     *
     * Note: Uses ENGLISH locale for case conversion to avoid Turkish i/ı issues.
     * Turkish: uppercase I → lowercase ı (dotless) with Turkish locale
     * English: uppercase I → lowercase i (dotted) with English locale
     */
    private boolean isRefundTransactionType(String transactionType) {
        if (transactionType == null) {
            return false;
        }

        // Use ENGLISH locale to avoid Turkish i/ı character issues
        // "Yurtdışı Operasyon Iade Bedeli" with English locale becomes "yurtdışı operasyon iade bedeli"
        String upper = transactionType.toUpperCase(Locale.ENGLISH);

        // Check for "IADE" keyword (case-insensitive with English locale)
        // Only actual refunds with "IADE" in the name should be treated as refunds
        // "AZ-Yurtdışı Operasyon Bedeli" is an international operation FEE (deduction),
        // NOT a refund - only "Yurtdışı Operasyon Iade Bedeli" is a refund
        return upper.contains("IADE");
    }

    // ================================================================================
    // Category-level item queries (for "Fatura Kalemleri" and "Ürünler" tabs)
    // ================================================================================

    // Komisyon transaction types
    private static final List<String> KOMISYON_TYPES = List.of(
            "Platform Hizmet Bedeli",
            "AZ - Komisyon Faturası",
            "AZ-Komisyon Geliri",
            "Komisyon Faturası"
    );

    /**
     * Get all cargo invoice items for a date range with pagination.
     * Used for "Fatura Kalemleri" tab when KARGO category is selected.
     * Enriches items with product name and image from TrendyolProduct table.
     */
    @Transactional(readOnly = true)
    public Page<CargoInvoiceItemDto> getCargoItemsByDateRange(UUID storeId, LocalDate startDate,
                                                               LocalDate endDate, Pageable pageable) {
        log.info("Getting cargo items by date range for store: {} from {} to {}", storeId, startDate, endDate);

        // Verify store exists
        storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));

        Page<TrendyolCargoInvoice> cargoPage = cargoInvoiceRepository
                .findByStoreIdAndInvoiceDateBetweenOrderByInvoiceDateDescCreatedAtDesc(
                        storeId, startDate, endDate, pageable);

        // Convert to DTOs
        List<CargoInvoiceItemDto> dtos = cargoPage.getContent().stream()
                .map(CargoInvoiceItemDto::fromEntity)
                .collect(Collectors.toList());

        // Collect all unique barcodes to fetch product info
        Set<String> barcodes = dtos.stream()
                .map(CargoInvoiceItemDto::getBarcode)
                .filter(b -> b != null && !b.isEmpty())
                .collect(Collectors.toSet());

        if (!barcodes.isEmpty()) {
            // Fetch products by barcodes
            List<TrendyolProduct> products = productRepository.findByStoreIdAndBarcodeIn(storeId, new ArrayList<>(barcodes));
            Map<String, TrendyolProduct> productMap = products.stream()
                    .collect(Collectors.toMap(TrendyolProduct::getBarcode, p -> p, (a, b) -> a));

            // Enrich DTOs with product info
            for (CargoInvoiceItemDto dto : dtos) {
                if (dto.getBarcode() != null) {
                    TrendyolProduct product = productMap.get(dto.getBarcode());
                    if (product != null) {
                        dto.setProductName(product.getTitle());
                        dto.setProductImageUrl(product.getImage());
                    }
                }
            }
        }

        return new PageImpl<>(dtos, pageable, cargoPage.getTotalElements());
    }

    /**
     * Get all commission invoice items for a date range with pagination.
     * Used for "Fatura Kalemleri" tab when KOMISYON category is selected.
     */
    @Transactional(readOnly = true)
    public Page<InvoiceDetailDto> getCommissionItemsByDateRange(UUID storeId, LocalDate startDate,
                                                                  LocalDate endDate, Pageable pageable) {
        log.info("Getting commission items by date range for store: {} from {} to {}", storeId, startDate, endDate);

        // Verify store exists
        storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        Page<TrendyolDeductionInvoice> invoicePage = deductionInvoiceRepository
                .findByStoreIdAndTypesInDateRangePaged(storeId, KOMISYON_TYPES, startDateTime, endDateTime, pageable);

        List<InvoiceDetailDto> dtos = invoicePage.getContent().stream()
                .map(this::convertToDetailDto)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, invoicePage.getTotalElements());
    }

    /**
     * Get aggregated products for a category (KARGO or KOMISYON).
     * Groups invoice items by barcode and returns totals.
     * Used for "Ürünler" tab in category view.
     */
    @Transactional(readOnly = true)
    public List<AggregatedProductDto> getAggregatedProductsByCategory(UUID storeId, String category,
                                                                       LocalDate startDate, LocalDate endDate) {
        log.info("Getting aggregated products for category {} in store: {} from {} to {}",
                category, storeId, startDate, endDate);

        // Verify store exists
        storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));

        List<AggregatedProductDto> results = new ArrayList<>();

        if ("KARGO".equals(category)) {
            // Aggregate from cargo invoices
            var rows = cargoInvoiceRepository.aggregateByBarcodeAndDateRange(storeId, startDate, endDate);

            for (var row : rows) {
                results.add(AggregatedProductDto.builder()
                        .barcode(row.getBarcode())
                        .productName(row.getProductName())
                        .totalQuantity(row.getTotalQuantity() != null ? row.getTotalQuantity().intValue() : 0)
                        .totalAmount(row.getTotalAmount())
                        .totalVatAmount(row.getTotalVatAmount())
                        .totalDesi(row.getTotalDesi() != null ? row.getTotalDesi().intValue() : null)
                        .invoiceCount(row.getInvoiceCount() != null ? row.getInvoiceCount().intValue() : 0)
                        .build());
            }
        } else if ("KOMISYON".equals(category)) {
            // Aggregate commission from financial_transactions (actual settlement data)
            // Uses NET commission calculation: Satış - İndirim - Kupon (İade excluded)
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

            var rows = orderRepository.aggregateCommissionByBarcodeFromSettlements(
                    storeId, startDateTime, endDateTime);

            for (var row : rows) {
                results.add(AggregatedProductDto.builder()
                        .barcode(row.getBarcode())
                        .productName(row.getProductName())
                        .totalQuantity(row.getTransactionCount() != null ? row.getTransactionCount().intValue() : 0) // transactionCount as quantity
                        .totalAmount(row.getNetCommission())  // netCommission
                        .totalVatAmount(row.getNetVatAmount()) // netVatAmount
                        .invoiceCount(row.getOrderCount() != null ? row.getOrderCount().intValue() : 0) // orderCount
                        // Additional breakdown fields (can be used in frontend if needed)
                        .saleCommission(row.getSaleCommission())
                        .discountCommission(row.getDiscountCommission())
                        .couponCommission(row.getCouponCommission())
                        .build());
            }
        }

        // Enrich with product info
        if (!results.isEmpty()) {
            Set<String> barcodes = results.stream()
                    .map(AggregatedProductDto::getBarcode)
                    .filter(b -> b != null && !b.isEmpty())
                    .collect(Collectors.toSet());

            if (!barcodes.isEmpty()) {
                List<TrendyolProduct> products = productRepository.findByStoreIdAndBarcodeIn(storeId, new ArrayList<>(barcodes));
                Map<String, TrendyolProduct> productMap = products.stream()
                        .collect(Collectors.toMap(TrendyolProduct::getBarcode, p -> p, (a, b) -> a));

                for (AggregatedProductDto dto : results) {
                    if (dto.getBarcode() != null) {
                        TrendyolProduct product = productMap.get(dto.getBarcode());
                        if (product != null) {
                            dto.setProductName(product.getTitle());
                            dto.setProductImageUrl(product.getImage());
                            dto.setProductUrl(product.getProductUrl());
                        }
                    }
                }
            }
        }

        return results;
    }

    // ================================================================================
    // Product Commission Breakdown (for "Detay" panel in KOMISYON Ürünler tab)
    // ================================================================================

    // Transaction type display names (Turkish)
    private static final Map<String, String> TRANSACTION_TYPE_DISPLAY = Map.of(
            "Sale", "Satış",
            "Coupon", "Kupon",
            "Discount", "İndirim",
            "Return", "İade",
            "Commission", "Komisyon"
    );

    /**
     * Get commission breakdown by transaction type for a specific product (barcode).
     * Aggregates commission data from orders within the date range.
     * Used for "Detay" panel in KOMISYON Ürünler tab.
     *
     * IMPORTANT: Net Commission Calculation (matching Trendyol invoice):
     * - Satış (Sale): ADD commission (goes to Trendyol)
     * - İndirim (Discount): SUBTRACT commission (returned to seller)
     * - Kupon (Coupon): SUBTRACT commission (returned to seller)
     * - İade (Return): EXCLUDED from commission invoice (separate invoice)
     *
     * Net Commission = Satış - İndirim - Kupon
     */
    @Transactional(readOnly = true)
    public ProductCommissionBreakdownDto getProductCommissionBreakdown(UUID storeId, String barcode,
                                                                         LocalDate startDate, LocalDate endDate) {
        log.info("Getting commission breakdown for product {} in store: {} from {} to {}",
                barcode, storeId, startDate, endDate);

        // Verify store exists
        storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        // Find all orders in date range
        List<TrendyolOrder> orders = orderRepository.findByStoreIdAndOrderDateBetween(storeId, startDateTime, endDateTime);

        // Track transaction type aggregations
        Map<String, TransactionTypeBreakdownDto.TransactionTypeBreakdownDtoBuilder> typeAggregations = new LinkedHashMap<>();

        // Separate tracking for net calculation
        BigDecimal saleCommission = BigDecimal.ZERO;
        BigDecimal discountCommission = BigDecimal.ZERO;
        BigDecimal couponCommission = BigDecimal.ZERO;
        BigDecimal returnCommission = BigDecimal.ZERO; // Tracked but excluded from net

        int totalItemCount = 0;
        int netItemCount = 0; // Items included in net calculation (excluding İade)
        Set<String> uniqueOrders = new HashSet<>();

        // Process each order
        for (TrendyolOrder order : orders) {
            if (order.getFinancialTransactions() == null) continue;

            for (FinancialOrderItemData itemData : order.getFinancialTransactions()) {
                if (itemData.getTransactions() == null) continue;

                for (FinancialSettlement settlement : itemData.getTransactions()) {
                    // Check if this settlement is for our barcode
                    String settlementBarcode = settlement.getBarcode() != null ? settlement.getBarcode() : itemData.getBarcode();
                    if (!barcode.equals(settlementBarcode)) continue;

                    // Get transaction type (final for lambda)
                    final String txType = (settlement.getTransactionType() == null || settlement.getTransactionType().isEmpty())
                            ? "Commission"
                            : settlement.getTransactionType();

                    // Get commission amount (final for lambda)
                    final BigDecimal commAmount = settlement.getCommissionAmount() != null
                            ? settlement.getCommissionAmount()
                            : BigDecimal.ZERO;

                    // Calculate VAT (20% for commission) - final for lambda
                    final BigDecimal vatAmt = commAmount.multiply(BigDecimal.valueOf(0.20))
                            .setScale(2, java.math.RoundingMode.HALF_UP);

                    // Get commission rate (final for lambda)
                    final BigDecimal commRate = settlement.getCommissionRate();

                    // Track by transaction type for net calculation
                    switch (txType) {
                        case "Satış" -> {
                            saleCommission = saleCommission.add(commAmount);
                            netItemCount++;
                        }
                        case "İndirim" -> {
                            discountCommission = discountCommission.add(commAmount);
                            netItemCount++;
                        }
                        case "Kupon" -> {
                            couponCommission = couponCommission.add(commAmount);
                            netItemCount++;
                        }
                        case "İade" -> {
                            returnCommission = returnCommission.add(commAmount);
                            // İade is NOT included in net calculation
                        }
                        default -> netItemCount++; // Other types are included in net
                    }

                    totalItemCount++;
                    uniqueOrders.add(order.getTyOrderNumber());

                    // Aggregate by transaction type (for breakdown display)
                    typeAggregations.compute(txType, (k, existing) -> {
                        if (existing == null) {
                            return TransactionTypeBreakdownDto.builder()
                                    .transactionType(txType)
                                    .transactionTypeDisplay(TRANSACTION_TYPE_DISPLAY.getOrDefault(txType, txType))
                                    .itemCount(1)
                                    .totalCommission(commAmount)
                                    .totalVatAmount(vatAmt)
                                    .averageCommissionRate(commRate != null ? commRate : BigDecimal.ZERO);
                        } else {
                            var built = existing.build();
                            // Calculate new average rate
                            BigDecimal newTotalCommission = built.getTotalCommission().add(commAmount);
                            int newCount = built.getItemCount() + 1;
                            BigDecimal newAvgRate = built.getAverageCommissionRate();
                            if (commRate != null && commRate.compareTo(BigDecimal.ZERO) > 0) {
                                // Simple average (could be weighted by amount)
                                newAvgRate = built.getAverageCommissionRate()
                                        .multiply(BigDecimal.valueOf(built.getItemCount()))
                                        .add(commRate)
                                        .divide(BigDecimal.valueOf(newCount), 2, java.math.RoundingMode.HALF_UP);
                            }
                            return TransactionTypeBreakdownDto.builder()
                                    .transactionType(txType)
                                    .transactionTypeDisplay(TRANSACTION_TYPE_DISPLAY.getOrDefault(txType, txType))
                                    .itemCount(newCount)
                                    .totalCommission(newTotalCommission)
                                    .totalVatAmount(built.getTotalVatAmount().add(vatAmt))
                                    .averageCommissionRate(newAvgRate);
                        }
                    });
                }
            }
        }

        // Calculate NET commission: Satış - İndirim - Kupon (İade excluded)
        BigDecimal netCommission = saleCommission.subtract(discountCommission).subtract(couponCommission);
        BigDecimal netVatAmount = netCommission.multiply(BigDecimal.valueOf(0.20))
                .setScale(2, java.math.RoundingMode.HALF_UP);

        // Build breakdown list (sorted by commission amount descending)
        // EXCLUDE İade from breakdown - it's not part of commission invoice (handled separately in İadeler)
        List<TransactionTypeBreakdownDto> breakdown = typeAggregations.values().stream()
                .map(TransactionTypeBreakdownDto.TransactionTypeBreakdownDtoBuilder::build)
                .filter(dto -> !"İade".equals(dto.getTransactionType()) && !"Return".equals(dto.getTransactionType()))
                .sorted((a, b) -> b.getTotalCommission().compareTo(a.getTotalCommission()))
                .collect(Collectors.toList());

        // Get product info
        String productName = null;
        String productImageUrl = null;
        String productUrl = null;
        Optional<TrendyolProduct> productOpt = productRepository.findByStoreIdAndBarcode(storeId, barcode);
        if (productOpt.isPresent()) {
            TrendyolProduct product = productOpt.get();
            productName = product.getTitle();
            productImageUrl = product.getImage();
            productUrl = product.getProductUrl();
        }

        log.info("Found {} transaction types with {} total items for product {}, netCommission: {}",
                breakdown.size(), totalItemCount, barcode, netCommission);

        return ProductCommissionBreakdownDto.builder()
                .barcode(barcode)
                .productName(productName)
                .productImageUrl(productImageUrl)
                .productUrl(productUrl)
                // NET values (what matches Trendyol invoice)
                .totalCommission(netCommission)
                .totalVatAmount(netVatAmount)
                .totalItemCount(netItemCount)
                .orderCount(uniqueOrders.size())
                // Breakdown by transaction type
                .breakdown(breakdown)
                // Individual transaction type totals (for detailed view)
                .saleCommission(saleCommission)
                .discountCommission(discountCommission)
                .couponCommission(couponCommission)
                // returnCommission excluded - handled separately in İadeler section
                .build();
    }

    // ================================================================================
    // Product Cargo Breakdown (for "Detay" panel in KARGO Ürünler tab)
    // ================================================================================

    /**
     * Get cargo breakdown for a specific product (barcode).
     * Aggregates cargo invoice data from the date range.
     * Used for "Detay" panel in KARGO Ürünler tab.
     *
     * Returns:
     * - Total cargo cost, VAT, desi
     * - Shipment count and order count
     * - Average desi and cost per shipment
     * - Paginated list of shipments (default 50 per page)
     */
    @Transactional(readOnly = true)
    public ProductCargoBreakdownDto getProductCargoBreakdown(UUID storeId, String barcode,
                                                              LocalDate startDate, LocalDate endDate,
                                                              int page, int size) {
        log.info("Getting cargo breakdown for product {} in store: {} from {} to {} (page: {}, size: {})",
                barcode, storeId, startDate, endDate, page, size);

        // Verify store exists
        storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));

        // Get summary totals from dedicated query (complete data, no pagination)
        CargoSummaryProjection summary = cargoInvoiceRepository
                .getCargoSummaryByStoreIdAndBarcodeAndDateRange(storeId, barcode, startDate, endDate);

        BigDecimal totalAmount = summary != null && summary.getTotalAmount() != null
                ? summary.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal totalVatAmount = summary != null && summary.getTotalVatAmount() != null
                ? summary.getTotalVatAmount() : BigDecimal.ZERO;
        BigDecimal totalDesi = summary != null && summary.getTotalDesi() != null
                ? summary.getTotalDesi() : BigDecimal.ZERO;
        long totalShipmentCount = summary != null && summary.getShipmentCount() != null
                ? summary.getShipmentCount() : 0L;
        long orderCount = summary != null && summary.getOrderCount() != null
                ? summary.getOrderCount() : 0L;

        log.info("Cargo summary for product {}: {} shipments, {} orders, total cost: {}",
                barcode, totalShipmentCount, orderCount, totalAmount);

        // Calculate averages
        BigDecimal averageDesi = BigDecimal.ZERO;
        BigDecimal averageCostPerShipment = BigDecimal.ZERO;
        if (totalShipmentCount > 0) {
            averageDesi = totalDesi.divide(BigDecimal.valueOf(totalShipmentCount), 2, RoundingMode.HALF_UP);
            averageCostPerShipment = totalAmount.divide(BigDecimal.valueOf(totalShipmentCount), 2, RoundingMode.HALF_UP);
        }

        // Fetch paginated cargo invoices for shipment list
        Pageable pageable = PageRequest.of(page, size);
        Page<TrendyolCargoInvoice> cargoPage = cargoInvoiceRepository
                .findByStoreIdAndBarcodeAndDateRangePaginated(storeId, barcode, startDate, endDate, pageable);

        // Build shipment list from current page
        List<ProductCargoBreakdownDto.CargoShipmentDetailDto> shipments = cargoPage.getContent().stream()
                .map(invoice -> ProductCargoBreakdownDto.CargoShipmentDetailDto.builder()
                        .orderNumber(invoice.getOrderNumber())
                        .shipmentPackageId(invoice.getShipmentPackageId())
                        .invoiceSerialNumber(invoice.getInvoiceSerialNumber())
                        .amount(invoice.getAmount())
                        .vatAmount(invoice.getVatAmount())
                        .desi(invoice.getDesi())
                        .invoiceDate(invoice.getInvoiceDate())
                        .cargoCompany(extractCargoCompany(invoice))
                        .build())
                .collect(Collectors.toList());

        // Get product info
        String productName = null;
        String productImageUrl = null;
        String productUrl = null;
        Optional<TrendyolProduct> productOpt = productRepository.findByStoreIdAndBarcode(storeId, barcode);
        if (productOpt.isPresent()) {
            TrendyolProduct product = productOpt.get();
            productName = product.getTitle();
            productImageUrl = product.getImage();
            productUrl = product.getProductUrl();
        }

        log.info("Returning cargo breakdown for product {}: page {} of {}, {} shipments on this page",
                barcode, page, cargoPage.getTotalPages(), shipments.size());

        return ProductCargoBreakdownDto.builder()
                .barcode(barcode)
                .productName(productName)
                .productImageUrl(productImageUrl)
                .productUrl(productUrl)
                .totalAmount(totalAmount)
                .totalVatAmount(totalVatAmount)
                .totalDesi(totalDesi)
                .totalShipmentCount((int) totalShipmentCount)
                .orderCount((int) orderCount)
                .averageDesi(averageDesi)
                .averageCostPerShipment(averageCostPerShipment)
                .shipments(shipments)
                // Pagination info
                .currentPage(page)
                .totalPages(cargoPage.getTotalPages())
                .hasMore(cargoPage.hasNext())
                .build();
    }

    /**
     * Backward compatible method for cargo breakdown without pagination.
     * Uses default page 0 and size 50.
     */
    @Transactional(readOnly = true)
    public ProductCargoBreakdownDto getProductCargoBreakdown(UUID storeId, String barcode,
                                                              LocalDate startDate, LocalDate endDate) {
        return getProductCargoBreakdown(storeId, barcode, startDate, endDate, 0, 50);
    }

    /**
     * Get all invoice items (cargo, commission, deductions) linked to a specific order.
     * Used to show actual invoiced expenses in order detail panel.
     *
     * @param storeId     The store ID
     * @param orderNumber The Trendyol order number
     * @return OrderInvoiceItemsDto containing all invoice items for this order
     */
    @Transactional(readOnly = true)
    public OrderInvoiceItemsDto getInvoiceItemsByOrderNumber(UUID storeId, String orderNumber) {
        log.info("Getting invoice items for order: {} in store: {}", orderNumber, storeId);

        // Verify store exists
        storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));

        // 1. Get cargo invoices for this order
        List<TrendyolCargoInvoice> cargoInvoices = cargoInvoiceRepository
                .findByStoreIdAndOrderNumber(storeId, orderNumber);
        List<CargoInvoiceItemDto> cargoItems = cargoInvoices.stream()
                .map(CargoInvoiceItemDto::fromEntity)
                .collect(Collectors.toList());
        BigDecimal totalCargoAmount = cargoInvoices.stream()
                .map(TrendyolCargoInvoice::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCargoVatAmount = cargoInvoices.stream()
                .map(TrendyolCargoInvoice::getVatAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Get deduction invoices for this order (platform fees, advertising, penalties, etc.)
        List<TrendyolDeductionInvoice> deductionInvoices = deductionInvoiceRepository
                .findByStoreIdAndOrderNumber(storeId, orderNumber);
        List<InvoiceItemDto> deductionItems = deductionInvoices.stream()
                .map(InvoiceItemDto::fromEntity)
                .collect(Collectors.toList());
        BigDecimal totalDeductionAmount = deductionItems.stream()
                .map(InvoiceItemDto::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDeductionVatAmount = deductionItems.stream()
                .map(InvoiceItemDto::getVatAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Get commission items from order's financial_transactions
        List<CommissionInvoiceItemDto> commissionItems = new ArrayList<>();
        BigDecimal totalCommissionAmount = BigDecimal.ZERO;
        BigDecimal totalCommissionVatAmount = BigDecimal.ZERO;

        List<TrendyolOrder> orders = orderRepository.findByStoreIdAndTyOrderNumber(storeId, orderNumber);
        if (!orders.isEmpty()) {
            TrendyolOrder order = orders.get(0);
            if (order.getFinancialTransactions() != null) {
                for (FinancialOrderItemData itemData : order.getFinancialTransactions()) {
                    // Each FinancialOrderItemData contains a list of FinancialSettlement transactions
                    String itemBarcode = itemData.getBarcode();

                    if (itemData.getTransactions() != null) {
                        for (FinancialSettlement transaction : itemData.getTransactions()) {
                            String transactionType = transaction.getTransactionType() != null
                                    ? transaction.getTransactionType() : "Satış Komisyonu";

                            // Extract commission amounts from transaction
                            BigDecimal commissionAmount = transaction.getCommissionAmount();
                            if (commissionAmount == null) {
                                commissionAmount = BigDecimal.ZERO;
                            }

                            BigDecimal commissionRate = transaction.getCommissionRate();
                            BigDecimal sellerRevenue = transaction.getSellerRevenue();

                            // Get receipt/settlement ID
                            String recordId = transaction.getReceiptId() != null
                                    ? transaction.getReceiptId().toString() : null;

                            // Get transaction date
                            LocalDateTime transactionDate = null;
                            Long dateObj = transaction.getTransactionDate();
                            if (dateObj != null) {
                                transactionDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(dateObj), ZoneId.of("Europe/Istanbul"));
                            }

                            // Get barcode from order items or from the itemData
                            String barcode = itemBarcode;
                            String productName = null;
                            if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
                                // Try to find matching order item by barcode
                                for (OrderItem orderItem : order.getOrderItems()) {
                                    if (barcode != null && barcode.equals(orderItem.getBarcode())) {
                                        productName = orderItem.getProductName();
                                        break;
                                    }
                                }
                                // If no match found, use first item
                                if (productName == null) {
                                    OrderItem firstItem = order.getOrderItems().get(0);
                                    if (barcode == null) {
                                        barcode = firstItem.getBarcode();
                                    }
                                    productName = firstItem.getProductName();
                                }
                            }

                            CommissionInvoiceItemDto item = CommissionInvoiceItemDto.builder()
                                    .orderNumber(orderNumber)
                                    .orderDate(order.getOrderDate())
                                    .barcode(barcode)
                                    .productName(productName)
                                    .commissionRate(commissionRate)
                                    .commissionAmount(commissionAmount)
                                    .sellerRevenue(sellerRevenue)
                                    .trendyolRevenue(commissionAmount)
                                    .transactionType(transactionType)
                                    .recordId(recordId)
                                    .transactionDate(transactionDate)
                                    .build();
                            commissionItems.add(item);

                            totalCommissionAmount = totalCommissionAmount.add(commissionAmount);
                        }
                    }
                }

                // Calculate VAT for commission (20% included in amount)
                if (totalCommissionAmount.compareTo(BigDecimal.ZERO) > 0) {
                    totalCommissionVatAmount = totalCommissionAmount.multiply(BigDecimal.valueOf(20))
                            .divide(BigDecimal.valueOf(120), 2, RoundingMode.HALF_UP);
                }
            }
        }

        // Calculate grand total (all expenses)
        BigDecimal grandTotal = totalCargoAmount.add(totalDeductionAmount).add(totalCommissionAmount);

        // Check if we have any invoice data
        boolean hasInvoiceData = !cargoItems.isEmpty() || !deductionItems.isEmpty() || !commissionItems.isEmpty();

        log.info("Order {} invoice summary: cargo={} items ({}₺), deductions={} items ({}₺), commission={} items ({}₺)",
                orderNumber, cargoItems.size(), totalCargoAmount,
                deductionItems.size(), totalDeductionAmount,
                commissionItems.size(), totalCommissionAmount);

        return OrderInvoiceItemsDto.builder()
                .orderNumber(orderNumber)
                .cargoItems(cargoItems)
                .totalCargoAmount(totalCargoAmount)
                .totalCargoVatAmount(totalCargoVatAmount)
                .commissionItems(commissionItems)
                .totalCommissionAmount(totalCommissionAmount)
                .totalCommissionVatAmount(totalCommissionVatAmount)
                .deductionItems(deductionItems)
                .totalDeductionAmount(totalDeductionAmount)
                .totalDeductionVatAmount(totalDeductionVatAmount)
                .grandTotal(grandTotal)
                .hasInvoiceData(hasInvoiceData)
                .build();
    }

    /**
     * Extract BigDecimal value from a map, handling various number types.
     */
    private BigDecimal extractBigDecimal(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Extract cargo company name from invoice rawData if available.
     * Trendyol may include cargo company info in the rawData JSONB field.
     */
    private String extractCargoCompany(TrendyolCargoInvoice invoice) {
        if (invoice.getRawData() == null) return null;

        // Try to extract from rawData - common field names
        Object cargoCompany = invoice.getRawData().get("cargoProviderName");
        if (cargoCompany != null) return cargoCompany.toString();

        cargoCompany = invoice.getRawData().get("cargoCompany");
        if (cargoCompany != null) return cargoCompany.toString();

        cargoCompany = invoice.getRawData().get("shipmentCompany");
        if (cargoCompany != null) return cargoCompany.toString();

        return null;
    }

    // ================================================================================
    // Stoppage (Withholding Tax) Methods
    // ================================================================================

    /**
     * Get stoppage summary for a store within a date range.
     * Returns total amount and count of stopaj records.
     */
    @Transactional(readOnly = true)
    public StoppageSummaryDto getStoppageSummary(UUID storeId, LocalDate startDate, LocalDate endDate) {
        log.info("Getting stoppage summary for store: {} from {} to {}", storeId, startDate, endDate);

        // Verify store exists
        storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        // Get stoppage total using repository method that filters by 'stopaj' keyword
        BigDecimal totalAmount = stoppageRepository.sumStoppageOnly(storeId, startDateTime, endDateTime);
        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }

        // Get count of stoppage records
        List<TrendyolStoppage> stoppages = stoppageRepository
                .findByStoreIdAndTransactionDateBetweenOrderByTransactionDateDesc(storeId, startDateTime, endDateTime);

        // Filter only stopaj records
        long count = stoppages.stream()
                .filter(s -> s.getDescription() != null && s.getDescription().toLowerCase().contains("stopaj"))
                .count();

        return StoppageSummaryDto.builder()
                .storeId(storeId.toString())
                .periodStart(startDate)
                .periodEnd(endDate)
                .count((int) count)
                .totalAmount(totalAmount)
                .build();
    }

    /**
     * Get stoppages with pagination for a store within a date range.
     * Filters only records containing 'stopaj' in description.
     */
    @Transactional(readOnly = true)
    public StoppageSummaryDto getStoppages(UUID storeId, LocalDate startDate, LocalDate endDate, int page, int size) {
        log.info("Getting stoppages for store: {} from {} to {}, page: {}, size: {}",
                storeId, startDate, endDate, page, size);

        // Verify store exists
        storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        // Get all stoppages for the period
        List<TrendyolStoppage> allStoppages = stoppageRepository
                .findByStoreIdAndTransactionDateBetweenOrderByTransactionDateDesc(storeId, startDateTime, endDateTime);

        // Filter only stopaj records
        List<TrendyolStoppage> filteredStoppages = allStoppages.stream()
                .filter(s -> s.getDescription() != null && s.getDescription().toLowerCase().contains("stopaj"))
                .collect(Collectors.toList());

        // Manual pagination
        int start = page * size;
        int end = Math.min(start + size, filteredStoppages.size());

        List<StoppageDto> pageContent = start < filteredStoppages.size()
                ? filteredStoppages.subList(start, end).stream()
                    .map(StoppageDto::fromEntity)
                    .collect(Collectors.toList())
                : Collections.emptyList();

        int totalPages = (int) Math.ceil((double) filteredStoppages.size() / size);
        boolean hasNext = page < totalPages - 1;

        // Calculate total amount
        BigDecimal totalAmount = filteredStoppages.stream()
                .map(TrendyolStoppage::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return StoppageSummaryDto.builder()
                .storeId(storeId.toString())
                .periodStart(startDate)
                .periodEnd(endDate)
                .count(filteredStoppages.size())
                .totalAmount(totalAmount)
                .items(pageContent)
                .page(page)
                .size(size)
                .totalElements(filteredStoppages.size())
                .totalPages(totalPages)
                .hasNext(hasNext)
                .build();
    }

    // ================================================================================
    // ÜRÜN BAZLI GİDER DAĞILIMI (Product Expense Breakdown)
    // ================================================================================

    /**
     * Get expense breakdown for a specific product (barcode) in a date range.
     * Categorizes expenses into: platform service fee, international shipping, penalties, other.
     * Used for "Detay" panel in Product Detail view.
     *
     * @param storeId   The store ID
     * @param barcode   The product barcode (SKU)
     * @param startDate Start date of the period
     * @param endDate   End date of the period
     * @return ProductExpenseBreakdownDto containing categorized expenses
     */
    @Transactional(readOnly = true)
    public ProductExpenseBreakdownDto getProductExpenseBreakdown(UUID storeId, String barcode,
                                                                  LocalDate startDate, LocalDate endDate) {
        log.info("Getting expense breakdown for product {} in store: {} from {} to {}",
                barcode, storeId, startDate, endDate);

        // Verify store exists
        storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));

        // Convert dates to LocalDateTime for query
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        // Get all deduction invoices for this barcode
        List<TrendyolDeductionInvoice> deductions = deductionInvoiceRepository
                .findByStoreIdAndBarcodeAndDateRange(storeId, barcode, startDateTime, endDateTime);

        log.info("Found {} deduction invoices for product {} in date range", deductions.size(), barcode);

        // Categorize expenses
        BigDecimal platformServiceFee = BigDecimal.ZERO;
        int platformServiceFeeCount = 0;

        BigDecimal internationalShippingFee = BigDecimal.ZERO;
        int internationalShippingCount = 0;

        BigDecimal penaltyFee = BigDecimal.ZERO;
        int penaltyCount = 0;
        List<ProductExpenseBreakdownDto.ExpenseItemDto> penaltyItems = new ArrayList<>();

        BigDecimal otherExpenses = BigDecimal.ZERO;
        int otherExpenseCount = 0;
        List<ProductExpenseBreakdownDto.ExpenseItemDto> otherExpenseItems = new ArrayList<>();

        for (TrendyolDeductionInvoice deduction : deductions) {
            String transactionType = deduction.getTransactionType();
            BigDecimal amount = deduction.getDebt() != null ? deduction.getDebt() : BigDecimal.ZERO;

            if (isPlatformServiceFee(transactionType)) {
                platformServiceFee = platformServiceFee.add(amount);
                platformServiceFeeCount++;
            } else if (isInternationalFee(transactionType)) {
                internationalShippingFee = internationalShippingFee.add(amount);
                internationalShippingCount++;
            } else if (isPenaltyFee(transactionType)) {
                penaltyFee = penaltyFee.add(amount);
                penaltyCount++;
                penaltyItems.add(createExpenseItem(deduction));
            } else {
                // Other expenses (excluding commission - it's already shown separately)
                if (!isCommissionFee(transactionType)) {
                    otherExpenses = otherExpenses.add(amount);
                    otherExpenseCount++;
                    otherExpenseItems.add(createExpenseItem(deduction));
                }
            }
        }

        // Calculate totals
        BigDecimal totalExpenses = platformServiceFee
                .add(internationalShippingFee)
                .add(penaltyFee)
                .add(otherExpenses);
        int totalTransactionCount = platformServiceFeeCount + internationalShippingCount
                + penaltyCount + otherExpenseCount;

        // VAT is typically 20% for these services (already included in debt amount)
        // Calculate VAT from gross: vatAmount = gross * (20/120)
        BigDecimal totalVatAmount = totalExpenses.multiply(new BigDecimal("0.20"))
                .divide(new BigDecimal("1.20"), 2, RoundingMode.HALF_UP);

        // Get product info
        String productName = null;
        String productImageUrl = null;
        Optional<TrendyolProduct> productOpt = productRepository.findByStoreIdAndBarcode(storeId, barcode);
        if (productOpt.isPresent()) {
            TrendyolProduct product = productOpt.get();
            productName = product.getTitle();
            productImageUrl = product.getImage();
        }

        log.info("Expense breakdown for product {}: platform={}, international={}, penalty={}, other={}, total={}",
                barcode, platformServiceFee, internationalShippingFee, penaltyFee, otherExpenses, totalExpenses);

        return ProductExpenseBreakdownDto.builder()
                .barcode(barcode)
                .productName(productName)
                .productImageUrl(productImageUrl)
                .platformServiceFee(platformServiceFee)
                .platformServiceFeeCount(platformServiceFeeCount)
                .internationalShippingFee(internationalShippingFee)
                .internationalShippingCount(internationalShippingCount)
                .penaltyFee(penaltyFee)
                .penaltyCount(penaltyCount)
                .penaltyItems(penaltyItems)
                .otherExpenses(otherExpenses)
                .otherExpenseCount(otherExpenseCount)
                .otherExpenseItems(otherExpenseItems)
                .totalExpenses(totalExpenses)
                .totalVatAmount(totalVatAmount)
                .totalTransactionCount(totalTransactionCount)
                .hasExpenseData(!deductions.isEmpty())
                .build();
    }

    /**
     * Check if transaction type is a platform service fee
     */
    private boolean isPlatformServiceFee(String transactionType) {
        if (transactionType == null) return false;
        String lower = transactionType.toLowerCase();
        return lower.contains("platform") && lower.contains("hizmet");
    }

    /**
     * Check if transaction type is an international fee
     */
    private boolean isInternationalFee(String transactionType) {
        if (transactionType == null) return false;
        String lower = transactionType.toLowerCase();
        return lower.contains("uluslararası") || lower.contains("yurt dışı") || lower.contains("yurtdışı");
    }

    /**
     * Check if transaction type is a penalty fee
     */
    private boolean isPenaltyFee(String transactionType) {
        if (transactionType == null) return false;
        String lower = transactionType.toLowerCase();
        return lower.contains("tedarik edememe") || lower.contains("termin gecikme")
                || lower.contains("eksik ürün") || lower.contains("yanlış ürün")
                || lower.contains("kusurlu ürün") || lower.contains("ceza");
    }

    /**
     * Check if transaction type is a commission fee
     */
    private boolean isCommissionFee(String transactionType) {
        if (transactionType == null) return false;
        String lower = transactionType.toLowerCase();
        return lower.contains("komisyon");
    }

    /**
     * Create expense item DTO from deduction invoice
     */
    private ProductExpenseBreakdownDto.ExpenseItemDto createExpenseItem(TrendyolDeductionInvoice deduction) {
        BigDecimal amount = deduction.getDebt() != null ? deduction.getDebt() : BigDecimal.ZERO;
        BigDecimal vatAmount = amount.multiply(new BigDecimal("0.20"))
                .divide(new BigDecimal("1.20"), 2, RoundingMode.HALF_UP);

        return ProductExpenseBreakdownDto.ExpenseItemDto.builder()
                .transactionType(deduction.getTransactionType())
                .description(deduction.getDescription())
                .amount(amount)
                .vatAmount(vatAmount)
                .orderNumber(deduction.getOrderNumber())
                .invoiceSerialNumber(deduction.getInvoiceSerialNumber())
                .build();
    }
}
