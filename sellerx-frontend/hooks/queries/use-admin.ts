import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  AdminDashboardStats,
  AdminUser,
  AdminUserListItem,
  AdminStore,
  AdminStoreListItem,
  Page,
  ChangeRoleRequest,
} from "@/types/admin";
import { TrendyolProduct } from "@/types/product";
import { TrendyolOrder } from "@/types/order";

// Admin Dashboard Stats
export function useAdminDashboardStats() {
  return useQuery<AdminDashboardStats>({
    queryKey: ["admin", "dashboard", "stats"],
    queryFn: async () => {
      const response = await fetch("/api/admin/dashboard/stats");
      if (!response.ok) {
        throw new Error("Admin dashboard stats alınamadı");
      }
      return response.json();
    },
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
}

// Admin Users List
export function useAdminUsers(page: number = 0, size: number = 20, sort: string = "id,desc") {
  return useQuery<Page<AdminUserListItem>>({
    queryKey: ["admin", "users", page, size, sort],
    queryFn: async () => {
      const params = new URLSearchParams({
        page: String(page),
        size: String(size),
        sort,
      });
      const response = await fetch(`/api/admin/users?${params}`);
      if (!response.ok) {
        throw new Error("Kullanıcı listesi alınamadı");
      }
      return response.json();
    },
  });
}

// Admin User Detail
export function useAdminUser(id: number | string | undefined) {
  return useQuery<AdminUser>({
    queryKey: ["admin", "users", id],
    queryFn: async () => {
      const response = await fetch(`/api/admin/users/${id}`);
      if (!response.ok) {
        throw new Error("Kullanıcı bilgisi alınamadı");
      }
      return response.json();
    },
    enabled: !!id,
  });
}

// Admin User Search
export function useAdminUserSearch(query: string) {
  return useQuery<AdminUserListItem[]>({
    queryKey: ["admin", "users", "search", query],
    queryFn: async () => {
      const response = await fetch(`/api/admin/users/search?q=${encodeURIComponent(query)}`);
      if (!response.ok) {
        throw new Error("Kullanıcı araması başarısız");
      }
      return response.json();
    },
    enabled: query.length >= 2,
  });
}

// Admin Change User Role
export function useChangeUserRole() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, role }: { id: number; role: ChangeRoleRequest["role"] }) => {
      const response = await fetch(`/api/admin/users/${id}/role`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ role }),
      });
      if (!response.ok) {
        throw new Error("Rol değiştirilemedi");
      }
      return response.json();
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ["admin", "users"] });
      queryClient.invalidateQueries({ queryKey: ["admin", "users", variables.id] });
      queryClient.invalidateQueries({ queryKey: ["admin", "dashboard", "stats"] });
    },
  });
}

// Admin Stores List
export function useAdminStores(page: number = 0, size: number = 20, sort: string = "createdAt,desc") {
  return useQuery<Page<AdminStoreListItem>>({
    queryKey: ["admin", "stores", page, size, sort],
    queryFn: async () => {
      const params = new URLSearchParams({
        page: String(page),
        size: String(size),
        sort,
      });
      const response = await fetch(`/api/admin/stores?${params}`);
      if (!response.ok) {
        throw new Error("Mağaza listesi alınamadı");
      }
      return response.json();
    },
  });
}

// Admin Store Detail
export function useAdminStore(id: string | undefined) {
  return useQuery<AdminStore>({
    queryKey: ["admin", "stores", id],
    queryFn: async () => {
      const response = await fetch(`/api/admin/stores/${id}`);
      if (!response.ok) {
        throw new Error("Mağaza bilgisi alınamadı");
      }
      return response.json();
    },
    enabled: !!id,
  });
}

// Admin Store Search
export function useAdminStoreSearch(query: string) {
  return useQuery<AdminStoreListItem[]>({
    queryKey: ["admin", "stores", "search", query],
    queryFn: async () => {
      const response = await fetch(`/api/admin/stores/search?q=${encodeURIComponent(query)}`);
      if (!response.ok) {
        throw new Error("Mağaza araması başarısız");
      }
      return response.json();
    },
    enabled: query.length >= 2,
  });
}

// Admin Impersonate User
export function useImpersonateUser() {
  return useMutation<
    { token: string },
    Error,
    { id: number; name: string; email: string }
  >({
    mutationFn: async ({ id }) => {
      const response = await fetch(`/api/admin/users/${id}/impersonate`, {
        method: "POST",
      });
      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || "Hesaba giriş başarısız");
      }
      return response.json();
    },
  });
}

