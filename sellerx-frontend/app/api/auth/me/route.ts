import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";

export async function GET(req: NextRequest) {
  const cookieStore = await cookies();
  const token = cookieStore.get("access_token")?.value;
  if (!token) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }
  const backendRes = await fetch(`${process.env.API_BASE_URL}/auth/me`, {
    headers: { Authorization: `Bearer ${token}` },
    credentials: "include",
  });

  const data = await backendRes.json();
  return NextResponse.json(data, { status: backendRes.status });
}
