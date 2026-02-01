# SellerX Dokümantasyon

Bu klasör SellerX projesinin teknik dokümantasyonunu içerir.

## Yapı

```
docs/
├── README.md           # Bu dosya
├── CHANGELOG.md        # Geliştirme günlüğü
├── features/           # Özellik bazlı dokümantasyon
│   └── WEBHOOKS.md     # Webhook sistemi detayları
├── api/                # API dokümantasyonu (planlanıyor)
└── changelog/          # Release notları (planlanıyor)
```

## Hızlı Erişim

### Özellikler

| Özellik | Dokümantasyon | Durum |
|---------|---------------|-------|
| Webhooks | [features/WEBHOOKS.md](./features/WEBHOOKS.md) | ✅ Tamamlandı |
| Eğitim Videoları | [features/EDUCATION_VIDEOS.md](./features/EDUCATION_VIDEOS.md) | ✅ Tamamlandı |
| Auth | - | Planlanıyor |
| Products | - | Planlanıyor |
| Orders | - | Planlanıyor |

### Geliştirme

- [CHANGELOG.md](./CHANGELOG.md) - Tüm değişikliklerin kronolojik kaydı
- [../CLAUDE.md](../CLAUDE.md) - Claude Code için proje rehberi
- [../README.md](../README.md) - Genel proje bilgisi

## Katkıda Bulunma

Yeni bir özellik eklerken:

1. `features/` altına özellik dokümantasyonu ekleyin
2. `CHANGELOG.md` dosyasını güncelleyin
3. `../CLAUDE.md` dosyasını güncelleyin
4. İlgili API endpoint'lerini `api/` altında belgeleyin

## Sürüm Notları

Önemli sürümler için `changelog/` klasörünü kullanın:
- `changelog/v1.0.0.md` - Major sürümler
- `changelog/v1.1.0.md` - Minor sürümler
