import { NextRequest, NextResponse } from "next/server";
import { cookies } from "next/headers";

const API_BASE_URL = process.env.API_BASE_URL;
const isDev = process.env.NODE_ENV === "development";

// GET /api/purchasing/orders/[storeId]/[poId]/export - Export PO to Excel
export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ storeId: string; poId: string }> }
) {
  try {
    const { storeId, poId } = await params;
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("access_token")?.value;

    if (!accessToken) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const response = await fetch(
      `${API_BASE_URL}/api/stores/${storeId}/purchase-orders/${poId}/export`,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      }
    );

    if (!response.ok) {
      if (response.status === 401) {
        return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
      }
      throw new Error(`HTTP ${response.status}`);
    }

    const contentDisposition = response.headers.get("content-disposition");
    const contentType = response.headers.get("content-type") || "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    const blob = await response.arrayBuffer();

    const headers: Record<string, string> = {
      "Content-Type": contentType,
    };
    if (contentDisposition) {
      headers["Content-Disposition"] = contentDisposition;
    }

    return new NextResponse(blob, { headers });
  } catch (error) {
    if (isDev) console.error("[API] /purchasing/orders/export GET error:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}
