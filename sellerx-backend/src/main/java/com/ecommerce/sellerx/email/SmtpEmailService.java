package com.ecommerce.sellerx.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * SMTP-based email service implementation.
 * Activated when email.provider=smtp
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "email.provider", havingValue = "smtp", matchIfMissing = true)
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final SmtpEmailConfig emailConfig;

    @Override
    @Async("taskExecutor")
    public void sendEmail(String to, String subject, String htmlBody) {
        if (!emailConfig.isEnabled()) {
            log.info("[EMAIL-DISABLED] Would send to={}, subject={}", to, subject);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailConfig.getFromAddress(), emailConfig.getFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML

            mailSender.send(message);
            log.info("[EMAIL-SENT] to={}, subject={}", to, subject);

        } catch (Exception e) {
            log.error("[EMAIL-FAILED] to={}, subject={}, error={}", to, subject, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    public void sendTemplateEmail(String to, EmailTemplate template, Map<String, Object> variables) {
        // Template emails will be handled by EmailQueueService -> EmailTemplateService
        // This is a fallback for direct template sends
        String subject = template.getDefaultSubject();
        String body = "<p>Template: " + template.getTemplateId() + "</p>";
        sendEmail(to, subject, body);
    }

    @Override
    public void sendEmail(EmailRequest request) {
        sendEmail(request.getTo(), request.getSubject(), request.getHtmlBody());
    }

    // === Support Ticket Methods ===

    @Override
    public void sendTicketCreatedEmail(String to, String ticketNumber, String subject) {
        String emailSubject = "Destek Talebiniz Alındı - #" + ticketNumber;
        String body = buildTicketCreatedBody(ticketNumber, subject);
        sendEmail(to, emailSubject, body);
    }

    @Override
    public void sendTicketReplyEmail(String to, String ticketNumber, String senderName, String replyPreview) {
        String emailSubject = "Destek Talebinize Yanıt - #" + ticketNumber;
        String body = buildTicketReplyBody(ticketNumber, senderName, replyPreview);
        sendEmail(to, emailSubject, body);
    }

    @Override
    public void sendTicketClosedEmail(String to, String ticketNumber) {
        String emailSubject = "Destek Talebiniz Kapatıldı - #" + ticketNumber;
        String body = buildTicketClosedBody(ticketNumber);
        sendEmail(to, emailSubject, body);
    }

    @Override
    public void sendTicketAssignedEmail(String adminEmail, String ticketNumber, String subject, String userName) {
        String emailSubject = "Destek Talebi Size Atandı - #" + ticketNumber;
        String body = buildTicketAssignedBody(ticketNumber, subject, userName);
        sendEmail(adminEmail, emailSubject, body);
    }

    @Override
    public void sendAdminNewTicketEmail(String adminEmail, String ticketNumber, String userName, String subject) {
        String emailSubject = "Yeni Destek Talebi - #" + ticketNumber;
        String body = buildAdminNewTicketBody(ticketNumber, userName, subject);
        sendEmail(adminEmail, emailSubject, body);
    }

    // === Alert Methods ===

    @Override
    public void sendStockAlertEmail(String to, String productName, String barcode, int currentStock,
                                    int threshold, String storeName, String severity) {
        String emailSubject = "Stok Uyarısı: " + productName;
        String body = buildStockAlertBody(productName, barcode, currentStock, threshold, storeName, severity);
        sendEmail(to, emailSubject, body);
    }

    @Override
    public void sendProfitAlertEmail(String to, String productName, String barcode, double currentMargin,
                                     double threshold, String storeName, String severity) {
        String emailSubject = "Kar Marjı Uyarısı: " + productName;
        String body = buildProfitAlertBody(productName, barcode, currentMargin, threshold, storeName, severity);
        sendEmail(to, emailSubject, body);
    }

    @Override
    public void sendAlertEmail(String to, String alertTitle, String alertMessage,
                               String storeName, String severity, String alertType) {
        String body = buildGenericAlertBody(alertTitle, alertMessage, storeName, severity, alertType);
        sendEmail(to, alertTitle, body);
    }

    @Override
    public void sendAlertDigestEmail(String to, String digestPeriod,
                                     List<String> alertSummaries, int totalAlerts) {
        String emailSubject = digestPeriod + " Alarm Özeti";
        String body = buildDigestBody(digestPeriod, alertSummaries, totalAlerts);
        sendEmail(to, emailSubject, body);
    }

    // === Private Body Builders ===

    private String buildTicketCreatedBody(String ticketNumber, String subject) {
        return wrapInTemplate("""
            <h2>Destek Talebiniz Alındı</h2>
            <p>Destek talebiniz başarıyla oluşturuldu.</p>
            <p><strong>Talep No:</strong> #%s</p>
            <p><strong>Konu:</strong> %s</p>
            <p>En kısa sürede size dönüş yapacağız.</p>
            """.formatted(ticketNumber, subject));
    }

    private String buildTicketReplyBody(String ticketNumber, String senderName, String replyPreview) {
        return wrapInTemplate("""
            <h2>Destek Talebinize Yanıt</h2>
            <p><strong>Talep No:</strong> #%s</p>
            <p><strong>Yanıtlayan:</strong> %s</p>
            <p>%s</p>
            """.formatted(ticketNumber, senderName, replyPreview));
    }

    private String buildTicketClosedBody(String ticketNumber) {
        return wrapInTemplate("""
            <h2>Destek Talebiniz Kapatıldı</h2>
            <p><strong>Talep No:</strong> #%s</p>
            <p>Sorununuzun çözüldüğünü umuyoruz. İyi günler!</p>
            """.formatted(ticketNumber));
    }

    private String buildTicketAssignedBody(String ticketNumber, String subject, String userName) {
        return wrapInTemplate("""
            <h2>Yeni Destek Talebi Atandı</h2>
            <p><strong>Talep No:</strong> #%s</p>
            <p><strong>Kullanıcı:</strong> %s</p>
            <p><strong>Konu:</strong> %s</p>
            """.formatted(ticketNumber, userName, subject));
    }

    private String buildAdminNewTicketBody(String ticketNumber, String userName, String subject) {
        return wrapInTemplate("""
            <h2>Yeni Destek Talebi</h2>
            <p><strong>Talep No:</strong> #%s</p>
            <p><strong>Kullanıcı:</strong> %s</p>
            <p><strong>Konu:</strong> %s</p>
            """.formatted(ticketNumber, userName, subject));
    }

    private String buildStockAlertBody(String productName, String barcode, int currentStock,
                                       int threshold, String storeName, String severity) {
        return wrapInTemplate("""
            <h2>Stok Uyarısı</h2>
            <p><strong>Mağaza:</strong> %s</p>
            <p><strong>Ürün:</strong> %s</p>
            <p><strong>Barkod:</strong> %s</p>
            <p><strong>Mevcut Stok:</strong> %d</p>
            <p><strong>Eşik Değer:</strong> %d</p>
            <p><strong>Önem:</strong> %s</p>
            """.formatted(storeName, productName, barcode, currentStock, threshold, severity));
    }

    private String buildProfitAlertBody(String productName, String barcode, double currentMargin,
                                        double threshold, String storeName, String severity) {
        return wrapInTemplate("""
            <h2>Kar Marjı Uyarısı</h2>
            <p><strong>Mağaza:</strong> %s</p>
            <p><strong>Ürün:</strong> %s</p>
            <p><strong>Barkod:</strong> %s</p>
            <p><strong>Mevcut Marj:</strong> %.2f%%</p>
            <p><strong>Eşik Değer:</strong> %.2f%%</p>
            <p><strong>Önem:</strong> %s</p>
            """.formatted(storeName, productName, barcode, currentMargin, threshold, severity));
    }

    private String buildGenericAlertBody(String alertTitle, String alertMessage,
                                         String storeName, String severity, String alertType) {
        return wrapInTemplate("""
            <h2>%s</h2>
            <p><strong>Tür:</strong> %s</p>
            %s
            <p>%s</p>
            <p><strong>Önem:</strong> %s</p>
            """.formatted(
                alertTitle,
                alertType,
                storeName != null ? "<p><strong>Mağaza:</strong> " + storeName + "</p>" : "",
                alertMessage,
                severity
        ));
    }

    private String buildDigestBody(String digestPeriod, List<String> alertSummaries, int totalAlerts) {
        StringBuilder summaryHtml = new StringBuilder();
        for (String summary : alertSummaries) {
            summaryHtml.append("<li>").append(summary).append("</li>");
        }

        return wrapInTemplate("""
            <h2>%s Alarm Özeti</h2>
            <p>Toplam <strong>%d</strong> alarm tetiklendi:</p>
            <ul>%s</ul>
            """.formatted(digestPeriod, totalAlerts, summaryHtml.toString()));
    }

    private String wrapInTemplate(String content) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    h2 { color: #2563eb; }
                    strong { color: #1f2937; }
                </style>
            </head>
            <body>
                %s
                <hr>
                <p style="color: #6b7280; font-size: 12px;">Bu email SellerX tarafından otomatik olarak gönderilmiştir.</p>
            </body>
            </html>
            """.formatted(content);
    }
}
