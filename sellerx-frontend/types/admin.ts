// Admin types

export interface AdminUser {
  id: number;
  email: string;
  name: string;
  role: "USER" | "ADMIN";
  storeCount: number;
  stores: AdminUserStore[];
  activeSubscription: boolean;
  subscriptionPlan: string | null;
  createdAt: string;
  lastLoginAt: string | null;
}

export interface AdminUserStore {
  id: string;
  storeName: string;
  marketplace: string;
  syncStatus: string;
  productCount: number;
  orderCount: number;
}

export interface AdminUserListItem {
  id: number;
  email: string;
  name: string;
  role: "USER" | "ADMIN";
  storeCount: number;
  createdAt: string;
  lastLoginAt: string | null;
}

export interface AdminStore {
  id: string;
  storeName: string;
  marketplace: string;
  userId: number;
  userEmail: string;
  userName: string;
  syncStatus: string;
  syncErrorMessage: string | null;
  initialSyncCompleted: boolean;
  overallSyncStatus: string;
  syncPhases: Record<string, SyncPhaseInfo>;
  webhookId: string | null;
  webhookStatus: string | null;
  webhookErrorMessage: string | null;
  historicalSyncStatus: string | null;
  historicalSyncDate: string | null;
  historicalSyncTotalChunks: number | null;
  historicalSyncCompletedChunks: number | null;
  productCount: number;
  orderCount: number;
  createdAt: string;
  updatedAt: string;
  sellerId: string | null;
  hasApiKey: boolean;
  hasApiSecret: boolean;
}

export interface SyncPhaseInfo {
  status: string;
  startedAt: string | null;
  completedAt: string | null;
  errorMessage: string | null;
}

export interface AdminStoreListItem {
  id: string;
  storeName: string;
  marketplace: string;
  userId: number;
  userEmail: string;
  syncStatus: string;
  initialSyncCompleted: boolean;
  webhookStatus: string | null;
  productCount: number;
  orderCount: number;
  createdAt: string;
  lastSyncAt: string;
}

export interface AdminDashboardStats {
  totalUsers: number;
  newUsersToday: number;
  newUsersThisWeek: number;
  newUsersThisMonth: number;
  activeUsersLast30Days: number;
  totalStores: number;
  activeStores: number;
  storesWithSyncErrors: number;
  storesWithWebhookErrors: number;
  pendingSyncs: number;
  activeSyncs: number;
  completedSyncsToday: number;
  failedSyncsToday: number;
  totalOrders: number;
  ordersToday: number;
  ordersThisWeek: number;
  ordersThisMonth: number;
  mrr: number | null;
  activeSubscriptions: number;
  trialUsers: number;
  recentActivities: RecentActivity[];
}

export interface RecentActivity {
  id: number;
  type: string;
  message: string;
  userId: number | null;
  userEmail: string | null;
  storeId: string | null;
  storeName: string | null;
  createdAt: string;
}

export interface ChangeRoleRequest {
  role: "USER" | "ADMIN";
}

// Paginated response type
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}
