# Sprint 2: Veritabanı Şeması ve Migration'lar

Sprint 2'nin odak alanı: Mevcut koda göre veritabanı şeması ve Flyway migration'larının dokümantasyonu. **Sadece doc yazılır; kod değiştirilmez.**

## Dosya Listesi

| Dosya | İçerik |
|-------|--------|
| [01-migration-list.md](01-migration-list.md) | Tüm Flyway migration'lar (V1–V84): V numarası, dosya adı, kısa açıklama. |
| [02-entity-table-map.md](02-entity-table-map.md) | JPA Entity → tablo adı ve ana kolonlar (id, FK, JSONB); domain/paket bazlı gruplu. |
| [03-schema-overview.md](03-schema-overview.md) | Ana tablolar, ilişkiler özeti, JSONB kolonlar, Flyway konumu ve sıra. |

## Kapsam

- **Kaynak:** `sellerx-backend/src/main/resources/db/migration/*.sql`, `sellerx-backend/.../**/*.java` içinde `@Entity` sınıfları.
- **Güncelleme:** Yeni migration veya entity eklendiğinde ilgili doc güncellenir; kod değişikliği yapılmaz.

## Nasıl Kullanılır

- Yeni migration eklenince → `01-migration-list.md` ve gerekirse `03-schema-overview.md` (Flyway bölümü) güncellenir.
- Yeni entity veya tablo/kolon eklenince → `02-entity-table-map.md` ve `03-schema-overview.md` (tablolar, ilişkiler, JSONB) güncellenir.
