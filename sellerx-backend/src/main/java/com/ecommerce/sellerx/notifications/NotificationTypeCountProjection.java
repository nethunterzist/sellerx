package com.ecommerce.sellerx.notifications;

/**
 * Projection for notification type count grouping.
 */
public interface NotificationTypeCountProjection {
    NotificationType getNotificationType();
    Long getTypeCount();
}
