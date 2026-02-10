package com.ecommerce.sellerx.expenses;

import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.common.TestDataBuilder;
import com.ecommerce.sellerx.common.exception.UnauthorizedAccessException;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreNotFoundException;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StoreExpenseService.
 * Tests CRUD operations, expense calculations, and authorization.
 */
@DisplayName("StoreExpenseService")
class StoreExpenseServiceTest extends BaseUnitTest {

    @Mock
    private StoreExpenseRepository storeExpenseRepository;

    @Mock
    private ExpenseCategoryRepository expenseCategoryRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private TrendyolProductRepository productRepository;

    @Mock
    private StoreExpenseMapper storeExpenseMapper;

    @Mock
    private ExpenseCategoryMapper expenseCategoryMapper;

    @InjectMocks
    private StoreExpenseService expenseService;

    private User testUser;
    private Store testStore;
    private ExpenseCategory testCategory;
    private UUID storeId;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetSequence();
        testUser = TestDataBuilder.user().build();
        testUser.setId(1L);
        testStore = TestDataBuilder.store(testUser).build();
        storeId = UUID.randomUUID();
        testStore.setId(storeId);

        categoryId = UUID.randomUUID();
        testCategory = TestDataBuilder.expenseCategory().build();
        testCategory.setId(categoryId);
    }

    @Nested
    @DisplayName("getExpensesByStore")
    class GetExpensesByStore {

        @Test
        @DisplayName("should return expenses with total for existing store")
        void shouldReturnExpensesWithTotal() {
            // Given
            StoreExpense expense1 = TestDataBuilder.storeExpense(testStore, testCategory).build();
            StoreExpense expense2 = TestDataBuilder.storeExpense(testStore, testCategory)
                    .amount(new BigDecimal("200.00"))
                    .build();

            StoreExpenseDto dto1 = mock(StoreExpenseDto.class);
            StoreExpenseDto dto2 = mock(StoreExpenseDto.class);

            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(storeExpenseRepository.findByStoreIdWithRelations(storeId))
                    .thenReturn(List.of(expense1, expense2));
            when(storeExpenseRepository.getTotalExpensesByStoreId(storeId))
                    .thenReturn(new BigDecimal("350.00"));
            when(storeExpenseMapper.toDto(expense1)).thenReturn(dto1);
            when(storeExpenseMapper.toDto(expense2)).thenReturn(dto2);

            // When
            StoreExpensesResponse result = expenseService.getExpensesByStore(storeId);

            // Then
            assertThat(result.totalExpense())
                    .as("Total expense should match sum from repository")
                    .isEqualByComparingTo(new BigDecimal("350.00"));
            assertThat(result.expenses())
                    .as("Should return both expense DTOs")
                    .hasSize(2);
        }

        @Test
        @DisplayName("should return zero total when no expenses exist")
        void shouldReturnZeroTotalWhenNoExpenses() {
            // Given
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(storeExpenseRepository.findByStoreIdWithRelations(storeId)).thenReturn(List.of());
            when(storeExpenseRepository.getTotalExpensesByStoreId(storeId)).thenReturn(null);

            // When
            StoreExpensesResponse result = expenseService.getExpensesByStore(storeId);

            // Then
            assertThat(result.totalExpense())
                    .as("Total should be zero when no expenses exist")
                    .isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.expenses()).isEmpty();
        }

        @Test
        @DisplayName("should throw StoreNotFoundException when store does not exist")
        void shouldThrowWhenStoreNotFound() {
            // Given
            UUID nonExistentStoreId = UUID.randomUUID();
            when(storeRepository.findById(nonExistentStoreId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> expenseService.getExpensesByStore(nonExistentStoreId))
                    .as("Should throw StoreNotFoundException for non-existent store")
                    .isInstanceOf(StoreNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("createExpense")
    class CreateExpense {

        @Test
        @DisplayName("should create expense with VAT calculation")
        void shouldCreateExpenseWithVatCalculation() {
            // Given
            CreateStoreExpenseRequest request = new CreateStoreExpenseRequest(
                    categoryId, null, LocalDateTime.now(),
                    ExpenseFrequency.MONTHLY, null, "Office Rent",
                    new BigDecimal("1000.00"), 20
            );

            StoreExpense mappedExpense = StoreExpense.builder()
                    .name("Office Rent")
                    .amount(new BigDecimal("1000.00"))
                    .frequency(ExpenseFrequency.MONTHLY)
                    .date(LocalDateTime.now())
                    .build();

            StoreExpenseDto resultDto = mock(StoreExpenseDto.class);

            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(expenseCategoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
            when(storeExpenseMapper.toEntity(request)).thenReturn(mappedExpense);
            when(storeExpenseMapper.toDto(any(StoreExpense.class))).thenReturn(resultDto);

            // When
            StoreExpenseDto result = expenseService.createExpense(storeId, request);

            // Then
            verify(storeExpenseRepository).save(mappedExpense);
            assertThat(mappedExpense.getStore())
                    .as("Expense should be associated with the store")
                    .isEqualTo(testStore);
            assertThat(mappedExpense.getExpenseCategory())
                    .as("Expense should be associated with the category")
                    .isEqualTo(testCategory);
            assertThat(mappedExpense.getVatRate())
                    .as("VAT rate should be set from request")
                    .isEqualTo(20);
        }

        @Test
        @DisplayName("should throw StoreNotFoundException when store does not exist")
        void shouldThrowWhenStoreNotFoundForCreate() {
            // Given
            CreateStoreExpenseRequest request = new CreateStoreExpenseRequest(
                    categoryId, null, null,
                    ExpenseFrequency.ONE_TIME, null, "Test",
                    new BigDecimal("50.00"), null
            );
            when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> expenseService.createExpense(storeId, request))
                    .isInstanceOf(StoreNotFoundException.class);
        }

        @Test
        @DisplayName("should throw ExpenseCategoryNotFoundException when category does not exist")
        void shouldThrowWhenCategoryNotFound() {
            // Given
            UUID unknownCategoryId = UUID.randomUUID();
            CreateStoreExpenseRequest request = new CreateStoreExpenseRequest(
                    unknownCategoryId, null, null,
                    ExpenseFrequency.ONE_TIME, null, "Test",
                    new BigDecimal("50.00"), null
            );
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(expenseCategoryRepository.findById(unknownCategoryId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> expenseService.createExpense(storeId, request))
                    .isInstanceOf(ExpenseCategoryNotFoundException.class);
        }

        @Test
        @DisplayName("should set current date when request date is null")
        void shouldSetCurrentDateWhenRequestDateIsNull() {
            // Given
            CreateStoreExpenseRequest request = new CreateStoreExpenseRequest(
                    categoryId, null, null, // null date
                    ExpenseFrequency.ONE_TIME, null, "No Date Expense",
                    new BigDecimal("75.00"), null
            );

            StoreExpense mappedExpense = StoreExpense.builder()
                    .name("No Date Expense")
                    .amount(new BigDecimal("75.00"))
                    .frequency(ExpenseFrequency.ONE_TIME)
                    .build();

            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(expenseCategoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
            when(storeExpenseMapper.toEntity(request)).thenReturn(mappedExpense);
            when(storeExpenseMapper.toDto(any())).thenReturn(mock(StoreExpenseDto.class));

            // When
            expenseService.createExpense(storeId, request);

            // Then
            assertThat(mappedExpense.getDate())
                    .as("Date should be set to current time when request date is null")
                    .isNotNull();
        }
    }

    @Nested
    @DisplayName("updateExpense")
    class UpdateExpense {

        @Test
        @DisplayName("should update expense when owned by store")
        void shouldUpdateExpenseWhenOwnedByStore() {
            // Given
            UUID expenseId = UUID.randomUUID();
            StoreExpense existingExpense = TestDataBuilder.storeExpense(testStore, testCategory).build();
            existingExpense.setId(expenseId);

            UpdateStoreExpenseRequest request = new UpdateStoreExpenseRequest(
                    categoryId, null, LocalDateTime.now(),
                    ExpenseFrequency.YEARLY, null, "Updated Expense",
                    new BigDecimal("2000.00"), 10
            );

            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(storeExpenseRepository.findById(expenseId)).thenReturn(Optional.of(existingExpense));
            when(expenseCategoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
            when(storeExpenseMapper.toDto(any())).thenReturn(mock(StoreExpenseDto.class));

            // When
            expenseService.updateExpense(storeId, expenseId, request);

            // Then
            verify(storeExpenseMapper).update(request, existingExpense);
            verify(storeExpenseRepository).save(existingExpense);
        }

        @Test
        @DisplayName("should throw UnauthorizedAccessException when expense belongs to different store")
        void shouldThrowWhenExpenseBelongsToDifferentStore() {
            // Given
            UUID expenseId = UUID.randomUUID();
            Store otherStore = TestDataBuilder.store(testUser).build();
            otherStore.setId(UUID.randomUUID());

            StoreExpense otherStoreExpense = TestDataBuilder.storeExpense(otherStore, testCategory).build();
            otherStoreExpense.setId(expenseId);

            UpdateStoreExpenseRequest request = new UpdateStoreExpenseRequest(
                    categoryId, null, LocalDateTime.now(),
                    ExpenseFrequency.MONTHLY, null, "Hacked Expense",
                    new BigDecimal("999.00"), null
            );

            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(storeExpenseRepository.findById(expenseId)).thenReturn(Optional.of(otherStoreExpense));

            // When/Then
            assertThatThrownBy(() -> expenseService.updateExpense(storeId, expenseId, request))
                    .as("Should throw UnauthorizedAccessException when expense belongs to different store")
                    .isInstanceOf(UnauthorizedAccessException.class);
        }

        @Test
        @DisplayName("should throw StoreExpenseNotFoundException when expense does not exist")
        void shouldThrowWhenExpenseNotFound() {
            // Given
            UUID nonExistentExpenseId = UUID.randomUUID();
            UpdateStoreExpenseRequest request = new UpdateStoreExpenseRequest(
                    categoryId, null, LocalDateTime.now(),
                    ExpenseFrequency.MONTHLY, null, "Missing",
                    new BigDecimal("100.00"), null
            );

            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(storeExpenseRepository.findById(nonExistentExpenseId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> expenseService.updateExpense(storeId, nonExistentExpenseId, request))
                    .isInstanceOf(StoreExpenseNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteExpense")
    class DeleteExpense {

        @Test
        @DisplayName("should delete expense when owned by store")
        void shouldDeleteExpenseWhenOwnedByStore() {
            // Given
            UUID expenseId = UUID.randomUUID();
            StoreExpense expense = TestDataBuilder.storeExpense(testStore, testCategory).build();
            expense.setId(expenseId);

            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(storeExpenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));

            // When
            expenseService.deleteExpense(storeId, expenseId);

            // Then
            verify(storeExpenseRepository).delete(expense);
        }

        @Test
        @DisplayName("should throw UnauthorizedAccessException when expense belongs to different store")
        void shouldThrowWhenDeletingExpenseFromDifferentStore() {
            // Given
            UUID expenseId = UUID.randomUUID();
            Store otherStore = TestDataBuilder.store(testUser).build();
            otherStore.setId(UUID.randomUUID());

            StoreExpense otherStoreExpense = TestDataBuilder.storeExpense(otherStore, testCategory).build();
            otherStoreExpense.setId(expenseId);

            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(storeExpenseRepository.findById(expenseId)).thenReturn(Optional.of(otherStoreExpense));

            // When/Then
            assertThatThrownBy(() -> expenseService.deleteExpense(storeId, expenseId))
                    .isInstanceOf(UnauthorizedAccessException.class);

            verify(storeExpenseRepository, never()).delete(any());
        }

        @Test
        @DisplayName("should throw StoreExpenseNotFoundException when expense does not exist")
        void shouldThrowWhenDeletingNonExistentExpense() {
            // Given
            UUID nonExistentExpenseId = UUID.randomUUID();
            when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
            when(storeExpenseRepository.findById(nonExistentExpenseId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> expenseService.deleteExpense(storeId, nonExistentExpenseId))
                    .isInstanceOf(StoreExpenseNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getAllExpenseCategories")
    class GetAllCategories {

        @Test
        @DisplayName("should return all categories sorted by name")
        void shouldReturnAllCategories() {
            // Given
            ExpenseCategory cat1 = TestDataBuilder.expenseCategory().build();
            cat1.setName("Ambalaj");
            ExpenseCategory cat2 = TestDataBuilder.expenseCategory().build();
            cat2.setName("Ofis Giderleri");

            ExpenseCategoryDto dto1 = mock(ExpenseCategoryDto.class);
            ExpenseCategoryDto dto2 = mock(ExpenseCategoryDto.class);

            when(expenseCategoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(cat1, cat2));
            when(expenseCategoryMapper.toDto(eq(cat1), anyLong())).thenReturn(dto1);
            when(expenseCategoryMapper.toDto(eq(cat2), anyLong())).thenReturn(dto2);

            // When
            List<ExpenseCategoryDto> result = expenseService.getAllExpenseCategories();

            // Then
            assertThat(result)
                    .as("Should return all categories")
                    .hasSize(2);
        }
    }
}
