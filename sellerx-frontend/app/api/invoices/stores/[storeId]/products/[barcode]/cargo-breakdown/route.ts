import { NextRequest, NextResponse } from "next/server";
import { getBackendHeaders } from "@/lib/api/bff-auth";

const API_BASE_URL = process.env.API_BASE_URL;
const isDev = process.env.NODE_ENV === "development";

/**
 * GET /api/invoices/stores/[storeId]/products/[barcode]/cargo-breakdown
 *
 * Get cargo cost breakdown for a specific product (barcode).
 * Used for "Detay" panel when clicking on a product row in KARGO Ürünler tab.
 *
 * Path params:
 * - storeId: Store UUID
 * - barcode: Product barcode (URL encoded)
 *
 * Query params:
 * - startDate: Start date (YYYY-MM-DD)
 * - endDate: End date (YYYY-MM-DD)
 */
export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ storeId: string; barcode: string }> }
) {
  try {
    const { storeId, barcode } = await params;
    const headers = await getBackendHeaders(request);

    if (!headers.Authorization) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    // Decode barcode in case it's URL encoded
    const decodedBarcode = decodeURIComponent(barcode);

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
      `${API_BASE_URL}/api/invoices/stores/${storeId}/products/${encodeURIComponent(decodedBarcode)}/cargo-breakdown${queryString ? `?${queryString}` : ""}`,
      {
        method: "GET",
        headers,
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
    if (isDev) console.error("[API] GET /invoices/stores/[storeId]/products/[barcode]/cargo-breakdown error:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}
