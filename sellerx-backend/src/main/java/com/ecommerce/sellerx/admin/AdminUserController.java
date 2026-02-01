package com.ecommerce.sellerx.admin;

import com.ecommerce.sellerx.admin.dto.AdminUserDto;
import com.ecommerce.sellerx.admin.dto.AdminUserListDto;
import com.ecommerce.sellerx.admin.dto.ChangeRoleRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

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
     * Search users by email or name
     * GET /api/admin/users/search?q=query
     */
    @GetMapping("/search")
    public ResponseEntity<List<AdminUserListDto>> searchUsers(@RequestParam String q) {
        log.info("Admin searching users with query: {}", q);
        List<AdminUserListDto> users = adminUserService.searchUsers(q);
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
}
