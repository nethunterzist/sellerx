# SellerX Dokümantasyon

Tüm teknik dokümantasyon **bu klasördedir** (`docs/`). Yeni yazılımcı ve canlı ortam için buradan başlayın.

## Amaç

- Hiçbir alan atlanmadan proje analizi (API, DB, backend domain'ler, frontend, UI, cross-cutting, doc–kod eşlemesi).
- Sprint bazlı envanter/harita; architecture/ ile derin mimari; features/ ile özellik dokümanları.
- Kod değişince ilgili doc güncellenir.

## Sprint Planı (Envanter / Harita)

| Sprint | Odak | Çıktı Klasörü |
|--------|------|----------------|
| 1 | API envanteri (Backend + BFF) | sprint-1-api-inventory |
| 2 | Veritabanı şeması ve migration'lar | sprint-2-db-schema |
| 3 | Backend domain'ler ve scheduled job'lar | sprint-3-backend-domains |
| 4 | Frontend sayfa ve feature haritası | sprint-4-frontend-pages |
| 5 | UI bileşenleri ve stil | sprint-5-ui-components |
| 6 | Cross-cutting (auth, güvenlik, env, deploy) | sprint-6-cross-cutting |
| 7 | Doc–kod eşlemesi ve doc mimarisi | sprint-7-doc-map |

## Klasör Yapısı

```
docs/
├── README.md                    # Bu dosya (tek giriş)
├── CHANGELOG.md                 # Geliştirme günlüğü
│
├── sprint-1-api-inventory/      # API envanteri
├── sprint-2-db-schema/          # Veritabanı şeması
├── sprint-3-backend-domains/    # Backend domain'ler ve job'lar
├── sprint-4-frontend-pages/     # Frontend sayfa ve feature haritası
├── sprint-5-ui-components/      # UI bileşenleri ve stil
├── sprint-6-cross-cutting/       # Auth, güvenlik, env, deploy
├── sprint-7-doc-map/            # Doc–kod eşlemesi, mimari
│
├── architecture/                # Derin mimari (nasıl çalışıyor)
│   ├── README.md
│   ├── ADMIN_IMPERSONATION.md
│   ├── COMMISSION_SYSTEM.md
│   ├── STOPPAGE_SYSTEM.md
│   ├── WEBHOOK_SYSTEM.md
│   ├── STORE_ONBOARDING.md
│   ├── DATABASE_SCHEMA.md
│   └── ...
│
├── features/                    # Özellik dokümanları (kodda karşılığı olanlar)
│   ├── AUTO_STOCK_DETECTION.md
│   ├── WEBHOOKS.md
│   ├── EDUCATION_VIDEOS.md
│   ├── PURCHASING.md
│   └── ...
│
├── scaling/                     # Ölçeklendirme ve Microservices Dönüşüm Planı
│   ├── README.md                # Plan özeti
│   ├── FAZ_0_ACIL_DUZELTMELER.md # Faz 0: Acil düzeltmeler
│   ├── FAZ_1_RABBITMQ.md       # Faz 1: RabbitMQ entegrasyonu
│   ├── FAZ_2_RESILIENCE4J.md   # Faz 2: Resilience4j entegrasyonu
│   ├── FAZ_3_WEBSOCKET.md      # Faz 3: WebSocket entegrasyonu
│   ├── FAZ_4_DATABASE.md       # Faz 4: Database optimizasyonları
│   └── FAZ_5_FRONTEND.md       # Faz 5: Frontend optimizasyonları
│
├── deployment/                  # CI/CD ve deployment dokümanları
│   └── CI_CD_GUIDE.md
│
└── archive/                     # Tarihli / referanssız (arşiv)
    ├── DOC_VERIFICATION_REPORT.md
    ├── DOC_CODE_GAP_REPORT.md
    ├── FULL_PROJECT_DOCS_ANALYSIS.md
    ├── COOLIFY_DEPLOYMENT_PLAN.md
    └── ...
```

## Navigasyon

- **Envanter / harita (nerede ne var):** sprint-1 … sprint-7.
- **Nasıl çalışıyor (derin mimari):** [architecture/](architecture/).
- **Özellik detayı:** [features/](features/).
- **Ölçeklendirme planı:** [scaling/](scaling/) - Microservices dönüşüm planı.
- **Agent ekip yapısı:** [AGENT_TEAM.md](AGENT_TEAM.md).
- **Tech stack, TODO ve teknik borç:** [TECHSTACK_TODO_DEBT.md](TECHSTACK_TODO_DEBT.md).
- **Geliştirme günlüğü:** [CHANGELOG.md](CHANGELOG.md).
- **Hangi kod hangi doc:** [sprint-7-doc-map/02-code-to-doc.md](sprint-7-doc-map/02-code-to-doc.md).
- **Docker ile kurulum:** [../README-Docker.md](../README-Docker.md).
- **CI/CD ve deployment:** [deployment/CI_CD_GUIDE.md](deployment/CI_CD_GUIDE.md).

## Güncelleme Kuralı

Kod değişikliği yapıldığında ilgili doc güncellenir. Örn: yeni backend controller → sprint-1 (01-backend-endpoints.md, 04-bff-to-backend-map.md); yeni migration → sprint-2; yeni sayfa → sprint-4. Detay: [sprint-7-doc-map/03-doc-architecture.md](sprint-7-doc-map/03-doc-architecture.md).
