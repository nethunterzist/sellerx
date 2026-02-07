# SellerX GitHub Actions CI/CD Pipeline

Bu doküman, SellerX projesinin GitHub Actions tabanlı CI/CD pipeline'ını detaylı olarak açıklar.

---

## 1. Genel Bakış

### 1.1 Mimari Diyagramı

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                           GitHub Actions Workflow                             │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│   ┌─────────────┐    ┌─────────────────┐    ┌─────────────────────────────┐ │
│   │   Trigger   │───▶│  GitHub Runner  │───▶│  GitHub Container Registry  │ │
│   │  git push   │    │  ubuntu-latest  │    │         (GHCR)              │ │
│   │  main/paths │    │    7GB RAM      │    │  ghcr.io/nethunterzist/     │ │
│   └─────────────┘    └─────────────────┘    └──────────────┬──────────────┘ │
│                                                             │                 │
└─────────────────────────────────────────────────────────────┼─────────────────┘
                                                              │
                                                              │ SSH Deploy
                                                              ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                        Hetzner Server (157.180.78.53)                         │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │                         Coolify Network                              │   │
│   │                                                                       │   │
│   │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────┐ │   │
│   │  │ sellerx-frontend│  │ sellerx-backend │  │ PostgreSQL (Coolify)│ │   │
│   │  │   Next.js 15    │  │ Spring Boot 3.4 │  │  uws408w88ko044...  │ │   │
│   │  │   Port: 3000    │──│   Port: 8080    │──│    Port: 5432       │ │   │
│   │  └────────┬────────┘  └────────┬────────┘  └─────────────────────┘ │   │
│   │           │                    │                                     │   │
│   └───────────┼────────────────────┼─────────────────────────────────────┘   │
│               │                    │                                         │
│   ┌───────────▼────────────────────▼─────────────────────────────────────┐   │
│   │                         Traefik Reverse Proxy                         │   │
│   │  sellerx.157.180.78.53.sslip.io      → :3000 (Frontend)              │   │
│   │  uwgss4kkocg4kks8880kwwwo...sslip.io → :8080 (Backend)               │   │
│   └───────────────────────────────────────────────────────────────────────┘   │
│                                                                               │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Bu Mimari Neden Seçildi?

| Sorun | Önceki Durum | Çözüm |
|-------|--------------|-------|
| **RAM Yetersizliği** | Sunucuda 3.7GB RAM var, Next.js build 4GB+ gerektiriyor | GitHub Actions runner'da build (7GB RAM) |
| **Private Repo** | Coolify private repo'dan çekemiyordu | GHCR ile public image registry |
| **Cross-Platform** | Mac ARM → Linux AMD64 build sorunu | GitHub runner'da native AMD64 build |
| **Uzun Deploy Süresi** | Manuel deploy ~40 dakika | Otomatik deploy ~4-5 dakika |

### 1.3 Akış Özeti

```
1. Developer → git push main (sellerx-backend/** veya sellerx-frontend/**)
2. GitHub Actions trigger olur
3. GitHub Runner'da Docker image build edilir
4. Image GHCR'a push edilir (ghcr.io/nethunterzist/sellerx/...)
5. SSH ile sunucuya bağlanılır
6. Eski container durdurulur, yeni image çekilir
7. Yeni container başlatılır (Traefik labels ile)
8. Health check ve log kontrolü yapılır
```

---

## 2. Workflow Dosyaları

### 2.1 Backend Workflow

**Dosya**: `.github/workflows/deploy-backend.yml`

```yaml
name: Deploy Backend

on:
  push:
    branches: [main]
    paths: ['sellerx-backend/**']  # Sadece backend değişikliklerinde çalışır
  workflow_dispatch:                 # Manuel tetikleme

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}/backend

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest          # 7GB RAM, 2 CPU
    permissions:
      contents: read
      packages: write               # GHCR'a push için gerekli

    steps:
      # 1. Kodu çek
      - name: Checkout code
        uses: actions/checkout@v4

      # 2. Docker Buildx kur (multi-platform için)
      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      # 3. GHCR'a login
      - name: Login to GHCR
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      # 4. Image tag'leri hazırla
      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=sha,prefix=        # commit hash (abc1234)
            type=raw,value=latest   # latest tag

      # 5. Build ve Push (GitHub Actions cache ile)
      - name: Build and Push
        uses: docker/build-push-action@v5
        with:
          context: ./sellerx-backend
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          cache-from: type=gha      # GitHub Actions cache
          cache-to: type=gha,mode=max

      # 6. SSH ile sunucuya deploy
      - name: Deploy to Server
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: 157.180.78.53
          username: root
          password: ${{ secrets.SERVER_PASSWORD }}
          script: |
            # Container yönetimi ve başlatma (aşağıda detaylı)
```

