# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SellerX is an e-commerce management platform for Turkish marketplaces (primarily Trendyol). Sellers manage products, orders, financials, and expenses across multiple stores.

- **Frontend**: Next.js 15 + React 19 + TypeScript (`sellerx-frontend/`)
- **Backend**: Spring Boot 3.4.4 + Java 21 (`sellerx-backend/`)
- **Database**: PostgreSQL 15
- **Timezone**: Turkey (`Europe/Istanbul`) - configured in backend

## ⚠️ Claude Code Rules

**Frontend Changes Require Rebuild**: After any frontend code change, ask the user:
> "Frontend code was modified. Rebuild required (~1 min) to see changes. Should I rebuild?"

If approved: `cd sellerx-frontend && npm run build && npm start`

**Backend Changes**: No rebuild needed - Spring Boot supports hot reload.

**Frontend Runs in Production Mode**: Always use `npm run build && npm start` for lower RAM usage (~200 MB vs ~900 MB dev mode).

## Development Commands

### Quick Start
```bash
# Option 1: Use startup script (opens 4 Terminal windows)
./start-sellerx.sh

# Option 2: Manual startup
./db.sh start                            # 1. Start database (Docker)
cd sellerx-backend && export JWT_SECRET='sellerx-development-jwt-secret-key-2026-minimum-256-bits-required' && ./mvnw spring-boot:run  # 2. Backend
cd sellerx-frontend && npm run build && npm start  # 3. Frontend (production mode)

# Access: Frontend=localhost:3000 | Backend=localhost:8080 | DB=localhost:5432
# Test user: test@test.com / 123456
```

### Database
```bash
./db.sh start    # Start PostgreSQL (port 5432)
./db.sh stop     # Stop container
./db.sh connect  # Connect to psql shell
./db.sh reset    # Reset database (WARNING: deletes all data!)
```

### Frontend
```bash
cd sellerx-frontend
npm run build && npm start  # Production (recommended - low RAM)
npm run dev                 # Development (hot reload, high RAM)
npm run lint                # ESLint check
npx tsc --noEmit            # TypeScript check (no build output)
npx prettier --check .      # Check formatting
npx prettier --write .      # Fix formatting
```

### Backend
```bash
cd sellerx-backend
./mvnw spring-boot:run                    # Start application
./mvnw test                               # Run all tests (~619 tests)
./mvnw test -Dtest=ClassName              # Run single test class
./mvnw test -Dtest=ClassName#methodName   # Run single test method
./mvnw test -Dtest=*ControllerTest        # Run all controller tests
./mvnw clean install -DskipTests          # Build without tests
```

### Alternative: Full Docker Development
```bash
# First time (builds all containers)
docker-compose -f docker-compose.dev.yml up --build -d

# Subsequent runs
docker-compose -f docker-compose.dev.yml up -d

# Stop all services
docker-compose -f docker-compose.dev.yml down

# Reset everything including database
docker-compose -f docker-compose.dev.yml down -v
```

## Architecture

### Data Flow
```
Frontend Components → React Query hooks (hooks/queries/use-*.ts)
    → API Client (lib/api/client.ts) with auto token refresh
    → Next.js API routes (app/api/**/route.ts) - BFF pattern
    → Spring Boot REST API with JWT validation
    → JPA/PostgreSQL
```

### Frontend Structure (`sellerx-frontend/`)
```
app/
├── [locale]/              # i18n routing (tr/en, default: tr)
│   ├── (app-shell)/       # Authenticated layout with sidebar
│   │   ├── dashboard/     # Sales stats, charts, cost analysis
│   │   ├── products/      # Product list, sync, cost/stock management
│   │   ├── orders/        # Order management
│   │   ├── purchasing/    # Purchase orders, suppliers, reports (profitability, stock valuation)
│   │   ├── hakedis/       # Trendyol settlement tracking
│   │   ├── profit/        # Profit analysis
│   │   ├── settings/      # Store settings, webhooks configuration
│   │   └── new-store/     # Store creation with Trendyol credentials
│   └── (auth)/            # Public auth pages (sign-in, register)
└── api/                   # Next.js API routes (proxy to backend)
```

**Key Directories**:
- `components/` - React components by feature (auth, dashboard, products, settings, ui)
- `hooks/queries/` - React Query hooks: `use-auth.ts`, `use-stores.ts`, `use-products.ts`, `use-orders.ts`, `use-webhooks.ts`, `use-financial.ts`, `use-ads.ts`, `use-qa.ts`, `use-returns.ts`, `use-purchasing.ts`, `use-billing.ts`, `use-hakedis.ts`, `use-admin.ts`
- `lib/api/client.ts` - Centralized API client with queue-based token refresh (prevents duplicate refresh requests via `isRefreshing` flag and subscriber queue)
- `lib/validators/` - Zod schemas for form validation
- `messages/` - i18n translations (en.json, tr.json)

