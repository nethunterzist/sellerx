package com.ecommerce.sellerx.expenses;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, UUID> {

    // Store-specific queries
    List<ExpenseCategory> findByStoreIdOrderByNameAsc(UUID storeId);

    Optional<ExpenseCategory> findByStoreIdAndName(UUID storeId, String name);

    boolean existsByStoreIdAndName(UUID storeId, String name);

    // Count expenses using this category
    @Query("SELECT COUNT(e) FROM StoreExpense e WHERE e.expenseCategory.id = :categoryId")
    long countExpensesByCategoryId(@Param("categoryId") UUID categoryId);

    // Legacy methods - may be removed after migration verified
    @Deprecated
    Optional<ExpenseCategory> findByName(String name);

    @Deprecated
    List<ExpenseCategory> findAllByOrderByNameAsc();
}
