# Faz 5: Frontend Optimizasyonlari

> **Durum**: TAMAMLANDI
> **Tarih**: 2026-02-11

## Ozet

Frontend optimizasyonlari kapsaminda:
- WebSocket entegrasyonu (real-time alerts)
- Dynamic imports (Excel ve Chart bilesenlerinin lazy loading)
- Bundle size optimizasyonu (~530KB azalma potansiyeli)

## 5.1 WebSocket Client Hook

### Dosyalar
- `hooks/use-websocket.ts` - WebSocket baglanti yonetimi hook'u

### Ozellikler
- STOMP over SockJS protokolu
- Otomatik reconnect (exponential backoff)
- Auth token destegi
- Message subscription yonetimi

### Kullanim
```typescript
import { useWebSocket } from '@/hooks/use-websocket';

function Component() {
  const { isConnected, subscribe, unsubscribe } = useWebSocket();

  useEffect(() => {
    const sub = subscribe('/user/queue/alerts', (message) => {
      // Handle alert
    });
    return () => unsubscribe(sub);
  }, []);
}
```

## 5.2 Next.js WebSocket Proxy

### Yapilandirma
- `next.config.mjs` - WebSocket proxy ayarlari
- Frontend `/ws` endpoint'i backend'e yonlendirilir

### Ayarlar
```javascript
async rewrites() {
  return [
    {
      source: '/ws/:path*',
      destination: `${process.env.API_BASE_URL}/ws/:path*`
    }
  ]
}
```

## 5.3 Alert Polling -> WebSocket Gecisi

### Onceki Durum
```typescript
// 30 saniyede bir polling
useUnreadAlertCount({ refetchInterval: 30000 })
useUnreadAlerts({ refetchInterval: 30000 })
```

### Sonraki Durum
```typescript
// WebSocket bagli ise polling devre disi
const wsContext = useWebSocketContext();
const isWsConnected = wsContext?.isConnected ?? false;

useUnreadAlertCount({ disablePolling: isWsConnected })
useUnreadAlerts({ disablePolling: isWsConnected })
```

### Duzenlenen Dosyalar
- `hooks/queries/use-alerts.ts` - `disablePolling` parametresi eklendi
- `components/alerts/notification-center.tsx` - WebSocket state entegrasyonu
- `app/[locale]/(app-shell)/layout.tsx` - WebSocketProvider eklendi

## 5.4 Bundle Optimization ve Dynamic Imports

### Lazy Chart Components

`components/charts/lazy-chart.tsx` dosyasinda tum grafik bilesenleri lazy-loaded wrapper'lara sarild:

| Bilesen | Orjinal Import | Lazy Wrapper |
|---------|---------------|--------------|
| DashboardChart | ~150KB | LazyDashboardChart |
| Sparkline | ~150KB | LazySparkline |
| ExpenseCategoryChart | ~150KB | LazyExpenseCategoryChart |
| ExpenseTrendChart | ~150KB | LazyExpenseTrendChart |
| ReturnCostBreakdown | ~150KB | LazyReturnCostBreakdown |
| ReturnTrendChart | ~150KB | LazyReturnTrendChart |
| ReturnReasonsChart | ~150KB | LazyReturnReasonsChart |
| StockHistoryChart | ~150KB | LazyStockHistoryChart |
| ProfitBreakdownChart | ~150KB | LazyProfitBreakdownChart |
| RevenueTrendChart | ~150KB | LazyRevenueTrendChart |
| StoreStatusChart | ~150KB | LazyStoreStatusChart |
| UserGrowthChart | ~150KB | LazyUserGrowthChart |
| OrderVolumeChart | ~150KB | LazyOrderVolumeChart |

### Lazy Excel Utilities

`lib/utils/lazy-excel.ts` dosyasinda Excel export fonksiyonlari lazy-loaded:

| Fonksiyon | Aciklama |
|-----------|----------|
| exportToXLSX | xlsx kutuphanesi ile export (~180KB) |
| exportToExcelJS | exceljs kutuphanesi ile export (~200KB) |
| formatProductsForExport | Urun verisini Excel formatina cevirir |
| formatOrderItemsForExport | Siparis verisini Excel formatina cevirir |

### Duzenlenen Dosyalar

| Dosya | Degisiklik |
|-------|-----------|
| `components/dashboard/products-table.tsx` | `import * as XLSX` -> dynamic import |
| `components/financial/invoice-export-button.tsx` | `import ExcelJS` -> dynamic import |
| `components/dashboard/chart-view.tsx` | `DashboardChart` -> `LazyDashboardChart` |
| `app/[locale]/(app-shell)/products/page.tsx` | `import ExcelJS` -> dynamic import |
| `app/[locale]/(admin)/admin/users/page.tsx` | `import ExcelJS` -> dynamic import |

### Dynamic Import Pattern

```typescript
// ONCEKI (statik import)
import ExcelJS from "exceljs";
import { saveAs } from "file-saver";

const handleExport = async () => {
  const workbook = new ExcelJS.Workbook();
  // ...
};

// SONRAKI (dynamic import)
const handleExport = async () => {
  const [ExcelJS, { saveAs }] = await Promise.all([
    import("exceljs").then((m) => m.default),
    import("file-saver"),
  ]);

  const workbook = new ExcelJS.Workbook();
  // ...
};
```

## Kazanimlar

### Bundle Size Azalmasi

| Kutuphane | Boyut | Aciklama |
|-----------|-------|----------|
| recharts | ~150KB | Lazy loaded (sadece grafik sayfalarinda yuklenir) |
| xlsx | ~180KB | Lazy loaded (sadece export'ta yuklenir) |
| exceljs | ~200KB | Lazy loaded (sadece export'ta yuklenir) |
| **Toplam** | **~530KB** | Initial bundle'dan cikarildi |

### Performans Iyilestirmeleri

1. **Initial Load Time**: ~530KB daha az indirme
2. **Time to Interactive (TTI)**: Daha hizli ilk kullanilabilirlik
3. **Memory Usage**: Gereksiz kutuphaneler yuklenmez

### Network Kazanimlari

1. **Alert Polling Azalmasi**: WebSocket bagli iken 0 polling request
2. **Bandwith Tasarrufu**: 100 kullanici = ~200 req/dk -> 0 req/dk

## Test Sonuclari

```bash
# TypeScript type check
npx tsc --noEmit --skipLibCheck  # PASSED

# ESLint check
npm run lint  # PASSED (sadece mevcut uyarilar)

# Build test
npm run build  # PASSED
```

## Sonraki Adimlar

1. **Monitoring**: Bundle size izleme (webpack-bundle-analyzer)
2. **Preloading**: Kritik grafiklerin prefetch edilmesi
3. **Code Splitting**: Route-based code splitting
