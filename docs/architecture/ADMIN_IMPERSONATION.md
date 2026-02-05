# Admin Impersonation (Hesabina Gir)

Admin panelinden kullanicilarin hesabini **salt okunur (read-only)** olarak goruntulemek icin kullanilan ozellik.

## Ozet

Admin kullanicilari, musteri destek islemleri sirasinda bir kullanicinin hesabini yeni sekmede goruntuleyebilir. Bu islem tamamen salt okunurdur — veri degisikligi yapilmaz. Koruma cift katmanlidir: frontend `apiRequest()` icinde write isteklerini engeller, backend `ReadOnlyInterceptor` ile POST/PUT/DELETE/PATCH isteklerini 403 ile reddeder.

## Mimari

### Token Akisi

```
Admin Panel (Sekme 1)              Hedef Kullanici Gorunumu (Sekme 2)
┌──────────────┐                   ┌──────────────────────┐
│ Admin Users  │                   │ /impersonate?token=..│
│ sayfasi      │──── Yeni sekme ──>│ sessionStorage kaydet│
│              │     window.open() │ /dashboard redirect  │
└──────┬───────┘                   └──────────┬───────────┘
       │                                      │
       │ POST /api/admin/users/{id}/impersonate│ apiRequest() + X-Impersonation-Token header
       │                                      │
       ▼                                      ▼
┌──────────────┐                   ┌──────────────────────┐
│ Backend      │                   │ BFF Route            │
│ JWT uretir   │                   │ getBackendHeaders()  │
│ + audit log  │                   │ impersonation token  │
└──────────────┘                   │ → Authorization:     │
                                   │   Bearer <token>     │
                                   └──────────┬───────────┘
                                              │
                                              ▼
                                   ┌──────────────────────┐
                                   │ Spring Boot          │
                                   │ JwtAuthFilter:       │
                                   │ impersonatedBy claim │
                                   │ readOnly claim       │
                                   │                      │
                                   │ ReadOnlyInterceptor: │
                                   │ POST/PUT/DELETE → 403│
                                   └──────────────────────┘
```

### SessionStorage Yaklasimi

**Neden cookie degil?**
- Cookie'ler tum sekmeler arasi paylasimlidir. Admin oturumunu etkiler.
- `sessionStorage` sekme bazlidir (tab-scoped). Admin sekmesi etkilenmez.

