/**
 * API configuration constants
 */

// Server-side API base URL
export const API_BASE_URL = process.env.API_BASE_URL || "http://localhost:8080";

// Client-side API base URL (requires NEXT_PUBLIC_ prefix)
export const PUBLIC_API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

/**
 * Get the appropriate API base URL based on the environment
 * @param isClient - Whether this is being called from client-side code
 */
export function getApiBaseUrl(isClient = false): string {
  return isClient ? PUBLIC_API_BASE_URL : API_BASE_URL;
}
