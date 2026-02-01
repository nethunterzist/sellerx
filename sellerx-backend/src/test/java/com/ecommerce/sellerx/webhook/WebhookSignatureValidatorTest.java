package com.ecommerce.sellerx.webhook;

import com.ecommerce.sellerx.common.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for WebhookSignatureValidator.
 * Tests HMAC-SHA256 signature validation and event ID generation.
 */
@DisplayName("WebhookSignatureValidator")
class WebhookSignatureValidatorTest extends BaseUnitTest {

    private WebhookSignatureValidator validator;

    private static final String TEST_SECRET = "test-webhook-secret-key-for-testing";
    private static final String TEST_SELLER_ID = "123456";
    private static final String TEST_PAYLOAD = "{\"orderNumber\":\"TY-ORDER-001\",\"status\":\"Delivered\"}";

    @BeforeEach
    void setUp() throws Exception {
        validator = new WebhookSignatureValidator();
        setField(validator, "signatureSecret", TEST_SECRET);
        setField(validator, "signatureValidationEnabled", true);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private String computeValidSignature(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hmacBytes);
    }

    @Nested
    @DisplayName("validateSignature")
    class ValidateSignature {

        @Test
        @DisplayName("should accept valid HMAC-SHA256 signature")
        void shouldAcceptValidSignature() throws Exception {
            // Given
            String validSignature = computeValidSignature(TEST_PAYLOAD);

            // When
            boolean result = validator.validateSignature(validSignature, TEST_PAYLOAD, TEST_SELLER_ID);

            // Then
            assertThat(result)
                    .as("Valid HMAC-SHA256 signature should be accepted")
                    .isTrue();
        }

        @Test
        @DisplayName("should reject invalid signature")
        void shouldRejectInvalidSignature() {
            // Given
            String invalidSignature = "totally-invalid-signature-value";

            // When
            boolean result = validator.validateSignature(invalidSignature, TEST_PAYLOAD, TEST_SELLER_ID);

            // Then
            assertThat(result)
                    .as("Invalid signature should be rejected")
                    .isFalse();
        }

        @Test
        @DisplayName("should reject tampered payload with valid format signature")
        void shouldRejectTamperedPayload() throws Exception {
            // Given - signature computed for original payload
            String validSignature = computeValidSignature(TEST_PAYLOAD);
            String tamperedPayload = "{\"orderNumber\":\"TY-ORDER-999\",\"status\":\"Cancelled\"}";

            // When
            boolean result = validator.validateSignature(validSignature, tamperedPayload, TEST_SELLER_ID);

            // Then
            assertThat(result)
                    .as("Signature for different payload should be rejected")
                    .isFalse();
        }

        @Test
        @DisplayName("should reject null signature")
        void shouldRejectNullSignature() {
            // When
            boolean result = validator.validateSignature(null, TEST_PAYLOAD, TEST_SELLER_ID);

            // Then
            assertThat(result)
                    .as("Null signature should be rejected")
                    .isFalse();
        }

        @Test
        @DisplayName("should reject empty signature")
        void shouldRejectEmptySignature() {
            // When
            boolean result = validator.validateSignature("", TEST_PAYLOAD, TEST_SELLER_ID);

            // Then
            assertThat(result)
                    .as("Empty signature should be rejected")
                    .isFalse();
        }

        @Test
        @DisplayName("should reject when signature secret is not configured")
        void shouldRejectWhenSecretNotConfigured() throws Exception {
            // Given
            setField(validator, "signatureSecret", "");

            // When
            boolean result = validator.validateSignature("some-signature", TEST_PAYLOAD, TEST_SELLER_ID);

            // Then
            assertThat(result)
                    .as("Missing secret configuration should cause rejection")
                    .isFalse();
        }

        @Test
        @DisplayName("should accept any signature when validation is disabled")
        void shouldAcceptAnySignatureWhenValidationDisabled() throws Exception {
            // Given
            setField(validator, "signatureValidationEnabled", false);

            // When
            boolean result = validator.validateSignature("any-signature", TEST_PAYLOAD, TEST_SELLER_ID);

            // Then
            assertThat(result)
                    .as("Validation disabled should accept any signature")
                    .isTrue();
        }

        @Test
        @DisplayName("should accept null signature when validation is disabled")
        void shouldAcceptNullSignatureWhenValidationDisabled() throws Exception {
            // Given
            setField(validator, "signatureValidationEnabled", false);

            // When
            boolean result = validator.validateSignature(null, TEST_PAYLOAD, TEST_SELLER_ID);

            // Then
            assertThat(result)
                    .as("Validation disabled should accept null signature")
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("generateEventId")
    class GenerateEventId {

        @Test
        @DisplayName("should generate consistent event ID for same inputs")
        void shouldGenerateConsistentEventId() {
            // Given
            Long timestamp = 1700000000000L;

            // When
            String eventId1 = validator.generateEventId(TEST_SELLER_ID, "TY-ORDER-001", "Delivered", timestamp);
            String eventId2 = validator.generateEventId(TEST_SELLER_ID, "TY-ORDER-001", "Delivered", timestamp);

            // Then
            assertThat(eventId1)
                    .as("Same inputs should produce same event ID")
                    .isEqualTo(eventId2);
        }

        @Test
        @DisplayName("should generate different event IDs for different order numbers")
        void shouldGenerateDifferentEventIdsForDifferentOrders() {
            // Given
            Long timestamp = 1700000000000L;

            // When
            String eventId1 = validator.generateEventId(TEST_SELLER_ID, "TY-ORDER-001", "Delivered", timestamp);
            String eventId2 = validator.generateEventId(TEST_SELLER_ID, "TY-ORDER-002", "Delivered", timestamp);

            // Then
            assertThat(eventId1)
                    .as("Different order numbers should produce different event IDs")
                    .isNotEqualTo(eventId2);
        }

        @Test
        @DisplayName("should generate different event IDs for different statuses")
        void shouldGenerateDifferentEventIdsForDifferentStatuses() {
            // Given
            Long timestamp = 1700000000000L;

            // When
            String eventId1 = validator.generateEventId(TEST_SELLER_ID, "TY-ORDER-001", "Created", timestamp);
            String eventId2 = validator.generateEventId(TEST_SELLER_ID, "TY-ORDER-001", "Delivered", timestamp);

            // Then
            assertThat(eventId1)
                    .as("Different statuses should produce different event IDs")
                    .isNotEqualTo(eventId2);
        }

        @Test
        @DisplayName("should handle null inputs gracefully")
        void shouldHandleNullInputs() {
            // When
            String eventId = validator.generateEventId(null, null, null, null);

            // Then
            assertThat(eventId)
                    .as("Null inputs should not cause NPE")
                    .isNotNull()
                    .isNotEmpty();
        }

        @Test
        @DisplayName("should generate non-empty event ID")
        void shouldGenerateNonEmptyEventId() {
            // When
            String eventId = validator.generateEventId(TEST_SELLER_ID, "ORDER-1", "Delivered", 1700000000000L);

            // Then
            assertThat(eventId)
                    .as("Event ID should not be blank")
                    .isNotBlank();
        }
    }
}
