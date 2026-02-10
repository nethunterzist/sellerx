import { NextRequest, NextResponse } from "next/server";
import { logger } from "@/lib/logger";
import { getBackendHeaders } from "@/lib/api/bff-auth";

const API_BASE_URL = process.env.API_BASE_URL;

// GET /api/expenses/store/[storeId] - Get store expenses
export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ storeId: string }> }
) {
  try {
    const { storeId } = await params;
    const headers = await getBackendHeaders(request);

    if (!headers.Authorization) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    // Forward date query params if present
    const searchParams = request.nextUrl.searchParams;
    const startDate = searchParams.get("startDate");
    const endDate = searchParams.get("endDate");

    let url = `${API_BASE_URL}/expenses/store/${storeId}`;
    if (startDate && endDate) {
      url += `?startDate=${encodeURIComponent(startDate)}&endDate=${encodeURIComponent(endDate)}`;
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
    logger.error("GET /expenses/store/[storeId] error", { endpoint: "/expenses/store/[storeId]", error });
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}

// POST /api/expenses/store/[storeId] - Create new expense
export async function POST(
  request: NextRequest,
  { params }: { params: Promise<{ storeId: string }> }
) {
  try {
    const { storeId } = await params;
    const headers = await getBackendHeaders(request);

    if (!headers.Authorization) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const body = await request.json();

    const response = await fetch(`${API_BASE_URL}/expenses/store/${storeId}`, {
      method: "POST",
      headers,
      body: JSON.stringify(body),
    });

    if (!response.ok) {
      if (response.status === 401) {
        return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
      }
      const errorData = await response.text();
      throw new Error(errorData || `HTTP ${response.status}`);
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    logger.error("POST /expenses/store/[storeId] error", { endpoint: "/expenses/store/[storeId]", method: "POST", error });
    return NextResponse.json(
      { error: error instanceof Error ? error.message : "Internal server error" },
      { status: 500 }
    );
  }
}
