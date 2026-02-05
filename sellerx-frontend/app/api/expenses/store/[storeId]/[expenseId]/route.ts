import { NextRequest, NextResponse } from "next/server";
import { logger } from "@/lib/logger";
import { getBackendHeaders } from "@/lib/api/bff-auth";

const API_BASE_URL = process.env.API_BASE_URL;

// PUT /api/expenses/store/[storeId]/[expenseId] - Update expense
export async function PUT(
  request: NextRequest,
  { params }: { params: Promise<{ storeId: string; expenseId: string }> }
) {
  try {
    const { storeId, expenseId } = await params;
    const headers = await getBackendHeaders(request);

    if (!headers.Authorization) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const body = await request.json();

    const response = await fetch(
      `${API_BASE_URL}/expenses/store/${storeId}/${expenseId}`,
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
      const errorData = await response.text();
      throw new Error(errorData || `HTTP ${response.status}`);
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    logger.error("PUT /expenses/store/[storeId]/[expenseId] error", { endpoint: "/expenses/store/[storeId]/[expenseId]", method: "PUT", error });
    return NextResponse.json(
      { error: error instanceof Error ? error.message : "Internal server error" },
      { status: 500 }
    );
  }
}

// DELETE /api/expenses/store/[storeId]/[expenseId] - Delete expense
export async function DELETE(
  request: NextRequest,
  { params }: { params: Promise<{ storeId: string; expenseId: string }> }
) {
  try {
    const { storeId, expenseId } = await params;
    const headers = await getBackendHeaders(request);

    if (!headers.Authorization) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const response = await fetch(
      `${API_BASE_URL}/expenses/store/${storeId}/${expenseId}`,
      {
        method: "DELETE",
        headers,
      }
    );

    if (!response.ok) {
      if (response.status === 401) {
        return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
      }
      throw new Error(`HTTP ${response.status}`);
    }

    return NextResponse.json({ success: true });
  } catch (error) {
    logger.error("DELETE /expenses/store/[storeId]/[expenseId] error", { endpoint: "/expenses/store/[storeId]/[expenseId]", method: "DELETE", error });
    return NextResponse.json(
      { error: error instanceof Error ? error.message : "Internal server error" },
      { status: 500 }
    );
  }
}
