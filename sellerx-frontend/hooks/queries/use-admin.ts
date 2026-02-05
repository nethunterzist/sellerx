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
