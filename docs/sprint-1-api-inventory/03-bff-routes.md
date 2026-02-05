# BFF Route Envanteri

Next.js API routes (`app/api/**/route.ts`). BFF path = dosya yolundan (app/api kısmı /api/... olarak). Backend path = fetch içinde API_BASE_URL sonrası path (query string hariç özet).

## Açıklama

- **BFF path:** Tarayıcı/client'ın çağırdığı path (Next.js route). Dinamik segmentler `[id]`, `[storeId]` vb.
- **Backend path:** BFF'nin çağırdığı backend URL path (API_BASE_URL sonrası). Parametreler `{id}`, `{storeId}` ile gösterilir.
- Auth login/logout/refresh: Cookie forwarding; backend path aynı.

---

## Auth

| BFF path | HTTP | Backend path | route.ts |
|----------|------|--------------|----------|
| /api/auth/login | POST | /auth/login | app/api/auth/login/route.ts |
| /api/auth/logout | POST | /auth/logout | app/api/auth/logout/route.ts |
| /api/auth/refresh | POST | /auth/refresh | app/api/auth/refresh/route.ts |
| /api/auth/me | GET | /auth/me | app/api/auth/me/route.ts |
| /api/auth/register | POST | /users | app/api/auth/register/route.ts |

---

## Stores

| BFF path | HTTP | Backend path | route.ts |
|----------|------|--------------|----------|
| /api/stores | GET, POST | /stores | app/api/stores/route.ts |
| /api/stores/my | GET | /stores/my | app/api/stores/my/route.ts |
| /api/stores/[id] | GET, PUT, DELETE | /stores/{id} | app/api/stores/[id]/route.ts |
| /api/stores/[id]/retry-sync | POST | /stores/{id}/retry-sync | app/api/stores/[id]/retry-sync/route.ts |
| /api/stores/[id]/cancel-sync | POST | /stores/{id}/cancel-sync | app/api/stores/[id]/cancel-sync/route.ts |
| /api/stores/[id]/sync-progress | GET | /stores/{id}/sync-progress | app/api/stores/[id]/sync-progress/route.ts |
| /api/stores/[id]/test-connection | POST | /trendyol/test-connection | app/api/stores/[id]/test-connection/route.ts |
| /api/stores/[id]/webhooks/status | GET | /api/stores/{id}/webhooks/status | app/api/stores/[id]/webhooks/status/route.ts |
| /api/stores/[id]/webhooks/enable | POST | /api/stores/{id}/webhooks/enable | app/api/stores/[id]/webhooks/enable/route.ts |
| /api/stores/[id]/webhooks/disable | POST | /api/stores/{id}/webhooks/disable | app/api/stores/[id]/webhooks/disable/route.ts |
| /api/stores/[id]/webhooks/events | GET | /api/stores/{id}/webhooks/events | app/api/stores/[id]/webhooks/events/route.ts |
| /api/stores/[id]/webhooks/test | POST | /api/stores/{id}/webhooks/test | app/api/stores/[id]/webhooks/test/route.ts |
| /api/stores/test-credentials | POST | /trendyol/test-credentials | app/api/stores/test-credentials/route.ts |
| /api/stores/test-connection | POST | /trendyol/test-connection | app/api/stores/test-connection/route.ts |

---

## Dashboard

| BFF path | HTTP | Backend path | route.ts |
|----------|------|--------------|----------|
| /api/dashboard/stats/[id] | GET | /dashboard/stats/{id} | app/api/dashboard/stats/[id]/route.ts |
| /api/dashboard/stats/[id]/range | GET | /dashboard/stats/{id}/range | app/api/dashboard/stats/[id]/range/route.ts |
| /api/dashboard/stats/[id]/cities | GET | /dashboard/stats/{id}/cities | app/api/dashboard/stats/[id]/cities/route.ts |
| /api/dashboard/stats/[id]/multi-period | GET | /dashboard/stats/{id}/multi-period | app/api/dashboard/stats/[id]/multi-period/route.ts |

---

## Products

