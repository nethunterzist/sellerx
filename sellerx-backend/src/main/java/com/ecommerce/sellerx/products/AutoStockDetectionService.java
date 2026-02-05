package com.ecommerce.sellerx.products;

import com.ecommerce.sellerx.alerts.*;
import com.ecommerce.sellerx.purchasing.PurchaseOrderItem;
import com.ecommerce.sellerx.purchasing.PurchaseOrderItemRepository;
import com.ecommerce.sellerx.purchasing.PurchaseOrderStatus;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.users.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Detects stock increases from Trendyol sync and creates a PENDING_APPROVAL
 * alert for the user. The user must approve the stock entry before a
 * CostAndStockInfo record is created. This prevents false entries from
 * returns or inventory adjustments.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutoStockDetectionService {

    private final TrendyolProductRepository productRepository;
    private final PurchaseOrderItemRepository poItemRepository;
    private final AlertHistoryRepository alertHistoryRepository;

    private static final int PO_LOOKBACK_DAYS = 2;
    private static final double PO_QUANTITY_TOLERANCE = 0.20; // ±20%

    /**
     * Called from saveOrUpdateProduct() when trendyolQuantity increases.
     *
     * @param product     the product entity (already saved with new trendyolQuantity)
     * @param oldQuantity previous trendyol quantity
     * @param newQuantity new trendyol quantity
     */
    @Transactional
    public void handleStockIncrease(TrendyolProduct product, int oldQuantity, int newQuantity) {
        int delta = newQuantity - oldQuantity;
        if (delta <= 0) return;

        Store store = product.getStore();

        log.info("[AUTO_STOCK] Detected stock increase for product {} ({}): {} -> {} (delta: +{})",
                product.getBarcode(), product.getTitle(), oldQuantity, newQuantity, delta);

        // 1. Check if a recent PO close explains this increase
        if (isExplainedByRecentPO(product, delta)) {
            log.info("[AUTO_STOCK] Stock increase for {} explained by recent PO close, skipping",
                    product.getBarcode());
            return;
        }

        // 2. Find last known cost (for alert data, not for creating entry)
        CostAndStockInfo lastKnown = getLastKnownCost(product);

        // 3. Create PENDING_APPROVAL alert — user must approve before cost entry is created
        createAlert(store, product, delta, lastKnown);

        log.info("[AUTO_STOCK] Pending approval alert created for {} : +{} units (awaiting user decision)",
                product.getBarcode(), delta);
    }

    /**
     * Check if a PO was CLOSED within the last N days for this product
     * with a quantity within ±tolerance% of the detected delta.
     */
    boolean isExplainedByRecentPO(TrendyolProduct product, int delta) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(PO_LOOKBACK_DAYS);

        List<PurchaseOrderItem> items = poItemRepository
                .findByProductIdAndStoreId(product.getId(), product.getStore().getId());

        for (PurchaseOrderItem item : items) {
            var po = item.getPurchaseOrder();
            if (po.getStatus() != PurchaseOrderStatus.CLOSED) continue;
            if (po.getUpdatedAt() == null || po.getUpdatedAt().isBefore(cutoff)) continue;

            int poQuantity = item.getUnitsOrdered();
            if (poQuantity == 0) continue;

            double ratio = (double) delta / poQuantity;
            if (ratio >= (1.0 - PO_QUANTITY_TOLERANCE) && ratio <= (1.0 + PO_QUANTITY_TOLERANCE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Find the most recent cost entry with a non-null unitCost.
     */
    CostAndStockInfo getLastKnownCost(TrendyolProduct product) {
        if (product.getCostAndStockInfo() == null || product.getCostAndStockInfo().isEmpty()) {
            return null;
        }
        return product.getCostAndStockInfo().stream()
                .filter(c -> c.getStockDate() != null && c.getUnitCost() != null)
                .max(Comparator.comparing(CostAndStockInfo::getStockDate))
                .orElse(null);
    }

    private void createAlert(Store store, TrendyolProduct product, int delta, CostAndStockInfo lastKnown) {
        User user = store.getUser();

        Map<String, Object> data = new HashMap<>();
        data.put("productId", product.getId().toString());
        data.put("barcode", product.getBarcode());
        data.put("productName", product.getTitle());
        data.put("delta", delta);
        data.put("pendingApproval", true);

        if (product.getImage() != null) {
            data.put("imageUrl", product.getImage());
        }

        String title;
        String message;
        AlertSeverity severity;

        if (lastKnown != null) {
            data.put("unitCost", lastKnown.getUnitCost());
            data.put("costVatRate", lastKnown.getCostVatRate());
            data.put("hasCostInfo", true);

            title = String.format("Stok Artisi: %s (+%d adet)",
                    truncate(product.getTitle(), 50), delta);
            message = String.format(
                    "%s (Barkod: %s) icin +%d adet stok artisi algilandi. " +
                    "Son maliyet: %.2f TL (KDV %%%d). " +
                    "Mal girisi olarak onaylamak ister misiniz?",
                    product.getTitle(), product.getBarcode(), delta,
                    lastKnown.getUnitCost(), lastKnown.getCostVatRate());
            severity = AlertSeverity.MEDIUM;
        } else {
            data.put("hasCostInfo", false);

            title = String.format("Stok Artisi: %s (+%d adet)",
                    truncate(product.getTitle(), 50), delta);
            message = String.format(
                    "%s (Barkod: %s) icin +%d adet stok artisi algilandi. " +
                    "Maliyet bilgisi bulunamadi. " +
                    "Mal girisi olarak onaylamak ister misiniz?",
                    product.getTitle(), product.getBarcode(), delta);
            severity = AlertSeverity.HIGH;
        }

        AlertHistory alert = AlertHistory.builder()
                .user(user)
                .store(store)
                .alertType(AlertType.STOCK)
                .title(title)
                .message(message)
                .severity(severity)
                .data(data)
                .status("PENDING_APPROVAL")
                .emailSent(false)
                .pushSent(false)
                .inAppSent(true)
                .build();

        alertHistoryRepository.save(alert);

        log.info("[AUTO_STOCK] Pending approval alert created for user {} : {}", user.getId(), title);
    }

    private static String truncate(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }
}
