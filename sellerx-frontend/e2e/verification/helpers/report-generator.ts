/**
 * Report generator for triple verification results.
 * Produces Markdown and HTML reports with raw JSON data.
 */
import * as fs from "fs";
import * as path from "path";
import type { ComparisonSummary, ComparisonResult } from "./comparison";

export interface ReportSection {
  title: string;
  summary: ComparisonSummary;
  screenshots?: string[];
}

export interface VerificationReport {
  storeName: string;
  storeEmail: string;
  timestamp: string;
  sections: ReportSection[];
}

// ─── Report directory ──────────────────────────────────────────

function getReportDir(): string {
  const now = new Date();
  const dateStr = now.toISOString().replace(/[:.]/g, "-").slice(0, 16);
  const dir = path.join(
    process.cwd(),
    "verification-reports",
    dateStr
  );
  fs.mkdirSync(dir, { recursive: true });
  fs.mkdirSync(path.join(dir, "screenshots"), { recursive: true });
  return dir;
}

let currentReportDir: string | null = null;

export function initReportDir(): string {
  currentReportDir = getReportDir();
  return currentReportDir;
}

export function getOrInitReportDir(): string {
  if (!currentReportDir) {
    currentReportDir = getReportDir();
  }
  return currentReportDir;
}

// ─── Markdown Report ───────────────────────────────────────────

function resultIcon(r: ComparisonResult): string {
  if (r.match) return "OK";
  if (r.severity === "warning") return "WARN";
  return "ERR";
}

function resultEmoji(r: ComparisonResult): string {
  if (r.match) return "&#x2705;"; // green check (HTML entity for reports)
  if (r.severity === "warning") return "&#x26A0;&#xFE0F;"; // warning
  return "&#x274C;"; // red cross
}

