package com.ecommerce.sellerx.currency;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service for currency conversion and exchange rate management.
 * Fetches rates from TCMB (Turkish Central Bank) daily.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final TcmbApiClient tcmbApiClient;

    // Supported currencies
    public static final String TRY = "TRY";
    public static final String USD = "USD";
    public static final String EUR = "EUR";

    /**
     * On startup, check if exchange rates are missing or expired and refresh from TCMB.
     */
    @PostConstruct
    @CacheEvict(value = "exchangeRates", allEntries = true)
    public void initRates() {
        log.info("Checking exchange rates on startup...");
        Optional<ExchangeRate> usdTry = exchangeRateRepository
                .findByBaseCurrencyAndTargetCurrency(USD, TRY);

        boolean needsRefresh = usdTry.isEmpty() || !usdTry.get().isValid();

        if (needsRefresh) {
            log.info("Exchange rates missing or expired, fetching from TCMB...");
            try {
                TcmbRates rates = tcmbApiClient.fetchRates();

                saveRate(USD, TRY, rates.getUsdTry());
                saveRate(TRY, USD, BigDecimal.ONE.divide(rates.getUsdTry(), 8, RoundingMode.HALF_UP));
                saveRate(EUR, TRY, rates.getEurTry());
                saveRate(TRY, EUR, BigDecimal.ONE.divide(rates.getEurTry(), 8, RoundingMode.HALF_UP));

                log.info("Exchange rates refreshed on startup: USD/TRY={}, EUR/TRY={}",
                        rates.getUsdTry(), rates.getEurTry());
            } catch (Exception e) {
                log.error("Failed to refresh exchange rates on startup. Stale rates will be used as fallback.", e);
            }
        } else {
            log.info("Exchange rates are valid (USD/TRY={}, expires={})",
                    usdTry.get().getRate(), usdTry.get().getValidUntil());
        }
    }

    /**
     * Get exchange rate from cache or database.
     * Falls back to stale (expired) rates before returning BigDecimal.ONE.
     */
    @Cacheable(value = "exchangeRates", key = "#baseCurrency + '_' + #targetCurrency")
    public BigDecimal getRate(String baseCurrency, String targetCurrency) {
        if (baseCurrency.equals(targetCurrency)) {
            return BigDecimal.ONE;
        }

        Optional<ExchangeRate> rate = exchangeRateRepository
                .findByBaseCurrencyAndTargetCurrency(baseCurrency, targetCurrency);

        if (rate.isPresent() && rate.get().isValid()) {
            return rate.get().getRate();
        }

        // Fallback: try inverse rate (valid)
        Optional<ExchangeRate> inverseRate = exchangeRateRepository
                .findByBaseCurrencyAndTargetCurrency(targetCurrency, baseCurrency);

        if (inverseRate.isPresent() && inverseRate.get().isValid()) {
            return BigDecimal.ONE.divide(inverseRate.get().getRate(), 8, RoundingMode.HALF_UP);
        }

        // Fallback: use stale (expired) rate rather than returning 1.0
        if (rate.isPresent()) {
            log.warn("Using stale exchange rate for {} -> {} (expired at {})",
                    baseCurrency, targetCurrency, rate.get().getValidUntil());
            return rate.get().getRate();
        }
        if (inverseRate.isPresent()) {
            log.warn("Using stale inverse exchange rate for {} -> {} (expired at {})",
                    baseCurrency, targetCurrency, inverseRate.get().getValidUntil());
            return BigDecimal.ONE.divide(inverseRate.get().getRate(), 8, RoundingMode.HALF_UP);
        }

        log.warn("No exchange rate found for {} -> {}, returning 1", baseCurrency, targetCurrency);
        return BigDecimal.ONE;
    }

    /**
     * Convert amount from one currency to another.
     */
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }
        BigDecimal rate = getRate(fromCurrency, toCurrency);
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Get all current exchange rates as a map.
     * Keys: "USD_TRY", "EUR_TRY", "TRY_USD", "TRY_EUR"
     */
    public Map<String, BigDecimal> getAllRates() {
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("USD_TRY", getRate(USD, TRY));
        rates.put("EUR_TRY", getRate(EUR, TRY));
        rates.put("TRY_USD", getRate(TRY, USD));
        rates.put("TRY_EUR", getRate(TRY, EUR));
        return rates;
    }

    /**
     * Scheduled job to update exchange rates daily at 10:00 AM Turkey time.
     * TCMB publishes new rates around 3:30 PM, but we use 10 AM for morning access.
     */
    @Scheduled(cron = "0 0 10 * * ?", zone = "Europe/Istanbul")
    @CacheEvict(value = "exchangeRates", allEntries = true)
    public void updateExchangeRates() {
        log.info("Starting scheduled exchange rate update from TCMB");
        try {
            TcmbRates rates = tcmbApiClient.fetchRates();

            // Save USD/TRY and inverse
            saveRate(USD, TRY, rates.getUsdTry());
            saveRate(TRY, USD, BigDecimal.ONE.divide(rates.getUsdTry(), 8, RoundingMode.HALF_UP));

            // Save EUR/TRY and inverse
            saveRate(EUR, TRY, rates.getEurTry());
            saveRate(TRY, EUR, BigDecimal.ONE.divide(rates.getEurTry(), 8, RoundingMode.HALF_UP));

            log.info("Exchange rates updated successfully: USD/TRY={}, EUR/TRY={}",
                    rates.getUsdTry(), rates.getEurTry());
        } catch (Exception e) {
            log.error("Failed to update exchange rates from TCMB", e);
        }
    }

    /**
     * Manually trigger exchange rate update (for admin use or initialization)
     */
    @CacheEvict(value = "exchangeRates", allEntries = true)
    public void forceUpdateRates() {
        log.info("Manual exchange rate update triggered");
        updateExchangeRates();
    }

    /**
     * Save or update exchange rate in database
     */
    private void saveRate(String base, String target, BigDecimal rate) {
        ExchangeRate entity = exchangeRateRepository
                .findByBaseCurrencyAndTargetCurrency(base, target)
                .orElse(new ExchangeRate());

        entity.setBaseCurrency(base);
        entity.setTargetCurrency(target);
        entity.setRate(rate);
        entity.setSource("TCMB");
        entity.setFetchedAt(LocalDateTime.now());
        entity.setValidUntil(LocalDateTime.now().plusDays(3));

        exchangeRateRepository.save(entity);
    }
}
