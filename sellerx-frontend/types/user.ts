// User Types

export type UserRole = "USER" | "ADMIN";

export interface User {
  id: string;
  email: string;
  name: string;
  phone?: string;
  company?: string;
  role?: UserRole;
  createdAt?: string;
  updatedAt?: string;
}

export interface UpdateProfileRequest {
  name?: string;
  phone?: string;
  company?: string;
}

export interface UpdateProfileResponse {
  id: string;
  email: string;
  name: string;
  phone?: string;
  company?: string;
  message?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface ChangePasswordResponse {
  success: boolean;
  message: string;
}

export type SyncInterval = 0 | 30 | 60 | 120 | 300;

export interface UserPreferences {
  language: "tr" | "en";
  theme: "light" | "dark" | "system";
  currency: "TRY" | "USD" | "EUR";
  notifications: {
    email: boolean;
    push: boolean;
    orderUpdates: boolean;
    stockAlerts: boolean;
    weeklyReport: boolean;
  };
  /** Auto-refresh interval in seconds. 0 = disabled */
  syncInterval: SyncInterval;
}

export interface UpdatePreferencesRequest {
  language?: "tr" | "en";
  theme?: "light" | "dark" | "system";
  currency?: "TRY" | "USD" | "EUR";
  notifications?: Partial<UserPreferences["notifications"]>;
  /** Auto-refresh interval in seconds. 0 = disabled */
  syncInterval?: SyncInterval;
}
