package com.ecommerce.sellerx.auth;

import com.ecommerce.sellerx.activitylog.ActivityLogService;
import com.ecommerce.sellerx.config.CookieConfig;
import com.ecommerce.sellerx.users.UserDto;
import com.ecommerce.sellerx.users.UserMapper;
import com.ecommerce.sellerx.users.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@AllArgsConstructor
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User authentication and token management")
public class AuthController {
    private final JwtConfig jwtConfig;
    private final CookieConfig cookieConfig;
    private final UserMapper userMapper;
    private final AuthService authService;
    private final ActivityLogService activityLogService;
    private final UserRepository userRepository;
    private final AuthRateLimiter authRateLimiter;

    @Operation(
            summary = "User login",
            description = "Authenticate user with email and password. Returns JWT access token and sets HTTP-only cookies for access_token and refreshToken."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = JwtResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content),
            @ApiResponse(responseCode = "429", description = "Too many login attempts", content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {

        String clientIp = getClientIp(httpRequest);

        // Check rate limit before processing
        if (!authRateLimiter.isLoginAllowed(clientIp)) {
            long resetSeconds = authRateLimiter.getLoginResetTimeSeconds(clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(resetSeconds))
                    .body(new RateLimitResponse(
                            "Too many login attempts. Please try again later.",
                            resetSeconds,
                            0
                    ));
        }

        // Record the attempt
        authRateLimiter.recordLoginAttempt(clientIp);

        var loginResult = authService.login(request);

        // Log successful login and update lastLoginAt
        var userId = loginResult.getAccessToken().getUserId();
        activityLogService.logLogin(userId, request.getEmail(), httpRequest);
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        });

        var refreshToken = loginResult.getRefreshToken().toString();
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(cookieConfig.isHttpOnly())
                .secure(cookieConfig.isSecure())
                .sameSite(cookieConfig.getSameSite())
                .path("/")
                .maxAge(Duration.ofSeconds(jwtConfig.getRefreshTokenExpiration()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        var accessToken = loginResult.getAccessToken().toString();
        ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken)
                .httpOnly(cookieConfig.isHttpOnly())
                .secure(cookieConfig.isSecure())
                .sameSite(cookieConfig.getSameSite())
                .path("/")
                .maxAge(Duration.ofSeconds(jwtConfig.getAccessTokenExpiration()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        return ResponseEntity.ok(new JwtResponse(loginResult.getAccessToken().toString()));
    }

    @Operation(
            summary = "User logout",
            description = "Logout user by clearing all authentication cookies (access_token, refreshToken, selected_store_id)."
    )
    @ApiResponse(responseCode = "200", description = "Logout successful")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest, HttpServletResponse response) {
        // Log logout (try to get user info from current context)
        try {
            var user = authService.getCurrentUser();
            if (user != null) {
                activityLogService.logLogout(user.getId(), user.getEmail(), httpRequest);
            }
        } catch (Exception ignored) {
            // If we can't get user info, still proceed with logout
        }

        // access_token cookie'sini sil
        ResponseCookie accessTokenCookie = ResponseCookie.from("access_token", "")
                .httpOnly(cookieConfig.isHttpOnly())
                .secure(cookieConfig.isSecure())
                .sameSite(cookieConfig.getSameSite())
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());

        // refreshToken cookie'sini sil
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(cookieConfig.isHttpOnly())
                .secure(cookieConfig.isSecure())
                .sameSite(cookieConfig.getSameSite())
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        // selected_store_id cookie'sini de sil
        ResponseCookie selectedStoreCookie = ResponseCookie.from("selected_store_id", "")
                .httpOnly(false)
                .secure(cookieConfig.isSecure())
                .sameSite(cookieConfig.getSameSite())
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, selectedStoreCookie.toString());

        return ResponseEntity.ok().build();
    }


    @Operation(
            summary = "Refresh access token",
            description = "Use refresh token from cookie to obtain a new access token. The new access token is set as HTTP-only cookie."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = JwtResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token", content = @Content)
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @Parameter(hidden = true) @CookieValue(value = "refreshToken") String refreshToken,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {

        String clientIp = getClientIp(httpRequest);
        if (!authRateLimiter.isRefreshAllowed(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("error", "Too many refresh attempts."));
        }
        authRateLimiter.recordRefreshAttempt(clientIp);

        var tokenPair = authService.refreshTokens(refreshToken);

        ResponseCookie accessCookie = ResponseCookie.from("access_token", tokenPair.getAccessToken().toString())
                .httpOnly(cookieConfig.isHttpOnly())
                .secure(cookieConfig.isSecure())
                .sameSite(cookieConfig.getSameSite())
                .path("/")
                .maxAge(Duration.ofSeconds(jwtConfig.getAccessTokenExpiration()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", tokenPair.getRefreshToken().toString())
                .httpOnly(cookieConfig.isHttpOnly())
                .secure(cookieConfig.isSecure())
                .sameSite(cookieConfig.getSameSite())
                .path("/")
                .maxAge(Duration.ofSeconds(jwtConfig.getRefreshTokenExpiration()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.ok(new JwtResponse(tokenPair.getAccessToken().toString()));
    }

    @Operation(
            summary = "Get current user",
            description = "Returns the authenticated user's profile. If user is being impersonated by admin, includes impersonation metadata."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User profile retrieved",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @GetMapping("/me")
    public ResponseEntity<UserDto> me(HttpServletRequest httpRequest) {
        var user = authService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        var userDto = userMapper.toDto(user);

        // Add impersonation metadata if present
        Long impersonatedBy = (Long) httpRequest.getAttribute("impersonatedBy");
        if (impersonatedBy != null) {
            userDto.setIsImpersonated(true);
            userDto.setImpersonatedBy(impersonatedBy);
            userDto.setReadOnly(true);
        }

        return ResponseEntity.ok(userDto);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Void> handleBadCredentialsException() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    /**
     * Extract client IP address from request, handling proxies.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take the first IP in the chain (original client)
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    /**
     * Rate limit response DTO.
     */
    public record RateLimitResponse(
            String message,
            long retryAfterSeconds,
            int remainingAttempts
    ) {}
}

