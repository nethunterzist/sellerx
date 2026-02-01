package com.ecommerce.sellerx.users;

import com.ecommerce.sellerx.referral.ReferralService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@AllArgsConstructor
@Service
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ReferralService referralService;

    public Iterable<UserDto> getAllUsers(String sortBy) {
        if (!Set.of("name", "email").contains(sortBy))
            sortBy = "name";

        return userRepository.findAll(Sort.by(sortBy))
                .stream()
                .map(userMapper::toDto)
                .toList();
    }

    public UserDto getUser(Long userId) {
        var user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        return userMapper.toDto(user);
    }

    /**
     * Find user by ID - used by billing module
     */
    public java.util.Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    /**
     * Find user by email - used by billing module
     */
    public java.util.Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public UserDto registerUser(RegisterUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateUserException();
        }

        var user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Role.USER);
        userRepository.save(user);

        // Record referral if code was provided during registration
        if (request.getReferralCode() != null && !request.getReferralCode().isBlank()) {
            try {
                referralService.recordReferral(user.getId(), request.getReferralCode().toUpperCase());
            } catch (Exception e) {
                log.warn("Referral recording failed for user {}: {}", user.getId(), e.getMessage());
                // Don't block registration if referral fails
            }
        }

        return userMapper.toDto(user);
    }

    public UserDto updateUser(Long userId, UpdateUserRequest request) {
        var user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        userMapper.update(request, user);
        userRepository.save(user);

        return userMapper.toDto(user);
    }

    public void deleteUser(Long userId) {
        var user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        userRepository.delete(user);
    }

    public void changePassword(Long userId, ChangePasswordRequest request) {
        var user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new AccessDeniedException("Password does not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public UUID getSelectedStoreId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException());
        return user.getSelectedStoreId();
    }

    public void setSelectedStoreId(Long userId, UUID storeId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException());
        user.setSelectedStoreId(storeId);
        userRepository.save(user);
    }

    /**
     * Get user preferences (language, theme, currency, notifications)
     */
    public UserPreferences getPreferences(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        UserPreferences prefs = user.getPreferences();
        return prefs != null ? prefs : new UserPreferences();
    }

    /**
     * Update user preferences (partial update supported)
     */
    public UserPreferences updatePreferences(Long userId, UpdatePreferencesRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        UserPreferences prefs = user.getPreferences();
        if (prefs == null) {
            prefs = new UserPreferences();
        }

        // Update only provided fields
        if (request.getLanguage() != null) {
            prefs.setLanguage(request.getLanguage());
        }
        if (request.getTheme() != null) {
            prefs.setTheme(request.getTheme());
        }
        if (request.getCurrency() != null) {
            prefs.setCurrency(request.getCurrency());
        }
        if (request.getNotifications() != null) {
            UserPreferences.NotificationPreferences notifPrefs = prefs.getNotifications();
            if (notifPrefs == null) {
                notifPrefs = new UserPreferences.NotificationPreferences();
            }
            UpdatePreferencesRequest.NotificationUpdate notifUpdate = request.getNotifications();
            if (notifUpdate.getEmail() != null) {
                notifPrefs.setEmail(notifUpdate.getEmail());
            }
            if (notifUpdate.getPush() != null) {
                notifPrefs.setPush(notifUpdate.getPush());
            }
            if (notifUpdate.getOrderUpdates() != null) {
                notifPrefs.setOrderUpdates(notifUpdate.getOrderUpdates());
            }
            if (notifUpdate.getStockAlerts() != null) {
                notifPrefs.setStockAlerts(notifUpdate.getStockAlerts());
            }
            if (notifUpdate.getWeeklyReport() != null) {
                notifPrefs.setWeeklyReport(notifUpdate.getWeeklyReport());
            }
            prefs.setNotifications(notifPrefs);
        }

        // Update sync interval (for frontend auto-refresh)
        if (request.getSyncInterval() != null) {
            // Validate: only allowed values are 0, 30, 60, 120, 300
            Integer interval = request.getSyncInterval();
            if (interval == 0 || interval == 30 || interval == 60 || interval == 120 || interval == 300) {
                prefs.setSyncInterval(interval);
            }
        }

        user.setPreferences(prefs);
        userRepository.save(user);
        return prefs;
    }
}
