# Env ve Deploy Özeti (Mevcut Kod)

Ortam değişkenleri, Docker ve CI. Kaynak: [CLAUDE.md](CLAUDE.md), [.github/workflows/ci.yml](.github/workflows/ci.yml), [docker-compose](docker-compose.*), [db.sh](db.sh), [start-sellerx.sh](start-sellerx.sh).

## Ortam Değişkenleri

**Frontend (.env.local):**
- API_BASE_URL, NEXT_PUBLIC_API_BASE_URL (örn. http://localhost:8080)

**Backend (Spring / env):**
- SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD
- JWT_SECRET (en az 32 karakter; yoksa login 500)
- WEBHOOK_BASE_URL, WEBHOOK_SIGNATURE_SECRET (opsiyonel)
- cors.allowed-origins (varsayılan http://localhost:3000)

**Not:** .env.dev, .env.prod.example proje kökünde referans için kullanılabilir.

## Deploy / Çalıştırma

| Araç | Açıklama |
|------|----------|
| db.sh | PostgreSQL (Docker): start, stop, connect, reset. Port 5432. |
| start-sellerx.sh | 4 terminal: DB, backend, frontend; JWT_SECRET set eder. |
| docker-compose.db.yml | Sadece DB container. |
| docker-compose.dev.yml | Full stack (DB + backend + frontend) geliştirme. |
| docker-compose.backend.yml | Backend + DB. |

**Erişim:** Frontend localhost:3000, Backend localhost:8080, DB localhost:5432. Test kullanıcı: test@test.com / 123456.

## CI/CD

**GitHub Actions (.github/workflows/ci.yml):**
- Tetikleyici: push/PR → main, master, develop
- Backend job: PostgreSQL 15 service; JDK 21; Maven build + test
- Frontend job: Node 20; npm ci; build; test (vitest --run)
- Env: JAVA_VERSION 21, NODE_VERSION 20

**Özet:** Env: Frontend API_BASE_URL; Backend DB + JWT_SECRET + WEBHOOK_BASE_URL. Deploy: db.sh, start-sellerx.sh veya docker-compose.dev.yml; CI: backend + frontend build ve test.
