/**
 * 01 - Dashboard Cards Triple Verification
 *
 * Verifies period card metrics (Bugun, Dun, Bu Ay, Gecen Ay) across:
 * - PostgreSQL database (direct query)
 * - SellerX frontend (browser read)
 * - Trendyol seller panel (browser read)
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
  formatLocalDate,
  closePool,
} from "./helpers/db-client";
import {
  extractDashboardCards,
  extractTrendyolFinance,
  parseTurkishNumber,
  takeScreenshot,
} from "./helpers/data-extractor";
import { compareField, compareSets, type ComparisonResult } from "./helpers/comparison";
import { appendSection, getOrInitReportDir } from "./helpers/report-generator";

test.describe("01 - Dashboard Cards Verification", () => {
  test.setTimeout(120000);

  test.afterAll(async () => {
    await closePool();
  });

  test("should verify dashboard card metrics across all three sources", async ({
    page,
  }) => {
    const reportDir = getOrInitReportDir();

    // ─── Step 1: Get store ID from DB ──────────────────────────
    const email = getTestUserEmail();
    const storeId = await getStoreIdForUser(email);
    expect(storeId).toBeTruthy();

    // ─── Step 2: Get DB stats for current date ranges ──────────
    const today = new Date();
    const todayStr = formatLocalDate(today);

    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);
    const yesterdayStr = formatLocalDate(yesterday);

    const firstOfMonth = new Date(today.getFullYear(), today.getMonth(), 1);
    const firstOfMonthStr = formatLocalDate(firstOfMonth);

    const firstOfLastMonth = new Date(
      today.getFullYear(),
      today.getMonth() - 1,
      1
    );
    const lastOfLastMonth = new Date(today.getFullYear(), today.getMonth(), 0);
    const firstOfLastMonthStr = formatLocalDate(firstOfLastMonth);
    const lastOfLastMonthStr = formatLocalDate(lastOfLastMonth);

    console.log(`\nDate ranges: today=${todayStr}, firstOfMonth=${firstOfMonthStr}, lastMonth=${firstOfLastMonthStr}..${lastOfLastMonthStr}`);

    const dbToday = await getDashboardStats(storeId!, todayStr, todayStr);
    const dbYesterday = await getDashboardStats(storeId!, yesterdayStr, yesterdayStr);
    const dbThisMonth = await getDashboardStats(storeId!, firstOfMonthStr, todayStr);
    const dbLastMonth = await getDashboardStats(storeId!, firstOfLastMonthStr, lastOfLastMonthStr);

    // ─── Step 3: Read SellerX frontend ─────────────────────────
    await loginToSellerX(page);
    await page.goto("/tr/dashboard");
    await page.waitForLoadState("networkidle");
    await page.waitForTimeout(5000); // Wait for stats to load

    await takeScreenshot(page, "01-dashboard-cards-sellerx", reportDir);

    const feCards = await extractDashboardCards(page);

    // ─── Step 4: Read Trendyol panel (if session available) ────
    let trendyolFinance: Record<string, string> = {};
    let trendyolAvailable = false;

    if (hasValidAuthState()) {
      try {
        const ty = await createTrendyolContext();
        try {
          await ty.page.goto(TrendyolPages.finance, {
            waitUntil: "domcontentloaded",
            timeout: 30000,
          });
          await ty.page.waitForTimeout(5000);
          await takeScreenshot(ty.page, "01-dashboard-cards-trendyol", reportDir);
          trendyolFinance = await extractTrendyolFinance(ty.page);
          trendyolAvailable = true;
        } finally {
          await ty.close();
        }
      } catch (e) {
        console.log("Trendyol oturumu kullanılamadı:", (e as Error).message);
      }
    }

    // ─── Step 5: Compare ───────────────────────────────────────
    const results: ComparisonResult[] = [];

    // Helper to find a card by title keyword
    const findCard = (keyword: string) =>
      feCards.find((c) =>
        c.title.toLowerCase().includes(keyword.toLowerCase())
      );

    // Today comparisons
    const todayCard = findCard("Bugun") || findCard("Bugün");
    if (todayCard) {
      results.push(
        compareField(
          "Bugun - Ciro",
          dbToday.totalRevenue,
          todayCard.sales ? parseTurkishNumber(todayCard.sales) : null,
          trendyolAvailable ? parseTurkishNumber(trendyolFinance["Toplam Satış"] || "") : null,
          "money"
        )
      );
      results.push(
        compareField(
          "Bugun - Siparis Sayisi",
          dbToday.totalOrders,
          todayCard.ordersUnits
            ? parseInt(todayCard.ordersUnits.split("/")[0]?.trim() || "0", 10)
            : null,
          null,
          "count"
        )
      );
      results.push(
        compareField(
          "Bugun - Iade",
          dbToday.returnCount,
          todayCard.refunds ? parseTurkishNumber(todayCard.refunds) : null,
          null,
          "count"
        )
      );
    }

    // Yesterday comparisons
    const yesterdayCard = findCard("Dun") || findCard("Dün");
    if (yesterdayCard) {
      results.push(
        compareField(
          "Dun - Ciro",
          dbYesterday.totalRevenue,
          yesterdayCard.sales
            ? parseTurkishNumber(yesterdayCard.sales)
            : null,
          null,
          "money"
        )
      );
      results.push(
        compareField(
          "Dun - Siparis Sayisi",
          dbYesterday.totalOrders,
          yesterdayCard.ordersUnits
            ? parseInt(yesterdayCard.ordersUnits.split("/")[0]?.trim() || "0", 10)
            : null,
          null,
          "count"
        )
      );
    }

    // This Month comparisons
    const thisMonthCard = findCard("Bu Ay");
    if (thisMonthCard) {
      results.push(
        compareField(
          "Bu Ay - Ciro",
          dbThisMonth.totalRevenue,
          thisMonthCard.sales
            ? parseTurkishNumber(thisMonthCard.sales)
            : null,
          null,
          "money"
        )
      );
      results.push(
        compareField(
          "Bu Ay - Siparis Sayisi",
          dbThisMonth.totalOrders,
          thisMonthCard.ordersUnits
            ? parseInt(
                thisMonthCard.ordersUnits.split("/")[0]?.trim() || "0",
                10
              )
            : null,
          null,
          "count"
        )
      );
      results.push(
        compareField(
          "Bu Ay - Iade",
          dbThisMonth.returnCount,
          thisMonthCard.refunds
            ? parseTurkishNumber(thisMonthCard.refunds)
            : null,
          null,
          "count"
        )
      );
      results.push(
        compareField(
          "Bu Ay - Brut Kar",
          dbThisMonth.grossProfit,
          thisMonthCard.grossProfit
            ? parseTurkishNumber(thisMonthCard.grossProfit)
            : null,
          null,
          "money"
        )
      );
    }

    // Last Month comparisons
    const lastMonthCard = findCard("Gecen") || findCard("Geçen");
    if (lastMonthCard) {
      results.push(
        compareField(
          "Gecen Ay - Ciro",
          dbLastMonth.totalRevenue,
          lastMonthCard.sales
            ? parseTurkishNumber(lastMonthCard.sales)
            : null,
          null,
          "money"
        )
      );
      results.push(
        compareField(
          "Gecen Ay - Siparis Sayisi",
          dbLastMonth.totalOrders,
          lastMonthCard.ordersUnits
            ? parseInt(
                lastMonthCard.ordersUnits.split("/")[0]?.trim() || "0",
                10
              )
            : null,
          null,
          "count"
        )
      );
    }

    // ─── Step 6: Generate report section ───────────────────────
    const summary = compareSets(results);
    appendSection({
      title: "Dashboard Kartları",
      summary,
      screenshots: ["01-dashboard-cards-sellerx.png", "01-dashboard-cards-trendyol.png"],
    });

    // Log summary
    console.log(
      `\nDashboard Kartları: ${summary.matching}/${summary.total} eslesen, ${summary.warnings} uyari, ${summary.errors} hata`
    );

    // Soft assertion: log mismatches but don't fail test
    for (const r of results) {
      if (!r.match) {
        console.log(
          `  ${r.severity.toUpperCase()}: ${r.field} - DB:${r.dbValue} FE:${r.frontendValue} TY:${r.trendyolValue} (sapma: %${r.deviation})`
        );
      }
    }
  });
});
