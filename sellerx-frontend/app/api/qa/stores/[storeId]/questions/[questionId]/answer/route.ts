import { NextRequest, NextResponse } from "next/server";
import { getBackendHeaders } from "@/lib/api/bff-auth";

const API_BASE_URL = process.env.API_BASE_URL || "http://localhost:8080";

export async function POST(
  request: NextRequest,
  { params }: { params: Promise<{ storeId: string; questionId: string }> }
) {
  const { storeId, questionId } = await params;
  const headers = await getBackendHeaders(request);

    if (!headers.Authorization) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  const body = await request.json();

  const response = await fetch(
    `${API_BASE_URL}/qa/stores/${storeId}/questions/${questionId}/answer`,
    {
      method: "POST",
      headers,
      body: JSON.stringify(body),
    }
  );

  if (!response.ok) {
    if (response.status === 401) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }
    const errorText = await response.text();
    return NextResponse.json(
      { error: errorText || "Failed to submit answer" },
      { status: response.status }
    );
  }

  return NextResponse.json({ success: true });
}
