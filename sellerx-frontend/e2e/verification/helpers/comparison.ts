/**
 * Triple comparison engine for DB vs SellerX vs Trendyol values.
 */

export type Severity = "critical" | "warning" | "info";

export interface ComparisonResult {
  field: string;
  match: boolean;
  dbValue: string | number | null;
  frontendValue: string | number | null;
  trendyolValue: string | number | null;
  deviation: number | null; // percentage deviation
  severity: Severity;
  note?: string;
}

export interface ComparisonSummary {
  total: number;
  matching: number;
  warnings: number;
  errors: number;
  results: ComparisonResult[];
}

// ─── Tolerances ────────────────────────────────────────────────

const MONEY_TOLERANCE = 0.01; // TL (kurus yuvarlama)
const PERCENT_TOLERANCE = 0.1; // percentage points
const COUNT_TOLERANCE = 0; // exact match

// ─── Core comparison ───────────────────────────────────────────

function parseNum(val: string | number | null | undefined): number | null {
  if (val === null || val === undefined || val === "" || val === "-") return null;
  if (typeof val === "number") return val;
  // Remove currency symbols, thousands separators, whitespace
  const cleaned = val
    .replace(/[₺$€]/g, "")
    .replace(/\./g, "") // Turkish thousands separator
    .replace(",", ".") // Turkish decimal separator
    .replace(/\s/g, "")
    .replace(/%/g, "")
    .trim();
  const num = parseFloat(cleaned);
  return isNaN(num) ? null : num;
}

type ValueType = "money" | "percent" | "count";

function getTolerance(type: ValueType): number {
  switch (type) {
    case "money":
      return MONEY_TOLERANCE;
    case "percent":
      return PERCENT_TOLERANCE;
    case "count":
      return COUNT_TOLERANCE;
  }
}

function calcDeviation(a: number, b: number): number | null {
  if (a === 0 && b === 0) return 0;
  if (a === 0) return b === 0 ? 0 : 100;
  return Math.abs((b - a) / a) * 100;
}

function determineSeverity(
  deviation: number | null,
  type: ValueType
): Severity {
  if (deviation === null) return "info";
  if (deviation === 0) return "info";

  const tolerance = getTolerance(type);
  if (type === "money") {
    // For money, deviation is in percentage; use absolute diff too
    if (deviation <= 0.01) return "info";
    if (deviation <= 1) return "warning";
    return "critical";
  }
  if (type === "count") {
    return deviation === 0 ? "info" : "critical";
  }
  // percent
  if (deviation <= tolerance) return "info";
  if (deviation <= 1) return "warning";
  return "critical";
}

/**
 * Compare a single field across three sources.
 */
export function compareField(
  field: string,
  dbValue: string | number | null,
  frontendValue: string | number | null,
  trendyolValue: string | number | null,
  type: ValueType = "money"
): ComparisonResult {
  const db = parseNum(dbValue);
  const fe = parseNum(frontendValue);
  const ty = parseNum(trendyolValue);

  // All null = skip
  if (db === null && fe === null && ty === null) {
    return {
      field,
      match: true,
      dbValue,
      frontendValue,
      trendyolValue,
      deviation: null,
      severity: "info",
      note: "All values null/missing",
    };
  }

  const tolerance = getTolerance(type);
  let match = true;
  let maxDeviation = 0;
  const notes: string[] = [];

  // DB vs Frontend
  if (db !== null && fe !== null) {
    const diff = Math.abs(db - fe);
    const dev = calcDeviation(db, fe);
    if (type === "count" ? diff > 0 : diff > tolerance) {
      match = false;
      if (dev !== null) maxDeviation = Math.max(maxDeviation, dev);
      notes.push(`DB vs FE: diff=${diff.toFixed(2)}`);
    }
  }

  // DB vs Trendyol
  if (db !== null && ty !== null) {
    const diff = Math.abs(db - ty);
    const dev = calcDeviation(db, ty);
    if (type === "count" ? diff > 0 : diff > tolerance) {
      match = false;
      if (dev !== null) maxDeviation = Math.max(maxDeviation, dev);
      notes.push(`DB vs TY: diff=${diff.toFixed(2)}`);
    }
  }

  // Frontend vs Trendyol
  if (fe !== null && ty !== null) {
    const diff = Math.abs(fe - ty);
    const dev = calcDeviation(fe, ty);
    if (type === "count" ? diff > 0 : diff > tolerance) {
      match = false;
      if (dev !== null) maxDeviation = Math.max(maxDeviation, dev);
      notes.push(`FE vs TY: diff=${diff.toFixed(2)}`);
    }
  }

  const severity = match
    ? "info"
    : determineSeverity(maxDeviation, type);

  return {
    field,
    match,
    dbValue: db,
    frontendValue: fe,
    trendyolValue: ty,
    deviation: match ? 0 : parseFloat(maxDeviation.toFixed(2)),
    severity,
    note: notes.length > 0 ? notes.join("; ") : undefined,
  };
}

/**
 * Compare a set of fields and return a summary.
 */
export function compareSets(
  results: ComparisonResult[]
): ComparisonSummary {
  const total = results.length;
  const matching = results.filter((r) => r.match).length;
  const warnings = results.filter((r) => r.severity === "warning").length;
  const errors = results.filter((r) => r.severity === "critical").length;

  return { total, matching, warnings, errors, results };
}

/**
 * Shorthand for comparing two values (DB vs Frontend only).
 */
export function compareTwoWay(
  field: string,
  dbValue: string | number | null,
  frontendValue: string | number | null,
  type: ValueType = "money"
): ComparisonResult {
  return compareField(field, dbValue, frontendValue, null, type);
}

/**
 * Format a number as Turkish currency string for display.
 */
export function formatTL(value: number | null): string {
  if (value === null) return "-";
  return new Intl.NumberFormat("tr-TR", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}
