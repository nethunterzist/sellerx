package com.ecommerce.sellerx.expenses;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, UUID> {
    
    Optional<ExpenseCategory> findByName(String name);
    
    List<ExpenseCategory> findAllByOrderByNameAsc();
}
