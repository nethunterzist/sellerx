import { NextRequest, NextResponse } from "next/server";
import { cookies } from "next/headers";

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

    const cookieStore = await cookies();
    const accessToken = cookieStore.get("access_token")?.value;

    if (!accessToken) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    // Get query parameters
    const { searchParams } = new URL(request.url);
    const periodType = searchParams.get("periodType") || "monthly";
    const periodCount = searchParams.get("periodCount") || "12";
    const productBarcode = searchParams.get("productBarcode");

    // Build backend URL with query params
    const backendParams = new URLSearchParams({ periodType, periodCount });
    if (productBarcode) {
      backendParams.append("productBarcode", productBarcode);
    }

    const response = await fetch(
      `${API_BASE_URL}/dashboard/stats/${id}/multi-period?${backendParams.toString()}`,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "application/json",
        },
      },
    );

    if (!response.ok) {
      if (response.status === 401) {
        return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
      }
      if (response.status === 400) {
        return NextResponse.json(
          { error: "Invalid parameters" },
          { status: 400 },
        );
      }
      // Return the actual error from backend
      const errorText = await response.text();
      if (isDev) console.error(`[API] Backend error for /dashboard/stats/${id}/multi-period:`, response.status, errorText);
      return NextResponse.json(
        { error: `Backend error: ${response.status}` },
        { status: response.status },
      );
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    if (isDev) console.error(`[API] /dashboard/stats/${storeId}/multi-period error:`, error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 },
    );
  }
}
