# Sellerx

Modern e-ticaret yönetim platformu. Frontend (Next.js) + Backend (Spring Boot) + PostgreSQL ile Docker geliştirme ortamı.

## 🎯 Hızlı Başlangıç

### Gereksinimler

- **Docker Desktop** (Windows/Mac/Linux)
- **Git**
- **VS Code** (önerilen)

### 1️⃣ Projeyi Klonla

```bash
git clone https://github.com/semdin/sellerx-frontend.git
git clone https://github.com/semdin/sellerx-backend.git
```

### 2️⃣ Docker ile Başlat

```bash
# İlk defa çalıştırıyorsan
docker-compose -f docker-compose.dev.yml up --build -d

# Sonraki çalıştırmalarda
docker-compose -f docker-compose.dev.yml up -d
```

### 3️⃣ Erişim Adresleri

- 🌐 **Frontend**: http://localhost:3000
- 🔧 **Backend API**: http://localhost:8080
- 🗄️ **Database**: localhost:5432 (postgres/123123)

## 💻 VS Code Entegrasyonu (Önerilen)

### Dev Containers Kurulumu

1. **Extension yükle**: VS Code'da "Dev Containers" extension'ını yükle
2. **Workspace aç**: `Sellerx.code-workspace` dosyasını aç
3. **Container'da çalış**: `Ctrl+Shift+P` → "Dev Containers: Reopen in Container"

### Avantajları

- ✅ Real-time hata tespiti (TypeScript, ESLint)
- ✅ Tam IntelliSense ve kod tamamlama
- ✅ Debug desteği container içinde
- ✅ Tüm extensionlar otomatik yüklenir

## 🔧 Sık Kullanılan Komutlar

### Servis Yönetimi

```bash
# Servisleri durdur ve temizle
docker-compose -f docker-compose.dev.yml down

# Volume'ları da sil (database sıfırlama)
docker-compose -f docker-compose.dev.yml down -v

# Sadece belirli servisi yeniden başlat
docker-compose -f docker-compose.dev.yml restart frontend
docker-compose -f docker-compose.dev.yml restart backend

# Yeniden build et ve başlat
docker-compose -f docker-compose.dev.yml up --build -d
```

### Debug ve İnceleme

```bash
# Container içine gir
docker exec -it sellerx-frontend sh
docker exec -it sellerx-backend bash
docker exec -it sellerx-postgres psql -U postgres -d sellerx_db

# Disk kullanımı
docker system df

# Logları dosyaya kaydet
docker-compose -f docker-compose.dev.yml logs > debug.log
```

### Performance Monitoring

```bash
# Anlık resource kullanımı
docker stats --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}"

# Sadece Sellerx containerları
docker stats sellerx-frontend sellerx-backend sellerx-postgres
```

## 🐛 Sorun Giderme

### Port Çakışması

```bash
# Hangi portlar kullanılıyor?
netstat -tulpn | grep -E ":(3000|8080|5432)"

# Windows'ta:
netstat -ano | findstr "3000"
netstat -ano | findstr "8080"
netstat -ano | findstr "5432"
```

### Hot Reload Çalışmıyor

```bash
# Frontend container'ını restart et
docker-compose -f docker-compose.dev.yml restart frontend

# Node modules yeniden yükle
docker-compose -f docker-compose.dev.yml exec frontend npm install

# Temiz build
docker-compose -f docker-compose.dev.yml down
docker-compose -f docker-compose.dev.yml up --build -d
```

### Database Problemleri

```bash
# Database connection test
docker-compose -f docker-compose.dev.yml exec postgres pg_isready -U postgres

# Database'e manuel bağlan
docker-compose -f docker-compose.dev.yml exec postgres psql -U postgres -d sellerx_db

# Database tamamen sıfırla
docker-compose -f docker-compose.dev.yml down -v
docker volume prune -f
docker-compose -f docker-compose.dev.yml up -d
```

### Container Temizliği

```bash
# Çalışmayan containerları temizle
docker container prune -f

# Kullanılmayan image'ları temizle
docker image prune -f

# Tüm sistemi temizle (DİKKAT: Tüm Docker data silinir!)
docker system prune -a --volumes
```

## 📊 Monitoring Dashboard

### Development Metrics

```bash
# Sürekli monitoring (yeni terminal'de çalıştır)
watch -n 2 'docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}"'

# Log summary
docker-compose -f docker-compose.dev.yml logs --tail=50 | grep -E "(ERROR|WARN|✓|⚠)"
```

## 🚀 Production

### Environment Setup

```bash
# Production build (gelecekte)
docker-compose -f docker-compose.prod.yml up --build -d

# SSL sertifikaları için
# - Let's Encrypt ya da manuel sertifika
# - Reverse proxy (Nginx/Traefik)
```

## 📁 Proje Yapısı

```
Sellerx/
├── 📁 sellerx-frontend/           # Frontend (Next.js 14+)
│   ├── app/                  # App Router
│   ├── components/           # React Components
│   ├── lib/                  # Utilities & API
│   └── Dockerfile.dev        # Development Docker
├── 📁 sellerx-backend/       # Backend (Spring Boot)
│   ├── src/main/java/        # Java Source
│   ├── src/main/resources/   # Config Files
│   └── Dockerfile            # Backend Docker
├── 📁 .vscode/               # VS Code Settings
├── 📁 .devcontainer/         # Dev Container Config
├── docker-compose.dev.yml    # Development Stack
└── Sellerx.code-workspace # VS Code Workspace
```

## 📝 Notlar

- **Development**: Hot reload aktif, debug mode açık
- **Database**: Development datası container'da saklanır
- **Logs**: Container logları `/var/log/` altında
- **Ports**: 3000 (Frontend), 8080 (Backend), 5432 (DB)

Detaylı bilgi için `README-Docker.md` dosyasına bakın.
