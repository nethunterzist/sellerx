import { NextRequest, NextResponse } from "next/server";
import { logger } from "@/lib/logger";

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();

    const backendRes = await fetch(`${process.env.API_BASE_URL}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
      credentials: "include",
    });

    // Backend'den dönen TÜM Set-Cookie header'larını al
    const setCookies = backendRes.headers.getSetCookie();

    // Response body'sini text olarak al, sonra JSON parse et
    const text = await backendRes.text();

    let data;
    try {
      data = text ? JSON.parse(text) : { error: "Empty response from backend" };
    } catch {
      data = { error: "Invalid JSON response", raw: text };
    }

    const response = NextResponse.json(data, { status: backendRes.status });

    // TÜM Cookie'leri Next.js response'una ekle
    if (setCookies && setCookies.length > 0) {
      setCookies.forEach((cookie) => {
        response.headers.append("set-cookie", cookie);
      });
    }

    return response;
  } catch (error) {
    logger.error("Login error", { endpoint: "/auth/login", error });
    return NextResponse.json(
      { error: "Backend connection failed", details: String(error) },
      { status: 500 }
    );
  }
}
