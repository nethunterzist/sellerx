package com.ecommerce.sellerx.common.encryption;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption service for sensitive data like API credentials.
 * Uses 12-byte IV (recommended for GCM) and 128-bit authentication tag.
 */
@Service
@Slf4j
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;
    private final boolean encryptionEnabled;

    public EncryptionService(
            @Value("${app.encryption.key:}") String encryptionKey,
            @Value("${app.encryption.enabled:false}") boolean encryptionEnabled) {
        this.encryptionEnabled = encryptionEnabled;
        this.secureRandom = new SecureRandom();

        if (encryptionEnabled && (encryptionKey == null || encryptionKey.isEmpty())) {
            log.warn("Encryption is enabled but no key provided - using fallback key (NOT SECURE FOR PRODUCTION)");
            encryptionKey = "sellerx-dev-encryption-key-32ch"; // 32 chars = 256 bits
        }

        if (encryptionKey != null && !encryptionKey.isEmpty()) {
            // Ensure key is exactly 32 bytes (256 bits) for AES-256
            byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length != 32) {
                byte[] paddedKey = new byte[32];
                System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
                keyBytes = paddedKey;
            }
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
        } else {
            this.secretKey = null;
        }
    }

    /**
     * Encrypt a plaintext string using AES-256-GCM.
     * Returns Base64-encoded ciphertext with IV prepended.
     *
     * @param plaintext The text to encrypt
     * @return Base64-encoded encrypted text, or original if encryption disabled
     */
    public String encrypt(String plaintext) {
        if (!encryptionEnabled || secretKey == null || plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }

        // Don't re-encrypt already encrypted data
        if (isEncrypted(plaintext)) {
            return plaintext;
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to ciphertext
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);

            // Add prefix to identify encrypted data
            return "ENC:" + Base64.getEncoder().encodeToString(byteBuffer.array());

        } catch (Exception e) {
            log.error("Encryption failed: {}", e.getMessage());
            throw new RuntimeException("Failed to encrypt data", e);
        }
    }

    /**
     * Decrypt a Base64-encoded ciphertext using AES-256-GCM.
     *
     * @param encryptedText The Base64-encoded encrypted text (with ENC: prefix)
     * @return Decrypted plaintext, or original if not encrypted or decryption disabled
     */
    public String decrypt(String encryptedText) {
        if (!encryptionEnabled || secretKey == null || encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        // Only decrypt data with our prefix
        if (!isEncrypted(encryptedText)) {
            return encryptedText;
        }

        try {
            // Remove prefix
            String base64Data = encryptedText.substring(4);
            byte[] encryptedBytes = Base64.getDecoder().decode(base64Data);

            // Extract IV and ciphertext
            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedBytes);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] ciphertext = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Decryption failed: {}", e.getMessage());
            throw new RuntimeException("Failed to decrypt data", e);
        }
    }

    /**
     * Check if a string is already encrypted (has ENC: prefix).
     */
    public boolean isEncrypted(String text) {
        return text != null && text.startsWith("ENC:");
    }

    /**
     * Check if encryption is enabled.
     */
    public boolean isEnabled() {
        return encryptionEnabled && secretKey != null;
    }
}