**Depolanan Veriler:**
- `impersonation_token` — JWT token (hedef kullanicinin kimlik bilgileri + `impersonatedBy` + `readOnly` claim'leri)
- `impersonation_meta` — JSON metadata (targetUserId, targetUserName, targetUserEmail, adminUserId, startedAt)

### Cift Katmanli Salt Okunur Koruma

**Katman 1 — Frontend (`apiRequest()` engeli):**
```
lib/api/client.ts → apiRequest() fonksiyonu
  ├─ sessionStorage'dan impersonation_token kontrol et
  ├─ Token varsa VE method !== "GET" ise:
  │   └─ throw new Error("Salt okunur modda bu islem kullanilamaz")
  └─ Token varsa VE method === "GET" ise:
      └─ X-Impersonation-Token header ekle → devam et
```
- Tum mutation'lar (POST/PUT/DELETE/PATCH) otomatik engellenir
- Backend'e gereksiz istek gitmez
- React Query `onError` ile kullaniciya hata mesaji gosterir

**Katman 2 — Backend (`ReadOnlyInterceptor`):**
- `JwtAuthFilter` token'daki `readOnly` claim'i request attribute olarak set eder
- `ReadOnlyInterceptor` tum POST/PUT/DELETE/PATCH isteklerini 403 ile reddeder
- Allowlist: `/auth/refresh`, `/auth/me`, `/auth/logout`

### Header'da Hedef Kullanici Bilgisi

```
auth.ts → me() fonksiyonu
  ├─ sessionStorage'dan impersonation_token oku
  ├─ Token varsa → fetch header'ina X-Impersonation-Token ekle
  ├─ BFF route (/api/auth/me) → getBackendHeaders() ile token'i al
  ├─ Backend → JwtAuthFilter impersonation token'i cikar
  └─ Response: hedef kullanicinin bilgileri (admin degil)
```

Bu sayede header'da hedef kullanicinin adi gorunur, admin'in degil.

### Layout Entegrasyonu

Impersonation aktifken sayfanin en ustunde kirmizi banner goruntulenir. Tum icerik asagi kayar.

**Z-Index Katmanlari:**
```
z-[100]  ─── ImpersonationBanner (sayfanin en ustu)
z-50     ─── Header
z-40     ─── Sidebar
```

**Offset Sistemi:**
```
                          Normal          Impersonation Aktif
┌─────────────────────┬───────────────┬─────────────────────┐
│ ImpersonationBanner │ gorunmuyor    │ fixed top-0 z-[100] │
│ Header              │ fixed top-0   │ fixed top-10        │
│ Sidebar             │ fixed top-0   │ fixed top-10        │
│ Main content        │ pt-14         │ pt-24               │
└─────────────────────┴───────────────┴─────────────────────┘
```

**Banner Componenti** (`impersonation-banner.tsx`):
- `fixed top-0 left-0 right-0 z-[100] bg-red-600`
- Hedef kullanicinin adini gosterir
- "Salt Okunur" badge (AlertTriangle ikonu)
- "Sonlandir" butonu → `stopImpersonation()` → sessionStorage temizle + sekme kapat

**Layout dosyalari:**
- `header.tsx` — `useImpersonation()` ile `top-10` offset uygular
- `sidebar.tsx` — `useImpersonation()` ile `top-10` offset uygular
- `app-layout.tsx` — `isImpersonating ? "pt-24" : "pt-14"` ile main content offset

## Dosya Yapisi

### Backend

| Dosya | Tur | Aciklama |
|-------|-----|----------|
| `auth/Jwt.java` | Modify | `isImpersonated()`, `getImpersonatedBy()`, `isReadOnly()` |
| `auth/JwtService.java` | Modify | `generateImpersonationToken()` metodu |
| `auth/JwtAuthenticationFilter.java` | Modify | Impersonation claim'leri request attribute olarak set eder |
| `auth/ReadOnlyInterceptor.java` | New | POST/PUT/DELETE/PATCH engeller (readOnly attribute varsa) |
| `config/WebConfig.java` | New | ReadOnlyInterceptor'u register eder |
| `admin/ImpersonationLog.java` | New | Audit log entity |
| `admin/ImpersonationLogRepository.java` | New | Audit log repository |
| `admin/AdminUserService.java` | Modify | `generateImpersonationToken()` + audit log kaydeder |
| `admin/AdminUserController.java` | Modify | `POST /{id}/impersonate` endpoint |
| `auth/AuthController.java` | Modify | `/me` response'una impersonation metadata ekler |
| `users/UserDto.java` | Modify | `isImpersonated`, `impersonatedBy`, `readOnly` alanlari |
| `db/migration/V91__add_impersonation_audit_log.sql` | New | Audit log tablosu + indexler |

### Frontend

| Dosya | Tur | Aciklama |
|-------|-----|----------|
| `hooks/use-impersonation.ts` | New | SessionStorage bazli state yonetimi |
| `lib/api/bff-auth.ts` | New | BFF route'lar icin auth helper (impersonation token oncelikli) |
| `lib/api/auth.ts` | Modify | `me()` fonksiyonunda `X-Impersonation-Token` header injection |
| `lib/api/client.ts` | Modify | `apiRequest()` icinde write engeli + header injection |
| `components/admin/impersonation-banner.tsx` | New | Kirmizi banner component (`z-[100]`) |
| `components/layout/header.tsx` | Modify | `useImpersonation()` ile `top-10` offset |
| `components/layout/sidebar.tsx` | Modify | `useImpersonation()` ile `top-10` offset |
| `components/layout/app-layout.tsx` | Modify | `isImpersonating ? "pt-24" : "pt-14"` |
| `app/[locale]/(app-shell)/impersonate/page.tsx` | New | Init sayfasi (token kaydet + redirect) |
| `app/[locale]/(app-shell)/layout.tsx` | Modify | Banner entegrasyonu |
| `app/api/admin/users/[id]/impersonate/route.ts` | New | BFF impersonation proxy |
| `app/[locale]/(admin)/admin/users/page.tsx` | Modify | "Hesabina Gir" butonu |
| `hooks/queries/use-admin.ts` | Modify | `useImpersonateUser()` mutation hook |
| `messages/tr.json` | Modify | Turkce ceviriler |
| `messages/en.json` | Modify | Ingilizce ceviriler |
| `app/api/**/route.ts` | Modify | Tum BFF route'lar `getBackendHeaders()` kullanir |

## Veritabani

### impersonation_logs tablosu

```sql
CREATE TABLE impersonation_logs (
    id              BIGSERIAL PRIMARY KEY,
    admin_user_id   BIGINT NOT NULL REFERENCES users(id),
    target_user_id  BIGINT NOT NULL REFERENCES users(id),
    action          VARCHAR(50) NOT NULL DEFAULT 'IMPERSONATE',
    ip_address      VARCHAR(50),
    user_agent      TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
-- Indexler: admin_user_id, target_user_id, created_at DESC
```

## JWT Token Yapisi

Impersonation token normal JWT ile ayni yapidadir, ek claim'ler:

```json
{
  "sub": "target_user_email",
  "userId": 123,
  "role": "USER",
  "impersonatedBy": 1,
  "readOnly": true,
  "iat": 1706...,
  "exp": 1706...
}
```

- **Omur**: 1 saat
- **Refresh yok**: Token yenilenemez, suresi dolunca oturum biter

## Guvenlik

- Token 1 saat omurlu, refresh yok
- Read-only cift katmanli koruma (frontend `apiRequest()` + backend `ReadOnlyInterceptor`)
- Frontend engeli: `apiRequest()` icinde GET disindaki tum istekler `throw Error` ile engellenir
- Backend engeli: `ReadOnlyInterceptor` ile 403 response
- Audit log: her impersonation kaydedilir (admin, hedef, IP, user-agent)
- SessionStorage: tab-scoped, diger sekmelere sizmaz
- URL'deki token hemen temizlenir (`window.history.replaceState`)
- Hedef kullanicinin rolu korunur (privilege escalation yok)
- Admin sekmesi etkilenmez — cookie'ler degismez

## Kullanim

1. Admin paneli → Kullanicilar sayfasi
2. Hedef kullanicinin satirinda "Hesabina Gir" butonuna tikla
3. Onay dialog'u goruntulenir
4. Yeni sekmede hedef kullanicinin dashboard'u acilir
5. Kirmizi banner: "[Kullanici Adi] hesabini goruntuluyorsunuz - Salt Okunur"
6. Header'da hedef kullanicinin bilgileri gorunur (admin degil)
7. Herhangi bir yazma islemi denediginde hata mesaji gosterilir
8. "Sonlandir" butonu ile sekme kapatilir
9. Admin sekmesi etkilenmez

## E2E Test Sonuclari (Playwright)

| Test | Aciklama | Sonuc |
|------|----------|-------|
| Header Kontrolu | Header'da hedef kullanici ("T Test User") gorunuyor, admin bilgisi gorunmuyor | GECTI |
| Banner Kontrolu | Kirmizi banner `z-[100]` ile header/sidebar ustunde, "Salt okunur mod" yazisi gorunuyor | GECTI |
| Write Engeli | Urun maliyeti guncelleme denemesi → "Salt okunur modda bu islem kullanilamaz" hatasi, backend'e istek gitmedi | GECTI |

## Bilinen Kisitlamalar

- Token suresi (1 saat) dolunca otomatik cikis mekanizmasi yok — sayfa refresh'te 401 alinir
- Admin tarafinda impersonation gecmisi (log) goruntuleme UI'i henuz yok
- Impersonation sirasinda real-time bildirimler hedef kullaniciya gitmez (WebSocket baglantisi yok)
