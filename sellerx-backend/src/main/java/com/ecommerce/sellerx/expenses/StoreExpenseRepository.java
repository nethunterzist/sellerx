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

    // For VAT reconciliation - find expenses by date range
    @Query("SELECT se FROM StoreExpense se WHERE se.store.id = :storeId " +
           "AND se.date BETWEEN :startDate AND :endDate")
    List<StoreExpense> findByStoreIdAndDateBetween(
            @Param("storeId") UUID storeId,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);

    // ============== Kategori Bazlı Gider Sorguları ==============

    /**
     * Kategori adına göre gider toplamı.
     * ExpenseCategory.name alanına göre filtreleme yapar.
     */
    @Query("SELECT COALESCE(SUM(se.amount), 0) FROM StoreExpense se " +
           "WHERE se.store.id = :storeId " +
           "AND se.date BETWEEN :startDate AND :endDate " +
           "AND LOWER(se.expenseCategory.name) LIKE LOWER(CONCAT('%', :categoryKeyword, '%'))")
    BigDecimal sumByCategoryKeyword(
            @Param("storeId") UUID storeId,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate,
            @Param("categoryKeyword") String categoryKeyword);

    /**
     * Belirli kategoriler dışındaki giderlerin toplamı (Diğer Giderler için).
     */
    @Query("SELECT COALESCE(SUM(se.amount), 0) FROM StoreExpense se " +
           "WHERE se.store.id = :storeId " +
           "AND se.date BETWEEN :startDate AND :endDate " +
           "AND LOWER(se.expenseCategory.name) NOT LIKE '%ofis%' " +
           "AND LOWER(se.expenseCategory.name) NOT LIKE '%ambalaj%' " +
           "AND LOWER(se.expenseCategory.name) NOT LIKE '%paketleme%' " +
           "AND LOWER(se.expenseCategory.name) NOT LIKE '%muhasebe%' " +
           "AND LOWER(se.expenseCategory.name) NOT LIKE '%reklam%'")
    BigDecimal sumOtherExpenses(
            @Param("storeId") UUID storeId,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);

    /**
     * Reklam giderlerinin toplamı (Trendyol API'de olmadığı için manuel girilen).
     */
    @Query("SELECT COALESCE(SUM(se.amount), 0) FROM StoreExpense se " +
           "WHERE se.store.id = :storeId " +
           "AND se.date BETWEEN :startDate AND :endDate " +
           "AND LOWER(se.expenseCategory.name) LIKE '%reklam%'")
    BigDecimal sumAdvertisingExpenses(
            @Param("storeId") UUID storeId,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);

    /**
     * Tüm giderleri kategori bazında gruplar.
     * Returns: [categoryName, totalAmount] pairs
     * Dinamik kategori desteği için - yeni kategoriler otomatik olarak döner.
     */
    @Query("SELECT se.expenseCategory.name AS categoryName, COALESCE(SUM(se.amount), 0) AS totalAmount FROM StoreExpense se " +
           "WHERE se.store.id = :storeId " +
           "AND se.date BETWEEN :startDate AND :endDate " +
           "AND se.expenseCategory IS NOT NULL " +
           "GROUP BY se.expenseCategory.name " +
           "ORDER BY SUM(se.amount) DESC")
    List<CategoryExpenseSum> sumByAllCategories(
            @Param("storeId") UUID storeId,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);
}
