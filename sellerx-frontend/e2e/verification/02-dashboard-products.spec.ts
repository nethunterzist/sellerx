/**
 * 02 - Dashboard Products Table Triple Verification
 *
 * Verifies the top 10 products in the dashboard table across:
 * - PostgreSQL database
 * - SellerX frontend
 * - Trendyol seller panel (price, stock)
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
  getProducts,
  getProductSalesStats,
  formatLocalDate,
  closePool,
} from "./helpers/db-client";
import {
  extractProductsTable,
  extractTrendyolProducts,
  parseTurkishNumber,
  takeScreenshot,
} from "./helpers/data-extractor";
import { compareField, compareSets, type ComparisonResult } from "./helpers/comparison";
import { appendSection, getOrInitReportDir } from "./helpers/report-generator";

test.describe("02 - Dashboard Products Table Verification", () => {
  test.setTimeout(180000);

  test.afterAll(async () => {
    await closePool();
  });

  test("should verify product table data across sources", async ({ page }) => {
    const reportDir = getOrInitReportDir();
    const email = getTestUserEmail();
    const storeId = await getStoreIdForUser(email);
    expect(storeId).toBeTruthy();

    // ─── DB: Get products and sales stats ──────────────────────
    const today = new Date();
    const todayStr = formatLocalDate(today);
    const firstOfMonth = new Date(today.getFullYear(), today.getMonth(), 1);
    const firstOfMonthStr = formatLocalDate(firstOfMonth);

    // Fetch more products to ensure barcode overlap with FE (FE sorts by sales DESC)
    const dbProducts = await getProducts(storeId!, 200);
    const dbSalesStats = await getProductSalesStats(
      storeId!,
      firstOfMonthStr,
      todayStr,
      200 // Fetch many to ensure overlap with FE top products
    );

    // ─── SellerX: Read dashboard products table ────────────────
    await loginToSellerX(page);
    await page.goto("/tr/dashboard");
    await page.waitForLoadState("networkidle");
    await page.waitForTimeout(5000);

    // Products table shows data for the selected period card.
    // Default is "Bugün" which may be empty early in the day.
    // Click "Bu Ay" card to ensure product data is available.
    const buAyCard = page.locator('h3:has-text("Bu Ay")').first();
    if (await buAyCard.isVisible({ timeout: 5000 }).catch(() => false)) {
      await buAyCard.click();
      await page.waitForTimeout(3000);
      console.log(`\nDebug: Clicked "Bu Ay" period card`);
    } else {
      console.log(`\nDebug: "Bu Ay" card not found, using default period`);
    }

    await takeScreenshot(page, "02-products-table-sellerx", reportDir);

    const feProducts = await extractProductsTable(page, 10);
    console.log(`Debug: extractProductsTable returned ${feProducts.length} products`);
    if (feProducts.length > 0) {
      console.log(`Debug: First product: name="${feProducts[0].name.slice(0, 40)}" sku="${feProducts[0].sku}" sales="${feProducts[0].sales}"`);
    } else {
      // Debug: Check table state
      const tableRows = page.locator("table tbody tr");
      const rowCount = await tableRows.count();
      console.log(`Debug: Table tbody tr count: ${rowCount}`);
      if (rowCount > 0) {
        const firstRowText = (await tableRows.first().textContent())?.trim().slice(0, 200) || "";
        console.log(`Debug: First row text: "${firstRowText}"`);
      }
    }

    // ─── Trendyol: Read product list ───────────────────────────
    let tyProducts: Awaited<ReturnType<typeof extractTrendyolProducts>> = [];
    if (hasValidAuthState()) {
      try {
        const ty = await createTrendyolContext();
        try {
          await ty.page.goto(TrendyolPages.products, {
            waitUntil: "domcontentloaded",
            timeout: 30000,
          });
          await ty.page.waitForTimeout(5000);
          await takeScreenshot(ty.page, "02-products-table-trendyol", reportDir);
          tyProducts = await extractTrendyolProducts(ty.page, 20);
        } finally {
          await ty.close();
        }
      } catch (e) {
        console.log("Trendyol kullanılamadı:", (e as Error).message);
      }
    }

    // ─── Compare ───────────────────────────────────────────────
    const results: ComparisonResult[] = [];

    // Compare first 10 products from frontend against DB
    for (let i = 0; i < Math.min(feProducts.length, 10); i++) {
      const feRow = feProducts[i];
      const feBarcode = feRow.sku;

      // Find matching DB product by barcode
      const dbProduct = dbProducts.find(
        (p) => p.barcode === feBarcode
      );
      const dbSales = dbSalesStats.find(
        (s) => s.barcode === feBarcode
      );

      // Find matching Trendyol product
      const tyProduct = tyProducts.find(
        (p) => p.barcode === feBarcode || p.name.includes(feRow.name.slice(0, 20))
      );

      const prefix = `Urun ${i + 1} (${feBarcode || feRow.name.slice(0, 20)})`;

      // Product name
      if (dbProduct) {
        results.push(
          compareField(
            `${prefix} - Ad`,
            dbProduct.name ? 1 : 0, // just check existence
            feRow.name ? 1 : 0,
            tyProduct?.name ? 1 : 0,
            "count"
          )
        );
      }

      // Units sold (DB sales stats vs frontend)
      if (dbSales) {
        results.push(
          compareField(
            `${prefix} - Satilan Adet`,
            dbSales.unitsSold,
            feRow.unitsSold ? parseTurkishNumber(feRow.unitsSold) : null,
            null,
            "count"
          )
        );
      }

      // Sales amount
      if (dbSales) {
        results.push(
          compareField(
            `${prefix} - Satis Tutari`,
            dbSales.totalSales,
            feRow.sales ? parseTurkishNumber(feRow.sales) : null,
            null,
            "money"
          )
        );
      }

      // Stock (DB vs FE vs Trendyol)
      if (dbProduct) {
        results.push(
          compareField(
            `${prefix} - Stok`,
            dbProduct.trendyolQuantity,
            null, // Stock is shown in the product cell subtitle
            tyProduct?.stock ? parseTurkishNumber(tyProduct.stock) : null,
            "count"
          )
        );
      }

      // Sale price (DB vs Trendyol)
      if (dbProduct) {
        results.push(
          compareField(
            `${prefix} - Satis Fiyati`,
            dbProduct.salePrice,
            null,
            tyProduct?.price ? parseTurkishNumber(tyProduct.price) : null,
            "money"
          )
        );
      }

      // Commission rate
      if (dbProduct?.lastCommissionRate !== null && dbProduct?.lastCommissionRate !== undefined) {
        results.push(
          compareField(
            `${prefix} - Komisyon Orani`,
            dbProduct.lastCommissionRate,
            null,
            null,
            "percent"
          )
        );
      }
    }

    // ─── Report ────────────────────────────────────────────────
    const summary = compareSets(results);
    appendSection({
      title: "Dashboard Urun Tablosu (Ilk 10)",
      summary,
      screenshots: [
        "02-products-table-sellerx.png",
        "02-products-table-trendyol.png",
      ],
    });

    console.log(
      `\nDashboard Urunleri: ${summary.matching}/${summary.total} eslesen, ${summary.warnings} uyari, ${summary.errors} hata`
    );

    for (const r of results) {
      if (!r.match) {
        console.log(
          `  ${r.severity.toUpperCase()}: ${r.field} - DB:${r.dbValue} FE:${r.frontendValue} TY:${r.trendyolValue}`
        );
      }
    }
  });
});
