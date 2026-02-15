# Auth Özeti (Mevcut Kod)

JWT akışı, cookie'ler, middleware ve token yenileme. Kaynak: [sellerx-backend/auth/](sellerx-backend/src/main/java/com/ecommerce/sellerx/auth/), [sellerx-frontend/middleware.ts](sellerx-frontend/middleware.ts), [sellerx-frontend/lib/api/client.ts](sellerx-frontend/lib/api/client.ts).

## Backend

- **SecurityConfig:** Stateless JWT; CORS; `featureSecurityRules` ile permitAll/authenticated/hasRole; JwtAuthenticationFilter; 401/403 handler.
- **JWT:** Access token (1 saat), refresh token (7 gün); HMAC-SHA256; JWT_SECRET en az 32 karakter (yoksa login 500).
- **Auth endpoint'leri (permitAll):** POST /auth/login, POST /auth/refresh, POST /auth/logout (AuthSecurityRules).
- **Cookie:** Backend Set-Cookie ile access_token, refreshToken (HTTP-only); frontend BFF üzerinden cookie taşınır.

## Frontend

- **Middleware (middleware.ts):** next-intl routing; PUBLIC_ROUTES: /sign-in, /register, /forgot-password, /reset-password, /verify-email, /verification-pending; cookie kontrolü (access_token veya refreshToken); yoksa /sign-in yönlendirme; /api ve /_next public.
- **API client (lib/api/client.ts):** BFF route'larına istek (cookie otomatik); 401'de refresh (POST /auth/refresh); queue tabanlı refresh (isRefreshing + subscriber queue) ile çift refresh engelleme.
- **Auth hook:** use-auth.ts; login, register, logout, refresh; cookie tabanlı session.

## Akış

1. Login → Backend JWT döner, Set-Cookie (access_token, refreshToken).
2. Sayfa isteği → Middleware cookie kontrolü; yoksa /sign-in.
3. BFF API çağrısı → Cookie backend'e iletilir; 401'de client refresh dener, tekrar istek.
4. Logout → Backend logout çağrılır; cookie temizlenir.

## Email Doğrulama (Email Verification)

Kayıt sonrası kullanıcıdan email doğrulaması istenir. Doğrulama tamamlanana kadar erişim kısıtlıdır.

**Backend:** `EmailVerificationService`, `EmailVerificationController`

- Kayıt sırasında `sendVerificationEmail(userId)` çağrılır → SecureRandom token (48 byte, base64) üretilir → `User` entity'sine kaydedilir → `EmailVerificationEvent` publish edilir (email gönderimi).
- Token süresi: varsayılan ayar (User entity'deki `emailVerificationTokenExpiry`).
- Resend limiti: aynı kullanıcıya en az 2 dakika arayla yeniden gönderim.
- Doğrulama: `GET /auth/verify-email?token=...` → token kontrol → `emailVerified = true`.

**Frontend endpoint'leri:**
- `GET /api/auth/verify-email` → token ile doğrulama
- `POST /api/auth/resend-verification` → yeni token gönder
- `GET /api/auth/verification-status` → doğrulama durumu sorgula

**Sayfalar:** `/verify-email`, `/verification-pending`

## Şifre Sıfırlama (Password Reset)

**Backend:** `PasswordResetService`, `PasswordResetController`, `PasswordResetToken` entity, `PasswordResetTokenRepository`

- Kullanıcı email girer → `requestPasswordReset(email)` → email varsa SecureRandom token üretilir → `PasswordResetToken` entity'sine kaydedilir (1 saat geçerli) → `PasswordResetRequestedEvent` publish edilir.
- Email enumeration saldırısını önlemek için her durumda başarı mesajı döner.
- Resend limiti: aynı IP'den en az 2 dakika arayla.
- Token doğrulama: `GET /auth/verify-reset-token?token=...`
- Şifre değiştirme: `POST /auth/reset-password` (token + yeni şifre)

**Frontend endpoint'leri:**
- `POST /api/auth/forgot-password` → reset email gönder
- `GET /api/auth/verify-reset-token` → token geçerliliği kontrol
- `POST /api/auth/reset-password` → yeni şifre kaydet

**Sayfalar:** `/forgot-password`, `/reset-password`

## Auth Rate Limiter

**Sınıf:** `AuthRateLimiter` — IP bazlı brute-force koruması (Guava Cache).

| Endpoint | Limit | Pencere |
|----------|-------|---------|
| Login | 5 deneme | 1 dakika |
| Password reset | 3 deneme | 1 saat |
| Email verification resend | 3 deneme | 1 saat |

Süresi dolan entry'ler otomatik temizlenir (2 saat inactivity sonrası expire).
