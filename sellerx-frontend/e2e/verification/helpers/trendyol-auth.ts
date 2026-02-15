/**
 * Trendyol session management for verification tests.
 *
 * Flow:
 * 1. Open headed Chromium browser
 * 2. Navigate to partner.trendyol.com
 * 3. Print "Trendyol'a giris yapin, sonra ENTER'a basin" to console
 * 4. Wait for ENTER key press from stdin
 * 5. Validate session (check dashboard loaded)
 * 6. Save storageState to trendyol-auth.json
 * 7. Subsequent tests reuse saved session
 */
import { chromium, type BrowserContext, type Page } from "@playwright/test";
import * as fs from "fs";
import * as path from "path";

const AUTH_FILE = path.join(__dirname, "..", "trendyol-auth.json");
const TRENDYOL_BASE = "https://partner.trendyol.com";

/**
 * Check if a valid Trendyol auth state file exists.
 */
export function hasValidAuthState(): boolean {
  if (!fs.existsSync(AUTH_FILE)) return false;
  // Check file age (cookie validity ~24h)
  const stats = fs.statSync(AUTH_FILE);
  const ageMs = Date.now() - stats.mtimeMs;
  const maxAgeMs = 20 * 60 * 60 * 1000; // 20 hours
  return ageMs < maxAgeMs;
}

/**
 * Get the path to the auth state file.
 */
export function getAuthFilePath(): string {
  return AUTH_FILE;
}

/**
 * Perform manual Trendyol login and save session.
 * Must be run with --headed flag.
 * Auto-detects login by polling the URL - no ENTER needed.
 */
export async function setupTrendyolSession(): Promise<void> {
  const browser = await chromium.launch({ headless: false, slowMo: 100 });
  const context = await browser.newContext({
    viewport: { width: 1280, height: 800 },
  });
  const page = await context.newPage();

  try {
    await page.goto(TRENDYOL_BASE, { waitUntil: "domcontentloaded" });

    console.log("\n" + "=".repeat(60));
    console.log("  TRENDYOL MANUAL LOGIN");
    console.log("=".repeat(60));
    console.log("\n  1. Trendyol Satici Paneli acildi.");
    console.log("  2. Giris bilgilerinizi girin.");
    console.log("  3. Giris algilananinca otomatik kaydedilecek.\n");
    console.log("  Bekleniyor...\n");

    // Poll URL until we detect a logged-in state (max 4 minutes)
    const maxWait = 4 * 60 * 1000;
    const pollInterval = 2000;
    const start = Date.now();
    let loggedIn = false;

    while (Date.now() - start < maxWait) {
      await page.waitForTimeout(pollInterval);
      const url = page.url();
      // Logged in = on partner.trendyol.com but NOT on login/auth pages
      if (
        url.includes("partner.trendyol.com") &&
        !url.includes("login") &&
        !url.includes("auth") &&
        !url.includes("giris") &&
        (url.includes("dashboard") ||
          url.includes("orders") ||
          url.includes("products") ||
          url.includes("product-listing") ||
          url.includes("finance") ||
          url.includes("payments") ||
          url.includes("supplier") ||
          url.includes("account"))
      ) {
        loggedIn = true;
        break;
      }
    }

    if (loggedIn) {
      // Wait a bit more for cookies to settle
      await page.waitForTimeout(3000);
      console.log("  Giris algilandi! Oturum kaydediliyor...");
    } else {
      console.log(
        "  UYARI: Giris algılanamadı, mevcut durum kaydedilecek."
      );
    }

    // Save storage state
    await context.storageState({ path: AUTH_FILE });
    console.log(`  Oturum kaydedildi: ${AUTH_FILE}\n`);
  } finally {
    await browser.close();
  }
}

/**
 * Create a Trendyol browser context using the saved session.
 */
export async function createTrendyolContext(): Promise<{
  context: BrowserContext;
  page: Page;
  close: () => Promise<void>;
}> {
  if (!fs.existsSync(AUTH_FILE)) {
    throw new Error(
      `Trendyol oturum dosyasi bulunamadi: ${AUTH_FILE}\n` +
        "Once 'npm run verify:setup' calistirin."
    );
  }

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    storageState: AUTH_FILE,
    viewport: { width: 1280, height: 800 },
  });
  const page = await context.newPage();

  return {
    context,
    page,
    close: async () => {
      await context.close();
      await browser.close();
    },
  };
}

/**
 * Navigate to Trendyol seller panel pages.
 */
export const TrendyolPages = {
  dashboard: `${TRENDYOL_BASE}/dashboard`,
  orders: `${TRENDYOL_BASE}/orders/shipment-packages`,
  products: `${TRENDYOL_BASE}/product-listing/all-products`,
  finance: `${TRENDYOL_BASE}/payments/my-payments`,
  returns: `${TRENDYOL_BASE}/orders/claims`,
  invoices: `${TRENDYOL_BASE}/payments/invoice`,
} as const;
