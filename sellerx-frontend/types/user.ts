// User Types

export interface User {
  id: string;
  email: string;
  name: string;
  phone?: string;
  company?: string;
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
}

export interface UpdatePreferencesRequest {
  language?: "tr" | "en";
  theme?: "light" | "dark" | "system";
  currency?: "TRY" | "USD" | "EUR";
  notifications?: Partial<UserPreferences["notifications"]>;
}
