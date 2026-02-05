# Sayfa Listesi (Mevcut Kod)

Tüm Next.js sayfaları: path (locale sonrası), layout grubu, auth, dosya yolu. Kaynak: [sellerx-frontend/app/[locale]/](sellerx-frontend/app/[locale]/).

| Path (locale sonrası) | Layout | Auth | Dosya |
|----------------------|--------|------|-------|
| / | — | — | [locale]/page.tsx |
| /sign-in | (auth) | Hayır | [locale]/(auth)/sign-in/page.tsx |
| /register | (auth) | Hayır | [locale]/(auth)/register/page.tsx |
| /pricing | (public) | Hayır | [locale]/(public)/pricing/page.tsx |
| /dashboard | (app-shell) | Evet | [locale]/(app-shell)/dashboard/page.tsx |
| /products | (app-shell) | Evet | [locale]/(app-shell)/products/page.tsx |
| /orders | (app-shell) | Evet | [locale]/(app-shell)/orders/page.tsx |
| /financial | (app-shell) | Evet | [locale]/(app-shell)/financial/page.tsx |
| /financial/invoices | (app-shell) | Evet | [locale]/(app-shell)/financial/invoices/page.tsx |
| /financial/settlement | (app-shell) | Evet | [locale]/(app-shell)/financial/settlement/page.tsx |
| /financial/vat | (app-shell) | Evet | [locale]/(app-shell)/financial/vat/page.tsx |
| /purchasing | (app-shell) | Evet | [locale]/(app-shell)/purchasing/page.tsx |
| /purchasing/[poId] | (app-shell) | Evet | [locale]/(app-shell)/purchasing/[poId]/page.tsx |
| /purchasing/suppliers | (app-shell) | Evet | [locale]/(app-shell)/purchasing/suppliers/page.tsx |
| /purchasing/reports/cost-history | (app-shell) | Evet | [locale]/(app-shell)/purchasing/reports/cost-history/page.tsx |
| /purchasing/reports/profitability | (app-shell) | Evet | [locale]/(app-shell)/purchasing/reports/profitability/page.tsx |
| /purchasing/reports/stock-valuation | (app-shell) | Evet | [locale]/(app-shell)/purchasing/reports/stock-valuation/page.tsx |
| /billing | (app-shell) | Evet | [locale]/(app-shell)/billing/page.tsx |
| /billing/checkout | (app-shell) | Evet | [locale]/(app-shell)/billing/checkout/page.tsx |
| /profit | (app-shell) | Evet | [locale]/(app-shell)/profit/page.tsx |
| /expenses | (app-shell) | Evet | [locale]/(app-shell)/expenses/page.tsx |
| /alerts | (app-shell) | Evet | [locale]/(app-shell)/alerts/page.tsx |
| /stock-tracking | (app-shell) | Evet | [locale]/(app-shell)/stock-tracking/page.tsx |
| /stock-tracking/[id] | (app-shell) | Evet | [locale]/(app-shell)/stock-tracking/[id]/page.tsx |
| /qa | (app-shell) | Evet | [locale]/(app-shell)/qa/page.tsx |
| /returns | (app-shell) | Evet | [locale]/(app-shell)/returns/page.tsx |
| /education | (app-shell) | Evet | [locale]/(app-shell)/education/page.tsx |
| /notifications | (app-shell) | Evet | [locale]/(app-shell)/notifications/page.tsx |
| /support | (app-shell) | Evet | [locale]/(app-shell)/support/page.tsx |
| /support/[id] | (app-shell) | Evet | [locale]/(app-shell)/support/[id]/page.tsx |
| /settings | (app-shell) | Evet | [locale]/(app-shell)/settings/page.tsx |
| /profile | (app-shell) | Evet | [locale]/(app-shell)/profile/page.tsx |
| /new-store | (app-shell) | Evet | [locale]/(app-shell)/new-store/page.tsx |
| /analytics | (app-shell) | Evet | [locale]/(app-shell)/analytics/page.tsx |
| /dumen | (app-shell) | Evet | [locale]/(app-shell)/dumen/page.tsx |
| /kdv | (app-shell) | Evet | [locale]/(app-shell)/kdv/page.tsx |
| /admin/dashboard | (admin) | Admin | [locale]/(admin)/admin/dashboard/page.tsx |
| /admin/stores | (admin) | Admin | [locale]/(admin)/admin/stores/page.tsx |
| /admin/stores/[id] | (admin) | Admin | [locale]/(admin)/admin/stores/[id]/page.tsx |
| /admin/users | (admin) | Admin | [locale]/(admin)/admin/users/page.tsx |
| /admin/users/[id] | (admin) | Admin | [locale]/(admin)/admin/users/[id]/page.tsx |
| /admin/orders | (admin) | Admin | [locale]/(admin)/admin/orders/page.tsx |
| /admin/products | (admin) | Admin | [locale]/(admin)/admin/products/page.tsx |
| /admin/referrals | (admin) | Admin | [locale]/(admin)/admin/referrals/page.tsx |
| /admin/subscriptions | (admin) | Admin | [locale]/(admin)/admin/subscriptions/page.tsx |
| /admin/revenue | (admin) | Admin | [locale]/(admin)/admin/revenue/page.tsx |
| /admin/activity-logs | (admin) | Admin | [locale]/(admin)/admin/activity-logs/page.tsx |
| /admin/support | (admin) | Admin | [locale]/(admin)/admin/support/page.tsx |
| /admin/support/[id] | (admin) | Admin | [locale]/(admin)/admin/support/[id]/page.tsx |
| /admin/education | (admin) | Admin | [locale]/(admin)/admin/education/page.tsx |
| /admin/notifications | (admin) | Admin | [locale]/(admin)/admin/notifications/page.tsx |
| /admin/security | (admin) | Admin | [locale]/(admin)/admin/security/page.tsx |

**Toplam:** 52 sayfa. Dinamik segmentler: [id] (stock-tracking, support; admin: stores, users, support), [poId] (purchasing).
