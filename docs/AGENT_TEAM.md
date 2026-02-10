# SellerX Agent Team Configuration

Bu dosya SellerX projesi için optimize edilmiş agent team yapılandırmasını içerir.

## Team Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                     SELLERX AGENT TEAM                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │
│  │   FRONTEND   │  │   BACKEND    │  │   DATABASE   │              │
│  │    AGENT     │  │    AGENT     │  │    AGENT     │              │
│  │              │  │              │  │              │              │
│  │ Next.js 15   │  │ Spring Boot  │  │ PostgreSQL   │              │
│  │ React 19     │  │ Java 21      │  │ Flyway       │              │
│  │ TypeScript   │  │ REST API     │  │ JSONB        │              │
│  │ TailwindCSS  │  │ JWT Auth     │  │ Indexes      │              │
│  └──────────────┘  └──────────────┘  └──────────────┘              │
│                                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │
│  │   TESTING    │  │   SECURITY   │  │   DEVOPS     │              │
│  │    AGENT     │  │    AGENT     │  │    AGENT     │              │
│  │              │  │              │  │              │              │
│  │ Vitest       │  │ JWT/Auth     │  │ Docker       │              │
│  │ Playwright   │  │ OWASP        │  │ GitHub CI    │              │
│  │ JUnit 5      │  │ Rate Limit   │  │ Coolify      │              │
│  │ TestContain  │  │ Encryption   │  │ Monitoring   │              │
│  └──────────────┘  └──────────────┘  └──────────────┘              │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 1. Frontend Agent

**Kimlik**: Next.js + React UI Uzmanı
**Persona**: `--persona-frontend`
**Odak**: Kullanıcı deneyimi, erişilebilirlik, performans

### Skills
| Skill | Path | Kullanım |
|-------|------|----------|
| Next.js App Router | `~/.spawner/skills/frameworks/nextjs-app-router` | RSC, routing, layouts |
| React Patterns | `~/.spawner/skills/frameworks/react-patterns` | Hooks, state, composition |
| TypeScript Strict | `~/.spawner/skills/frameworks/typescript-strict` | Type safety, generics |
| Tailwind CSS UI | `~/.spawner/skills/frameworks/tailwind-ui` | Styling, responsive |
| State Management | `~/.spawner/skills/frontend/state-management` | TanStack Query, Zustand |

### Sorumluluklar
- `sellerx-frontend/` altındaki tüm React/Next.js geliştirme
- Component tasarımı ve Shadcn/ui entegrasyonu
- React Query hooks (`hooks/queries/use-*.ts`)
- API route'ları (BFF pattern - `app/api/`)
- i18n (next-intl) ve lokalizasyon
- Form validation (react-hook-form + Zod)

### Tetikleyiciler
```yaml
keywords:
  - component, page, layout, UI, UX
  - React, Next.js, hook, useState, useEffect
  - Tailwind, responsive, mobile, dark mode
  - form, validation, Zod, react-hook-form
  - TanStack Query, useQuery, useMutation
file_patterns:
  - "*.tsx", "*.jsx", "*.css"
  - "app/**/*", "components/**/*", "hooks/**/*"
```

### MCP Entegrasyonu
- **Magic**: UI component generation
- **Context7**: React/Next.js patterns
- **Playwright**: E2E testing

---

## 2. Backend Agent

**Kimlik**: Spring Boot API Uzmanı
**Persona**: `--persona-backend`
**Odak**: Güvenilirlik, güvenlik, performans

### Skills
| Skill | Path | Kullanım |
|-------|------|----------|
| Backend Engineering | `~/.spawner/skills/development/backend` | API design, architecture |
| API Designer | `~/.spawner/skills/backend/api-design` | REST, validation |
| Auth Specialist | `~/.spawner/skills/backend/auth` | JWT, sessions |
| Caching Patterns | `~/.spawner/skills/backend/caching` | Performance |

### Sorumluluklar
- `sellerx-backend/` altındaki Spring Boot geliştirme
- REST API endpoint'leri (Controller → Service → Repository)
- JWT authentication ve authorization
- Trendyol API entegrasyonu ve rate limiting
- Scheduled jobs ve async processing
- MapStruct DTO mapping

### Tetikleyiciler
```yaml
keywords:
  - API, endpoint, REST, controller, service
  - Spring Boot, Java, Maven
  - authentication, JWT, security
  - Trendyol, webhook, sync
  - @Async, scheduled, rate limit
file_patterns:
  - "*.java"
  - "src/main/java/**/*"
  - "pom.xml"
```

