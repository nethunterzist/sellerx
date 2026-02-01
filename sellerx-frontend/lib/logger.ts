/**
 * Structured Logger Utility
 * - Development: logs to console with structured format and timestamps
 * - Production: silent (no logging)
 *
 * Usage:
 *   import { logger } from "@/lib/logger";
 *   logger.error("Failed to fetch orders", { endpoint: "/api/orders", status: 500 });
 *   logger.info("Store selected", { storeId: "abc123" });
 *   logger.warn("Token expiring soon", { userId: "user1" });
 *   logger.debug("Cache hit", { key: "dashboard-stats" });
 */

type LogLevel = "error" | "warn" | "info" | "debug";

interface LogContext {
  endpoint?: string;
  userId?: string;
  storeId?: string;
  status?: number;
  method?: string;
  error?: unknown;
  [key: string]: unknown;
}

const isDevelopment = process.env.NODE_ENV === "development";

const LOG_LEVEL_PRIORITY: Record<LogLevel, number> = {
  error: 0,
  warn: 1,
  info: 2,
  debug: 3,
};

// In development, show all levels. In production, show nothing.
const ACTIVE_LEVEL: LogLevel = "debug";

function shouldLog(level: LogLevel): boolean {
  if (!isDevelopment) return false;
  return LOG_LEVEL_PRIORITY[level] <= LOG_LEVEL_PRIORITY[ACTIVE_LEVEL];
}

function formatTimestamp(): string {
  return new Date().toISOString().split("T")[1].split(".")[0];
}

function formatContext(context?: LogContext): string {
  if (!context) return "";
  const parts: string[] = [];
  if (context.method) parts.push(`method=${context.method}`);
  if (context.endpoint) parts.push(`endpoint=${context.endpoint}`);
  if (context.status) parts.push(`status=${context.status}`);
  if (context.storeId) parts.push(`storeId=${context.storeId}`);
  if (context.userId) parts.push(`userId=${context.userId}`);

  // Include any extra keys not in the standard set
  const standardKeys = new Set(["method", "endpoint", "status", "storeId", "userId", "error"]);
  for (const [key, value] of Object.entries(context)) {
    if (!standardKeys.has(key) && value !== undefined && value !== null) {
      parts.push(`${key}=${typeof value === "object" ? JSON.stringify(value) : value}`);
    }
  }

  return parts.length > 0 ? ` ${parts.join(", ")}` : "";
}

function formatError(context?: LogContext): unknown[] {
  if (!context?.error) return [];
  return [context.error];
}

function log(level: LogLevel, message: string, context?: LogContext): void {
  if (!shouldLog(level)) return;

  const timestamp = formatTimestamp();
  const prefix = `${timestamp} [${level.toUpperCase()}]`;
  const contextStr = formatContext(context);
  const formatted = `${prefix} ${message}${contextStr}`;
  const errorArgs = formatError(context);

  switch (level) {
    case "error":
      // eslint-disable-next-line no-console
      console.error(formatted, ...errorArgs);
      break;
    case "warn":
      // eslint-disable-next-line no-console
      console.warn(formatted, ...errorArgs);
      break;
    case "info":
      // eslint-disable-next-line no-console
      console.log(formatted, ...errorArgs);
      break;
    case "debug":
      // eslint-disable-next-line no-console
      console.log(formatted, ...errorArgs);
      break;
  }
}

export const logger = {
  /**
   * Log errors - failed operations, exceptions, critical issues
   */
  error: (message: string, context?: LogContext) => log("error", message, context),

  /**
   * Log warnings - degraded operations, unexpected but recoverable situations
   */
  warn: (message: string, context?: LogContext) => log("warn", message, context),

  /**
   * Log info - successful operations, state changes, key events
   */
  info: (message: string, context?: LogContext) => log("info", message, context),

  /**
   * Log debug - detailed diagnostic information for development
   */
  debug: (message: string, context?: LogContext) => log("debug", message, context),
};

export default logger;
