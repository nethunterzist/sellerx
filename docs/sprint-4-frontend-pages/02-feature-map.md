# Feature Haritası (Mevcut Kod)

Feature bazlı sayfa grupları. Kaynak: [01-page-list.md](01-page-list.md).

## App-shell (Satıcı)

| Feature | Sayfalar | Açıklama | İlgili hook |
|---------|----------|----------|-------------|
| dashboard | /dashboard | Ana panel, KPI ve grafikler | use-stats, use-stores |
| products | /products | Ürün listesi, maliyet/stok | use-products |
| orders | /orders | Sipariş listesi | use-orders |
| financial | /financial, /financial/invoices, /financial/settlement, /financial/vat | Finansal özet, faturalar, mutabakat, KDV | use-financial, use-invoices |
| purchasing | /purchasing, /purchasing/[poId], /purchasing/orders, /purchasing/suppliers, /purchasing/reports/cost-history, /purchasing/reports/stock-valuation | Satın alma siparişleri, tedarikçi, raporlar | use-purchasing, use-suppliers |
| billing | /billing, /billing/checkout | Abonelik, ödeme, checkout | use-billing |
| profit | /profit | Kârlılık analizi | use-stats, use-purchasing |
| expenses | /expenses | Gider yönetimi | use-expenses |
| alerts | /alerts | Uyarı kuralları ve geçmişi | use-alerts |
| stock-tracking | /stock-tracking, /stock-tracking/[id] | Stok takibi, detay | use-stock-tracking |
| qa | /qa | Müşteri soruları, pattern | use-qa |
| returns | /returns | İadeler, claims | use-returns |
| education | /education | Eğitim videoları | use-education |
| notifications | /notifications | Kullanıcı bildirimleri | use-notifications |
| support | /support, /support/[id] | Destek talepleri, detay | use-support |
| settings | /settings | Mağaza ayarları, webhook | use-settings, use-webhooks |
| customer-analytics | /customer-analytics | Müşteri davranış analizi, satın alma kalıpları, segmentler | use-customer-analytics |
| kar-hesaplama | /kar-hesaplama | Trendyol satıcıları için interaktif kâr/maliyet hesaplayıcı | — |
| impersonate | /impersonate | Admin impersonation token handler | use-auth |
| new-store | /new-store | Yeni mağaza ekleme (Trendyol) | use-stores |
| analytics | /analytics | Analitik | use-stats |
| dumen | /dumen | Dümen sayfası | — |
| kdv | /kdv | KDV sayfası | use-financial |

## Admin

| Feature | Sayfalar | Açıklama | İlgili hook |
|---------|----------|----------|-------------|
| admin-dashboard | /admin/dashboard | Admin ana panel | use-admin |
| admin-stores | /admin/stores, /admin/stores/[id] | Mağaza listesi ve detay | use-admin |
| admin-users | /admin/users, /admin/users/[id] | Kullanıcı listesi ve detay | use-admin |
| admin-orders | /admin/orders | Admin sipariş görünümü | use-admin-orders |
| admin-products | /admin/products | Admin ürün görünümü | use-admin |
| admin-referrals | /admin/referrals | Davet istatistikleri | use-admin-referrals |
| admin-subscriptions | /admin/subscriptions | Abonelik yönetimi | use-admin-billing |
| admin-revenue | /admin/revenue | Gelir özeti | use-admin |
| admin-activity-logs | /admin/activity-logs | Aktivite logları | use-admin-activity |
| admin-support | /admin/support, /admin/support/[id] | Destek talepleri yönetimi | use-admin |
| admin-education | /admin/education | Eğitim içeriği yönetimi | use-admin |
| admin-notifications | /admin/notifications | Bildirim yönetimi | use-admin-notification |
| admin-email-templates | /admin/email-templates, /admin/email-templates/[type], /admin/email-templates/layout-settings | E-posta şablon yönetimi, önizleme, düzenleme | use-admin-email-templates |
| admin-sandbox | /admin/sandbox/invoices, /admin/sandbox/orders, /admin/sandbox/products, /admin/sandbox/returns | Geliştirme için test veri üretimi | use-admin |
| admin-security | /admin/security | Güvenlik ayarları | use-admin |

## Auth

| Feature | Sayfalar | Açıklama | İlgili hook |
|---------|----------|----------|-------------|
| sign-in | /sign-in | Giriş | use-auth |
| register | /register | Kayıt | use-auth |
| forgot-password | /forgot-password | Şifre sıfırlama isteği | use-auth |
| reset-password | /reset-password | Yeni şifre belirleme | use-auth |
| verify-email | /verify-email | E-posta doğrulama handler | use-auth |
| verification-pending | /verification-pending | E-posta doğrulama bekleme durumu | use-auth |

## Public

| Feature | Sayfalar | Açıklama | İlgili hook |
|---------|----------|----------|-------------|
| pricing | /pricing | Fiyatlandırma (public) | — |
| hesaplama | /hesaplama | Trendyol komisyon hesaplayıcı (public) | — |
| privacy | /privacy | Gizlilik politikası | — |
| terms | /terms | Kullanım koşulları | — |

## Root

| Feature | Sayfalar | Açıklama |
|---------|----------|----------|
| home | / | Ana sayfa (yönlendirme) | — |

---

**Not:** `not-found.tsx` sayfa değil; 01'de listelenmez. Dinamik segmentler: [id] (stock-tracking, support; admin: stores, users, support), [type] (admin: email-templates), [poId] (purchasing).
