package com.ecommerce.sellerx.users;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating user preferences.
 * All fields are optional - only provided fields will be updated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePreferencesRequest {

    /**
     * Language preference: "tr" or "en"
     */
    private String language;

    /**
     * Theme preference: "light", "dark", or "system"
     */
    private String theme;

    /**
     * Currency preference: "TRY", "USD", or "EUR"
     */
    private String currency;

    /**
     * Notification preferences (partial update)
     */
    private NotificationUpdate notifications;

    /**
     * Auto-refresh interval in seconds for frontend data polling.
     * Valid values: 0 (disabled), 30, 60, 120, 300
     */
    private Integer syncInterval;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationUpdate {
        private Boolean email;
        private Boolean push;
        private Boolean orderUpdates;
        private Boolean stockAlerts;
        private Boolean weeklyReport;
    }
}
