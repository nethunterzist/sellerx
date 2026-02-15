package com.ecommerce.sellerx.auth;

import com.ecommerce.sellerx.email.event.PasswordResetRequestedEvent;
import com.ecommerce.sellerx.users.User;
import com.ecommerce.sellerx.users.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Service for handling password reset flow.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    private static final int TOKEN_LENGTH = 48; // 48 bytes = 64 chars base64
    private static final int TOKEN_VALIDITY_HOURS = 1;
    private static final int MIN_MINUTES_BETWEEN_REQUESTS = 2;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Request a password reset for the given email.
     * Returns true if email was sent, false if user not found or rate limited.
     * Always returns success message to prevent email enumeration attacks.
     */
    @Transactional
    public boolean requestPasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email.toLowerCase().trim());

        if (userOpt.isEmpty()) {
            log.info("[PASSWORD-RESET] Request for unknown email: {}", maskEmail(email));
            return false; // Don't reveal if email exists
        }

        User user = userOpt.get();

        // Rate limiting: check if user has a recent token
        OffsetDateTime since = OffsetDateTime.now().minusMinutes(MIN_MINUTES_BETWEEN_REQUESTS);
        if (tokenRepository.hasRecentToken(user.getId(), since)) {
            log.info("[PASSWORD-RESET] Rate limited for userId={}", user.getId());
            return false;
        }

        // Invalidate any existing tokens for this user
        tokenRepository.invalidateAllTokensForUser(user.getId());

        // Generate new token
        String token = generateSecureToken();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(TOKEN_VALIDITY_HOURS);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(expiresAt)
                .build();

        tokenRepository.save(resetToken);

        // Build reset link
        String resetLink = frontendUrl + "/reset-password?token=" + token;

        // Publish event to send email
        eventPublisher.publishEvent(new PasswordResetRequestedEvent(
                this,
                user.getId(),
                user.getEmail(),
                user.getName(),
                token,
                resetLink
        ));

        log.info("[PASSWORD-RESET] Token created for userId={}, expiresAt={}", user.getId(), expiresAt);
        return true;
    }

    /**
     * Verify if a token is valid (exists, not expired, not used).
     */
    @Transactional(readOnly = true)
    public TokenValidationResult validateToken(String token) {
        if (token == null || token.isBlank()) {
            return TokenValidationResult.ofInvalid("Token is required");
        }

        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            return TokenValidationResult.ofInvalid("Invalid or expired token");
        }

        PasswordResetToken resetToken = tokenOpt.get();

        if (resetToken.isUsed()) {
            return TokenValidationResult.ofInvalid("Token has already been used");
        }

        if (resetToken.isExpired()) {
            return TokenValidationResult.ofInvalid("Token has expired");
        }

        return TokenValidationResult.ofValid(resetToken.getUser().getEmail());
    }

    /**
     * Reset password using a valid token.
     */
    @Transactional
    public ResetPasswordResult resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            return ResetPasswordResult.ofFailure("Token is required");
        }

        if (newPassword == null || newPassword.length() < 6) {
            return ResetPasswordResult.ofFailure("Password must be at least 6 characters");
        }

        Optional<PasswordResetToken> tokenOpt = tokenRepository.findValidToken(token);

        if (tokenOpt.isEmpty()) {
            log.warn("[PASSWORD-RESET] Invalid/expired token used");
            return ResetPasswordResult.ofFailure("Invalid or expired token");
        }

        PasswordResetToken resetToken = tokenOpt.get();
        User user = resetToken.getUser();

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used
        resetToken.markAsUsed();
        tokenRepository.save(resetToken);

        // Invalidate all other tokens for this user
        tokenRepository.invalidateAllTokensForUser(user.getId());

        log.info("[PASSWORD-RESET] Password reset successful for userId={}", user.getId());
        return ResetPasswordResult.ofSuccess();
    }

    /**
     * Cleanup expired tokens (called by scheduled job).
     */
    @Transactional
    public int cleanupExpiredTokens() {
        // Delete tokens expired more than 7 days ago
        OffsetDateTime before = OffsetDateTime.now().minusDays(7);
        int deleted = tokenRepository.deleteExpiredTokensBefore(before);
        if (deleted > 0) {
            log.info("[PASSWORD-RESET] Cleaned up {} expired tokens", deleted);
        }
        return deleted;
    }

    /**
     * Generate a secure random token.
     */
    private String generateSecureToken() {
        byte[] bytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Mask email for logging (show first 2 chars and domain).
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) {
            return "***" + email.substring(atIndex);
        }
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    // Result classes
    public record TokenValidationResult(boolean valid, String email, String error) {
        public static TokenValidationResult ofValid(String email) {
            return new TokenValidationResult(true, email, null);
        }
        public static TokenValidationResult ofInvalid(String error) {
            return new TokenValidationResult(false, null, error);
        }
    }

    public record ResetPasswordResult(boolean success, String error) {
        public static ResetPasswordResult ofSuccess() {
            return new ResetPasswordResult(true, null);
        }
        public static ResetPasswordResult ofFailure(String error) {
            return new ResetPasswordResult(false, error);
        }
    }
}
