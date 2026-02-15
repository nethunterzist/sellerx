/**
 * 07 - Products Page Triple Verification
 *
 * Verifies the first 10 products from the products page across:
 * - PostgreSQL database (trendyol_products)
 * - SellerX frontend (/products)
 * - Trendyol seller panel (products list)
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
  closePool,
} from "./helpers/db-client";
import {
  extractTrendyolProducts,
  parseTurkishNumber,
  takeScreenshot,
} from "./helpers/data-extractor";
import { compareField, compareSets, type ComparisonResult } from "./helpers/comparison";
import { appendSection, getOrInitReportDir } from "./helpers/report-generator";

test.describe("07 - Products Page Verification", () => {
  test.setTimeout(240000);

  test.afterAll(async () => {
    await closePool();
  });

  test("should verify products page data across sources", async ({ page }) => {
    const reportDir = getOrInitReportDir();
    const email = getTestUserEmail();
    const storeId = await getStoreIdForUser(email);
    expect(storeId).toBeTruthy();

    const PRODUCT_LIMIT = 10;

    // ─── DB: Get products (large batch to ensure overlap with FE) ──
    // FE sorts by onSale DESC; DB sorts by sale_price DESC
    // Fetch 200 to ensure we find all FE products by barcode
    const dbProducts = await getProducts(storeId!, 200);

    // ─── SellerX: Read products page ──────────────────────────
    await loginToSellerX(page);
    await page.goto("/tr/products");
    await page.waitForLoadState("networkidle");
    await page.waitForTimeout(5000);

    await takeScreenshot(page, "07-products-sellerx", reportDir);

    // Extract product data using page.evaluate for speed (avoid many locator calls)
    const feProducts = await page.evaluate((limit: number) => {
      const rows = document.querySelectorAll("table tbody tr");
      const results: Array<{
        name: string;
        barcode: string;
        price: string;
        stock: string;
        status: string;
      }> = [];

      for (let i = 0; i < Math.min(rows.length, limit); i++) {
        const cells = rows[i].querySelectorAll("td");
        if (cells.length < 6) continue;

        // Product name: 2nd cell (index 1)
        // Can be <p class="font-medium"> or <a class="font-medium"> (when product has URL)
        const nameEl =
          cells[1]?.querySelector("p.font-medium") ||
          cells[1]?.querySelector("a.font-medium");
        const name = nameEl?.textContent?.trim()?.slice(0, 100) || "";

        // Barcode: 2nd cell, <p> elements containing "Barkod:" text
        let barcode = "";
        const pEls = cells[1]?.querySelectorAll("p") || [];
        for (const el of pEls) {
          const text = el.textContent?.trim() || "";
          if (text.startsWith("Barkod:")) {
            barcode = text.replace("Barkod:", "").trim();
            break;
          }
        }

        // Price: 3rd cell (index 2), span.font-medium
        const priceEl = cells[2]?.querySelector("span.font-medium");
        const price = priceEl?.textContent?.trim() || "";

        // Stock: 5th cell (index 4), span.font-medium or colored span
        const stockEl = cells[4]?.querySelector("span.font-medium");
        const stock = stockEl?.textContent?.trim() || "";

        // Status: 7th cell (index 6), span.inline-flex badge
        const statusEl = cells[6]?.querySelector("span.inline-flex");
        const status = statusEl?.textContent?.trim() || "";

        results.push({ name, barcode, price, stock, status });
      }
      return results;
    }, PRODUCT_LIMIT);

    const feProductCount = feProducts.length;

    // Debug: Log extracted products
    console.log(`\nFE ürün sayısı: ${feProductCount}`);
    for (let i = 0; i < Math.min(feProducts.length, 3); i++) {
      const p = feProducts[i];
      console.log(`  FE[${i}]: name="${p.name.slice(0, 40)}" barcode="${p.barcode}" price="${p.price}" stock="${p.stock}"`);
    }
    console.log(`DB ürün sayısı: ${dbProducts.length}`);
    for (let i = 0; i < Math.min(dbProducts.length, 3); i++) {
      const p = dbProducts[i];
      console.log(`  DB[${i}]: name="${p.name.slice(0, 40)}" barcode="${p.barcode}" price=${p.salePrice} stock=${p.trendyolQuantity}`);
    }

    // ─── Trendyol: Read products ──────────────────────────────
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
          await takeScreenshot(ty.page, "07-products-trendyol", reportDir);
          tyProducts = await extractTrendyolProducts(ty.page, PRODUCT_LIMIT);
        } finally {
          await ty.close();
        }
      } catch (e) {
        console.log("Trendyol kullanılamadı:", (e as Error).message);
      }
    }

    // ─── Compare ───────────────────────────────────────────────
    const results: ComparisonResult[] = [];

    // FE products extracted (just informational, not a strict comparison)
    results.push(
      compareField(
        "FE Tabloda Ürün Sayısı",
        feProductCount,
        feProductCount,
        tyProducts.length || null,
        "count"
      )
    );

    // Compare individual products
    let matchedProducts = 0;
    for (let i = 0; i < feProducts.length; i++) {
      const feProduct = feProducts[i];
      if (!feProduct.barcode) continue;

      // Find in DB by barcode
      const dbProduct = dbProducts.find(
        (p) => p.barcode === feProduct.barcode
      );

      // Find in Trendyol by barcode
      const tyProduct = tyProducts.find(
        (p) =>
          p.barcode === feProduct.barcode ||
          p.name.includes(feProduct.name.slice(0, 20))
      );

      const prefix = `Ürün ${i + 1} (${feProduct.barcode})`;

      if (!dbProduct) {
        console.log(`  ⚠️ ${prefix}: DB'de bulunamadı`);
        continue;
      }

      matchedProducts++;

      // Sale price (DB vs FE vs Trendyol)
      results.push(
        compareField(
          `${prefix} - Satış Fiyatı`,
          dbProduct.salePrice,
          feProduct.price
            ? parseTurkishNumber(feProduct.price)
            : null,
          tyProduct?.price
            ? parseTurkishNumber(tyProduct.price)
            : null,
          "money"
        )
      );

      // Stock (DB vs FE vs Trendyol)
      results.push(
        compareField(
          `${prefix} - Stok`,
          dbProduct.trendyolQuantity,
          feProduct.stock !== ""
            ? parseInt(feProduct.stock, 10)
            : null,
          tyProduct?.stock
            ? parseTurkishNumber(tyProduct.stock)
            : null,
          "count"
        )
      );

      // Commission rate (DB only - not always visible on FE/TY)
      if (
        dbProduct.lastCommissionRate !== null &&
        dbProduct.lastCommissionRate !== undefined
      ) {
        results.push(
          compareField(
            `${prefix} - Komisyon Oranı`,
            dbProduct.lastCommissionRate,
            null,
            null,
            "percent"
          )
        );
      }
    }

    console.log(`  DB eşleşen ürün: ${matchedProducts}/${feProducts.filter(p => !!p.barcode).length}`);

    // ─── Report ────────────────────────────────────────────────
    const summary = compareSets(results);
    appendSection({
      title: "Ürünler Sayfası (İlk 10)",
      summary,
      screenshots: ["07-products-sellerx.png", "07-products-trendyol.png"],
    });

    console.log(
      `\nÜrünler: ${summary.matching}/${summary.total} eşleşen, ${summary.warnings} uyarı, ${summary.errors} hata`
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
