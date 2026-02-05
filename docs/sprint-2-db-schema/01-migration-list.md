# Flyway Migration Listesi (Mevcut Kod)

[sellerx-backend/src/main/resources/db/migration/](sellerx-backend/src/main/resources/db/migration/) altındaki tüm V*.sql dosyaları. Sıra: V numarasına göre.

| V | Dosya adı | Açıklama |
|---|------------|----------|
| V1 | V1__initial_migration.sql | initial_migration |
| V2 | V2__add_stores_table.sql | add_stores_table |
| V3 | V3__add_selected_store_to_users.sql | add_selected_store_to_users |
| V4 | V4__fix_selected_store_constraint.sql | fix_selected_store_constraint |
| V5 | V5__create_trendyol_products_table.sql | create_trendyol_products_table |
| V6 | V6__add_product_status_fields.sql | add_product_status_fields |
| V7 | V7__create_expense_categories_table.sql | create_expense_categories_table |
| V8 | V8__create_store_expenses_table.sql | create_store_expenses_table |
| V9 | V9__convert_frequency_to_enum.sql | convert_frequency_to_enum |
| V10 | V10__create_trendyol_orders_table.sql | create_trendyol_orders_table |
| V11 | V11__add_webhook_id_to_stores.sql | add_webhook_id_to_stores |
| V12 | V12__add_total_price_to_trendyol_orders.sql | add_total_price_to_trendyol_orders |
| V13 | V13__add_stopaj_to_trendyol_orders.sql | add_stopaj_to_trendyol_orders |
| V14 | V14__add_pim_category_id_to_trendyol_products.sql | add_pim_category_id_to_trendyol_products |
| V15 | V15__create_trendyol_categories_table.sql | create_trendyol_categories_table |
| V16 | V16__add_commission_rate_and_shipping_volume_weight_to_trendyol_products.sql | add_commission_rate_and_shipping_volume_weight_to_trendyol_products |
| V17 | V17__add_estimated_commission_to_trendyol_orders.sql | add_estimated_commission_to_trendyol_orders |
| V18 | V18__add_settlement_columns_to_trendyol_orders.sql | add_settlement_columns_to_trendyol_orders |
| V19 | V19__add_financial_transaction_summaries_column.sql | add_financial_transaction_summaries_column |
| V20 | V20__create_webhook_events_table.sql | create_webhook_events_table |
| V21 | V21__add_city_columns_to_orders.sql | add_city_columns_to_orders |
| V22 | V22__create_return_records_table.sql | create_return_records_table |
| V23 | V23__add_performance_indexes.sql | add_performance_indexes |
| V24 | V24__create_qa_tables.sql | create_qa_tables |
| V25 | V25__create_ad_reports_table.sql | create_ad_reports_table |
| V26 | V26__create_ai_knowledge_base.sql | create_ai_knowledge_base |
| V27 | V27__add_user_preferences_column.sql | add_user_preferences_column |
| V28 | V28__create_exchange_rates_table.sql | create_exchange_rates_table |
| V29 | V29__create_knowledge_suggestions.sql | create_knowledge_suggestions |
| V30 | V30__create_qa_patterns.sql | create_qa_patterns |
| V31 | V31__create_conflict_alerts.sql | create_conflict_alerts |
| V32 | V32__create_purchase_orders_table.sql | create_purchase_orders_table |
| V33 | V33__create_purchase_order_items_table.sql | create_purchase_order_items_table |
| V34 | V34__create_activity_log_table.sql | create_activity_log_table |
| V35 | V35__create_subscription_plans.sql | create_subscription_plans |
| V36 | V36__create_subscription_prices.sql | create_subscription_prices |
| V37 | V37__create_subscriptions.sql | create_subscriptions |
| V38 | V38__create_payment_methods.sql | create_payment_methods |
| V39 | V39__create_invoices_and_transactions.sql | create_invoices_and_transactions |
| V40 | V40__create_e_invoices.sql | create_e_invoices |
| V41 | V41__create_plan_features_and_usage.sql | create_plan_features_and_usage |
| V42 | V42__create_subscription_events.sql | create_subscription_events |
| V43 | V43__add_yearly_billing_cycle.sql | add_yearly_billing_cycle |
| V44 | V44__create_trendyol_claims_table.sql | create_trendyol_claims_table |
| V45 | V45__add_commission_tracking_fields.sql | add_commission_tracking_fields |
| V46 | V46__add_store_sync_tracking.sql | add_store_sync_tracking |
| V47 | V47__add_vat_fields_to_store_expenses.sql | add_vat_fields_to_store_expenses |
| V48 | V48__add_vat_fields_to_ad_reports.sql | add_vat_fields_to_ad_reports |
| V49 | V49__create_trendyol_stoppages_table.sql | create_trendyol_stoppages_table |
| V50 | V50__create_trendyol_cargo_invoices_table.sql | create_trendyol_cargo_invoices_table |
| V51 | V51__create_trendyol_payment_orders_table.sql | create_trendyol_payment_orders_table |
| V52 | V52__add_data_source_to_orders.sql | add_data_source_to_orders |
| V53 | V53__add_sync_lock_fields.sql | add_sync_lock_fields |
| V54 | V54__add_historical_sync_progress_fields.sql | add_historical_sync_progress_fields |
| V55 | V55__add_commission_reconciliation.sql | add_commission_reconciliation |
| V56 | V56__add_parallel_sync_phases.sql | add_parallel_sync_phases |
| V57 | V57__add_cost_vat_rate_to_po_items.sql | add_cost_vat_rate_to_po_items |
| V58 | V58__create_education_videos_tables.sql | create_education_videos_tables |
| V59 | V59__create_support_tables.sql | create_support_tables |
| V60 | V60__create_alert_rules_table.sql | create_alert_rules_table |
| V61 | V61__create_trendyol_campaigns_table.sql | create_trendyol_campaigns_table |
| V62 | V62__create_campaign_daily_stats_table.sql | create_campaign_daily_stats_table |
| V63 | V63__add_campaign_id_to_ad_reports.sql | add_campaign_id_to_ad_reports |
| V64 | V64__add_coupon_and_early_payment_to_orders.sql | add_coupon_and_early_payment_to_orders |
| V65 | V65__create_trendyol_deduction_invoices_table.sql | create_trendyol_deduction_invoices_table |
| V66 | V66__add_shipping_estimation_fields.sql | add_shipping_estimation_fields |
| V67 | V67__fix_kargo_fatura_invoice_serial_number.sql | fix_kargo_fatura_invoice_serial_number |
| V68 | V68__create_trendyol_invoices_table.sql | create_trendyol_invoices_table |
| V69 | V69__fix_komisyon_fatura_invoice_serial_number.sql | fix_komisyon_fatura_invoice_serial_number |
| V70 | V70__fix_platform_hizmet_invoice_serial_number.sql | fix_platform_hizmet_invoice_serial_number |
| V71 | V71__fix_yurtdisi_operasyon_iade_bedeli.sql | fix_yurtdisi_operasyon_iade_bedeli |
| V72 | V72__create_buybox_tables.sql | create_buybox_tables |
| V73 | V73__create_stock_tracking_tables.sql | create_stock_tracking_tables |
| V74 | V74__create_suppliers_table.sql | create_suppliers_table |
| V75 | V75__add_supplier_id_to_purchase_orders.sql | add_supplier_id_to_purchase_orders |
| V76 | V76__add_purchasing_features_columns.sql | add_purchasing_features_columns |
| V77 | V77__create_po_attachments_table.sql | create_po_attachments_table |
| V78 | V78__create_referral_system.sql | create_referral_system |
| V79 | V79__add_created_at_to_users.sql | add_created_at_to_users |
| V80 | V80__create_shedlock_table.sql | create_shedlock_table |
| V81 | V81__add_webhook_seller_id_index.sql | add_webhook_seller_id_index |
| V82 | V82__standardize_decimal_precision.sql | standardize_decimal_precision |
| V83 | V83__normalize_store_status_enums_to_uppercase.sql | normalize_store_status_enums_to_uppercase |
| V84 | V84__add_stock_entry_date_and_cost_source.sql | add_stock_entry_date_and_cost_source |

**Toplam:** 84 migration. Flyway sırası V1 → V84; uygulama başlangıcında bu sırayla çalışır.

**Not:** V61–V63 (kampanya tabloları: trendyol_campaigns, campaign_daily_stats, ad_reports.campaign_id) şu an kullanılmıyor (backend'de `ads/` servisi yok). Eski planlama doc'u: [archive/CAMPAIGNS.md](../archive/CAMPAIGNS.md).