### 2.2 Frontend Workflow

**Dosya**: `.github/workflows/deploy-frontend.yml`

```yaml
name: Deploy Frontend

on:
  push:
    branches: [main]
    paths: ['sellerx-frontend/**']
  workflow_dispatch:

# Build sırasında NEXT_PUBLIC_API_BASE_URL gerekli
- name: Build and Push
  uses: docker/build-push-action@v5
  with:
    context: ./sellerx-frontend
    file: ./sellerx-frontend/Dockerfile.production
    build-args: |
      NEXT_PUBLIC_API_BASE_URL=https://uwgss4kkocg4kks8880kwwwo.157.180.78.53.sslip.io
```

### 2.3 Trigger Kuralları

| Trigger | Açıklama |
|---------|----------|
| `push: branches: [main]` | Sadece main branch'e push'ta çalışır |
| `paths: ['sellerx-backend/**']` | Sadece belirtilen klasördeki değişikliklerde çalışır |
| `workflow_dispatch` | GitHub Actions UI'dan manuel tetikleme |

---

## 3. Docker Yapılandırması

### 3.1 Backend Dockerfile

**Dosya**: `sellerx-backend/Dockerfile`

```dockerfile
# Stage 1: Builder - Maven ile build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Maven wrapper ve pom.xml kopyala (dependency cache için)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw

# Dependency'leri indir (cache'lenir)
RUN ./mvnw dependency:go-offline -B

# Source kopyala ve build
COPY src/ src/
RUN ./mvnw -DskipTests clean package

# Stage 2: Runtime - Sadece JRE ile çalıştır
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Güvenlik: Non-root user
RUN addgroup --system --gid 1001 spring && \
    adduser --system --uid 1001 spring --ingroup spring

# JAR kopyala
COPY --from=builder /app/target/*.jar app.jar
RUN chown spring:spring app.jar
USER spring

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# JVM optimizasyonları
CMD ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

**Önemli Notlar**:
- `eclipse-temurin:21-jdk-alpine` → Build için JDK
- `eclipse-temurin:21-jre-alpine` → Runtime için sadece JRE (daha küçük)
- `dependency:go-offline` → Dependency cache katmanı oluşturur
- `MaxRAMPercentage=75.0` → Container RAM'inin %75'ini kullanır

### 3.2 Frontend Dockerfile

**Dosya**: `sellerx-frontend/Dockerfile.production`

```dockerfile
# Stage 1: Dependencies
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci --only=production && npm cache clean --force

# Stage 2: Builder
FROM node:20-alpine AS builder
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci
COPY . .
RUN npm run build

# Stage 3: Runner
FROM node:20-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production

# Güvenlik: Non-root user
RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

# Standalone build çıktılarını kopyala
COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs
EXPOSE 3000

ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

CMD ["node", "server.js"]
```

**Önemli Notlar**:
- `standalone` output mode kullanılıyor (next.config.js'de ayarlanmış)
- 3-stage build ile minimal image boyutu
- Production dependencies ayrı layer'da cache'lenir

### 3.3 Image Boyutları

| Image | Boyut | Açıklama |
|-------|-------|----------|
| Backend | ~250MB | JRE + Spring Boot JAR |
| Frontend | ~150MB | Node.js Alpine + Standalone build |

---

## 4. Environment Variables

### 4.1 Backend Environment Variables (17 adet)

```bash
# Database Bağlantısı
DB_HOST=uws408w88ko044c84gwgkw04        # Coolify PostgreSQL container adı
DB_PORT=5432
DB_NAME=sellerx_db
DB_USERNAME=postgres
DB_PASSWORD=L6CAVfFloyZIYbvxqJR6k0dM8E79SalonpemTqs3oPBMMKwYIjrhAM7sLsVV9YZB

