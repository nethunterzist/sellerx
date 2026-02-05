# Satın Alma (Purchasing) ve Stok Tükenme Raporu

Bu dokümantasyon SellerX platformundaki satın alma siparişleri (PO), tedarikçi yönetimi ve raporlar (maliyet geçmişi, FIFO, stok değeri, karlılık, **stok tükenme**) özelliğini açıklar.

## Genel Bakış

Purchasing modülü satın alma siparişlerini (PO), tedarikçileri ve ilgili raporları yönetir. Raporlar: maliyet geçmişi (ürün bazlı), FIFO analizi, stok değeri, karlılık, özet ve **stok tükenme** (depleted stock). Frontend’de purchasing sayfaları (`/purchasing`, `/purchasing/[poId]`, `/purchasing/suppliers`, `/purchasing/reports/*`) ve dashboard’daki **stock-depletion banner** (stok tükenen ürün uyarısı) bu modüle bağlıdır.

## Backend Yapısı

Paket: `sellerx-backend/src/main/java/com/ecommerce/sellerx/purchasing/`

Ana sınıflar:

- **PurchaseOrderController** — PO CRUD, kalemler, ekler, export/import, rapor endpoint’leri (cost-history, fifo-analysis, stock-valuation, profitability, summary, **stock-depletion**).
- **PurchaseOrderService**, **PurchaseReportService** — PO ve rapor iş mantığı.
- **StockDepletionService** — Stok tükenen ürünleri listeler; `TrendyolProduct.stockDepleted` ve `cost_and_stock_info` son stok tarihi kullanılır.
- **SupplierController**, **SupplierService** — Tedarikçi CRUD.
- **DTO’lar:** DepletedProductDto, ProfitabilityResponse, StockValuationResponse, PurchaseOrderDto, PurchaseOrderSummaryDto vb.

Endpoint özeti (tam liste: [sprint-1-api-inventory/01-backend-endpoints.md](../sprint-1-api-inventory/01-backend-endpoints.md) — Purchasing & Suppliers):

- `GET/POST /api/stores/{storeId}/purchase-orders`, `GET/PUT/DELETE .../purchase-orders/{poId}`, kalemler, ekler, status, duplicate, split, attachments, export, import.
- `GET /api/stores/{storeId}/purchase-orders/reports/product/{productId}/cost-history`
- `GET /api/stores/{storeId}/purchase-orders/reports/fifo-analysis`
- `GET /api/stores/{storeId}/purchase-orders/reports/stock-valuation`
- `GET /api/stores/{storeId}/purchase-orders/reports/profitability`
- `GET /api/stores/{storeId}/purchase-orders/reports/summary`
- **`GET /api/stores/{storeId}/purchase-orders/reports/stock-depletion`** — Stok tükenen ürün listesi.
- `GET/POST/PUT/DELETE /api/stores/{storeId}/suppliers`, `.../suppliers/{supplierId}`.

## Stock Depletion Raporu

Stok tükenen ürünleri listeleyen rapor; sipariş/sync tarafında stok bilgisi tükenen (stockDepleted) ürünleri gösterir.

### Backend

- **StockDepletionService** — `getDepletedProducts(storeId)`: mağaza için `TrendyolProduct` listesinde `stockDepleted == true` olanları döner; `DepletedProductDto` (productId, productName, barcode, productImage, lastStockDate) üretir.
- **DepletedProductDto** — productId, productName, barcode, productImage, lastStockDate.
- **Endpoint:** `GET /api/stores/{storeId}/purchase-orders/reports/stock-depletion` (PurchaseOrderController).

### BFF

- **Route:** `app/api/purchasing/orders/[storeId]/reports/stock-depletion/route.ts` — Backend’e proxy; `GET /api/stores/{storeId}/purchase-orders/reports/stock-depletion`.

### Frontend

