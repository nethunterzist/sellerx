package com.ecommerce.sellerx.email;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Email templates for different notification types.
 * Each template has a default subject and a template identifier.
 */
@Getter
@RequiredArgsConstructor
public enum EmailTemplate {

    // Support ticket templates
    TICKET_CREATED("Destek Talebiniz Alındı - #{ticketNumber}", "ticket-created"),
    TICKET_REPLY("Destek Talebinize Yanıt - #{ticketNumber}", "ticket-reply"),
    TICKET_CLOSED("Destek Talebiniz Kapatıldı - #{ticketNumber}", "ticket-closed"),
    TICKET_ASSIGNED("Destek Talebi Size Atandı - #{ticketNumber}", "ticket-assigned"),

    // User account templates
    WELCOME("SellerX'e Hoş Geldiniz!", "welcome"),
    PASSWORD_RESET("Şifre Sıfırlama Talebi", "password-reset"),
    EMAIL_VERIFICATION("E-posta Adresinizi Doğrulayın", "email-verification"),

    // Store and order templates
    STORE_CONNECTED("Mağazanız Başarıyla Bağlandı", "store-connected"),
    ORDER_ALERT("Yeni Sipariş Bildirimi", "order-alert"),
    LOW_STOCK_ALERT("Düşük Stok Uyarısı", "low-stock-alert"),

    // Admin notifications
    ADMIN_NEW_USER("Yeni Kullanıcı Kaydı", "admin-new-user"),
    ADMIN_NEW_TICKET("Yeni Destek Talebi", "admin-new-ticket");

    /**
     * Default subject line for the email.
     * Can contain placeholders like #{ticketNumber} that will be replaced.
     */
    private final String defaultSubject;

    /**
     * Template identifier for future HTML template support.
     */
    private final String templateId;

    /**
     * Builds the subject line by replacing placeholders with actual values.
     *
     * @param ticketNumber The ticket number to insert (if applicable)
     * @return The formatted subject line
     */
    public String buildSubject(String ticketNumber) {
        return defaultSubject.replace("#{ticketNumber}", ticketNumber != null ? ticketNumber : "");
    }
}
