# 🚀 SellerX Production Deployment Guide

**Son Güncelleme:** 16 Şubat 2026
**Durum:** ✅ STABLE - GitHub Actions CI/CD ile otomatik deployment aktif

---

## 📋 İçindekiler

1. [Deployment Mimarisi](#deployment-mimarisi)
2. [Ön Gereksinimler](#ön-gereksinimler)
3. [GitHub Secrets Yapılandırması](#github-secrets-yapılandırması)
4. [Deployment Akışı](#deployment-akışı)
5. [Production Readiness Checklist](#production-readiness-checklist)
6. [Health Check & Doğrulama](#health-check--doğrulama)
7. [Rollback Prosedürü](#rollback-prosedürü)
8. [Troubleshooting](#troubleshooting)
9. [Bilinen Hatalar ve Çözümler](#bilinen-hatalar-ve-çözümler)

---

## 🏗️ Deployment Mimarisi

```
┌─────────────────────────────────────────────────────────────────┐
│                      GITHUB REPOSITORY                           │
│  main branch'e push → sellerx-backend/** veya frontend/**        │
└─────────────┬───────────────────────────────────────────────────┘
              │ trigger
              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   GITHUB ACTIONS                                 │
│  ┌───────────────────┐    ┌──────────────────────┐             │
│  │  Backend Build    │    │  Frontend Build      │             │
│  │  - mvn test       │    │  - npm ci            │             │
│  │  - Docker build   │    │  - npm run build     │             │
│  │  - Push to GHCR   │    │  - Docker build      │             │
│  └─────────┬─────────┘    └──────────┬───────────┘             │
└────────────┼────────────────────────────┼─────────────────────────┘
             │ Docker image               │
             ▼                            ▼
┌─────────────────────────────────────────────────────────────────┐
│          GITHUB CONTAINER REGISTRY (GHCR)                        │
│  ghcr.io/nethunterzist/sellerx/backend:latest                   │
│  ghcr.io/nethunterzist/sellerx/frontend:latest                  │
└─────────────┬───────────────────────────────────────────────────┘
              │ SSH Deploy
              ▼
┌─────────────────────────────────────────────────────────────────┐
│          PRODUCTION SERVER (157.180.78.53)                       │
│  ┌──────────────────────────────────────────────────┐           │
│  │         Coolify Network + Traefik Proxy          │           │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────┐ │           │
│  │  │   Backend    │ │  Frontend    │ │PostgreSQL│ │           │
│  │  │   :8080      │ │   :3000      │ │  :5432   │ │           │
│  │  └──────────────┘ └──────────────┘ └──────────┘ │           │
│  └──────────────────────────────────────────────────┘           │
└─────────────────────────────────────────────────────────────────┘
```

### Neden Bu Mimari?

| Sorun | Önceki Durum | Çözüm |
|-------|--------------|-------|
| RAM Yetersizliği | Sunucu 3.7GB, Next.js build 4GB+ gerektirir | GitHub Actions runner (7GB RAM) |
| Private Repo | Coolify private repo'dan çekemez | GHCR public image registry |
| Platform Farkı | Mac ARM → Linux AMD64 | GitHub runner native AMD64 build |
| Manuel Deploy | ~40 dakika | Otomatik ~5 dakika |

---

## ✅ Ön Gereksinimler

### GitHub Repository

- [x] Repository: `nethunterzist/sellerx` (private)
- [x] Branch: `main` (deployment branch)
- [x] GitHub Actions enabled
- [x] GitHub Packages enabled (GHCR)

### Production Server

- [x] Server: Hetzner VPS (157.180.78.53)
- [x] OS: Ubuntu 24.04 LTS
- [x] Docker: Yüklü ve çalışır durumda
- [x] Coolify: Network ve Traefik konfigürasyonu
- [x] PostgreSQL: Coolify managed (uws408w88ko044c84gwgkw04)
- [x] SSH Access: root@157.180.78.53

### Gerekli Araçlar (Local)

```bash
# GitHub CLI
brew install gh
gh auth login

# SSH key (deployment için)
ssh-keygen -t ed25519 -C "github-actions-deploy"
```

---

## 🔑 GitHub Secrets Yapılandırması

**Lokasyon:** Repository → Settings → Secrets and variables → Actions

### Gerekli Secrets

| Secret Name | Açıklama | Örnek/Format |
|-------------|----------|--------------|
| `SSH_PRIVATE_KEY` | Sunucu SSH private key | `-----BEGIN OPENSSH PRIVATE KEY-----` |
| `DB_PASSWORD` | PostgreSQL şifresi | (sunucu PostgreSQL password) |
| `JWT_SECRET` | JWT signing key | **Min 32 karakter!** |
| `ENCRYPTION_KEY` | AES-256 encryption key | 32-byte hex string |
| `GHCR_PAT` | GitHub Container Registry PAT | `ghp_xxxxx...` |

### GitHub PAT Oluşturma (GHCR)

```bash
# GitHub → Settings → Developer Settings → Personal Access Tokens → Tokens (classic)

1. "Generate new token (classic)"
2. Note: "sellerx-ghcr-deploy"
3. Expiration: 90 days veya No expiration
4. Scopes:
   ✓ read:packages
   ✓ write:packages
   ✓ delete:packages
5. Generate → Copy token
6. GitHub repo → Settings → Secrets → Add: GHCR_PAT
```

### SSH Key Setup

```bash
# 1. Lokal makinede key oluştur
ssh-keygen -t ed25519 -C "github-actions-sellerx" -f ~/.ssh/github_deploy -N ""

# 2. Public key'i sunucuya ekle
ssh-copy-id -i ~/.ssh/github_deploy.pub root@157.180.78.53

# 3. Test et
ssh -i ~/.ssh/github_deploy root@157.180.78.53 "echo 'SSH OK'"

# 4. Private key'i GitHub Secrets'a ekle
cat ~/.ssh/github_deploy
# Copy entire output (including BEGIN/END lines) → GitHub Secrets → SSH_PRIVATE_KEY
```

---

## 🚢 Deployment Akışı

### Workflow Dosyaları

| Dosya | Tetikleyici | Açıklama |
|-------|-------------|----------|
| `.github/workflows/ci.yml` | PR, push | Build & test (all branches) |
| `.github/workflows/deploy-backend.yml` | `main` + `sellerx-backend/**` | Backend production deploy |
| `.github/workflows/deploy-frontend.yml` | `main` + `sellerx-frontend/**` | Frontend production deploy |

### 1. Otomatik Deployment (Önerilen ⭐)

```bash
# 1. Değişiklik yap
git checkout -b feature/new-feature
# ... code changes ...

# 2. Commit ve push
git add .
git commit -m "feat: add new feature"
git push origin feature/new-feature

# 3. Pull Request aç
gh pr create --title "feat: Add new feature" --body "..."

# 4. PR merge edilince main branch'e push
gh pr merge --auto --squash

# 5. GitHub Actions otomatik tetiklenir!
# Backend değişikliği → deploy-backend.yml çalışır
# Frontend değişikliği → deploy-frontend.yml çalışır
```

**GitHub Actions Progress:**
```bash
# Web UI'den izle
https://github.com/nethunterzist/sellerx/actions

# Veya CLI ile
gh run watch
gh run list --workflow=deploy-backend.yml
```

### 2. Manuel Deployment

```bash
# Specific workflow çalıştır
gh workflow run deploy-backend.yml
gh workflow run deploy-frontend.yml

# Veya GitHub web arayüzünden:
# Actions → Deploy Backend/Frontend → Run workflow → Run
```

### Deployment Aşamaları (Otomatik)

```yaml
1. Checkout Code ✓
2. Setup Docker Buildx ✓
3. Login to GHCR ✓
4. Build Docker Image ✓
5. Push to GHCR ✓
6. SSH to Server ✓
7. Tag Current Image as Rollback ✓
8. Pull New Image ✓
9. Stop Old Container ✓
10. Start New Container ✓
11. Health Check (60s wait + 15 retries × 10s) ✓
12. Rollback if Failed (automatic) ✓
13. Cleanup Old Images ✓
```

---

## ✅ Production Readiness Checklist

**Bu checklist'i her deployment öncesi kontrol edin!**

### 📦 Code Quality

- [ ] All tests passing locally
  ```bash
  # Backend
  cd sellerx-backend && ./mvnw test

  # Frontend
  cd sellerx-frontend && npm test -- --run
  ```
- [ ] TypeScript compilation successful
  ```bash
  npx tsc --noEmit
  ```
- [ ] ESLint no errors
  ```bash
  npm run lint
  ```
- [ ] No console.log/console.error in production code

### 🔧 Configuration

- [ ] All GitHub Secrets defined
  ```bash
  # Check required secrets
  gh secret list | grep -E "SSH_PRIVATE_KEY|DB_PASSWORD|JWT_SECRET|ENCRYPTION_KEY|GHCR_PAT"
  ```
- [ ] Environment variables documented
  - Backend: Check `application.yaml` required vars
  - Frontend: Check `.env.example` vs production
- [ ] CORS origins configured correctly
  ```yaml
  # application.yaml
  cors:
    allowed-origins: https://sellerx.157.180.78.53.sslip.io
  ```
- [ ] Health checks validated
  ```yaml
  management:
    health:
      db: enabled: true
      rabbit: enabled: false  # ✅ Optional service disabled
      mail: enabled: false    # ✅ Optional service disabled
  ```

### 🏗️ Infrastructure

- [ ] Database accessible from server
  ```bash
  ssh root@157.180.78.53 "docker exec postgres-container pg_isready"
  ```
- [ ] Network connectivity verified (Coolify network)
- [ ] Traefik labels correct in deployment script
- [ ] No port conflicts

### 🧪 Testing

- [ ] Local Docker build successful
  ```bash
  # Backend
  cd sellerx-backend && docker build -t sellerx-backend:test .

  # Frontend
  cd sellerx-frontend && docker build -f Dockerfile.production -t sellerx-frontend:test .
  ```
- [ ] `npm ci` tested (not just `npm install`)
  ```bash
  rm -rf node_modules package-lock.json
  npm install
  # Then test: npm ci
  ```
- [ ] Flyway migrations reversible (if needed)
- [ ] No hardcoded localhost URLs in production code

### 🔄 Rollback Ready

- [ ] Previous version tagged as `:rollback`
- [ ] Database migrations backward compatible
- [ ] Rollback script tested

### ⚠️ Critical Checks (MUST PASS!)

- [ ] **JWT_SECRET is 32+ characters** (shorter = 500 error on login!)
- [ ] **Optional services disabled in health check**
  ```yaml
  # RabbitMQ not available? Disable:
  management.health.rabbit.enabled: false

  # Mail not configured? Disable:
  management.health.mail.enabled: false
  ```
- [ ] **package.json + package-lock.json both committed**
- [ ] **Application class name correct in health check**
  - `Started StoreApplication` NOT `Started SellerxApplication`

---

## 🏥 Health Check & Doğrulama

### Production URLs

| Servis | URL |
|--------|-----|
| Frontend | https://sellerx.157.180.78.53.sslip.io |
| Backend | https://uwgss4kkocg4kks8880kwwwo.157.180.78.53.sslip.io |
| Backend Health | https://uwgss4kkocg4kks8880kwwwo.157.180.78.53.sslip.io/actuator/health |
| Coolify Panel | http://157.180.78.53:8000 |

### Health Check

```bash
# 1. Backend health
curl -k https://uwgss4kkocg4kks8880kwwwo.157.180.78.53.sslip.io/actuator/health

# Expected:
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"}
  }
}

# 2. Frontend access
curl -k -I https://sellerx.157.180.78.53.sslip.io
# Expected: HTTP/2 200
```

### Container Status

```bash
# SSH to server
ssh root@157.180.78.53

# List containers
docker ps | grep sellerx

# Expected:
# sellerx-backend   Up X minutes   (healthy)
# sellerx-frontend  Up X minutes

# Check logs
docker logs sellerx-backend --tail 50
# Should see: "Started StoreApplication in XX seconds"

docker logs sellerx-frontend --tail 20
# Should see: "Ready in XXXms"
```

### GitHub Actions Status

```bash
# Recent runs
gh run list --limit 5

# Specific workflow
gh run list --workflow=deploy-backend.yml

# Watch live
gh run watch

# View logs
gh run view <run-id> --log
```

---

## 🔄 Rollback Prosedürü

### Otomatik Rollback

Deployment health check fail ederse **otomatik rollback** çalışır:

```yaml
# deploy-backend.yml
- Health check failed
  ↓
- Show container logs
  ↓
- Check if rollback image exists
  ↓
- Stop failed container
  ↓
- Start rollback container (ghcr.io/.../backend:rollback)
  ↓
- Verify rollback container running
  ↓
- Exit with failure (mark deployment failed)
```

### Manuel Rollback

```bash
# 1. SSH to server
ssh root@157.180.78.53

# 2. Check available images
docker images | grep sellerx

# 3. Backend rollback
docker stop sellerx-backend && docker rm sellerx-backend

docker run -d \
  --name sellerx-backend \
  --network coolify \
  --restart unless-stopped \
  -e DB_HOST=uws408w88ko044c84gwgkw04 \
  -e DB_PORT=5432 \
  -e DB_NAME=sellerx_db \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=<password> \
  -e JWT_SECRET=<jwt-secret> \
  -e ENCRYPTION_KEY=<encryption-key> \
  -e SERVER_PORT=8080 \
  -e SPRING_PROFILES_ACTIVE=production \
  -e CORS_ALLOWED_ORIGINS=https://sellerx.157.180.78.53.sslip.io \
  -e WEBHOOK_BASE_URL=https://uwgss4kkocg4kks8880kwwwo.157.180.78.53.sslip.io \
  -e FRONTEND_URL=https://sellerx.157.180.78.53.sslip.io \
  -l traefik.enable=true \
  -l 'traefik.http.routers.sellerx-backend.rule=Host(`uwgss4kkocg4kks8880kwwwo.157.180.78.53.sslip.io`)' \
  -l traefik.http.routers.sellerx-backend.entrypoints=https \
  -l traefik.http.routers.sellerx-backend.tls=true \
  -l traefik.http.services.sellerx-backend.loadbalancer.server.port=8080 \
  ghcr.io/nethunterzist/sellerx/backend:rollback

# 4. Verify
docker ps | grep sellerx-backend
docker logs sellerx-backend --tail 20
```

### Rollback to Specific Version

```bash
# 1. Find specific commit SHA from git history
git log --oneline -10

# 2. Pull that version's image
docker pull ghcr.io/nethunterzist/sellerx/backend:<commit-sha>

# 3. Use that image in docker run command
```

---

## 🐛 Troubleshooting

### Problem 1: GitHub Actions Başarısız

#### Kontrol Listesi:

1. **Actions tab'ını kontrol et**
   ```bash
   gh run list --workflow=deploy-backend.yml --limit 5
   gh run view <run-id> --log
   ```

2. **Secrets tanımlı mı?**
   ```bash
   gh secret list
   # Check: SSH_PRIVATE_KEY, DB_PASSWORD, JWT_SECRET, ENCRYPTION_KEY, GHCR_PAT
   ```

3. **Branch doğru mu?**
   ```bash
   git branch --show-current
   # Must be: main
   ```

4. **Path doğru mu?**
   ```bash
   git diff --name-only origin/main | grep -E "sellerx-(backend|frontend)"
   # Should show files in correct directory
   ```

#### Yaygın Hatalar:

| Hata | Sebep | Çözüm |
|------|-------|-------|
| `npm ERR! ERESOLVE` | package-lock.json outdated | `npm install && git add package-lock.json` |
| `Permission denied (publickey)` | SSH key yanlış | SSH_PRIVATE_KEY secret'ı kontrol et |
| `unauthorized: authentication required` | GHCR PAT invalid | Yeni PAT oluştur, GHCR_PAT güncelle |
| `Health check timeout` | App startup > 210s | Artır: `MAX_HEALTH_RETRIES=15` |

### Problem 2: Container Başlamıyor

```bash
# 1. Container loglarını kontrol et
docker logs sellerx-backend --tail 200

# 2. En yaygın hatalar:

# ❌ Database connection refused
#    → DB_HOST yanlış mı? Container aynı network'te mi?
#    → docker network ls && docker network inspect coolify

# ❌ JWT secret too short
#    → Error: "The specified key byte array is X bits which is not secure enough"
#    → JWT_SECRET min 32 karakter olmalı!

# ❌ Port already in use
#    → Başka container aynı portu kullanıyor mu?
#    → docker ps | grep :8080

# ❌ Application fails to start: "Failed to bind properties"
#    → Required env var eksik
#    → Check: docker inspect sellerx-backend | grep -A50 "Env"
```

### Problem 3: Health Check Fail

```bash
# Backend health endpoint DOWN

# 1. Check individual health indicators
curl -k https://uwgss4kkocg4kks8880kwwwo.157.180.78.53.sslip.io/actuator/health

# 2. Common issues:

# ❌ RabbitMQ health DOWN (but RabbitMQ not available)
#    Solution: Disable in application.yaml
#    management.health.rabbit.enabled: false

# ❌ Mail health DOWN (but mail not configured)
#    Solution: Disable in application.yaml
#    management.health.mail.enabled: false

# ❌ DB health DOWN
#    Solution: Check PostgreSQL container
#    docker exec postgres-container pg_isready
```

### Problem 4: Frontend Build Fails

```bash
# ❌ npm ci fails with version mismatch

# Solution:
cd sellerx-frontend
rm -rf node_modules package-lock.json
npm install
git add package-lock.json
git commit -m "fix: Regenerate package-lock.json"
git push

# ❌ TypeScript errors
npx tsc --noEmit
# Fix all errors before deployment

# ❌ ESLint errors
npm run lint -- --fix
```

### Problem 5: Rollback Failed

```bash
# ❌ Rollback image not found

# Check available images:
docker images | grep sellerx

# If no rollback image:
# 1. Find previous working commit
git log --oneline -10

# 2. Manually trigger deployment of that commit
gh workflow run deploy-backend.yml --ref <commit-sha>
```

---

## ⚠️ Bilinen Hatalar ve Çözümler

**Bu bölüm production deployment sırasında yaşanan gerçek hatalardan oluşturulmuştur.**

Detaylı analiz: `docs/deployment/DEPLOYMENT_ERRORS_ANALYSIS.md`

### Hata #1: RabbitMQ Health Indicator

**Durum:** Container başladı ama `UNHEALTHY`, health endpoint 503

**Root Cause:**
```java
// RabbitMQ config her zaman aktif
@Configuration
public class RabbitMQConfig { ... }

// Production'da RabbitMQ YOK!
// Spring Boot bağlanamaz → Health DOWN
```

**Çözüm:**
```java
@Configuration
@ConditionalOnProperty(
    name = "spring.rabbitmq.enabled",
    havingValue = "true",
    matchIfMissing = false  // Default: disabled
)
public class RabbitMQConfig { ... }
```

```yaml
# application.yaml
management:
  health:
    rabbit:
      enabled: false
```

**Prevention:**
- Tüm optional services için `@ConditionalOnProperty` kullan
- Health indicator'ları optional services için disable et
- Production checklist'te kontrol et

### Hata #2: Health Check Timeout

**Durum:** Deployment 55 saniyede timeout, ama app 70 saniyede başlıyor

**Root Cause:**
```yaml
# Yetersiz bekleme süresi
sleep 30
for i in 1 2 3 4 5; do  # 5 × 5s = 25s
  curl /health && break
  sleep 5
done
# Toplam: 30 + 25 = 55s
# App startup: ~70s ❌
```

**Çözüm:**
```yaml
sleep 60  # Production cold start
MAX_HEALTH_RETRIES=15
HEALTH_RETRY_DELAY=10
# Toplam: 60 + (15 × 10) = 210s ✅
```

**Prevention:**
- Production startup süresini ölç ve 2x buffer ekle
- Health check timeout'u yeterince uzun tut

### Hata #3: package-lock.json Conflict

**Durum:** Frontend build fails: `npm ERR! ERESOLVE`

**Root Cause:**
```bash
# Developer:
npm install next@latest  # package.json updated
git add package.json     # ❌ package-lock.json forgotten!
git commit

# CI/CD:
npm ci  # Strict mode, fails on mismatch
```

**Çözüm:**
```bash
rm -rf node_modules package-lock.json
npm install
git add package.json package-lock.json  # ✅ Both
```

**Prevention:**
- Git pre-commit hook:
  ```bash
  # .git/hooks/pre-commit
  if git diff --cached --name-only | grep -q "package.json"; then
    if ! git diff --cached --name-only | grep -q "package-lock.json"; then
      echo "ERROR: package.json changed but package-lock.json didn't"
      exit 1
    fi
  fi
  ```

### Hata #4: JWT Secret Too Short

**Durum:** Login returns 500 (not 401!)

**Root Cause:**
```java
// JWT_SECRET < 32 characters
// HMAC-SHA256 requires min 256 bits (32 bytes)

Error: "The specified key byte array is 128 bits which is not secure enough"
```

**Çözüm:**
```bash
# Generate 32+ character secret
openssl rand -base64 32
# Or: Use long password (32+ chars)
```

**Prevention:**
- Document minimum length requirements
- Validate secret length at startup

### Hata #5: Application Name Mismatch

**Durum:** Log-based health check timeout

**Root Cause:**
```bash
# Health check:
docker logs | grep "Started SellerxApplication"
# ❌ Never matches!

# Actual log:
"Started StoreApplication in 65.2 seconds"
```

**Çözüm:**
```bash
# Use correct class name
docker logs | grep "Started StoreApplication"
```

**Prevention:**
- Test health check script locally before deployment
- Use exact log message from actual startup

---

## 📚 Ek Kaynaklar

### Dokümantasyon

- [CI/CD Guide (this file)](#)
- [Deployment Errors Analysis](./DEPLOYMENT_ERRORS_ANALYSIS.md) - Detaylı hata analizi
- [GitHub Actions Workflows](../../.github/workflows/) - Workflow dosyaları
- [Architecture Docs](../architecture/) - Sistem mimarisi

### External Links

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Docker Documentation](https://docs.docker.com/)
- [Coolify Documentation](https://coolify.io/docs)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

### GitHub Workflow URLs

- [CI Workflow](https://github.com/nethunterzist/sellerx/blob/main/.github/workflows/ci.yml)
- [Backend Deploy](https://github.com/nethunterzist/sellerx/blob/main/.github/workflows/deploy-backend.yml)
- [Frontend Deploy](https://github.com/nethunterzist/sellerx/blob/main/.github/workflows/deploy-frontend.yml)

---

**Maintenance:** Bu döküman her major deployment change'de güncellenmeli.
**Last Major Update:** 16 Şubat 2026 - Production deployment errors analysis eklendi
**Next Review:** Mart 2026