### Backend Structure (`sellerx-backend/src/main/java/com/ecommerce/sellerx/`)
```
admin/          # Admin panel: users, stores, billing, referrals, notifications, activity logs
auth/           # JWT auth, SecurityConfig, JwtAuthFilter
users/          # User entity, service, controller, Role enum
stores/         # Store management, credentials JSONB
products/       # Trendyol products, cost/stock history JSONB
orders/         # Order sync, status tracking, order_items JSONB
purchasing/     # Purchase orders, suppliers, stock valuation, profitability reports
hakedis/        # Trendyol settlement (hakedis) calculation and simulation
dashboard/      # Stats calculation, daily/monthly aggregations
financial/      # Financial data sync from Trendyol
expenses/       # Expense categories, frequency enum
trendyol/       # Trendyol API client integration
webhook/        # Real-time Trendyol webhook receiver (public endpoints)
common/         # TrendyolRateLimiter (10 req/sec), GlobalExceptionHandler
config/         # Spring, CORS, AsyncConfig for @Async support
```

### Database Schema

Key tables (Flyway migrations in `src/main/resources/db/migration/`):
- `users` - User accounts with BCrypt passwords, `selected_store_id` for active store
- `stores` - Store config with `credentials` JSONB (Trendyol API keys), `sync_status` for onboarding
- `trendyol_products` - Product catalog with `cost_and_stock_info` JSONB for history, `last_commission_rate`
- `trendyol_orders` - Order history with `order_items` JSONB, `estimated_commission`, `is_commission_estimated`
- `webhook_events` - Webhook audit log with idempotency (`event_id` unique)

### Key System Behaviors

**Store Onboarding**: When a new store is created, `StoreOnboardingService` automatically triggers async sync:
1. `SYNCING_PRODUCTS` - Fetches all products from Trendyol
2. `SYNCING_ORDERS` - Fetches order history
3. `SYNCING_FINANCIAL` - Fetches financial settlements
4. `COMPLETED` - Sets `initialSyncCompleted = true`

**Scheduled Jobs** (see `TrendyolOrderScheduledService`):
- `syncOrdersForAllTrendyolStores`: Daily at 06:15 - Full order sync
- `catchUpSync`: Every hour - Syncs last 2 hours of orders
- `fetchAndUpdateSettlementsForAllStores`: Daily at 07:00 - Financial settlements

**Rate Limiting**: `TrendyolRateLimiter` limits all Trendyol API calls to 10 requests/second using Guava RateLimiter.

**Commission System**: Orders have estimated commission (`vatBaseAmount × commissionRate / 100`) until financial settlement arrives with actual rates. `isCommissionEstimated` tracks this.

**Admin Impersonation**: Admin users can view a customer's account in read-only mode via a new browser tab:
1. Admin clicks "Hesabına Gir" on admin users page → backend generates impersonation JWT (1h, no refresh)
2. New tab opens `/impersonate?token=...` → token saved to `sessionStorage` (tab-scoped) → redirect to `/dashboard`
3. API client reads `sessionStorage` impersonation token → sends `X-Impersonation-Token` header → BFF routes forward via `getBackendHeaders()`
4. Backend `ReadOnlyInterceptor` blocks POST/PUT/DELETE/PATCH for read-only tokens (403)
5. Red banner shows in UI: "[UserName] hesabını görüntülüyorsunuz - Salt Okunur"
6. Audit log: every impersonation recorded in `impersonation_logs` table (admin, target, IP, user-agent)

**Trendyol API Integration** (`TrendyolService.java`):
- All API calls go through `TrendyolService` which handles auth headers and rate limiting
- Credentials stored encrypted in `stores.credentials` JSONB column
- API base URL: `https://api.trendyol.com/sapigw/suppliers/{sellerId}/`
- Auth: Basic auth with `apiKey:apiSecret` base64 encoded

### Authentication
- Access token: 1 hour validity, HTTP-only cookie (`access_token`)
- Refresh token: 7 days validity, HTTP-only cookie (`refreshToken`)
- Frontend middleware (`middleware.ts`) checks for cookie presence, redirects to `/sign-in` if missing
- API client (`lib/api/client.ts`) auto-refreshes tokens on 401 with queue-based retry
- Public routes (no auth required): `/sign-in`, `/register`, `/forgot-password`

## Environment Variables

