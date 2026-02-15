package com.ecommerce.sellerx.admin;

import com.ecommerce.sellerx.admin.dto.*;
import com.ecommerce.sellerx.email.EmailService;
import com.ecommerce.sellerx.email.EmailType;
import com.ecommerce.sellerx.email.entity.EmailBaseLayout;
import com.ecommerce.sellerx.email.entity.EmailTemplateEntity;
import com.ecommerce.sellerx.email.service.EmailTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/email-templates")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminEmailTemplateController {

    private final EmailTemplateService emailTemplateService;
    private final EmailService emailService;

    /**
     * Get all email templates
     * GET /api/admin/email-templates
     */
    @GetMapping
    public ResponseEntity<List<EmailTemplateDto>> getAllTemplates() {
        log.info("Admin fetching all email templates");
        List<EmailTemplateDto> templates = emailTemplateService.getAllTemplates().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(templates);
    }

    /**
     * Get single template by type
     * GET /api/admin/email-templates/{type}
     */
    @GetMapping("/{type}")
    public ResponseEntity<EmailTemplateDto> getTemplate(@PathVariable String type) {
        log.info("Admin fetching email template: {}", type);
        EmailType emailType = parseEmailType(type);
        if (emailType == null) {
            return ResponseEntity.notFound().build();
        }

        return emailTemplateService.getTemplateByType(emailType)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update template
     * PUT /api/admin/email-templates/{type}
     */
    @PutMapping("/{type}")
    public ResponseEntity<EmailTemplateDto> updateTemplate(
            @PathVariable String type,
            @Valid @RequestBody EmailTemplateUpdateRequest request) {
        log.info("Admin updating email template: {}", type);
        EmailType emailType = parseEmailType(type);
        if (emailType == null) {
            return ResponseEntity.notFound().build();
        }

        return emailTemplateService.getTemplateByType(emailType)
                .map(template -> {
                    template.setSubjectTemplate(request.getSubjectTemplate());
                    template.setBodyTemplate(request.getBodyTemplate());
                    if (request.getDescription() != null) {
                        template.setDescription(request.getDescription());
                    }
                    if (request.getIsActive() != null) {
                        template.setIsActive(request.getIsActive());
                    }
                    EmailTemplateEntity saved = emailTemplateService.saveTemplate(template);
                    return ResponseEntity.ok(toDto(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Preview rendered template
     * POST /api/admin/email-templates/{type}/preview
     */
    @PostMapping("/{type}/preview")
    public ResponseEntity<EmailPreviewResponse> previewTemplate(
            @PathVariable String type,
            @RequestBody EmailPreviewRequest request) {
        log.info("Admin previewing email template: {}", type);
        EmailType emailType = parseEmailType(type);
        if (emailType == null) {
            return ResponseEntity.notFound().build();
        }

        // Use sample variables if not provided
        Map<String, Object> variables = request.getSampleVariables();
        if (variables == null || variables.isEmpty()) {
            variables = getSampleVariables(emailType);
        }

        // If custom template content provided, use it; otherwise use DB template
        String subject;
        String body;

        if (request.getSubjectTemplate() != null && request.getBodyTemplate() != null) {
            // Preview with custom content (live editing)
            EmailTemplateEntity tempTemplate = new EmailTemplateEntity();
            tempTemplate.setSubjectTemplate(request.getSubjectTemplate());
            tempTemplate.setBodyTemplate(request.getBodyTemplate());
            EmailTemplateService.RenderedEmail rendered = emailTemplateService.previewTemplate(tempTemplate, variables);
            subject = rendered.subject();
            body = rendered.body();
        } else {
            // Preview DB template
            EmailTemplateService.RenderedEmail rendered = emailTemplateService.render(emailType, variables);
            subject = rendered.subject();
            body = rendered.body();
        }

        return ResponseEntity.ok(EmailPreviewResponse.builder()
                .subject(subject)
                .body(body)
                .build());
    }

    /**
     * Send test email
     * POST /api/admin/email-templates/{type}/test
     */
    @PostMapping("/{type}/test")
    public ResponseEntity<Map<String, Object>> sendTestEmail(
            @PathVariable String type,
            @Valid @RequestBody EmailTestRequest request) {
        log.info("Admin sending test email for template: {} to: {}", type, request.getRecipientEmail());
        EmailType emailType = parseEmailType(type);
        if (emailType == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Map<String, Object> variables = getSampleVariables(emailType);
            EmailTemplateService.RenderedEmail rendered = emailTemplateService.render(emailType, variables);

            emailService.sendEmail(
                    request.getRecipientEmail(),
                    "[TEST] " + rendered.subject(),
                    rendered.body()
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Test email sent successfully to " + request.getRecipientEmail()
            ));
        } catch (Exception e) {
            log.error("Failed to send test email", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Failed to send email: " + e.getMessage()
            ));
        }
    }

    /**
     * Get all available variables by template type
     * GET /api/admin/email-templates/variables
     */
    @GetMapping("/variables")
    public ResponseEntity<EmailVariablesDto> getVariables() {
        log.info("Admin fetching email template variables");

        Map<String, List<EmailVariablesDto.VariableInfo>> variablesByType = new LinkedHashMap<>();

        // User lifecycle templates
        variablesByType.put("WELCOME", List.of(
                variable("userName", "Kullanıcı adı", "Ahmet Yılmaz"),
                variable("userEmail", "Kullanıcı email adresi", "ahmet@example.com")
        ));

        variablesByType.put("PASSWORD_RESET", List.of(
                variable("userName", "Kullanıcı adı", "Ahmet Yılmaz"),
                variable("resetLink", "Şifre sıfırlama linki", "https://sellerx.com/reset?token=abc123")
        ));

        variablesByType.put("EMAIL_VERIFICATION", List.of(
                variable("userName", "Kullanıcı adı", "Ahmet Yılmaz"),
                variable("verificationLink", "Email doğrulama linki", "https://sellerx.com/verify?token=xyz789")
        ));

        // Subscription templates
        variablesByType.put("SUBSCRIPTION_CONFIRMED", List.of(
                variable("userName", "Kullanıcı adı", "Ahmet Yılmaz"),
                variable("planName", "Plan adı", "Pro"),
                variable("amount", "Ödeme tutarı", "299 TL")
        ));

        variablesByType.put("SUBSCRIPTION_REMINDER_7", List.of(
                variable("userName", "Kullanıcı adı", "Ahmet Yılmaz"),
                variable("planName", "Plan adı", "Pro"),
                variable("expiryDate", "Bitiş tarihi", "15 Mart 2024")
        ));

        variablesByType.put("SUBSCRIPTION_REMINDER_1", List.of(
                variable("userName", "Kullanıcı adı", "Ahmet Yılmaz"),
                variable("planName", "Plan adı", "Pro"),
                variable("expiryDate", "Bitiş tarihi", "15 Mart 2024")
        ));

        variablesByType.put("SUBSCRIPTION_RENEWED", List.of(
                variable("userName", "Kullanıcı adı", "Ahmet Yılmaz"),
                variable("planName", "Plan adı", "Pro"),
                variable("amount", "Ödeme tutarı", "299 TL")
        ));

        variablesByType.put("PAYMENT_FAILED", List.of(
                variable("userName", "Kullanıcı adı", "Ahmet Yılmaz"),
                variable("planName", "Plan adı", "Pro"),
                variable("amount", "Ödeme tutarı", "299 TL")
        ));

        variablesByType.put("SUBSCRIPTION_CANCELLED", List.of(
                variable("userName", "Kullanıcı adı", "Ahmet Yılmaz"),
                variable("planName", "Plan adı", "Pro")
        ));

        // Alert templates
        variablesByType.put("ALERT_NOTIFICATION", List.of(
                variable("userName", "Kullanıcı adı", "Ahmet Yılmaz"),
                variable("alertTitle", "Alarm başlığı", "Stok Uyarısı"),
                variable("alertMessage", "Alarm mesajı", "Ürün stoğu kritik seviyeye düştü.")
        ));

        variablesByType.put("DAILY_DIGEST", List.of(
                variable("userName", "Kullanıcı adı", "Ahmet Yılmaz"),
                variable("date", "Tarih", "14 Mart 2024"),
                variable("digestContent", "Özet içeriği", "<ul><li>5 yeni sipariş</li><li>3 stok uyarısı</li></ul>")
        ));

        variablesByType.put("WEEKLY_REPORT", List.of(
                variable("userName", "Kullanıcı adı", "Ahmet Yılmaz"),
                variable("reportContent", "Rapor içeriği", "<p>Bu hafta 150 sipariş işlendi...</p>")
        ));

        variablesByType.put("ADMIN_BROADCAST", List.of(
                variable("subject", "Email konusu", "Sistem Bakımı"),
                variable("content", "Email içeriği", "<p>Yarın saat 03:00-05:00 arası bakım yapılacaktır.</p>")
        ));

        return ResponseEntity.ok(EmailVariablesDto.builder()
                .variablesByType(variablesByType)
                .build());
    }

    /**
     * Get base layout
     * GET /api/admin/email-templates/base-layout
     */
    @GetMapping("/base-layout")
    public ResponseEntity<EmailBaseLayoutDto> getBaseLayout() {
        log.info("Admin fetching email base layout");
        EmailBaseLayout layout = emailTemplateService.getBaseLayout();
        return ResponseEntity.ok(toLayoutDto(layout));
    }

    /**
     * Update base layout
     * PUT /api/admin/email-templates/base-layout
     */
    @PutMapping("/base-layout")
    public ResponseEntity<EmailBaseLayoutDto> updateBaseLayout(
            @Valid @RequestBody EmailBaseLayoutUpdateRequest request) {
        log.info("Admin updating email base layout");
        EmailBaseLayout layout = emailTemplateService.getBaseLayout();

        layout.setHeaderHtml(request.getHeaderHtml());
        layout.setFooterHtml(request.getFooterHtml());
        layout.setStyles(request.getStyles());
        layout.setLogoUrl(request.getLogoUrl());
        layout.setPrimaryColor(request.getPrimaryColor());

        EmailBaseLayout saved = emailTemplateService.saveBaseLayout(layout);
        return ResponseEntity.ok(toLayoutDto(saved));
    }

    // === Helper Methods ===

    private EmailType parseEmailType(String type) {
        try {
            return EmailType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid email type: {}", type);
            return null;
        }
    }

    private EmailTemplateDto toDto(EmailTemplateEntity entity) {
        return EmailTemplateDto.builder()
                .id(entity.getId())
                .emailType(entity.getEmailType().name())
                .name(entity.getName())
                .subjectTemplate(entity.getSubjectTemplate())
                .bodyTemplate(entity.getBodyTemplate())
                .description(entity.getDescription())
                .isActive(entity.getIsActive())
                .availableVariables(getVariableNames(entity.getEmailType()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private EmailBaseLayoutDto toLayoutDto(EmailBaseLayout entity) {
        return EmailBaseLayoutDto.builder()
                .id(entity.getId())
                .headerHtml(entity.getHeaderHtml())
                .footerHtml(entity.getFooterHtml())
                .styles(entity.getStyles())
                .logoUrl(entity.getLogoUrl())
                .primaryColor(entity.getPrimaryColor())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private List<String> getVariableNames(EmailType emailType) {
        return switch (emailType) {
            case WELCOME -> List.of("userName", "userEmail");
            case PASSWORD_RESET -> List.of("userName", "resetLink");
            case EMAIL_VERIFICATION -> List.of("userName", "verificationLink");
            case SUBSCRIPTION_CONFIRMED, SUBSCRIPTION_RENEWED, PAYMENT_FAILED -> List.of("userName", "planName", "amount");
            case SUBSCRIPTION_REMINDER_7, SUBSCRIPTION_REMINDER_1 -> List.of("userName", "planName", "expiryDate");
            case SUBSCRIPTION_CANCELLED -> List.of("userName", "planName");
            case ALERT_NOTIFICATION -> List.of("userName", "alertTitle", "alertMessage");
            case DAILY_DIGEST -> List.of("userName", "date", "digestContent");
            case WEEKLY_REPORT -> List.of("userName", "reportContent");
            case ADMIN_BROADCAST -> List.of("subject", "content");
            default -> List.of("userName");
        };
    }

    private Map<String, Object> getSampleVariables(EmailType emailType) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", "Ahmet Yılmaz");
        variables.put("userEmail", "ahmet@example.com");

        switch (emailType) {
            case PASSWORD_RESET -> variables.put("resetLink", "https://sellerx.com/reset?token=sample123");
            case EMAIL_VERIFICATION -> variables.put("verificationLink", "https://sellerx.com/verify?token=sample456");
            case SUBSCRIPTION_CONFIRMED, SUBSCRIPTION_RENEWED, PAYMENT_FAILED -> {
                variables.put("planName", "Pro");
                variables.put("amount", "299 TL");
            }
            case SUBSCRIPTION_REMINDER_7, SUBSCRIPTION_REMINDER_1 -> {
                variables.put("planName", "Pro");
                variables.put("expiryDate", "15 Mart 2024");
            }
            case SUBSCRIPTION_CANCELLED -> variables.put("planName", "Pro");
            case ALERT_NOTIFICATION -> {
                variables.put("alertTitle", "Stok Uyarısı");
                variables.put("alertMessage", "iPhone 15 Pro Max ürününün stoğu kritik seviyeye (5 adet) düştü.");
            }
            case DAILY_DIGEST -> {
                variables.put("date", "14 Mart 2024");
                variables.put("digestContent", "<ul><li>5 yeni sipariş alındı</li><li>3 ürün stoğu kritik</li><li>Toplam ciro: 15.000 TL</li></ul>");
            }
            case WEEKLY_REPORT -> variables.put("reportContent", "<p>Bu hafta toplam 150 sipariş işlendi. Geçen haftaya göre %12 artış gösterdiniz.</p>");
            case ADMIN_BROADCAST -> {
                variables.put("subject", "Sistem Güncellemesi");
                variables.put("content", "<p>Değerli kullanıcımız,</p><p>Yarın saat 03:00-05:00 arası planlı bakım yapılacaktır.</p>");
            }
            default -> {}
        }

        return variables;
    }

    private EmailVariablesDto.VariableInfo variable(String name, String description, String sampleValue) {
        return EmailVariablesDto.VariableInfo.builder()
                .name(name)
                .description(description)
                .sampleValue(sampleValue)
                .build();
    }
}
