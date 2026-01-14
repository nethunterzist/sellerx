# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SellerX is an e-commerce management platform for Turkish marketplaces (primarily Trendyol integration). Sellers can manage products, orders, financials, and expenses across multiple stores.

- **Frontend**: Next.js 15 + React 19 + TypeScript (`sellerx-frontend/`)
- **Backend**: Spring Boot 3.4.4 + Java 21 (`sellerx-backend/`)
- **Database**: PostgreSQL 15

## Development Commands

### Docker Development (Recommended)
```bash
# Start all services (frontend: 3011, backend: 8011, db: 5411)
docker-compose -f docker-compose.dev.yml up --build -d

# Stop services
docker-compose -f docker-compose.dev.yml down

# Reset database (deletes all data)
docker-compose -f docker-compose.dev.yml down -v

# View logs
docker-compose -f docker-compose.dev.yml logs -f [frontend|backend|postgres]

# Enter container shell
docker exec -it sellerx-frontend sh
docker exec -it sellerx-backend bash
docker exec -it sellerx-postgres psql -U postgres -d sellerx_db

# Quick restart after code changes
docker-compose -f docker-compose.dev.yml restart backend   # 5-10 seconds
docker-compose -f docker-compose.dev.yml up --build backend  # 1-2 min (new files/pom.xml changes)
```

### Frontend (standalone at localhost:3000)
```bash
cd sellerx-frontend
npm install
npm run dev      # Development server
npm run build    # Production build
npm run lint     # ESLint check
```

### Backend (standalone at localhost:8080)
```bash
cd sellerx-backend
./mvnw spring-boot:run                    # Run with Maven wrapper
./mvnw clean install -DskipTests          # Build without tests
./mvnw flyway:migrate                     # Run database migrations
./mvnw flyway:clean flyway:migrate        # Reset and re-run migrations
```

## Architecture

### Frontend Structure (`sellerx-frontend/`)
```
app/
├── [locale]/              # i18n routing (tr/en, default: tr)
│   ├── (app-shell)/       # Authenticated layout with sidebar
│   │   ├── dashboard/     # Sales stats, charts, cost analysis
│   │   ├── products/      # Product list, sync, cost/stock management
│   │   ├── orders/        # Order management
│   │   ├── profile/       # User profile
│   │   ├── settings/      # App settings
│   │   └── new-store/     # Store creation with Trendyol credentials
│   └── (auth)/            # Public auth pages (sign-in, register)
└── api/                   # Next.js API routes (BFF pattern)
    ├── auth/              # login, register, refresh, me, logout
    ├── stores/            # Store CRUD + test-connection
    ├── products/          # Product CRUD + sync
    ├── orders/            # Orders CRUD + sync + date range
    ├── dashboard/         # Stats endpoints
    └── users/             # User management + selected-store
```

### Key Frontend Directories
- `components/` - React components organized by feature (auth, dashboard, products, forms, ui)
- `hooks/queries/` - React Query hooks: `use-auth.ts`, `use-stores.ts`, `use-products.ts`, `use-orders.ts`, `use-stats.ts`
- `lib/api/client.ts` - Centralized API client with auto token refresh
- `lib/validators/` - Zod schemas for form validation
- `messages/` - i18n translation files (en.json, tr.json)

### Backend API Endpoints

**Auth (`/auth`)**
- `POST /auth/login` - JWT login (returns access + refresh tokens)
- `POST /auth/logout` - Logout
- `POST /auth/refresh` - Refresh access token
- `GET /auth/me` - Current user info

**Users (`/users`)**
- `GET/POST /users` - List all / Create new (public)
- `GET/PUT/DELETE /users/{id}` - CRUD operations
- `GET/POST /users/selected-store` - Get/Set selected store

**Stores (`/stores`)**
- `GET /stores/my` - Current user's stores
- `GET/POST /stores` - List all / Create new
- `GET/PUT/DELETE /stores/{id}` - CRUD operations
- `GET /stores/test-connection` - Test Trendyol API connection

**Products (`/products`)**
- `POST /products/sync/{storeId}` - Sync from Trendyol
- `GET /products/store/{storeId}` - List store products
- `PUT /products/{productId}/cost-and-stock` - Update cost/stock

**Orders (`/api/orders`)**
- `POST /api/orders/stores/{storeId}/sync` - Sync orders from Trendyol
- `GET /api/orders/stores/{storeId}` - List orders (paginated)
- `GET /api/orders/stores/{storeId}/by-date-range` - Filter by date
- `GET /api/orders/stores/{storeId}/statistics` - Order stats

