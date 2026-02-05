# Sprint 6: Cross-cutting (Auth, Güvenlik, Env, Deploy)

Sprint 6'ın odak alanı: Auth akışı, backend güvenlik kuralları, ortam değişkenleri ve deploy/CI dokümantasyonu. **Sadece doc yazılır; kod değiştirilmez.**

## Dosya Listesi

| Dosya | İçerik |
|-------|--------|
| [01-auth-overview.md](01-auth-overview.md) | JWT, cookie, middleware, API client refresh özeti. |
| [02-security-rules.md](02-security-rules.md) | Backend SecurityRules implementasyonları (permitAll, hasRole, method-level). |
| [03-env-and-deploy.md](03-env-and-deploy.md) | Env değişkenleri, db.sh, start-sellerx, Docker, CI. |

## Kapsam

- **Kaynak:** auth paketi, SecurityConfig, SecurityRules implementasyonları, middleware.ts, lib/api/client.ts, CLAUDE.md, .github/workflows/ci.yml, docker-compose, db.sh, start-sellerx.sh.
- **Güncelleme:** Yeni public endpoint veya SecurityRules eklenince 02 güncellenir; env/deploy değişince 03 güncellenir.

## Nasıl Kullanılır

- Yeni permitAll/hasRole kuralı → 02-security-rules.md güncellenir.
- Yeni env değişkeni veya CI adımı → 03-env-and-deploy.md güncellenir.
