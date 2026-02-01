package com.ecommerce.sellerx.webhook;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
@Slf4j
public class WebhookSignatureValidator {

    private static final String HMAC_SHA256 = "HmacSHA256";

    @Value("${app.webhook.signature-secret:}")
    private String signatureSecret;

    @Value("${app.webhook.signature-validation-enabled:true}")
    private boolean signatureValidationEnabled;

    /**
     * Validate the X-Trendyol-Signature header against the request body.
     * Trendyol uses HMAC-SHA256 with Base64 encoding.
     *
     * @param signature The signature from X-Trendyol-Signature header
     * @param payload   The raw request body
     * @param sellerId  The seller ID (for logging)
     * @return true if signature is valid or validation is disabled (dev only)
     */
    public boolean validateSignature(String signature, String payload, String sellerId) {
        if (!signatureValidationEnabled) {
            log.warn("Signature validation is disabled - NOT RECOMMENDED for production!");
            return true;
        }

        if (signature == null || signature.isEmpty()) {
            log.error("Missing X-Trendyol-Signature header for seller: {} - REJECTING request", sellerId);
            return false;
        }

        if (signatureSecret == null || signatureSecret.isEmpty()) {
            log.error("Webhook signature secret not configured but validation is enabled - REJECTING request for seller: {}", sellerId);
            return false;
        }

        try {
            String expectedSignature = computeSignature(payload);

            // Constant-time comparison to prevent timing attacks
            boolean valid = MessageDigest.isEqual(
                    signature.getBytes(StandardCharsets.UTF_8),
                    expectedSignature.getBytes(StandardCharsets.UTF_8)
            );

            if (!valid) {
                log.warn("Invalid webhook signature for seller: {}", sellerId);
            }

            return valid;

        } catch (Exception e) {
            log.error("Error validating webhook signature: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Compute HMAC-SHA256 signature for the given payload.
     */
    private String computeSignature(String payload) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                signatureSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        mac.init(secretKeySpec);

        byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hmacBytes);
    }

    /**
     * Generate a unique event ID for idempotency.
     * Uses SHA-256 hash of key fields.
     *
     * @param sellerId    Seller ID
     * @param orderNumber Order number
     * @param status      Order status
     * @param timestamp   Timestamp in milliseconds
     * @return Unique event ID
     */
    public String generateEventId(String sellerId, String orderNumber, String status, Long timestamp) {
        String data = String.format("%s:%s:%s:%d",
                sellerId != null ? sellerId : "",
                orderNumber != null ? orderNumber : "",
                status != null ? status : "",
                timestamp != null ? timestamp : System.currentTimeMillis()
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple concatenation
            log.warn("SHA-256 not available, using fallback event ID generation");
            return data.replace(":", "_");
        }
    }
}
