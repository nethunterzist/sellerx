package com.ecommerce.sellerx.email;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * SendGrid implementation of the EmailService interface.
 * Handles email sending through SendGrid's API with async support.
 */
@Service
@Primary
@Slf4j
@RequiredArgsConstructor
public class SendGridEmailService implements EmailService {

    private final EmailConfig emailConfig;

    @Override
    @Async("taskExecutor")
    public void sendEmail(String to, String subject, String htmlBody) {
        if (!emailConfig.isEnabled()) {
            log.info("[EMAIL DISABLED] Would send to: {}, subject: {}", to, subject);
            return;
        }

        if (emailConfig.getApiKey() == null || emailConfig.getApiKey().isBlank()) {
            log.warn("[EMAIL] SendGrid API key is not configured. Skipping email to: {}", to);
            return;
        }

        try {
            Email from = new Email(emailConfig.getFromEmail(), emailConfig.getFromName());
            Email toEmail = new Email(to);
            Content content = new Content("text/html", htmlBody);
            Mail mail = new Mail(from, subject, toEmail, content);

            SendGrid sg = new SendGrid(emailConfig.getApiKey());
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("[EMAIL] Successfully sent email to: {}, subject: {}, status: {}",
                        to, subject, response.getStatusCode());
            } else {
                log.error("[EMAIL] Failed to send email to: {}, status: {}, body: {}",
                        to, response.getStatusCode(), response.getBody());
            }
        } catch (IOException e) {
            log.error("[EMAIL] Exception while sending email to: {}, error: {}", to, e.getMessage(), e);
        }
    }

    @Override
    @Async("taskExecutor")
    public void sendTemplateEmail(String to, EmailTemplate template, Map<String, Object> variables) {
        String ticketNumber = variables != null ? (String) variables.get("ticketNumber") : null;
        String subject = template.buildSubject(ticketNumber);
        String htmlBody = buildHtmlFromTemplate(template, variables);
        sendEmail(to, subject, htmlBody);
    }

    @Override
    @Async("taskExecutor")
    public void sendEmail(EmailRequest request) {
        if (request.getTemplate() != null) {
            sendTemplateEmail(request.getTo(), request.getTemplate(), request.getTemplateVariables());
        } else {
            String body = request.getHtmlBody() != null ? request.getHtmlBody() : request.getTextBody();
            sendEmail(request.getTo(), request.getSubject(), body);
        }
    }

    // === Support Ticket Email Methods ===

    @Override
    @Async("taskExecutor")
    public void sendTicketCreatedEmail(String to, String ticketNumber, String subject) {
        String emailSubject = String.format("Destek Talebiniz Alındı - #%s", ticketNumber);
        String htmlBody = buildTicketCreatedHtml(ticketNumber, subject);
        sendEmail(to, emailSubject, htmlBody);
    }

    @Override
    @Async("taskExecutor")
    public void sendTicketReplyEmail(String to, String ticketNumber, String senderName, String replyPreview) {
        String emailSubject = String.format("Destek Talebinize Yanıt - #%s", ticketNumber);
        String htmlBody = buildTicketReplyHtml(ticketNumber, senderName, replyPreview);
        sendEmail(to, emailSubject, htmlBody);
    }

    @Override
    @Async("taskExecutor")
    public void sendTicketClosedEmail(String to, String ticketNumber) {
        String emailSubject = String.format("Destek Talebiniz Kapatıldı - #%s", ticketNumber);
        String htmlBody = buildTicketClosedHtml(ticketNumber);
        sendEmail(to, emailSubject, htmlBody);
    }

    @Override
    @Async("taskExecutor")
    public void sendTicketAssignedEmail(String adminEmail, String ticketNumber, String subject, String userName) {
        String emailSubject = String.format("Destek Talebi Size Atandı - #%s", ticketNumber);
        String htmlBody = buildTicketAssignedHtml(ticketNumber, subject, userName);
        sendEmail(adminEmail, emailSubject, htmlBody);
    }

    @Override
    @Async("taskExecutor")
    public void sendAdminNewTicketEmail(String adminEmail, String ticketNumber, String userName, String subject) {
        String emailSubject = String.format("Yeni Destek Talebi - #%s", ticketNumber);
        String htmlBody = buildAdminNewTicketHtml(ticketNumber, userName, subject);
        sendEmail(adminEmail, emailSubject, htmlBody);
    }

    // === Alert Email Methods ===

    @Override
    @Async("taskExecutor")
    public void sendStockAlertEmail(String to, String productName, String barcode, int currentStock,
                                    int threshold, String storeName, String severity) {
        String subject = String.format("⚠️ Stok Uyarısı: %s", productName);
        String htmlBody = buildStockAlertHtml(productName, barcode, currentStock, threshold, storeName, severity);
        sendEmail(to, subject, htmlBody);
    }

    @Override
    @Async("taskExecutor")
    public void sendProfitAlertEmail(String to, String productName, String barcode, double currentMargin,
                                     double threshold, String storeName, String severity) {
        String subject = String.format("📉 Kar Marjı Uyarısı: %s", productName);
        String htmlBody = buildProfitAlertHtml(productName, barcode, currentMargin, threshold, storeName, severity);
        sendEmail(to, subject, htmlBody);
    }

    @Override
    @Async("taskExecutor")
    public void sendAlertEmail(String to, String alertTitle, String alertMessage,
                               String storeName, String severity, String alertType) {
        String emoji = getAlertTypeEmoji(alertType);
        String subject = String.format("%s %s", emoji, alertTitle);
        String htmlBody = buildGenericAlertHtml(alertTitle, alertMessage, storeName, severity, alertType);
        sendEmail(to, subject, htmlBody);
    }

    @Override
    @Async("taskExecutor")
    public void sendAlertDigestEmail(String to, String digestPeriod,
                                     List<String> alertSummaries, int totalAlerts) {
        String subject = String.format("📊 %s Uyarı Özeti - %d Uyarı", digestPeriod, totalAlerts);
        String htmlBody = buildAlertDigestHtml(digestPeriod, alertSummaries, totalAlerts);
        sendEmail(to, subject, htmlBody);
    }

    private String getAlertTypeEmoji(String alertType) {
        return switch (alertType) {
            case "STOCK" -> "📦";
            case "PROFIT" -> "💰";
            case "PRICE" -> "🏷️";
            case "ORDER" -> "🛒";
            case "SYSTEM" -> "⚙️";
            default -> "⚠️";
        };
    }

    // === HTML Template Builders ===

    private String buildHtmlFromTemplate(EmailTemplate template, Map<String, Object> variables) {
        // For now, use simple HTML. In the future, this can be replaced with Thymeleaf templates.
        String ticketNumber = variables != null ? (String) variables.getOrDefault("ticketNumber", "") : "";
        String subject = variables != null ? (String) variables.getOrDefault("subject", "") : "";
        String senderName = variables != null ? (String) variables.getOrDefault("senderName", "") : "";

        return switch (template) {
            case TICKET_CREATED -> buildTicketCreatedHtml(ticketNumber, subject);
            case TICKET_REPLY -> buildTicketReplyHtml(ticketNumber, senderName, "");
            case TICKET_CLOSED -> buildTicketClosedHtml(ticketNumber);
            default -> buildGenericHtml(template.getDefaultSubject(), "");
        };
    }

    private String buildTicketCreatedHtml(String ticketNumber, String subject) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #f59e0b; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background-color: #f9fafb; padding: 20px; border-radius: 0 0 8px 8px; }
                    .ticket-number { font-size: 24px; font-weight: bold; color: #f59e0b; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>SellerX Destek</h1>
                    </div>
                    <div class="content">
                        <h2>Destek Talebiniz Alındı</h2>
                        <p>Merhaba,</p>
                        <p>Destek talebiniz başarıyla oluşturuldu. En kısa sürede size dönüş yapacağız.</p>
                        <p><strong>Talep Numaranız:</strong> <span class="ticket-number">#%s</span></p>
                        <p><strong>Konu:</strong> %s</p>
                        <p>Talebinizin durumunu SellerX panelinden takip edebilirsiniz.</p>
                        <p>Teşekkürler,<br>SellerX Destek Ekibi</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2024 SellerX. Tüm hakları saklıdır.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(ticketNumber, subject);
    }

    private String buildTicketReplyHtml(String ticketNumber, String senderName, String replyPreview) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #f59e0b; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background-color: #f9fafb; padding: 20px; border-radius: 0 0 8px 8px; }
                    .ticket-number { font-size: 24px; font-weight: bold; color: #f59e0b; }
                    .reply-preview { background-color: #fff; padding: 15px; border-left: 4px solid #f59e0b; margin: 15px 0; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>SellerX Destek</h1>
                    </div>
                    <div class="content">
                        <h2>Destek Talebinize Yanıt</h2>
                        <p>Merhaba,</p>
                        <p><strong>%s</strong> destek talebinize yanıt verdi.</p>
                        <p><strong>Talep Numarası:</strong> <span class="ticket-number">#%s</span></p>
                        %s
                        <p>Yanıtı görmek ve cevap vermek için SellerX panelini ziyaret edin.</p>
                        <p>Teşekkürler,<br>SellerX Destek Ekibi</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2024 SellerX. Tüm hakları saklıdır.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                senderName,
                ticketNumber,
                replyPreview != null && !replyPreview.isBlank()
                    ? "<div class=\"reply-preview\"><p>" + truncate(replyPreview, 200) + "</p></div>"
                    : ""
            );
    }

    private String buildTicketClosedHtml(String ticketNumber) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #10b981; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background-color: #f9fafb; padding: 20px; border-radius: 0 0 8px 8px; }
                    .ticket-number { font-size: 24px; font-weight: bold; color: #10b981; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>SellerX Destek</h1>
                    </div>
                    <div class="content">
                        <h2>Destek Talebiniz Kapatıldı</h2>
                        <p>Merhaba,</p>
                        <p>Destek talebiniz çözüldü ve kapatıldı.</p>
                        <p><strong>Talep Numarası:</strong> <span class="ticket-number">#%s</span></p>
                        <p>Eğer hala yardıma ihtiyacınız varsa, yeni bir destek talebi oluşturabilirsiniz.</p>
                        <p>Teşekkürler,<br>SellerX Destek Ekibi</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2024 SellerX. Tüm hakları saklıdır.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(ticketNumber);
    }

    private String buildTicketAssignedHtml(String ticketNumber, String subject, String userName) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #3b82f6; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background-color: #f9fafb; padding: 20px; border-radius: 0 0 8px 8px; }
                    .ticket-number { font-size: 24px; font-weight: bold; color: #3b82f6; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>SellerX Admin</h1>
                    </div>
                    <div class="content">
                        <h2>Yeni Atama</h2>
                        <p>Merhaba,</p>
                        <p>Bir destek talebi size atandı.</p>
                        <p><strong>Talep Numarası:</strong> <span class="ticket-number">#%s</span></p>
                        <p><strong>Konu:</strong> %s</p>
                        <p><strong>Kullanıcı:</strong> %s</p>
                        <p>Admin panelinden talebi inceleyip yanıt verebilirsiniz.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2024 SellerX. Tüm hakları saklıdır.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(ticketNumber, subject, userName);
    }

    private String buildAdminNewTicketHtml(String ticketNumber, String userName, String subject) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #ef4444; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background-color: #f9fafb; padding: 20px; border-radius: 0 0 8px 8px; }
                    .ticket-number { font-size: 24px; font-weight: bold; color: #ef4444; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>SellerX Admin</h1>
                    </div>
                    <div class="content">
                        <h2>Yeni Destek Talebi</h2>
                        <p>Yeni bir destek talebi oluşturuldu.</p>
                        <p><strong>Talep Numarası:</strong> <span class="ticket-number">#%s</span></p>
                        <p><strong>Kullanıcı:</strong> %s</p>
                        <p><strong>Konu:</strong> %s</p>
                        <p>Admin panelinden talebi inceleyebilirsiniz.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2024 SellerX. Tüm hakları saklıdır.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(ticketNumber, userName, subject);
    }

    private String buildGenericHtml(String title, String content) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #f59e0b; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background-color: #f9fafb; padding: 20px; border-radius: 0 0 8px 8px; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>SellerX</h1>
                    </div>
                    <div class="content">
                        <h2>%s</h2>
                        %s
                        <p>Teşekkürler,<br>SellerX Ekibi</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2024 SellerX. Tüm hakları saklıdır.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(title, content);
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    // === Alert HTML Template Builders ===

    private String getSeverityColor(String severity) {
        return switch (severity) {
            case "CRITICAL" -> "#ef4444"; // red
            case "HIGH" -> "#f97316"; // orange
            case "MEDIUM" -> "#eab308"; // yellow
            case "LOW" -> "#6b7280"; // gray
            default -> "#3b82f6"; // blue
        };
    }

    private String getSeverityLabel(String severity) {
        return switch (severity) {
            case "CRITICAL" -> "Kritik";
            case "HIGH" -> "Yüksek";
            case "MEDIUM" -> "Orta";
            case "LOW" -> "Düşük";
            default -> severity;
        };
    }

    private String buildStockAlertHtml(String productName, String barcode, int currentStock,
                                       int threshold, String storeName, String severity) {
        String severityColor = getSeverityColor(severity);
        String severityLabel = getSeverityLabel(severity);

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: %s; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background-color: #f9fafb; padding: 20px; border-radius: 0 0 8px 8px; }
                    .alert-badge { display: inline-block; background-color: %s; color: white; padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: bold; }
                    .info-box { background-color: white; border: 1px solid #e5e7eb; border-radius: 8px; padding: 15px; margin: 15px 0; }
                    .info-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #f3f4f6; }
                    .info-row:last-child { border-bottom: none; }
                    .info-label { color: #6b7280; font-size: 14px; }
                    .info-value { font-weight: 600; color: #111827; }
                    .stock-critical { color: #ef4444; font-size: 24px; font-weight: bold; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                    .cta-button { display: inline-block; background-color: #3b82f6; color: white; padding: 12px 24px; border-radius: 6px; text-decoration: none; font-weight: 600; margin-top: 15px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>📦 Stok Uyarısı</h1>
                    </div>
                    <div class="content">
                        <p style="text-align: center;"><span class="alert-badge">%s Öncelik</span></p>

                        <h2 style="margin-top: 20px;">%s</h2>

                        <div class="info-box">
                            <div class="info-row">
                                <span class="info-label">Barkod:</span>
                                <span class="info-value">%s</span>
                            </div>
                            <div class="info-row">
                                <span class="info-label">Mağaza:</span>
                                <span class="info-value">%s</span>
                            </div>
                            <div class="info-row">
                                <span class="info-label">Mevcut Stok:</span>
                                <span class="stock-critical">%d</span>
                            </div>
                            <div class="info-row">
                                <span class="info-label">Uyarı Eşiği:</span>
                                <span class="info-value">%d</span>
                            </div>
                        </div>

                        <p style="text-align: center;">
                            <strong>Ürününüzün stoğu belirlediğiniz eşik değerinin altına düştü.</strong>
                        </p>

                        <p style="text-align: center;">
                            <a href="#" class="cta-button">SellerX'e Git</a>
                        </p>
                    </div>
                    <div class="footer">
                        <p>Bu e-posta SellerX uyarı sistemi tarafından otomatik gönderilmiştir.</p>
                        <p>&copy; 2024 SellerX. Tüm hakları saklıdır.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(severityColor, severityColor, severityLabel, productName, barcode, storeName, currentStock, threshold);
    }

    private String buildProfitAlertHtml(String productName, String barcode, double currentMargin,
                                        double threshold, String storeName, String severity) {
        String severityColor = getSeverityColor(severity);
        String severityLabel = getSeverityLabel(severity);
        String marginFormatted = String.format("%.2f%%", currentMargin);
        String thresholdFormatted = String.format("%.2f%%", threshold);

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: %s; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background-color: #f9fafb; padding: 20px; border-radius: 0 0 8px 8px; }
                    .alert-badge { display: inline-block; background-color: %s; color: white; padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: bold; }
                    .info-box { background-color: white; border: 1px solid #e5e7eb; border-radius: 8px; padding: 15px; margin: 15px 0; }
                    .info-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #f3f4f6; }
                    .info-row:last-child { border-bottom: none; }
                    .info-label { color: #6b7280; font-size: 14px; }
                    .info-value { font-weight: 600; color: #111827; }
                    .margin-critical { color: #ef4444; font-size: 24px; font-weight: bold; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                    .cta-button { display: inline-block; background-color: #3b82f6; color: white; padding: 12px 24px; border-radius: 6px; text-decoration: none; font-weight: 600; margin-top: 15px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>💰 Kar Marjı Uyarısı</h1>
                    </div>
                    <div class="content">
                        <p style="text-align: center;"><span class="alert-badge">%s Öncelik</span></p>

                        <h2 style="margin-top: 20px;">%s</h2>

                        <div class="info-box">
                            <div class="info-row">
                                <span class="info-label">Barkod:</span>
                                <span class="info-value">%s</span>
                            </div>
                            <div class="info-row">
                                <span class="info-label">Mağaza:</span>
                                <span class="info-value">%s</span>
                            </div>
                            <div class="info-row">
                                <span class="info-label">Mevcut Kar Marjı:</span>
                                <span class="margin-critical">%s</span>
                            </div>
                            <div class="info-row">
                                <span class="info-label">Uyarı Eşiği:</span>
                                <span class="info-value">%s</span>
                            </div>
                        </div>

                        <p style="text-align: center;">
                            <strong>Ürününüzün kar marjı belirlediğiniz eşik değerinin altına düştü.</strong>
                        </p>

                        <p style="text-align: center;">
                            <a href="#" class="cta-button">SellerX'e Git</a>
                        </p>
                    </div>
                    <div class="footer">
                        <p>Bu e-posta SellerX uyarı sistemi tarafından otomatik gönderilmiştir.</p>
                        <p>&copy; 2024 SellerX. Tüm hakları saklıdır.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(severityColor, severityColor, severityLabel, productName, barcode, storeName, marginFormatted, thresholdFormatted);
    }

    private String buildGenericAlertHtml(String alertTitle, String alertMessage,
                                         String storeName, String severity, String alertType) {
        String severityColor = getSeverityColor(severity);
        String severityLabel = getSeverityLabel(severity);
        String emoji = getAlertTypeEmoji(alertType);
        String alertTypeLabel = switch (alertType) {
            case "STOCK" -> "Stok Uyarısı";
            case "PROFIT" -> "Kar Marjı Uyarısı";
            case "PRICE" -> "Fiyat Uyarısı";
            case "ORDER" -> "Sipariş Uyarısı";
            case "SYSTEM" -> "Sistem Uyarısı";
            default -> "Uyarı";
        };

        String storeSection = storeName != null && !storeName.isBlank()
            ? "<div class=\"info-row\"><span class=\"info-label\">Mağaza:</span><span class=\"info-value\">" + storeName + "</span></div>"
            : "";

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: %s; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background-color: #f9fafb; padding: 20px; border-radius: 0 0 8px 8px; }
                    .alert-badge { display: inline-block; background-color: %s; color: white; padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: bold; }
                    .info-box { background-color: white; border: 1px solid #e5e7eb; border-radius: 8px; padding: 15px; margin: 15px 0; }
                    .info-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #f3f4f6; }
                    .info-row:last-child { border-bottom: none; }
                    .info-label { color: #6b7280; font-size: 14px; }
                    .info-value { font-weight: 600; color: #111827; }
                    .message-box { background-color: white; border-left: 4px solid %s; padding: 15px; margin: 15px 0; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                    .cta-button { display: inline-block; background-color: #3b82f6; color: white; padding: 12px 24px; border-radius: 6px; text-decoration: none; font-weight: 600; margin-top: 15px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>%s %s</h1>
                    </div>
                    <div class="content">
                        <p style="text-align: center;"><span class="alert-badge">%s Öncelik</span></p>

                        <h2 style="margin-top: 20px;">%s</h2>

                        <div class="info-box">
                            <div class="info-row">
                                <span class="info-label">Uyarı Türü:</span>
                                <span class="info-value">%s</span>
                            </div>
                            %s
                        </div>

                        <div class="message-box">
                            <p>%s</p>
                        </div>

                        <p style="text-align: center;">
                            <a href="#" class="cta-button">SellerX'e Git</a>
                        </p>
                    </div>
                    <div class="footer">
                        <p>Bu e-posta SellerX uyarı sistemi tarafından otomatik gönderilmiştir.</p>
                        <p>&copy; 2024 SellerX. Tüm hakları saklıdır.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(severityColor, severityColor, severityColor, emoji, alertTypeLabel, severityLabel, alertTitle, alertTypeLabel, storeSection, alertMessage);
    }

    private String buildAlertDigestHtml(String digestPeriod, List<String> alertSummaries, int totalAlerts) {
        StringBuilder summaryHtml = new StringBuilder();
        for (String summary : alertSummaries) {
            summaryHtml.append("<li style=\"padding: 8px 0; border-bottom: 1px solid #f3f4f6;\">").append(summary).append("</li>");
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #3b82f6; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background-color: #f9fafb; padding: 20px; border-radius: 0 0 8px 8px; }
                    .stat-box { background-color: white; border: 1px solid #e5e7eb; border-radius: 8px; padding: 20px; margin: 15px 0; text-align: center; }
                    .stat-number { font-size: 48px; font-weight: bold; color: #3b82f6; }
                    .stat-label { color: #6b7280; font-size: 14px; }
                    .summary-list { background-color: white; border: 1px solid #e5e7eb; border-radius: 8px; padding: 15px; margin: 15px 0; list-style: none; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                    .cta-button { display: inline-block; background-color: #3b82f6; color: white; padding: 12px 24px; border-radius: 6px; text-decoration: none; font-weight: 600; margin-top: 15px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>📊 %s Uyarı Özeti</h1>
                    </div>
                    <div class="content">
                        <div class="stat-box">
                            <div class="stat-number">%d</div>
                            <div class="stat-label">Toplam Uyarı</div>
                        </div>

                        <h3>Uyarı Özetleri</h3>
                        <ul class="summary-list">
                            %s
                        </ul>

                        <p style="text-align: center;">
                            <a href="#" class="cta-button">Tüm Uyarıları Görüntüle</a>
                        </p>
                    </div>
                    <div class="footer">
                        <p>Bu e-posta SellerX uyarı sistemi tarafından otomatik gönderilmiştir.</p>
                        <p>&copy; 2024 SellerX. Tüm hakları saklıdır.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(digestPeriod, totalAlerts, summaryHtml.toString());
    }
}
