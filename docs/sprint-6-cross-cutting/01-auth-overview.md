# Auth Özeti (Mevcut Kod)

JWT akışı, cookie'ler, middleware ve token yenileme. Kaynak: [sellerx-backend/auth/](sellerx-backend/src/main/java/com/ecommerce/sellerx/auth/), [sellerx-frontend/middleware.ts](sellerx-frontend/middleware.ts), [sellerx-frontend/lib/api/client.ts](sellerx-frontend/lib/api/client.ts).

## Backend

- **SecurityConfig:** Stateless JWT; CORS; `featureSecurityRules` ile permitAll/authenticated/hasRole; JwtAuthenticationFilter; 401/403 handler.
- **JWT:** Access token (1 saat), refresh token (7 gün); HMAC-SHA256; JWT_SECRET en az 32 karakter (yoksa login 500).
- **Auth endpoint'leri (permitAll):** POST /auth/login, POST /auth/refresh, POST /auth/logout (AuthSecurityRules).
- **Cookie:** Backend Set-Cookie ile access_token, refreshToken (HTTP-only); frontend BFF üzerinden cookie taşınır.

## Frontend

- **Middleware (middleware.ts):** next-intl routing; PUBLIC_ROUTES: /sign-in, /register, /forgot-password; cookie kontrolü (access_token veya refreshToken); yoksa /sign-in yönlendirme; /api ve /_next public.
- **API client (lib/api/client.ts):** BFF route'larına istek (cookie otomatik); 401'de refresh (POST /auth/refresh); queue tabanlı refresh (isRefreshing + subscriber queue) ile çift refresh engelleme.
- **Auth hook:** use-auth.ts; login, register, logout, refresh; cookie tabanlı session.

## Akış

1. Login → Backend JWT döner, Set-Cookie (access_token, refreshToken).
2. Sayfa isteği → Middleware cookie kontrolü; yoksa /sign-in.
3. BFF API çağrısı → Cookie backend'e iletilir; 401'de client refresh dener, tekrar istek.
4. Logout → Backend logout çağrılır; cookie temizlenir.
