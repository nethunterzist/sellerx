package com.ecommerce.sellerx.expenses;

import com.ecommerce.sellerx.stores.StoreService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/expenses")
public class StoreExpenseController {
    
    private final StoreExpenseService storeExpenseService;
    private final StoreService storeService;
    
    @Deprecated // Use store-specific endpoint instead
    @GetMapping("/categories")
    public ResponseEntity<List<ExpenseCategoryDto>> getAllExpenseCategories() {
        List<ExpenseCategoryDto> categories = storeExpenseService.getAllExpenseCategories();
        return ResponseEntity.ok(categories);
    }

    // Store-specific category endpoints
    @GetMapping("/store/{storeId}/categories")
    public ResponseEntity<List<ExpenseCategoryDto>> getExpenseCategoriesByStore(
            @PathVariable UUID storeId) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!storeService.isStoreOwnedByUser(storeId, userId)) {
            throw new AccessDeniedException("Bu store'a erişim yetkiniz yok.");
        }

        List<ExpenseCategoryDto> categories = storeExpenseService.getExpenseCategoriesByStore(storeId);
        return ResponseEntity.ok(categories);
    }

    @PostMapping("/store/{storeId}/categories")
    public ResponseEntity<ExpenseCategoryDto> createExpenseCategory(
            @PathVariable UUID storeId,
            @Valid @RequestBody CreateExpenseCategoryRequest request,
            UriComponentsBuilder uriBuilder) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!storeService.isStoreOwnedByUser(storeId, userId)) {
            throw new AccessDeniedException("Bu store'a erişim yetkiniz yok.");
        }

        ExpenseCategoryDto categoryDto = storeExpenseService.createExpenseCategory(storeId, request);

        var uri = uriBuilder.path("/expenses/store/{storeId}/categories/{categoryId}")
            .buildAndExpand(storeId, categoryDto.id()).toUri();

        return ResponseEntity.created(uri).body(categoryDto);
    }

    @PutMapping("/store/{storeId}/categories/{categoryId}")
    public ResponseEntity<ExpenseCategoryDto> updateExpenseCategory(
            @PathVariable UUID storeId,
            @PathVariable UUID categoryId,
            @Valid @RequestBody UpdateExpenseCategoryRequest request) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!storeService.isStoreOwnedByUser(storeId, userId)) {
            throw new AccessDeniedException("Bu store'a erişim yetkiniz yok.");
        }

        ExpenseCategoryDto categoryDto = storeExpenseService.updateExpenseCategory(storeId, categoryId, request);
        return ResponseEntity.ok(categoryDto);
    }

    @DeleteMapping("/store/{storeId}/categories/{categoryId}")
    public ResponseEntity<Void> deleteExpenseCategory(
            @PathVariable UUID storeId,
            @PathVariable UUID categoryId) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!storeService.isStoreOwnedByUser(storeId, userId)) {
            throw new AccessDeniedException("Bu store'a erişim yetkiniz yok.");
        }

        storeExpenseService.deleteExpenseCategory(storeId, categoryId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/store/{storeId}")
    public ResponseEntity<StoreExpensesResponse> getExpensesByStore(
            @PathVariable UUID storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Store'un bu user'a ait olduğunu kontrol et
        if (!storeService.isStoreOwnedByUser(storeId, userId)) {
            throw new AccessDeniedException("Bu store'a erişim yetkiniz yok.");
        }

        StoreExpensesResponse response = storeExpenseService.getExpensesByStore(storeId, startDate, endDate);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/store/{storeId}")
    public ResponseEntity<StoreExpenseDto> createExpense(
            @PathVariable UUID storeId,
            @Valid @RequestBody CreateStoreExpenseRequest request,
            UriComponentsBuilder uriBuilder) {
        
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        // Store'un bu user'a ait olduğunu kontrol et
        if (!storeService.isStoreOwnedByUser(storeId, userId)) {
            throw new AccessDeniedException("Bu store'a erişim yetkiniz yok.");
        }
        
        StoreExpenseDto expenseDto = storeExpenseService.createExpense(storeId, request);
        
        var uri = uriBuilder.path("/expenses/store/{storeId}/{expenseId}")
            .buildAndExpand(storeId, expenseDto.id()).toUri();
        
        return ResponseEntity.created(uri).body(expenseDto);
    }
    
    @PutMapping("/store/{storeId}/{expenseId}")
    public ResponseEntity<StoreExpenseDto> updateExpense(
            @PathVariable UUID storeId,
            @PathVariable UUID expenseId,
            @Valid @RequestBody UpdateStoreExpenseRequest request) {
        
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        // Store'un bu user'a ait olduğunu kontrol et
        if (!storeService.isStoreOwnedByUser(storeId, userId)) {
            throw new AccessDeniedException("Bu store'a erişim yetkiniz yok.");
        }
        
        StoreExpenseDto expenseDto = storeExpenseService.updateExpense(storeId, expenseId, request);
        return ResponseEntity.ok(expenseDto);
    }
    
    @DeleteMapping("/store/{storeId}/{expenseId}")
    public ResponseEntity<Void> deleteExpense(
            @PathVariable UUID storeId,
            @PathVariable UUID expenseId) {
        
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        // Store'un bu user'a ait olduğunu kontrol et
        if (!storeService.isStoreOwnedByUser(storeId, userId)) {
            throw new AccessDeniedException("Bu store'a erişim yetkiniz yok.");
        }
        
        storeExpenseService.deleteExpense(storeId, expenseId);
        return ResponseEntity.noContent().build();
    }
    
    // Exception Handlers
    @ExceptionHandler(StoreExpenseNotFoundException.class)
    public ResponseEntity<Void> handleStoreExpenseNotFound() {
        return ResponseEntity.notFound().build();
    }
    
    @ExceptionHandler(ExpenseCategoryNotFoundException.class)
    public ResponseEntity<Void> handleExpenseCategoryNotFound() {
        return ResponseEntity.notFound().build();
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Void> handleAccessDenied() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @ExceptionHandler(ExpenseCategoryInUseException.class)
    public ResponseEntity<ErrorResponse> handleExpenseCategoryInUse(ExpenseCategoryInUseException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(e.getMessage(), e.getExpenseCount()));
    }

    @ExceptionHandler(ExpenseCategoryDuplicateException.class)
    public ResponseEntity<ErrorResponse> handleExpenseCategoryDuplicate(ExpenseCategoryDuplicateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(e.getMessage(), 0));
    }

    public record ErrorResponse(String message, long expenseCount) {}
}