// Admin Trigger Store Sync
export function useTriggerStoreSync() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (storeId: string) => {
      const response = await fetch(`/api/admin/stores/${storeId}/sync`, {
        method: "POST",
      });
      if (!response.ok) {
        throw new Error("Sync tetiklenemedi");
      }
      return response.json();
    },
    onSuccess: (_, storeId) => {
      queryClient.invalidateQueries({ queryKey: ["admin", "stores"] });
      queryClient.invalidateQueries({ queryKey: ["admin", "stores", storeId] });
      queryClient.invalidateQueries({ queryKey: ["admin", "dashboard", "stats"] });
    },
  });
}

// ============================================
// SANDBOX HOOKS
// ============================================

// Sandbox ürün ekleme için request tipi
export interface CreateSandboxProductRequest {
  // Temel Bilgiler (Zorunlu)
  barcode: string;
  title: string;
  brand: string;
  brandId?: number;
  categoryName: string;
  pimCategoryId?: number;
  // Fiyat & Stok (Zorunlu)
  salePrice: number;
  listPrice?: number;
  vatRate: number;
  quantity: number;
  // Ürün Detayları (Opsiyonel)
  productMainId?: string;
  dimensionalWeight?: number;
  image?: string;
  productUrl?: string;
  stockCode?: string;
  color?: string;
  size?: string;
  gender?: string;
  description?: string;
  // Durum Bilgileri
  approved?: boolean;
  onSale?: boolean;
  archived?: boolean;
  rejected?: boolean;
  blacklisted?: boolean;
  hasActiveCampaign?: boolean;
}

// Sandbox ürünlerini listele
export function useSandboxProducts() {
  return useQuery<TrendyolProduct[]>({
    queryKey: ["admin", "sandbox", "products"],
    queryFn: async () => {
      const response = await fetch("/api/admin/sandbox/products");
      if (!response.ok) {
        throw new Error("Sandbox ürünleri alınamadı");
      }
      return response.json();
    },
  });
}

// Sandbox ürünü ekle
export function useCreateSandboxProduct() {
  const queryClient = useQueryClient();

  return useMutation<TrendyolProduct, Error, CreateSandboxProductRequest>({
    mutationFn: async (data) => {
      const response = await fetch("/api/admin/sandbox/products", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(data),
      });
      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || "Ürün eklenemedi");
      }
      return response.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "sandbox", "products"] });
    },
  });
}

// Sandbox ürününü güncelle
export function useUpdateSandboxProduct() {
  const queryClient = useQueryClient();

  return useMutation<TrendyolProduct, Error, { id: string; data: Partial<CreateSandboxProductRequest> }>({
    mutationFn: async ({ id, data }) => {
      const response = await fetch(`/api/admin/sandbox/products/${id}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(data),
      });
      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || "Ürün güncellenemedi");
      }
      return response.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "sandbox", "products"] });
    },
  });
}

// Sandbox ürününü sil
export function useDeleteSandboxProduct() {
  const queryClient = useQueryClient();

  return useMutation<void, Error, string>({
    mutationFn: async (productId) => {
      const response = await fetch(`/api/admin/sandbox/products/${productId}`, {
        method: "DELETE",
      });
      if (!response.ok) {
        throw new Error("Ürün silinemedi");
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "sandbox", "products"] });
    },
  });
}

// ============================================
// SANDBOX ORDER HOOKS
// ============================================

/**
 * Sandbox sipariş ekleme için request tipi.
 *
 * NOT: Komisyon ve kargo bilgileri SORULMAZ - canlı sistemle aynı algoritma kullanılır:
 * - Komisyon: product.lastCommissionRate → product.commissionRate → 0
 * - Kargo: product.lastShippingCostPerUnit → 0
 * Bu değerler fatura eklendiğinde ürüne yazılır.
 */
export interface CreateSandboxOrderRequest {
  // Ürün bilgisi (zorunlu)
  barcode: string;
  // Satış bilgileri (zorunlu)
  quantity: number;
  unitPrice: number;
  // Sipariş tarihi (opsiyonel - default: bugün)
  orderDate?: string; // ISO date string (YYYY-MM-DD)
  // Sipariş durumu (opsiyonel - default: "Delivered")
  status?: string;
  // Müşteri bilgileri (opsiyonel - test için)
  customerName?: string;
  city?: string;
}

// Sandbox siparişlerini listele
export function useSandboxOrders() {
  return useQuery<TrendyolOrder[]>({
    queryKey: ["admin", "sandbox", "orders"],
    queryFn: async () => {
      const response = await fetch("/api/admin/sandbox/orders");
      if (!response.ok) {
        throw new Error("Sandbox siparişleri alınamadı");
      }
      return response.json();
    },
  });
}

// Sandbox siparişi ekle
export function useCreateSandboxOrder() {
  const queryClient = useQueryClient();

  return useMutation<TrendyolOrder, Error, CreateSandboxOrderRequest>({
    mutationFn: async (data) => {
      const response = await fetch("/api/admin/sandbox/orders", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(data),
      });
      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || "Sipariş eklenemedi");
      }
      return response.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "sandbox", "orders"] });
    },
  });
}

// Sandbox siparişini güncelle
export function useUpdateSandboxOrder() {
  const queryClient = useQueryClient();

  return useMutation<TrendyolOrder, Error, { id: string; data: CreateSandboxOrderRequest }>({
    mutationFn: async ({ id, data }) => {
      const response = await fetch(`/api/admin/sandbox/orders/${id}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(data),
      });
      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || "Sipariş güncellenemedi");
      }
      return response.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "sandbox", "orders"] });
    },
  });
}

