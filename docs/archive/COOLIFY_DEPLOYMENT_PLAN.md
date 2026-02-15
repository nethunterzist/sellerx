# SellerX Coolify Deployment Planı

## 🎯 Hedef
SellerX'i Hetzner sunucusundaki Coolify'a (v4.0.0-beta.460) deploy etmek.

**Sunucu**: 157.180.78.53 (Coolify: http://157.180.78.53:8000)

---

## 📋 Mevcut Altyapı Analizi

### Coolify Durumu
- **Versiyon**: v4.0.0-beta.460
- **Server**: localhost (Hetzner IP: 157.180.78.53)
- **Reverse Proxy**: Traefik
- **Build System**: Nixpacks (auto-detection)

### Mevcut Projeler
1. **Digital Contract System** - NextJS + PostgreSQL (çalışıyor)
2. **Dijital Kartvizit** - Çalışıyor

### Mevcut Veritabanları
- imza-dev-db (PostgreSQL) - Running
- imza-prod-db (PostgreSQL) - Running
- postgresql-database (PostgreSQL) - Running

### GitHub Entegrasyonu
- GitHub App: `imza-deployment-app` (mevcut)

---

## 🏗️ SellerX Deployment Mimarisi

```
┌─────────────────────────────────────────────────────────────┐
│                    Coolify (Hetzner)                        │
│                    157.180.78.53                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────┐    ┌─────────────────┐                │
│  │   sellerx-fe    │───▶│   sellerx-be    │                │
│  │   (Next.js)     │    │  (Spring Boot)  │                │
│  │   Port: 3000    │    │   Port: 8080    │                │
│  └────────┬────────┘    └────────┬────────┘                │
│           │                      │                          │
│           │              ┌───────▼───────┐                  │
│           │              │  sellerx-db   │                  │
│           │              │  (PostgreSQL) │                  │
│           │              │   Port: 5432  │                  │
│           │              └───────────────┘                  │
│           │                                                 │
│  ┌────────▼────────────────────────────────┐               │
│  │              Traefik Proxy              │               │
│  │  sellerx.157.180.78.53.sslip.io        │               │
│  │  api.sellerx.157.180.78.53.sslip.io    │               │
│  └─────────────────────────────────────────┘               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 📝 Deployment Adımları

### Adım 1: Yeni Proje Oluştur
1. Coolify Dashboard → Projects → Add New
2. Proje adı: `SellerX`
3. Açıklama: "E-commerce management platform for Turkish marketplaces"

### Adım 2: PostgreSQL Veritabanı Kur
1. SellerX projesi → Resources → Add New Resource → Database → PostgreSQL
2. Ayarlar:
   - **Name**: `sellerx-db`
   - **Version**: `15-alpine`
   - **Database Name**: `sellerx_db`
   - **Username**: `sellerx`
   - **Password**: (Güçlü şifre oluştur)
   - **Port**: 5432 (internal)

3. Deploy et ve "Internal Address" not al (örn: `abc123xyz:5432`)

### Adım 3: GitHub Repository Bağla
**Seçenek A: Mevcut GitHub App Kullan**
- `imza-deployment-app` zaten bağlı
- SellerX reposuna erişim ver

**Seçenek B: Yeni GitHub App Oluştur**
- Settings → Sources → Add New → GitHub App
- Sadece SellerX reposuna erişim ver

### Adım 4: Backend Deploy (Spring Boot)
1. SellerX projesi → Resources → Add New Resource → Application
2. Source: GitHub → `sellerx-backend` klasörü seç
3. Build ayarları:
   ```yaml
   Build Pack: Nixpacks
   Base Directory: /sellerx-backend
   Build Command: ./mvnw clean package -DskipTests
   Start Command: java -jar target/*.jar
   ```

4. Environment Variables:
   ```env
   # Database
   SPRING_DATASOURCE_URL=jdbc:postgresql://[INTERNAL_DB_ADDRESS]:5432/sellerx_db
   SPRING_DATASOURCE_USERNAME=sellerx
   SPRING_DATASOURCE_PASSWORD=[DB_PASSWORD]

   # JWT
   JWT_SECRET=[MIN_32_KARAKTER_SECRET]

   # Server
   SERVER_PORT=8080
   SPRING_PROFILES_ACTIVE=production

   # Trendyol (opsiyonel - sonra eklenebilir)
   # WEBHOOK_BASE_URL=https://api.sellerx.157.180.78.53.sslip.io

   # Java
   JAVA_OPTS=-Xmx512m -Xms256m
   ```

5. Network ayarları:
   - **Port**: 8080
   - **Domain**: `api.sellerx.157.180.78.53.sslip.io`

6. Health Check:
   - Path: `/actuator/health`
   - Interval: 30s

### Adım 5: Frontend Deploy (Next.js)
1. SellerX projesi → Resources → Add New Resource → Application
2. Source: GitHub → `sellerx-frontend` klasörü seç
3. Build ayarları:
   ```yaml
   Build Pack: Nixpacks
   Base Directory: /sellerx-frontend
   Build Command: npm run build
   Start Command: node .next/standalone/server.js
   ```

4. Environment Variables:
   ```env
   # API URL (backend internal address)
   API_BASE_URL=http://[BACKEND_INTERNAL_ADDRESS]:8080
   NEXT_PUBLIC_API_BASE_URL=https://api.sellerx.157.180.78.53.sslip.io

   # Node
   NODE_ENV=production
   PORT=3000

   # Next.js
   NEXT_TELEMETRY_DISABLED=1
   ```

5. Network ayarları:
   - **Port**: 3000
   - **Domain**: `sellerx.157.180.78.53.sslip.io`

---

## 🗄️ Veri Migration (Localhost → Coolify)

### Mevcut Veri Durumu
- **62 tablo**
- **~57,000+ satır** (trendyol_orders: 29,989, cargo_invoices: 22,834, vb.)

### Migration Adımları

#### 1. Localhost'tan Export
```bash
# Zaten export edildi: /tmp/sellerx_data.sql
# Eğer tekrar gerekirse:
docker exec sellerx-db pg_dump -U postgres -d sellerx_db \
  --data-only \
  --exclude-table=flyway_schema_history \
  --exclude-table=shedlock \
  > /tmp/sellerx_data.sql
```

#### 2. Coolify DB'ye Bağlan
Coolify'da veritabanı deploy edildikten sonra:
1. Database → Terminal veya
2. SSH ile sunucuya bağlan ve docker exec kullan

```bash
# Coolify sunucusuna SSH
ssh root@157.180.78.53

# Container ID bul
docker ps | grep sellerx-db

# psql bağlan
docker exec -it [CONTAINER_ID] psql -U sellerx -d sellerx_db
```

#### 3. Schema Oluştur
Spring Boot ilk çalıştığında Flyway otomatik olarak schema'yı oluşturacak.
Alternatif: Manuel migration dosyalarını çalıştır.

#### 4. Veri Import
```bash
# SQL dosyasını sunucuya kopyala
scp /tmp/sellerx_data.sql root@157.180.78.53:/tmp/

# Sunucuda import et
ssh root@157.180.78.53
docker cp /tmp/sellerx_data.sql [CONTAINER_ID]:/tmp/
docker exec -it [CONTAINER_ID] bash

# Container içinde
psql -U sellerx -d sellerx_db -c "SET session_replication_role = 'replica';"
psql -U sellerx -d sellerx_db < /tmp/sellerx_data.sql
psql -U sellerx -d sellerx_db -c "SET session_replication_role = 'origin';"
```

#### 5. Sequence'leri Güncelle
```sql
-- Her tablo için auto-increment değerlerini düzelt
SELECT setval(pg_get_serial_sequence('users', 'id'), COALESCE(MAX(id), 1)) FROM users;
SELECT setval(pg_get_serial_sequence('stores', 'id'), COALESCE(MAX(id), 1)) FROM stores;
-- ... diğer tablolar
```

---

## ✅ Doğrulama Checklist

### Database
- [ ] PostgreSQL container çalışıyor
- [ ] `sellerx_db` veritabanı oluştu
- [ ] Tüm tablolar mevcut (62 tablo)
- [ ] Veri migration tamamlandı

### Backend
- [ ] Container çalışıyor (healthy)
- [ ] `/actuator/health` 200 döndürüyor
- [ ] Flyway migrations başarılı
- [ ] Database bağlantısı çalışıyor
- [ ] JWT authentication çalışıyor

### Frontend
- [ ] Container çalışıyor
- [ ] `sellerx.157.180.78.53.sslip.io` erişilebilir
- [ ] Login sayfası yükleniyor
- [ ] Backend API'ye bağlanabiliyor
- [ ] test@test.com ile giriş yapılabiliyor

### Entegrasyon
- [ ] Frontend → Backend API çağrıları çalışıyor
- [ ] Dashboard verileri görüntüleniyor
- [ ] Orders listesi yükleniyor
- [ ] Products listesi yükleniyor

---

## 🔧 Troubleshooting

### Backend başlamıyor
```bash
# Logs kontrol et
docker logs [BACKEND_CONTAINER_ID]

# Olası sorunlar:
# 1. JWT_SECRET < 32 karakter → 500 error
# 2. Database bağlantısı yok → Connection refused
# 3. Port çakışması → Address already in use
```

### Frontend 502 Bad Gateway
```bash
# Backend çalışıyor mu kontrol et
curl http://[BACKEND_INTERNAL]:8080/actuator/health

# Environment variables doğru mu?
# API_BASE_URL internal address olmalı
```

### Database bağlantı hatası
```bash
# Internal address doğru mu?
# Coolify dashboard → Database → Internal Address

# Network aynı mı?
# Aynı proje altındaki containerlar otomatik iletişim kurabilir
```

---

## 📊 Kaynak Tahmini

| Servis | RAM | CPU | Disk |
|--------|-----|-----|------|
| PostgreSQL | 512MB | 0.5 core | 5GB |
| Backend (Spring Boot) | 512MB-1GB | 1 core | 500MB |
| Frontend (Next.js) | 256MB | 0.5 core | 500MB |
| **Toplam** | **~2GB** | **2 cores** | **6GB** |

---

## 🔐 Güvenlik Notları

1. **JWT_SECRET**: Minimum 32 karakter, güçlü rastgele string
2. **Database Password**: Güçlü şifre kullan
3. **HTTPS**: Coolify otomatik Let's Encrypt sertifikası alabilir (domain gerekli)
4. **Firewall**: Sadece 80, 443, 22 (SSH), 8000 (Coolify) portları açık olmalı

---

## 🚀 Sonraki Adımlar (Post-Deployment)

1. **Custom Domain**: Gerçek domain ekle (örn: app.sellerx.com)
2. **SSL Sertifikası**: Let's Encrypt ile HTTPS aktif et
3. **Monitoring**: Coolify built-in monitoring kullan
4. **Backups**: Otomatik veritabanı backup ayarla
5. **Trendyol Webhook**: Webhook URL'i güncelle

---

## ⏱️ Tahmini Süre

| Adım | Süre |
|------|------|
| Proje oluşturma | 5 dk |
| PostgreSQL kurulum | 10 dk |
| Backend deploy | 15-20 dk |
| Frontend deploy | 10-15 dk |
| Veri migration | 15-20 dk |
| Test ve doğrulama | 15 dk |
| **Toplam** | **~1-1.5 saat** |

---

**Hazırlandı**: Claude Code
**Tarih**: 2026-02-07
**Kaynak**: Coolify exploration via Playwright
