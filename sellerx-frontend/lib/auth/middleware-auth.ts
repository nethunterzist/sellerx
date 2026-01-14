import { NextRequest, NextResponse } from "next/server";
import { authCache } from "./auth-cache";

export interface AuthResult {
  isAuthenticated: boolean;
  response?: NextResponse;
}

export async function checkAuthentication(
  request: NextRequest,
): Promise<AuthResult> {
  const accessToken = request.cookies.get("access_token")?.value;
  const refreshToken = request.cookies.get("refreshToken")?.value;

  // Hiç token yoksa
  if (!accessToken && !refreshToken) {
    return { isAuthenticated: false };
  }

  // If access token exists, check shared cache first
  if (accessToken) {
    const cached = authCache.isValid(accessToken);
    if (cached === true) {
      return { isAuthenticated: true };
    } else if (cached === false) {
      // In cache marked as invalid, try with refresh token
    } else {
      // In cache not found, check with API
      try {
        const response = await fetch(new URL("/api/auth/me", request.url), {
          headers: {
            Cookie: request.headers.get("cookie") || "",
            "Content-Type": "application/json",
          },
          cache: "no-store",
        });

        if (response.ok) {
          const userData = await response.json();
          authCache.set(accessToken, userData, true);
          return { isAuthenticated: true };
        } else {
          authCache.set(accessToken, null, false);
        }
      } catch {
        authCache.set(accessToken, null, false);
      }
    }
  }

  // Access token yoksa veya geçersizse, refresh token ile yenile
  if (refreshToken) {
    try {
      const refreshResponse = await fetch(
        new URL("/api/auth/refresh", request.url),
        {
          method: "POST",
          headers: {
            Cookie: request.headers.get("cookie") || "",
            "Content-Type": "application/json",
          },
          cache: "no-store",
        },
      );

      if (refreshResponse.ok) {
        // Yeni token'ları al ve cookie'leri güncelle
        const responseWithCookies = NextResponse.next();

        // Refresh response'dan gelen cookie'leri kopyala
        const setCookieHeaders = refreshResponse.headers.getSetCookie();
        setCookieHeaders.forEach((cookieString) => {
          responseWithCookies.headers.append("Set-Cookie", cookieString);
        });

        return { isAuthenticated: true, response: responseWithCookies };
      }
    } catch {
      // Refresh token da başarısız
    }
  }

  return { isAuthenticated: false };
}
