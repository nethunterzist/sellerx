package com.ecommerce.sellerx.purchasing;

import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.common.TestDataBuilder;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("SupplierService")
class SupplierServiceTest extends BaseUnitTest {

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private StoreRepository storeRepository;

    private SupplierService service;

    private User testUser;
    private Store testStore;
    private UUID storeId;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetSequence();
        service = new SupplierService(supplierRepository, storeRepository);

        testUser = TestDataBuilder.user().build();
        testStore = TestDataBuilder.completedStore(testUser).build();
        storeId = testStore.getId();
    }

    @Nested
    @DisplayName("getSuppliers")
    class GetSuppliers {

        @Test
        @DisplayName("should return all suppliers for a store")
        void shouldReturnAllSuppliers() {
            Supplier s1 = createTestSupplier(1L, "Alpha Corp");
            Supplier s2 = createTestSupplier(2L, "Beta Inc");

            when(supplierRepository.findByStoreIdOrderByNameAsc(storeId))
                    .thenReturn(List.of(s1, s2));

            List<SupplierDto> result = service.getSuppliers(storeId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("Alpha Corp");
            assertThat(result.get(1).getName()).isEqualTo("Beta Inc");
        }

        @Test
        @DisplayName("should return empty list when no suppliers exist")
        void shouldReturnEmptyListWhenNoSuppliers() {
            when(supplierRepository.findByStoreIdOrderByNameAsc(storeId))
                    .thenReturn(Collections.emptyList());

            List<SupplierDto> result = service.getSuppliers(storeId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getSupplier")
    class GetSupplier {

        @Test
        @DisplayName("should return supplier by ID")
        void shouldReturnSupplierById() {
            Supplier supplier = createTestSupplier(1L, "Test Supplier");
            supplier.setEmail("test@supplier.com");
            supplier.setPhone("+90 555 1234567");

            when(supplierRepository.findByStoreIdAndId(storeId, 1L))
                    .thenReturn(Optional.of(supplier));

            SupplierDto result = service.getSupplier(storeId, 1L);

            assertThat(result.getName()).isEqualTo("Test Supplier");
            assertThat(result.getEmail()).isEqualTo("test@supplier.com");
            assertThat(result.getPhone()).isEqualTo("+90 555 1234567");
        }

        @Test
        @DisplayName("should throw when supplier not found")
        void shouldThrowWhenSupplierNotFound() {
            when(supplierRepository.findByStoreIdAndId(storeId, 999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getSupplier(storeId, 999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Supplier not found");
        }
    }

    @Nested
    @DisplayName("createSupplier")
    class CreateSupplier {

        @Test
        @DisplayName("should create supplier with all fields")
        void shouldCreateSupplierWithAllFields() {
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(supplierRepository.existsByStoreIdAndName(storeId, "New Supplier")).thenReturn(false);

            ArgumentCaptor<Supplier> captor = ArgumentCaptor.forClass(Supplier.class);
            when(supplierRepository.save(captor.capture())).thenAnswer(inv -> {
                Supplier s = inv.getArgument(0);
                s.setId(1L);
                return s;
            });

            CreateSupplierRequest request = CreateSupplierRequest.builder()
                    .name("New Supplier")
                    .contactPerson("John Doe")
                    .email("john@supplier.com")
                    .phone("+90 555 1234567")
                    .address("Istanbul, Turkey")
                    .country("Turkey")
                    .currency("USD")
                    .paymentTermsDays(30)
                    .notes("Preferred supplier")
                    .build();

            SupplierDto result = service.createSupplier(storeId, request);

            Supplier saved = captor.getValue();
            assertThat(saved.getName()).isEqualTo("New Supplier");
            assertThat(saved.getContactPerson()).isEqualTo("John Doe");
            assertThat(saved.getCurrency()).isEqualTo("USD");
            assertThat(saved.getPaymentTermsDays()).isEqualTo(30);
            assertThat(saved.getStore()).isEqualTo(testStore);
        }

        @Test
        @DisplayName("should default currency to TRY when not provided")
        void shouldDefaultCurrencyToTRY() {
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(supplierRepository.existsByStoreIdAndName(storeId, "Supplier")).thenReturn(false);

            ArgumentCaptor<Supplier> captor = ArgumentCaptor.forClass(Supplier.class);
            when(supplierRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            CreateSupplierRequest request = CreateSupplierRequest.builder()
                    .name("Supplier")
                    .build();

            service.createSupplier(storeId, request);

            assertThat(captor.getValue().getCurrency()).isEqualTo("TRY");
        }

        @Test
        @DisplayName("should throw when duplicate name exists")
        void shouldThrowWhenDuplicateNameExists() {
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(supplierRepository.existsByStoreIdAndName(storeId, "Existing Supplier")).thenReturn(true);

            CreateSupplierRequest request = CreateSupplierRequest.builder()
                    .name("Existing Supplier")
                    .build();

            assertThatThrownBy(() -> service.createSupplier(storeId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("should throw when store not found")
        void shouldThrowWhenStoreNotFound() {
            when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

            CreateSupplierRequest request = CreateSupplierRequest.builder()
                    .name("Supplier")
                    .build();

            assertThatThrownBy(() -> service.createSupplier(storeId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Store not found");
        }
    }

    @Nested
    @DisplayName("updateSupplier")
    class UpdateSupplier {

        @Test
        @DisplayName("should update only provided fields")
        void shouldUpdateOnlyProvidedFields() {
            Supplier supplier = createTestSupplier(1L, "Original Name");
            supplier.setEmail("original@email.com");
            supplier.setPhone("+90 555 0000000");

            when(supplierRepository.findByStoreIdAndId(storeId, 1L))
                    .thenReturn(Optional.of(supplier));
            when(supplierRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateSupplierRequest request = UpdateSupplierRequest.builder()
                    .email("updated@email.com")
                    .build();

            SupplierDto result = service.updateSupplier(storeId, 1L, request);

            assertThat(result.getEmail()).isEqualTo("updated@email.com");
            assertThat(result.getName()).isEqualTo("Original Name");
            assertThat(result.getPhone()).isEqualTo("+90 555 0000000");
        }

        @Test
        @DisplayName("should update all fields when all provided")
        void shouldUpdateAllFieldsWhenAllProvided() {
            Supplier supplier = createTestSupplier(1L, "Old Name");

            when(supplierRepository.findByStoreIdAndId(storeId, 1L))
                    .thenReturn(Optional.of(supplier));
            when(supplierRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateSupplierRequest request = UpdateSupplierRequest.builder()
                    .name("New Name")
                    .contactPerson("Jane")
                    .email("jane@example.com")
                    .phone("+1 234 567 8900")
                    .address("New York")
                    .country("USA")
                    .currency("EUR")
                    .paymentTermsDays(45)
                    .notes("Updated notes")
                    .build();

            SupplierDto result = service.updateSupplier(storeId, 1L, request);

            assertThat(result.getName()).isEqualTo("New Name");
            assertThat(result.getCurrency()).isEqualTo("EUR");
            assertThat(result.getPaymentTermsDays()).isEqualTo(45);
            assertThat(result.getCountry()).isEqualTo("USA");
        }

        @Test
        @DisplayName("should throw when supplier not found on update")
        void shouldThrowWhenSupplierNotFoundOnUpdate() {
            when(supplierRepository.findByStoreIdAndId(storeId, 999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateSupplier(storeId, 999L,
                    UpdateSupplierRequest.builder().build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Supplier not found");
        }
    }

    @Nested
    @DisplayName("deleteSupplier")
    class DeleteSupplier {

        @Test
        @DisplayName("should delete existing supplier")
        void shouldDeleteExistingSupplier() {
            Supplier supplier = createTestSupplier(1L, "To Delete");
            when(supplierRepository.findByStoreIdAndId(storeId, 1L))
                    .thenReturn(Optional.of(supplier));

            service.deleteSupplier(storeId, 1L);

            verify(supplierRepository).delete(supplier);
        }

        @Test
        @DisplayName("should throw when supplier not found on delete")
        void shouldThrowWhenSupplierNotFoundOnDelete() {
            when(supplierRepository.findByStoreIdAndId(storeId, 999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteSupplier(storeId, 999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Supplier not found");
        }
    }

    // === Helper Methods ===

    private Supplier createTestSupplier(Long id, String name) {
        return Supplier.builder()
                .id(id)
                .store(testStore)
                .name(name)
                .currency("TRY")
                .build();
    }
}
