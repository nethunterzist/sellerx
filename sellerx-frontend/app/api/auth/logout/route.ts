import { NextRequest, NextResponse } from "next/server";
import { logger } from "@/lib/logger";

export async function POST(req: NextRequest) {
  try {
    const cookie = req.headers.get("cookie");
    const backendRes = await fetch(`${process.env.API_BASE_URL}/auth/logout`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...(cookie ? { cookie } : {}),
      },
      credentials: "include",
    });

    const setCookie = backendRes.headers.get("set-cookie");

    const response = NextResponse.json(
      { message: "Logged out successfully" },
      { status: 200 },
    );

    // Cookie'yi Next.js response'una ekle
    if (setCookie) {
      response.headers.set("set-cookie", setCookie);
    }

    return response;
  } catch (error) {
    logger.error("Logout error", { endpoint: "/auth/logout", error });
    // Hata olsa bile cookie'leri sil
    const response = NextResponse.json(
      { message: "Logged out" },
      { status: 200 },
    );

    // Manuel olarak cookie'leri sil - backend ile aynı path'leri kullan
    response.cookies.set("access_token", "", {
      httpOnly: true,
      secure: process.env.NODE_ENV === "production",
      path: "/",
      maxAge: 0,
    });

    response.cookies.set("refreshToken", "", {
      httpOnly: true,
      secure: process.env.NODE_ENV === "production",
      path: "/", // Backend ile tutarlı olması için "/" kullan
      maxAge: 0,
    });

    return response;
  }
}