| BFF path | HTTP | Backend path | route.ts |
|----------|------|--------------|----------|
| /api/products | GET, POST | /products | app/api/products/route.ts |
| /api/products/store/[id] | GET | /products/store/{id} | app/api/products/store/[id]/route.ts |
| /api/products/store/[id]/bulk-cost-update | POST | /products/store/{id}/bulk-cost-update | app/api/products/store/[id]/bulk-cost-update/route.ts |
| /api/products/sync/[storeId] | POST | /products/sync/{storeId} | app/api/products/sync/[storeId]/route.ts |
| /api/products/[id]/cost-and-stock | PUT | /products/{id}/cost-and-stock | app/api/products/[id]/cost-and-stock/route.ts |
| /api/products/[id]/stock-info | POST | /products/{id}/stock-info | app/api/products/[id]/stock-info/route.ts |

---

## Orders

| BFF path | HTTP | Backend path | route.ts |
|----------|------|--------------|----------|
| /api/orders/stores/[storeId] | GET | /api/orders/stores/{storeId} | app/api/orders/stores/[storeId]/route.ts |
| /api/orders/stores/[storeId]/sync | POST | /orders/stores/{storeId}/sync | app/api/orders/stores/[storeId]/sync/route.ts |
| /api/orders/stores/[storeId]/by-date-range | GET | /orders/stores/{storeId}/by-date-range | app/api/orders/stores/[storeId]/by-date-range/route.ts |
| /api/orders/stores/[storeId]/by-status | GET | /api/orders/stores/{storeId}/by-status | app/api/orders/stores/[storeId]/by-status/route.ts |
| /api/orders/stores/[storeId]/statistics | GET | /api/orders/stores/{storeId}/statistics | app/api/orders/stores/[storeId]/statistics/route.ts |
| /api/orders/stock-sync/status/[storeId] | GET | /api/orders/stock-sync/status/{storeId} | app/api/orders/stock-sync/status/[storeId]/route.ts |
| /api/orders/stock-sync/synchronize/[storeId] | POST | /api/orders/stock-sync/synchronize/{storeId} | app/api/orders/stock-sync/synchronize/[storeId]/route.ts |

---

## Financial

| BFF path | HTTP | Backend path | route.ts |
|----------|------|--------------|----------|
| /api/financial/stores/[storeId]/stats | GET | /api/financial/stores/{storeId}/stats | app/api/financial/stores/[storeId]/stats/route.ts |
| /api/financial/stores/[storeId]/sync | POST | /api/financial/stores/{storeId}/sync | app/api/financial/stores/[storeId]/sync/route.ts |

---

## Invoices (Trendyol financial)

| BFF path | HTTP | Backend path | route.ts |
|----------|------|--------------|----------|
| /api/invoices/stores/[storeId] | GET | /api/invoices/stores/{storeId} | app/api/invoices/stores/[storeId]/route.ts |
| /api/invoices/stores/[storeId]/summary | GET | /api/invoices/stores/{storeId}/summary | app/api/invoices/stores/[storeId]/summary/route.ts |
| /api/invoices/stores/[storeId]/sync | POST | /api/invoices/stores/{storeId}/sync | app/api/invoices/stores/[storeId]/sync/route.ts |
| /api/invoices/stores/[storeId]/by-type/[typeCode] | GET | /api/invoices/stores/{storeId}/by-type/{typeCode} | app/api/invoices/stores/[storeId]/by-type/[typeCode]/route.ts |
| /api/invoices/stores/[storeId]/by-category/[category] | GET | /api/invoices/stores/{storeId}/by-category/{category} | app/api/invoices/stores/[storeId]/by-category/[category]/route.ts |
| /api/invoices/stores/[storeId]/cargo-items/[invoiceSerialNumber] | GET | /api/invoices/stores/{storeId}/cargo-items/{invoiceSerialNumber} | app/api/invoices/stores/[storeId]/cargo-items/[invoiceSerialNumber]/route.ts |
| /api/invoices/stores/[storeId]/items/[invoiceSerialNumber] | GET | /api/invoices/stores/{storeId}/items/{invoiceSerialNumber} | app/api/invoices/stores/[storeId]/items/[invoiceSerialNumber]/route.ts |
| /api/invoices/stores/[storeId]/commission-items/[invoiceSerialNumber] | GET | /api/invoices/stores/{storeId}/commission-items/{invoiceSerialNumber} | app/api/invoices/stores/[storeId]/commission-items/[invoiceSerialNumber]/route.ts |
| /api/invoices/stores/[storeId]/category/cargo-items | GET | /api/invoices/stores/{storeId}/category/cargo-items | app/api/invoices/stores/[storeId]/category/cargo-items/route.ts |
| /api/invoices/stores/[storeId]/category/commission-items | GET | /api/invoices/stores/{storeId}/category/commission-items | app/api/invoices/stores/[storeId]/category/commission-items/route.ts |
| /api/invoices/stores/[storeId]/category/[category]/products | GET | /api/invoices/stores/{storeId}/category/{category}/products | app/api/invoices/stores/[storeId]/category/[category]/products/route.ts |
| /api/invoices/stores/[storeId]/products/[barcode]/commission-breakdown | GET | /api/invoices/stores/{storeId}/products/{barcode}/commission-breakdown | app/api/invoices/stores/[storeId]/products/[barcode]/commission-breakdown/route.ts |
| /api/invoices/stores/[storeId]/products/[barcode]/cargo-breakdown | GET | /api/invoices/stores/{storeId}/products/{barcode}/cargo-breakdown | app/api/invoices/stores/[storeId]/products/[barcode]/cargo-breakdown/route.ts |

