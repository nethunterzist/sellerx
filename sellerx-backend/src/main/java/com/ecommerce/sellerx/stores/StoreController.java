package com.ecommerce.sellerx.stores;

import com.ecommerce.sellerx.users.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import com.ecommerce.sellerx.users.User;
import com.ecommerce.sellerx.users.UserRepository;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/stores")
@Tag(name = "Stores", description = "Store management and Trendyol credentials")
public class StoreController {
    private final StoreService storeService;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final StoreOnboardingService storeOnboardingService;

    @Operation(summary = "Get user's stores", description = "Returns all stores owned by the authenticated user")
    @ApiResponse(responseCode = "200", description = "List of stores")
    @GetMapping("/my")
    public Iterable<StoreDto> getMyStores() {
        Long userId = (Long) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userId).orElseThrow(com.ecommerce.sellerx.users.UserNotFoundException::new);
        return storeService.getStoresByUser(user);
    }

    @Operation(summary = "Get all stores (Admin)", description = "Returns all stores in the system. Admin only.")
    @ApiResponse(responseCode = "200", description = "List of all stores")
    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public Iterable<StoreDto> getAllStores(
            @Parameter(description = "Sort field (e.g., 'name', 'createdAt')")
            @RequestParam(required = false, defaultValue = "", name = "sort") String sortBy) {
        return storeService.getAllStores(sortBy);
    }

    @Operation(summary = "Get store by ID", description = "Returns store details. User must own the store.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Store details"),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
            @ApiResponse(responseCode = "404", description = "Store not found", content = @Content)
    })
    @GetMapping("/{id}")
    public StoreDto getStore(@Parameter(description = "Store UUID") @PathVariable UUID id) {
        Long userId = (Long) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userId).orElseThrow(com.ecommerce.sellerx.users.UserNotFoundException::new);
        StoreDto storeDto = storeService.getStore(id);
        if (!storeDto.getUserId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Bu store'a erişim yetkiniz yok.");
        }
        return storeDto;
    }

    @Operation(summary = "Register new store", description = "Creates a new store with Trendyol credentials. Triggers initial sync.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Store created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
    })
    @PostMapping
    public ResponseEntity<?> registerStore(@Valid @RequestBody RegisterStoreRequest request, UriComponentsBuilder uriBuilder) {
        // UserId yerine token'dan userId al
        Long userId = (Long) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userId).orElseThrow(com.ecommerce.sellerx.users.UserNotFoundException::new);
        var storeDto = storeService.registerStore(request, user);
        
        // Eğer kullanıcının hiç seçili store'u yoksa, yeni oluşturulan store'u seçili yap
        if (userService.getSelectedStoreId(userId) == null) {
            userService.setSelectedStoreId(userId, storeDto.getId());
        }
        
        var uri = uriBuilder.path("/stores/{id}").buildAndExpand(storeDto.getId()).toUri();
        return ResponseEntity.created(uri).body(storeDto);
    }

    @PutMapping("/{id}")
    public StoreDto updateStore(@PathVariable UUID id, @RequestBody UpdateStoreRequest request) {
        Long userId = (Long) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userId).orElseThrow(com.ecommerce.sellerx.users.UserNotFoundException::new);
        return storeService.updateStoreByUser(id, request, user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStore(@PathVariable UUID id, HttpServletResponse response) {
        Long userId = (Long) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userId).orElseThrow(com.ecommerce.sellerx.users.UserNotFoundException::new);

        // Store'u sil (StoreService içinde selected store logic'i var)
        storeService.deleteStoreByUser(id, user);

        // Delete sonrası yeni selected store'u al
        UUID newSelectedStoreId = userService.getSelectedStoreId(userId);

        // Cookie'yi güncelle
        if (newSelectedStoreId != null) {
            // Yeni store seçildiyse cookie'yi güncelle
            var storeIdCookie = new Cookie("selected_store_id", newSelectedStoreId.toString());
            storeIdCookie.setHttpOnly(false);
            storeIdCookie.setPath("/");
            storeIdCookie.setMaxAge(30 * 24 * 60 * 60); // 30 days
            storeIdCookie.setSecure(false);
            response.addCookie(storeIdCookie);
        } else {
            // Hiç store kalmadıysa cookie'yi sil
            var storeIdCookie = new Cookie("selected_store_id", "");
            storeIdCookie.setHttpOnly(false);
            storeIdCookie.setPath("/");
            storeIdCookie.setMaxAge(0); // Delete cookie
            storeIdCookie.setSecure(false);
            response.addCookie(storeIdCookie);
        }

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/retry-sync")
    public ResponseEntity<String> retrySyncStore(@PathVariable UUID id) {
        Long userId = (Long) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userId).orElseThrow(com.ecommerce.sellerx.users.UserNotFoundException::new);

        // Verify user owns this store
        StoreDto storeDto = storeService.getStore(id);
        if (!storeDto.getUserId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Bu store'a erişim yetkiniz yok.");
        }

        storeOnboardingService.retryInitialSync(id);
        return ResponseEntity.ok("Sync retry başlatıldı. Status: " + storeOnboardingService.getSyncStatus(id));
    }

    @PostMapping("/{id}/cancel-sync")
    public ResponseEntity<String> cancelSyncStore(@PathVariable UUID id) {
        Long userId = (Long) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userId).orElseThrow(com.ecommerce.sellerx.users.UserNotFoundException::new);

        // Verify user owns this store
        StoreDto storeDto = storeService.getStore(id);
        if (!storeDto.getUserId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Bu store'a erişim yetkiniz yok.");
        }

        storeOnboardingService.requestCancelSync(id);
        return ResponseEntity.ok("Sync iptal isteği gönderildi.");
    }

    @GetMapping("/{id}/sync-status")
    public ResponseEntity<String> getSyncStatus(@PathVariable UUID id) {
        Long userId = (Long) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userId).orElseThrow(com.ecommerce.sellerx.users.UserNotFoundException::new);

        // Verify user owns this store
        StoreDto storeDto = storeService.getStore(id);
        if (!storeDto.getUserId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Bu store'a erişim yetkiniz yok.");
        }

        return ResponseEntity.ok(storeOnboardingService.getSyncStatus(id));
    }

    @GetMapping("/{id}/sync-progress")
    public ResponseEntity<java.util.Map<String, Object>> getSyncProgress(@PathVariable UUID id) {
        Long userId = (Long) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userId).orElseThrow(() -> new com.ecommerce.sellerx.users.UserNotFoundException());

        // Verify user owns this store
        StoreDto storeDto = storeService.getStore(id);
        if (!storeDto.getUserId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Bu store'a erişim yetkiniz yok.");
        }

        Store store = storeRepository.findById(id)
            .orElseThrow(() -> new com.ecommerce.sellerx.stores.StoreNotFoundException("Store not found"));

        java.util.Map<String, Object> progress = new java.util.HashMap<>();

        // Legacy status (backward compatibility)
        progress.put("syncStatus", store.getSyncStatus());

        // New parallel sync phases
        progress.put("overallSyncStatus", store.getOverallSyncStatus());
        progress.put("syncPhases", store.getSyncPhases() != null ? store.getSyncPhases() : new java.util.HashMap<>());

        // Historical sync details
        progress.put("currentProcessingDate", store.getHistoricalSyncCurrentProcessingDate());
        progress.put("completedChunks", store.getHistoricalSyncCompletedChunks());
        progress.put("totalChunks", store.getHistoricalSyncTotalChunks());
        progress.put("checkpointDate", store.getHistoricalSyncCheckpointDate());
        progress.put("startDate", store.getHistoricalSyncStartDate());

        // Calculate percentage based on phases if available
        java.util.Map<String, PhaseStatus> phases = store.getSyncPhases();
        if (phases != null && !phases.isEmpty()) {
            // Phase-based progress calculation with weights
            java.util.Map<String, Integer> phaseWeights = new java.util.HashMap<>();
            phaseWeights.put("PRODUCTS", 10);
            phaseWeights.put("HISTORICAL", 35);
            phaseWeights.put("FINANCIAL", 15);
            phaseWeights.put("GAP", 10);
            phaseWeights.put("COMMISSIONS", 10);
            phaseWeights.put("RETURNS", 10);
            phaseWeights.put("QA", 10);

            int totalWeight = phaseWeights.values().stream().mapToInt(Integer::intValue).sum();
            int completedWeight = 0;

            for (java.util.Map.Entry<String, PhaseStatus> entry : phases.entrySet()) {
                String phaseName = entry.getKey();
                PhaseStatus phaseStatus = entry.getValue();
                Integer weight = phaseWeights.getOrDefault(phaseName, 0);

                if (phaseStatus != null && PhaseStatusType.COMPLETED == phaseStatus.getStatus()) {
                    completedWeight += weight;
                } else if (phaseStatus != null && PhaseStatusType.ACTIVE == phaseStatus.getStatus()) {
                    // Active phases count as 50% complete
                    completedWeight += weight / 2;
                }
            }

            double percentage = (double) completedWeight / totalWeight * 100;
            progress.put("percentage", Math.round(percentage * 100.0) / 100.0);
        } else if (store.getHistoricalSyncTotalChunks() != null &&
            store.getHistoricalSyncTotalChunks() > 0) {
            // Fallback to legacy percentage calculation
            double percentage = (double) (store.getHistoricalSyncCompletedChunks() != null ?
                store.getHistoricalSyncCompletedChunks() : 0) /
                store.getHistoricalSyncTotalChunks() * 100;
            progress.put("percentage", Math.round(percentage * 100.0) / 100.0);
        } else {
            progress.put("percentage", 0.0);
        }

        return ResponseEntity.ok(progress);
    }
}
