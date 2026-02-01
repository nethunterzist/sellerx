package com.ecommerce.sellerx.admin;

import com.ecommerce.sellerx.admin.dto.AdminStoreDto;
import com.ecommerce.sellerx.admin.dto.AdminStoreListDto;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/stores")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminStoreController {

    private final AdminStoreService adminStoreService;

    /**
     * Get paginated list of all stores
     * GET /api/admin/stores?page=0&size=20&sort=createdAt,desc
     */
    @GetMapping
    public ResponseEntity<Page<AdminStoreListDto>> getAllStores(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        size = Math.min(size, 100);
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        log.info("Admin fetching stores - page: {}, size: {}, sort: {}", page, size, sort);
        Page<AdminStoreListDto> stores = adminStoreService.getAllStores(pageable);
        return ResponseEntity.ok(stores);
    }

    /**
     * Get store details by ID
     * GET /api/admin/stores/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdminStoreDto> getStoreById(@PathVariable UUID id) {
        log.info("Admin fetching store details for id: {}", id);
        AdminStoreDto store = adminStoreService.getStoreById(id);
        return ResponseEntity.ok(store);
    }

    /**
     * Trigger manual sync for a store
     * POST /api/admin/stores/{id}/sync
     */
    @PostMapping("/{id}/sync")
    public ResponseEntity<AdminStoreDto> triggerSync(@PathVariable UUID id) {
        log.info("Admin triggering sync for store: {}", id);
        AdminStoreDto store = adminStoreService.triggerSync(id);
        return ResponseEntity.ok(store);
    }

    /**
     * Search stores by store name or user email
     * GET /api/admin/stores/search?q=query
     */
    @GetMapping("/search")
    public ResponseEntity<List<AdminStoreListDto>> searchStores(@RequestParam String q) {
        log.info("Admin searching stores with query: {}", q);
        List<AdminStoreListDto> stores = adminStoreService.searchStores(q);
        return ResponseEntity.ok(stores);
    }
}
