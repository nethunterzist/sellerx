# Sellerx Development Environment

Bu proje Docker ile geliştirme ortamı kurulumu için hazırlanmıştır. Tek komutla tüm servisleri (Frontend, Backend, Database) çalıştırabilirsiniz.

## 🚀 Hızlı Başlangıç

### Gereksinimler

- Docker Desktop
- Git

### Kurulum ve Çalıştırma

1. **Projeyi klonlayın:**

```bash
git clone https://github.com/semdin/sellerx-frontend.git
git clone https://github.com/semdin/sellerx-backend.git
```

2. **Docker Desktop'ın çalıştığından emin olun**

3. **Geliştirme ortamını başlatın:**

```bash
# İlk defa çalıştırıyorsanız (build ile)
docker-compose -f docker-compose.dev.yml up --build -d

# Sonraki çalıştırmalarda
docker-compose -f docker-compose.dev.yml up -d
```

4. **Servislere erişim:**

- 🌐 Frontend: http://localhost:3000
- 🔧 Backend API: http://localhost:8080
- 🗄️ Database: localhost:5432 (postgres/123123)

## 🚀 Geliştirici Workflow'u

### 1️⃣ Günlük Geliştirme Rutini

```bash
# 1. Sistemi başlat
docker-compose -f docker-compose.dev.yml up -d

# 2. Logları ayrı terminal'de izle
docker-compose -f docker-compose.dev.yml logs -f backend

# 3. VS Code workspace'i aç
code Sellerx.code-workspace

# 4. Kod değiştirdikten sonra hızlı restart
docker-compose -f docker-compose.dev.yml restart backend

# 5. Dev Container'da çalışmak için (opsiyonel)
# Ctrl+Shift+P → "Dev Containers: Reopen in Container"
```

### ⚡ Süper Hızlı Backend Update (5-10 saniye)

```bash
# En sık kullanacağın komut - sadece backend'i restart et
docker-compose -f docker-compose.dev.yml restart backend

# Log'u ayrı terminalde izle
docker-compose -f docker-compose.dev.yml logs -f backend

# "Started StoreApplication in X.XXX seconds" mesajını bekle
```

**🎯 Ne zaman hangi komutu kullan:**

| Değişiklik Türü           | Komut                 | Süre        |
| ------------------------- | --------------------- | ----------- |
| 📝 Sadece kod değişikliği | `restart backend`     | 5-10 saniye |
| 🆕 Yeni Java class/file   | `up --build backend`  | 1-2 dakika  |
| 🔧 pom.xml değişikliği    | `up --build backend`  | 1-2 dakika  |
| 🗄️ Database migration     | `restart backend`     | 5-10 saniye |
| 🌐 Frontend değişikliği   | Otomatik (hot reload) | 1-2 saniye  |

### 2️⃣ Hot Reload Test

```bash
# Frontend'de değişiklik yap ve şu mesajları bekle:
# ✓ Compiled in 526ms (763 modules)
# GET /tr/dashboard 200 in 768ms

# Backend'de değişiklik yap ve şu mesajları bekle:
# Restarting due to 1 class changes
# Started SellerxBackendApplication in X.XXX seconds
```

### 3️⃣ Performance Monitoring

```bash
# Resource kullanımını sürekli izle
docker stats sellerx-frontend sellerx-backend sellerx-postgres

# Sadece CPU ve Memory
docker stats --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}"

# Network trafiği
docker stats --format "table {{.Name}}\t{{.NetIO}}\t{{.BlockIO}}"
```

## 📋 Kullanışlı Komutlar

### Servisleri durdurma:

```bash
docker-compose -f docker-compose.dev.yml down
```

### ⚡ Hızlı Backend Güncelleme (Yeni Kod Ekledikten Sonra)

**En hızlı yöntem** - Sadece backend container'ını yeniden başlat:

```bash
# 1. Sadece backend'i durdur ve yeniden başlat (5-10 saniye)
docker-compose -f docker-compose.dev.yml restart backend

# 2. Eğer yeni Java class'ı eklediysen, sadece backend'i rebuild et (1-2 dakika)
docker-compose -f docker-compose.dev.yml up --build backend

# 3. En hızlı - Sadece backend container'ını değiştir
docker-compose -f docker-compose.dev.yml stop backend
docker-compose -f docker-compose.dev.yml up -d backend --build
```

