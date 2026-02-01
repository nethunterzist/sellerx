package com.ecommerce.sellerx.users;

import com.ecommerce.sellerx.auth.JwtService;
import com.ecommerce.sellerx.config.CookieConfig;
import com.ecommerce.sellerx.stores.StoreService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final JwtService jwtService;
    private final CookieConfig cookieConfig;
    private final StoreService storeService;

    @GetMapping
    public Iterable<UserDto> getAllUsers(
            @RequestParam(required = false, defaultValue = "", name = "sort") String sortBy
    ) {
        return userService.getAllUsers(sortBy);
    }

    @GetMapping("/{id}")
    public UserDto getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }

    @PostMapping
    public ResponseEntity<?> registerUser(
            @Valid @RequestBody RegisterUserRequest request,
            UriComponentsBuilder uriBuilder) {

        var userDto = userService.registerUser(request);
        var uri = uriBuilder.path("/users/{id}").buildAndExpand(userDto.getId()).toUri();
        return ResponseEntity.created(uri).body(userDto);
    }

    @PutMapping("/{id}")
    public UserDto updateUser(
            @PathVariable(name = "id") Long id,
            @RequestBody UpdateUserRequest request) {
        return userService.updateUser(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }

    @PostMapping("/{id}/change-password")
    public void changePassword(
            @PathVariable Long id,
            @RequestBody ChangePasswordRequest request) {
        userService.changePassword(id, request);
    }

    @GetMapping("/selected-store")
    public ResponseEntity<?> getSelectedStore(HttpServletRequest request, HttpServletResponse response) {
        try {
            Long userId = jwtService.getUserIdFromToken(request);
            UUID selectedStoreId = userService.getSelectedStoreId(userId);
            
            // DB'deki değeri cookie'ye de yaz (sync için)
            if (selectedStoreId != null) {
                var storeIdCookie = new Cookie("selected_store_id", selectedStoreId.toString());
                storeIdCookie.setHttpOnly(false); // Frontend'den okunabilir olmalı
                storeIdCookie.setPath("/");
                storeIdCookie.setMaxAge(30 * 24 * 60 * 60); // 30 days
                storeIdCookie.setSecure(cookieConfig.isSecure());
                response.addCookie(storeIdCookie);
            } else {
                // DB'de store yoksa cookie'yi de sil
                var storeIdCookie = new Cookie("selected_store_id", "");
                storeIdCookie.setHttpOnly(false); // Frontend'den okunabilir olmalı
                storeIdCookie.setPath("/");
                storeIdCookie.setMaxAge(0);
                storeIdCookie.setSecure(cookieConfig.isSecure());
                response.addCookie(storeIdCookie);
            }
            
            return ResponseEntity.ok(Map.of("selectedStoreId", selectedStoreId != null ? selectedStoreId.toString() : ""));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid token"));
        }
    }

    // ==================== PROFILE ENDPOINTS ====================

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(HttpServletRequest request) {
        try {
            Long userId = jwtService.getUserIdFromToken(request);
            UserDto userDto = userService.getUser(userId);
            return ResponseEntity.ok(userDto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid token"));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestBody UpdateUserRequest updateRequest,
            HttpServletRequest request) {
        try {
            Long userId = jwtService.getUserIdFromToken(request);
            UserDto updated = userService.updateUser(userId, updateRequest);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid token"));
        }
    }

    // ==================== PASSWORD ENDPOINT ====================

    @PutMapping("/password")
    public ResponseEntity<?> changePasswordWithToken(
            @RequestBody ChangePasswordRequest changeRequest,
            HttpServletRequest request) {
        try {
            Long userId = jwtService.getUserIdFromToken(request);
            userService.changePassword(userId, changeRequest);
            return ResponseEntity.ok(Map.of("success", true, "message", "Password changed successfully"));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Current password is incorrect"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid token"));
        }
    }

    // ==================== PREFERENCES ENDPOINTS ====================

    @GetMapping("/preferences")
    public ResponseEntity<?> getPreferences(HttpServletRequest request) {
        try {
            Long userId = jwtService.getUserIdFromToken(request);
            UserPreferences preferences = userService.getPreferences(userId);
            return ResponseEntity.ok(preferences);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid token"));
        }
    }

    @PutMapping("/preferences")
    public ResponseEntity<?> updatePreferences(
            @RequestBody UpdatePreferencesRequest updateRequest,
            HttpServletRequest request) {
        try {
            Long userId = jwtService.getUserIdFromToken(request);
            UserPreferences updated = userService.updatePreferences(userId, updateRequest);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid token"));
        }
    }

    // ==================== SELECTED STORE ENDPOINTS ====================

    @PostMapping("/selected-store")
    public ResponseEntity<?> setSelectedStore(@RequestBody Map<String, String> request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            Long userId = jwtService.getUserIdFromToken(httpRequest);
            String storeIdString = request.get("storeId");
            
            if (storeIdString == null || storeIdString.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Store ID is required"));
            }
            
            UUID storeId;
            try {
                storeId = UUID.fromString(storeIdString);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid store ID format"));
            }
            
            // Store'un bu user'a ait olduğunu kontrol et
            if (!storeService.isStoreOwnedByUser(storeId, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied"));
            }
            
            // User'ın selected store'unu güncelle
            userService.setSelectedStoreId(userId, storeId);
            
            // Cookie set et (AuthController.login() gibi)
            var storeIdCookie = new Cookie("selected_store_id", storeId.toString());
            storeIdCookie.setHttpOnly(false); // Client-side erişim için (frontend'den okunabilir olmalı)
            storeIdCookie.setPath("/");
            storeIdCookie.setMaxAge(30 * 24 * 60 * 60); // 30 days
            storeIdCookie.setSecure(cookieConfig.isSecure());
            httpResponse.addCookie(storeIdCookie);
            
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid token"));
        }
    }

    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateUser() {
        return ResponseEntity.badRequest().body(
                Map.of("email", "Email is already registered.")
        );
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Void> handleUserNotFound() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Void> handleAccessDenied() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
