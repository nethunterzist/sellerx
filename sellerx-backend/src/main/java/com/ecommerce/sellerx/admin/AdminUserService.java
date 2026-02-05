package com.ecommerce.sellerx.admin;

import com.ecommerce.sellerx.admin.dto.AdminUserDto;
import com.ecommerce.sellerx.admin.dto.AdminUserListDto;
import com.ecommerce.sellerx.auth.JwtService;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.users.Role;
import com.ecommerce.sellerx.users.User;
import com.ecommerce.sellerx.users.UserNotFoundException;
import com.ecommerce.sellerx.users.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final JwtService jwtService;
    private final ImpersonationLogRepository impersonationLogRepository;

    /**
     * Get paginated list of all users with basic info
     */
    public Page<AdminUserListDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::toListDto);
    }

    /**
     * Get detailed user info by ID
     */
    public AdminUserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        return toDetailDto(user);
    }

    /**
     * Change user role
     */
    @Transactional
    public AdminUserDto changeUserRole(Long id, Role newRole) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        log.info("Changing role for user {} from {} to {}", user.getEmail(), user.getRole(), newRole);
        user.setRole(newRole);
        user = userRepository.save(user);

        return toDetailDto(user);
    }

    /**
     * Get total user count
     */
    public long getTotalUserCount() {
        return userRepository.count();
    }

    /**
     * Search users by email or name
     */
    public List<AdminUserListDto> searchUsers(String query) {
        // Simple implementation - can be enhanced with Specification
        return userRepository.findAll().stream()
                .filter(user -> user.getEmail().toLowerCase().contains(query.toLowerCase())
                        || (user.getName() != null && user.getName().toLowerCase().contains(query.toLowerCase())))
                .map(this::toListDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all users for export (no pagination)
     */
    public List<AdminUserListDto> getAllUsersForExport() {
        log.info("Exporting all users");
        return userRepository.findAll().stream()
                .map(this::toListDto)
                .collect(Collectors.toList());
    }

    /**
     * Generate impersonation token for viewing target user's account
     */
    @Transactional
    public String generateImpersonationToken(Long targetUserId, Long adminUserId, HttpServletRequest request) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + targetUserId));

        log.info("Admin {} starting impersonation of user {} ({})", adminUserId, targetUserId, targetUser.getEmail());

        ImpersonationLog auditLog = ImpersonationLog.builder()
                .adminUserId(adminUserId)
                .targetUserId(targetUserId)
                .action("START")
                .ipAddress(getClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .build();
        impersonationLogRepository.save(auditLog);

        return jwtService.generateImpersonationToken(targetUser, adminUserId).toString();
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private AdminUserListDto toListDto(User user) {
        List<Store> stores = storeRepository.findAllByUser(user);
        return AdminUserListDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .storeCount(stores.size())
                .createdAt(null) // User entity doesn't have createdAt - can be added later
                .lastLoginAt(null) // Can be fetched from activity_logs
                .subscriptionStatus(null) // Can be fetched from billing module
                .planName(null) // Can be fetched from billing module
                .build();
    }

    private AdminUserDto toDetailDto(User user) {
        List<Store> stores = storeRepository.findAllByUser(user);

        List<AdminUserDto.UserStoreInfo> storeInfos = stores.stream()
                .map(store -> AdminUserDto.UserStoreInfo.builder()
                        .id(store.getId())
                        .storeName(store.getStoreName())
                        .marketplace(store.getMarketplace())
                        .syncStatus(store.getSyncStatus())
                        .initialSyncCompleted(store.getInitialSyncCompleted())
                        .createdAt(store.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return AdminUserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .selectedStoreId(user.getSelectedStoreId())
                .storeCount(stores.size())
                .stores(storeInfos)
                .createdAt(null) // User entity doesn't have createdAt
                .lastLoginAt(null) // Can be fetched from activity_logs
                .subscriptionStatus(null) // Can be fetched from billing module
                .planName(null) // Can be fetched from billing module
                .build();
    }
}
