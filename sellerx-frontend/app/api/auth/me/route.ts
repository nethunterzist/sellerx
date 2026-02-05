import { NextRequest, NextResponse } from "next/server";
import { getBackendHeaders } from "@/lib/api/bff-auth";

export async function GET(req: NextRequest) {
  const headers = await getBackendHeaders(req);
  if (!headers.Authorization) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }
  const backendRes = await fetch(`${process.env.API_BASE_URL}/auth/me`, {
    headers,
    credentials: "include",
  });

  const data = await backendRes.json();
  return NextResponse.json(data, { status: backendRes.status });
}
