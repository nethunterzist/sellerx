/**
 * Auth Flow Logger Utility
 * Provides structured logging for authentication flows
 * Helps debug token refresh, auth failures, and session issues
 */

type AuthEvent =
  | "token_refresh_start"
  | "token_refresh_success"
  | "token_refresh_fail"
  | "unauthorized"
  | "redirect_login"
  | "cookie_missing"
  | "cookie_found";

interface AuthLogContext {
  endpoint?: string;
  userId?: string;
  reason?: string;
  status?: number;
}

const isDevelopment = process.env.NODE_ENV === "development";

export const authLogger = {
  /**
   * Log token refresh events
   */
  tokenRefresh: (
    status: "start" | "success" | "fail",
    context?: AuthLogContext
  ) => {
    const event = `token_refresh_${status}` as AuthEvent;
    const message = formatMessage(event, context);

    if (isDevelopment) {
      if (status === "fail") {
        console.error(message);
      } else {
        console.log(message);
      }
    }
  },

  /**
   * Log 401 Unauthorized responses
   */
  unauthorized: (endpoint: string, status?: number) => {
    if (isDevelopment) {
      console.warn(
        formatMessage("unauthorized", { endpoint, status })
      );
    }
  },

  /**
   * Log redirects to login page
   */
  redirectToLogin: (reason: string, endpoint?: string) => {
    if (isDevelopment) {
      console.log(
        formatMessage("redirect_login", { reason, endpoint })
      );
    }
  },

  /**
   * Log cookie check results (middleware)
   */
  cookieCheck: (found: boolean, pathname: string) => {
    if (isDevelopment) {
      const event = found ? "cookie_found" : "cookie_missing";
      console.log(formatMessage(event, { endpoint: pathname }));
    }
  },

  /**
   * Generic debug log (development only)
   */
  debug: (message: string, data?: Record<string, unknown>) => {
    if (isDevelopment) {
      console.log(`[AUTH:DEBUG] ${message}`, data || "");
    }
  },
};

function formatMessage(event: AuthEvent, context?: AuthLogContext): string {
  const timestamp = new Date().toISOString().split("T")[1].split(".")[0];
  const prefix = `[AUTH:${event.toUpperCase()}]`;

  let details = "";
  if (context) {
    const parts: string[] = [];
    if (context.endpoint) parts.push(`endpoint=${context.endpoint}`);
    if (context.status) parts.push(`status=${context.status}`);
    if (context.reason) parts.push(`reason=${context.reason}`);
    if (context.userId) parts.push(`user=${context.userId}`);
    details = parts.join(", ");
  }

  return `${timestamp} ${prefix}${details ? ` ${details}` : ""}`;
}

export default authLogger;
