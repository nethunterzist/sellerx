import { NextRequest, NextResponse } from "next/server";

export async function POST(req: NextRequest) {
  // 1. Gelen istekteki cookie'yi alın
  const cookie = req.headers.get("cookie");

  // 2. Backend'e istek atarken cookie'yi header olarak ekleyin
  const backendRes = await fetch(`${process.env.API_BASE_URL}/auth/refresh`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(cookie ? { cookie } : {}),
    },
    credentials: "include",
  });

  const setCookie = backendRes.headers.get("set-cookie");
  const text = await backendRes.text();
  let data;
  try {
    data = text ? JSON.parse(text) : {};
  } catch {
    data = {};
  }
  const response = NextResponse.json(data, { status: backendRes.status });

  if (setCookie) {
    response.headers.set("set-cookie", setCookie);
  }

  return response;
}
