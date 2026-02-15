// Alert Types

export type AlertType = 'STOCK' | 'PROFIT' | 'PRICE' | 'ORDER' | 'RETURN' | 'SYSTEM';

export type AlertConditionType = 'BELOW' | 'ABOVE' | 'EQUALS' | 'CHANGED' | 'ZERO';

export type AlertSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type AlertStatus = 'INFO' | 'PENDING_APPROVAL' | 'APPROVED' | 'DISMISSED';

// Alert Rule interfaces
export interface AlertRule {
  id: string;
  storeId?: string;
  storeName?: string;
  name: string;
  alertType: AlertType;
  conditionType: AlertConditionType;
  threshold?: number;
  productBarcode?: string;
  categoryName?: string;
  emailEnabled: boolean;
  pushEnabled: boolean;
  inAppEnabled: boolean;
  active: boolean;
  cooldownMinutes: number;
  lastTriggeredAt?: string;
  triggerCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAlertRuleRequest {
  storeId?: string;
  name: string;
  alertType: AlertType;
  conditionType: AlertConditionType;
  threshold?: number;
  productBarcode?: string;
  categoryName?: string;
  emailEnabled?: boolean;
  pushEnabled?: boolean;
  inAppEnabled?: boolean;
  cooldownMinutes?: number;
}

export interface UpdateAlertRuleRequest {
  storeId?: string;
  name?: string;
  alertType?: AlertType;
  conditionType?: AlertConditionType;
  threshold?: number;
  productBarcode?: string;
  categoryName?: string;
  emailEnabled?: boolean;
  pushEnabled?: boolean;
  inAppEnabled?: boolean;
  active?: boolean;
  cooldownMinutes?: number;
}

// Alert History interfaces
export interface AlertHistory {
  id: string;
  ruleId?: string;
  ruleName?: string;
  storeId?: string;
  storeName?: string;
  alertType: AlertType;
  title: string;
  message?: string;
  severity: AlertSeverity;
  data?: Record<string, unknown>;
  emailSent: boolean;
  pushSent: boolean;
  inAppSent: boolean;
  status?: AlertStatus;
  read: boolean;
  readAt?: string;
  createdAt: string;
}

export interface AlertStats {
  unreadCount: number;
  stockAlertsLast24h: number;
  profitAlertsLast24h: number;
  orderAlertsLast24h: number;
  totalAlertsLast7Days: number;
}

export interface AlertRuleCounts {
  total: number;
  active: number;
  inactive: number;
}

// Paginated response
export interface PaginatedAlerts {
  content: AlertHistory[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

// Helper constants for UI
export const ALERT_TYPE_LABELS: Record<AlertType, string> = {
  STOCK: 'Stok Uyarısı',
  PROFIT: 'Kar Uyarısı',
  PRICE: 'Fiyat Uyarısı',
  ORDER: 'Sipariş Uyarısı',
  RETURN: 'İade Uyarısı',
  SYSTEM: 'Sistem Uyarısı',
};

export const ALERT_CONDITION_LABELS: Record<AlertConditionType, string> = {
  BELOW: 'Altına düşerse',
  ABOVE: 'Üstüne çıkarsa',
  EQUALS: 'Eşit olursa',
  CHANGED: 'Değişirse',
  ZERO: 'Sıfır olursa',
};

export const ALERT_SEVERITY_LABELS: Record<AlertSeverity, string> = {
  LOW: 'Düşük',
  MEDIUM: 'Orta',
  HIGH: 'Yüksek',
  CRITICAL: 'Kritik',
};

export const ALERT_SEVERITY_COLORS: Record<AlertSeverity, string> = {
  LOW: 'text-gray-500',
  MEDIUM: 'text-yellow-500',
  HIGH: 'text-orange-500',
  CRITICAL: 'text-red-500',
};

export const ALERT_TYPE_ICONS: Record<AlertType, string> = {
  STOCK: '📦',
  PROFIT: '💰',
  PRICE: '🏷️',
  ORDER: '🛒',
  RETURN: '↩️',
  SYSTEM: '⚙️',
};
