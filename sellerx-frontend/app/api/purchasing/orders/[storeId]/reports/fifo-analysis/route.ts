import { NextRequest, NextResponse } from "next/server";
import { cookies } from "next/headers";

const API_BASE_URL = process.env.API_BASE_URL;
const isDev = process.env.NODE_ENV === "development";

// GET /api/purchasing/orders/[storeId]/reports/fifo-analysis?barcode=xxx&startDate=xxx&endDate=xxx
// Get FIFO analysis for a product
export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ storeId: string }> }
) {
  try {
    const { storeId } = await params;
    const { searchParams } = new URL(request.url);
    const barcode = searchParams.get("barcode");
    const startDate = searchParams.get("startDate");
    const endDate = searchParams.get("endDate");

    if (!barcode || !startDate || !endDate) {
      return NextResponse.json(
        { error: "Missing required parameters: barcode, startDate, endDate" },
        { status: 400 }
      );
    }

    const cookieStore = await cookies();
    const accessToken = cookieStore.get("access_token")?.value;

    if (!accessToken) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const response = await fetch(
      `${API_BASE_URL}/api/stores/${storeId}/purchase-orders/reports/fifo-analysis?barcode=${encodeURIComponent(barcode)}&startDate=${startDate}&endDate=${endDate}`,
      {
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
      if (response.status === 404) {
        return NextResponse.json({ error: "Product not found" }, { status: 404 });
      }
      throw new Error(`HTTP ${response.status}`);
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    if (isDev) console.error("[API] /purchasing/reports/fifo-analysis GET error:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}
