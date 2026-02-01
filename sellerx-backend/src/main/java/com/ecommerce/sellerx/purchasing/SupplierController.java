package com.ecommerce.sellerx.purchasing;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stores/{storeId}/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping
    public ResponseEntity<List<SupplierDto>> getSuppliers(@PathVariable UUID storeId) {
        return ResponseEntity.ok(supplierService.getSuppliers(storeId));
    }

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @GetMapping("/{supplierId}")
    public ResponseEntity<SupplierDto> getSupplier(
            @PathVariable UUID storeId,
            @PathVariable Long supplierId) {
        return ResponseEntity.ok(supplierService.getSupplier(storeId, supplierId));
    }

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @PostMapping
    public ResponseEntity<SupplierDto> createSupplier(
            @PathVariable UUID storeId,
            @RequestBody CreateSupplierRequest request) {
        SupplierDto supplier = supplierService.createSupplier(storeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(supplier);
    }

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @PutMapping("/{supplierId}")
    public ResponseEntity<SupplierDto> updateSupplier(
            @PathVariable UUID storeId,
            @PathVariable Long supplierId,
            @RequestBody UpdateSupplierRequest request) {
        return ResponseEntity.ok(supplierService.updateSupplier(storeId, supplierId, request));
    }

    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    @DeleteMapping("/{supplierId}")
    public ResponseEntity<Void> deleteSupplier(
            @PathVariable UUID storeId,
            @PathVariable Long supplierId) {
        supplierService.deleteSupplier(storeId, supplierId);
        return ResponseEntity.noContent().build();
    }
}