**Frontend** (`.env.local`):
```
API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

**Backend** (via Spring or environment):
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/sellerx_db
SPRING_DATASOURCE_USERNAME=furkanyigit  # Local PostgreSQL user
SPRING_DATASOURCE_PASSWORD=             # Empty for local dev
JWT_SECRET=sellerx-development-jwt-secret-key-2026-minimum-256-bits-required  # MUST be 32+ chars!
WEBHOOK_BASE_URL=http://localhost:8080
WEBHOOK_SIGNATURE_SECRET=<optional-hmac-secret>
```

**JWT Secret Requirement**:
- Must be at least **32 characters** (256-bit) for HMAC-SHA256
- Shorter secrets cause **500 Internal Server Error** on login (not 401!)
- The `start-sellerx.sh` script sets this automatically

## Troubleshooting

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Login returns 500 | JWT_SECRET not set or < 32 chars | `export JWT_SECRET='sellerx-development-jwt-secret-key-2026-minimum-256-bits-required'` |
| Database connection fails | Docker not running | `./db.sh start` |
| Port conflicts | Local PostgreSQL running | `brew services stop postgresql` (Mac) |
| `'id' !== 'storeId'` error | Inconsistent dynamic route params | All routes under same path must use same param name |

### Reset Test User Password
```bash
./db.sh connect
UPDATE users SET password = '$2a$10$lK3BvFvJbhPclnwzqxJ1guJW0ILBLNBzTqGm8eNi8m8hDCpigKw3a' WHERE email = 'test@test.com';
```

### Backend Not Starting
```bash
# Check if port 8080 is in use
lsof -i :8080

# If Flyway migration fails, check migration files
ls sellerx-backend/src/main/resources/db/migration/

# Verify database is accessible
./db.sh connect
\dt  # List tables
```

### Frontend Build Errors
```bash
cd sellerx-frontend
npm run lint          # Check ESLint errors
npx tsc --noEmit     # Check TypeScript errors without building
```

## Debugging and Observability

After any code change, **always** run related tests first. If there's a bug, check test output and logs before anything else.

### Backend Debugging

1. **Run tests** — first step after any change:
```bash
cd sellerx-backend
./mvnw test                               # All tests (~619 tests)
./mvnw test -Dtest=*ServiceTest           # Service tests only
./mvnw test -Dtest=ClassName              # Single test class
./mvnw test -Dtest=ClassName#methodName   # Single test method
```

2. **Read logs** — structured logging is active at runtime:
   - Development: human-readable format (console)
   - Production: JSON format (configured via logback-spring.xml)
   - Each request has `requestId` and `userId` in MDC context

3. **Actuator endpoints**:
```bash
curl localhost:8080/actuator/health        # App + Trendyol API health
curl localhost:8080/actuator/metrics       # Metrics list
curl localhost:8080/actuator/metrics/trendyol.api.calls  # Trendyol API call count
```

4. **Custom metrics** (Micrometer):
   - `order.sync.duration` — order sync duration
   - `trendyol.api.calls` — Trendyol API call count (by endpoint + error)
   - `webhook.events` — webhook event count (by event type)

### Frontend Debugging

1. **Run tests**:
```bash
cd sellerx-frontend
npm run test                  # All tests (Vitest, watch mode)
npm run test -- --run         # CI mode (no watch)
```

2. **Error Boundary**: Shows error page instead of white screen on component crash. Errors are logged via `lib/logger.ts`.

3. **Logger** (`lib/logger.ts`): Writes to console in development, silent in production. Use this instead of `console.log`:
```typescript
import { logger } from '@/lib/logger';
logger.error('Order sync failed', { storeId, error });
```

## Development Patterns

### Frontend
- **Types/Validators**: Check `sellerx-frontend/types/` and `lib/validators/` before creating new ones
- **API Calls**: Use React Query hooks in `hooks/queries/` - they handle caching and token refresh
- **UI Components**: Use Shadcn/ui from `components/ui/` - based on Radix primitives (Tailwind v4)
- **Forms**: Use React Hook Form + Zod for validation
- **i18n**: All user-facing strings go in `messages/tr.json` and `messages/en.json`
- **Icons**: Use `lucide-react` for icons
- **Charts**: Use `recharts` for data visualization

### Backend
- **DTOs**: Use MapStruct for entity-to-DTO mapping
  - **CRITICAL**: In `pom.xml` annotation processors, Lombok MUST be listed BEFORE MapStruct (order matters!)
- **JSONB Storage**: Product cost/stock history and order items stored as JSONB for flexibility
- **Security**: Webhook endpoints are public via `WebhookSecurityRules.java`; all other endpoints require JWT
- **Migrations**: Flyway migrations in `src/main/resources/db/migration/` with naming `V{n}__description.sql`
- **Async**: Use `@Async` annotation for long-running operations (see `AsyncConfig.java`)

