package com.ecommerce.sellerx.email.service;

import com.ecommerce.sellerx.email.EmailType;
import com.ecommerce.sellerx.email.entity.EmailBaseLayout;
import com.ecommerce.sellerx.email.entity.EmailTemplateEntity;
import com.ecommerce.sellerx.email.repository.EmailBaseLayoutRepository;
import com.ecommerce.sellerx.email.repository.EmailTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for rendering email templates with variable substitution.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final EmailTemplateRepository templateRepository;
    private final EmailBaseLayoutRepository baseLayoutRepository;

    // Pattern to match {{variableName}}
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    /**
     * Get template by email type.
     */
    public Optional<EmailTemplateEntity> getTemplate(EmailType emailType) {
        return templateRepository.findByEmailTypeAndIsActiveTrue(emailType);
    }

    /**
     * Get all templates (for admin panel).
     */
    public List<EmailTemplateEntity> getAllTemplates() {
        return templateRepository.findAll();
    }

    /**
     * Get template by type (for admin panel).
     */
    public Optional<EmailTemplateEntity> getTemplateByType(EmailType emailType) {
        return templateRepository.findByEmailType(emailType);
    }

    /**
     * Save template (for admin panel).
     */
    public EmailTemplateEntity saveTemplate(EmailTemplateEntity template) {
        return templateRepository.save(template);
    }

    /**
     * Get base layout.
     */
    @Cacheable(value = "emailBaseLayout", key = "'singleton'")
    public EmailBaseLayout getBaseLayout() {
        return baseLayoutRepository.findFirst().orElse(createDefaultBaseLayout());
    }

    /**
     * Save base layout.
     */
    public EmailBaseLayout saveBaseLayout(EmailBaseLayout layout) {
        return baseLayoutRepository.save(layout);
    }

    /**
     * Render subject with variables.
     */
    public String renderSubject(EmailType emailType, Map<String, Object> variables) {
        return getTemplate(emailType)
                .map(t -> substituteVariables(t.getSubjectTemplate(), variables))
                .orElse("SellerX Bildirimi");
    }

    /**
     * Render body with variables.
     */
    public String renderBody(EmailType emailType, Map<String, Object> variables) {
        return getTemplate(emailType)
                .map(t -> wrapInBaseTemplate(substituteVariables(t.getBodyTemplate(), variables)))
                .orElse(buildFallbackBody(emailType, variables));
    }

    /**
     * Render both subject and body.
     */
    public RenderedEmail render(EmailType emailType, Map<String, Object> variables) {
        return new RenderedEmail(
                renderSubject(emailType, variables),
                renderBody(emailType, variables)
        );
    }

    /**
     * Preview rendered email with sample variables (for admin panel).
     */
    public RenderedEmail previewTemplate(EmailTemplateEntity template, Map<String, Object> sampleVariables) {
        String subject = substituteVariables(template.getSubjectTemplate(), sampleVariables);
        String body = wrapInBaseTemplate(substituteVariables(template.getBodyTemplate(), sampleVariables));
        return new RenderedEmail(subject, body);
    }

    /**
     * Preview body only (for live editing).
     */
    public String previewBody(String bodyTemplate, Map<String, Object> sampleVariables) {
        return wrapInBaseTemplate(substituteVariables(bodyTemplate, sampleVariables));
    }

    /**
     * Substitute {{variable}} placeholders with values from map.
     */
    private String substituteVariables(String template, Map<String, Object> variables) {
        if (template == null || variables == null) {
            return template;
        }

        StringBuffer result = new StringBuffer();
        Matcher matcher = VARIABLE_PATTERN.matcher(template);

        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = variables.get(variableName);
            String replacement = value != null ? Matcher.quoteReplacement(value.toString()) : "";
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Wrap content in base HTML template from database.
     */
    private String wrapInBaseTemplate(String content) {
        EmailBaseLayout layout = getBaseLayout();

        // Replace placeholders in styles
        String styles = layout.getStyles();
        if (styles != null) {
            styles = styles.replace("{{primaryColor}}", layout.getPrimaryColor());
        }

        // Replace placeholders in header
        String header = layout.getHeaderHtml();
        if (header != null) {
            header = header.replace("{{logoUrl}}", layout.getLogoUrl() != null ? layout.getLogoUrl() : "");
        }

        return """
            <!DOCTYPE html>
            <html lang="tr">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    %s
                </style>
            </head>
            <body>
                <div class="container">
                    %s
                    <div class="content">
                        %s
                    </div>
                    %s
                </div>
            </body>
            </html>
            """.formatted(
                styles != null ? styles : getDefaultStyles(),
                header != null ? header : "",
                content,
                layout.getFooterHtml() != null ? layout.getFooterHtml() : ""
            );
    }

    /**
     * Build fallback body when template not found.
     */
    private String buildFallbackBody(EmailType emailType, Map<String, Object> variables) {
        String content = """
            <h2>SellerX Bildirimi</h2>
            <p>Email tipi: %s</p>
            """.formatted(emailType.name());
        return wrapInBaseTemplate(content);
    }

    /**
     * Create default base layout when none exists in DB.
     */
    private EmailBaseLayout createDefaultBaseLayout() {
        return EmailBaseLayout.builder()
                .headerHtml("<div style=\"text-align: center; padding: 24px 0; border-bottom: 1px solid #e5e7eb;\"><h1 style=\"color: #2563eb; margin: 0;\">SellerX</h1></div>")
                .footerHtml("<div style=\"margin-top: 32px; padding-top: 16px; border-top: 1px solid #e5e7eb; color: #6b7280; font-size: 12px; text-align: center;\"><p>Bu email SellerX tarafından otomatik olarak gönderilmiştir.</p><p>&copy; 2024 SellerX. Tüm hakları saklıdır.</p></div>")
                .styles(getDefaultStyles())
                .primaryColor("#2563eb")
                .build();
    }

    /**
     * Default CSS styles.
     */
    private String getDefaultStyles() {
        return """
            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                line-height: 1.6;
                color: #1f2937;
                background-color: #f3f4f6;
                margin: 0;
                padding: 20px;
            }
            .container {
                max-width: 600px;
                margin: 0 auto;
                background-color: #ffffff;
                border-radius: 8px;
                box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
                padding: 32px;
            }
            h1, h2 { color: #111827; margin-top: 0; }
            a { color: #2563eb; }
            .button {
                display: inline-block;
                background-color: #2563eb;
                color: #ffffff !important;
                text-decoration: none;
                padding: 12px 24px;
                border-radius: 6px;
                margin: 16px 0;
            }
            .content {
                padding: 24px 0;
            }
            """;
    }

    /**
     * Rendered email result.
     */
    public record RenderedEmail(String subject, String body) {}
}