function formatVal(val: string | number | null): string {
  if (val === null || val === undefined) return "-";
  if (typeof val === "number") {
    return new Intl.NumberFormat("tr-TR", {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(val);
  }
  return String(val);
}

export function generateMarkdownReport(report: VerificationReport): string {
  const totalChecks = report.sections.reduce(
    (sum, s) => sum + s.summary.total,
    0
  );
  const totalMatch = report.sections.reduce(
    (sum, s) => sum + s.summary.matching,
    0
  );
  const totalWarn = report.sections.reduce(
    (sum, s) => sum + s.summary.warnings,
    0
  );
  const totalErr = report.sections.reduce(
    (sum, s) => sum + s.summary.errors,
    0
  );
  const matchPct =
    totalChecks > 0 ? ((totalMatch / totalChecks) * 100).toFixed(1) : "0";

  let md = `# SellerX Dogrulama Raporu

Tarih: ${report.timestamp}
Magaza: ${report.storeName} (${report.storeEmail})

## Ozet

- Toplam kontrol: ${totalChecks}
- Eslesen: ${totalMatch} (${matchPct}%)
- Uyari: ${totalWarn}
- Hata: ${totalErr}

`;

  for (const section of report.sections) {
    md += `## ${section.title}\n\n`;
    md += `| # | Alan | DB | SellerX | Trendyol | Sapma | Sonuc |\n`;
    md += `|---|------|-----|---------|----------|-------|-------|\n`;

    section.summary.results.forEach((r, i) => {
      md += `| ${i + 1} | ${r.field} | ${formatVal(r.dbValue)} | ${formatVal(r.frontendValue)} | ${formatVal(r.trendyolValue)} | ${r.deviation !== null ? `%${r.deviation}` : "-"} | ${resultIcon(r)} |\n`;
    });

    md += `\n**Sonuc**: ${section.summary.matching}/${section.summary.total} eslesen`;
    if (section.summary.warnings > 0)
      md += `, ${section.summary.warnings} uyari`;
    if (section.summary.errors > 0)
      md += `, ${section.summary.errors} hata`;
    md += `\n\n`;
  }

  return md;
}

// ─── HTML Report ───────────────────────────────────────────────

export function generateHtmlReport(report: VerificationReport): string {
  const totalChecks = report.sections.reduce(
    (sum, s) => sum + s.summary.total,
    0
  );
  const totalMatch = report.sections.reduce(
    (sum, s) => sum + s.summary.matching,
    0
  );
  const totalWarn = report.sections.reduce(
    (sum, s) => sum + s.summary.warnings,
    0
  );
  const totalErr = report.sections.reduce(
    (sum, s) => sum + s.summary.errors,
    0
  );
  const matchPct =
    totalChecks > 0 ? ((totalMatch / totalChecks) * 100).toFixed(1) : "0";

  let html = `<!DOCTYPE html>
<html lang="tr">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>SellerX Dogrulama Raporu - ${report.timestamp}</title>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; padding: 24px; background: #f8f9fa; color: #1a1a2e; }
  .container { max-width: 1200px; margin: 0 auto; }
  h1 { font-size: 24px; margin-bottom: 8px; }
  h2 { font-size: 18px; margin: 24px 0 12px; padding-bottom: 8px; border-bottom: 2px solid #e0e0e0; }
  .meta { color: #666; font-size: 14px; margin-bottom: 24px; }
  .summary-cards { display: flex; gap: 16px; margin-bottom: 24px; flex-wrap: wrap; }
  .summary-card { flex: 1; min-width: 150px; padding: 16px; border-radius: 8px; background: white; border: 1px solid #e0e0e0; text-align: center; }
  .summary-card .num { font-size: 28px; font-weight: 700; }
  .summary-card .label { font-size: 12px; color: #666; margin-top: 4px; }
  .match { color: #16a34a; }
  .warn { color: #f59e0b; }
  .err { color: #dc2626; }
  table { width: 100%; border-collapse: collapse; background: white; border-radius: 8px; overflow: hidden; margin-bottom: 24px; border: 1px solid #e0e0e0; }
  th { background: #f1f5f9; text-align: left; padding: 10px 12px; font-size: 12px; font-weight: 600; color: #475569; text-transform: uppercase; }
  td { padding: 10px 12px; font-size: 13px; border-top: 1px solid #f1f5f9; }
  tr:hover { background: #f8fafc; }
  .status-match { background: #dcfce7; color: #16a34a; padding: 2px 8px; border-radius: 12px; font-size: 11px; font-weight: 600; }
  .status-warn { background: #fef3c7; color: #d97706; padding: 2px 8px; border-radius: 12px; font-size: 11px; font-weight: 600; }
  .status-err { background: #fecaca; color: #dc2626; padding: 2px 8px; border-radius: 12px; font-size: 11px; font-weight: 600; }
  .section-summary { font-size: 13px; color: #666; margin-bottom: 16px; }
  .text-right { text-align: right; }
</style>
</head>
<body>
<div class="container">
  <h1>SellerX Dogrulama Raporu</h1>
  <div class="meta">${report.timestamp} | ${report.storeName} (${report.storeEmail})</div>

  <div class="summary-cards">
    <div class="summary-card">
      <div class="num">${totalChecks}</div>
      <div class="label">Toplam Kontrol</div>
    </div>
    <div class="summary-card">
      <div class="num match">${totalMatch} (${matchPct}%)</div>
      <div class="label">Eslesen</div>
    </div>
    <div class="summary-card">
      <div class="num warn">${totalWarn}</div>
      <div class="label">Uyari</div>
    </div>
    <div class="summary-card">
      <div class="num err">${totalErr}</div>
      <div class="label">Hata</div>
    </div>
  </div>
`;

  for (const section of report.sections) {
    html += `  <h2>${section.title}</h2>\n`;
    html += `  <div class="section-summary">${section.summary.matching}/${section.summary.total} eslesen`;
    if (section.summary.warnings > 0)
      html += `, ${section.summary.warnings} uyari`;
    if (section.summary.errors > 0)
      html += `, ${section.summary.errors} hata`;
    html += `</div>\n`;

    html += `  <table>
    <thead>
      <tr>
        <th>#</th>
        <th>Alan</th>
        <th class="text-right">DB</th>
        <th class="text-right">SellerX</th>
        <th class="text-right">Trendyol</th>
        <th class="text-right">Sapma</th>
        <th>Sonuc</th>
      </tr>
    </thead>
    <tbody>\n`;

    section.summary.results.forEach((r, i) => {
      const statusClass = r.match
        ? "status-match"
        : r.severity === "warning"
          ? "status-warn"
          : "status-err";
      const statusText = r.match ? "OK" : r.severity === "warning" ? "UYARI" : "HATA";

      html += `      <tr>
        <td>${i + 1}</td>
        <td>${r.field}${r.note ? `<br><small style="color:#999">${r.note}</small>` : ""}</td>
        <td class="text-right">${formatVal(r.dbValue)}</td>
        <td class="text-right">${formatVal(r.frontendValue)}</td>
        <td class="text-right">${formatVal(r.trendyolValue)}</td>
        <td class="text-right">${r.deviation !== null && r.deviation > 0 ? `%${r.deviation}` : "-"}</td>
        <td><span class="${statusClass}">${statusText}</span></td>
      </tr>\n`;
    });

    html += `    </tbody>\n  </table>\n`;
  }

  html += `</div>\n</body>\n</html>`;
  return html;
}

// ─── Write Report Files ────────────────────────────────────────

export function writeReport(report: VerificationReport): string {
  const dir = getOrInitReportDir();

  // Markdown
  const md = generateMarkdownReport(report);
  fs.writeFileSync(path.join(dir, "report.md"), md, "utf-8");

  // HTML
  const html = generateHtmlReport(report);
  fs.writeFileSync(path.join(dir, "report.html"), html, "utf-8");

  // Raw JSON
  fs.writeFileSync(
    path.join(dir, "raw-data.json"),
    JSON.stringify(report, null, 2),
    "utf-8"
  );

  console.log(`\nVerification report written to: ${dir}`);
  return dir;
}

/**
 * Append a section to an existing report or create new.
 */
export function appendSection(
  section: ReportSection,
  reportPath?: string
): void {
  const dir = reportPath || getOrInitReportDir();
  const jsonPath = path.join(dir, "raw-data.json");

  let report: VerificationReport;

  if (fs.existsSync(jsonPath)) {
    report = JSON.parse(fs.readFileSync(jsonPath, "utf-8"));
    report.sections.push(section);
  } else {
    report = {
      storeName: "Test Magazasi",
      storeEmail: "test@test.com",
      timestamp: new Date().toLocaleString("tr-TR"),
      sections: [section],
    };
  }

  // Rewrite all files
  fs.writeFileSync(jsonPath, JSON.stringify(report, null, 2), "utf-8");
  fs.writeFileSync(
    path.join(dir, "report.md"),
    generateMarkdownReport(report),
    "utf-8"
  );
  fs.writeFileSync(
    path.join(dir, "report.html"),
    generateHtmlReport(report),
    "utf-8"
  );
}
