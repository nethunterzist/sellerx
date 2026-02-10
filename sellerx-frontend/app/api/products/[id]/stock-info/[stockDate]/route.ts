import { NextRequest, NextResponse } from "next/server";
import { logger } from "@/lib/logger";
import { getBackendHeaders } from "@/lib/api/bff-auth";

const API_BASE_URL = process.env.API_BASE_URL;

// PUT - Update stock info by date
export async function PUT(
  request: NextRequest,
  { params }: { params: Promise<{ id: string; stockDate: string }> },
) {
  try {
    const { id, stockDate } = await params;
    const headers = await getBackendHeaders(request);

    if (!headers.Authorization) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const body = await request.json();

    const response = await fetch(
      `${API_BASE_URL}/products/${id}/stock-info/${stockDate}`,
      {
        method: "PUT",
        headers,
        body: JSON.stringify(body),
      },
    );

    if (!response.ok) {
      if (response.status === 401) {
        return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
      }
      const errorData = await response.json().catch(() => ({}));
      return NextResponse.json(
        { error: errorData.message || "Maliyet güncellenemedi" },
        { status: response.status },
      );
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    const { id, stockDate } = await params;
    logger.error(`PUT /products/${id}/stock-info/${stockDate} error`, {
      endpoint: `/products/${id}/stock-info/${stockDate}`,
      method: "PUT",
      error,
    });
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 },
    );
  }
}

// DELETE - Delete stock info by date
export async function DELETE(
  request: NextRequest,
  { params }: { params: Promise<{ id: string; stockDate: string }> },
) {
  try {
    const { id, stockDate } = await params;
    const headers = await getBackendHeaders(request);

    if (!headers.Authorization) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const response = await fetch(
      `${API_BASE_URL}/products/${id}/stock-info/${stockDate}`,
      {
        method: "DELETE",
        headers,
      },
    );

    if (!response.ok) {
      if (response.status === 401) {
        return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
      }
      const errorData = await response.json().catch(() => ({}));
      return NextResponse.json(
        { error: errorData.message || "Maliyet silinemedi" },
        { status: response.status },
      );
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    const { id, stockDate } = await params;
    logger.error(`DELETE /products/${id}/stock-info/${stockDate} error`, {
      endpoint: `/products/${id}/stock-info/${stockDate}`,
      method: "DELETE",
      error,
    });
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 },
    );
  }
}