---

## Purchasing (Purchase Orders)

| BFF path | HTTP | Backend path | route.ts |
|----------|------|--------------|----------|
| /api/purchasing/orders/[storeId] | GET, POST | /api/stores/{storeId}/purchase-orders | app/api/purchasing/orders/[storeId]/route.ts |
| /api/purchasing/orders/[storeId]/stats | GET | /api/stores/{storeId}/purchase-orders/stats | app/api/purchasing/orders/[storeId]/stats/route.ts |
| /api/purchasing/orders/[storeId]/[poId] | GET, PUT, DELETE | /api/stores/{storeId}/purchase-orders/{poId} | app/api/purchasing/orders/[storeId]/[poId]/route.ts |
| /api/purchasing/orders/[storeId]/[poId]/status | PUT | /api/stores/{storeId}/purchase-orders/{poId}/status | app/api/purchasing/orders/[storeId]/[poId]/status/route.ts |
| /api/purchasing/orders/[storeId]/[poId]/items | POST | /api/stores/{storeId}/purchase-orders/{poId}/items | app/api/purchasing/orders/[storeId]/[poId]/items/route.ts |
| /api/purchasing/orders/[storeId]/[poId]/items/[itemId] | PUT, DELETE | /api/stores/{storeId}/purchase-orders/{poId}/items/{itemId} | app/api/purchasing/orders/[storeId]/[poId]/items/[itemId]/route.ts |
| /api/purchasing/orders/[storeId]/[poId]/duplicate | POST | /api/stores/{storeId}/purchase-orders/{poId}/duplicate | app/api/purchasing/orders/[storeId]/[poId]/duplicate/route.ts |
| /api/purchasing/orders/[storeId]/[poId]/split | POST | /api/stores/{storeId}/purchase-orders/{poId}/split | app/api/purchasing/orders/[storeId]/[poId]/split/route.ts |
| /api/purchasing/orders/[storeId]/[poId]/attachments | GET, POST | /api/stores/{storeId}/purchase-orders/{poId}/attachments | app/api/purchasing/orders/[storeId]/[poId]/attachments/route.ts |
| /api/purchasing/orders/[storeId]/[poId]/attachments/[attachmentId] | GET, DELETE | /api/stores/{storeId}/purchase-orders/{poId}/attachments/{attachmentId} veya .../download | app/api/purchasing/orders/[storeId]/[poId]/attachments/[attachmentId]/route.ts |
| /api/purchasing/orders/[storeId]/[poId]/export | GET | /api/stores/{storeId}/purchase-orders/{poId}/export | app/api/purchasing/orders/[storeId]/[poId]/export/route.ts |
| /api/purchasing/orders/[storeId]/[poId]/import | POST | /api/stores/{storeId}/purchase-orders/{poId}/import | app/api/purchasing/orders/[storeId]/[poId]/import/route.ts |
| /api/purchasing/orders/[storeId]/reports/summary | GET | /api/stores/{storeId}/purchase-orders/reports/summary | app/api/purchasing/orders/[storeId]/reports/summary/route.ts |
| /api/purchasing/orders/[storeId]/reports/profitability | GET | /api/stores/{storeId}/purchase-orders/reports/profitability | app/api/purchasing/orders/[storeId]/reports/profitability/route.ts |
| /api/purchasing/orders/[storeId]/reports/stock-valuation | GET | /api/stores/{storeId}/purchase-orders/reports/stock-valuation | app/api/purchasing/orders/[storeId]/reports/stock-valuation/route.ts |
| /api/purchasing/orders/[storeId]/reports/fifo-analysis | GET | /api/stores/{storeId}/purchase-orders/reports/fifo-analysis | app/api/purchasing/orders/[storeId]/reports/fifo-analysis/route.ts |
| /api/purchasing/orders/[storeId]/reports/stock-depletion | GET | /api/stores/{storeId}/purchase-orders/reports/stock-depletion | app/api/purchasing/orders/[storeId]/reports/stock-depletion/route.ts |
| /api/purchasing/orders/[storeId]/reports/product/[productId]/cost-history | GET | /api/stores/{storeId}/purchase-orders/reports/product/{productId}/cost-history | app/api/purchasing/orders/[storeId]/reports/product/[productId]/cost-history/route.ts |

