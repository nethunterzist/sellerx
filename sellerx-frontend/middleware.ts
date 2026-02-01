import { NextRequest, NextResponse } from "next/server";
import createMiddleware from "next-intl/middleware";
import { routing } from "./i18n/routing";

// next-intl middleware'ini oluştur
const handleI18nRouting = createMiddleware(routing);

// Performans için Set kullanımı
const PUBLIC_ROUTES = new Set([
  "/sign-in",
  "/register",
  "/forgot-password",
  "/favicon.ico",
]);

// Public route kontrolü
function isPublicRoute(pathname: string): boolean {
  // Next.js internal routes ve API routes
  if (pathname.startsWith("/_next") || pathname.startsWith("/api")) {
    return true;
  }

  // Direct public routes
  if (PUBLIC_ROUTES.has(pathname)) {
    return true;
  }

  return routing.locales.some((locale) =>
    PUBLIC_ROUTES.has(pathname.replace(`/${locale}`, "")),
  );
}

// Sign-in sayfasına yönlendirme
function redirectToSignIn(request: NextRequest, pathname: string) {
  const locale =
    routing.locales.find((loc) => pathname.startsWith(`/${loc}`)) ||
    routing.defaultLocale;
  const signInUrl = new URL(`/${locale}/sign-in`, request.url);
  let callbackPath = pathname;
  routing.locales.forEach((loc) => {
    if (pathname.startsWith(`/${loc}/`)) {
      callbackPath = pathname.substring(`/${loc}`.length);
    }
  });

  signInUrl.searchParams.set("callbackUrl", callbackPath);
  return NextResponse.redirect(signInUrl);
}

// Dashboard'a yönlendirme
function redirectToDashboard(request: NextRequest, pathname: string) {
  return NextResponse.redirect(new URL(`${pathname}/dashboard`, request.url));
}

// Basit cookie-based auth kontrolü (backend'e istek atmadan)
function hasAuthCookie(request: NextRequest): boolean {
  const accessToken = request.cookies.get("access_token")?.value;
  const refreshToken = request.cookies.get("refreshToken")?.value;
  const hasAuth = !!(accessToken || refreshToken);

  // Development logging
  if (process.env.NODE_ENV === "development") {
    const pathname = request.nextUrl.pathname;
    if (!hasAuth) {
      console.log(`[MIDDLEWARE] No auth cookie for: ${pathname}`);
    }
  }

  return hasAuth;
}

export default function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // Auth kontrolü - sadece cookie varlığına bak
  if (!isPublicRoute(pathname)) {
    if (!hasAuthCookie(request)) {
      if (process.env.NODE_ENV === "development") {
        console.log(`[MIDDLEWARE] Redirecting to sign-in: ${pathname}`);
      }
      return redirectToSignIn(request, pathname);
    }
  }

  const isRootPath = pathname === "/";
  const isLocaleRoot = routing.locales.some(
    (locale) => pathname === `/${locale}`,
  );

  if (isRootPath || isLocaleRoot) {
    const response = handleI18nRouting(request);

    // next-intl redirect response'unu dashboard ile değiştir
    if (response?.status >= 300 && response?.status < 400) {
      const location = response.headers.get("location");
      if (location) {
        const url = new URL(location);
        if (
          isRootPath &&
          routing.locales.some((locale) => url.pathname === `/${locale}`)
        ) {
          return redirectToDashboard(request, url.pathname);
        }
      }
    }

    // Direct locale paths
    if (isLocaleRoot) {
      return redirectToDashboard(request, pathname);
    }

    return response;
  }

  // Diğer tüm route'lar için normal next-intl middleware
  return handleI18nRouting(request);
}

export const config = {
  matcher: [
    // Root path
    "/",
    // Locale paths: /(tr|en)/:path*
    "/(tr|en)/:path*",
    // Modern negative lookahead pattern
    "/((?!api|_next/static|_next/image|favicon.ico|images).*)",
  ],
};
