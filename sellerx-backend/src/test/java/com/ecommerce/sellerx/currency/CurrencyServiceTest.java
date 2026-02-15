package com.ecommerce.sellerx.currency;

import com.ecommerce.sellerx.common.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CurrencyServiceTest extends BaseUnitTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private TcmbApiClient tcmbApiClient;

    private CurrencyService currencyService;

    @BeforeEach
    void setUp() {
        currencyService = new CurrencyService(exchangeRateRepository, tcmbApiClient);
    }

    @Nested
    @DisplayName("getRate")
    class GetRate {

        @Test
        @DisplayName("should return ONE for same currency")
        void shouldReturnOneForSameCurrency() {
            BigDecimal result = currencyService.getRate("TRY", "TRY");

            assertThat(result).isEqualByComparingTo(BigDecimal.ONE);
            verifyNoInteractions(exchangeRateRepository);
        }

        @Test
        @DisplayName("should return rate from database when found and valid")
        void shouldReturnRateFromDatabase() {
            ExchangeRate rate = ExchangeRate.builder()
                    .baseCurrency("USD")
                    .targetCurrency("TRY")
                    .rate(new BigDecimal("34.50"))
                    .validUntil(LocalDateTime.now().plusDays(1))
                    .build();

            when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrency("USD", "TRY"))
                    .thenReturn(Optional.of(rate));

            BigDecimal result = currencyService.getRate("USD", "TRY");

            assertThat(result).isEqualByComparingTo(new BigDecimal("34.50"));
        }

        @Test
        @DisplayName("should fallback to inverse rate when direct rate not found")
        void shouldFallbackToInverseRate() {
            when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrency("TRY", "USD"))
                    .thenReturn(Optional.empty());

            ExchangeRate inverseRate = ExchangeRate.builder()
                    .baseCurrency("USD")
                    .targetCurrency("TRY")
                    .rate(new BigDecimal("34.00"))
                    .validUntil(LocalDateTime.now().plusDays(1))
                    .build();

            when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrency("USD", "TRY"))
                    .thenReturn(Optional.of(inverseRate));

            BigDecimal result = currencyService.getRate("TRY", "USD");

            BigDecimal expected = BigDecimal.ONE.divide(new BigDecimal("34.00"), 8, RoundingMode.HALF_UP);
            assertThat(result).isEqualByComparingTo(expected);
        }

        @Test
        @DisplayName("should return ONE when no rate found at all")
        void shouldReturnOneWhenNoRateFound() {
            when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrency(any(), any()))
                    .thenReturn(Optional.empty());

            BigDecimal result = currencyService.getRate("USD", "EUR");

            assertThat(result).isEqualByComparingTo(BigDecimal.ONE);
        }

        @Test
        @DisplayName("should return stale rate when rate is expired")
        void shouldReturnStaleRateWhenExpired() {
            ExchangeRate expiredRate = ExchangeRate.builder()
                    .baseCurrency("USD")
                    .targetCurrency("TRY")
                    .rate(new BigDecimal("33.00"))
                    .validUntil(LocalDateTime.now().minusDays(1)) // expired
                    .build();

            when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrency("USD", "TRY"))
                    .thenReturn(Optional.of(expiredRate));
            // inverse not found
            when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrency("TRY", "USD"))
                    .thenReturn(Optional.empty());

            BigDecimal result = currencyService.getRate("USD", "TRY");

            // Should return stale rate (33.00), NOT BigDecimal.ONE
            assertThat(result).isEqualByComparingTo(new BigDecimal("33.00"));
        }

        @Test
        @DisplayName("should return stale inverse rate when both direct and inverse are expired")
        void shouldReturnStaleInverseRateWhenExpired() {
            // Direct rate not found
            when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrency("TRY", "USD"))
                    .thenReturn(Optional.empty());

            // Inverse rate expired
            ExchangeRate expiredInverse = ExchangeRate.builder()
                    .baseCurrency("USD")
                    .targetCurrency("TRY")
                    .rate(new BigDecimal("42.00"))
                    .validUntil(LocalDateTime.now().minusDays(1))
                    .build();

            when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrency("USD", "TRY"))
                    .thenReturn(Optional.of(expiredInverse));

            BigDecimal result = currencyService.getRate("TRY", "USD");

            // Should return 1/42.00, NOT BigDecimal.ONE
            BigDecimal expected = BigDecimal.ONE.divide(new BigDecimal("42.00"), 8, RoundingMode.HALF_UP);
            assertThat(result).isEqualByComparingTo(expected);
        }
    }

    @Nested
    @DisplayName("convert")
    class Convert {

        @Test
        @DisplayName("should return same amount for same currency")
        void shouldReturnSameAmountForSameCurrency() {
            BigDecimal amount = new BigDecimal("100.00");

            BigDecimal result = currencyService.convert(amount, "TRY", "TRY");

            assertThat(result).isEqualByComparingTo(amount);
        }

        @Test
        @DisplayName("should convert amount using exchange rate")
        void shouldConvertAmount() {
            ExchangeRate rate = ExchangeRate.builder()
                    .baseCurrency("USD")
                    .targetCurrency("TRY")
                    .rate(new BigDecimal("34.50"))
                    .validUntil(LocalDateTime.now().plusDays(1))
                    .build();

            when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrency("USD", "TRY"))
                    .thenReturn(Optional.of(rate));

            BigDecimal result = currencyService.convert(new BigDecimal("100.00"), "USD", "TRY");

            assertThat(result).isEqualByComparingTo(new BigDecimal("3450.00"));
        }
    }

    @Nested
    @DisplayName("updateExchangeRates")
    class UpdateExchangeRates {

        @Test
        @DisplayName("should fetch rates from TCMB and save them")
        void shouldFetchAndSaveRates() {
            TcmbRates rates = TcmbRates.builder()
                    .usdTry(new BigDecimal("34.50"))
                    .eurTry(new BigDecimal("37.20"))
                    .build();

            when(tcmbApiClient.fetchRates()).thenReturn(rates);
            when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrency(any(), any()))
                    .thenReturn(Optional.empty());
            when(exchangeRateRepository.save(any(ExchangeRate.class))).thenAnswer(i -> i.getArgument(0));

            currencyService.updateExchangeRates();

            // 4 rates saved: USD->TRY, TRY->USD, EUR->TRY, TRY->EUR
            verify(exchangeRateRepository, times(4)).save(any(ExchangeRate.class));
        }

        @Test
        @DisplayName("should handle TCMB API failure gracefully")
        void shouldHandleTcmbApiFailure() {
            when(tcmbApiClient.fetchRates()).thenThrow(new RuntimeException("Connection timeout"));

            // Should not throw
            currencyService.updateExchangeRates();

            verify(exchangeRateRepository, never()).save(any());
        }

        @Test
        @DisplayName("should update existing exchange rate entity")
        void shouldUpdateExistingRate() {
            TcmbRates rates = TcmbRates.builder()
                    .usdTry(new BigDecimal("35.00"))
                    .eurTry(new BigDecimal("38.00"))
                    .build();

            ExchangeRate existingRate = ExchangeRate.builder()
                    .baseCurrency("USD")
                    .targetCurrency("TRY")
                    .rate(new BigDecimal("34.50"))
                    .source("TCMB")
                    .fetchedAt(LocalDateTime.now().minusDays(1))
                    .validUntil(LocalDateTime.now().minusHours(1))
                    .build();
            existingRate.setId(1L);

            when(tcmbApiClient.fetchRates()).thenReturn(rates);
            lenient().when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrency(any(), any()))
                    .thenReturn(Optional.empty());
            when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrency("USD", "TRY"))
                    .thenReturn(Optional.of(existingRate));
            when(exchangeRateRepository.save(any(ExchangeRate.class))).thenAnswer(i -> i.getArgument(0));

            currencyService.updateExchangeRates();

            ArgumentCaptor<ExchangeRate> captor = ArgumentCaptor.forClass(ExchangeRate.class);
            verify(exchangeRateRepository, atLeast(1)).save(captor.capture());

            // Verify at least one saved rate has the updated value
            boolean foundUpdated = captor.getAllValues().stream()
                    .anyMatch(r -> "USD".equals(r.getBaseCurrency()) &&
                            "TRY".equals(r.getTargetCurrency()) &&
                            r.getRate().compareTo(new BigDecimal("35.00")) == 0);
            assertThat(foundUpdated).isTrue();
        }

        @Test
        @DisplayName("should set validUntil to 3 days ahead")
        void shouldSetValidUntilToThreeDays() {
            TcmbRates rates = TcmbRates.builder()
                    .usdTry(new BigDecimal("43.65"))
                    .eurTry(new BigDecimal("47.00"))
                    .build();

            when(tcmbApiClient.fetchRates()).thenReturn(rates);
            when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrency(any(), any()))
                    .thenReturn(Optional.empty());
            when(exchangeRateRepository.save(any(ExchangeRate.class))).thenAnswer(i -> i.getArgument(0));

            LocalDateTime before = LocalDateTime.now().plusDays(2).plusHours(23);
            currencyService.updateExchangeRates();
            LocalDateTime after = LocalDateTime.now().plusDays(3).plusMinutes(1);

            ArgumentCaptor<ExchangeRate> captor = ArgumentCaptor.forClass(ExchangeRate.class);
            verify(exchangeRateRepository, atLeast(1)).save(captor.capture());

            captor.getAllValues().forEach(saved -> {
                assertThat(saved.getValidUntil()).isAfter(before);
                assertThat(saved.getValidUntil()).isBefore(after);
            });
        }
    }

    @Nested
    @DisplayName("initRates")
    class InitRates {

        @Test
        @DisplayName("should fetch rates when no rates exist in DB")
        void shouldFetchRatesWhenMissing() {
            when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrency("USD", "TRY"))
                    .thenReturn(Optional.empty());

            TcmbRates rates = TcmbRates.builder()
                    .usdTry(new BigDecimal("43.65"))
                    .eurTry(new BigDecimal("47.00"))
                    .build();
            when(tcmbApiClient.fetchRates()).thenReturn(rates);
            when(exchangeRateRepository.save(any(ExchangeRate.class))).thenAnswer(i -> i.getArgument(0));

            currencyService.initRates();

            verify(tcmbApiClient).fetchRates();
            verify(exchangeRateRepository, times(4)).save(any(ExchangeRate.class));
        }

        @Test
        @DisplayName("should fetch rates when existing rates are expired")
        void shouldFetchRatesWhenExpired() {
            ExchangeRate expiredRate = ExchangeRate.builder()
                    .baseCurrency("USD")
                    .targetCurrency("TRY")
                    .rate(new BigDecimal("34.50"))
                    .validUntil(LocalDateTime.now().minusDays(1))
                    .build();

            // initRates checks USD/TRY, saveRate also looks up existing rates
            lenient().when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrency(any(), any()))
                    .thenReturn(Optional.empty());
            when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrency("USD", "TRY"))
                    .thenReturn(Optional.of(expiredRate));

            TcmbRates rates = TcmbRates.builder()
                    .usdTry(new BigDecimal("43.65"))
                    .eurTry(new BigDecimal("47.00"))
                    .build();
            when(tcmbApiClient.fetchRates()).thenReturn(rates);
            when(exchangeRateRepository.save(any(ExchangeRate.class))).thenAnswer(i -> i.getArgument(0));

            currencyService.initRates();

            verify(tcmbApiClient).fetchRates();
            verify(exchangeRateRepository, times(4)).save(any(ExchangeRate.class));
        }

        @Test
        @DisplayName("should skip fetch when rates are still valid")
        void shouldSkipFetchWhenValid() {
            ExchangeRate validRate = ExchangeRate.builder()
                    .baseCurrency("USD")
                    .targetCurrency("TRY")
                    .rate(new BigDecimal("43.65"))
                    .validUntil(LocalDateTime.now().plusDays(2))
                    .build();

            when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrency("USD", "TRY"))
                    .thenReturn(Optional.of(validRate));

            currencyService.initRates();

            verify(tcmbApiClient, never()).fetchRates();
            verify(exchangeRateRepository, never()).save(any());
        }

        @Test
        @DisplayName("should not block startup when TCMB fails")
        void shouldNotBlockStartupOnTcmbFailure() {
            when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrency("USD", "TRY"))
                    .thenReturn(Optional.empty());
            when(tcmbApiClient.fetchRates()).thenThrow(new RuntimeException("TCMB unavailable"));

            // Should not throw — startup continues with stale/no rates
            currencyService.initRates();

            verify(tcmbApiClient).fetchRates();
            verify(exchangeRateRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getAllRates")
    class GetAllRates {

        @Test
        @DisplayName("should return map with all currency pairs")
        void shouldReturnAllCurrencyPairs() {
            // All lookups will return empty, so rates default to ONE
            when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrency(any(), any()))
                    .thenReturn(Optional.empty());

            Map<String, BigDecimal> result = currencyService.getAllRates();

            assertThat(result).containsKeys("USD_TRY", "EUR_TRY", "TRY_USD", "TRY_EUR");
            assertThat(result).hasSize(4);
        }
    }
}
