package com.ecommerce.sellerx.currency;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * REST controller for currency exchange rate operations.
 */
@RestController
@RequestMapping("/currency")
@RequiredArgsConstructor
public class CurrencyController {

    private final CurrencyService currencyService;

    /**
     * Get all current exchange rates.
     * Response: { "USD_TRY": 34.5, "EUR_TRY": 37.2, "TRY_USD": 0.029, "TRY_EUR": 0.027 }
     */
    @GetMapping("/rates")
    public ResponseEntity<Map<String, BigDecimal>> getRates() {
        return ResponseEntity.ok(currencyService.getAllRates());
    }

    /**
     * Get specific exchange rate between two currencies.
     */
    @GetMapping("/rates/{from}/{to}")
    public ResponseEntity<Map<String, Object>> getRate(
            @PathVariable String from,
            @PathVariable String to) {
        BigDecimal rate = currencyService.getRate(from.toUpperCase(), to.toUpperCase());
        return ResponseEntity.ok(Map.of(
                "from", from.toUpperCase(),
                "to", to.toUpperCase(),
                "rate", rate
        ));
    }

    /**
     * Convert an amount from one currency to another.
     *
     * @param amount Amount to convert
     * @param from   Source currency (TRY, USD, EUR)
     * @param to     Target currency (TRY, USD, EUR)
     */
    @GetMapping("/convert")
    public ResponseEntity<Map<String, Object>> convert(
            @RequestParam BigDecimal amount,
            @RequestParam String from,
            @RequestParam String to) {

        String fromCurrency = from.toUpperCase();
        String toCurrency = to.toUpperCase();

        BigDecimal converted = currencyService.convert(amount, fromCurrency, toCurrency);
        BigDecimal rate = currencyService.getRate(fromCurrency, toCurrency);

        return ResponseEntity.ok(Map.of(
                "originalAmount", amount,
                "fromCurrency", fromCurrency,
                "toCurrency", toCurrency,
                "convertedAmount", converted,
                "rate", rate
        ));
    }

    /**
     * Force update exchange rates from TCMB (admin endpoint).
     * Useful for manual refresh if rates seem outdated.
     */
    @PostMapping("/rates/refresh")
    public ResponseEntity<Map<String, Object>> refreshRates() {
        currencyService.forceUpdateRates();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Exchange rates updated from TCMB",
                "rates", currencyService.getAllRates()
        ));
    }
}
