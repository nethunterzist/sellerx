/**
 * Trendyol manual login setup.
 * Run with: npm run verify:setup
 * Opens a headed browser for manual Trendyol login, then saves the session.
 */
import { test } from "@playwright/test";
import { setupTrendyolSession, hasValidAuthState } from "./helpers/trendyol-auth";

test.describe("Trendyol Session Setup", () => {
  test("should setup Trendyol session via manual login", async () => {
    test.setTimeout(300000); // 5 minutes for manual login

    if (hasValidAuthState()) {
      console.log("\nMevcut Trendyol oturumu gecerli. Atlanıyor...");
      console.log("Yeniden giris icin trendyol-auth.json dosyasini silin.\n");
      return;
    }

    await setupTrendyolSession();
  });
});
