/**
 * Data extraction functions for SellerX frontend and Trendyol panel.
 * Reads visible values from page DOM elements.
 */
import type { Page } from "@playwright/test";

// ─── Utility ───────────────────────────────────────────────────

/**
 * Parse a Turkish-formatted currency string to number.
 * Examples: "12.450,00" → 12450.00, "₺1.245,50" → 1245.50
 */
export function parseTurkishNumber(text: string): number {
  const cleaned = text
    .replace(/[₺$€\s]/g, "")
    .replace(/\./g, "")    // remove thousands dots
    .replace(",", ".")      // decimal comma → dot
    .replace(/-$/, "")      // trailing dash
    .trim();
  const num = parseFloat(cleaned);
  return isNaN(num) ? 0 : num;
}

/**
 * Get text content of an element, trimmed.
 */
async function getText(page: Page, selector: string): Promise<string> {
  try {
    const el = page.locator(selector).first();
    await el.waitFor({ state: "visible", timeout: 5000 });
    return (await el.textContent())?.trim() || "";
  } catch {
    return "";
  }
}

/**
 * Get all matching elements' text content.
 */
async function getAllTexts(page: Page, selector: string): Promise<string[]> {
  try {
    const elements = page.locator(selector);
    const count = await elements.count();
    const texts: string[] = [];
    for (let i = 0; i < count; i++) {
      const text = await elements.nth(i).textContent();
      texts.push(text?.trim() || "");
    }
    return texts;
  } catch {
    return [];
  }
}

// ─── SellerX Dashboard Cards ───────────────────────────────────

export interface DashboardCardData {
  title: string;
  sales: string;        // Brut ciro
  ordersUnits: string;  // "45 / 67" format
  refunds: string;
  grossProfit: string;
  netProfit: string;
}

/**
 * Extract period card data from the dashboard.
 * Each card has a colored header with an h3 title, then labeled values in the body.
 * Structure per card:
 *   h3 = title (Bugün, Dün, Bu Ay, Geçen Ay)
 *   "Satışlar" label → large formatted currency
 *   "Sipariş / Adet" → "X / Y"
 *   "İadeler" → number
 *   "Brüt Kâr" → currency
 *   "Net Kâr" → currency
 */
export async function extractDashboardCards(
  page: Page
): Promise<DashboardCardData[]> {
  await page.waitForTimeout(3000);

  const cards: DashboardCardData[] = [];

  // Find cards by their h3 headers (each PeriodCard has exactly one h3)
  const cardHeaders = page.locator("h3.text-sm.font-semibold.text-white");
  const count = await cardHeaders.count();

  for (let i = 0; i < count; i++) {
    const h3 = cardHeaders.nth(i);
    const title = (await h3.textContent())?.trim() || `Card ${i}`;

    // Navigate up to the card root: h3 → header div → card wrapper
    const card = h3.locator("../..");

    // Extract the large sales value (text-2xl font-bold)
    const salesEl = card.locator("p.text-2xl").first();
    const sales = (await salesEl.textContent())?.trim() || "";

    // Extract orders/units - it's the value after "Sipariş / Adet" label
    const ordersUnits = await extractLabelValue(card, "Sipariş / Adet");

    // Extract refunds - value after "İadeler" label
    const refunds = await extractLabelValue(card, "İadeler");

    // Extract gross profit - value after "Brüt Kâr" label
    const grossProfit = await extractLabelValue(card, "Brüt Kâr");

    // Extract net profit - value after "Net Kâr" label
    const netProfit = await extractLabelValue(card, "Net Kâr");

    cards.push({
      title,
      sales,
      ordersUnits,
      refunds,
      grossProfit,
      netProfit,
    });
  }

  return cards;
}

/**
 * Extract the value that follows a label span within a card element.
 * Card structure: <span class="text-muted-foreground">Label</span> then <p>Value</p>
 */
async function extractLabelValue(
  card: ReturnType<Page["locator"]>,
  label: string
): Promise<string> {
  try {
    // Find the span containing the label text
    const labelEl = card.locator(`span:has-text("${label}")`).first();
    if (!(await labelEl.isVisible({ timeout: 1000 }).catch(() => false))) {
      return "";
    }
    // The value is in the next sibling <p> element
    const parent = labelEl.locator("..");
    const valueEl = parent.locator("p").first();
    return (await valueEl.textContent())?.trim() || "";
  } catch {
    return "";
  }
}

