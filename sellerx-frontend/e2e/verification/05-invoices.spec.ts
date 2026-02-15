/**
 * 05 - Invoices Page Triple Verification
 *
 * Verifies invoice summary data from the financial/invoices page across:
 * - PostgreSQL database (category grouping)
 * - SellerX frontend (/financial/invoices)
 * - Trendyol seller panel (finance/invoices)
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
  getInvoiceSummary,
  formatLocalDate,
  closePool,
} from "./helpers/db-client";
import {
  extractTrendyolFinance,
  parseTurkishNumber,
  takeScreenshot,
} from "./helpers/data-extractor";
import { compareField, compareSets, type ComparisonResult } from "./helpers/comparison";
import { appendSection, getOrInitReportDir } from "./helpers/report-generator";

test.describe("05 - Invoices Page Verification", () => {
  test.setTimeout(180000);

  test.afterAll(async () => {
    await closePool();
  });

  test("should verify invoice summary data across sources", async ({
    page,
  }) => {
    const reportDir = getOrInitReportDir();
    const email = getTestUserEmail();
    const storeId = await getStoreIdForUser(email);
    expect(storeId).toBeTruthy();

    // ─── DB: Get invoice summary for last 30 days ──────────────
    const today = new Date();
    const todayStr = formatLocalDate(today);
    const thirtyDaysAgo = new Date(today);
    thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
    const thirtyDaysAgoStr = formatLocalDate(thirtyDaysAgo);

    const dbSummary = await getInvoiceSummary(
      storeId!,
      thirtyDaysAgoStr,
      todayStr
    );

    // ─── SellerX: Read invoice page summary cards ──────────────
    await loginToSellerX(page);
    await page.goto("/tr/financial/invoices");
    await page.waitForLoadState("networkidle");
    await page.waitForTimeout(5000);

    await takeScreenshot(page, "05-invoices-sellerx", reportDir);

    // Extract summary card values using DOM structure
    // Each card: colored header with h3 title → body with "Toplam Tutar" → p.text-xl.font-bold
    const categoryCards: Record<string, { amount: string; count: string }> = {};
    const cardHeaders = page.locator("h3.text-sm.font-semibold.text-white");
    const cardCount = await cardHeaders.count();

    for (let ci = 0; ci < cardCount; ci++) {
      const h3 = cardHeaders.nth(ci);
      const title = (await h3.textContent())?.trim() || "";

      // Navigate to card root: h3 → flex div → header div → card wrapper
      const card = h3.locator("../../..");

      // Extract amount from p.text-xl.font-bold
      let amount = "";
      try {
        const amountEl = card.locator("p.text-xl.font-bold").first();
        amount = (await amountEl.textContent())?.trim() || "";
      } catch { /* ignore */ }

      // Extract invoice count from "Fatura" label sibling
      let count = "";
      try {
        const countLabel = card.locator('span:has-text("Fatura")').first();
        if (await countLabel.isVisible({ timeout: 1000 }).catch(() => false)) {
          const countEl = countLabel.locator("..").locator("p.font-medium").first();
          count = (await countEl.textContent())?.trim() || "";
        }
      } catch { /* ignore */ }

      categoryCards[title] = { amount, count };
    }

    // ─── Trendyol: Read finance invoices ───────────────────────
    let tyInvoiceData: Record<string, string> = {};
    if (hasValidAuthState()) {
      try {
        const ty = await createTrendyolContext();
        try {
          await ty.page.goto(TrendyolPages.invoices, {
            waitUntil: "domcontentloaded",
            timeout: 30000,
          });
          await ty.page.waitForTimeout(5000);
          await takeScreenshot(ty.page, "05-invoices-trendyol", reportDir);

          // Extract Trendyol invoice page data using dedicated extractor
          const tyRawData = await extractTrendyolFinance(ty.page);

          // Map extracted keys to our comparison keys
          tyInvoiceData = {
            komisyon: tyRawData["Komisyon"] || "",
            kargo: tyRawData["Kargo"] || "",
            ceza: tyRawData["Ceza"] || "",
            iade: tyRawData["İade"] || "",
            toplamFatura: tyRawData["Toplam Fatura"] || "",
          };

          console.log("Trendyol fatura verisi:", JSON.stringify(tyInvoiceData));
        } finally {
          await ty.close();
        }
      } catch (e) {
        console.log("Trendyol kullanılamadı:", (e as Error).message);
      }
    }

    // ─── Compare ───────────────────────────────────────────────
    const results: ComparisonResult[] = [];

    // Map DB categories to card titles (exact match from invoice-summary-cards.tsx)
    // FE shows deductions with "-" prefix and refunds with "+" prefix
    // DB returns debt-credit (positive for deductions, negative for refunds)
    // We compare absolute values since the sign is just a display convention
    const categoryMap: Record<string, { cardTitle: string; tyKey: string }> = {
      KOMISYON: { cardTitle: "Komisyon", tyKey: "komisyon" },
      KARGO: { cardTitle: "Kargo", tyKey: "kargo" },
      CEZA: { cardTitle: "Ceza", tyKey: "ceza" },
      IADE: { cardTitle: "Geri Yatan", tyKey: "iade" },
      REKLAM: { cardTitle: "Reklam", tyKey: "reklam" },
      ULUSLARARASI: { cardTitle: "Uluslararası", tyKey: "" },
      PLATFORM_UCRETLERI: { cardTitle: "Platform", tyKey: "" },
      DIGER: { cardTitle: "Diğer", tyKey: "" },
    };

    for (const [category, { cardTitle, tyKey }] of Object.entries(categoryMap)) {
      const dbCat = dbSummary.find((s) => s.category === category);

      // Find matching card by title (partial match for safety)
      const cardKey = Object.keys(categoryCards).find((k) =>
        k.includes(cardTitle)
      );
      const feCard = cardKey ? categoryCards[cardKey] : undefined;
      const tyValue = tyKey ? tyInvoiceData[tyKey] || "" : "";

      // Compare absolute values (FE adds +/- sign prefix for display)
      const dbAmount = Math.abs(dbCat?.totalAmount ?? 0);
      const feAmount = feCard?.amount ? Math.abs(parseTurkishNumber(feCard.amount)) : null;
      const tyAmount = tyValue ? Math.abs(parseTurkishNumber(tyValue)) : null;

      results.push(
        compareField(
          `Fatura - ${category} Toplam`,
          dbAmount,
          feAmount,
          tyAmount,
          "money"
        )
      );

      if (feCard?.count) {
        results.push(
          compareField(
            `Fatura - ${category} Adet`,
            dbCat?.count ?? 0,
            parseInt(feCard.count, 10) || null,
            null,
            "count"
          )
        );
      }
    }

    // Total invoice count from DB
    const totalDbInvoices = dbSummary.reduce((sum, s) => sum + s.count, 0);
    results.push(
      compareField(
        "Toplam Fatura Sayisi (DB)",
        totalDbInvoices,
        null,
        null,
        "count"
      )
    );

    // ─── Report ────────────────────────────────────────────────
    const summary = compareSets(results);
    appendSection({
      title: "Faturalar Sayfasi (Son 30 Gun)",
      summary,
      screenshots: ["05-invoices-sellerx.png", "05-invoices-trendyol.png"],
    });

    console.log(
      `\nFaturalar: ${summary.matching}/${summary.total} eslesen, ${summary.warnings} uyari, ${summary.errors} hata`
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
