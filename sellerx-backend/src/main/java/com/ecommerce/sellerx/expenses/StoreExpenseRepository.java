package com.ecommerce.sellerx.expenses;

import com.ecommerce.sellerx.stores.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface StoreExpenseRepository extends JpaRepository<StoreExpense, UUID> {
    
    List<StoreExpense> findByStoreOrderByDateDesc(Store store);
    
    List<StoreExpense> findByStoreIdOrderByDateDesc(UUID storeId);
    
    @Query("SELECT SUM(se.amount) FROM StoreExpense se WHERE se.store.id = :storeId")
    BigDecimal getTotalExpensesByStoreId(@Param("storeId") UUID storeId);
    
    @Query("SELECT se FROM StoreExpense se " +
           "LEFT JOIN FETCH se.expenseCategory " +
           "LEFT JOIN FETCH se.product " +
           "WHERE se.store.id = :storeId " +
           "ORDER BY se.date DESC")
    List<StoreExpense> findByStoreIdWithRelations(@Param("storeId") UUID storeId);
    
    void deleteByIdAndStoreId(UUID expenseId, UUID storeId);
}
