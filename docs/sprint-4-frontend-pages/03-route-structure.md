# Route Yapısı Özeti (Mevcut Kod)

Next.js App Router: [locale], layout grupları, dinamik segmentler. Kaynak: [01-page-list.md](01-page-list.md).

## [locale]

Tüm sayfalar `app/[locale]/` altında. Varsayılan dil tr; desteklenen: tr, en. URL örnekleri: `/tr/dashboard`, `/en/products`.

## Layout Grupları

| Grup | Klasör | Açıklama |
|------|--------|----------|
| — | [locale]/ | Root: page.tsx (ana sayfa, yönlendirme). |
| (auth) | [locale]/(auth)/ | Giriş/kayıt/şifre sıfırlama/e-posta doğrulama; sidebar yok, auth gerekmez. |
| (public) | [locale]/(public)/ | Public sayfalar (pricing, hesaplama, yasal); auth gerekmez. |
| (app-shell) | [locale]/(app-shell)/ | Uygulama sayfaları; sidebar + layout; JWT gerekir. |
| (admin) | [locale]/(admin)/ | Admin sayfaları; admin sidebar + layout; Admin rolü gerekir. |

## Ana Path Ağacı (locale sonrası)

```
/
├── sign-in, register, forgot-password, reset-password,
│   verify-email, verification-pending                    (auth)
├── pricing, hesaplama, privacy, terms                    (public)
├── dashboard, products, orders, financial, purchasing, billing, profit,
│   expenses, alerts, stock-tracking, qa, returns, education,
│   notifications, support, settings, new-store, analytics,
│   customer-analytics, kar-hesaplama, impersonate, dumen, kdv  (app-shell)
│   financial/invoices, financial/settlement, financial/vat
│   purchasing/[poId], purchasing/orders, purchasing/suppliers, purchasing/reports/*
│   stock-tracking/[id], support/[id]
│   billing/checkout
└── admin/                     (admin)
    ├── dashboard, stores, users, orders, products, referrals, subscriptions,
    │   revenue, activity-logs, support, education, notifications, security
    ├── email-templates, email-templates/[type], email-templates/layout-settings
    ├── sandbox/invoices, sandbox/orders, sandbox/products, sandbox/returns
    ├── stores/[id], users/[id], support/[id]
    └── ...
```

## Dinamik Segmentler

| Segment | Kullanıldığı path | Açıklama |
|---------|-------------------|----------|
| [id] | /stock-tracking/[id], /support/[id]; /admin/stores/[id], /admin/users/[id], /admin/support/[id] | Tekil kayıt detayı. |
| [type] | /admin/email-templates/[type] | E-posta şablon türü. |
| [poId] | /purchasing/[poId] | Satın alma siparişi ID. |

**Kritik kural (CLAUDE.md):** Aynı path hiyerarşisinde dinamik segment adı tutarlı olmalı. Örn. `/api/stores/[id]/...` ile uyumlu; aynı dalda `[storeId]` kullanılmamalı.

## Özet

- **68** sayfa (page.tsx).
- **4** layout grubu: root, (auth), (public), (app-shell), (admin).
- **Dinamik:** [id] (5 yerde), [type] (1 yerde), [poId] (1 yerde).
