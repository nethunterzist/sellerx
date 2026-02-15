# Sayfa Listesi (Mevcut Kod)

Tüm Next.js sayfaları: path (locale sonrası), layout grubu, auth, dosya yolu. Kaynak: [sellerx-frontend/app/[locale]/](sellerx-frontend/app/[locale]/).

| Path (locale sonrası) | Layout | Auth | Dosya |
|----------------------|--------|------|-------|
| / | — | — | [locale]/page.tsx |
| /sign-in | (auth) | Hayır | [locale]/(auth)/sign-in/page.tsx |
| /register | (auth) | Hayır | [locale]/(auth)/register/page.tsx |
| /forgot-password | (auth) | Hayır | [locale]/(auth)/forgot-password/page.tsx |
| /reset-password | (auth) | Hayır | [locale]/(auth)/reset-password/page.tsx |
| /verify-email | (auth) | Hayır | [locale]/(auth)/verify-email/page.tsx |
| /verification-pending | (auth) | Hayır | [locale]/(auth)/verification-pending/page.tsx |
| /pricing | (public) | Hayır | [locale]/(public)/pricing/page.tsx |
| /hesaplama | (public) | Hayır | [locale]/(public)/hesaplama/page.tsx |
| /privacy | (public) | Hayır | [locale]/(public)/privacy/page.tsx |
| /terms | (public) | Hayır | [locale]/(public)/terms/page.tsx |
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
| /purchasing/orders | (app-shell) | Evet | [locale]/(app-shell)/purchasing/orders/page.tsx |
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
| /new-store | (app-shell) | Evet | [locale]/(app-shell)/new-store/page.tsx |
| /analytics | (app-shell) | Evet | [locale]/(app-shell)/analytics/page.tsx |
| /customer-analytics | (app-shell) | Evet | [locale]/(app-shell)/customer-analytics/page.tsx |
| /kar-hesaplama | (app-shell) | Evet | [locale]/(app-shell)/kar-hesaplama/page.tsx |
| /impersonate | (app-shell) | Evet | [locale]/(app-shell)/impersonate/page.tsx |
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
| /admin/email-templates | (admin) | Admin | [locale]/(admin)/admin/email-templates/page.tsx |
| /admin/email-templates/[type] | (admin) | Admin | [locale]/(admin)/admin/email-templates/[type]/page.tsx |
| /admin/email-templates/layout-settings | (admin) | Admin | [locale]/(admin)/admin/email-templates/layout-settings/page.tsx |
| /admin/sandbox/invoices | (admin) | Admin | [locale]/(admin)/admin/sandbox/invoices/page.tsx |
| /admin/sandbox/orders | (admin) | Admin | [locale]/(admin)/admin/sandbox/orders/page.tsx |
| /admin/sandbox/products | (admin) | Admin | [locale]/(admin)/admin/sandbox/products/page.tsx |
| /admin/sandbox/returns | (admin) | Admin | [locale]/(admin)/admin/sandbox/returns/page.tsx |
| /admin/security | (admin) | Admin | [locale]/(admin)/admin/security/page.tsx |

**Toplam:** 68 sayfa. Dinamik segmentler: [id] (stock-tracking, support; admin: stores, users, support), [type] (admin: email-templates), [poId] (purchasing).