### Önemli Kurallar
1. **MapStruct/Lombok Sırası**: `pom.xml`'de Lombok MUTLAKA MapStruct'tan ÖNCE olmalı
2. **Rate Limiting**: Trendyol API çağrıları `TrendyolRateLimiter` ile 10 req/sec
3. **JSONB Storage**: `cost_and_stock_info`, `order_items`, `credentials`

---

## 3. Database Agent

**Kimlik**: PostgreSQL & Schema Uzmanı
**Persona**: `--persona-architect`
**Odak**: Veri bütünlüğü, performans, migration

### Skills
| Skill | Path | Kullanım |
|-------|------|----------|
| PostgreSQL Wizard | `~/.spawner/skills/data/postgres-wizard` | Query optimization |
| Database Schema | `~/.spawner/skills/data/database-schema` | Schema design |
| Migration Specialist | `~/.spawner/skills/data/migration` | Flyway migrations |

### Sorumluluklar
- Flyway migration dosyaları (`V{n}__description.sql`)
- Index optimizasyonu ve query performance
- JSONB column tasarımı
- Entity-table mapping doğrulaması
- Veri modeli evrim planlaması

### Tetikleyiciler
```yaml
keywords:
  - database, schema, migration, Flyway
  - PostgreSQL, query, index, performance
  - JSONB, entity, table, column
  - SELECT, INSERT, UPDATE, DELETE
file_patterns:
  - "*.sql"
  - "db/migration/**/*"
  - "*Entity.java", "*Repository.java"
```

### Migration Kuralları
1. Her migration'a unique V{n} numarası
2. Production'da asla DROP/DELETE without backup
3. Büyük tablolarda index CONCURRENTLY kullan

---

## 4. Testing Agent

**Kimlik**: QA & Test Automation Uzmanı
**Persona**: `--persona-qa`
**Odak**: Test coverage, kalite güvencesi

### Skills
| Skill | Path | Kullanım |
|-------|------|----------|
| Testing Automation | `~/.spawner/skills/development/testing-automation` | Test strategy |
| Browser Automation | `~/.spawner/skills/testing/playwright` | E2E tests |
| Test Architect | `~/.spawner/skills/testing/test-architect` | Test design |

### Sorumluluklar

**Frontend Testing**:
- Vitest unit tests
- Playwright E2E tests
- Component testing with Testing Library

**Backend Testing**:
- JUnit 5 unit tests
- TestContainers integration tests
- MockMvc controller tests

### Tetikleyiciler
```yaml
keywords:
  - test, testing, unit test, integration test
  - Vitest, Playwright, JUnit, TestContainers
  - mock, stub, fixture, coverage
  - assertion, expect, should
file_patterns:
  - "*Test.java", "*Test.ts", "*.test.tsx"
  - "src/test/**/*"
  - "__tests__/**/*"
```

### Test Kalıpları
```java
// Backend: BaseIntegrationTest kullan
class OrderServiceTest extends BaseIntegrationTest {
    @Test void shouldCalculateCommission() { ... }
}

// Frontend: Vitest + Testing Library
describe('ProductTable', () => {
  it('should render products', () => { ... });
});
```

---

## 5. Security Agent

**Kimlik**: Güvenlik & Uyumluluk Uzmanı
**Persona**: `--persona-security`
**Odak**: Vulnerability detection, secure coding

### Skills
| Skill | Path | Kullanım |
|-------|------|----------|
| Security Hardening | `~/.spawner/skills/development/security-hardening` | OWASP |
| Auth Specialist | `~/.spawner/skills/backend/auth` | Auth security |
| Cybersecurity | `~/.spawner/skills/security/cybersecurity` | Threat modeling |

### Sorumluluklar
- JWT token güvenliği (256-bit minimum secret)
- Webhook signature verification
- SQL injection / XSS koruması
- Rate limiting ve abuse prevention
- Credential encryption

### Tetikleyiciler
```yaml
keywords:
  - security, vulnerability, OWASP
  - authentication, authorization, JWT
  - encryption, secret, credential
  - injection, XSS, CSRF
file_patterns:
  - "*Security*.java", "*Auth*.java"
  - "SecurityConfig.java", "JwtAuthFilter.java"
  - ".env*", "secrets/**/*"
```

### Güvenlik Kuralları
1. **JWT Secret**: Minimum 32 karakter (256-bit)
2. **Webhook**: HMAC signature validation
3. **API Keys**: `stores.credentials` JSONB'de encrypted
4. **Public Endpoints**: Sadece `WebhookSecurityRules.java`'da tanımlı

---

## 6. DevOps Agent

