import { NextRequest, NextResponse } from "next/server";
import { cookies } from "next/headers";

const API_BASE_URL = process.env.API_BASE_URL;
const isDev = process.env.NODE_ENV === "development";

// GET /api/purchasing/orders/[storeId]/[poId]/attachments/[attachmentId]/download
export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ storeId: string; poId: string; attachmentId: string }> }
) {
  try {
    const { storeId, poId, attachmentId } = await params;
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("access_token")?.value;

    if (!accessToken) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const response = await fetch(
      `${API_BASE_URL}/api/stores/${storeId}/purchase-orders/${poId}/attachments/${attachmentId}/download`,
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
      if (response.status === 404) {
        return NextResponse.json({ error: "Not found" }, { status: 404 });
      }
      throw new Error(`HTTP ${response.status}`);
    }

    const contentDisposition = response.headers.get("content-disposition");
    const contentType = response.headers.get("content-type") || "application/octet-stream";
    const blob = await response.arrayBuffer();

    const headers: Record<string, string> = {
      "Content-Type": contentType,
    };
    if (contentDisposition) {
      headers["Content-Disposition"] = contentDisposition;
    }

    return new NextResponse(blob, { headers });
  } catch (error) {
    if (isDev) console.error("[API] /attachments/[attachmentId] GET error:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}

// DELETE /api/purchasing/orders/[storeId]/[poId]/attachments/[attachmentId]
export async function DELETE(
  request: NextRequest,
  { params }: { params: Promise<{ storeId: string; poId: string; attachmentId: string }> }
) {
  try {
    const { storeId, poId, attachmentId } = await params;
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("access_token")?.value;

    if (!accessToken) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const response = await fetch(
      `${API_BASE_URL}/api/stores/${storeId}/purchase-orders/${poId}/attachments/${attachmentId}`,
      {
        method: "DELETE",
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

    return new NextResponse(null, { status: 204 });
  } catch (error) {
    if (isDev) console.error("[API] /attachments/[attachmentId] DELETE error:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}
