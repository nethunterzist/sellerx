package com.ecommerce.sellerx.auth;

import com.ecommerce.sellerx.activitylog.ActivityLogService;
import com.ecommerce.sellerx.config.CookieConfig;
import com.ecommerce.sellerx.users.UserDto;
import com.ecommerce.sellerx.users.UserMapper;
import com.ecommerce.sellerx.users.UserRepository;
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

@AllArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final JwtConfig jwtConfig;
    private final CookieConfig cookieConfig;
    private final UserMapper userMapper;
    private final AuthService authService;
    private final ActivityLogService activityLogService;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public JwtResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {

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

        return new JwtResponse(loginResult.getAccessToken().toString());
    }

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


    @PostMapping("/refresh")
    public JwtResponse refresh(@CookieValue(value = "refreshToken") String refreshToken, HttpServletResponse response) {
        var accessToken = authService.refreshAccessToken(refreshToken);
        ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken.toString())
                .httpOnly(cookieConfig.isHttpOnly())
                .secure(cookieConfig.isSecure())
                .sameSite(cookieConfig.getSameSite())
                .path("/")
                .maxAge(Duration.ofSeconds(jwtConfig.getAccessTokenExpiration()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        return new JwtResponse(accessToken.toString());
    }

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
}

