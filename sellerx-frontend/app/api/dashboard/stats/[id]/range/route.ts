import { NextRequest, NextResponse } from "next/server";
import { getBackendHeaders } from "@/lib/api/bff-auth";

const API_BASE_URL = process.env.API_BASE_URL;
const isDev = process.env.NODE_ENV === "development";

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> },
) {
  let storeId = "unknown";

  try {
    const { id } = await params;
    storeId = id;

    const headers = await getBackendHeaders(request);

    if (!headers.Authorization) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    // Get query parameters
    const { searchParams } = new URL(request.url);
    const startDate = searchParams.get("startDate");
    const endDate = searchParams.get("endDate");
    const periodLabel = searchParams.get("periodLabel");

    // Validate required params
    if (!startDate || !endDate) {
      return NextResponse.json(
        { error: "startDate and endDate are required" },
        { status: 400 },
      );
    }

    // Build backend URL with query params
    const backendParams = new URLSearchParams({ startDate, endDate });
    if (periodLabel) {
      backendParams.append("periodLabel", periodLabel);
    }

    const response = await fetch(
      `${API_BASE_URL}/dashboard/stats/${id}/range?${backendParams.toString()}`,
      {
        headers,
      },
    );

    if (!response.ok) {
      if (response.status === 401) {
        return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
      }
      if (response.status === 400) {
        return NextResponse.json(
          { error: "Invalid date range parameters" },
          { status: 400 },
        );
      }
      // Return the actual error from backend
      const errorText = await response.text();
      if (isDev) console.error(`[API] Backend error for /dashboard/stats/${id}/range:`, response.status, errorText);
      return NextResponse.json(
        { error: `Backend error: ${response.status}` },
        { status: response.status },
      );
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    if (isDev) console.error(`[API] /dashboard/stats/${storeId}/range error:`, error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 },
    );
  }
}
