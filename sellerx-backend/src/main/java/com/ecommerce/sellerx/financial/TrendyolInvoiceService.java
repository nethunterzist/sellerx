package com.ecommerce.sellerx.financial;

import com.ecommerce.sellerx.common.exception.ResourceNotFoundException;
import com.ecommerce.sellerx.financial.dto.AggregatedProductDto;
import com.ecommerce.sellerx.financial.dto.PurchaseVatDto;
import com.ecommerce.sellerx.financial.dto.SalesVatDto;
import com.ecommerce.sellerx.financial.dto.CargoInvoiceItemDto;
import com.ecommerce.sellerx.financial.dto.CommissionInvoiceItemDto;
import com.ecommerce.sellerx.financial.dto.InvoiceDetailDto;
import com.ecommerce.sellerx.financial.dto.InvoiceItemDto;
import com.ecommerce.sellerx.financial.dto.InvoiceSummaryDto;
import com.ecommerce.sellerx.financial.dto.InvoiceTypeStatsDto;
import com.ecommerce.sellerx.financial.dto.ProductCargoBreakdownDto;
import com.ecommerce.sellerx.financial.dto.ProductCommissionBreakdownDto;
import com.ecommerce.sellerx.financial.dto.TransactionTypeBreakdownDto;
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
    private final TrendyolOrderRepository orderRepository;
    private final TrendyolProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    // Invoice type to category mapping
    private static final Map<String, String> TYPE_TO_CATEGORY = Map.ofEntries(
            // Komisyon
            Map.entry("Platform Hizmet Bedeli", "KOMISYON"),
            Map.entry("AZ - Komisyon Faturası", "KOMISYON"),
            Map.entry("AZ-Komisyon Geliri", "KOMISYON"),
            Map.entry("Komisyon Faturası", "KOMISYON"),

            // Kargo
            Map.entry("Kargo Fatura", "KARGO"),
            Map.entry("Kargo Faturası", "KARGO"),
            Map.entry("AZ - Kargo Fatura", "KARGO"),
            Map.entry("MP Kargo İtiraz İade Faturası", "KARGO"),

            // Uluslararasi
            Map.entry("Uluslararası Hizmet Bedeli", "ULUSLARARASI"),
            Map.entry("AZ-Yurtdışı Operasyon Bedeli", "ULUSLARARASI"),
            Map.entry("AZ-YURTDÕ_Õ OPERASYON BEDELI %18", "ULUSLARARASI"),
            Map.entry("AZ-Platform Hizmet Bedeli", "ULUSLARARASI"),
            Map.entry("AZ-Uluslararası Hizmet Bedeli", "ULUSLARARASI"),
            Map.entry("Yurtdışı Operasyon Iade Bedeli", "ULUSLARARASI"),
            Map.entry("Yurt Dışı Operasyon Bedeli", "ULUSLARARASI"),

            // Ceza
            Map.entry("Tedarik Edememe", "CEZA"),
            Map.entry("TEDARIK EDEMEME FATURASI", "CEZA"),
            Map.entry("Termin Gecikme Bedeli", "CEZA"),
            Map.entry("Eksik Ürün Faturası", "CEZA"),
            Map.entry("Yanlış Ürün Faturası", "CEZA"),
            Map.entry("YANLIS URUN FATURASI", "CEZA"),
            Map.entry("Kusurlu Ürün Faturası", "CEZA"),

            // Reklam
            Map.entry("Reklam Bedeli", "REKLAM"),
            Map.entry("Sabit Bütçeli Influencer Reklam Bedeli", "REKLAM"),
            Map.entry("Komisyonlu İnfluencer Reklam Bedeli", "REKLAM"),

            // Iade (Geri yatan ödeme - refund/compensation)
            Map.entry("Tazmin Faturası", "IADE"),

            // Diger
            Map.entry("Kurumsal Kampanya Yansıtma Bedeli", "DIGER"),
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
            Map.entry("Kurumsal Kampanya Yansıtma Bedeli", "KURUMSAL_KAMPANYA"),
            Map.entry("Erken Ödeme Kesinti Faturası", "ERKEN_ODEME_KESINTI"),
            Map.entry("Fatura Kontör Satış Bedeli", "KONTOR_SATIS"),
            Map.entry("Müşteri Duyuruları Faturası", "MUSTERI_DUYURU"),
            Map.entry("TEX Tazmin - İşleme- %0", "TEX_TAZMIN"),
            Map.entry("Tazmin Faturası", "TAZMIN_FATURASI")
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

        // Build barcode -> vatRate map from products (order items don't have saleVatRate populated)
        Map<String, Integer> barcodeToVatRate = productRepository.findByStoreId(storeId).stream()
                .filter(p -> p.getBarcode() != null && p.getVatRate() != null)
                .collect(Collectors.toMap(TrendyolProduct::getBarcode, TrendyolProduct::getVatRate, (a, b) -> a));

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
                Integer vatRate = (item.getBarcode() != null) ? barcodeToVatRate.get(item.getBarcode()) : null;
                if (vatRate == null || vatRate <= 0) {
                    salesItemsWithoutVatRate += qty;
                    continue;
                }

                // Calculate sales amount (price * qty) and VAT from vatBaseAmount
                BigDecimal itemSalesAmount = (item.getPrice() != null)
                        ? item.getPrice().multiply(BigDecimal.valueOf(qty))
                        : BigDecimal.ZERO;

                // VAT = vatBaseAmount * vatRate / 100
                BigDecimal itemVatAmount = BigDecimal.ZERO;
                if (item.getVatBaseAmount() != null) {
                    itemVatAmount = item.getVatBaseAmount()
                            .multiply(BigDecimal.valueOf(vatRate))
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(qty));
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
                .build();

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
        List<TrendyolDeductionInvoice> filteredInvoices = allInvoices.stream()
                .filter(inv -> {
                    if ("IADE".equals(category)) {
                        // For IADE category, check if it's a refund by:
                        // 1. credit > 0 (direct refund from Trendyol)
                        // 2. transaction type is a refund type (iade or AZ-yurtdışı operasyon)
                        boolean hasCreditValue = inv.getCredit() != null && inv.getCredit().compareTo(BigDecimal.ZERO) > 0;
                        boolean isRefundByType = isRefundTransactionType(inv.getTransactionType());
                        return hasCreditValue || isRefundByType;
                    } else {
                        // For other categories, use the regular TYPE_TO_CATEGORY mapping
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
        if (upper.contains("IADE")) {
            return true;
        }

        // Check for AZ-Yurtdışı Operasyon Bedeli pattern (overseas operation refunds)
        // These show as negative amounts in Trendyol UI but come as debt in API
        // Pattern: starts with "AZ-" and contains "YURT" and "OPERASYON"
        if (upper.startsWith("AZ-") && upper.contains("YURT") && upper.contains("OPERASYON")) {
            return true;
        }

        return false;
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
     * - List of recent shipments (last 50)
     */
    @Transactional(readOnly = true)
    public ProductCargoBreakdownDto getProductCargoBreakdown(UUID storeId, String barcode,
                                                              LocalDate startDate, LocalDate endDate) {
        log.info("Getting cargo breakdown for product {} in store: {} from {} to {}",
                barcode, storeId, startDate, endDate);

        // Verify store exists
        storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId.toString()));

        // Fetch cargo invoices for this barcode
        List<TrendyolCargoInvoice> cargoInvoices = cargoInvoiceRepository
                .findByStoreIdAndBarcodeAndDateRange(storeId, barcode, startDate, endDate);

        log.info("Found {} cargo invoices for product {}", cargoInvoices.size(), barcode);

        // Calculate totals
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalVatAmount = BigDecimal.ZERO;
        BigDecimal totalDesi = BigDecimal.ZERO;
        Set<String> uniqueOrders = new HashSet<>();

        for (TrendyolCargoInvoice invoice : cargoInvoices) {
            if (invoice.getAmount() != null) {
                totalAmount = totalAmount.add(invoice.getAmount());
            }
            if (invoice.getVatAmount() != null) {
                totalVatAmount = totalVatAmount.add(invoice.getVatAmount());
            }
            if (invoice.getDesi() != null) {
                totalDesi = totalDesi.add(BigDecimal.valueOf(invoice.getDesi()));
            }
            if (invoice.getOrderNumber() != null) {
                uniqueOrders.add(invoice.getOrderNumber());
            }
        }

        int totalShipmentCount = cargoInvoices.size();
        int orderCount = uniqueOrders.size();

        // Calculate averages
        BigDecimal averageDesi = BigDecimal.ZERO;
        BigDecimal averageCostPerShipment = BigDecimal.ZERO;
        if (totalShipmentCount > 0) {
            averageDesi = totalDesi.divide(BigDecimal.valueOf(totalShipmentCount), 2, RoundingMode.HALF_UP);
            averageCostPerShipment = totalAmount.divide(BigDecimal.valueOf(totalShipmentCount), 2, RoundingMode.HALF_UP);
        }

        // Build shipment list (already limited to 50 by query)
        List<ProductCargoBreakdownDto.CargoShipmentDetailDto> shipments = cargoInvoices.stream()
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

        log.info("Cargo breakdown for product {}: {} shipments, {} orders, total cost: {}",
                barcode, totalShipmentCount, orderCount, totalAmount);

        return ProductCargoBreakdownDto.builder()
                .barcode(barcode)
                .productName(productName)
                .productImageUrl(productImageUrl)
                .productUrl(productUrl)
                .totalAmount(totalAmount)
                .totalVatAmount(totalVatAmount)
                .totalDesi(totalDesi)
                .totalShipmentCount(totalShipmentCount)
                .orderCount(orderCount)
                .averageDesi(averageDesi)
                .averageCostPerShipment(averageCostPerShipment)
                .shipments(shipments)
                .build();
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
}
