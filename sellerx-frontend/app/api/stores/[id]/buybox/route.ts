import { NextRequest, NextResponse } from "next/server";
import { getBackendHeaders } from "@/lib/api/bff-auth";

const API_BASE_URL = process.env.API_BASE_URL;
const isDev = process.env.NODE_ENV === "development";

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const { id: storeId } = await params;
    const { searchParams } = new URL(request.url);

    const headers = await getBackendHeaders(request);
    if (!headers.Authorization) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const backendParams = new URLSearchParams();
    backendParams.set("page", searchParams.get("page") || "0");
    backendParams.set("size", searchParams.get("size") || "20");
    backendParams.set("sortBy", searchParams.get("sortBy") || "buyboxOrder");
    backendParams.set("sortDir", searchParams.get("sortDir") || "asc");

    const optionalParams = ["search", "status"];
    optionalParams.forEach((param) => {
      const value = searchParams.get(param);
      if (value) backendParams.set(param, value);
    });

    const response = await fetch(
      `${API_BASE_URL}/api/stores/${storeId}/buybox?${backendParams.toString()}`,
      { headers }
    );

    if (!response.ok) {
      if (response.status === 401) {
        return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
      }
      throw new Error(`HTTP ${response.status}`);
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    if (isDev) console.error("[API] /buybox GET error:", error);
    return NextResponse.json({ error: "Internal server error" }, { status: 500 });
  }
}