---

## Suppliers

| BFF path | HTTP | Backend path | route.ts |
|----------|------|--------------|----------|
| /api/suppliers/store/[storeId] | GET, POST | /api/stores/{storeId}/suppliers | app/api/suppliers/store/[storeId]/route.ts |
| /api/suppliers/store/[storeId]/[supplierId] | GET, PUT, DELETE | /api/stores/{storeId}/suppliers/{supplierId} | app/api/suppliers/store/[storeId]/[supplierId]/route.ts |

---

## Categories

| BFF path | HTTP | Backend path | route.ts |
|----------|------|--------------|----------|
| /api/categories | GET, POST | /api/categories | app/api/categories/route.ts |

---

## Expenses

| BFF path | HTTP | Backend path | route.ts |
|----------|------|--------------|----------|
| /api/expenses/categories | GET | /expenses/categories | app/api/expenses/categories/route.ts |
| /api/expenses/store/[storeId] | GET, POST | /expenses/store/{storeId} | app/api/expenses/store/[storeId]/route.ts |
| /api/expenses/store/[storeId]/[expenseId] | PUT, DELETE | /expenses/store/{storeId}/{expenseId} | app/api/expenses/store/[storeId]/[expenseId]/route.ts |

---

## Alerts

| BFF path | HTTP | Backend path | route.ts |
|----------|------|--------------|----------|
| /api/alert-rules | GET, POST | /api/alert-rules | app/api/alert-rules/route.ts |
| /api/alert-rules/count | GET | /api/alert-rules/count | app/api/alert-rules/count/route.ts |
| /api/alert-rules/[id] | GET, PUT, DELETE | /api/alert-rules/{id} | app/api/alert-rules/[id]/route.ts |
| /api/alert-rules/[id]/toggle | PUT | /api/alert-rules/{id}/toggle | app/api/alert-rules/[id]/toggle/route.ts |
| /api/alerts | GET | /api/alerts | app/api/alerts/route.ts |
| /api/alerts/unread | GET | /api/alerts/unread | app/api/alerts/unread/route.ts |
| /api/alerts/unread-count | GET | /api/alerts/unread-count | app/api/alerts/unread-count/route.ts |
| /api/alerts/recent | GET | /api/alerts/recent | app/api/alerts/recent/route.ts |
| /api/alerts/stats | GET | /api/alerts/stats | app/api/alerts/stats/route.ts |
| /api/alerts/[id] | GET | /api/alerts/{id} | app/api/alerts/[id]/route.ts |
| /api/alerts/[id]/read | PUT | /api/alerts/{id}/read | app/api/alerts/[id]/read/route.ts |
| /api/alerts/read-all | PUT | /api/alerts/read-all | app/api/alerts/read-all/route.ts |

