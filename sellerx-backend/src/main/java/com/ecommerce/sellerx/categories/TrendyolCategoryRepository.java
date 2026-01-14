package com.ecommerce.sellerx.categories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrendyolCategoryRepository extends JpaRepository<TrendyolCategory, UUID> {
    
    Optional<TrendyolCategory> findByCategoryId(Long categoryId);
    
    boolean existsByCategoryId(Long categoryId);
    
    @Query("SELECT tc FROM TrendyolCategory tc ORDER BY tc.categoryName")
    List<TrendyolCategory> findAllOrderByCategoryName();
    
    @Query("SELECT tc.categoryId FROM TrendyolCategory tc WHERE tc.categoryId IN :categoryIds")
    List<Long> findExistingCategoryIds(@Param("categoryIds") List<Long> categoryIds);
}
