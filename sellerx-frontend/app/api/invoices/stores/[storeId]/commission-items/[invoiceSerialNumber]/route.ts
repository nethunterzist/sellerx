import { NextRequest, NextResponse } from "next/server";
import { getBackendHeaders } from "@/lib/api/bff-auth";

const API_BASE_URL = process.env.API_BASE_URL;
const isDev = process.env.NODE_ENV === "development";

/**
 * GET /api/invoices/stores/[storeId]/commission-items/[invoiceSerialNumber]
 * Fetches commission invoice items for a specific invoice serial number.
 * Returns order-level commission breakdown (similar to Trendyol Excel export).
 */
export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ storeId: string; invoiceSerialNumber: string }> }
) {
  try {
    const { storeId, invoiceSerialNumber } = await params;
    const headers = await getBackendHeaders(request);

    if (!headers.Authorization) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const response = await fetch(
      `${API_BASE_URL}/api/invoices/stores/${storeId}/commission-items/${invoiceSerialNumber}`,
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
    if (isDev) console.error("[API] GET /invoices/stores/[storeId]/commission-items/[invoiceSerialNumber] error:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}