// Sandbox siparişini sil
export function useDeleteSandboxOrder() {
  const queryClient = useQueryClient();

  return useMutation<void, Error, string>({
    mutationFn: async (orderId) => {
      const response = await fetch(`/api/admin/sandbox/orders/${orderId}`, {
        method: "DELETE",
      });
      if (!response.ok) {
        throw new Error("Sipariş silinemedi");
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "sandbox", "orders"] });
    },
  });
}

// ============================================
// SANDBOX INVOICE HOOKS
// ============================================

// Sandbox fatura tipi
export interface SandboxInvoice {
  id: string;
  trendyolId: string;
  transactionDate: string;
  transactionType: string;
  description?: string;
  debt: number;
  credit: number;
  invoiceSerialNumber?: string;
  orderNumber?: string;
  createdAt: string;
}

/**
 * Sandbox fatura ekleme için request tipi.
 *
 * Komisyon ve Kargo faturaları için özel alanlar:
 * - barcode: Hangi ürün için fatura (komisyon/kargo faturası)
 * - commissionRate: Komisyon oranı (%) - Komisyon Faturası için
 * - shippingCostPerUnit: Birim kargo maliyeti (TL) - Kargo Fatura için
 *
 * Bu alanlar fatura eklendiğinde ilgili ürünün lastCommissionRate / lastShippingCostPerUnit
 * değerlerini günceller, böylece yeni siparişler bu değerleri referans alır.
 */
export interface CreateSandboxInvoiceRequest {
  // Fatura türü (zorunlu)
  transactionType: string;
  // Tutar (zorunlu) - pozitif = borç, negatif = alacak
  amount: number;
  // Fatura tarihi (opsiyonel - default: bugün)
  transactionDate?: string; // YYYY-MM-DD
  // Açıklama (opsiyonel)
  description?: string;
  // İlişkili sipariş numarası (opsiyonel)
  orderNumber?: string;
  // Fatura seri numarası (opsiyonel)
  invoiceSerialNumber?: string;

  // === Ürün İlişkilendirme (Komisyon/Kargo Faturaları İçin) ===
  // Hangi ürün için fatura (Komisyon Faturası veya Kargo Fatura için zorunlu)
  barcode?: string;
  // Komisyon oranı (%) - Komisyon Faturası için
  commissionRate?: number;
  // Birim kargo maliyeti (TL/adet) - Kargo Fatura için
  shippingCostPerUnit?: number;
}

// Sandbox faturalarını listele
export function useSandboxInvoices() {
  return useQuery<SandboxInvoice[]>({
    queryKey: ["admin", "sandbox", "invoices"],
    queryFn: async () => {
      const response = await fetch("/api/admin/sandbox/invoices");
      if (!response.ok) {
        throw new Error("Sandbox faturaları alınamadı");
      }
      return response.json();
    },
  });
}

// Sandbox faturası ekle
export function useCreateSandboxInvoice() {
  const queryClient = useQueryClient();

  return useMutation<SandboxInvoice, Error, CreateSandboxInvoiceRequest>({
    mutationFn: async (data) => {
      const response = await fetch("/api/admin/sandbox/invoices", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(data),
      });
      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || "Fatura eklenemedi");
      }
      return response.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "sandbox", "invoices"] });
      // Komisyon/Kargo faturası ürün bilgisini güncelleyebilir
      queryClient.invalidateQueries({ queryKey: ["admin", "sandbox", "products"] });
    },
  });
}