# JWT Authentication
JWT_SECRET=sellerx-production-jwt-secret-key-2026-minimum-256-bits
# NOT: Minimum 32 karakter olmalı! Aksi halde 500 Internal Server Error alırsınız.

# Server
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=production

# Flyway Migration
SPRING_FLYWAY_VALIDATE_ON_MIGRATE=false
# NOT: Bu ayar zorunludur! Aksi halde checksum mismatch hatası alırsınız.

# CORS
CORS_ALLOWED_ORIGINS=https://sellerx.157.180.78.53.sslip.io

# Webhook
WEBHOOK_BASE_URL=https://uwgss4kkocg4kks8880kwwwo.157.180.78.53.sslip.io
WEBHOOK_API_KEY=webhook-api-key-placeholder
WEBHOOK_SIGNATURE_SECRET=webhook-signature-secret-placeholder

# 3rd Party Entegrasyonlar
IYZICO_CALLBACK_URL=https://sellerx.157.180.78.53.sslip.io/api/payment/callback
FRONTEND_URL=https://sellerx.157.180.78.53.sslip.io
PARASUT_REDIRECT_URI=https://sellerx.157.180.78.53.sslip.io/api/parasut/callback
```

### 4.2 Frontend Environment Variables (2 adet)

```bash
# Runtime'da backend'e bağlantı (container-to-container)
API_BASE_URL=http://sellerx-backend:8080

# Node.js
NODE_ENV=production
```

**NOT**: `NEXT_PUBLIC_API_BASE_URL` build-time'da verilir (Dockerfile build-args).

### 4.3 GitHub Secrets

Repository Settings → Secrets and variables → Actions

| Secret | Açıklama |
|--------|----------|
| `SERVER_PASSWORD` | Sunucu root şifresi (SSH erişimi için) |
| `GITHUB_TOKEN` | Otomatik sağlanır (GHCR erişimi için) |

---

## 5. Deployment Süreci (Adım Adım)

### 5.1 Build Aşaması (GitHub Actions)

```
1. Checkout code (actions/checkout@v4)
2. Setup Docker Buildx (multi-platform desteği)
3. Login to GHCR (github.actor + GITHUB_TOKEN)
4. Extract metadata (sha tag + latest tag)
5. Build Docker image
   - Cache: GitHub Actions cache (type=gha)
   - Context: ./sellerx-backend veya ./sellerx-frontend
6. Push to GHCR
   - ghcr.io/nethunterzist/sellerx/backend:latest
   - ghcr.io/nethunterzist/sellerx/frontend:latest
```

### 5.2 SSH Deploy Aşaması

```bash
# 1. Eski container durdur ve sil
docker stop sellerx-backend 2>/dev/null || true
docker rm sellerx-backend 2>/dev/null || true

# 2. Yeni image çek (public package)
docker pull ghcr.io/nethunterzist/sellerx/backend:latest

# 3. Yeni container başlat
docker run -d \
  --name sellerx-backend \
  --network coolify \                    # Coolify network'üne bağlan
  --restart unless-stopped \             # Crash'te otomatik restart
  -e DB_HOST=uws408w88ko044c84gwgkw04 \  # Environment variables
  ...
  -l traefik.enable=true \               # Traefik labels
  -l 'traefik.http.routers.sellerx-backend.rule=Host(`...`)' \
  ghcr.io/nethunterzist/sellerx/backend:latest

# 4. Health check ve log kontrolü
sleep 30
docker logs sellerx-backend --tail 20

# 5. Eski image'ları temizle
docker image prune -f
```

### 5.3 Traefik Entegrasyonu

Container'lar Traefik labels ile yapılandırılır:

```bash
# Backend Traefik Labels
-l traefik.enable=true
-l 'traefik.http.routers.sellerx-backend.rule=Host(`uwgss4kkocg4kks8880kwwwo.157.180.78.53.sslip.io`)'
-l traefik.http.routers.sellerx-backend.entrypoints=https
-l traefik.http.routers.sellerx-backend.tls=true
-l traefik.http.services.sellerx-backend.loadbalancer.server.port=8080

