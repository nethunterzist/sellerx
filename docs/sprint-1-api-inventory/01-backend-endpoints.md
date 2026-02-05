# Backend Endpoint Envanteri

Tüm REST controller'lar ve endpoint'ler. ExceptionHandler, ControllerAdvice, Test, billing-disabled hariç.

## Gruplar

- [Auth](#auth)
- [Stores](#stores)
- [Users & ActivityLog](#users--activitylog)
- [Dashboard](#dashboard)
- [Products](#products)
- [Orders](#orders)
- [Financial & Invoices](#financial--invoices)
- [Purchasing & Suppliers](#purchasing--suppliers)
- [Categories](#categories)
- [Expenses](#expenses)
- [Webhooks](#webhooks)
- [Alerts](#alerts)
- [Stock Tracking](#stock-tracking)
- [Returns & Claims](#returns--claims)
- [QA](#qa)
- [Referrals](#referrals)
- [Support](#support)
- [Billing](#billing)
- [Education](#education)
- [Notifications](#notifications)
- [AI & Knowledge](#ai--knowledge)
- [Currency](#currency)
- [Trendyol & Health](#trendyol--health)
- [Admin](#admin)
- [Test](#test)

---

## Auth

| Controller | Base path | Method path | HTTP | Full path | Path/query params | Not |
|------------|-----------|-------------|------|-----------|-------------------|-----|
| AuthController | /auth | /login | POST | /auth/login | body | Public |
| AuthController | /auth | /logout | POST | /auth/logout | - | Public |
| AuthController | /auth | /refresh | POST | /auth/refresh | - | Public |
| AuthController | /auth | /me | GET | /auth/me | - | Auth |

---

## Stores

| Controller | Base path | Method path | HTTP | Full path | Path/query params | Not |
|------------|-----------|-------------|------|-----------|-------------------|-----|
| StoreController | /stores | /my | GET | /stores/my | - | Auth |
| StoreController | /stores | | GET | /stores | sort (query) | Admin |
| StoreController | /stores | /{id} | GET | /stores/{id} | id: UUID | Auth |
| StoreController | /stores | | POST | /stores | body | Auth |
| StoreController | /stores | /{id} | PUT | /stores/{id} | id: UUID | Auth |
| StoreController | /stores | /{id} | DELETE | /stores/{id} | id: UUID | Auth |
| StoreController | /stores | /{id}/retry-sync | POST | /stores/{id}/retry-sync | id: UUID | Auth |
| StoreController | /stores | /{id}/cancel-sync | POST | /stores/{id}/cancel-sync | id: UUID | Auth |
| StoreController | /stores | /{id}/sync-status | GET | /stores/{id}/sync-status | id: UUID | Auth |
| StoreController | /stores | /{id}/sync-progress | GET | /stores/{id}/sync-progress | id: UUID | Auth |

---

## Users & ActivityLog

| Controller | Base path | Method path | HTTP | Full path | Path/query params | Not |
|------------|-----------|-------------|------|-----------|-------------------|-----|
| UserController | /users | | GET | /users | sort (query) | Auth |
| UserController | /users | /{id} | GET | /users/{id} | id: Long | Auth |
| UserController | /users | | POST | /users | body (register) | Public |
| UserController | /users | /{id} | PUT | /users/{id} | id: Long | Auth |
| UserController | /users | /{id} | DELETE | /users/{id} | id: Long | Auth |
| UserController | /users | /{id}/change-password | POST | /users/{id}/change-password | id: Long | Auth |
| UserController | /users | /selected-store | GET | /users/selected-store | - | Auth |
| UserController | /users | /profile | GET | /users/profile | - | Auth |
| UserController | /users | /profile | PUT | /users/profile | body | Auth |
| UserController | /users | /password | PUT | /users/password | body | Auth |
| UserController | /users | /preferences | GET | /users/preferences | - | Auth |
| UserController | /users | /preferences | PUT | /users/preferences | body | Auth |
| UserController | /users | /selected-store | POST | /users/selected-store | body | Auth |
| ActivityLogController | /users | /activity-logs | GET | /users/activity-logs | limit (query) | Auth |

---

## Dashboard

| Controller | Base path | Method path | HTTP | Full path | Path/query params | Not |
|------------|-----------|-------------|------|-----------|-------------------|-----|
| DashboardController | /dashboard | /stats | GET | /dashboard/stats | - | Auth |
| DashboardController | /dashboard | /stats/{storeId} | GET | /dashboard/stats/{storeId} | storeId | Auth |
| DashboardController | /dashboard | /stats/{storeId}/range | GET | /dashboard/stats/{storeId}/range | storeId, startDate, endDate, periodLabel | Auth |
| DashboardController | /dashboard | /stats/{storeId}/cities | GET | /dashboard/stats/{storeId}/cities | storeId, startDate, endDate, productBarcode | Auth |
| DashboardController | /dashboard | /stats/{storeId}/multi-period | GET | /dashboard/stats/{storeId}/multi-period | storeId, periodType, periodCount, productBarcode | Auth |

---

## Products

| Controller | Base path | Method path | HTTP | Full path | Path/query params | Not |
|------------|-----------|-------------|------|-----------|-------------------|-----|
| TrendyolProductController | /products | /sync/{storeId} | POST | /products/sync/{storeId} | storeId: UUID | Auth |
| TrendyolProductController | /products | /store/{storeId} | GET | /products/store/{storeId} | storeId, page, size, search, sortBy, sortDirection | Auth |
| TrendyolProductController | /products | /store/{storeId}/all | GET | /products/store/{storeId}/all | storeId | Auth |
| TrendyolProductController | /products | /{productId}/cost-and-stock | PUT | /products/{productId}/cost-and-stock | productId, body | Auth |
| TrendyolProductController | /products | /{productId}/stock-info | POST | /products/{productId}/stock-info | productId, body | Auth |
| TrendyolProductController | /products | /{productId}/stock-info/{stockDate} | PUT | /products/{productId}/stock-info/{stockDate} | productId, stockDate | Auth |
| TrendyolProductController | /products | /{productId}/stock-info/{stockDate} | DELETE | /products/{productId}/stock-info/{stockDate} | productId, stockDate | Auth |
| TrendyolProductController | /products | /store/{storeId}/bulk-cost-update | POST | /products/store/{storeId}/bulk-cost-update | storeId, body | Auth |

---

## Orders

| Controller | Base path | Method path | HTTP | Full path | Path/query params | Not |
|------------|-----------|-------------|------|-----------|-------------------|-----|
| TrendyolOrderController | /api/orders | /stores/{storeId}/sync | POST | /api/orders/stores/{storeId}/sync | storeId | Auth |
| TrendyolOrderController | /api/orders | /sync-all | POST | /api/orders/sync-all | - | Auth |
| TrendyolOrderController | /api/orders | /stores/{storeId} | GET | /api/orders/stores/{storeId} | storeId, page, size | Auth |
| TrendyolOrderController | /api/orders | /stores/{storeId}/by-date-range | GET | /api/orders/stores/{storeId}/by-date-range | storeId, startDate, endDate, page, size | Auth |
| TrendyolOrderController | /api/orders | /stores/{storeId}/by-status | GET | /api/orders/stores/{storeId}/by-status | storeId, status, page, size | Auth |
| TrendyolOrderController | /api/orders | /stores/{storeId}/statistics | GET | /api/orders/stores/{storeId}/statistics | storeId | Auth |
| TrendyolOrderController | /api/orders | /stores/{storeId}/recalculate-commissions | POST | /api/orders/stores/{storeId}/recalculate-commissions | storeId | Auth |
| StockOrderSynchronizationController | /api/orders/stock-sync | /synchronize/{storeId} | POST | /api/orders/stock-sync/synchronize/{storeId} | storeId, fromDate (query) | Auth |
| StockOrderSynchronizationController | /api/orders/stock-sync | /status/{storeId} | GET | /api/orders/stock-sync/status/{storeId} | storeId | Auth |

---

## Financial & Invoices

| Controller | Base path | Method path | HTTP | Full path | Path/query params | Not |
|------------|-----------|-------------|------|-----------|-------------------|-----|
| TrendyolFinancialOrderSettlementController | /api/financial | /stores/{storeId}/stats | GET | /api/financial/stores/{storeId}/stats | storeId | Auth |
| TrendyolFinancialOrderSettlementController | /api/financial | /stores/{storeId}/sync | POST | /api/financial/stores/{storeId}/sync | storeId | Auth |
| TrendyolFinancialOrderSettlementController | /api/financial | /stores/{storeId}/sync-historical-cargo | POST | /api/financial/stores/{storeId}/sync-historical-cargo | storeId | Auth |
| TrendyolInvoiceController | /api/invoices | /stores/{storeId}/summary | GET | /api/invoices/stores/{storeId}/summary | storeId | Auth |
| TrendyolInvoiceController | /api/invoices | /stores/{storeId}/by-type/{typeCode} | GET | /api/invoices/stores/{storeId}/by-type/{typeCode} | storeId, typeCode | Auth |
| TrendyolInvoiceController | /api/invoices | /stores/{storeId}/by-category/{category} | GET | /api/invoices/stores/{storeId}/by-category/{category} | storeId, category | Auth |
| TrendyolInvoiceController | /api/invoices | /stores/{storeId} | GET | /api/invoices/stores/{storeId} | storeId | Auth |
| TrendyolInvoiceController | /api/invoices | /stores/{storeId}/cargo-items/{invoiceSerialNumber} | GET | /api/invoices/stores/{storeId}/cargo-items/{invoiceSerialNumber} | storeId, invoiceSerialNumber | Auth |
| TrendyolInvoiceController | /api/invoices | /stores/{storeId}/items/{invoiceSerialNumber} | GET | /api/invoices/stores/{storeId}/items/{invoiceSerialNumber} | storeId, invoiceSerialNumber | Auth |
| TrendyolInvoiceController | /api/invoices | /stores/{storeId}/commission-items/{invoiceSerialNumber} | GET | /api/invoices/stores/{storeId}/commission-items/{invoiceSerialNumber} | storeId, invoiceSerialNumber | Auth |
| TrendyolInvoiceController | /api/invoices | /stores/{storeId}/sync | POST | /api/invoices/stores/{storeId}/sync | storeId | Auth |
| TrendyolInvoiceController | /api/invoices | /stores/{storeId}/category/cargo-items | GET | /api/invoices/stores/{storeId}/category/cargo-items | storeId | Auth |
| TrendyolInvoiceController | /api/invoices | /stores/{storeId}/category/commission-items | GET | /api/invoices/stores/{storeId}/category/commission-items | storeId | Auth |
| TrendyolInvoiceController | /api/invoices | /stores/{storeId}/category/{category}/products | GET | /api/invoices/stores/{storeId}/category/{category}/products | storeId, category | Auth |
| TrendyolInvoiceController | /api/invoices | /stores/{storeId}/products/{barcode}/commission-breakdown | GET | /api/invoices/stores/{storeId}/products/{barcode}/commission-breakdown | storeId, barcode | Auth |
| TrendyolInvoiceController | /api/invoices | /stores/{storeId}/products/{barcode}/cargo-breakdown | GET | /api/invoices/stores/{storeId}/products/{barcode}/cargo-breakdown | storeId, barcode | Auth |

---

## Purchasing & Suppliers

| Controller | Base path | Method path | HTTP | Full path | Path/query params | Not |
|------------|-----------|-------------|------|-----------|-------------------|-----|
| PurchaseOrderController | /api/stores/{storeId}/purchase-orders | | GET | /api/stores/{storeId}/purchase-orders | storeId | Auth |
| PurchaseOrderController | /api/stores/{storeId}/purchase-orders | /stats | GET | /api/stores/{storeId}/purchase-orders/stats | storeId | Auth |
| PurchaseOrderController | /api/stores/{storeId}/purchase-orders | /{poId} | GET | /api/stores/{storeId}/purchase-orders/{poId} | storeId, poId | Auth |
| PurchaseOrderController | /api/stores/{storeId}/purchase-orders | | POST | /api/stores/{storeId}/purchase-orders | storeId, body | Auth |
| PurchaseOrderController | /api/stores/{storeId}/purchase-orders | /{poId} | PUT | /api/stores/{storeId}/purchase-orders/{poId} | storeId, poId | Auth |
| PurchaseOrderController | /api/stores/{storeId}/purchase-orders | /{poId} | DELETE | /api/stores/{storeId}/purchase-orders/{poId} | storeId, poId | Auth |
| PurchaseOrderController | /api/stores/{storeId}/purchase-orders | /{poId}/status | PUT | /api/stores/{storeId}/purchase-orders/{poId}/status | storeId, poId | Auth |
| PurchaseOrderController | /api/stores/{storeId}/purchase-orders | /{poId}/items | POST | /api/stores/{storeId}/purchase-orders/{poId}/items | storeId, poId | Auth |
| PurchaseOrderController | /api/stores/{storeId}/purchase-orders | /{poId}/items/{itemId} | PUT | /api/stores/{storeId}/purchase-orders/{poId}/items/{itemId} | storeId, poId, itemId | Auth |
| PurchaseOrderController | /api/stores/{storeId}/purchase-orders | /{poId}/items/{itemId} | DELETE | /api/stores/{storeId}/purchase-orders/{poId}/items/{itemId} | storeId, poId, itemId | Auth |
| PurchaseOrderController | /api/stores/{storeId}/purchase-orders | /{poId}/duplicate | POST | /api/stores/{storeId}/purchase-orders/{poId}/duplicate | storeId, poId | Auth |
| PurchaseOrderController | /api/stores/{storeId}/purchase-orders | /{poId}/split | POST | /api/stores/{storeId}/purchase-orders/{poId}/split | storeId, poId | Auth |
| PurchaseOrderController | /api/stores/{storeId}/purchase-orders | /{poId}/attachments | GET | /api/stores/{storeId}/purchase-orders/{poId}/attachments | storeId, poId | Auth |
| PurchaseOrderController | /api/stores/{storeId}/purchase-orders | /{poId}/attachments | POST | /api/stores/{storeId}/purchase-orders/{poId}/attachments | storeId, poId, multipart | Auth |
| PurchaseOrderController | /api/stores/{storeId}/purchase-orders | /{poId}/attachments/{attachmentId}/download | GET | /api/stores/{storeId}/purchase-orders/{poId}/attachments/{attachmentId}/download | storeId, poId, attachmentId | Auth |
| PurchaseOrderController | /api/stores/{storeId}/purchase-orders | /{poId}/attachments/{attachmentId} | DELETE | /api/stores/{storeId}/purchase-orders/{poId}/attachments/{attachmentId} | storeId, poId, attachmentId | Auth |
| PurchaseOrderController | /api/stores/{storeId}/purchase-orders | /{poId}/export | GET | /api/stores/{storeId}/purchase-orders/{poId}/export | storeId, poId | Auth |
| PurchaseOrderController | /api/stores/{storeId}/purchase-orders | /{poId}/import | POST | /api/stores/{storeId}/purchase-orders/{poId}/import | storeId, poId, multipart | Auth |
| PurchaseOrderController | /api/stores/{storeId}/purchase-orders | /reports/product/{productId}/cost-history | GET | /api/stores/{storeId}/purchase-orders/reports/product/{productId}/cost-history | storeId, productId | Auth |
| PurchaseOrderController | /api/stores/{storeId}/purchase-orders | /reports/fifo-analysis | GET | /api/stores/{storeId}/purchase-orders/reports/fifo-analysis | storeId | Auth |
| PurchaseOrderController | /api/stores/{storeId}/purchase-orders | /reports/stock-valuation | GET | /api/stores/{storeId}/purchase-orders/reports/stock-valuation | storeId | Auth |
| PurchaseOrderController | /api/stores/{storeId}/purchase-orders | /reports/profitability | GET | /api/stores/{storeId}/purchase-orders/reports/profitability | storeId | Auth |
| PurchaseOrderController | /api/stores/{storeId}/purchase-orders | /reports/summary | GET | /api/stores/{storeId}/purchase-orders/reports/summary | storeId | Auth |
| PurchaseOrderController | /api/stores/{storeId}/purchase-orders | /reports/stock-depletion | GET | /api/stores/{storeId}/purchase-orders/reports/stock-depletion | storeId | Auth |
| SupplierController | /api/stores/{storeId}/suppliers | | GET | /api/stores/{storeId}/suppliers | storeId | Auth |
| SupplierController | /api/stores/{storeId}/suppliers | /{supplierId} | GET | /api/stores/{storeId}/suppliers/{supplierId} | storeId, supplierId | Auth |
| SupplierController | /api/stores/{storeId}/suppliers | | POST | /api/stores/{storeId}/suppliers | storeId | Auth |
| SupplierController | /api/stores/{storeId}/suppliers | /{supplierId} | PUT | /api/stores/{storeId}/suppliers/{supplierId} | storeId, supplierId | Auth |
| SupplierController | /api/stores/{storeId}/suppliers | /{supplierId} | DELETE | /api/stores/{storeId}/suppliers/{supplierId} | storeId, supplierId | Auth |

---

## Categories

| Controller | Base path | Method path | HTTP | Full path | Path/query params | Not |
|------------|-----------|-------------|------|-----------|-------------------|-----|
| TrendyolCategoryController | /api/categories | | GET | /api/categories | - | Auth |
| TrendyolCategoryController | /api/categories | /bulk-insert | POST | /api/categories/bulk-insert | body | Auth |

---

## Expenses

| Controller | Base path | Method path | HTTP | Full path | Path/query params | Not |
|------------|-----------|-------------|------|-----------|-------------------|-----|
| StoreExpenseController | /expenses | /categories | GET | /expenses/categories | - | Auth |
| StoreExpenseController | /expenses | /store/{storeId} | GET | /expenses/store/{storeId} | storeId | Auth |
| StoreExpenseController | /expenses | /store/{storeId} | POST | /expenses/store/{storeId} | storeId, body | Auth |
| StoreExpenseController | /expenses | /store/{storeId}/{expenseId} | PUT | /expenses/store/{storeId}/{expenseId} | storeId, expenseId | Auth |
| StoreExpenseController | /expenses | /store/{storeId}/{expenseId} | DELETE | /expenses/store/{storeId}/{expenseId} | storeId, expenseId | Auth |

---

## Webhooks

| Controller | Base path | Method path | HTTP | Full path | Path/query params | Not |
|------------|-----------|-------------|------|-----------|-------------------|-----|
| WebhookManagementController | /api/stores/{storeId}/webhooks | /status | GET | /api/stores/{storeId}/webhooks/status | storeId | Auth |
| WebhookManagementController | /api/stores/{storeId}/webhooks | /enable | POST | /api/stores/{storeId}/webhooks/enable | storeId | Auth |
| WebhookManagementController | /api/stores/{storeId}/webhooks | /disable | POST | /api/stores/{storeId}/webhooks/disable | storeId | Auth |
| WebhookManagementController | /api/stores/{storeId}/webhooks | /events | GET | /api/stores/{storeId}/webhooks/events | storeId | Auth |
| WebhookManagementController | /api/stores/{storeId}/webhooks | /test | POST | /api/stores/{storeId}/webhooks/test | storeId | Auth |
| TrendyolWebhookController | /api/webhook | /trendyol/{sellerId} | POST | /api/webhook/trendyol/{sellerId} | sellerId, body | Public |
| TrendyolWebhookController | /api/webhook | /health | GET | /api/webhook/health | - | Public |
| IyzicoWebhookController | /api/webhook/iyzico | /payment | POST | /api/webhook/iyzico/payment | body | Public |
| IyzicoWebhookController | /api/webhook/iyzico | /threeds | POST | /api/webhook/iyzico/threeds | body | Public |
| IyzicoWebhookController | /api/webhook/iyzico | /refund | POST | /api/webhook/iyzico/refund | body | Public |
| IyzicoWebhookController | /api/webhook/iyzico | /card | POST | /api/webhook/iyzico/card | body | Public |
| IyzicoWebhookController | /api/webhook/iyzico | /health | GET | /api/webhook/iyzico/health | - | Public |

---

## Alerts

| Controller | Base path | Method path | HTTP | Full path | Path/query params | Not |
|------------|-----------|-------------|------|-----------|-------------------|-----|
| AlertRuleController | /api/alert-rules | | GET | /api/alert-rules | - | Auth |
| AlertRuleController | /api/alert-rules | /paginated | GET | /api/alert-rules/paginated | - | Auth |
| AlertRuleController | /api/alert-rules | /{id} | GET | /api/alert-rules/{id} | id | Auth |
| AlertRuleController | /api/alert-rules | | POST | /api/alert-rules | body | Auth |
| AlertRuleController | /api/alert-rules | /{id} | PUT | /api/alert-rules/{id} | id | Auth |
| AlertRuleController | /api/alert-rules | /{id}/toggle | PUT | /api/alert-rules/{id}/toggle | id | Auth |
| AlertRuleController | /api/alert-rules | /{id} | DELETE | /api/alert-rules/{id} | id | Auth |
| AlertRuleController | /api/alert-rules | /count | GET | /api/alert-rules/count | - | Auth |
| AlertHistoryController | /api/alerts | | GET | /api/alerts | - | Auth |
| AlertHistoryController | /api/alerts | /unread | GET | /api/alerts/unread | - | Auth |
| AlertHistoryController | /api/alerts | /unread-count | GET | /api/alerts/unread-count | - | Auth |
| AlertHistoryController | /api/alerts | /recent | GET | /api/alerts/recent | - | Auth |
| AlertHistoryController | /api/alerts | /by-date-range | GET | /api/alerts/by-date-range | - | Auth |
| AlertHistoryController | /api/alerts | /{id} | GET | /api/alerts/{id} | id | Auth |
| AlertHistoryController | /api/alerts | /{id}/read | PUT | /api/alerts/{id}/read | id | Auth |
| AlertHistoryController | /api/alerts | /read-all | PUT | /api/alerts/read-all | - | Auth |
| AlertHistoryController | /api/alerts | /stats | GET | /api/alerts/stats | - | Auth |

---

## Stock Tracking

| Controller | Base path | Method path | HTTP | Full path | Path/query params | Not |
|------------|-----------|-------------|------|-----------|-------------------|-----|
| StockTrackingController | /api/stock-tracking | /preview | GET | /api/stock-tracking/preview | - | Auth |
| StockTrackingController | /api/stock-tracking | /stores/{storeId}/dashboard | GET | /api/stock-tracking/stores/{storeId}/dashboard | storeId | Auth |
| StockTrackingController | /api/stock-tracking | /stores/{storeId}/products | GET | /api/stock-tracking/stores/{storeId}/products | storeId | Auth |
| StockTrackingController | /api/stock-tracking | /stores/{storeId}/products | POST | /api/stock-tracking/stores/{storeId}/products | storeId | Auth |
| StockTrackingController | /api/stock-tracking | /products/{trackedProductId} | GET | /api/stock-tracking/products/{trackedProductId} | trackedProductId | Auth |
| StockTrackingController | /api/stock-tracking | /products/{trackedProductId}/settings | PUT | /api/stock-tracking/products/{trackedProductId}/settings | trackedProductId | Auth |
| StockTrackingController | /api/stock-tracking | /products/{trackedProductId}/check | POST | /api/stock-tracking/products/{trackedProductId}/check | trackedProductId | Auth |
| StockTrackingController | /api/stock-tracking | /products/{trackedProductId} | DELETE | /api/stock-tracking/products/{trackedProductId} | trackedProductId | Auth |
| StockTrackingController | /api/stock-tracking | /stores/{storeId}/alerts | GET | /api/stock-tracking/stores/{storeId}/alerts | storeId | Auth |
| StockTrackingController | /api/stock-tracking | /alerts/{alertId}/read | PUT | /api/stock-tracking/alerts/{alertId}/read | alertId | Auth |
| StockTrackingController | /api/stock-tracking | /stores/{storeId}/alerts/mark-all-read | PUT | /api/stock-tracking/stores/{storeId}/alerts/mark-all-read | storeId | Auth |

---

## Returns & Claims

| Controller | Base path | Method path | HTTP | Full path | Path/query params | Not |
|------------|-----------|-------------|------|-----------|-------------------|-----|
| ReturnAnalyticsController | /api/returns | /stores/{storeId}/analytics | GET | /api/returns/stores/{storeId}/analytics | storeId | Auth |
| ReturnAnalyticsController | /api/returns | /stores/{storeId}/analytics/current-month | GET | /api/returns/stores/{storeId}/analytics/current-month | storeId | Auth |
| ReturnAnalyticsController | /api/returns | /stores/{storeId}/analytics/last-30-days | GET | /api/returns/stores/{storeId}/analytics/last-30-days | storeId | Auth |
| ClaimsController | /api/returns | /stores/{storeId}/claims | GET | /api/returns/stores/{storeId}/claims | storeId | Auth |
| ClaimsController | /api/returns | /stores/{storeId}/claims/{claimId} | GET | /api/returns/stores/{storeId}/claims/{claimId} | storeId, claimId | Auth |
| ClaimsController | /api/returns | /stores/{storeId}/claims/sync | POST | /api/returns/stores/{storeId}/claims/sync | storeId | Auth |
| ClaimsController | /api/returns | /stores/{storeId}/claims/{claimId}/approve | PUT | /api/returns/stores/{storeId}/claims/{claimId}/approve | storeId, claimId | Auth |
| ClaimsController | /api/returns | /stores/{storeId}/claims/{claimId}/reject | POST | /api/returns/stores/{storeId}/claims/{claimId}/reject | storeId, claimId | Auth |
| ClaimsController | /api/returns | /stores/{storeId}/claims/bulk-approve | POST | /api/returns/stores/{storeId}/claims/bulk-approve | storeId | Auth |
| ClaimsController | /api/returns | /claim-issue-reasons | GET | /api/returns/claim-issue-reasons | - | Auth |
| ClaimsController | /api/returns | /stores/{storeId}/stats | GET | /api/returns/stores/{storeId}/stats | storeId | Auth |
| ClaimsController | /api/returns | /stores/{storeId}/test-date-range | POST | /api/returns/stores/{storeId}/test-date-range | storeId | Auth |
| ClaimsController | /api/returns | /stores/{storeId}/test-date-ranges | POST | /api/returns/stores/{storeId}/test-date-ranges | storeId | Auth |

---

## QA

| Controller | Base path | Method path | HTTP | Full path | Path/query params | Not |
|------------|-----------|-------------|------|-----------|-------------------|-----|
| QaController | /qa | /stores/{storeId}/questions | GET | /qa/stores/{storeId}/questions | storeId | Auth |
| QaController | /qa | /questions/{questionId} | GET | /qa/questions/{questionId} | questionId | Auth |
| QaController | /qa | /stores/{storeId}/questions/sync | POST | /qa/stores/{storeId}/questions/sync | storeId | Auth |
| QaController | /qa | /stores/{storeId}/questions/{questionId}/answer | POST | /qa/stores/{storeId}/questions/{questionId}/answer | storeId, questionId | Auth |
| QaController | /qa | /stores/{storeId}/stats | GET | /qa/stores/{storeId}/stats | storeId | Auth |
| QaController | /qa | /questions/{questionId}/ai-generate | POST | /qa/questions/{questionId}/ai-generate | questionId | Auth |
| QaController | /qa | /questions/{questionId}/ai-approve | POST | /qa/questions/{questionId}/ai-approve | questionId | Auth |
| QaController | /qa | /stores/{storeId}/suggestions | GET | /qa/stores/{storeId}/suggestions | storeId | Auth |
| QaController | /qa | /stores/{storeId}/suggestions/count | GET | /qa/stores/{storeId}/suggestions/count | storeId | Auth |
| QaController | /qa | /suggestions/{suggestionId}/approve | POST | /qa/suggestions/{suggestionId}/approve | suggestionId | Auth |
| QaController | /qa | /suggestions/{suggestionId}/reject | POST | /qa/suggestions/{suggestionId}/reject | suggestionId | Auth |
| QaController | /qa | /suggestions/{suggestionId}/modify | POST | /qa/suggestions/{suggestionId}/modify | suggestionId | Auth |
| QaController | /qa | /stores/{storeId}/patterns | GET | /qa/stores/{storeId}/patterns | storeId | Auth |
| QaController | /qa | /stores/{storeId}/patterns/stats | GET | /qa/stores/{storeId}/patterns/stats | storeId | Auth |
| QaController | /qa | /patterns/{patternId}/promote | POST | /qa/patterns/{patternId}/promote | patternId | Auth |
| QaController | /qa | /patterns/{patternId}/demote | POST | /qa/patterns/{patternId}/demote | patternId | Auth |
| QaController | /qa | /patterns/{patternId}/enable-auto-submit | POST | /qa/patterns/{patternId}/enable-auto-submit | patternId | Auth |
| QaController | /qa | /patterns/{patternId}/disable-auto-submit | POST | /qa/patterns/{patternId}/disable-auto-submit | patternId | Auth |
| QaController | /qa | /stores/{storeId}/conflicts | GET | /qa/stores/{storeId}/conflicts | storeId | Auth |
| QaController | /qa | /stores/{storeId}/conflicts/count | GET | /qa/stores/{storeId}/conflicts/count | storeId | Auth |
| QaController | /qa | /stores/{storeId}/conflicts/stats | GET | /qa/stores/{storeId}/conflicts/stats | storeId | Auth |
| QaController | /qa | /conflicts/{conflictId}/resolve | POST | /qa/conflicts/{conflictId}/resolve | conflictId | Auth |
| QaController | /qa | /conflicts/{conflictId}/dismiss | POST | /qa/conflicts/{conflictId}/dismiss | conflictId | Auth |

---

## Referrals

| Controller | Base path | Method path | HTTP | Full path | Path/query params | Not |
|------------|-----------|-------------|------|-----------|-------------------|-----|
| ReferralController | /api/referrals | /stats | GET | /api/referrals/stats | - | Auth |
| ReferralController | /api/referrals | /code | POST | /api/referrals/code | - | Auth |
| ReferralController | /api/referrals | /validate/{code} | GET | /api/referrals/validate/{code} | code | Public |

---

## Support

| Controller | Base path | Method path | HTTP | Full path | Path/query params | Not |
|------------|-----------|-------------|------|-----------|-------------------|-----|
| SupportTicketController | /api/support/tickets | | POST | /api/support/tickets | body | Auth |
| SupportTicketController | /api/support/tickets | | GET | /api/support/tickets | - | Auth |
| SupportTicketController | /api/support/tickets | /{id} | GET | /api/support/tickets/{id} | id | Auth |
| SupportTicketController | /api/support/tickets | /{id}/messages | POST | /api/support/tickets/{id}/messages | id | Auth |
| AdminSupportController | /api/admin/support/tickets | | GET | /api/admin/support/tickets | - | Admin |
| AdminSupportController | /api/admin/support/tickets | /active | GET | /api/admin/support/tickets/active | - | Admin |
| AdminSupportController | /api/admin/support/tickets | /{id} | GET | /api/admin/support/tickets/{id} | id | Admin |
| AdminSupportController | /api/admin/support/tickets | /{id}/reply | POST | /api/admin/support/tickets/{id}/reply | id | Admin |
| AdminSupportController | /api/admin/support/tickets | /{id}/status | PUT | /api/admin/support/tickets/{id}/status | id | Admin |
| AdminSupportController | /api/admin/support/tickets | /{id}/assign | PUT | /api/admin/support/tickets/{id}/assign | id | Admin |
| AdminSupportController | /api/admin/support/tickets | /search | GET | /api/admin/support/tickets/search | - | Admin |
| AdminSupportController | /api/admin/support/tickets | /stats | GET | /api/admin/support/tickets/stats | - | Admin |

---

## Billing

| Controller | Base path | Method path | HTTP | Full path | Path/query params | Not |
|------------|-----------|-------------|------|-----------|-------------------|-----|
| BillingController | /api/billing | /plans | GET | /api/billing/plans | - | Public |
| BillingController | /api/billing | /plans/{code} | GET | /api/billing/plans/{code} | code | Public |
| FeatureController | /api/billing/features | | GET | /api/billing/features | - | Auth |
| FeatureController | /api/billing/features | /{code} | GET | /api/billing/features/{code} | code | Auth |
| FeatureController | /api/billing/features | /{code}/use | POST | /api/billing/features/{code}/use | code | Auth |
| CheckoutController | /api/billing/checkout | /start | POST | /api/billing/checkout/start | body | Auth |
| CheckoutController | /api/billing/checkout | /complete-3ds | POST | /api/billing/checkout/complete-3ds | body | Auth |
| PaymentMethodController | /api/billing/payment-methods | | GET | /api/billing/payment-methods | - | Auth |
| PaymentMethodController | /api/billing/payment-methods | | POST | /api/billing/payment-methods | body | Auth |
| PaymentMethodController | /api/billing/payment-methods | /{id} | DELETE | /api/billing/payment-methods/{id} | id | Auth |
| PaymentMethodController | /api/billing/payment-methods | /{id}/default | POST | /api/billing/payment-methods/{id}/default | id | Auth |
| SubscriptionController | /api/billing/subscription | | GET | /api/billing/subscription | - | Auth |
| SubscriptionController | /api/billing/subscription | /trial | POST | /api/billing/subscription/trial | body | Auth |
| SubscriptionController | /api/billing/subscription | /activate | POST | /api/billing/subscription/activate | body | Auth |
| SubscriptionController | /api/billing/subscription | /plan | POST | /api/billing/subscription/plan | body | Auth |
| SubscriptionController | /api/billing/subscription | /cancel | POST | /api/billing/subscription/cancel | body | Auth |
| SubscriptionController | /api/billing/subscription | /reactivate | POST | /api/billing/subscription/reactivate | body | Auth |
| InvoiceController | /api/billing/invoices | | GET | /api/billing/invoices | - | Auth |
| InvoiceController | /api/billing/invoices | /{id} | GET | /api/billing/invoices/{id} | id | Auth |
| InvoiceController | /api/billing/invoices | /{id}/pdf | GET | /api/billing/invoices/{id}/pdf | id | Auth |

---

## Education

| Controller | Base path | Method path | HTTP | Full path | Path/query params | Not |
|------------|-----------|-------------|------|-----------|-------------------|-----|
| EducationVideoController | /api/education/videos | | GET | /api/education/videos | - | Public |
| EducationVideoController | /api/education/videos | /{id} | GET | /api/education/videos/{id} | id: UUID | Public |
| EducationVideoController | /api/education/videos | /category/{category} | GET | /api/education/videos/category/{category} | category | Public |
| EducationVideoController | /api/education/videos | /my-watch-status | GET | /api/education/videos/my-watch-status | - | Auth |
| EducationVideoController | /api/education/videos | /{id}/watch | POST | /api/education/videos/{id}/watch | id: UUID | Auth |
| EducationVideoController | /api/education/videos | | POST | /api/education/videos | body | Admin |
| EducationVideoController | /api/education/videos | /{id} | PUT | /api/education/videos/{id} | id: UUID | Admin |
| EducationVideoController | /api/education/videos | /{id} | DELETE | /api/education/videos/{id} | id: UUID | Admin |

---

## Notifications

| Controller | Base path | Method path | HTTP | Full path | Path/query params | Not |
|------------|-----------|-------------|------|-----------|-------------------|-----|
| NotificationController | /api/notifications | | GET | /api/notifications | - | Auth |
| NotificationController | /api/notifications | /unread-count | GET | /api/notifications/unread-count | - | Auth |
| NotificationController | /api/notifications | /{id}/read | PUT | /api/notifications/{id}/read | id | Auth |

---

## AI & Knowledge

| Controller | Base path | Method path | HTTP | Full path | Path/query params | Not |
|------------|-----------|-------------|------|-----------|-------------------|-----|
| AiSettingsController | /api/ai-settings | /stores/{storeId} | GET | /api/ai-settings/stores/{storeId} | storeId | Auth |
| AiSettingsController | /api/ai-settings | /stores/{storeId} | PUT | /api/ai-settings/stores/{storeId} | storeId | Auth |
| KnowledgeBaseController | /api/knowledge | /stores/{storeId} | GET | /api/knowledge/stores/{storeId} | storeId | Auth |
| KnowledgeBaseController | /api/knowledge | /stores/{storeId}/category/{category} | GET | /api/knowledge/stores/{storeId}/category/{category} | storeId, category | Auth |
| KnowledgeBaseController | /api/knowledge | /stores/{storeId} | POST | /api/knowledge/stores/{storeId} | storeId | Auth |
| KnowledgeBaseController | /api/knowledge | /{id} | PUT | /api/knowledge/{id} | id | Auth |
| KnowledgeBaseController | /api/knowledge | /{id} | DELETE | /api/knowledge/{id} | id | Auth |
| KnowledgeBaseController | /api/knowledge | /{id}/toggle | PATCH | /api/knowledge/{id}/toggle | id | Auth |

---

## Currency

| Controller | Base path | Method path | HTTP | Full path | Path/query params | Not |
|------------|-----------|-------------|------|-----------|-------------------|-----|
| CurrencyController | /currency | /rates | GET | /currency/rates | - | Auth |
| CurrencyController | /currency | /rates/{from}/{to} | GET | /currency/rates/{from}/{to} | from, to | Auth |
| CurrencyController | /currency | /convert | GET | /currency/convert | - | Auth |
| CurrencyController | /currency | /rates/refresh | POST | /currency/rates/refresh | - | Auth |

---

## Trendyol & Health

| Controller | Base path | Method path | HTTP | Full path | Path/query params | Not |
|------------|-----------|-------------|------|-----------|-------------------|-----|
| TrendyolController | /trendyol | /test-credentials | POST | /trendyol/test-credentials | body | Auth |
| HealthController | (none) | /health | GET | /health | - | Public |
| HealthController | (none) | / | GET | / | - | Public |

---

## Admin

| Controller | Base path | Method path | HTTP | Full path | Path/query params | Not |
|------------|-----------|-------------|------|-----------|-------------------|-----|
| AdminStoreController | /api/admin/stores | | GET | /api/admin/stores | - | Admin |
| AdminStoreController | /api/admin/stores | /{id} | GET | /api/admin/stores/{id} | id | Admin |
| AdminStoreController | /api/admin/stores | /{id}/sync | POST | /api/admin/stores/{id}/sync | id | Admin |
| AdminStoreController | /api/admin/stores | /search | GET | /api/admin/stores/search | q (query) | Admin |
| AdminUserController | /api/admin/users | | GET | /api/admin/users | - | Admin |
| AdminUserController | /api/admin/users | /{id} | GET | /api/admin/users/{id} | id | Admin |
| AdminUserController | /api/admin/users | /{id}/role | PUT | /api/admin/users/{id}/role | id | Admin |
| AdminUserController | /api/admin/users | /search | GET | /api/admin/users/search | q (query) | Admin |
| AdminUserController | /api/admin/users | /export | GET | /api/admin/users/export | - | Admin |
| AdminBillingController | /api/admin/billing | /subscriptions | GET | /api/admin/billing/subscriptions | - | Admin |
| AdminBillingController | /api/admin/billing | /subscriptions/{id} | GET | /api/admin/billing/subscriptions/{id} | id | Admin |
| AdminBillingController | /api/admin/billing | /revenue/stats | GET | /api/admin/billing/revenue/stats | - | Admin |
| AdminBillingController | /api/admin/billing | /revenue/history | GET | /api/admin/billing/revenue/history | - | Admin |
| AdminBillingController | /api/admin/billing | /payments | GET | /api/admin/billing/payments | - | Admin |
| AdminActivityLogController | /api/admin/activity-logs | | GET | /api/admin/activity-logs | - | Admin |
| AdminActivityLogController | /api/admin/activity-logs | /security/summary | GET | /api/admin/activity-logs/security/summary | - | Admin |
| AdminOrderController | /api/admin/orders | /stats | GET | /api/admin/orders/stats | - | Admin |
| AdminOrderController | /api/admin/orders | /recent | GET | /api/admin/orders/recent | - | Admin |
| AdminProductController | /api/admin/products | /stats | GET | /api/admin/products/stats | - | Admin |
| AdminProductController | /api/admin/products | /top | GET | /api/admin/products/top | - | Admin |
| AdminReferralController | /api/admin/referrals | | GET | /api/admin/referrals | - | Admin |
| AdminReferralController | /api/admin/referrals | /stats | GET | /api/admin/referrals/stats | - | Admin |
| AdminDashboardController | /api/admin/dashboard | /stats | GET | /api/admin/dashboard/stats | - | Admin |
| AdminNotificationController | /api/admin/notifications | /stats | GET | /api/admin/notifications/stats | - | Admin |
| AdminNotificationController | /api/admin/notifications | /broadcast | POST | /api/admin/notifications/broadcast | body | Admin |

---

## Test

| Controller | Base path | Method path | HTTP | Full path | Path/query params | Not |
|------------|-----------|-------------|------|-----------|-------------------|-----|
| TrendyolApiLimitTestController | /api/test/trendyol-limits | /orders/{storeId} | GET | /api/test/trendyol-limits/orders/{storeId} | storeId | Public |
| TrendyolApiLimitTestController | /api/test/trendyol-limits | /financials/{storeId} | GET | /api/test/trendyol-limits/financials/{storeId} | storeId | Public |
| TrendyolApiLimitTestController | /api/test/trendyol-limits | /sync-all-financials/{storeId} | POST | /api/test/trendyol-limits/sync-all-financials/{storeId} | storeId | Public |
| TrendyolApiLimitTestController | /api/test/trendyol-limits | /claims/{storeId} | GET | /api/test/trendyol-limits/claims/{storeId} | storeId | Public |
| TrendyolApiLimitTestController | /api/test/trendyol-limits | /settlements/{storeId} | GET | /api/test/trendyol-limits/settlements/{storeId} | storeId | Public |
| TrendyolApiLimitTestController | /api/test/trendyol-limits | /deduction-invoices/{storeId} | GET | /api/test/trendyol-limits/deduction-invoices/{storeId} | storeId | Public |
| TrendyolApiLimitTestController | /api/test/trendyol-limits | /questions/{storeId} | GET | /api/test/trendyol-limits/questions/{storeId} | storeId | Public |

---

**Hariç tutulanlar:** HomeController (view), GlobalExceptionHandler, StoreExceptionHandler, TrendyolOrderExceptionHandler, TrendyolProductExceptionHandler, billing-disabled/ altındaki kod.
