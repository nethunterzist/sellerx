# Entity – Tablo Eşlemesi (Mevcut Kod)

JPA Entity sınıfları ve karşılık gelen tablolar. `@Table(name = "...")` veya JPA default (sınıf adı → snake_case). Ana kolonlar: id, FK, JSONB.

## Users

| Entity | Tablo | Ana kolonlar / not |
|--------|-------|---------------------|
| User | users | id (BIGSERIAL), email, password, role, selected_store_id, referral_code, referred_by_user_id, preferences (JSONB), created_at, last_login_at |

## Stores

| Entity | Tablo | Ana kolonlar / not |
|--------|-------|---------------------|
| Store | stores | id (UUID), user_id (FK), store_name, marketplace, credentials (JSONB), webhook_id, webhook_status, sync_status, initial_sync_completed, historical_sync_*, sync_phases (JSONB), overall_sync_status, created_at, updated_at |

## Products & Categories

| Entity | Tablo | Ana kolonlar / not |
|--------|-------|---------------------|
| TrendyolProduct | trendyol_products | id (UUID), store_id (FK), product_id, barcode, title, category_name, sale_price, commission_rate, last_commission_rate, cost_and_stock_info (JSONB), on_sale, created_at, updated_at |
| TrendyolCategory | trendyol_categories | (id, category bilgileri) |

## Orders

| Entity | Tablo | Ana kolonlar / not |
|--------|-------|---------------------|
| TrendyolOrder | trendyol_orders | id (UUID), store_id (FK), ty_order_number, package_no, order_date, gross_amount, order_items (JSONB), financial_transactions (JSONB), total_price, estimated_commission, is_commission_estimated, data_source, created_at, updated_at |

## Financial & Invoices

| Entity | Tablo | Ana kolonlar / not |
|--------|-------|---------------------|
| TrendyolInvoice | trendyol_invoices | id, store_id (FK), invoice_number, invoice_type, invoice_category, invoice_date, amount, details (JSONB), created_at, updated_at |
| TrendyolDeductionInvoice | trendyol_deduction_invoices | id, store_id (FK), trendyol_id, transaction_date, transaction_type, debt, credit, created_at |
| TrendyolCargoInvoice | trendyol_cargo_invoices | (kargo faturaları) |
| TrendyolPaymentOrder | trendyol_payment_orders | (ödeme emirleri) |
| TrendyolStoppage | trendyol_stoppages | (kesintiler) |
| CommissionReconciliationLog | commission_reconciliation_log | (komisyon mutabakat log) |
| HistoricalSyncFailedChunk | historical_sync_failed_chunks | (sync başarısız chunk) |

## Purchasing

| Entity | Tablo | Ana kolonlar / not |
|--------|-------|---------------------|
| PurchaseOrder | purchase_orders | id, store_id (FK), supplier_id (FK), po_number, po_date, status, total_cost, total_units, created_at, updated_at |
| PurchaseOrderItem | purchase_order_items | id, purchase_order_id (FK), product_id (FK), units_ordered, manufacturing_cost_per_unit, total_cost_per_unit, stock_entry_date, cost_vat_rate, created_at |
| PurchaseOrderAttachment | po_attachments | id, purchase_order_id (FK), file_name, file_data (LOB), uploaded_at |
| Supplier | suppliers | id, store_id (FK), name, contact_person, email, phone, currency, created_at, updated_at |

## Expenses

| Entity | Tablo | Ana kolonlar / not |
|--------|-------|---------------------|
| ExpenseCategory | expense_categories | (kategori tanımları) |
| StoreExpense | store_expenses | id, store_id (FK), expense_category_id (FK), date, frequency, name, amount, vat_rate, vat_amount, net_amount, created_at, updated_at |

## Webhook

| Entity | Tablo | Ana kolonlar / not |
|--------|-------|---------------------|
| WebhookEvent | webhook_events | id, event_id (unique), seller_id, payload (JSONB veya TEXT), created_at |

## Alerts

| Entity | Tablo | Ana kolonlar / not |
|--------|-------|---------------------|
| AlertRule | alert_rules | id, user_id (FK), store_id (FK), rule tipi, koşul, enabled, created_at, updated_at |
| AlertHistory | alert_history | id, alert_rule_id (FK), store_id (FK), product_id (FK), severity, message, read, created_at |

## Stock Tracking

| Entity | Tablo | Ana kolonlar / not |
|--------|-------|---------------------|
| StockTrackedProduct | stock_tracked_products | id, store_id (FK), product_id (FK), barcode, created_at |
| StockSnapshot | stock_snapshots | id, tracked_product_id (FK), snapshot zamanı, quantity |
| StockAlert | stock_alerts | id, tracked_product_id (FK), alert_type, severity, is_read, created_at |

## Returns & Claims

| Entity | Tablo | Ana kolonlar / not |
|--------|-------|---------------------|
| ReturnRecord | return_records | id, store_id (FK), sipariş/iade bilgileri |
| TrendyolClaim | trendyol_claims | id, store_id (FK), claim_id, status, created_at, updated_at |

## QA