**Dashboard (`/dashboard`)**
- `GET /dashboard/stats` - All stores stats
- `GET /dashboard/stats/{storeId}` - Store-specific stats

**Expenses (`/expenses`)**
- `GET /expenses/categories` - Expense categories
- `GET/POST /expenses/store/{storeId}` - Store expenses
- `PUT/DELETE /expenses/store/{storeId}/{expenseId}` - Manage expense

### Backend Structure (`sellerx-backend/src/main/java/com/ecommerce/sellerx/`)
```
auth/           # JWT auth, SecurityConfig, JwtAuthFilter
users/          # User entity, service, controller, Role enum
stores/         # Store management, credentials JSONB
products/       # Trendyol products, cost/stock history JSONB
orders/         # Order sync, status tracking, order_items JSONB
dashboard/      # Stats calculation, daily/monthly aggregations
financial/      # Financial data sync from Trendyol
expenses/       # Expense categories, frequency enum
trendyol/       # Trendyol API client and integration
webhook/        # Trendyol webhook receiver (disabled by default)
categories/     # Product categories management
config/         # Spring, CORS, security configuration
common/         # Shared utilities, base entities
```

### Database Schema (Flyway migrations in `db/migration/`)

| Table | Key Columns | Notes |
|-------|-------------|-------|
| `users` | id, name, email, password, role, selected_store_id | BCrypt passwords |
| `stores` | id (UUID), user_id, store_name, marketplace, credentials (JSONB) | Trendyol API keys |
| `trendyol_products` | id (UUID), store_id, product_id, barcode, cost_and_stock_info (JSONB) | Product catalog |
| `trendyol_orders` | id (UUID), store_id, ty_order_number, order_items (JSONB), status | Order history |
| `expense_categories` | id, name | Fixed expense types |
| `store_expenses` | id, store_id, category_id, amount, frequency | Recurring expenses |

### Data Flow
1. Frontend components use React Query hooks (`hooks/queries/use-*.ts`)
2. Hooks call API client (`lib/api/client.ts`) with auto token refresh
3. Client calls Next.js API routes (`app/api/**/route.ts`)
4. API routes proxy to Spring Boot backend with JWT in headers
5. Backend validates JWT, processes request via JPA/PostgreSQL

### Authentication Flow
- Access token: 1 hour validity, stored in HTTP-only cookie
- Refresh token: 7 days validity, stored in HTTP-only cookie
- Frontend middleware (`middleware.ts`) checks auth, redirects to `/sign-in` if invalid
- API client auto-refreshes on 401 response

## Tech Stack

**Frontend**: Next.js 15, React 19, TypeScript 5, Tailwind CSS 4, Shadcn/ui (Radix), TanStack Query 5, SWR, React Hook Form, Zod, next-intl, next-themes, date-fns, Lucide icons

**Backend**: Spring Boot 3.4.4, Java 21, Spring Security, JWT (jjwt 0.12.6), Spring Data JPA, Hibernate, Flyway 10.20.1, Lombok, MapStruct, PostgreSQL 15

## Environment Variables

**Frontend** (`.env.local`):
```
API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

**Backend** (via Spring Dotenv or environment):
```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/sellerx_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=<password>
JWT_SECRET=<base64-encoded-secret>
```

## Known Technical Debt

- TypeScript strict mode disabled (`tsconfig.json`: `strict: false`)
- ESLint/TypeScript errors ignored during builds (`next.config.ts`: `ignoreBuildErrors: true`)
- Test coverage is minimal
- Incomplete modules: Orders (partial), Financial (backend only), Expenses (partial)
- JWT secret hardcoded in docker-compose.dev.yml
- CORS wide open for localhost

## Module Status

| Module | Backend | Frontend | Status |
|--------|---------|----------|--------|
| Auth | ✅ | ✅ | Working |
| Users | ✅ | ✅ | Working |
| Stores | ✅ | ✅ | Working |
| Products | ✅ | ✅ | Working |
| Orders | ⚠️ | ⚠️ | Partial |
| Dashboard | ✅ | ⚠️ | Partial |
| Financial | ⚠️ | ❌ | Backend only |
| Expenses | ⚠️ | ⚠️ | Partial |
| Webhook | ⚠️ | ❌ | Backend only |

## Deployment

- **Frontend**: Vercel (standalone output)
- **Backend**: Railway
- **Database**: PostgreSQL (managed)
