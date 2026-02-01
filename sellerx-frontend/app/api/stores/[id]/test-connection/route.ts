import { NextRequest, NextResponse } from "next/server";
import { cookies } from "next/headers";

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

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> },
) {
  try {
    const { id: storeId } = await params;
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("access_token")?.value;

    if (!accessToken) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    // Call your backend test endpoint
    const response = await fetch(
      `${API_BASE_URL}/stores/${storeId}/test-connection`,
      {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
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
