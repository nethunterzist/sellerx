# Sprint 1: API Envanteri

Backend REST endpoint'leri ve BFF (Next.js API routes) envanteri. Hiçbir controller/route atlanmadan çıkarılmıştır.

## Dosyalar

| Dosya | İçerik |
|-------|--------|
| [01-backend-endpoints.md](./01-backend-endpoints.md) | Tüm backend controller'lar ve endpoint'ler (HTTP, path, path params) |
| [02-public-endpoints.md](./02-public-endpoints.md) | JWT gerektirmeyen (permitAll) ve Admin-only endpoint'ler |
| [03-bff-routes.md](./03-bff-routes.md) | Tüm BFF route'ları ve çağırdıkları backend path'ler |
| [04-bff-to-backend-map.md](./04-bff-to-backend-map.md) | BFF ↔ Backend eşlemesi ve uyumsuzluklar |

## Nasıl Kullanılır

- Yeni endpoint eklerken: 01 ve (BFF kullanıyorsa) 03'ü güncelle; 04'te eşleşmeyi kontrol et.
- Public endpoint eklerken: 02'yi güncelle.
- Frontend'den hangi backend path'e gidildiğini bulmak: 03 veya 04'e bak.

## Kapsam

- **Backend:** `sellerx-backend/src/main/java/com/ecommerce/sellerx/**/*Controller.java` (ExceptionHandler, ControllerAdvice, Test, billing-disabled hariç).
- **BFF:** `sellerx-frontend/app/api/**/route.ts`.
