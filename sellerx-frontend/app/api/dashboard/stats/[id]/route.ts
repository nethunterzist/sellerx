import { NextRequest, NextResponse } from "next/server";
import { logger } from "@/lib/logger";
import { getBackendHeaders } from "@/lib/api/bff-auth";

const API_BASE_URL = process.env.API_BASE_URL;

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> },
) {
  let storeId = "unknown";

  try {
    const { id } = await params;
    storeId = id;

    const headers = await getBackendHeaders(request);

    // DEBUG: Log headers
    console.log(`[DEBUG] /dashboard/stats/${id} - Authorization: ${headers.Authorization ? 'EXISTS' : 'MISSING'}`);

    if (!headers.Authorization) {
      console.log(`[DEBUG] /dashboard/stats/${id} - Returning 401 because no Authorization header`);
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const backendUrl = `${API_BASE_URL}/dashboard/stats/${id}`;
    console.log(`[DEBUG] Calling backend: ${backendUrl}`);
    console.log(`[DEBUG] Headers being sent:`, JSON.stringify(headers, null, 2));

    const response = await fetch(backendUrl, {
      headers,
    });

    console.log(`[DEBUG] Backend response status: ${response.status}`);

    if (!response.ok) {
      const errorText = await response.text();
      console.log(`[DEBUG] Backend error body: ${errorText}`);

      if (response.status === 401) {
        return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
      }
      logger.error(`Backend error for /dashboard/stats/${id}`, { endpoint: `/dashboard/stats/${id}`, status: response.status, storeId: id, backendError: errorText });
      return NextResponse.json(
        { error: `Backend error: ${response.status}` },
        { status: response.status },
      );
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    logger.error(`GET /dashboard/stats/${storeId} error`, { endpoint: `/dashboard/stats/${storeId}`, storeId, error });
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 },
    );
  }
}
