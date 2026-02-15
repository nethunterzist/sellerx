package com.ecommerce.sellerx.auth;

import com.ecommerce.sellerx.users.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for email verification flow.
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    /**
     * Verify email using token (public endpoint).
     */
    @GetMapping("/verify-email")
    public ResponseEntity<VerifyEmailResponse> verifyEmail(@RequestParam String token) {
        var result = emailVerificationService.verifyEmail(token);

        if (result.success()) {
            if (result.alreadyVerified()) {
                return ResponseEntity.ok(new VerifyEmailResponse(
                        true, true, null, "Email is already verified."
                ));
            }
            return ResponseEntity.ok(new VerifyEmailResponse(
                    true, false, result.email(), "Email verified successfully."
            ));
        } else {
            return ResponseEntity.badRequest().body(new VerifyEmailResponse(
                    false, false, null, result.error()
            ));
        }
    }

    /**
     * Resend verification email (authenticated endpoint).
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<ResendResponse> resendVerification(
            @AuthenticationPrincipal User user) {

        var result = emailVerificationService.resendVerificationEmail(user.getId());

        if (result.success()) {
            return ResponseEntity.ok(new ResendResponse(
                    true, "Verification email sent.", 0
            ));
        } else if (result.alreadyVerified()) {
            return ResponseEntity.ok(new ResendResponse(
                    true, "Email is already verified.", 0
            ));
        } else if (result.rateLimited()) {
            return ResponseEntity.status(429).body(new ResendResponse(
                    false, "Please wait before requesting another email.",
                    result.retryAfterSeconds()
            ));
        }

        return ResponseEntity.badRequest().body(new ResendResponse(
                false, "Failed to send verification email.", 0
        ));
    }

    /**
     * Check email verification status (authenticated endpoint).
     */
    @GetMapping("/verification-status")
    public ResponseEntity<VerificationStatusResponse> getVerificationStatus(
            @AuthenticationPrincipal User user) {

        boolean isVerified = emailVerificationService.isEmailVerified(user.getId());
        return ResponseEntity.ok(new VerificationStatusResponse(isVerified));
    }

    // Response DTOs

    public record VerifyEmailResponse(
            boolean success,
            boolean alreadyVerified,
            String email,
            String message
    ) {}

    public record ResendResponse(
            boolean success,
            String message,
            long retryAfterSeconds
    ) {}

    public record VerificationStatusResponse(
            boolean verified
    ) {}
}