---

## Stock Tracking

| BFF path | HTTP | Backend path | route.ts |
|----------|------|--------------|----------|
| /api/stock-tracking/preview | GET | /api/stock-tracking/preview | app/api/stock-tracking/preview/route.ts |
| /api/stock-tracking/stores/[storeId]/dashboard | GET | /api/stock-tracking/stores/{storeId}/dashboard | app/api/stock-tracking/stores/[storeId]/dashboard/route.ts |
| /api/stock-tracking/stores/[storeId]/products | GET, POST | /api/stock-tracking/stores/{storeId}/products | app/api/stock-tracking/stores/[storeId]/products/route.ts |
| /api/stock-tracking/products/[id] | GET, DELETE | /api/stock-tracking/products/{id} | app/api/stock-tracking/products/[id]/route.ts |
| /api/stock-tracking/products/[id]/settings | PUT | /api/stock-tracking/products/{id}/settings | app/api/stock-tracking/products/[id]/settings/route.ts |
| /api/stock-tracking/products/[id]/check | POST | /api/stock-tracking/products/{id}/check | app/api/stock-tracking/products/[id]/check/route.ts |
| /api/stock-tracking/stores/[storeId]/alerts | GET | /api/stock-tracking/stores/{storeId}/alerts | app/api/stock-tracking/stores/[storeId]/alerts/route.ts |
| /api/stock-tracking/stores/[storeId]/alerts/mark-all-read | PUT | /api/stock-tracking/stores/{storeId}/alerts/mark-all-read | app/api/stock-tracking/stores/[storeId]/alerts/mark-all-read/route.ts |
| /api/stock-tracking/alerts/[id]/read | PUT | /api/stock-tracking/alerts/{id}/read | app/api/stock-tracking/alerts/[id]/read/route.ts |

---

## Returns & Claims

| BFF path | HTTP | Backend path | route.ts |
|----------|------|--------------|----------|
| /api/returns/claim-issue-reasons | GET | /api/returns/claim-issue-reasons | app/api/returns/claim-issue-reasons/route.ts |
| /api/returns/stores/[storeId]/stats | GET | /api/returns/stores/{storeId}/stats | app/api/returns/stores/[storeId]/stats/route.ts |
| /api/returns/stores/[storeId]/claims | GET | /api/returns/stores/{storeId}/claims | app/api/returns/stores/[storeId]/claims/route.ts |
| /api/returns/stores/[storeId]/claims/[claimId] | GET | /api/returns/stores/{storeId}/claims/{claimId} | app/api/returns/stores/[storeId]/claims/[claimId]/route.ts |
| /api/returns/stores/[storeId]/claims/sync | POST | /api/returns/stores/{storeId}/claims/sync | app/api/returns/stores/[storeId]/claims/sync/route.ts |
| /api/returns/stores/[storeId]/claims/[claimId]/approve | PUT | /api/returns/stores/{storeId}/claims/{claimId}/approve | app/api/returns/stores/[storeId]/claims/[claimId]/approve/route.ts |
| /api/returns/stores/[storeId]/claims/[claimId]/reject | POST | /api/returns/stores/{storeId}/claims/{claimId}/reject | app/api/returns/stores/[storeId]/claims/[claimId]/reject/route.ts |
| /api/returns/stores/[storeId]/claims/bulk-approve | POST | /api/returns/stores/{storeId}/claims/bulk-approve | app/api/returns/stores/[storeId]/claims/bulk-approve/route.ts |

---

## QA

