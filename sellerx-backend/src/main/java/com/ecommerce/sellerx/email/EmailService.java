package com.ecommerce.sellerx.email;

import java.util.Map;

/**
 * Interface for email sending operations.
 * Provides methods for sending emails using various approaches.
 */
public interface EmailService {

    /**
     * Sends an email with the specified parameters.
     *
     * @param to       Recipient email address
     * @param subject  Email subject line
     * @param htmlBody HTML content of the email
     */
    void sendEmail(String to, String subject, String htmlBody);

    /**
     * Sends an email using a predefined template.
     *
     * @param to        Recipient email address
     * @param template  Email template to use
     * @param variables Variables to inject into the template
     */
    void sendTemplateEmail(String to, EmailTemplate template, Map<String, Object> variables);

    /**
     * Sends an email from an EmailRequest object.
     *
     * @param request The email request containing all details
     */
    void sendEmail(EmailRequest request);

    // === Support Ticket Specific Methods ===

    /**
     * Sends notification when a support ticket is created.
     *
     * @param to           Recipient email address
     * @param ticketNumber Unique ticket identifier
     * @param subject      Ticket subject
     */
    void sendTicketCreatedEmail(String to, String ticketNumber, String subject);

    /**
     * Sends notification when a support ticket receives a reply.
     *
     * @param to           Recipient email address
     * @param ticketNumber Unique ticket identifier
     * @param senderName   Name of the person who replied
     * @param replyPreview Short preview of the reply content
     */
    void sendTicketReplyEmail(String to, String ticketNumber, String senderName, String replyPreview);

    /**
     * Sends notification when a support ticket is closed.
     *
     * @param to           Recipient email address
     * @param ticketNumber Unique ticket identifier
     */
    void sendTicketClosedEmail(String to, String ticketNumber);

    /**
     * Sends notification to admin when a new ticket is assigned.
     *
     * @param adminEmail   Admin's email address
     * @param ticketNumber Unique ticket identifier
     * @param subject      Ticket subject
     * @param userName     Name of the user who created the ticket
     */
    void sendTicketAssignedEmail(String adminEmail, String ticketNumber, String subject, String userName);

    // === Admin Notification Methods ===

    /**
     * Sends notification to admins about a new support ticket.
     *
     * @param adminEmail   Admin's email address
     * @param ticketNumber Unique ticket identifier
     * @param userName     Name of the user who created the ticket
     * @param subject      Ticket subject
     */
    void sendAdminNewTicketEmail(String adminEmail, String ticketNumber, String userName, String subject);

    // === Alert Notification Methods ===

    /**
     * Sends a stock alert email notification.
     *
     * @param to           Recipient email address
     * @param productName  Name of the product
     * @param barcode      Product barcode
     * @param currentStock Current stock level
     * @param threshold    Alert threshold value
     * @param storeName    Store name
     * @param severity     Alert severity (LOW, MEDIUM, HIGH, CRITICAL)
     */
    void sendStockAlertEmail(String to, String productName, String barcode, int currentStock,
                             int threshold, String storeName, String severity);

    /**
     * Sends a profit margin alert email notification.
     *
     * @param to            Recipient email address
     * @param productName   Name of the product
     * @param barcode       Product barcode
     * @param currentMargin Current profit margin percentage
     * @param threshold     Alert threshold percentage
     * @param storeName     Store name
     * @param severity      Alert severity (LOW, MEDIUM, HIGH, CRITICAL)
     */
    void sendProfitAlertEmail(String to, String productName, String barcode, double currentMargin,
                              double threshold, String storeName, String severity);

    /**
     * Sends a generic alert email notification.
     *
     * @param to          Recipient email address
     * @param alertTitle  Alert title
     * @param alertMessage Alert message/description
     * @param storeName   Store name (can be null)
     * @param severity    Alert severity (LOW, MEDIUM, HIGH, CRITICAL)
     * @param alertType   Alert type (STOCK, PROFIT, PRICE, ORDER, SYSTEM)
     */
    void sendAlertEmail(String to, String alertTitle, String alertMessage,
                        String storeName, String severity, String alertType);

    /**
     * Sends a daily/weekly alert digest email.
     *
     * @param to             Recipient email address
     * @param digestPeriod   Period description (e.g., "Günlük", "Haftalık")
     * @param alertSummaries List of alert summaries for the period
     * @param totalAlerts    Total number of alerts in period
     */
    void sendAlertDigestEmail(String to, String digestPeriod,
                              java.util.List<String> alertSummaries, int totalAlerts);
}
