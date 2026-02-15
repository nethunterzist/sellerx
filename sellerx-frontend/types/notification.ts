export type NotificationType =
  | 'VIDEO_ADDED'
  | 'ORDER_UPDATE'
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
