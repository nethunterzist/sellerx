/**
 * 03 - Dashboard Detail Panels Triple Verification
 *
 * Verifies:
 * 3a - Product Detail Panel (first 3 products)
 * 3b - Order Detail Panel (first 3 orders)
 * 3c - Period Detail Modal (This Month)
 */
import { test, expect } from "@playwright/test";
import { loginToSellerX, getTestUserEmail } from "./helpers/sellerx-auth";
import {
  createTrendyolContext,
  hasValidAuthState,
  TrendyolPages,
} from "./helpers/trendyol-auth";
import {
  getStoreIdForUser,
  getDashboardStats,
  getOrderByNumber,
  getProductByBarcode,
  formatLocalDate,
  closePool,
} from "./helpers/db-client";
import {
  extractProductDetail,
  extractOrderDetail,
  extractPeriodDetail,
  parseTurkishNumber,
  takeScreenshot,
} from "./helpers/data-extractor";
import { compareField, compareSets, type ComparisonResult } from "./helpers/comparison";
import { appendSection, getOrInitReportDir } from "./helpers/report-generator";

test.describe("03 - Dashboard Detail Panels Verification", () => {
  test.setTimeout(240000);

  test.afterAll(async () => {
    await closePool();
  });

  test("3a - should verify product detail panel data", async ({ page }) => {
    const reportDir = getOrInitReportDir();
    const email = getTestUserEmail();
    const storeId = await getStoreIdForUser(email);
    expect(storeId).toBeTruthy();

    await loginToSellerX(page);
    await page.goto("/tr/dashboard");
    await page.waitForLoadState("networkidle");
    await page.waitForTimeout(5000);

    const results: ComparisonResult[] = [];

    // Click "Urunler" tab
    const productsTab = page.locator('button:has-text("Ürünler")').first();
    if (await productsTab.isVisible()) {
      await productsTab.click();
      await page.waitForTimeout(1000);
    }

    // Click "Detay" on first 3 products
    const detayButtons = page.locator('button:has-text("Detay")');
    const detayCount = Math.min(await detayButtons.count(), 3);

    for (let i = 0; i < detayCount; i++) {
      // Click Detay button
      await detayButtons.nth(i).click();
      await page.waitForTimeout(2000);

      const screenshotName = `03a-product-detail-${i + 1}`;
      await takeScreenshot(page, screenshotName, reportDir);

      // Extract panel data
      const feDetail = await extractProductDetail(page);

      // Try to get DB data for this product
      if (feDetail.sku) {
        const dbProduct = await getProductByBarcode(storeId!, feDetail.sku);
        if (dbProduct) {
          results.push(
            compareField(
              `Urun ${i + 1} Detay - Fiyat`,
              dbProduct.salePrice,
              null,
              null,
              "money"
            )
          );
          results.push(
            compareField(
              `Urun ${i + 1} Detay - Stok`,
              dbProduct.trendyolQuantity,
              null,
              null,
              "count"
            )
          );
        }
      }

      // Close the panel (press Escape or click X)
      await page.keyboard.press("Escape");
      await page.waitForTimeout(500);
    }

    if (results.length > 0) {
      const summary = compareSets(results);
      appendSection({
        title: "Urun Detay Panelleri (Ilk 3)",
        summary,
      });

      console.log(
        `\nUrun Detay Panelleri: ${summary.matching}/${summary.total} eslesen`
      );
    }
  });

  test("3b - should verify order detail panel data", async ({ page }) => {
    const reportDir = getOrInitReportDir();
    const email = getTestUserEmail();
    const storeId = await getStoreIdForUser(email);
    expect(storeId).toBeTruthy();

    await loginToSellerX(page);
    await page.goto("/tr/dashboard");
    await page.waitForLoadState("networkidle");
    await page.waitForTimeout(5000);

    const results: ComparisonResult[] = [];

    // Click "Siparis Kalemleri" tab
    const ordersTab = page.locator('button:has-text("Sipariş Kalemleri")').first();
    if (await ordersTab.isVisible()) {
      await ordersTab.click();
      await page.waitForTimeout(1000);
    }

    // Click "Detay" on first 3 orders
    const detayButtons = page.locator('button:has-text("Detay")');
    const detayCount = Math.min(await detayButtons.count(), 3);

    for (let i = 0; i < detayCount; i++) {
      await detayButtons.nth(i).click();
      await page.waitForTimeout(2000);

      const screenshotName = `03b-order-detail-${i + 1}`;
      await takeScreenshot(page, screenshotName, reportDir);

      const feDetail = await extractOrderDetail(page);

      // Try Trendyol comparison
      if (feDetail.orderNumber && hasValidAuthState()) {
        try {
          const ty = await createTrendyolContext();
          try {
            await ty.page.goto(
              `${TrendyolPages.orders}?search=${feDetail.orderNumber}`,
              { waitUntil: "domcontentloaded", timeout: 30000 }
            );
            await ty.page.waitForTimeout(3000);
            await takeScreenshot(
              ty.page,
              `03b-order-trendyol-${i + 1}`,
              reportDir
            );
          } finally {
            await ty.close();
          }
        } catch {
          // Trendyol unavailable, skip
        }
      }

      // DB comparison
      if (feDetail.orderNumber) {
        const dbOrder = await getOrderByNumber(feDetail.orderNumber);
        if (dbOrder) {
          results.push(
            compareField(
              `Siparis ${i + 1} - Toplam Tutar`,
              dbOrder.totalPrice,
              feDetail.totalPrice
                ? parseTurkishNumber(feDetail.totalPrice)
                : null,
              null,
              "money"
            )
          );
        }
      }

      await page.keyboard.press("Escape");
      await page.waitForTimeout(500);
    }

    if (results.length > 0) {
      const summary = compareSets(results);
      appendSection({
        title: "Siparis Detay Panelleri (Ilk 3)",
        summary,
      });

      console.log(
        `\nSiparis Detay Panelleri: ${summary.matching}/${summary.total} eslesen`
      );
    }
  });

  test("3c - should verify period detail modal data", async ({ page }) => {
    const reportDir = getOrInitReportDir();
    const email = getTestUserEmail();
    const storeId = await getStoreIdForUser(email);
    expect(storeId).toBeTruthy();

    // DB stats for this month
    const today = new Date();
    const todayStr = formatLocalDate(today);
    const firstOfMonth = new Date(today.getFullYear(), today.getMonth(), 1);
    const firstOfMonthStr = formatLocalDate(firstOfMonth);

    const dbStats = await getDashboardStats(storeId!, firstOfMonthStr, todayStr);

    // SellerX: Open period detail modal for "Bu Ay"
    await loginToSellerX(page);
    await page.goto("/tr/dashboard");
    await page.waitForLoadState("networkidle");
    await page.waitForTimeout(5000);

    // Find and click "Detay" button on "Bu Ay" card
    // Period cards have h3.text-sm.font-semibold.text-white for title, button for "Detay"
    // Use h3 containing "Bu Ay" → navigate up to card container → find Detay button
    const buAyH3 = page.locator('h3:has-text("Bu Ay")').first();
    // Navigate up from h3 to find the closest parent card that contains the Detay button
    const buAyCard = buAyH3.locator("../../..");
    const detayButton = buAyCard.locator('button:has-text("Detay")').first();

    const results: ComparisonResult[] = [];

    const detayVisible = await detayButton.isVisible({ timeout: 5000 }).catch(() => false);

    if (detayVisible) {
      await detayButton.click();
      await page.waitForTimeout(2000);

      await takeScreenshot(page, "03c-period-detail-modal", reportDir);

      const feDetail = await extractPeriodDetail(page);

      // Compare DB vs Frontend modal
      results.push(
        compareField(
          "Bu Ay Modal - Ciro",
          dbStats.totalRevenue,
          feDetail.sales ? parseTurkishNumber(feDetail.sales) : null,
          null,
          "money"
        )
      );
      results.push(
        compareField(
          "Bu Ay Modal - Siparis",
          dbStats.totalOrders,
          feDetail.orders ? parseTurkishNumber(feDetail.orders) : null,
          null,
          "count"
        )
      );
      results.push(
        compareField(
          "Bu Ay Modal - Komisyon",
          dbStats.totalEstimatedCommission,
          feDetail.commission
            ? Math.abs(parseTurkishNumber(feDetail.commission))
            : null,
          null,
          "money"
        )
      );
      results.push(
        compareField(
          "Bu Ay Modal - Kargo",
          dbStats.totalShippingCost,
          feDetail.shippingCost
            ? Math.abs(parseTurkishNumber(feDetail.shippingCost))
            : null,
          null,
          "money"
        )
      );

      await page.keyboard.press("Escape");
    } else {
      console.log("Bu Ay detay butonu bulunamadi, atlaniyor.");
    }

    if (results.length > 0) {
      const summary = compareSets(results);
      appendSection({
        title: "Donem Detay Modali (Bu Ay)",
        summary,
        screenshots: ["03c-period-detail-modal.png"],
      });

      console.log(
        `\nDonem Detay Modali: ${summary.matching}/${summary.total} eslesen`
      );
    }
  });
});
