package com.ecommerce.sellerx.currency;

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
     * Get exchange rate from cache or database.
     * Returns BigDecimal.ONE if same currency or rate not found.
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

        // Fallback: try inverse rate
        Optional<ExchangeRate> inverseRate = exchangeRateRepository
                .findByBaseCurrencyAndTargetCurrency(targetCurrency, baseCurrency);

        if (inverseRate.isPresent() && inverseRate.get().isValid()) {
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
        entity.setValidUntil(LocalDateTime.now().plusDays(1));

        exchangeRateRepository.save(entity);
    }
}
