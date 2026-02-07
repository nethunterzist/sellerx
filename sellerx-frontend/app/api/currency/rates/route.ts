import { NextResponse } from "next/server";

const API_BASE_URL = process.env.API_BASE_URL;

/**
 * GET /api/currency/rates
 * Proxy to backend currency rates endpoint.
 * Returns: { USD_TRY, EUR_TRY, TRY_USD, TRY_EUR }
 */
export async function GET() {
  if (!API_BASE_URL) {
    // Return fallback rates if API is not configured
    return NextResponse.json({
      USD_TRY: 34.5,
      EUR_TRY: 37.2,
      TRY_USD: 0.029,
      TRY_EUR: 0.027,
    });
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
      // Return fallback rates on error
      return NextResponse.json({
        USD_TRY: 34.5,
        EUR_TRY: 37.2,
        TRY_USD: 0.029,
        TRY_EUR: 0.027,
      });
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error("Currency rates fetch error:", error);
    // Return fallback rates on network error
    return NextResponse.json({
      USD_TRY: 34.5,
      EUR_TRY: 37.2,
      TRY_USD: 0.029,
      TRY_EUR: 0.027,
    });
  }
}
