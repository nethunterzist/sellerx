import { NextRequest, NextResponse } from "next/server";

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();

    console.log("Login request to:", `${process.env.API_BASE_URL}/auth/login`);

    const backendRes = await fetch(`${process.env.API_BASE_URL}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });

    console.log("Backend response status:", backendRes.status);

    // Backend'den dönen Set-Cookie header'larını al
    const setCookie = backendRes.headers.get("set-cookie");

    // Response body'sini text olarak al, sonra JSON parse et
    const text = await backendRes.text();
    console.log("Backend response body:", text);

    let data;
    try {
      data = text ? JSON.parse(text) : { error: "Empty response from backend" };
    } catch {
      data = { error: "Invalid JSON response", raw: text };
    }

    const response = NextResponse.json(data, { status: backendRes.status });

    // Cookie'yi Next.js response'una ekle
    if (setCookie) {
      response.headers.set("set-cookie", setCookie);
    }

    return response;
  } catch (error) {
    console.error("Login error:", error);
    return NextResponse.json(
      { error: "Backend connection failed", details: String(error) },
      { status: 500 }
    );
  }
}
