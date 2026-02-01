export type NotificationType =
  | 'VIDEO_ADDED'
  | 'ORDER_UPDATE'
  | 'STOCK_ALERT'
  | 'SYSTEM'
  | 'SUCCESS'
  | 'WARNING';

export interface Notification {
  id: string;
  type: NotificationType;
  title: string;
  message: string;
  link?: string;
  read: boolean;
  createdAt: string;
}