**⚠️ Ne zaman restart, ne zaman rebuild?**

- 📝 **Sadece kod değişikliği** → `restart backend` (5-10 saniye)
- 🆕 **Yeni class/file ekleme** → `up --build backend` (1-2 dakika)
- 🔧 **pom.xml değişikliği** → `up --build backend` (1-2 dakika)
- 🗄️ **Database migration** → `restart backend` (migration otomatik çalışır)

**💡 Pro Tip - Development Workflow:**

```bash
# Terminal 1: Log izle
docker-compose -f docker-compose.dev.yml logs -f backend

# Terminal 2: Kod değiştirdikten sonra
docker-compose -f docker-compose.dev.yml restart backend

# 5-10 saniye bekle, Terminal 1'de şu mesajı gör:
# "Started StoreApplication in X.XXX seconds"
```

### 🔄 Sürekli Log İzleme (Hot Reload İçin Önemli!)

**En pratik yöntem** - Ayrı terminal açıp sürekli izleyin:

```bash
# Frontend loglarını sürekli izle (Hot Reload için)
docker-compose -f docker-compose.dev.yml logs -f frontend

# Tüm servislerin loglarını birlikte izle
docker-compose -f docker-compose.dev.yml logs -f

# Backend loglarını izle
docker-compose -f docker-compose.dev.yml logs -f backend
```

⚠️ **İpucu**: Bu komutları ayrı terminal penceresinde çalıştırın, böylece kod değişikliklerini anlık görebilirsiniz!

### 🚀 Backend Development Hızlandırma İpuçları

**1. Incremental Build İçin:**

```bash
# İlk build'den sonra, cache kullan
docker-compose -f docker-compose.dev.yml build --no-cache backend  # Sadece ilk defa
docker-compose -f docker-compose.dev.yml build backend             # Sonraki build'ler hızlı
```

**2. Volume Mounting ile Canlı Development:**

```bash
# Backend'de kod değiştirince otomatik restart için
# docker-compose.dev.yml'de volume mapping var:
# - ./sellerx-backend/src:/app/src
# Spring Boot DevTools bunu algılar ve restart eder
```

**3. Database Migration Sonrası:**

```bash
# Migration eklediysen sadece restart yeter
docker-compose -f docker-compose.dev.yml restart backend

# Ya da sadece backend container'ını yenile
docker exec sellerx-backend pkill java && docker-compose -f docker-compose.dev.yml up -d backend
```

**4. En Hızlı Test Workflow:**

```bash
# Terminal 1: Backend log izle
docker-compose -f docker-compose.dev.yml logs -f backend

# Terminal 2: VS Code'da kod değiştir

# Terminal 3: Hızlı restart
docker-compose -f docker-compose.dev.yml restart backend

# Terminal 1'de "Started StoreApplication" mesajını bekle (5-10 saniye)
# Terminal 4: API test et
curl http://localhost:8080/products/store/{store_id}
```

### Logları görüntüleme:

```bash
# Son 50 log satırı
docker-compose -f docker-compose.dev.yml logs --tail 50

# Belirli zaman aralığındaki loglar
docker-compose -f docker-compose.dev.yml logs --since "2024-01-01T00:00:00"

# Logları dosyaya kaydet
docker-compose -f docker-compose.dev.yml logs > debug.log

# Anlık log görüntüleme (frontend)
docker-compose -f docker-compose.dev.yml logs frontend -f

# Anlık log görüntüleme (backend)
docker-compose -f docker-compose.dev.yml logs backend -f

# Anlık log görüntüleme (tümü)
docker-compose -f docker-compose.dev.yml logs -f

```

### Servisleri yeniden build etme:

```bash
docker-compose -f docker-compose.dev.yml up --build
```

### Veritabanını sıfırlama:

```bash
docker-compose -f docker-compose.dev.yml down -v
docker-compose -f docker-compose.dev.yml up
```

