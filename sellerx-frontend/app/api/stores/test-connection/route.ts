import { NextRequest, NextResponse } from "next/server";
import { cookies } from "next/headers";

const API_BASE_URL = process.env.API_BASE_URL;

interface TrendyolTestResponse {
  sellerId: string;
  marketplace: string;
  statusCode: number;
  message: string;
  storeId: string;
  connected: boolean;
  storeName: string;
}

export async function GET() {
  try {
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("access_token")?.value;

    if (!accessToken) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    // console.log("accessToken:", accessToken);

    // Call your backend test endpoint with proper authorization
    const response = await fetch(`${API_BASE_URL}/trendyol/test-connection`, {
      method: "GET",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
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
    console.error("Test connection error:", error);
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
