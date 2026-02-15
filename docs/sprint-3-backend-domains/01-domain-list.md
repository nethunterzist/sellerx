# Backend Domain Paketleri (Mevcut Kod)

`com.ecommerce.sellerx.*` paketleri, kısa açıklama ve ana sınıflar. Kaynak: [sellerx-backend/src/main/java/com/ecommerce/sellerx/](sellerx-backend/src/main/java/com/ecommerce/sellerx/).

## Core (auth, users, stores, config, common)

| Paket | Açıklama | Ana sınıflar |
|-------|----------|--------------|
| auth | JWT doğrulama, giriş/kayıt, SecurityConfig | AuthController, AuthService, SecurityConfig, JwtAuthFilter, JwtService |
| users | Kullanıcı yönetimi, rol, tercihler, seçili mağaza | UserController, UserService, UserRepository, UserDetailsServiceImpl |
| stores | Mağaza yönetimi, Trendyol credentials, onboarding | StoreController, StoreService, StoreOnboardingService, StoreRepository |
| config | Zamanlama (Hybrid Sync), ShedLock, Async, CORS, RestTemplate | HybridSyncScheduleConfig, ShedLockConfig, AsyncConfig, RestTemplateConfig |
| common | Trendyol rate limiter, exception handler, paylaşılan yardımcılar | TrendyolRateLimiter, GlobalExceptionHandler |
| controller | Sağlık kontrolü | HealthController |

## Admin

| Paket | Açıklama | Ana sınıflar |
|-------|----------|--------------|
| admin | Admin panel API: mağaza, kullanıcı, sipariş, ürün, billing, referral, bildirim, activity log, dashboard, destek, sandbox, e-posta şablonları, resilience | AdminStoreController, AdminUserController, AdminOrderController, AdminProductController, AdminBillingController, AdminReferralController, AdminNotificationController, AdminActivityLogController, AdminDashboardController, AdminSupportController, AdminSandboxController, AdminSandboxService, AdminEmailTemplateController, AdminResilienceController, ilgili *Service sınıfları |

## Domain: Sipariş, Ürün, Finansal

| Paket | Açıklama | Ana sınıflar |
|-------|----------|--------------|
| orders | Trendyol sipariş senkronizasyonu, komisyon tahmini, gap analizi | TrendyolOrderService, TrendyolOrderScheduledService, TrendyolOrderController, OrderGapAnalysisService, CommissionReconciliationService, StockOrderSynchronizationService |
| products | Ürün kataloğu, maliyet/stok geçmişi | TrendyolProductController, TrendyolProductService, TrendyolProductRepository |
| financial | Trendyol settlement, fatura, kesinti, kargo, ödeme emirleri, mutabakat | TrendyolFinancialSettlementService, TrendyolFinancialSettlementScheduledService, TrendyolInvoiceService, TrendyolOtherFinancialsService, CommissionReconciliationService, TrendyolHistoricalSettlementService |
| categories | Trendyol kategori listesi | TrendyolCategoryController, TrendyolCategoryService, TrendyolCategoryRepository |
| dashboard | İstatistikler, KPI, grafik verisi | DashboardController, DashboardStatsService |

## Domain: Satın Alma, Gider, Billing

