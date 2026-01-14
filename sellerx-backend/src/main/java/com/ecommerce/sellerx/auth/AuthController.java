package com.ecommerce.sellerx.auth;

import com.ecommerce.sellerx.config.CookieConfig;
import com.ecommerce.sellerx.users.UserDto;
import com.ecommerce.sellerx.users.UserMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final JwtConfig jwtConfig;
    private final CookieConfig cookieConfig;
    private final UserMapper userMapper;
    private final AuthService authService;

    @PostMapping("/login")
    public JwtResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        var loginResult = authService.login(request);

        var refreshToken = loginResult.getRefreshToken().toString();
        var refresCookie = new Cookie("refreshToken", refreshToken);
        refresCookie.setHttpOnly(cookieConfig.isHttpOnly());
        refresCookie.setPath("/");
        refresCookie.setMaxAge(jwtConfig.getRefreshTokenExpiration());
        refresCookie.setSecure(cookieConfig.isSecure());
        response.addCookie(refresCookie);

        var accessToken = loginResult.getAccessToken().toString();
        var accessCookie = new Cookie("access_token", accessToken);
        accessCookie.setHttpOnly(cookieConfig.isHttpOnly());
        accessCookie.setPath("/");
        accessCookie.setMaxAge(jwtConfig.getAccessTokenExpiration());
        accessCookie.setSecure(cookieConfig.isSecure());
        response.addCookie(accessCookie);

        return new JwtResponse(loginResult.getAccessToken().toString());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        // access_token cookie'sini sil
        Cookie accessTokenCookie = new Cookie("access_token", "");
        accessTokenCookie.setHttpOnly(cookieConfig.isHttpOnly());
        accessTokenCookie.setSecure(cookieConfig.isSecure());
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(0); // Silinsin
        response.addCookie(accessTokenCookie);

        // refreshToken cookie'sini sil - path "/" olmalı (login sırasındaki ile aynı)
        Cookie refreshTokenCookie = new Cookie("refreshToken", "");
        refreshTokenCookie.setHttpOnly(cookieConfig.isHttpOnly());
        refreshTokenCookie.setSecure(cookieConfig.isSecure());
        refreshTokenCookie.setPath("/"); // Login sırasında "/" kullanıldı, logout'ta da aynı olmalı
        refreshTokenCookie.setMaxAge(0); // Silinsin
        response.addCookie(refreshTokenCookie);

        // selected_store_id cookie'sini de sil
        Cookie selectedStoreCookie = new Cookie("selected_store_id", "");
        selectedStoreCookie.setHttpOnly(false); // Bu cookie frontend'den okunabilir olmalı
        selectedStoreCookie.setSecure(cookieConfig.isSecure());
        selectedStoreCookie.setPath("/");
        selectedStoreCookie.setMaxAge(0); // Silinsin
        response.addCookie(selectedStoreCookie);

        return ResponseEntity.ok().build();
    }


    @PostMapping("/refresh")
    public JwtResponse refresh(@CookieValue(value = "refreshToken") String refreshToken, HttpServletResponse response) {
        var accessToken = authService.refreshAccessToken(refreshToken);
        var accessCookie = new Cookie("access_token", accessToken.toString());
        accessCookie.setHttpOnly(cookieConfig.isHttpOnly());
        accessCookie.setPath("/");
        accessCookie.setMaxAge(jwtConfig.getAccessTokenExpiration());
        accessCookie.setSecure(cookieConfig.isSecure());
        response.addCookie(accessCookie);
        return new JwtResponse(accessToken.toString());
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me() {
        var user = authService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        var userDto = userMapper.toDto(user);
        return ResponseEntity.ok(userDto);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Void> handleBadCredentialsException() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}

