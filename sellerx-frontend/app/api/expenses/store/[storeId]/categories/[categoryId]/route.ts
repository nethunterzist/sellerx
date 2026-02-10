import { NextRequest, NextResponse } from "next/server";
import { logger } from "@/lib/logger";
import { getBackendHeaders } from "@/lib/api/bff-auth";

const API_BASE_URL = process.env.API_BASE_URL;

// PUT /api/expenses/store/[storeId]/categories/[categoryId] - Update expense category
export async function PUT(
  request: NextRequest,
  { params }: { params: Promise<{ storeId: string; categoryId: string }> }
) {
  try {
    const { storeId, categoryId } = await params;
    const headers = await getBackendHeaders(request);

    if (!headers.Authorization) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const body = await request.json();

    const response = await fetch(
      `${API_BASE_URL}/expenses/store/${storeId}/categories/${categoryId}`,
      {
        method: "PUT",
        headers,
        body: JSON.stringify(body),
      }
    );

    if (!response.ok) {
      if (response.status === 401) {
        return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
      }
      // Handle conflict errors (duplicate category name)
      if (response.status === 409) {
        const errorData = await response.json();
        return NextResponse.json(errorData, { status: 409 });
      }
      if (response.status === 404) {
        return NextResponse.json({ error: "Category not found" }, { status: 404 });
      }
      throw new Error(`HTTP ${response.status}`);
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    logger.error("PUT /expenses/store/[storeId]/categories/[categoryId] error", {
      endpoint: "/expenses/store/[storeId]/categories/[categoryId]",
      method: "PUT",
      error,
    });
    return NextResponse.json(
      { error: error instanceof Error ? error.message : "Internal server error" },
      { status: 500 }
    );
  }
}

// DELETE /api/expenses/store/[storeId]/categories/[categoryId] - Delete expense category
export async function DELETE(
  request: NextRequest,
  { params }: { params: Promise<{ storeId: string; categoryId: string }> }
) {
  try {
    const { storeId, categoryId } = await params;
    const headers = await getBackendHeaders(request);

    if (!headers.Authorization) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const response = await fetch(
      `${API_BASE_URL}/expenses/store/${storeId}/categories/${categoryId}`,
      {
        method: "DELETE",
        headers,
      }
    );

    if (!response.ok) {
      if (response.status === 401) {
        return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
      }
      // Handle conflict errors (category in use)
      if (response.status === 409) {
        const errorData = await response.json();
        return NextResponse.json(errorData, { status: 409 });
      }
      if (response.status === 404) {
        return NextResponse.json({ error: "Category not found" }, { status: 404 });
      }
      throw new Error(`HTTP ${response.status}`);
    }

    return new NextResponse(null, { status: 204 });
  } catch (error) {
    logger.error("DELETE /expenses/store/[storeId]/categories/[categoryId] error", {
      endpoint: "/expenses/store/[storeId]/categories/[categoryId]",
      method: "DELETE",
      error,
    });
    return NextResponse.json(
      { error: error instanceof Error ? error.message : "Internal server error" },
      { status: 500 }
    );
  }
}
