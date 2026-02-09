# SellerX CI/CD Deployment Guide

Bu rehber, SellerX uygulamasının GitHub Actions ile otomatik deployment sürecini açıklar.

## Mimari

```
┌─────────────────────────────────────────────────────────────────┐
│                        GITHUB REPOSITORY                         │
│  main branch'e push                                              │
└─────────────────┬───────────────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                     GITHUB ACTIONS                               │
│  ┌─────────────────┐    ┌─────────────────┐                     │
│  │ Backend Build   │    │ Frontend Build  │                     │
│  │ - Maven test    │    │ - npm build     │                     │
│  │ - Docker build  │    │ - Docker build  │                     │
│  │ - Push to GHCR  │    │ - Push to GHCR  │                     │
│  └────────┬────────┘    └────────┬────────┘                     │
└───────────┼──────────────────────┼──────────────────────────────┘
            │                      │
            ▼                      ▼
┌─────────────────────────────────────────────────────────────────┐
│              GITHUB CONTAINER REGISTRY (GHCR)                    │
│  ghcr.io/nethunterzist/sellerx/backend:latest                   │
│  ghcr.io/nethunterzist/sellerx/frontend:latest                  │
└─────────────────┬───────────────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                    PRODUCTION SERVER                             │
│  157.180.78.53 (Coolify/Traefik)                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   Backend    │  │   Frontend   │  │  PostgreSQL  │          │
│  │   :8080      │  │   :3000      │  │   :5432      │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

## Workflow Dosyaları

| Dosya | Tetikleyici | Açıklama |
|-------|-------------|----------|
| `ci.yml` | PR, push (tüm branch'ler) | Build, test, Docker validation |
| `deploy-backend.yml` | push (main, `sellerx-backend/**`) | Backend production deploy |
| `deploy-frontend.yml` | push (main, `sellerx-frontend/**`) | Frontend production deploy |

## GitHub Secrets Yapılandırması

Repository → Settings → Secrets and variables → Actions

### Gerekli Secrets

| Secret Name | Değer | Açıklama |
|-------------|-------|----------|
| `SSH_PRIVATE_KEY` | (ed25519 private key) | Sunucu SSH erişimi |
| `DB_PASSWORD` | `L6CAVfFloy...` | PostgreSQL şifresi |
| `JWT_SECRET` | `sellerx-production-jwt...` | JWT signing key (min 32 karakter) |
| `ENCRYPTION_KEY` | (32-byte hex) | AES encryption key |
| `GHCR_PAT` | `ghp_xxx...` | GitHub Container Registry PAT |

### GitHub PAT Oluşturma (GHCR için)

1. GitHub → Settings → Developer Settings → Personal Access Tokens → Tokens (classic)
2. "Generate new token (classic)"
3. Note: `sellerx-deploy-ghcr`
4. Expiration: 90 days veya No expiration
5. Scopes:
   - `read:packages`
   - `write:packages`
   - `delete:packages`
6. Generate ve kopyala

### SSH Key Oluşturma

```bash
# Lokal makinede
ssh-keygen -t ed25519 -C "github-actions-deploy-sellerx" -f ~/.ssh/github_deploy_key -N ""

# Public key'i sunucuya ekle
ssh-copy-id -i ~/.ssh/github_deploy_key.pub root@157.180.78.53

# Private key'i GitHub Secrets'a ekle
cat ~/.ssh/github_deploy_key
```

## Deployment Akışı

### 1. Otomatik Deployment (Önerilen)

```bash
# Değişiklikleri commit et
git add .
git commit -m "feat: yeni özellik eklendi"

# main branch'e push et
git push origin main

# GitHub Actions otomatik tetiklenir
# Progress: https://github.com/nethunterzist/sellerx/actions
```

### 2. Manuel Deployment

```bash
# GitHub CLI ile
gh workflow run deploy-backend.yml
gh workflow run deploy-frontend.yml

# Veya GitHub web arayüzünden:
# Actions → Deploy Backend → Run workflow
```

## Health Check & Doğrulama

### Uygulama Durumu

```bash
# Backend health check
curl -k https://uwgss4kkocg4kks8880kwwwo.157.180.78.53.sslip.io/actuator/health

# Frontend erişim
curl -k -L https://sellerx.157.180.78.53.sslip.io
```

### Container Durumu

```bash
# SSH ile sunucuya bağlan
ssh root@157.180.78.53

# Container'ları listele
docker ps | grep sellerx

# Container logları
docker logs sellerx-backend --tail 100
docker logs sellerx-frontend --tail 50
```

### GitHub Actions Durumu

```bash
# Son çalışmalar
gh run list --workflow=deploy-backend.yml
gh run list --workflow=deploy-frontend.yml

# Belirli bir run'ın detayları
gh run view <run-id>

# Log'ları görüntüle
gh run view <run-id> --log
```

## Rollback

Deployment başarısız olursa workflow otomatik rollback yapar. Manuel rollback:

```bash
ssh root@157.180.78.53

# Backend rollback
docker stop sellerx-backend
docker rm sellerx-backend
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
  -e SERVER_PORT=8080 \
  -e SPRING_PROFILES_ACTIVE=production \
  -l traefik.enable=true \
  -l 'traefik.http.routers.sellerx-backend.rule=Host(`uwgss4kkocg4kks8880kwwwo.157.180.78.53.sslip.io`)' \
  -l traefik.http.routers.sellerx-backend.entrypoints=https \
  -l traefik.http.routers.sellerx-backend.tls=true \
  -l traefik.http.services.sellerx-backend.loadbalancer.server.port=8080 \
  ghcr.io/nethunterzist/sellerx/backend:rollback
```

## Troubleshooting

### GitHub Actions Başarısız

1. **Actions tab'ını kontrol et**: Hata mesajını oku
2. **Secrets kontrol et**: Tüm gerekli secrets tanımlı mı?
3. **Branch kontrol et**: `main` branch'e mi push edildi?
4. **Path kontrol et**: Doğru klasörde değişiklik var mı?

### Container Başlamıyor

```bash
# Container loglarını kontrol et
docker logs sellerx-backend --tail 200

# En yaygın hatalar:
# - DATABASE_URL yanlış → DB_HOST kontrol et
# - JWT_SECRET eksik → Secret tanımlı mı?
# - Port çakışması → Başka container aynı portu kullanıyor mu?
```

### GHCR Authentication Hatası

```bash
# Sunucuda GHCR login
ssh root@157.180.78.53
echo "<GHCR_PAT>" | docker login ghcr.io -u nethunterzist --password-stdin
```

### SSH Bağlantı Hatası

```bash
# Lokal makineden test
ssh -i ~/.ssh/github_deploy_key root@157.180.78.53

# Eğer çalışmıyorsa:
# 1. Public key sunucuya eklendi mi?
# 2. Private key GitHub Secrets'ta doğru mu?
# 3. Sunucu firewall SSH'a izin veriyor mu?
```

## Production Bilgileri

| Kaynak | URL/Değer |
|--------|-----------|
| Frontend | https://sellerx.157.180.78.53.sslip.io |
| Backend | https://uwgss4kkocg4kks8880kwwwo.157.180.78.53.sslip.io |
| Coolify Panel | http://157.180.78.53:8000 |
| Database | 157.180.78.53:5432 (internal: uws408w88ko044c84gwgkw04) |
| GHCR Backend | ghcr.io/nethunterzist/sellerx/backend |
| GHCR Frontend | ghcr.io/nethunterzist/sellerx/frontend |

## Güvenlik Notları

1. **Secrets asla commit etme**: `.env` dosyaları `.gitignore`'da olmalı
2. **PAT'ları düzenli yenile**: 90 günde bir yeni PAT oluştur
3. **SSH key'leri koruma**: Private key sadece GitHub Secrets'ta olmalı
4. **Minimum yetki**: PAT'lara sadece gerekli scope'ları ver
