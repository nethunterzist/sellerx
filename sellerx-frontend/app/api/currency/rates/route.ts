import { NextResponse } from "next/server";

const API_BASE_URL = process.env.API_BASE_URL;

// Fallback rates when backend is unavailable (approximate rates as of Feb 2026)
const FALLBACK_RATES = {
  USD_TRY: 43.65,
  EUR_TRY: 47.0,
  TRY_USD: 0.0229,
  TRY_EUR: 0.0213,
};

/**
 * GET /api/currency/rates
 * Proxy to backend currency rates endpoint.
 * Returns: { USD_TRY, EUR_TRY, TRY_USD, TRY_EUR }
 */
export async function GET() {
  if (!API_BASE_URL) {
    return NextResponse.json(FALLBACK_RATES);
  }

  try {
    const response = await fetch(`${API_BASE_URL}/currency/rates`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
      // Cache rates for 1 hour on the edge
      next: { revalidate: 3600 },
    });

    if (!response.ok) {
      console.error("Failed to fetch currency rates from backend:", response.status);
      return NextResponse.json(FALLBACK_RATES);
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error("Currency rates fetch error:", error);
    return NextResponse.json(FALLBACK_RATES);
  }
}