/**
 * Extract a value that appears after a label keyword in an array of text lines.
 */
function extractValueAfterLabel(
  lines: string[],
  keywords: string[]
): string {
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].toLowerCase();
    if (keywords.some((k) => line.includes(k.toLowerCase()))) {
      // The value might be on the same line (after the keyword) or next line
      const match = lines[i].match(/[\d.,]+/);
      if (match) return match[0];
      if (i + 1 < lines.length) {
        const nextMatch = lines[i + 1].match(/[₺\d.,\-\s]+/);
        if (nextMatch) return nextMatch[0].trim();
      }
    }
  }
  return "";
}

// ─── SellerX Dashboard Products Table ──────────────────────────

export interface DashboardProductRow {
  name: string;
  sku: string;
  unitsSold: string;
  refunds: string;
  sales: string;
  commission: string;
  grossProfit: string;
  netProfit: string;
  margin: string;
  roi: string;
}

/**
 * Extract products from the dashboard products table.
 * First cell: image + name (.font-medium.text-sm) + SKU line (p.text-xs: "{sku} - Maliyet: {cost}") + stock line
 * Remaining cells: dynamic visible columns (unitsSold, refunds, sales, commission, grossProfit, netProfit, margin, roi)
 * Last cell: "Detay" button
 */
export async function extractProductsTable(
  page: Page,
  maxRows = 10
): Promise<DashboardProductRow[]> {
  // Ensure products tab is active
  const productsTab = page.locator('button:has-text("Ürünler")').first();
  if (await productsTab.isVisible()) {
    await productsTab.click();
    await page.waitForTimeout(1000);
  }

  const rows: DashboardProductRow[] = [];
  const tableRows = page.locator("table tbody tr");
  const count = Math.min(await tableRows.count(), maxRows);

  for (let i = 0; i < count; i++) {
    const row = tableRows.nth(i);
    const cells = row.locator("td");
    const cellCount = await cells.count();
    if (cellCount < 3) continue; // Skip non-data rows

    const firstCell = cells.nth(0);

    // Product name: .font-medium.text-sm (could be p or a element)
    let name = "";
    try {
      const nameEl = firstCell.locator(".font-medium.text-sm").first();
      name = (await nameEl.textContent())?.trim() || "";
    } catch { /* ignore */ }

    // SKU/Barcode: first p.text-xs line format is "{sku} - Maliyet: {cost}"
    let sku = "";
    try {
      const skuLine = firstCell.locator("p.text-xs").first();
      const skuText = (await skuLine.textContent())?.trim() || "";
      const dashIdx = skuText.indexOf(" - ");
      sku = dashIdx > 0 ? skuText.substring(0, dashIdx).trim() : skuText;
    } catch { /* ignore */ }

    // Remaining cells (skip last cell which is "Detay" button)
    const values: string[] = [];
    for (let j = 1; j < cellCount - 1; j++) {
      const text = (await cells.nth(j).textContent())?.trim() || "";
      values.push(text);
    }

    rows.push({
      name,
      sku,
      unitsSold: values[0] || "",
      refunds: values[1] || "",
      sales: values[2] || "",
      commission: values[3] || "",
      grossProfit: values[4] || "",
      netProfit: values[5] || "",
      margin: values[6] || "",
      roi: values[7] || "",
    });
  }

  return rows;
}

// ─── SellerX Product Detail Panel ──────────────────────────────

export interface ProductDetailData {
  name: string;
  sku: string;
  sales: string;
  units: string;
  returns: string;
  commission: string;
  shippingCost: string;
  grossProfit: string;
  netProfit: string;
  margin: string;
  roi: string;
}

/**
 * Extract data from the product detail slide-over panel.
 * Must be called after clicking "Detay" on a product row.
 */
