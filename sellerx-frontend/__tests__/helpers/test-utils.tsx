import React, { ReactElement } from "react";
import { render, RenderOptions } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { vi } from "vitest";

// Mock next/navigation
vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    back: vi.fn(),
    forward: vi.fn(),
    refresh: vi.fn(),
    prefetch: vi.fn(),
  }),
  usePathname: () => "/dashboard",
  useParams: () => ({}),
  useSearchParams: () => ({
    get: vi.fn().mockReturnValue(null),
    getAll: vi.fn().mockReturnValue([]),
    has: vi.fn().mockReturnValue(false),
    toString: vi.fn().mockReturnValue(""),
  }),
}));

// Mock next/link
vi.mock("next/link", () => ({
  default: ({ children, href, ...props }: any) =>
    React.createElement("a", { href, ...props }, children),
}));

// Mock next-intl
vi.mock("next-intl", () => ({
  useTranslations: () => (key: string) => key,
  useLocale: () => "tr",
}));

// Mock @/i18n/navigation
vi.mock("@/i18n/navigation", () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    back: vi.fn(),
  }),
  usePathname: () => "/dashboard",
  Link: ({ children, ...props }: any) =>
    React.createElement("a", props, children),
}));

// Mock @/lib/contexts/currency-context
vi.mock("@/lib/contexts/currency-context", () => ({
  useCurrency: () => ({
    formatCurrency: (amount: number) =>
      new Intl.NumberFormat("tr-TR", {
        style: "currency",
        currency: "TRY",
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
      }).format(amount),
    currency: "TRY",
    exchangeRate: 1,
  }),
  CurrencyProvider: ({ children }: { children: React.ReactNode }) =>
    React.createElement(React.Fragment, null, children),
}));

/**
 * Creates a fresh QueryClient for testing with default settings
 * that disable retries and garbage collection timers.
 */
function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: Infinity,
      },
      mutations: {
        retry: false,
      },
    },
  });
}

/**
 * Wrapper component that provides all necessary providers for testing.
 */
function AllProviders({ children }: { children: React.ReactNode }) {
  const queryClient = createTestQueryClient();
  return React.createElement(
    QueryClientProvider,
    { client: queryClient },
    children
  );
}

/**
 * Custom render function that wraps the component with all providers.
 * Use this instead of @testing-library/react's render in component tests.
 */
function customRender(
  ui: ReactElement,
  options?: Omit<RenderOptions, "wrapper">
) {
  return render(ui, { wrapper: AllProviders, ...options });
}

export { customRender as render, createTestQueryClient, AllProviders };
export { default as userEvent } from "@testing-library/user-event";
export { screen, waitFor, within, act } from "@testing-library/react";
