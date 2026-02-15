package com.ecommerce.sellerx.auth;

import com.ecommerce.sellerx.email.event.EmailVerificationEvent;
import com.ecommerce.sellerx.users.User;
import com.ecommerce.sellerx.users.UserNotFoundException;
import com.ecommerce.sellerx.users.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Service for handling email verification flow.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    private static final int TOKEN_LENGTH = 48; // 48 bytes = 64 chars base64
    private static final int MIN_MINUTES_BETWEEN_RESEND = 2;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Send verification email to user.
     * Called during registration or on resend request.
     */
    @Transactional
    public boolean sendVerificationEmail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        if (user.getEmailVerified()) {
            log.info("[EMAIL-VERIFY] User already verified: userId={}", userId);
            return false;
        }

        // Rate limiting: check if we sent recently
        if (user.getEmailVerificationSentAt() != null) {
            OffsetDateTime canSendAfter = user.getEmailVerificationSentAt()
                    .plusMinutes(MIN_MINUTES_BETWEEN_RESEND);
            if (OffsetDateTime.now().isBefore(canSendAfter)) {
                log.info("[EMAIL-VERIFY] Rate limited for userId={}", userId);
                return false;
            }
        }

        // Generate new token
        String token = generateSecureToken();
        user.setEmailVerificationToken(token);
        user.setEmailVerificationSentAt(OffsetDateTime.now());
        userRepository.save(user);

        // Build verification link
        String verificationLink = frontendUrl + "/verify-email?token=" + token;

        // Publish event to send email
        eventPublisher.publishEvent(new EmailVerificationEvent(
                this,
                user.getId(),
                user.getEmail(),
                user.getName(),
                token,
                verificationLink
        ));

        log.info("[EMAIL-VERIFY] Verification email sent for userId={}", userId);
        return true;
    }

    /**
     * Resend verification email for a user.
     */
    @Transactional
    public ResendResult resendVerificationEmail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        if (user.getEmailVerified()) {
            return ResendResult.ofAlreadyVerified();
        }

        // Rate limiting
        if (user.getEmailVerificationSentAt() != null) {
            OffsetDateTime canSendAfter = user.getEmailVerificationSentAt()
                    .plusMinutes(MIN_MINUTES_BETWEEN_RESEND);
            if (OffsetDateTime.now().isBefore(canSendAfter)) {
                long secondsRemaining = java.time.Duration.between(
                        OffsetDateTime.now(), canSendAfter).getSeconds();
                return ResendResult.ofRateLimited(secondsRemaining);
            }
        }

        sendVerificationEmail(userId);
        return ResendResult.ofSuccess();
    }

    /**
     * Verify email using token.
     */
    @Transactional
    public VerificationResult verifyEmail(String token) {
        if (token == null || token.isBlank()) {
            return VerificationResult.ofInvalid("Token is required");
        }

        Optional<User> userOpt = userRepository.findByEmailVerificationToken(token);

        if (userOpt.isEmpty()) {
            log.warn("[EMAIL-VERIFY] Invalid token used");
            return VerificationResult.ofInvalid("Invalid or expired token");
        }

        User user = userOpt.get();

        if (user.getEmailVerificationSentAt() != null) {
            OffsetDateTime expiryTime = user.getEmailVerificationSentAt().plusHours(24);
            if (OffsetDateTime.now().isAfter(expiryTime)) {
                return VerificationResult.ofInvalid("Verification token has expired. Please request a new one.");
            }
        }

        if (user.getEmailVerified()) {
            return VerificationResult.ofAlreadyVerified();
        }

        // Mark as verified
        user.setEmailVerified(true);
        user.setEmailVerificationToken(null); // Clear token after use
        userRepository.save(user);

        log.info("[EMAIL-VERIFY] Email verified for userId={}", user.getId());
        return VerificationResult.ofSuccess(user.getEmail());
    }

    /**
     * Check if a user's email is verified.
     */
    @Transactional(readOnly = true)
    public boolean isEmailVerified(Long userId) {
        return userRepository.findById(userId)
                .map(User::getEmailVerified)
                .orElse(false);
    }

    /**
     * Generate a secure random token.
     */
    private String generateSecureToken() {
        byte[] bytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // Result classes
    public record VerificationResult(boolean success, boolean alreadyVerified, String email, String error) {
        public static VerificationResult ofSuccess(String email) {
            return new VerificationResult(true, false, email, null);
        }
        public static VerificationResult ofAlreadyVerified() {
            return new VerificationResult(true, true, null, null);
        }
        public static VerificationResult ofInvalid(String error) {
            return new VerificationResult(false, false, null, error);
        }
    }

    public record ResendResult(boolean success, boolean alreadyVerified, boolean rateLimited, long retryAfterSeconds) {
        public static ResendResult ofSuccess() {
            return new ResendResult(true, false, false, 0);
        }
        public static ResendResult ofAlreadyVerified() {
            return new ResendResult(false, true, false, 0);
        }
        public static ResendResult ofRateLimited(long retryAfterSeconds) {
            return new ResendResult(false, false, true, retryAfterSeconds);
        }
    }
}
