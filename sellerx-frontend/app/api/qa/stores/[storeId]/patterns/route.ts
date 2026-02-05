// app/api/qa/stores/[storeId]/patterns/route.ts
import { NextRequest, NextResponse } from "next/server";
import { getBackendHeaders } from "@/lib/api/bff-auth";

const API_BASE_URL = process.env.API_BASE_URL;

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

    // Get query parameters
    const { searchParams } = new URL(request.url);
    const seniorityLevel = searchParams.get("seniorityLevel");
    const autoSubmitOnly = searchParams.get("autoSubmitOnly");

    // Build query string
    const queryParams = new URLSearchParams();
    if (seniorityLevel) {
      queryParams.set("seniorityLevel", seniorityLevel);
    }
    if (autoSubmitOnly) {
      queryParams.set("autoSubmitOnly", autoSubmitOnly);
    }

    const url = `${API_BASE_URL}/qa/stores/${storeId}/patterns${queryParams.toString() ? '?' + queryParams.toString() : ''}`;

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
    console.error("[API] /qa/stores/[storeId]/patterns error:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}
