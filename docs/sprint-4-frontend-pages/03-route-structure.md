# Route Yapısı Özeti (Mevcut Kod)

Next.js App Router: [locale], layout grupları, dinamik segmentler. Kaynak: [01-page-list.md](01-page-list.md).

## [locale]

Tüm sayfalar `app/[locale]/` altında. Varsayılan dil tr; desteklenen: tr, en. URL örnekleri: `/tr/dashboard`, `/en/products`.

## Layout Grupları

| Grup | Klasör | Açıklama |
|------|--------|----------|
| — | [locale]/ | Root: page.tsx (ana sayfa, yönlendirme). |
| (auth) | [locale]/(auth)/ | Giriş/kayıt; sidebar yok, auth gerekmez. |
| (public) | [locale]/(public)/ | Public sayfalar (pricing); auth gerekmez. |
| (app-shell) | [locale]/(app-shell)/ | Uygulama sayfaları; sidebar + layout; JWT gerekir. |
| (admin) | [locale]/(admin)/ | Admin sayfaları; admin sidebar + layout; Admin rolü gerekir. |

## Ana Path Ağacı (locale sonrası)

```
/
├── sign-in, register          (auth)
├── pricing                    (public)
├── dashboard, products, orders, financial, purchasing, billing, profit,
│   expenses, alerts, stock-tracking, qa, returns, education,
│   notifications, support, settings, profile, new-store, analytics, dumen, kdv  (app-shell)
│   financial/invoices, financial/settlement, financial/vat
│   purchasing/[poId], purchasing/suppliers, purchasing/reports/*
│   stock-tracking/[id], support/[id]
│   billing/checkout
└── admin/                     (admin)
    ├── dashboard, stores, users, orders, products, referrals, subscriptions,
    │   revenue, activity-logs, support, education, notifications, security
    ├── stores/[id], users/[id], support/[id]
    └── ...
```

## Dinamik Segmentler

| Segment | Kullanıldığı path | Açıklama |
|---------|-------------------|----------|
| [id] | /stock-tracking/[id], /support/[id]; /admin/stores/[id], /admin/users/[id], /admin/support/[id] | Tekil kayıt detayı. |
| [poId] | /purchasing/[poId] | Satın alma siparişi ID. |

**Kritik kural (CLAUDE.md):** Aynı path hiyerarşisinde dinamik segment adı tutarlı olmalı. Örn. `/api/stores/[id]/...` ile uyumlu; aynı dalda `[storeId]` kullanılmamalı.

## Özet

- **52** sayfa (page.tsx).
- **4** layout grubu: root, (auth), (public), (app-shell), (admin).
- **Dinamik:** [id] (5 yerde), [poId] (1 yerde).