| BFF path | HTTP | Backend path | route.ts |
|----------|------|--------------|----------|
| /api/qa/stores/[storeId]/questions | GET | /qa/stores/{storeId}/questions | app/api/qa/stores/[storeId]/questions/route.ts |
| /api/qa/stores/[storeId]/questions/sync | POST | /qa/stores/{storeId}/questions/sync | app/api/qa/stores/[storeId]/questions/sync/route.ts |
| /api/qa/stores/[storeId]/questions/[questionId]/answer | POST | /qa/stores/{storeId}/questions/{questionId}/answer | app/api/qa/stores/[storeId]/questions/[questionId]/answer/route.ts |
| /api/qa/stores/[storeId]/stats | GET | /qa/stores/{storeId}/stats | app/api/qa/stores/[storeId]/stats/route.ts |
| /api/qa/questions/[id] | GET | /qa/questions/{id} | app/api/qa/questions/[id]/route.ts |
| /api/qa/questions/[id]/ai-generate | POST | /qa/questions/{id}/ai-generate | app/api/qa/questions/[id]/ai-generate/route.ts |
| /api/qa/questions/[id]/ai-approve | POST | /qa/questions/{id}/ai-approve | app/api/qa/questions/[id]/ai-approve/route.ts |
| /api/qa/stores/[storeId]/suggestions | GET | /qa/stores/{storeId}/suggestions | app/api/qa/stores/[storeId]/suggestions/route.ts |
| /api/qa/suggestions/[id]/approve | POST | /qa/suggestions/{id}/approve | app/api/qa/suggestions/[id]/approve/route.ts |
| /api/qa/suggestions/[id]/reject | POST | /qa/suggestions/{id}/reject | app/api/qa/suggestions/[id]/reject/route.ts |
| /api/qa/suggestions/[id]/modify | POST | /qa/suggestions/{id}/modify | app/api/qa/suggestions/[id]/modify/route.ts |
| /api/qa/stores/[storeId]/patterns | GET | /qa/stores/{storeId}/patterns | app/api/qa/stores/[storeId]/patterns/route.ts |
| /api/qa/stores/[storeId]/patterns/stats | GET | /qa/stores/{storeId}/patterns/stats | app/api/qa/stores/[storeId]/patterns/stats/route.ts |
| /api/qa/patterns/[id]/promote | POST | /qa/patterns/{id}/promote | app/api/qa/patterns/[id]/promote/route.ts |
| /api/qa/patterns/[id]/demote | POST | /qa/patterns/{id}/demote | app/api/qa/patterns/[id]/demote/route.ts |
| /api/qa/patterns/[id]/enable-auto-submit | POST | /qa/patterns/{id}/enable-auto-submit | app/api/qa/patterns/[id]/enable-auto-submit/route.ts |
| /api/qa/stores/[storeId]/conflicts | GET | /qa/stores/{storeId}/conflicts | app/api/qa/stores/[storeId]/conflicts/route.ts |
| /api/qa/stores/[storeId]/conflicts/stats | GET | /qa/stores/{storeId}/conflicts/stats | app/api/qa/stores/[storeId]/conflicts/stats/route.ts |
| /api/qa/conflicts/[id]/resolve | POST | /qa/conflicts/{id}/resolve | app/api/qa/conflicts/[id]/resolve/route.ts |
| /api/qa/conflicts/[id]/dismiss | POST | /qa/conflicts/{id}/dismiss | app/api/qa/conflicts/[id]/dismiss/route.ts |

---

## Referrals

| BFF path | HTTP | Backend path | route.ts |
|----------|------|--------------|----------|
| /api/referrals/stats | GET | /api/referrals/stats | app/api/referrals/stats/route.ts |
| /api/referrals/code | POST | /api/referrals/code | app/api/referrals/code/route.ts |
| /api/referrals/validate/[code] | GET | /api/referrals/validate/{code} | app/api/referrals/validate/[code]/route.ts |

---

## Support

| BFF path | HTTP | Backend path | route.ts |
|----------|------|--------------|----------|
| /api/support/tickets | GET, POST | /api/support/tickets | app/api/support/tickets/route.ts |
| /api/support/tickets/[id] | GET | /api/support/tickets/{id} | app/api/support/tickets/[id]/route.ts |
| /api/support/tickets/[id]/messages | POST | /api/support/tickets/{id}/messages | app/api/support/tickets/[id]/messages/route.ts |

---

## Billing

