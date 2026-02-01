import { NextRequest, NextResponse } from "next/server";
import { cookies } from "next/headers";

const API_BASE_URL = process.env.API_BASE_URL;
const isDev = process.env.NODE_ENV === "development";

/**
 * GET /api/invoices/stores/[storeId]/category/[category]/products
 *
 * Get aggregated products by category and date range.
 * Used for "Ürünler" tab when KARGO or KOMISYON category is selected.
 *
 * Path params:
 * - storeId: Store UUID
 * - category: Category code (KARGO or KOMISYON)
 *
 * Query params:
 * - startDate: Start date (YYYY-MM-DD)
 * - endDate: End date (YYYY-MM-DD)
 */
export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ storeId: string; category: string }> }
) {
  try {
    const { storeId, category } = await params;
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("access_token")?.value;

    if (!accessToken) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    // Validate category
    if (category !== "KARGO" && category !== "KOMISYON") {
      return NextResponse.json(
        { error: "Invalid category. Must be KARGO or KOMISYON" },
        { status: 400 }
      );
    }

    // Get query params
    const { searchParams } = new URL(request.url);
    const startDate = searchParams.get("startDate");
    const endDate = searchParams.get("endDate");

    // Build query string
    const queryParams = new URLSearchParams();
    if (startDate) queryParams.append("startDate", startDate);
    if (endDate) queryParams.append("endDate", endDate);
    const queryString = queryParams.toString();

    const response = await fetch(
      `${API_BASE_URL}/api/invoices/stores/${storeId}/category/${category}/products${queryString ? `?${queryString}` : ""}`,
      {
        method: "GET",
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "application/json",
        },
      }
    );

    if (!response.ok) {
      if (response.status === 401) {
        return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
      }
      const errorText = await response.text();
      return NextResponse.json(
        { error: errorText || `HTTP ${response.status}` },
        { status: response.status }
      );
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    if (isDev) console.error("[API] GET /invoices/stores/[storeId]/category/[category]/products error:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}
