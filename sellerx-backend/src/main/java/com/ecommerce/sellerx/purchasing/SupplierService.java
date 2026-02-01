package com.ecommerce.sellerx.purchasing;

import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final StoreRepository storeRepository;

    public List<SupplierDto> getSuppliers(UUID storeId) {
        return supplierRepository.findByStoreIdOrderByNameAsc(storeId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public SupplierDto getSupplier(UUID storeId, Long supplierId) {
        Supplier supplier = supplierRepository.findByStoreIdAndId(storeId, supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + supplierId));
        return toDto(supplier);
    }

    @Transactional
    public SupplierDto createSupplier(UUID storeId, CreateSupplierRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));

        if (supplierRepository.existsByStoreIdAndName(storeId, request.getName())) {
            throw new IllegalArgumentException("Supplier with name '" + request.getName() + "' already exists");
        }

        Supplier supplier = Supplier.builder()
                .store(store)
                .name(request.getName())
                .contactPerson(request.getContactPerson())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .country(request.getCountry())
                .currency(request.getCurrency() != null ? request.getCurrency() : "TRY")
                .paymentTermsDays(request.getPaymentTermsDays())
                .notes(request.getNotes())
                .build();

        supplierRepository.save(supplier);
        log.info("Created supplier '{}' for store {}", supplier.getName(), storeId);
        return toDto(supplier);
    }

    @Transactional
    public SupplierDto updateSupplier(UUID storeId, Long supplierId, UpdateSupplierRequest request) {
        Supplier supplier = supplierRepository.findByStoreIdAndId(storeId, supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + supplierId));

        if (request.getName() != null) {
            supplier.setName(request.getName());
        }
        if (request.getContactPerson() != null) {
            supplier.setContactPerson(request.getContactPerson());
        }
        if (request.getEmail() != null) {
            supplier.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            supplier.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            supplier.setAddress(request.getAddress());
        }
        if (request.getCountry() != null) {
            supplier.setCountry(request.getCountry());
        }
        if (request.getCurrency() != null) {
            supplier.setCurrency(request.getCurrency());
        }
        if (request.getPaymentTermsDays() != null) {
            supplier.setPaymentTermsDays(request.getPaymentTermsDays());
        }
        if (request.getNotes() != null) {
            supplier.setNotes(request.getNotes());
        }

        supplierRepository.save(supplier);
        log.info("Updated supplier {} for store {}", supplierId, storeId);
        return toDto(supplier);
    }

    @Transactional
    public void deleteSupplier(UUID storeId, Long supplierId) {
        Supplier supplier = supplierRepository.findByStoreIdAndId(storeId, supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + supplierId));
        supplierRepository.delete(supplier);
        log.info("Deleted supplier {} for store {}", supplierId, storeId);
    }

    private SupplierDto toDto(Supplier supplier) {
        return SupplierDto.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .contactPerson(supplier.getContactPerson())
                .email(supplier.getEmail())
                .phone(supplier.getPhone())
                .address(supplier.getAddress())
                .country(supplier.getCountry())
                .currency(supplier.getCurrency())
                .paymentTermsDays(supplier.getPaymentTermsDays())
                .notes(supplier.getNotes())
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .build();
    }
}
