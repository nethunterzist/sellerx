package com.ecommerce.sellerx.currency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for exchange rate persistence operations.
 */
@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    /**
     * Find exchange rate by currency pair
     */
    Optional<ExchangeRate> findByBaseCurrencyAndTargetCurrency(String baseCurrency, String targetCurrency);

    /**
     * Find all rates for a base currency
     */
    List<ExchangeRate> findByBaseCurrency(String baseCurrency);

    /**
     * Find all rates to a target currency
     */
    List<ExchangeRate> findByTargetCurrency(String targetCurrency);
}
