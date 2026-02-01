package com.ecommerce.sellerx.users;

import lombok.*;

import java.io.Serializable;

/**
 * POJO for storing user preferences as JSONB in the database.
 * Includes language, theme, currency, and notification settings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferences implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * User's preferred language: "tr" (Turkish) or "en" (English)
     */
    @Builder.Default
    private String language = "tr";

    /**
     * UI theme preference: "light", "dark", or "system"
     */
    @Builder.Default
    private String theme = "light";

    /**
     * Display currency for monetary values: "TRY", "USD", or "EUR"
     */
    @Builder.Default
    private String currency = "TRY";

    /**
     * Notification preferences
     */
    @Builder.Default
    private NotificationPreferences notifications = new NotificationPreferences();

    /**
     * Auto-refresh interval in seconds for frontend data polling.
     * Valid values: 0 (disabled), 30, 60, 120, 300
     * Default: 60 seconds (1 minute)
     */
    @Builder.Default
    private Integer syncInterval = 60;

    /**
     * Nested class for notification settings
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationPreferences implements Serializable {

        private static final long serialVersionUID = 1L;

        @Builder.Default
        private boolean email = true;

        @Builder.Default
        private boolean push = true;

        @Builder.Default
        private boolean orderUpdates = true;

        @Builder.Default
        private boolean stockAlerts = true;

        @Builder.Default
        private boolean weeklyReport = false;
    }
}