export async function extractProductDetail(
  page: Page
): Promise<ProductDetailData> {
  // Wait for panel to appear
  await page.waitForTimeout(1500);

  const panelText = await page
    .locator('[role="dialog"], .sheet-content, [data-state="open"]')
    .first()
    .textContent();

  const lines = (panelText || "").split("\n").map((l) => l.trim()).filter(Boolean);

  return {
    name: lines[0] || "",
    sku: extractValueAfterLabel(lines, ["Barkod", "SKU"]),
    sales: extractValueAfterLabel(lines, ["Satış", "Ciro"]),
    units: extractValueAfterLabel(lines, ["Satılan", "Adet"]),
    returns: extractValueAfterLabel(lines, ["İade"]),
    commission: extractValueAfterLabel(lines, ["Komisyon"]),
    shippingCost: extractValueAfterLabel(lines, ["Kargo"]),
    grossProfit: extractValueAfterLabel(lines, ["Brüt"]),
    netProfit: extractValueAfterLabel(lines, ["Net Kâr", "Net Kar"]),
    margin: extractValueAfterLabel(lines, ["Marj"]),
    roi: extractValueAfterLabel(lines, ["ROI"]),
  };
}

// ─── SellerX Order Detail Panel ────────────────────────────────

export interface OrderDetailData {
  orderNumber: string;
  orderDate: string;
  totalPrice: string;
  commission: string;
  shippingCost: string;
  grossProfit: string;
  netProfit: string;
}

/**
 * Extract data from the order detail slide-over panel.
 */
export async function extractOrderDetail(
  page: Page
): Promise<OrderDetailData> {
  await page.waitForTimeout(1500);

  const panelText = await page
    .locator('[role="dialog"], .sheet-content, [data-state="open"]')
    .first()
    .textContent();

  const lines = (panelText || "").split("\n").map((l) => l.trim()).filter(Boolean);

  return {
    orderNumber: extractValueAfterLabel(lines, ["Sipariş No", "#"]),
    orderDate: extractValueAfterLabel(lines, ["Tarih"]),
    totalPrice: extractValueAfterLabel(lines, ["Toplam", "Brüt Satış"]),
    commission: extractValueAfterLabel(lines, ["Komisyon"]),
    shippingCost: extractValueAfterLabel(lines, ["Kargo"]),
    grossProfit: extractValueAfterLabel(lines, ["Brüt Kâr", "Brüt Kar"]),
    netProfit: extractValueAfterLabel(lines, ["Net Kâr", "Net Kar"]),
  };
}

// ─── SellerX Period Detail Modal ───────────────────────────────

export interface PeriodDetailData {
  sales: string;
  orders: string;
  units: string;
  refunds: string;
  commission: string;
  shippingCost: string;
  productCosts: string;
  grossProfit: string;
  netProfit: string;
  [key: string]: string;
}

/**
 * Extract data from the period detail modal (32 metrics).
 */
export async function extractPeriodDetail(
  page: Page
): Promise<PeriodDetailData> {
  await page.waitForTimeout(1500);

  const dialog = page.locator('[role="dialog"]').first();

  // Extract all label-value pairs from ExpandableRow components.
  // Each row is a div.flex.items-center.justify-between containing:
  //   Simple: <span>label</span> <span>value</span>
  //   Expandable: <div><chevron/><span>label</span></div> <span>value</span>
  const rowData = await dialog.evaluate((el) => {
    const result: Record<string, string> = {};
    // querySelectorAll with Tailwind classes - use attribute selector for robustness
    const rows = el.querySelectorAll(
      "div.flex.items-center.justify-between"
    );
    rows.forEach((row) => {
      const children = Array.from(row.children);

      // Simple row: two direct <span> children
      const directSpans = children.filter((c) => c.tagName === "SPAN");
      if (directSpans.length >= 2) {
        const label = directSpans[0].textContent?.trim() || "";
        const value = directSpans[directSpans.length - 1].textContent?.trim() || "";
        if (label && value) result[label] = value;
        return;
      }

      // Expandable row: <div> (with label span inside) + <span> (value)
      const labelDiv = children.find((c) => c.tagName === "DIV");
      const valueSpan = children.find((c) => c.tagName === "SPAN");
      if (labelDiv && valueSpan) {
        const innerSpan = labelDiv.querySelector("span");
        if (innerSpan) {
          const label = innerSpan.textContent?.trim() || "";
          const value = valueSpan.textContent?.trim() || "";
          if (label && value) result[label] = value;
        }
      }
    });
    return result;
  });

  // Map labels to our fields using keyword matching
  const findValue = (keywords: string[]): string => {
    for (const key of Object.keys(rowData)) {
      if (keywords.some((kw) => key.includes(kw))) {
        return rowData[key];
      }
    }
    return "";
  };

  return {
    sales: findValue(["Satışlar", "Brüt Ciro"]),
    orders: findValue(["Sipariş Sayısı"]),
    units: findValue(["Satış Adedi"]),
    refunds: findValue(["İade"]),
    commission: findValue(["Komisyon"]),
    shippingCost: findValue(["Kargo Maliyeti"]),
    productCosts: findValue(["Ürün Maliyeti"]),
    grossProfit: findValue(["Brüt Kâr"]),
    netProfit: findValue(["Net Kâr"]),
  };
}

