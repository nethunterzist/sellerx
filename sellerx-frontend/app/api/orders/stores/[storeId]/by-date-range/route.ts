import { NextRequest, NextResponse } from "next/server";
import { logger } from "@/lib/logger";
import { getBackendHeaders } from "@/lib/api/bff-auth";

const API_BASE_URL = process.env.API_BASE_URL;

export async function GET(
  request: NextRequest,
  context: { params: Promise<{ storeId: string }> },
) {
  let storeId: string | undefined;

  try {
    const resolvedParams = await context.params;
    storeId = resolvedParams.storeId;

    console.log("[DEBUG] /orders/stores by-date-range - storeId:", storeId);

    const headers = await getBackendHeaders(request);
    console.log("[DEBUG] Authorization:", headers.Authorization ? "EXISTS" : "MISSING");

    if (!headers.Authorization) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    // Get query params
    const { searchParams } = new URL(request.url);
    const startDate = searchParams.get("startDate");
    const endDate = searchParams.get("endDate");
    const page = searchParams.get("page") || "0";
    const size = searchParams.get("size") || "20";

    console.log("[DEBUG] Date params:", { startDate, endDate, page, size });

    if (!startDate || !endDate) {
      return NextResponse.json(
        { error: "startDate and endDate are required" },
        { status: 400 },
      );
    }

    const backendUrl = `${API_BASE_URL}/api/orders/stores/${storeId}/by-date-range?startDate=${encodeURIComponent(startDate)}&endDate=${encodeURIComponent(endDate)}&page=${page}&size=${size}`;
    console.log("[DEBUG] Calling backend:", backendUrl);

    const response = await fetch(backendUrl, { headers });
    console.log("[DEBUG] Backend response status:", response.status);

    if (!response.ok) {
      if (response.status === 401) {
        return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
      }
      const errorText = await response.text();
      console.log("[DEBUG] Backend error text:", errorText);
      logger.error(`Backend error ${response.status}`, { storeId, errorText });
      return NextResponse.json(
        { error: errorText || `Backend error: ${response.status}` },
        { status: response.status },
      );
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error("[ERROR] /orders/stores by-date-range error:", error);
    logger.error(`GET /orders/stores/${storeId}/by-date-range error`, { storeId, error });
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 },
    );
  }
}
