# Sprint 3: Backend Domain'ler ve Scheduled Job'lar

Sprint 3'ün odak alanı: Mevcut koda göre backend domain paketleri ve tüm `@Scheduled` job'ların dokümantasyonu. **Sadece doc yazılır; kod değiştirilmez.**

## Dosya Listesi

| Dosya | İçerik |
|-------|--------|
| [01-domain-list.md](01-domain-list.md) | Tüm backend paketleri (com.ecommerce.sellerx.*): kısa açıklama, ana sınıflar. |
| [02-scheduled-jobs.md](02-scheduled-jobs.md) | Tüm @Scheduled metodlar: sınıf, metod, cron/fixedRate, zone, ShedLock, açıklama. |
| [03-schedule-overview.md](03-schedule-overview.md) | Job'ların zaman çizelgesi özeti (saat sırası, saatlik/periyodik, ShedLock). |

## Kapsam

- **Kaynak:** `sellerx-backend/src/main/java/com/ecommerce/sellerx/**` paketleri; `@Scheduled` ve `@SchedulerLock` kullanan sınıflar.
- **Güncelleme:** Yeni paket veya scheduled job eklenince ilgili doc güncellenir; kod değişikliği yapılmaz.

## Nasıl Kullanılır

- Yeni domain paketi eklenince → 01-domain-list.md güncellenir.
- Yeni @Scheduled metodu eklenince → 02-scheduled-jobs.md ve 03-schedule-overview.md güncellenir.