| Entity | Tablo | Ana kolonlar / not |
|--------|-------|---------------------|
| TrendyolQuestion | trendyol_questions | id, store_id (FK), question_id, metin, created_at |
| TrendyolAnswer | trendyol_answers | id, question_id (FK), cevap metni |
| KnowledgeSuggestion | knowledge_suggestions | id, store_id (FK), öneri içeriği |
| QaPattern | qa_patterns | id, store_id (FK), pattern, enabled |
| ConflictAlert | conflict_alerts | id, store_id (FK), conflict bilgisi |

## Billing

| Entity | Tablo | Ana kolonlar / not |
|--------|-------|---------------------|
| SubscriptionPlan | subscription_plans | id, code, name, fiyat bilgileri |
| SubscriptionPrice | subscription_prices | id, plan_id (FK), fiyat, dönem |
| Subscription | subscriptions | id, user_id (FK), plan_id (FK), status, current_period_start/end |
| SubscriptionEvent | subscription_events | id, subscription_id (FK), event_type, created_at |
| PaymentMethod | payment_methods | id, user_id (FK), type, token/ref |
| PaymentTransaction | payment_transactions | id, payment bilgileri |
| PaymentWebhookEvent | payment_webhook_events | id, event_id, payload |
| Invoice | invoices | id, user_id (FK), subscription_id (FK), amount, status |
| EInvoice | e_invoices | id, e-fatura bilgileri |
| PlanFeature | plan_features | id, plan_id (FK), feature_code |
| FeatureUsage | feature_usage | id, user_id (FK), feature_code, kullanım |
| BillingAddress | billing_addresses | id, user_id (FK), adres bilgileri |

## Education

| Entity | Tablo | Ana kolonlar / not |
|--------|-------|---------------------|
| EducationVideo | education_videos | id, title, description, category, duration, video_url, video_order, is_active, created_at, updated_at |
| VideoWatchHistory | video_watch_history | id, user_id (FK), video_id (FK), watched_at, watched_duration |

## Notifications

| Entity | Tablo | Ana kolonlar / not |
|--------|-------|---------------------|
| UserNotification | user_notifications | id, user_id (FK), type, title, message, read, created_at |

## Referral

| Entity | Tablo | Ana kolonlar / not |
|--------|-------|---------------------|
| Referral | referrals | id, referrer_user_id (FK), referral_code, referred_user_id (FK), status, reward_days_granted, created_at, updated_at |

## Support

| Entity | Tablo | Ana kolonlar / not |
|--------|-------|---------------------|
| SupportTicket | support_tickets | id, user_id (FK), subject, status, created_at, updated_at |
| TicketMessage | ticket_messages | id, ticket_id (FK), sender, message, created_at |

## Activity Log

| Entity | Tablo | Ana kolonlar / not |
|--------|-------|---------------------|
| ActivityLog | activity_logs | id, user_id (FK), action, entity_type, entity_id, details (JSONB), ip_address, created_at |

## Admin Audit

| Entity | Tablo | Ana kolonlar / not |
|--------|-------|---------------------|
| ImpersonationLog | impersonation_logs | id (BIGSERIAL), admin_user_id, target_user_id, action, ip_address, user_agent, created_at |

## Currency

| Entity | Tablo | Ana kolonlar / not |
|--------|-------|---------------------|
| ExchangeRate | exchange_rates | id, base_currency, target_currency, rate, date |

## Email System

| Entity | Tablo | Ana kolonlar / not |
|--------|-------|---------------------|
| EmailQueue | email_queue | id (UUID), email_type, recipient_email, recipient_name, subject, body (TEXT), variables (JSONB), status, retry_count, max_retries, scheduled_at, sent_at, error_message, user_id, created_at, updated_at |
| EmailTemplateEntity | email_templates | id (UUID), email_type (unique), name, subject_template, body_template (TEXT), description, is_active, created_at, updated_at |
| EmailBaseLayout | email_base_layout | id (UUID), header_html (TEXT), footer_html (TEXT), styles (TEXT), logo_url, primary_color, updated_at |

## Password Reset

| Entity | Tablo | Ana kolonlar / not |
|--------|-------|---------------------|
| PasswordResetToken | password_reset_tokens | id (UUID), user_id (FK → users), token (unique), expires_at, used_at, created_at |

## Sync Infrastructure

| Entity | Tablo | Ana kolonlar / not |
|--------|-------|---------------------|
| SyncTask | sync_tasks | id (UUID), store_id, task_type, status, progress_percentage, current_page, total_pages, items_processed, items_new, items_updated, items_skipped, items_failed, error_message, created_at, started_at, completed_at |

## AI & Knowledge Base

| Entity | Tablo | Ana kolonlar / not |
|--------|-------|---------------------|
| StoreAiSettings | store_ai_settings | id, store_id (FK), ayarlar |
| StoreKnowledgeBase | store_knowledge_base | id, store_id (FK), category, cevap şablonları |
| AnswerTemplate | answer_templates | id, knowledge_base ile ilişkili, şablon metni |
| AiAnswerLog | ai_answer_logs | id, log kayıtları |

---

**Toplam:** 61 entity. Tablo adları `@Table(name = "...")` ile entity'de tanımlı; tümü mevcut koda göre.
