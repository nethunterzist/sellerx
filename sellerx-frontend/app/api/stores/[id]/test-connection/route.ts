import { NextRequest, NextResponse } from "next/server";

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

    // Call your backend test endpoint
    const response = await fetch(
      `${process.env.API_BASE_URL}/trendyol/test-connection`,
      {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          // Add any auth headers if needed
        },
      },
    );

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
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
