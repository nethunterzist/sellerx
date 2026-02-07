/**
 * API configuration constants
 */

// Server-side API base URL - NO FALLBACK for production safety
export const API_BASE_URL = process.env.API_BASE_URL;

// Client-side API base URL (requires NEXT_PUBLIC_ prefix)
export const PUBLIC_API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL;

/**
 * Validates that API_BASE_URL is configured
 * Call this at the start of API routes to fail fast if not configured
 */
export function validateApiConfig(): { valid: true } | { valid: false; error: string } {
  if (!API_BASE_URL) {
    console.error("API_BASE_URL environment variable is not configured");
    return { valid: false, error: "Server configuration error: API_BASE_URL not set" };
  }
  return { valid: true };
}

/**
 * Get the appropriate API base URL based on the environment
 * @param isClient - Whether this is being called from client-side code
 * @returns The API base URL or undefined if not configured
 */
export function getApiBaseUrl(isClient = false): string | undefined {
  return isClient ? PUBLIC_API_BASE_URL : API_BASE_URL;
}

/**
 * Get the API base URL with validation - throws if not configured
 * Use this in API routes where the URL is required
 */
export function getRequiredApiBaseUrl(): string {
  if (!API_BASE_URL) {
    throw new Error("API_BASE_URL environment variable is required but not set");
  }
  return API_BASE_URL;
}