### Backend Testing
Tests use TestContainers (PostgreSQL) + JUnit 5 + Mockito. Base classes in `src/test/java/.../common/`:
- `BaseIntegrationTest` - Full Spring context with real PostgreSQL container
- `BaseControllerTest` - WebMvcTest with mocked services
- `BaseUnitTest` - Pure unit tests with Mockito
- `TestDataBuilder` - Fluent API for creating test entities
  - **Note**: `TestDataBuilder` does not set entity IDs (`@GeneratedValue`). In unit tests, set IDs manually: `entity.setId(UUID.randomUUID())`

```java
// Example: Integration test extending BaseIntegrationTest
@SpringBootTest
class OrderServiceTest extends BaseIntegrationTest {
    @Autowired private TrendyolOrderService orderService;

    @Test
    void shouldCalculateCommission() {
        Store store = TestDataBuilder.store().withUser(testUser).build();
        // TestContainers provides real PostgreSQL
    }
}

// Example: Controller test with mocked services
@WebMvcTest(StoreController.class)
class StoreControllerTest extends BaseControllerTest {
    @MockBean private StoreService storeService;

    @Test
    @WithMockUser
    void shouldReturnStores() throws Exception {
        mockMvc.perform(get("/api/stores/my"))
            .andExpect(status().isOk());
    }
}
```

### Store Selector Pattern
Users can have multiple stores. The active store is tracked via `user.selectedStoreId`:
- Frontend: `useSelectedStore()` hook returns current store
- Backend: Most endpoints filter by `storeId` from request params
- Changing store: `PUT /api/users/selected-store` updates preference

## Critical Constraints

### Next.js Dynamic Route Parameters
All dynamic route segments in the same path hierarchy **must use the same parameter name**:
- `app/api/stores/[id]/route.ts` ✅
- `app/api/stores/[id]/webhooks/status/route.ts` ✅
- `app/api/stores/[storeId]/webhooks/status/route.ts` ❌ (conflicts with `[id]`)

### Webhook Endpoints
Webhook receiver (`/api/webhook/trendyol/{sellerId}`) must:
- Return 200 OK within 5 seconds (Trendyol requirement)
- Be publicly accessible (no JWT required)
- Handle idempotency via `event_id` to prevent duplicate processing

### Known Technical Debt
- TypeScript strict mode disabled (`tsconfig.json`: `strict: false`)
- Net profit calculation missing: commission, stoppage, return costs not subtracted

## Adding New Features

### New API Endpoint Pattern
1. **Backend**: Add controller in appropriate package (`sellerx-backend/src/main/java/com/ecommerce/sellerx/{feature}/`)
2. **Frontend API Route**: Create `sellerx-frontend/app/api/{feature}/route.ts` - proxies to backend
3. **React Query Hook**: Add to `sellerx-frontend/hooks/queries/use-{feature}.ts`
4. **Component**: Use hook in component, React Query handles caching/refetching

### Example: Adding a new endpoint
```typescript
// 1. Frontend API route (app/api/example/route.ts)
import { apiClient } from "@/lib/api/client";
export async function GET(request: Request) {
  const response = await apiClient.get("/api/example", request);
  return Response.json(await response.json());
}

// 2. React Query hook (hooks/queries/use-example.ts)
export function useExample() {
  return useQuery({
    queryKey: ["example"],
    queryFn: () => fetch("/api/example").then(res => res.json())
  });
}
```

### Why BFF Pattern?
All frontend API calls go through Next.js API routes (not directly to Spring Boot) because:
- Cookie handling for JWT tokens happens server-side
- Automatic token refresh via `apiClient` queue
- Hides backend URL from browser
- Enables SSR data fetching

## Documentation Reference

All documentation (inventory, maps, architecture) is in the **docs/** directory. Entry point: [docs/README.md](docs/README.md).

Detailed architecture docs in `docs/architecture/`:
- `STORE_ONBOARDING.md` - Async store sync flow
- `COMMISSION_SYSTEM.md` - Estimated vs actual commission calculation
- `WEBHOOK_SYSTEM.md` - Trendyol webhook processing
- `HISTORICAL_SETTLEMENT_SYNC.md` - Workaround for Trendyol's 90-day API limit
- `ALERT_SYSTEM.md` - User-defined alert rules
- `RATE_LIMITING.md` - Trendyol API rate limiting (10 req/sec)
- `DATABASE_SCHEMA.md` - Full database schema reference
- `ASYNC_PROCESSING.md` - @Async patterns and thread pools
- `TRENDYOL_API_LIMITS.md` - API limitations and workarounds

Development log: `docs/CHANGELOG.md`
