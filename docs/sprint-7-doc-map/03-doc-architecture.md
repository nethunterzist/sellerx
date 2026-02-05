# Nihai Doc Mimarisi (docs)

docs klasörünün yapısı, navigasyon ve güncelleme kuralları.

## Yapı

- **Kök:** docs/README.md — Amaç, Sprint Planı tablosu, Klasör Yapısı şeması, Güncelleme Kuralı.
- **Sprint klasörleri:** sprint-1 … sprint-7. Liste/harita: “nerede ne var?” (endpoint listesi, migration listesi, sayfa listesi vb.).
- **architecture/:** Derin mimari — “nasıl çalışıyor?” (komisyon, webhook, rate limit, DB şeması detayı vb.). Sprint’lerden bağımsız, konu bazlı.
- **features/:** Özellik dokümanları — kullanıcıya dönük özellik detayı (webhook yönetimi, eğitim videoları vb.). Bazen architecture ile konu çakışır: architecture = teknik akış, features = özellik spec.
- **archive/:** Eski / referanssız dokümanlar; güncel değil, sadece arşiv.

## Navigasyon

1. **Yeni gelen:** docs/README.md → Sprint Planı tablosundan odak alanına göre sprint klasörüne gir.
2. **Kodda bir alan arıyorum:** sprint-7-doc-map/02-code-to-doc.md → hangi doc'a bakılacak.
3. **Doc'un ne dokümante ettiğini biliyorum:** sprint-7-doc-map/01-doc-to-code.md → hangi kod alanı.

## Güncelleme Kuralları

- Yeni backend controller / endpoint → sprint-1: 01-backend-endpoints.md, 04-bff-to-backend-map.md; public/Admin ise 02-public-endpoints.md.
- Yeni BFF route → sprint-1: 03-bff-routes.md, 04-bff-to-backend-map.md.
- Yeni migration / entity → sprint-2: 01-migration-list.md ve/veya 02-entity-table-map.md, 03-schema-overview.md.
- Yeni paket / @Scheduled job → sprint-3: 01-domain-list.md ve/veya 02-scheduled-jobs.md, 03-schedule-overview.md.
- Yeni sayfa / feature → sprint-4: 01-page-list.md, 02-feature-map.md, 03-route-structure.md.
- Yeni bileşen / ui primitive / stil → sprint-5: 01-component-list.md, 02-ui-primitives.md, 03-styles-overview.md.
- Yeni SecurityRules / auth veya env-deploy değişikliği → sprint-6: 02-security-rules.md veya 01-auth-overview.md, 03-env-and-deploy.md.
- Yeni sprint veya doc dosyası eklendiğinde → sprint-7: 01-doc-to-code.md, 02-code-to-doc.md; docs/README.md klasör yapısı.

## Tam Klasör Ağacı

```
docs/
├── README.md                    # Tek giriş noktası
├── CHANGELOG.md                 # Geliştirme günlüğü
├── DOC_VERIFICATION_REPORT.md   # Doc–kod doğrulama raporu
├── sprint-1-api-inventory/      # API envanteri (backend + BFF)
├── sprint-2-db-schema/          # DB şeması, migration listesi
├── sprint-3-backend-domains/    # Domain listesi, scheduled job'lar
├── sprint-4-frontend-pages/     # Sayfa listesi, feature haritası
├── sprint-5-ui-components/      # Bileşen listesi, stil
├── sprint-6-cross-cutting/      # Auth, güvenlik, env, deploy
├── sprint-7-doc-map/            # Doc↔kod eşlemesi, bu mimari
├── architecture/                # Derin mimari (nasıl çalışıyor)
├── features/                    # Özellik dokümanları
└── archive/                     # Eski / referanssız (güncel değil)
```

## Kafa Karışıklığını Önlemek

| Soru | Cevap |
|------|--------|
| Sprint = proje sprint’i mi? | Hayır. Buradaki “sprint” = doc grupları (envanter/harita). Proje sprint’i değil. |
| Aynı konu iki yerde (örn. webhook)? | Olabilir: **architecture/** = teknik akış (WEBHOOK_SYSTEM.md), **features/** = özellik detayı (WEBHOOKS.md). İkisi birbirini tamamlar. |
| DB şeması nerede? | **Liste/harita:** sprint-2-db-schema. **Detaylı şema açıklaması:** architecture/DATABASE_SCHEMA.md. |
| Hangi doc güncel? | DOC_VERIFICATION_REPORT.md’de architecture/ ve features/ doğrulama özeti var. archive/ güncel değil. |