| BFF path | HTTP | Backend path | route.ts |
|----------|------|--------------|----------|
| /api/billing/plans | GET | /api/billing/plans | app/api/billing/plans/route.ts |
| /api/billing/features | GET | /api/billing/features | app/api/billing/features/route.ts |
| /api/billing/checkout/start | POST | /api/billing/checkout/start | app/api/billing/checkout/start/route.ts |
| /api/billing/invoices | GET | /api/billing/invoices | app/api/billing/invoices/route.ts |
| /api/billing/invoices/[id] | GET | /api/billing/invoices/{id} | app/api/billing/invoices/[id]/route.ts |
| /api/billing/payment-methods | GET, POST | /api/billing/payment-methods | app/api/billing/payment-methods/route.ts |
| /api/billing/payment-methods/[id] | DELETE | /api/billing/payment-methods/{id} | app/api/billing/payment-methods/[id]/route.ts |
| /api/billing/payment-methods/[id]/default | PUT | /api/billing/payment-methods/{id}/default | app/api/billing/payment-methods/[id]/default/route.ts |
| /api/billing/subscription | GET | /api/billing/subscription | app/api/billing/subscription/route.ts |
| /api/billing/subscription/trial | POST | /api/billing/subscription/trial | app/api/billing/subscription/trial/route.ts |
| /api/billing/subscription/cancel | POST | /api/billing/subscription/cancel | app/api/billing/subscription/cancel/route.ts |

---

## Education

| BFF path | HTTP | Backend path | route.ts |
|----------|------|--------------|----------|
| /api/education/videos | GET, POST | /api/education/videos | app/api/education/videos/route.ts |
| /api/education/videos/[id] | GET, PUT, DELETE | /api/education/videos/{id} | app/api/education/videos/[id]/route.ts |
| /api/education/videos/my-watch-status | GET | /api/education/videos/my-watch-status | app/api/education/videos/my-watch-status/route.ts |
| /api/education/videos/[id]/watch | POST | /api/education/videos/{id}/watch | app/api/education/videos/[id]/watch/route.ts |

---

## Notifications

| BFF path | HTTP | Backend path | route.ts |
|----------|------|--------------|----------|
| /api/notifications | GET | /api/notifications | app/api/notifications/route.ts |
| /api/notifications/unread-count | GET | /api/notifications/unread-count | app/api/notifications/unread-count/route.ts |
| /api/notifications/[id]/read | PUT | /api/notifications/{id}/read | app/api/notifications/[id]/read/route.ts |

---

## Users

| BFF path | HTTP | Backend path | route.ts |
|----------|------|--------------|----------|
| /api/users/profile | GET, PUT | /users/profile | app/api/users/profile/route.ts |
| /api/users/password | PUT | /users/password | app/api/users/password/route.ts |
| /api/users/preferences | GET, PUT | /users/preferences | app/api/users/preferences/route.ts |
| /api/users/selected-store | GET, PUT | /users/selected-store | app/api/users/selected-store/route.ts |
| /api/users/activity-logs | GET | /users/activity-logs | app/api/users/activity-logs/route.ts |

---

## AI & Knowledge

| BFF path | HTTP | Backend path | route.ts |
|----------|------|--------------|----------|
| /api/ai-settings/stores/[storeId] | GET, PUT | /api/ai-settings/stores/{storeId} | app/api/ai-settings/stores/[storeId]/route.ts |
| /api/knowledge/stores/[storeId] | GET, POST | /api/knowledge/stores/{storeId} | app/api/knowledge/stores/[storeId]/route.ts |
| /api/knowledge/[id] | PUT, DELETE | /api/knowledge/{id} | app/api/knowledge/[id]/route.ts |
| /api/knowledge/[id]/toggle | PATCH | /api/knowledge/{id}/toggle | app/api/knowledge/[id]/toggle/route.ts |

---

## Currency

| BFF path | HTTP | Backend path | route.ts |
|----------|------|--------------|----------|
| /api/currency/rates | GET | /currency/rates | app/api/currency/rates/route.ts |

---

## Admin

