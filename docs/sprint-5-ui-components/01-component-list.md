# Bileşen Listesi (Mevcut Kod)

`sellerx-frontend/components/` altındaki klasörler ve ana bileşenler. Kaynak: [sellerx-frontend/components/](sellerx-frontend/components/).

## Klasör → Ana Bileşenler

| Klasör | Ana bileşenler (özet) | Açıklama |
|--------|------------------------|----------|
| ui | Shadcn primitives; ayrı liste 02-ui-primitives.md | Button, Card, Dialog, Table, Tabs, Sidebar vb. |
| layout | sidebar, header, app-layout, store-selector-dropdown | Uygulama layout, sidebar, mağaza seçici. |
| admin | admin-sidebar, admin-layout, admin-header; dashboard/*, education/* | Admin panel layout ve dashboard bileşenleri. |
| auth | login-form, register-form | Giriş ve kayıt formları. |
| dashboard | chart-view, cities-view, dashboard-chart, dashboard-filters, dashboard-tabs, period-cards, pl-view, trends-view, order-detail-panel, product-detail-panel, stock-depletion-banner | Dashboard grafik, filtre, dönem kartları, PL görünümü. |
| alerts | alert-rule-form-modal, notification-center | Uyarı kuralı formu, bildirim merkezi. |
| analytics | date-range-picker, order-metrics, product-performance-table, profit-breakdown-chart, revenue-trend-chart | Analitik grafikler ve tablolar. |
| billing | add-card-modal, feature-gate, invoice-list, payment-method-card, pricing-card, referral-tab, subscription-status-card | Abonelik, ödeme, fatura, referral. |
| education | video-player-modal | Eğitim video oynatıcı. |
| expenses | expense-list, expense-form-modal, expense-filters, expense-trend-chart, expense-category-chart, expense-stats-cards | Gider listesi, form, grafikler. |
| financial | invoice-table, invoice-detail-panel, invoice-summary-cards, invoice-category-table, invoice-export-button, product-commission-panel, product-cargo-panel | Fatura, komisyon, kargo paneli. |
| orders | order-stats-cards | Sipariş istatistik kartları. |
| products | cost-history-timeline, cost-edit-modal | Maliyet geçmişi, maliyet düzenleme. |
| profit | product-profit-table, profit-filters | Kârlılık tablosu, filtreler. |
| purchasing | po-form, po-list-table, po-items-table, po-status-cards, po-status-flow, po-attachments-tab, supplier-list-table, supplier-form-modal, add-product-modal, import-items-modal, split-po-modal; dashboard/recent-orders-list, quick-reports, monthly-summary-card | Satın alma siparişi formu, listeler, tedarikçi, raporlar. |
| qa | question-list, answer-modal, ai-answer-panel, conflict-alert-banner, conflict-detail-modal, knowledge-discovery-panel, pattern-card, qa-stats-cards, seniority-dashboard, seniority-stats-cards | Soru-cevap, pattern, conflict, seniority. |
| returns | claims-table, claim-detail-modal, reject-claim-modal, return-summary-cards, return-trend-chart, return-reasons-chart, return-cost-breakdown, top-returned-products | İade/claim listesi, detay, grafikler. |
| settings | settings-layout, settings-nav, settings-section; webhook-settings, stores-settings, profile-settings, security-settings, appearance-settings, language-settings, notification-settings, alert-rules-settings, ai-settings, subscription-settings, invoices-settings, sync-log-panel, shortcuts-dialog, data-export-settings, privacy-settings, activity-log-settings | Ayarlar layout, nav, bölümler; webhook, mağaza, profil, güvenlik vb. |
| stock-tracking | stock-tracked-products-table, add-product-modal, stock-alert-settings, stock-alerts-list, stock-dashboard-cards, stock-history-chart | Stok takip tablosu, uyarılar, grafik. |
| stores | sync-status-display | Mağaza sync durumu. |
| support | ticket-list, ticket-messages, message-input, ticket-form-modal, ticket-status-badge, ticket-priority-badge, ticket-category-badge | Destek talepleri, mesajlar, badge’ler. |
| providers | theme-provider, error-boundary, keyboard-shortcuts-provider | Tema, hata sınırı, kısayol provider. |
| (root) | store-sync-progress | Mağaza sync ilerleme (tek dosya). |

**Toplam:** 23 klasör + root'ta 1 bileşen. Bileşen sayısı: ~184 .tsx dosyası (ui dahil).
