package com.ecommerce.sellerx.admin;

import com.ecommerce.sellerx.admin.dto.AdminUserDto;
import com.ecommerce.sellerx.admin.dto.AdminUserListDto;
import com.ecommerce.sellerx.admin.dto.ChangeRoleRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * Get paginated list of all users
     * GET /api/admin/users?page=0&size=20&sort=id,desc
     */
    @GetMapping
    public ResponseEntity<Page<AdminUserListDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort) {

        size = Math.min(size, 100);
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Set<String> allowedSortFields = Set.of("id", "name", "email", "createdAt", "lastLoginAt", "role");
        String sortField = allowedSortFields.contains(sortParams[0]) ? sortParams[0] : "id";
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        log.info("Admin fetching users - page: {}, size: {}, sort: {}", page, size, sort);
        Page<AdminUserListDto> users = adminUserService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    /**
     * Get user details by ID
     * GET /api/admin/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdminUserDto> getUserById(@PathVariable Long id) {
        log.info("Admin fetching user details for id: {}", id);
        AdminUserDto user = adminUserService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * Change user role
     * PUT /api/admin/users/{id}/role
     */
    @PutMapping("/{id}/role")
    public ResponseEntity<AdminUserDto> changeUserRole(
            @PathVariable Long id,
            @Valid @RequestBody ChangeRoleRequest request) {

        log.info("Admin changing role for user {} to {}", id, request.getRole());
        AdminUserDto user = adminUserService.changeUserRole(id, request.getRole());
        return ResponseEntity.ok(user);
    }

    /**
     * Search users by email or name with pagination
     * GET /api/admin/users/search?q=query&page=0&size=20
     */
    @GetMapping("/search")
    public ResponseEntity<Page<AdminUserListDto>> searchUsers(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        log.info("Admin searching users with query: {}, page: {}, size: {}", q, page, size);
        Page<AdminUserListDto> users = adminUserService.searchUsers(q, pageable);
        return ResponseEntity.ok(users);
    }

    /**
     * Export all users for Excel
     * GET /api/admin/users/export
     */
    @GetMapping("/export")
    public ResponseEntity<List<AdminUserListDto>> exportAllUsers() {
        log.info("Admin exporting all users");
        List<AdminUserListDto> users = adminUserService.getAllUsersForExport();
        return ResponseEntity.ok(users);
    }

    /**
     * Generate impersonation token to view user's account (read-only)
     * POST /api/admin/users/{id}/impersonate
     */
    @PostMapping("/{id}/impersonate")
    public ResponseEntity<Map<String, String>> impersonateUser(
            @PathVariable Long id,
            HttpServletRequest request) {

        Long adminUserId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        log.info("Admin {} requesting impersonation of user {}", adminUserId, id);
        String token = adminUserService.generateImpersonationToken(id, adminUserId, request);
        return ResponseEntity.ok(Map.of("token", token));
    }
}