- **Dashboard:** `components/dashboard/stock-depletion-banner.tsx` — Seçili mağaza için stok tükenen ürün sayısı/listesi gösterir; ayrı bir stock-depletion sayfası yok.

## Veritabanı

Tablolar: `purchase_orders`, `purchase_order_items`, `suppliers`, ilgili migration’lar. Detay: [sprint-2-db-schema/01-migration-list.md](../sprint-2-db-schema/01-migration-list.md) (V32, V33, V74, V75 vb.).

## Mal Alış KDV'si (Purchase VAT)

KDV sayfasında (`/kdv`) satın alınan malların KDV'si aylık olarak hesaplanır. Türk Tek Düzen Hesap Planı'na uygun terminoloji kullanılır: **Mal Alış KDV'si** (İndirilecek KDV / 191 hesap).

### Hesaplama Mantığı

Sadece **CLOSED** statüdeki satın alma siparişleri (PO) dahil edilir. Her PO kaleminin KDV'si:

```
Kalem KDV = totalCostPerUnit × unitsOrdered × costVatRate / 100
```

- `totalCostPerUnit` = `manufacturingCostPerUnit + transportationCostPerUnit` (DB'de generated column)
- `costVatRate` = Kalem bazlı KDV oranı (%0, %10, %20 vb.)

### stockEntryDate Fallback Zinciri

Bir kalemin hangi aya ait olduğunu belirlemek için **stok giriş tarihi** kullanılır. Fallback sırası:

1. `PurchaseOrderItem.stockEntryDate` (item-level, en yüksek öncelik)
2. `PurchaseOrder.stockEntryDate` (PO-level)
3. `PurchaseOrder.poDate` (son çare — stockEntryDate null ise)

Kalem ancak **effectiveStockEntryDate** seçilen ay aralığında ise o ayın KDV hesabına dahil edilir.

### Oran Bazlı Gruplama

KDV sayfasında sonuçlar oran bazlı gruplanır:

| KDV Oranı | Maliyet (KDV hariç) | KDV Tutarı | Kalem Sayısı |
|-----------|---------------------|------------|--------------|
| %0        | ...                 | ₺0         | ...          |
| %10       | ...                 | ...        | ...          |
| %20       | ...                 | ...        | ...          |

### Backend Sınıfları

- **`PurchaseVatDto`** — Toplam maliyet, toplam KDV, kalem sayısı, `List<PurchaseVatByRate>` kırılım
- **`SalesVatDto`** — Satış tarafı KDV özeti (Hesaplanan KDV)
- **`PurchaseOrderRepository.findClosedWithItemsByStoreId(storeId)`** — CLOSED PO'ları item'larıyla fetch eder
- **`TrendyolInvoiceService.getInvoiceSummary()`** — Purchase VAT hesaplama, `InvoiceSummaryDto.purchaseVat` alanını doldurur

### Frontend Entegrasyonu

KDV sayfası (`app/[locale]/(app-shell)/kdv/page.tsx`):
- Üst özet kartında "Mal Alış" tutarı gösterilir
- GİDER tablosunda "Mal Alış KDV'si" satırı (kalem sayısı, KDV hariç maliyet, KDV tutarı)
- GENEL TOPLAM'da alış KDV'si düşülür

### Testler

6 unit test (`TrendyolInvoiceServicePurchaseVatTest`):
- Dönem içi kalemlerin KDV hesaplaması
- Dönem dışı kalemlerin hariç tutulması
- Item-level stockEntryDate override
- stockEntryDate null → poDate fallback
- %0, %10, %20 oran bazlı gruplama
- CLOSED PO yokken sıfır dönüş

## Referanslar

- Backend endpoint envanteri: [sprint-1-api-inventory/01-backend-endpoints.md](../sprint-1-api-inventory/01-backend-endpoints.md) (Purchasing & Suppliers).
- Domain listesi: [sprint-3-backend-domains/01-domain-list.md](../sprint-3-backend-domains/01-domain-list.md) (purchasing paketi).
