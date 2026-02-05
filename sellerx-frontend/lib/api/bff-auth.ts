import { cookies } from "next/headers";
import { NextRequest } from "next/server";

/**
 * Get the Bearer token for backend API calls from a BFF route.
 *
 * Priority:
 * 1. X-Impersonation-Token header (impersonated sessions) — the impersonation
 *    JWT contains the target user's identity with readOnly claims.
 * 2. access_token cookie (normal sessions).
 *
 * Returns null if neither is available.
 */
export async function getBackendToken(request: NextRequest): Promise<string | null> {
  // Check for impersonation token first
  const impersonationToken = request.headers.get("x-impersonation-token");
  if (impersonationToken) {
    return impersonationToken;
  }

  // Fall back to cookie
  const cookieStore = await cookies();
  return cookieStore.get("access_token")?.value ?? null;
}

/**
 * Build Authorization headers for backend API calls.
 * Includes Content-Type: application/json by default.
 */
export async function getBackendHeaders(
  request: NextRequest,
  contentType = "application/json",
): Promise<Record<string, string>> {
  const token = await getBackendToken(request);
  const headers: Record<string, string> = {};

  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }
  if (contentType) {
    headers["Content-Type"] = contentType;
  }

  return headers;
}
