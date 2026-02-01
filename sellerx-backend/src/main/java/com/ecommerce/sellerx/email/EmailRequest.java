package com.ecommerce.sellerx.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Data transfer object for email requests.
 * Encapsulates all the information needed to send an email.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {

    /**
     * Recipient email address.
     */
    private String to;

    /**
     * Email subject line.
     */
    private String subject;

    /**
     * Plain text body of the email.
     */
    private String textBody;

    /**
     * HTML body of the email (takes precedence over textBody if both are set).
     */
    private String htmlBody;

    /**
     * Email template to use (optional).
     */
    private EmailTemplate template;

    /**
     * Variables to use for template rendering.
     */
    private Map<String, Object> templateVariables;

    /**
     * Creates a simple email request with just recipient, subject, and HTML body.
     */
    public static EmailRequest simple(String to, String subject, String htmlBody) {
        return EmailRequest.builder()
                .to(to)
                .subject(subject)
                .htmlBody(htmlBody)
                .build();
    }

    /**
     * Creates an email request using a template.
     */
    public static EmailRequest fromTemplate(String to, EmailTemplate template, Map<String, Object> variables) {
        return EmailRequest.builder()
                .to(to)
                .template(template)
                .templateVariables(variables)
                .build();
    }
}
