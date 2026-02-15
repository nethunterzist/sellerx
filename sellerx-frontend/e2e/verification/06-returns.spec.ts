/**
 * 06 - Returns Page Triple Verification
 *
 * Verifies return analytics from the returns page across:
 * - PostgreSQL database (return_records + trendyol_claims)
 * - SellerX frontend (/returns)
 * - Trendyol seller panel (returns/claims)
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
  getReturnAnalytics,
  formatLocalDate,
  closePool,
} from "./helpers/db-client";
import {
  extractTrendyolReturns,
  parseTurkishNumber,
  takeScreenshot,
} from "./helpers/data-extractor";
import { compareField, compareSets, type ComparisonResult } from "./helpers/comparison";
import { appendSection, getOrInitReportDir } from "./helpers/report-generator";

test.describe("06 - Returns Page Verification", () => {
  test.setTimeout(180000);

  test.afterAll(async () => {
    await closePool();
  });

  test("should verify returns analytics data across sources", async ({
    page,
  }) => {
    const reportDir = getOrInitReportDir();
    const email = getTestUserEmail();
    const storeId = await getStoreIdForUser(email);
    expect(storeId).toBeTruthy();

    // ─── DB: Get return analytics (FE defaults to last 3 years) ─
    const today = new Date();
    const todayStr = formatLocalDate(today);
    const threeYearsAgo = new Date(today);
    threeYearsAgo.setFullYear(threeYearsAgo.getFullYear() - 3);
    const threeYearsAgoStr = formatLocalDate(threeYearsAgo);

    const dbReturns = await getReturnAnalytics(
      storeId!,
      threeYearsAgoStr,
      todayStr
    );

    // ─── SellerX: Read returns page ──────────────────────────
    await loginToSellerX(page);
    await page.goto("/tr/returns");
    await page.waitForLoadState("networkidle");
    await page.waitForTimeout(5000);

    await takeScreenshot(page, "06-returns-sellerx", reportDir);

    // Extract summary card values using DOM structure
    // Cards: grid of 4, each with p.text-sm.text-muted-foreground (title) + p.text-xl.font-bold (value)
    const extractReturnCard = async (cardTitle: string): Promise<string> => {
      try {
        const titleEl = page
          .locator(`p.text-sm.text-muted-foreground:has-text("${cardTitle}")`)
          .first();
        if (!(await titleEl.isVisible({ timeout: 2000 }).catch(() => false)))
          return "";
        // Value is the next sibling p element in the same parent div
        const parent = titleEl.locator("..");
        const valueEl = parent.locator("p.text-xl").first();
        return (await valueEl.textContent())?.trim() || "";
      } catch {
        return "";
      }
    };

    const feReturnCount = await extractReturnCard("Toplam İade");
    const feReturnRate = await extractReturnCard("İade Oranı");
    const feTotalLoss = await extractReturnCard("Toplam Zarar");

    // ─── Trendyol: Read returns page ─────────────────────────
    let tyReturnData: { totalReturns: string; returnItems: string[] } = {
      totalReturns: "",
      returnItems: [],
    };
    if (hasValidAuthState()) {
      try {
        const ty = await createTrendyolContext();
        try {
          await ty.page.goto(TrendyolPages.returns, {
            waitUntil: "domcontentloaded",
            timeout: 30000,
          });
          await ty.page.waitForTimeout(5000);
          await takeScreenshot(ty.page, "06-returns-trendyol", reportDir);

          const tyReturns = await extractTrendyolReturns(ty.page);
          tyReturnData = {
            totalReturns: tyReturns.totalCount || "",
            returnItems: [],
          };
        } finally {
          await ty.close();
        }
      } catch (e) {
        console.log("Trendyol kullanılamadı:", (e as Error).message);
      }
    }

    // ─── Compare ───────────────────────────────────────────────
    const results: ComparisonResult[] = [];

    // Return count
    results.push(
      compareField(
        "İade Adedi (Son 3 Yıl)",
        dbReturns.returnCount,
        feReturnCount ? parseTurkishNumber(feReturnCount) : null,
        tyReturnData.totalReturns
          ? parseInt(tyReturnData.totalReturns, 10)
          : null,
        "count"
      )
    );

    // Return rate (percentage)
    if (dbReturns.returnRate !== undefined && dbReturns.returnRate !== null) {
      results.push(
        compareField(
          "İade Oranı (%)",
          dbReturns.returnRate,
          feReturnRate ? parseTurkishNumber(feReturnRate) : null,
          null,
          "percent"
        )
      );
    }

    // Total loss amount (DB calculation is too complex to replicate - involves
    // cargo invoices, product costs, packaging, resalability, etc.)
    // Only log for reference, don't compare DB vs FE
    if (feTotalLoss) {
      results.push(
        compareField(
          "Toplam İade Kaybi (FE Only)",
          null,
          parseTurkishNumber(feTotalLoss),
          null,
          "money"
        )
      );
    }

    // ─── Report ────────────────────────────────────────────────
    const summary = compareSets(results);
    appendSection({
      title: "İadeler Sayfası (Son 3 Yıl)",
      summary,
      screenshots: ["06-returns-sellerx.png", "06-returns-trendyol.png"],
    });

    console.log(
      `\nİadeler: ${summary.matching}/${summary.total} eşleşen, ${summary.warnings} uyarı, ${summary.errors} hata`
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
