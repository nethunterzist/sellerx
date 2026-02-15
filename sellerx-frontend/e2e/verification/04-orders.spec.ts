/**
 * 04 - Orders Page Triple Verification
 *
 * Verifies the last 5 orders from the orders page across:
 * - PostgreSQL database
 * - SellerX frontend (/orders)
 * - Trendyol seller panel (order search)
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
  getOrders,
  formatLocalDate,
  closePool,
} from "./helpers/db-client";
import {
  extractTrendyolOrders,
  parseTurkishNumber,
  takeScreenshot,
} from "./helpers/data-extractor";
import { compareField, compareSets, type ComparisonResult } from "./helpers/comparison";
import { appendSection, getOrInitReportDir } from "./helpers/report-generator";

test.describe("04 - Orders Page Verification", () => {
  test.setTimeout(180000);

  test.afterAll(async () => {
    await closePool();
  });

  test("should verify orders page data across sources", async ({ page }) => {
    const reportDir = getOrInitReportDir();
    const email = getTestUserEmail();
    const storeId = await getStoreIdForUser(email);
    expect(storeId).toBeTruthy();

    // ─── DB: Get last 7 days orders ────────────────────────────
    const today = new Date();
    const todayStr = formatLocalDate(today);
    const weekAgo = new Date(today);
    weekAgo.setDate(weekAgo.getDate() - 7);
    const weekAgoStr = formatLocalDate(weekAgo);

    // Fetch more from DB to ensure overlap with FE rows
    const dbOrders = await getOrders(storeId!, weekAgoStr, todayStr, 50);

    // ─── SellerX: Read orders page ─────────────────────────────
    await loginToSellerX(page);
    await page.goto("/tr/orders");
    await page.waitForLoadState("networkidle");
    await page.waitForTimeout(5000);

    await takeScreenshot(page, "04-orders-sellerx", reportDir);

    // Extract order rows from the orders table
    // Columns: 0=expand, 1=order no, 2=date, 3=total, 4=cost,
    // 5=commission, 6=stoppage, 7=shipping, 8=platform, 9=margin, 10=ROI, 11=status, 12=items
    const feOrderRows = page.locator("table tbody tr");

    interface FeOrder {
      orderNumber: string;
      totalPrice: string;
      status: string;
    }

    const feOrders: FeOrder[] = [];
    const rowCount = await feOrderRows.count();
    for (let i = 0; i < rowCount && feOrders.length < 5; i++) {
      const row = feOrderRows.nth(i);
      const cells = row.locator("td");
      const cellCount = await cells.count();
      if (cellCount < 5) continue; // Skip expandable detail rows

      // Order number: 2nd cell (index 1), p.font-medium
      const orderNumEl = cells.nth(1).locator("p.font-medium").first();
      const orderNumber = (await orderNumEl.textContent())?.trim() || "";

      // Total price: 4th cell (index 3), span.font-medium
      const priceEl = cells.nth(3).locator("span.font-medium").first();
      const totalPrice = (await priceEl.textContent())?.trim() || "";

      // Status: 12th cell (index 11), span badge
      let status = "";
      try {
        const statusEl = cells.nth(11).locator("span.inline-flex").first();
        status = (await statusEl.textContent())?.trim() || "";
      } catch { /* optional */ }

      if (orderNumber) {
        feOrders.push({ orderNumber, totalPrice, status });
      }
    }
    const feOrderCount = feOrders.length;

    // ─── Trendyol: Read orders ─────────────────────────────────
    let tyOrders: Awaited<ReturnType<typeof extractTrendyolOrders>> = [];
    if (hasValidAuthState()) {
      try {
        const ty = await createTrendyolContext();
        try {
          await ty.page.goto(TrendyolPages.orders, {
            waitUntil: "domcontentloaded",
            timeout: 30000,
          });
          await ty.page.waitForTimeout(5000);
          await takeScreenshot(ty.page, "04-orders-trendyol", reportDir);
          tyOrders = await extractTrendyolOrders(ty.page, 10);
        } finally {
          await ty.close();
        }
      } catch (e) {
        console.log("Trendyol kullanılamadı:", (e as Error).message);
      }
    }

    // ─── Compare ───────────────────────────────────────────────
    const results: ComparisonResult[] = [];

    // FE extracted order count (informational)
    results.push(
      compareField(
        "FE Tabloda Sipariş Sayısı",
        feOrderCount,
        feOrderCount,
        tyOrders.length || null,
        "count"
      )
    );

    // Compare first 5 orders by matching order numbers
    for (let i = 0; i < Math.min(feOrders.length, 5); i++) {
      const feOrder = feOrders[i];
      if (!feOrder.orderNumber) continue;

      // Find in DB
      const dbOrder = dbOrders.find(
        (o) => o.orderNumber === feOrder.orderNumber
      );

      // Find in Trendyol
      const tyOrder = tyOrders.find(
        (o) => o.orderNumber === feOrder.orderNumber
      );

      if (dbOrder) {
        results.push(
          compareField(
            `Siparis #${feOrder.orderNumber} - Tutar`,
            dbOrder.totalPrice,
            feOrder.totalPrice
              ? parseTurkishNumber(feOrder.totalPrice)
              : null,
            tyOrder?.totalPrice
              ? parseTurkishNumber(tyOrder.totalPrice)
              : null,
            "money"
          )
        );
      }
    }

    // ─── Report ────────────────────────────────────────────────
    const summary = compareSets(results);
    appendSection({
      title: "Siparisler Sayfasi",
      summary,
      screenshots: ["04-orders-sellerx.png", "04-orders-trendyol.png"],
    });

    console.log(
      `\nSiparisler: ${summary.matching}/${summary.total} eslesen, ${summary.warnings} uyari, ${summary.errors} hata`
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