# Frontend Traefik Labels
-l traefik.enable=true
-l 'traefik.http.routers.sellerx-frontend.rule=Host(`sellerx.157.180.78.53.sslip.io`)'
-l traefik.http.routers.sellerx-frontend.entrypoints=https
-l traefik.http.routers.sellerx-frontend.tls=true
-l traefik.http.services.sellerx-frontend.loadbalancer.server.port=3000
```

---

## 6. Flyway Migration Yönetimi

### 6.1 Önemli: SPRING_FLYWAY_VALIDATE_ON_MIGRATE=false

Bu ayar **zorunludur** çünkü:

1. Production veritabanı Coolify ile oluşturuldu
2. Migration'lar manuel çalıştırıldı
3. `flyway_schema_history` tablosundaki checksum değerleri migration dosyalarıyla eşleşmiyor

Bu ayar olmadan şu hata alınır:
```
Migration checksum mismatch for migration version 1
-> Applied to database : 1
-> Resolved locally    : 674871629
```

### 6.2 Yeni Migration Ekleme Prosedürü

```bash
# 1. Lokal'de migration oluştur
# sellerx-backend/src/main/resources/db/migration/V94__description.sql

# 2. Lokal'de test et
cd sellerx-backend
./mvnw spring-boot:run

# 3. Git push yap (CI/CD otomatik deploy edecek)
git add .
git commit -m "feat: Add V94 migration - description"
git push origin main

# 4. Production'da Flyway otomatik çalışacak
# (VALIDATE_ON_MIGRATE=false olduğu için checksum kontrolü yapılmaz)
```

### 6.3 Flyway Schema History Düzeltme

Eğer migration history bozulursa:

```sql
-- Sunucuya SSH bağlan
ssh root@157.180.78.53

-- PostgreSQL container'a bağlan
docker exec -it uws408w88ko044c84gwgkw04 psql -U postgres -d sellerx_db

-- Mevcut durumu kontrol et
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;