// ─── Trendyol Panel Extractors ─────────────────────────────────

/**
 * Extract order list from Trendyol seller panel orders page.
 * Real DOM: Chakra UI table `table tbody tr` with 8 columns:
 * (checkbox) | Sipariş Bilgileri | Alıcı | Bilgiler | Birim Fiyat | Kargo | Fatura | Durum
 * Some <tr> rows have only 1 cell (expanded detail rows) — filter with cells >= 8.
 */
export async function extractTrendyolOrders(
  page: Page,
  maxRows = 5
): Promise<
  Array<{
    orderNumber: string;
    totalPrice: string;
    status: string;
    date: string;
    barcode: string;
  }>
> {
  await page.waitForTimeout(3000);

  return await page.evaluate((limit: number) => {
    const rows = document.querySelectorAll("table tbody tr");
    const results: Array<{
      orderNumber: string;
      totalPrice: string;
      status: string;
      date: string;
      barcode: string;
    }> = [];

    for (let i = 0; i < rows.length && results.length < limit; i++) {
      const cells = rows[i].querySelectorAll("td");
      if (cells.length < 8) continue; // Skip expanded detail rows

      const cell1Text = cells[1]?.textContent || "";
      const cell3Text = cells[3]?.textContent || "";
      const cell4Text = cells[4]?.textContent || "";
      const cell6Text = cells[6]?.textContent || "";

      // Order number from cell 1 (Sipariş Bilgileri)
      const orderNumMatch = cell1Text.match(/#?(\d{10,})/);
      const orderNumber = orderNumMatch?.[1] || "";

      // Date from cell 1
      const dateMatch = cell1Text.match(
        /Sipariş Tarihi:\s*(\d{2}\.\d{2}\.\d{4}\s+\d{2}:\d{2})/
      );
      const date = dateMatch?.[1]?.trim() || "";

      // Status from cell 7 (Durum) or first line of cell 1
      const statusText = cells[7]?.textContent?.trim() || "";
      const status = statusText || "";

      // Barcode from cell 3 (Bilgiler) — EAN-13 format
      const barcodeMatch = cell3Text.match(/(\d{13})/);
      const barcode = barcodeMatch?.[1] || "";

      // Price from cell 4 (Birim Fiyat)
      const priceMatch = cell4Text.match(/₺\s*([\d.,]+)/);
      let totalPrice = priceMatch?.[1] || "";

      // Prefer Satış Tutarı from cell 6 (Fatura) if available
      const salesMatch = cell6Text.match(/Satış Tutarı:\s*₺\s*([\d.,]+)/);
      if (salesMatch) {
        totalPrice = salesMatch[1];
      }

      if (orderNumber) {
        results.push({ orderNumber, totalPrice, status, date, barcode });
      }
    }
    return results;
  }, maxRows);
}

/**
 * Extract invoice data from Trendyol invoice page (/payments/invoice).
 * Real DOM: Filter area (Fatura Tipi, Tarih, Ülke) + invoice list in generic divs.
 * Each invoice row contains: Fatura Numarası, Fatura Tipi, Ülke, Fatura Tarihi, Tutar.
 * Extracts total invoice count from "Toplam N Fatura Listesi" text, plus
 * visible row amounts aggregated by invoice type.
 */
export async function extractTrendyolFinance(
  page: Page
): Promise<Record<string, string>> {
  await page.waitForTimeout(3000);

  return await page.evaluate(() => {
    const data: Record<string, string> = {};
    const bodyText = document.body.textContent || "";

    // Total invoice count: "Toplam 726 Fatura Listesi"
    const totalMatch = bodyText.match(/Toplam\s+(\d+)\s+Fatura/i);
    if (totalMatch) {
      data["Toplam Fatura"] = totalMatch[1];
    }

    // Aggregate visible invoice row amounts by type.
    // Invoice type keywords → our category keys
    const typeMap: Array<[string, RegExp]> = [
      ["Kargo", /Kargo Fatura/i],
      ["Komisyon", /Platform Hizmet Bedeli/i],
      ["Reklam", /Reklam Bedeli/i],
      [
        "Ceza",
        /Tedarik Edememe|Termin Gecikme|Eksik.*Yanlış|Kusurlu/i,
      ],
    ];

    // Scan table rows or div-based rows for type + amount pairs
    const candidates = Array.from(document.querySelectorAll("tr, div"));
    const typeAmounts: Record<string, number> = {};
    const typeCounts: Record<string, number> = {};

    for (const el of candidates) {
      if (el.children.length > 20) continue;
      const text = el.textContent?.trim() || "";
      if (text.length > 500 || text.length < 10) continue;

      // Look for Turkish currency format: 1.234,56 ₺
      const amountMatch = text.match(/([\d.]+,\d{2})\s*₺/);
      if (!amountMatch) continue;

      const amount = parseFloat(
        amountMatch[1].replace(/\./g, "").replace(",", ".")
      );
      if (isNaN(amount) || amount === 0) continue;

      for (const [key, regex] of typeMap) {
        if (regex.test(text)) {
          typeAmounts[key] = (typeAmounts[key] || 0) + amount;
          typeCounts[key] = (typeCounts[key] || 0) + 1;
          break;
        }
      }
    }

    for (const [type, amount] of Object.entries(typeAmounts)) {
      // Return as Turkish-formatted string for consistency
      data[type] = amount.toFixed(2).replace(".", ",");
      data[`${type} Adet`] = String(typeCounts[type] || 0);
    }

    return data;
  });
}

/**
 * Extract product list from Trendyol products page (/product-listing/all-products).
 * Real DOM: Custom div table `.spm-table .tbody .tr.product-list-item-{n} .td`
 * 15 columns: Ürün Bilgisi | Varyant | Durum | Doluluk Oranı | Stok Kodu |
 *             Komisyon | Trendyol Satış Fiyatı | Stok | Buybox Fiyatı | Buybox |
 *             Termin Süresi | Kategori | Marka | Komisyona Esas Fiyat | İşlemler
 *
 * Inactive products show "Güncelle" instead of price/stock — these are skipped.
 */
export async function extractTrendyolProducts(
  page: Page,
  maxRows = 20
): Promise<
  Array<{
    name: string;
    barcode: string;
    price: string;
    stock: string;
    status: string;
    commission: string;
    category: string;
    brand: string;
  }>
> {
  await page.waitForTimeout(3000);

  return await page.evaluate((limit: number) => {
    // Try custom div table first (primary Trendyol product list DOM)
    let rows = document.querySelectorAll(
      '.spm-table .tbody .tr[class*="product-list-item"]'
    );

    // Fallback: try broader selectors if specific class not found
    if (rows.length === 0) {
      rows = document.querySelectorAll(".spm-table .tbody .tr");
    }
    if (rows.length === 0) {
      rows = document.querySelectorAll(".tbody .tr");
    }

    const results: Array<{
      name: string;
      barcode: string;
      price: string;
      stock: string;
      status: string;
      commission: string;
      category: string;
      brand: string;
    }> = [];

    for (let i = 0; i < Math.min(rows.length, limit); i++) {
      const cells = rows[i].querySelectorAll(".td");
      if (cells.length < 8) continue;

      // Cell 0: Ürün Bilgisi — contains product name and barcode
      const cell0Text = cells[0]?.textContent || "";
      const barcodeMatch = cell0Text.match(/Barkod:\s*(\d+)/);
      const barcode = barcodeMatch?.[1] || "";

      // Name: first meaningful text line in cell 0
      const nameLines = cell0Text
        .split("\n")
        .map((l) => l.trim())
        .filter((l) => l && !l.startsWith("Barkod:") && !l.startsWith("Model"));
      const name = nameLines[0] || "";

      // Cell 2: Durum (Status) — "Satışta", "Tükendi", "Satışa Kapalı"
      const status = cells[2]?.textContent?.trim() || "";

      // Cell 5: Komisyon — e.g. "%19"
      const commission = cells[5]?.textContent?.trim() || "";

      // Cell 6: Trendyol Satış Fiyatı — e.g. "849,75 ₺" or "Güncelle"
      const priceText = cells[6]?.textContent?.trim() || "";
      const price = priceText.includes("Güncelle") ? "" : priceText;

      // Cell 7: Stok — number or "Güncelle"
      const stockText = cells[7]?.textContent?.trim() || "";
      const stock = stockText.includes("Güncelle") ? "" : stockText;

      // Cell 11: Kategori (may not exist if fewer columns)
      const category =
        cells.length > 11 ? cells[11]?.textContent?.trim() || "" : "";

      // Cell 12: Marka
      const brand =
        cells.length > 12 ? cells[12]?.textContent?.trim() || "" : "";

      results.push({
        name,
        barcode,
        price,
        stock,
        status,
        commission,
        category,
        brand,
      });
    }
    return results;
  }, maxRows);
}

/**
 * Extract return claims from Trendyol returns page (/orders/claims).
 * Real DOM: Same table structure as orders + İade Sebebi column.
 * Key data comes from tab text: "Tüm İadeler 157 Paket", "Onaylanan 148", etc.
 */
export async function extractTrendyolReturns(
  page: Page
): Promise<{
  totalCount: string;
  totalAmount: string;
  approved: string;
  rejected: string;
  pending: string;
}> {
  await page.waitForTimeout(3000);

  return await page.evaluate(() => {
    const bodyText = document.body.textContent || "";

    // Extract counts from tab text
    // Formats: "Tüm İadeler 157 Paket", "Onaylanan 148", "Reddedilen 6", "Talep Oluşturulan 3"
    const totalMatch = bodyText.match(/Tüm İadeler\s*(\d+)\s*Paket/);
    const approvedMatch = bodyText.match(/Onaylanan\s*(\d+)/);
    const rejectedMatch = bodyText.match(/Reddedilen\s*(\d+)/);
    const pendingMatch = bodyText.match(/Talep Oluşturulan\s*(\d+)/);

    // Try alternative total count pattern if first one fails
    let totalCount = totalMatch?.[1] || "";
    if (!totalCount) {
      // Try "N Paket" pattern from any tab
      const altMatch = bodyText.match(/(\d+)\s*Paket/);
      totalCount = altMatch?.[1] || "";
    }

    // Try to sum amounts from visible table rows for totalAmount
    let totalAmount = "";
    const rows = Array.from(document.querySelectorAll("table tbody tr"));
    let sum = 0;
    let foundPrices = false;
    for (const row of rows) {
      const cells = row.querySelectorAll("td");
      if (cells.length < 5) continue;
      // Price is typically in cell 4 (Birim Fiyat)
      const priceText = cells[4]?.textContent || "";
      const priceMatch = priceText.match(/₺\s*([\d.,]+)/);
      if (priceMatch) {
        const amount = parseFloat(
          priceMatch[1].replace(/\./g, "").replace(",", ".")
        );
        if (!isNaN(amount)) {
          sum += amount;
          foundPrices = true;
        }
      }
    }
    if (foundPrices) {
      totalAmount = sum.toFixed(2).replace(".", ",");
    }

    return {
      totalCount,
      totalAmount,
      approved: approvedMatch?.[1] || "",
      rejected: rejectedMatch?.[1] || "",
      pending: pendingMatch?.[1] || "",
    };
  });
}

// ─── Screenshot helper ─────────────────────────────────────────

/**
 * Take a labeled screenshot and return the path.
 */
export async function takeScreenshot(
  page: Page,
  name: string,
  outputDir: string
): Promise<string> {
  const path = `${outputDir}/screenshots/${name}.png`;
  await page.screenshot({ path, fullPage: false });
  return path;
}
