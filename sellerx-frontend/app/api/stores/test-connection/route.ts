import { NextRequest, NextResponse } from "next/server";
import { getBackendHeaders } from "@/lib/api/bff-auth";

const API_BASE_URL = process.env.API_BASE_URL;
const isDev = process.env.NODE_ENV === "development";

interface TrendyolTestResponse {
  sellerId: string;
  marketplace: string;
  statusCode: number;
  message: string;
  storeId: string;
  connected: boolean;
  storeName: string;
}

export async function GET(request: NextRequest) {
  try {
    const headers = await getBackendHeaders(request);

    if (!headers.Authorization) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    // console.log("accessToken:", accessToken);

    // Call your backend test endpoint with proper authorization
    const response = await fetch(`${API_BASE_URL}/trendyol/test-connection`, {
      method: "GET",
      headers,
    });

    if (!response.ok) {
      if (response.status === 401) {
        return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
      }
      throw new Error(`HTTP ${response.status}`);
    }

    const data: TrendyolTestResponse = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    if (isDev) console.error("Test connection error:", error);
    return NextResponse.json(
      {
        error: "Failed to test connection",
        message: error instanceof Error ? error.message : "Unknown error",
        connected: false,
      },
      { status: 500 },
    );
  }
}
