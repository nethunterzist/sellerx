package com.ecommerce.sellerx.financial;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendyolFinancialSettlementScheduledService {

    private final TrendyolFinancialSettlementService settlementService;

    /**
     * Automatically sync settlements for all stores every day at 2 AM
     * This runs after the order sync which happens at 1 AM
     */
    @Scheduled(cron = "0 0 2 * * *") // Her gün saat 2:00'de çalışır
    public void syncSettlementsForAllStores() {
        log.info("Starting scheduled settlement sync for all stores");
        
        try {
            settlementService.fetchAndUpdateSettlementsForAllStores();
            log.info("Scheduled settlement sync completed successfully");
        } catch (Exception e) {
            log.error("Scheduled settlement sync failed", e);
        }
    }

    /**
     * Sync settlements every 6 hours to catch any recent settlements
     */
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000) // 6 saatte bir
    public void syncSettlementsRegularly() {
        log.info("Starting regular settlement sync for all stores");
        
        try {
            settlementService.fetchAndUpdateSettlementsForAllStores();
            log.info("Regular settlement sync completed successfully");
        } catch (Exception e) {
            log.error("Regular settlement sync failed", e);
        }
    }
}
