import { NextRequest, NextResponse } from "next/server";
import { getBackendHeaders } from "@/lib/api/bff-auth";

const API_BASE_URL = process.env.API_BASE_URL;
const isDev = process.env.NODE_ENV === "development";

/**
 * GET /api/stores/[id]/customer-analytics/products/[barcode]/buyers
 *
 * Get paginated buyers for a specific product (barcode).
 *
 * Query params:
 * - page: Page number (0-indexed, default: 0)
 * - size: Page size (default: 20)
 */
export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ id: string; barcode: string }> }
) {
  try {
    const { id: storeId, barcode } = await params;

    const headers = await getBackendHeaders(request);

    if (!headers.Authorization) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    // Get query params
    const { searchParams } = new URL(request.url);
    const page = searchParams.get("page") || "0";
    const size = searchParams.get("size") || "20";

    const response = await fetch(
      `${API_BASE_URL}/api/stores/${storeId}/customer-analytics/products/${encodeURIComponent(barcode)}/buyers?page=${page}&size=${size}`,
      {
        headers,
      }
    );

    if (!response.ok) {
      if (response.status === 401) {
        return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
      }
      throw new Error(`HTTP ${response.status}`);
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    if (isDev) console.error("[API] /customer-analytics/products/[barcode]/buyers GET error:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}