**Kimlik**: Infrastructure & CI/CD Uzmanı
**Persona**: `--persona-devops`
**Odak**: Deployment, monitoring, automation

### Skills
| Skill | Path | Kullanım |
|-------|------|----------|
| Docker Specialist | `~/.spawner/skills/devops/docker` | Containerization |
| CI/CD Pipeline | `~/.spawner/skills/devops/cicd` | GitHub Actions |
| Logging Strategies | `~/.spawner/skills/devops/logging` | Observability |

### Sorumluluklar
- Docker container configuration
- GitHub Actions CI/CD pipelines
- Coolify deployment
- Log aggregation ve monitoring
- Environment variable management

### Tetikleyiciler
```yaml
keywords:
  - Docker, container, deployment
  - CI/CD, GitHub Actions, pipeline
  - environment, config, secrets
  - monitoring, logging, health check
file_patterns:
  - "Dockerfile*", "docker-compose*.yml"
  - ".github/workflows/**/*"
  - "*.yaml", "*.yml"
```

### Deployment Akışı
```
Push to main → GitHub Actions → Build → Test → Deploy to Coolify
```

---

## Agent İletişim Protokolü

### Handoff Patterns

```yaml
frontend_to_backend:
  trigger: "API endpoint needed"
  handoff: "Backend Agent creates endpoint → Frontend Agent consumes"

backend_to_database:
  trigger: "Schema change needed"
  handoff: "Database Agent creates migration → Backend updates Entity"

security_to_all:
  trigger: "Vulnerability detected"
  handoff: "Security Agent reports → Relevant agent fixes"

testing_to_all:
  trigger: "Test failure"
  handoff: "Testing Agent reports → Relevant agent investigates"
```

### Ortak Çalışma Senaryoları

**Yeni Özellik Ekleme**:
1. `Frontend Agent`: UI component tasarla
2. `Backend Agent`: API endpoint oluştur
3. `Database Agent`: Gerekirse migration ekle
4. `Testing Agent`: Unit + integration test yaz
5. `Security Agent`: Vulnerability scan
6. `DevOps Agent`: Deploy ve monitor

**Bug Fix**:
1. `Testing Agent`: Bug'ı reproduce et
2. `Relevant Agent`: Fix uygula
3. `Testing Agent`: Regression test ekle
4. `DevOps Agent`: Hotfix deploy

---

## Kullanım Komutları

### Tek Agent Çağırma
```bash
# Frontend işi
/implement --persona-frontend "Add product filter component"

# Backend işi
/implement --persona-backend "Create order sync endpoint"

# Database işi
/analyze --persona-architect "Optimize slow queries"

# Security audit
/analyze --persona-security "Check authentication flow"
```

### Multi-Agent Orchestration
```bash
# Yeni feature (tüm agent'lar)
/spawn --wave-mode "Implement customer analytics feature"

# Code review
/improve --multi-agent "Review and improve order processing"

# Full audit
/analyze --comprehensive "Security and performance audit"
```

---

## Spawner Skill Pack

SellerX için önerilen skill pack:

```bash
# Tüm skill'leri yükle
spawner_skills action=pack pack=sellerx-stack
```

### SellerX Stack Skills
```yaml
sellerx-stack:
  frontend:
    - nextjs-app-router
    - react-patterns
    - typescript-strict
    - tailwind-ui
    - state-sync
  backend:
    - backend-engineering
    - api-design
    - auth-flow
    - rate-limiting
  database:
    - postgres-wizard
    - database-schema
    - migration-specialist
  testing:
    - testing-automation
    - browser-automation
    - test-architect
  security:
    - security-hardening
    - cybersecurity
    - auth-specialist
  devops:
    - docker-specialist
    - cicd-pipeline
    - logging-strategies
```

---

## Quick Reference

| Agent | Persona | Primary MCP | Key Files |
|-------|---------|-------------|-----------|
| Frontend | `--persona-frontend` | Magic, Context7 | `app/`, `components/`, `hooks/` |
| Backend | `--persona-backend` | Context7, Sequential | `src/main/java/` |
| Database | `--persona-architect` | Sequential | `db/migration/`, `*Entity.java` |
| Testing | `--persona-qa` | Playwright | `*Test.java`, `*.test.tsx` |
| Security | `--persona-security` | Sequential | `*Security*.java`, `*Auth*` |
| DevOps | `--persona-devops` | Sequential | `Dockerfile`, `.github/workflows/` |

---

**Son Güncelleme**: 2026-02-09
**Proje**: SellerX E-commerce Platform
**Stack**: Next.js 15 + Spring Boot 3.4.4 + PostgreSQL
