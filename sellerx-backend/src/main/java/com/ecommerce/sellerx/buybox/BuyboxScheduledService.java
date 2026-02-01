package com.ecommerce.sellerx.buybox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Buybox kontrol scheduled job servisi.
 * Her 12 saatte bir tüm aktif takip edilen ürünlerin buybox durumunu kontrol eder.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BuyboxScheduledService {

    private final BuyboxTrackedProductRepository trackedProductRepository;
    private final BuyboxService buyboxService;

    /**
     * Her 12 saatte bir (00:00 ve 12:00) buybox kontrolü yapar.
     * Timezone: Europe/Istanbul
     */
    @Scheduled(cron = "0 0 */12 * * ?", zone = "Europe/Istanbul")
    public void checkAllTrackedProducts() {
        log.info("Starting scheduled buybox check at {}", LocalDateTime.now());

        List<BuyboxTrackedProduct> allTracked = trackedProductRepository.findAllActiveForScheduledCheck();

        if (allTracked.isEmpty()) {
            log.info("No active tracked products found, skipping buybox check");
            return;
        }

        log.info("Found {} active tracked products to check", allTracked.size());

        int successCount = 0;
        int failCount = 0;

        for (BuyboxTrackedProduct tracked : allTracked) {
            try {
                buyboxService.checkBuyboxForProduct(tracked);
                successCount++;
                log.debug("Buybox check successful for product: {} (store: {})",
                        tracked.getProduct().getTitle(),
                        tracked.getStore().getStoreName());
            } catch (Exception e) {
                failCount++;
                log.error("Buybox check failed for product {} (store: {}): {}",
                        tracked.getProduct().getId(),
                        tracked.getStore().getId(),
                        e.getMessage());
            }

            // Rate limiting için 1 saniye bekle (10 req/sec limiti ile uyumlu)
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        log.info("Scheduled buybox check completed. Success: {}, Failed: {}, Total: {}",
                successCount, failCount, allTracked.size());
    }

    /**
     * Manuel tetikleme için (test amaçlı).
     */
    public void triggerManualCheck() {
        log.info("Manual buybox check triggered");
        checkAllTrackedProducts();
    }
}
