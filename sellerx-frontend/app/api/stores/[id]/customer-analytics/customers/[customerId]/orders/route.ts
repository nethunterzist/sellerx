import { NextRequest, NextResponse } from "next/server";
import { getBackendHeaders } from "@/lib/api/bff-auth";

const API_BASE_URL = process.env.API_BASE_URL;
const isDev = process.env.NODE_ENV === "development";

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ id: string; customerId: string }> }
) {
  try {
    const { id: storeId, customerId } = await params;

    const headers = await getBackendHeaders(request);

    if (!headers.Authorization) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    // Forward pagination params if present
    const { searchParams } = new URL(request.url);
    const page = searchParams.get("page");
    const size = searchParams.get("size");

    let url = `${API_BASE_URL}/api/stores/${storeId}/customer-analytics/customers/${customerId}/orders`;
    if (page !== null || size !== null) {
      const queryParams = new URLSearchParams();
      if (page !== null) queryParams.set("page", page);
      if (size !== null) queryParams.set("size", size);
      url += `?${queryParams.toString()}`;
    }

    const response = await fetch(url, {
      headers,
    });

    if (!response.ok) {
      if (response.status === 401) {
        return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
      }
      throw new Error(`HTTP ${response.status}`);
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    if (isDev) console.error("[API] /customer-analytics/customers/orders GET error:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}
