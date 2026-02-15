package com.ecommerce.sellerx.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for password reset flow.
 * All endpoints are public (no authentication required).
 * Rate limited to prevent abuse (3 requests per hour per IP).
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;
    private final AuthRateLimiter authRateLimiter;

    /**
     * Request a password reset email.
     * Rate limited to 3 requests per hour per IP.
     * Always returns success to prevent email enumeration attacks.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIp(httpRequest);

        // Check rate limit before processing
        if (!authRateLimiter.isPasswordResetAllowed(clientIp)) {
            log.warn("[PASSWORD-RESET] Rate limited IP: {}", maskIp(clientIp));
            // Still return success-like response to prevent enumeration
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new RateLimitResponse(
                            "Too many password reset requests. Please try again later.",
                            3600 // 1 hour
                    ));
        }

        // Record the attempt
        authRateLimiter.recordPasswordResetAttempt(clientIp);

        log.info("[PASSWORD-RESET] Forgot password request for email: {}", maskEmail(request.email()));

        // Always return success to prevent email enumeration
        passwordResetService.requestPasswordReset(request.email());

        return ResponseEntity.ok(new ForgotPasswordResponse(
                true,
                "If an account exists with this email, you will receive a password reset link."
        ));
    }

    /**
     * Verify if a reset token is valid.
     */
    @GetMapping("/verify-reset-token")
    public ResponseEntity<VerifyTokenResponse> verifyResetToken(
            @RequestParam String token) {

        var result = passwordResetService.validateToken(token);

        if (result.valid()) {
            return ResponseEntity.ok(new VerifyTokenResponse(true, result.email(), null));
        } else {
            return ResponseEntity.ok(new VerifyTokenResponse(false, null, result.error()));
        }
    }

    /**
     * Reset password using a valid token.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ResetPasswordResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        var result = passwordResetService.resetPassword(request.token(), request.newPassword());

        if (result.success()) {
            log.info("[PASSWORD-RESET] Password reset successful");
            return ResponseEntity.ok(new ResetPasswordResponse(true, "Password has been reset successfully.", null));
        } else {
            log.warn("[PASSWORD-RESET] Password reset failed: {}", result.error());
            return ResponseEntity.badRequest().body(new ResetPasswordResponse(false, null, result.error()));
        }
    }

    // Request/Response DTOs

    public record ForgotPasswordRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email
    ) {}

    public record ForgotPasswordResponse(
            boolean success,
            String message
    ) {}

    public record VerifyTokenResponse(
            boolean valid,
            String email,
            String error
    ) {}

    public record ResetPasswordRequest(
            @NotBlank(message = "Token is required")
            String token,

            @NotBlank(message = "Password is required")
            @Size(min = 6, message = "Password must be at least 6 characters")
            String newPassword
    ) {}

    public record ResetPasswordResponse(
            boolean success,
            String message,
            String error
    ) {}

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

    /**
     * Extract client IP address from request, handling proxies.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    /**
     * Mask IP address for logging (privacy).
     */
    private String maskIp(String ipAddress) {
        if (ipAddress == null) {
            return "unknown";
        }
        if (ipAddress.contains(".")) {
            int lastDot = ipAddress.lastIndexOf('.');
            return ipAddress.substring(0, lastDot) + ".***";
        }
        int lastColon = ipAddress.lastIndexOf(':');
        return lastColon > 0 ? ipAddress.substring(0, lastColon) + ":****" : ipAddress;
    }

    public record RateLimitResponse(
            String message,
            long retryAfterSeconds
    ) {}
}