| BFF path | HTTP | Backend path | route.ts |
|----------|------|--------------|----------|
| /api/admin/stores | GET | /api/admin/stores | app/api/admin/stores/route.ts |
| /api/admin/stores/search | GET | /api/admin/stores/search | app/api/admin/stores/search/route.ts |
| /api/admin/stores/[id] | GET | /api/admin/stores/{id} | app/api/admin/stores/[id]/route.ts |
| /api/admin/stores/[id]/sync | POST | /api/admin/stores/{id}/sync | app/api/admin/stores/[id]/sync/route.ts |
| /api/admin/users | GET | /api/admin/users | app/api/admin/users/route.ts |
| /api/admin/users/search | GET | /api/admin/users/search | app/api/admin/users/search/route.ts |
| /api/admin/users/[id] | GET | /api/admin/users/{id} | app/api/admin/users/[id]/route.ts |
| /api/admin/users/[id]/role | PUT | /api/admin/users/{id}/role | app/api/admin/users/[id]/role/route.ts |
| /api/admin/users/export | GET | /api/admin/users/export | app/api/admin/users/export/route.ts |
| /api/admin/dashboard/stats | GET | /api/admin/dashboard/stats | app/api/admin/dashboard/stats/route.ts |
| /api/admin/orders/stats | GET | /api/admin/orders/stats | app/api/admin/orders/stats/route.ts |
| /api/admin/orders/recent | GET | /api/admin/orders/recent | app/api/admin/orders/recent/route.ts |
| /api/admin/products/stats | GET | /api/admin/products/stats | app/api/admin/products/stats/route.ts |
| /api/admin/products/top | GET | /api/admin/products/top | app/api/admin/products/top/route.ts |
| /api/admin/referrals | GET | /api/admin/referrals | app/api/admin/referrals/route.ts |
| /api/admin/referrals/stats | GET | /api/admin/referrals/stats | app/api/admin/referrals/stats/route.ts |
| /api/admin/activity-logs | GET | /api/admin/activity-logs | app/api/admin/activity-logs/route.ts |
| /api/admin/activity-logs/security | GET | /api/admin/activity-logs/security/summary | app/api/admin/activity-logs/security/route.ts |
| /api/admin/billing/subscriptions | GET | /api/admin/billing/subscriptions | app/api/admin/billing/subscriptions/route.ts |
| /api/admin/billing/revenue/stats | GET | /api/admin/billing/revenue/stats | app/api/admin/billing/revenue/stats/route.ts |
| /api/admin/billing/revenue/history | GET | /api/admin/billing/revenue/history | app/api/admin/billing/revenue/history/route.ts |
| /api/admin/billing/payments | GET | /api/admin/billing/payments | app/api/admin/billing/payments/route.ts |
| /api/admin/notifications/stats | GET | /api/admin/notifications/stats | app/api/admin/notifications/stats/route.ts |
| /api/admin/notifications/broadcast | POST | /api/admin/notifications/broadcast | app/api/admin/notifications/broadcast/route.ts |
| /api/admin/support/tickets | GET | /api/admin/support/tickets | app/api/admin/support/tickets/route.ts |
| /api/admin/support/tickets/active | GET | /api/admin/support/tickets/active | app/api/admin/support/tickets/active/route.ts |
| /api/admin/support/tickets/stats | GET | /api/admin/support/tickets/stats | app/api/admin/support/tickets/stats/route.ts |
| /api/admin/support/tickets/search | GET | /api/admin/support/tickets/search | app/api/admin/support/tickets/search/route.ts |
| /api/admin/support/tickets/[id] | GET | /api/admin/support/tickets/{id} | app/api/admin/support/tickets/[id]/route.ts |
| /api/admin/support/tickets/[id]/reply | POST | /api/admin/support/tickets/{id}/reply | app/api/admin/support/tickets/[id]/reply/route.ts |
| /api/admin/support/tickets/[id]/status | PUT | /api/admin/support/tickets/{id}/status | app/api/admin/support/tickets/[id]/status/route.ts |
| /api/admin/support/tickets/[id]/assign | PUT | /api/admin/support/tickets/{id}/assign | app/api/admin/support/tickets/[id]/assign/route.ts |