## 📊 Debug ve Troubleshooting

### 🔍 Container İçine Giriş

```bash
# Frontend container'ına gir (sh shell)
docker exec -it sellerx-frontend sh

# Backend container'ına gir (bash shell)
docker exec -it sellerx-backend bash

# Database'e direk bağlan
docker exec -it sellerx-postgres psql -U postgres -d sellerx_db
```

### 🔧 Servis Specific Debugging

```bash
# Frontend node_modules kontrolü
docker exec sellerx-frontend npm list --depth=0

# Backend heap memory kontrolü
docker exec sellerx-backend jps -l
docker exec sellerx-backend jstat -gc [PID]

# Database connection test
docker exec sellerx-postgres pg_isready -U postgres -d sellerx_db
```

### 📈 Log Analysis

```bash
# Error loglarını filtrele
docker-compose -f docker-compose.dev.yml logs | grep -i error

# Warning loglarını filtrele
docker-compose -f docker-compose.dev.yml logs | grep -i warn

# Success mesajlarını filtrele
docker-compose -f docker-compose.dev.yml logs | grep -E "✓|Ready|Started"

# JSON loglarını güzel formatta göster (jq gerekli)
docker-compose -f docker-compose.dev.yml logs -f frontend | jq .
```

### 🧹 Temizlik Komutları

```bash
# Sadece bu projenin container'larını durdur
docker-compose -f docker-compose.dev.yml down

# Volume'ları da sil (database reset)
docker-compose -f docker-compose.dev.yml down -v

# Orphan container'ları temizle
docker-compose -f docker-compose.dev.yml down --remove-orphans

# Build cache temizle
docker builder prune -f

# Tüm stopped container'ları sil
docker container prune -f
```

## 🎯 Production Hazırlığı

### Environment Configuration

```bash
# Production için ayrı docker-compose dosyası oluşturulacak
# docker-compose.prod.yml

# Environment variables
cp .env.example .env.production
# .env.production dosyasını production değerleriyle doldur
```

### Security Checklist

- [ ] Database password değiştir
- [ ] JWT secret key güvenli hale getir
- [ ] CORS policy'leri production'a göre ayarla
- [ ] SSL/TLS sertifikası ekle
- [ ] Rate limiting ekle
- [ ] Health check endpoint'leri ekle

## 🏗️ Proje Yapısı

```
Sellerx/
├── 📁 sellerx-frontend/              # Frontend (Next.js 14+)
│   ├── app/                     # App Router
│   ├── components/              # React Components
│   ├── lib/                     # API & Utilities
│   ├── Dockerfile.dev           # Development Docker
│   └── next.config.ts           # Next.js Config (Hot Reload)
├── 📁 sellerx-backend/          # Backend (Spring Boot)
│   ├── src/main/java/           # Java Source
│   ├── src/main/resources/      # Config Files
│   └── Dockerfile               # Backend Docker
├── 📁 .vscode/                  # VS Code Workspace Settings
│   ├── settings.json            # Editor preferences
│   └── extensions.json          # Recommended extensions
├── 📁 .devcontainer/            # Dev Container Configuration
│   └── devcontainer.json        # VS Code Dev Containers
├── docker-compose.dev.yml       # Development Stack
├── Sellerx.code-workspace   # VS Code Multi-folder Workspace
├── README.md                    # Hızlı başlangıç
└── README-Docker.md             # Detaylı döküman (bu dosya)
```

## 🔧 Geliştirme Notları

### Hot Reload

- Frontend: Kod değişikliklerinde otomatik yeniden yüklenir
- Backend: Spring Boot DevTools ile otomatik yeniden başlatma

### Environment Variables

- Backend: `application-docker.yaml` dosyasında Docker ortamı ayarları
- Frontend: `.env.local` dosyasında environment variables

### Database

- PostgreSQL 15
- Port: 5432
- Database: sellerx_db
- Username: postgres
- Password: 123123

### Volume Mounting

- Source kod değişiklikleri container içinde anında yansır
- node_modules ve .next klasörleri container içinde kalır (performans için)
