"use client";

import {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  useMemo,
  type ReactNode,
} from "react";
import { useUserPreferences, useUpdatePreferences } from "@/hooks/queries/use-settings";

// =============================================================================
// Types
// =============================================================================

export type SupportedCurrency = "TRY" | "USD" | "EUR";

export interface ExchangeRates {
  USD_TRY: number;
  EUR_TRY: number;
  TRY_USD: number;
  TRY_EUR: number;
}

export interface CurrencyContextValue {
  /** Current display currency from user preferences */
  currency: SupportedCurrency;
  /** Update user's preferred currency */
  setCurrency: (currency: SupportedCurrency) => Promise<void>;
  /** Current exchange rates from TCMB */
  rates: ExchangeRates | null;
  /** Loading state for rates */
  isLoadingRates: boolean;
  /** Convert amount from source currency (default TRY) to display currency */
  convert: (amount: number, fromCurrency?: SupportedCurrency) => number;
  /** Format and convert amount with currency symbol */
  formatCurrency: (amount: number, fromCurrency?: SupportedCurrency, options?: FormatOptions) => string;
  /** Get currency symbol */
  getCurrencySymbol: (currency?: SupportedCurrency) => string;
  /** Get locale for number formatting */
  getCurrencyLocale: (currency?: SupportedCurrency) => string;
}

interface FormatOptions {
  showSymbol?: boolean;
  minimumFractionDigits?: number;
  maximumFractionDigits?: number;
  compact?: boolean; // Use compact notation (K, M) for large numbers
}

interface CurrencyProviderProps {
  children: ReactNode;
}

// =============================================================================
// Constants
// =============================================================================

const CURRENCY_CONFIG: Record<SupportedCurrency, { symbol: string; locale: string }> = {
  TRY: { symbol: "\u20BA", locale: "tr-TR" }, // ₺
  USD: { symbol: "$", locale: "en-US" },
  EUR: { symbol: "\u20AC", locale: "de-DE" }, // €
};

const FALLBACK_RATES: ExchangeRates = {
  USD_TRY: 43.65,
  EUR_TRY: 47.0,
  TRY_USD: 0.0229,
  TRY_EUR: 0.0213,
};

// =============================================================================
// Context
// =============================================================================

const CurrencyContext = createContext<CurrencyContextValue | undefined>(undefined);

// =============================================================================
// Provider
// =============================================================================

export function CurrencyProvider({ children }: CurrencyProviderProps) {
  // User preferences from backend
  const { data: preferences, isLoading: isLoadingPreferences } = useUserPreferences();
  const updatePreferencesMutation = useUpdatePreferences();

  // Exchange rates state
  const [rates, setRates] = useState<ExchangeRates | null>(null);
  const [isLoadingRates, setIsLoadingRates] = useState(true);

  // Current currency (from user preferences or default)
  const currency: SupportedCurrency = (preferences?.currency as SupportedCurrency) ?? "TRY";

  // Fetch exchange rates on mount
  useEffect(() => {
    async function fetchRates() {
      try {
        const response = await fetch("/api/currency/rates");
        if (response.ok) {
          const data = await response.json();
          setRates(data);
        } else {
          console.warn("Failed to fetch exchange rates, using fallback");
          setRates(FALLBACK_RATES);
        }
      } catch (error) {
        console.error("Failed to fetch exchange rates:", error);
        setRates(FALLBACK_RATES);
      } finally {
        setIsLoadingRates(false);
      }
    }
    fetchRates();
  }, []);

  // Set currency (updates user preferences)
  const setCurrency = useCallback(
    async (newCurrency: SupportedCurrency) => {
      await updatePreferencesMutation.mutateAsync({ currency: newCurrency });
    },
    [updatePreferencesMutation]
  );

  // Get currency symbol
  const getCurrencySymbol = useCallback(
    (curr: SupportedCurrency = currency): string => {
      return CURRENCY_CONFIG[curr].symbol;
    },
    [currency]
  );

  // Get locale for formatting
  const getCurrencyLocale = useCallback(
    (curr: SupportedCurrency = currency): string => {
      return CURRENCY_CONFIG[curr].locale;
    },
    [currency]
  );

  // Convert amount from source currency (default TRY) to display currency
  const convert = useCallback(
    (amount: number, fromCurrency: SupportedCurrency = "TRY"): number => {
      if (fromCurrency === currency || !rates) {
        return amount;
      }

      // All data from Trendyol is in TRY, convert to target currency
      if (fromCurrency === "TRY") {
        if (currency === "USD") return amount * rates.TRY_USD;
        if (currency === "EUR") return amount * rates.TRY_EUR;
      }

      // For other conversions, go through TRY first
      let inTry = amount;
      if (fromCurrency === "USD") inTry = amount * rates.USD_TRY;
      if (fromCurrency === "EUR") inTry = amount * rates.EUR_TRY;

      if (currency === "TRY") return inTry;
      if (currency === "USD") return inTry * rates.TRY_USD;
      if (currency === "EUR") return inTry * rates.TRY_EUR;

      return amount;
    },
    [currency, rates]
  );

  // Format currency with conversion
  const formatCurrency = useCallback(
    (
      amount: number,
      fromCurrency: SupportedCurrency = "TRY",
      options?: FormatOptions
    ): string => {
      const {
        showSymbol = true,
        minimumFractionDigits = 2,
        maximumFractionDigits = 2,
        compact = false,
      } = options ?? {};

      const convertedAmount = convert(amount, fromCurrency);
      const locale = getCurrencyLocale();
      const symbol = showSymbol ? getCurrencySymbol() : "";

      const absValue = Math.abs(convertedAmount);

      // Compact notation for large numbers
      if (compact) {
        if (absValue >= 1_000_000) {
          const value = (convertedAmount / 1_000_000).toFixed(1);
          return `${symbol}${value}M`;
        }
        if (absValue >= 1_000) {
          const value = (convertedAmount / 1_000).toFixed(0);
          return `${symbol}${value}K`;
        }
      }

      const formatted = new Intl.NumberFormat(locale, {
        minimumFractionDigits,
        maximumFractionDigits,
      }).format(absValue);

      const prefix = convertedAmount < 0 ? "-" : "";
      return `${prefix}${symbol}${formatted}`;
    },
    [convert, getCurrencyLocale, getCurrencySymbol]
  );

  const value = useMemo<CurrencyContextValue>(
    () => ({
      currency,
      setCurrency,
      rates,
      isLoadingRates,
      convert,
      formatCurrency,
      getCurrencySymbol,
      getCurrencyLocale,
    }),
    [
      currency,
      setCurrency,
      rates,
      isLoadingRates,
      convert,
      formatCurrency,
      getCurrencySymbol,
      getCurrencyLocale,
    ]
  );

  return (
    <CurrencyContext.Provider value={value}>{children}</CurrencyContext.Provider>
  );
}

// =============================================================================
// Hook
// =============================================================================

/**
 * Hook to access currency context.
 * Must be used within CurrencyProvider.
 */
export function useCurrency(): CurrencyContextValue {
  const context = useContext(CurrencyContext);
  if (!context) {
    throw new Error("useCurrency must be used within a CurrencyProvider");
  }
  return context;
}