| Paket | Açıklama | Ana sınıflar |
|-------|----------|--------------|
| purchasing | Satın alma siparişleri, tedarikçi, raporlar, stok tükenme | PurchaseOrderController, PurchaseOrderService, SupplierController, SupplierService, PurchaseReportService, StockDepletionService, PurchaseOrderExcelService |
| expenses | Gider kategorileri, mağaza giderleri | StoreExpenseService, ExpenseCategory, StoreExpense (entity) |
| billing | Abonelik planları, ödeme, fatura, iyzico entegrasyonu | SubscriptionController, BillingController, CheckoutController, PaymentMethodController, InvoiceController, FeatureController, SubscriptionService, PaymentService, InvoiceService, iyzico/* |

## Domain: Webhook, Trendyol API, Alerts, QA, Returns

| Paket | Açıklama | Ana sınıflar |
|-------|----------|--------------|
| webhook | Trendyol webhook alıcı, yönetim, idempotency | TrendyolWebhookController, TrendyolWebhookService, TrendyolWebhookManagementService, WebhookManagementController, WebhookEventRepository |
| trendyol | Trendyol API client (auth, rate limit) | TrendyolService, TrendyolController |
| alerts | Kural ve uyarı yönetimi, AlertEngine | AlertRuleController, AlertHistoryController, AlertRuleService, AlertHistoryService, AlertEngine |
| qa | Müşteri soruları, pattern keşfi, conflict, auto-submit | TrendyolQaService, QaController, PatternDiscoveryService, SeniorityService, KnowledgeSuggestionService, ConflictDetectionService |
| returns | İadeler, Trendyol claims, analitik | ClaimsController, TrendyolClaimsService, ReturnAnalyticsController |

## Domain: Stok Takibi, Activity Log

| Paket | Açıklama | Ana sınıflar |
|-------|----------|--------------|
| stocktracking | Stok takibi, snapshot, cleanup | StockTrackingController, StockTrackingService, StockTrackingScheduledService, TrendyolPublicStockClient |
| activitylog | Kullanıcı aksiyon logu | ActivityLogController, ActivityLogService, ActivityLogRepository |

## Domain: Eğitim, Bildirim, Referral, Destek, E-posta, Döviz, AI

| Paket | Açıklama | Ana sınıflar |
|-------|----------|--------------|
| education | Eğitim videoları, izleme geçmişi | EducationVideoController, EducationVideoService |
| notifications | Kullanıcı bildirimleri | NotificationController, NotificationService, UserNotificationRepository |
| referral | Davet kodu, ödül günü | ReferralController, ReferralService, ReferralConfig, ReferralRepository |
| support | Destek talepleri, mesajlar | SupportTicketController, SupportTicketService, AdminSupportController |
| email | E-posta gönderimi (SendGrid + SMTP), kuyruk, şablonlar, event-driven bildirimler. Alt paketler: entity/ (EmailQueue, EmailBaseLayout, EmailTemplateEntity), event/ (EmailEventListener, AlertTriggeredEvent, SubscriptionEvent, UserRegisteredEvent, PasswordResetRequestedEvent, EmailVerificationEvent, AdminBroadcastEvent), repository/ (EmailQueueRepository, EmailTemplateRepository, EmailBaseLayoutRepository), scheduler/ (EmailQueueProcessor, SubscriptionReminderJob), service/ (EmailQueueService, EmailTemplateService) | EmailService, SendGridEmailService, SmtpEmailService, SmtpEmailConfig, EmailConfig, EmailStatus, EmailType |
| currency | Döviz kurları (TCMB), scheduled güncelleme | CurrencyController, CurrencyService, TcmbApiClient, ExchangeRateRepository |
| ai | Mağaza AI ayarları, knowledge base, cevap şablonları | AiSettingsController, KnowledgeBaseController, AiSettingsService |

## Domain: Bakım, Kuyruk, Dayanıklılık, WebSocket, Sync

| Paket | Açıklama | Ana sınıflar |
|-------|----------|--------------|
| maintenance | Veritabanı bakımı, veri saklama politikası, materialized view yenileme, tablo bloat kontrolü | DataMaintenanceScheduler, DashboardStatsRepository |
| queue | RabbitMQ tabanlı senkronizasyon mesaj kuyruğu | SyncMessage, SyncQueueConsumer, SyncQueueProducer |
| resilience | Dayanıklı API istemcisi, circuit breaker, retry | ResilientApiClient, ResilientApiException |
| websocket | Gerçek zamanlı bildirimler, WebSocket yapılandırması | WebSocketConfig, WebSocketAuthInterceptor, AlertNotificationService, WebSocketSecurityRules |
| sync | Asenkron sipariş/ürün senkronizasyonu, görev takibi | AsyncOrderSyncService, AsyncProductSyncService, SyncTask, SyncTaskService, SyncTaskRepository |

---

**Toplam:** 34 paket (activitylog, admin, ai, alerts, auth, billing, categories, common, config, controller, currency, dashboard, education, email, expenses, financial, maintenance, notifications, orders, products, purchasing, qa, queue, referral, resilience, returns, stocktracking, stores, support, sync, trendyol, users, webhook, websocket).