-- Gerekirse satır ekle/güncelle
INSERT INTO flyway_schema_history
  (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES
  (94, '94', 'your description', 'SQL', 'V94__your_description.sql', 1, 'postgres', NOW(), 1, true);
```

---

## 7. Troubleshooting

### 7.1 Yaygın Hatalar ve Çözümleri

| Hata | Sebep | Çözüm |
|------|-------|-------|
| `500 Internal Server Error` on login | JWT_SECRET < 32 karakter | Min 32 karakter secret kullan |
| `Connection refused` to database | Yanlış DB_HOST | Container adını kontrol et |
| `Flyway checksum mismatch` | Migration history uyumsuz | `SPRING_FLYWAY_VALIDATE_ON_MIGRATE=false` |
| `Could not resolve placeholder` | Eksik env var | Tüm env var'ları kontrol et |
| Container sürekli restart | Uygulama crash | `docker logs` ile hata kontrol et |

### 7.2 Log Kontrol Komutları

```bash
# Sunucuya bağlan
ssh root@157.180.78.53

# Container durumunu kontrol et
docker ps -a --filter name=sellerx

# Son 50 log satırı
docker logs sellerx-backend --tail 50
docker logs sellerx-frontend --tail 50

# Canlı log takibi
docker logs -f sellerx-backend

# Hata filtrele
docker logs sellerx-backend 2>&1 | grep -E 'Error|Exception|failed'

# Başarılı başlangıç kontrolü
docker logs sellerx-backend 2>&1 | grep 'Started StoreApplication'
```

### 7.3 Container Restart Prosedürü

```bash
# Tek container restart
docker restart sellerx-backend

# Tamamen yeniden başlat
docker stop sellerx-backend
docker rm sellerx-backend
docker pull ghcr.io/nethunterzist/sellerx/backend:latest
docker run -d \
  --name sellerx-backend \
  --network coolify \
  --restart unless-stopped \
  -e DB_HOST=uws408w88ko044c84gwgkw04 \
  -e DB_PORT=5432 \
  ... (tüm env vars) ...
  ghcr.io/nethunterzist/sellerx/backend:latest
```

---

## 8. Sunucu Bilgileri

### 8.1 Bağlantı Bilgileri

| Bilgi | Değer |
|-------|-------|
| **Sunucu IP** | 157.180.78.53 |
| **SSH User** | root |
| **Coolify Dashboard** | http://157.180.78.53:8000 |

### 8.2 Container Bilgileri

| Container | Image | Port | Network |
|-----------|-------|------|---------|
| sellerx-backend | ghcr.io/nethunterzist/sellerx/backend:latest | 8080 | coolify |
| sellerx-frontend | ghcr.io/nethunterzist/sellerx/frontend:latest | 3000 | coolify |
| uws408w88ko044c84gwgkw04 | PostgreSQL 15 | 5432 | coolify |

### 8.3 URL'ler

| Servis | URL |
|--------|-----|
| **Frontend** | https://sellerx.157.180.78.53.sslip.io |
| **Backend API** | https://uwgss4kkocg4kks8880kwwwo.157.180.78.53.sslip.io |
| **Health Check** | https://uwgss4kkocg4kks8880kwwwo.157.180.78.53.sslip.io/actuator/health |

### 8.4 Network Yapısı

```
┌─────────────────────────────────────────────────────────────┐
│                      coolify network                         │
│                                                              │
│  Container Name          │ Internal DNS              │ Port │
│  ────────────────────────┼───────────────────────────┼──────│
│  sellerx-frontend        │ sellerx-frontend          │ 3000 │
│  sellerx-backend         │ sellerx-backend           │ 8080 │
│  uws408w88ko044c84gwgkw04│ uws408w88ko044c84gwgkw04  │ 5432 │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 9. Manuel Deploy Prosedürü

Acil durumlar için manuel deploy adımları.

### 9.1 Backend Manuel Deploy

```bash
# 1. Sunucuya bağlan
ssh root@157.180.78.53

# 2. Eski container durdur
docker stop sellerx-backend 2>/dev/null || true
docker rm sellerx-backend 2>/dev/null || true

# 3. Yeni image çek
docker pull ghcr.io/nethunterzist/sellerx/backend:latest

# 4. Container başlat (tüm env vars ile)
docker run -d \
  --name sellerx-backend \
  --network coolify \
  --restart unless-stopped \
  -e DB_HOST=uws408w88ko044c84gwgkw04 \
  -e DB_PORT=5432 \
  -e DB_NAME=sellerx_db \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=L6CAVfFloyZIYbvxqJR6k0dM8E79SalonpemTqs3oPBMMKwYIjrhAM7sLsVV9YZB \
  -e JWT_SECRET=sellerx-production-jwt-secret-key-2026-minimum-256-bits \
  -e SERVER_PORT=8080 \
  -e SPRING_PROFILES_ACTIVE=production \
  -e SPRING_FLYWAY_VALIDATE_ON_MIGRATE=false \
  -e CORS_ALLOWED_ORIGINS=https://sellerx.157.180.78.53.sslip.io \
  -e WEBHOOK_BASE_URL=https://uwgss4kkocg4kks8880kwwwo.157.180.78.53.sslip.io \
  -e WEBHOOK_API_KEY=webhook-api-key-placeholder \
  -e WEBHOOK_SIGNATURE_SECRET=webhook-signature-secret-placeholder \
  -e IYZICO_CALLBACK_URL=https://sellerx.157.180.78.53.sslip.io/api/payment/callback \
  -e FRONTEND_URL=https://sellerx.157.180.78.53.sslip.io \
  -e PARASUT_REDIRECT_URI=https://sellerx.157.180.78.53.sslip.io/api/parasut/callback \
  -l traefik.enable=true \
  -l 'traefik.http.routers.sellerx-backend.rule=Host(`uwgss4kkocg4kks8880kwwwo.157.180.78.53.sslip.io`)' \
  -l traefik.http.routers.sellerx-backend.entrypoints=https \
  -l traefik.http.routers.sellerx-backend.tls=true \
  -l traefik.http.services.sellerx-backend.loadbalancer.server.port=8080 \
  ghcr.io/nethunterzist/sellerx/backend:latest

# 5. Başlangıcı bekle ve kontrol et
sleep 40
docker logs sellerx-backend 2>&1 | grep 'Started StoreApplication'
```

### 9.2 Frontend Manuel Deploy

```bash
# 1. Sunucuya bağlan
ssh root@157.180.78.53

# 2. Eski container durdur
docker stop sellerx-frontend 2>/dev/null || true
docker rm sellerx-frontend 2>/dev/null || true

# 3. Yeni image çek
docker pull ghcr.io/nethunterzist/sellerx/frontend:latest

# 4. Container başlat
docker run -d \
  --name sellerx-frontend \
  --network coolify \
  --restart unless-stopped \
  -e API_BASE_URL=http://sellerx-backend:8080 \
  -e NODE_ENV=production \
  -l traefik.enable=true \
  -l 'traefik.http.routers.sellerx-frontend.rule=Host(`sellerx.157.180.78.53.sslip.io`)' \
  -l traefik.http.routers.sellerx-frontend.entrypoints=https \
  -l traefik.http.routers.sellerx-frontend.tls=true \
  -l traefik.http.services.sellerx-frontend.loadbalancer.server.port=3000 \
  ghcr.io/nethunterzist/sellerx/frontend:latest

# 5. Kontrol et
sleep 10
docker logs sellerx-frontend --tail 10
```

### 9.3 Lokal'den Image Build ve Push

GitHub Actions çalışmıyorsa lokal'den build:

```bash
# Backend
cd sellerx-backend
docker buildx build --platform linux/amd64 -t ghcr.io/nethunterzist/sellerx/backend:latest .

# GHCR login (Personal Access Token gerekli)
echo $GITHUB_TOKEN | docker login ghcr.io -u USERNAME --password-stdin

# Push
docker push ghcr.io/nethunterzist/sellerx/backend:latest

# Frontend
cd sellerx-frontend
docker buildx build --platform linux/amd64 \
  --build-arg NEXT_PUBLIC_API_BASE_URL=https://uwgss4kkocg4kks8880kwwwo.157.180.78.53.sslip.io \
  -f Dockerfile.production \
  -t ghcr.io/nethunterzist/sellerx/frontend:latest .

docker push ghcr.io/nethunterzist/sellerx/frontend:latest
```

---

## 10. Performans ve Maliyet

### 10.1 Build Süreleri

| Aşama | Süre (cache ile) | Süre (cache'siz) |
|-------|------------------|------------------|
| Backend Build | ~2-3 dk | ~5-6 dk |
| Frontend Build | ~2-3 dk | ~4-5 dk |
| SSH Deploy | ~30-40 sn | ~30-40 sn |
| **Toplam** | **~4-5 dk** | **~10-12 dk** |

### 10.2 GitHub Actions Limitleri

| Plan | Dakika/Ay | Mevcut Kullanım |
|------|-----------|-----------------|
| Free | 2,000 dk | Deploy başına ~5 dk |
| Team | 3,000 dk | - |
| Enterprise | 50,000 dk | - |

**Hesaplama**: Günde 5 deploy × 5 dk = 25 dk/gün × 30 = 750 dk/ay (Free tier yeterli)

---

## 11. Güvenlik Notları

### 11.1 Secrets Yönetimi

- `SERVER_PASSWORD` GitHub Secrets'ta saklanır
- `DB_PASSWORD` workflow dosyasında (değiştirilmeli)
- `JWT_SECRET` minimum 32 karakter olmalı

### 11.2 Öneriler

1. **DB_PASSWORD değiştir**: Workflow dosyasındaki şifre değiştirilmeli
2. **GitHub Secrets kullan**: Hassas bilgileri secrets'a taşı
3. **SSH Key kullan**: Password yerine SSH key authentication önerilir
4. **GHCR visibility**: Package'lar public, private yapmak için ayar gerekli

---

## 12. Gelecek İyileştirmeler

- [ ] GitHub Secrets'a tüm hassas bilgileri taşı
- [ ] SSH key authentication ekle
- [ ] Slack/Discord notification ekle
- [ ] Blue-green deployment implementasyonu
- [ ] Rollback mekanizması ekle
- [ ] Health check timeout'larını optimize et

---

**Son Güncelleme**: 2026-02-07
**Hazırlayan**: Claude Code
**Kaynak**: CI/CD kurulum süreci (GitHub Actions + Hetzner Server)