// Sandbox faturasını güncelle
export function useUpdateSandboxInvoice() {
  const queryClient = useQueryClient();

  return useMutation<SandboxInvoice, Error, { id: string; data: CreateSandboxInvoiceRequest }>({
    mutationFn: async ({ id, data }) => {
      const response = await fetch(`/api/admin/sandbox/invoices/${id}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(data),
      });
      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || "Fatura güncellenemedi");
      }
      return response.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "sandbox", "invoices"] });
      // Komisyon/Kargo faturası ürün bilgisini güncelleyebilir
      queryClient.invalidateQueries({ queryKey: ["admin", "sandbox", "products"] });
    },
  });
}

// Sandbox faturasını sil
export function useDeleteSandboxInvoice() {
  const queryClient = useQueryClient();

  return useMutation<void, Error, string>({
    mutationFn: async (invoiceId) => {
      const response = await fetch(`/api/admin/sandbox/invoices/${invoiceId}`, {
        method: "DELETE",
      });
      if (!response.ok) {
        throw new Error("Fatura silinemedi");
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "sandbox", "invoices"] });
    },
  });
}

// ============================================
// SANDBOX RETURN HOOKS
// ============================================

// Sandbox iade tipi
export interface SandboxReturn {
  id: string;
  barcode: string;
  productName: string;
  quantity: number;
  productCost: number;
  shippingCostOut: number;
  shippingCostReturn: number;
  commissionLoss: number;
  packagingCost: number;
  totalLoss: number;
  returnReason?: string;
  returnDate: string;
  returnStatus: string;
  commissionRefunded: boolean;
  order?: {
    id: string;
    tyOrderNumber: string;
  };
  createdAt: string;
}

// Sandbox iade ekleme için request tipi
export interface CreateSandboxReturnRequest {
  orderNumber: string; // Zorunlu
  barcode: string; // Zorunlu
  productName?: string;
  quantity: number; // Zorunlu
  productCost?: number;
  shippingCostOut?: number;
  shippingCostReturn?: number;
  commissionLoss?: number;
  packagingCost?: number;
  returnReason?: string;
  returnDate?: string; // YYYY-MM-DD
}

// Sandbox iadelerini listele
export function useSandboxReturns() {
  return useQuery<SandboxReturn[]>({
    queryKey: ["admin", "sandbox", "returns"],
    queryFn: async () => {
      const response = await fetch("/api/admin/sandbox/returns");
      if (!response.ok) {
        throw new Error("Sandbox iadeleri alınamadı");
      }
      return response.json();
    },
  });
}

// Sandbox iadesi ekle
export function useCreateSandboxReturn() {
  const queryClient = useQueryClient();

  return useMutation<SandboxReturn, Error, CreateSandboxReturnRequest>({
    mutationFn: async (data) => {
      const response = await fetch("/api/admin/sandbox/returns", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(data),
      });
      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || "İade eklenemedi");
      }
      return response.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "sandbox", "returns"] });
    },
  });
}

// Sandbox iadesini sil
export function useDeleteSandboxReturn() {
  const queryClient = useQueryClient();

  return useMutation<void, Error, string>({
    mutationFn: async (returnId) => {
      const response = await fetch(`/api/admin/sandbox/returns/${returnId}`, {
        method: "DELETE",
      });
      if (!response.ok) {
        throw new Error("İade silinemedi");
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "sandbox", "returns"] });
    },
  });
}

// ============================================
// SANDBOX SETTLEMENT HOOKS
// ============================================

// Tek siparişi settle et
export function useSettleSandboxOrder() {
  const queryClient = useQueryClient();

  return useMutation<TrendyolOrder, Error, string>({
    mutationFn: async (orderId) => {
      const response = await fetch(`/api/admin/sandbox/orders/${orderId}/settle`, {
        method: "POST",
      });
      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || "Sipariş settle edilemedi");
      }
      return response.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "sandbox", "orders"] });
    },
  });
}

// Tüm siparişleri settle et
export function useSettleAllSandboxOrders() {
  const queryClient = useQueryClient();

  return useMutation<{ settledCount: number }, Error, void>({
    mutationFn: async () => {
      const response = await fetch("/api/admin/sandbox/orders/settle-all", {
        method: "POST",
      });
      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || "Siparişler settle edilemedi");
      }
      return response.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "sandbox", "orders"] });
    },
  });
}
