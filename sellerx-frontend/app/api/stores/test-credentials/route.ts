import { NextRequest, NextResponse } from "next/server";
import { cookies } from "next/headers";

const API_BASE_URL = process.env.API_BASE_URL;
const isDev = process.env.NODE_ENV === "development";

export async function POST(request: NextRequest) {
  try {
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("access_token")?.value;

    if (!accessToken) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const body = await request.json();
    const { sellerId, apiKey, apiSecret } = body;

    if (!sellerId || !apiKey || !apiSecret) {
      return NextResponse.json(
        { error: "Missing required credentials", connected: false },
        { status: 400 }
      );
    }

    // Call backend to test Trendyol credentials
    const response = await fetch(`${API_BASE_URL}/trendyol/test-credentials`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ sellerId, apiKey, apiSecret }),
    });

    if (!response.ok) {
      if (response.status === 401) {
        return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
      }

      // Try to get error message from response
      const errorData = await response.json().catch(() => ({}));
      return NextResponse.json(
        {
          connected: false,
          error: errorData.message || `Connection failed with status ${response.status}`,
          message: errorData.message || "Could not connect to Trendyol API"
        },
        { status: response.status }
      );
    }

    const data = await response.json();
    return NextResponse.json({
      connected: true,
      sellerId: data.sellerId || sellerId,
      storeName: data.storeName || "Trendyol Store",
      message: "Connection successful!",
      ...data
    });
  } catch (error) {
    if (isDev) console.error("Test credentials error:", error);
    return NextResponse.json(
      {
        connected: false,
        error: "Failed to test credentials",
        message: error instanceof Error ? error.message : "Unknown error",
      },
      { status: 500 }
    );
  }
}
