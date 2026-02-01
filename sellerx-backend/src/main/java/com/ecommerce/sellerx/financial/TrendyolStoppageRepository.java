package com.ecommerce.sellerx.financial;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrendyolStoppageRepository extends JpaRepository<TrendyolStoppage, UUID> {

    List<TrendyolStoppage> findByStoreIdOrderByTransactionDateDesc(UUID storeId);

    List<TrendyolStoppage> findByStoreIdAndTransactionDateBetweenOrderByTransactionDateDesc(
            UUID storeId, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(s.amount), 0) FROM TrendyolStoppage s " +
            "WHERE s.store.id = :storeId " +
            "AND s.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByStoreAndDateRange(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    Optional<TrendyolStoppage> findByStoreIdAndTransactionDateAndAmount(
            UUID storeId, LocalDateTime transactionDate, BigDecimal amount);

    List<TrendyolStoppage> findByStoreIdAndPaymentOrderId(UUID storeId, Long paymentOrderId);

    @Query("SELECT COUNT(s) FROM TrendyolStoppage s WHERE s.store.id = :storeId")
    long countByStoreId(@Param("storeId") UUID storeId);

    boolean existsByStoreIdAndTransactionIdAndTransactionDate(
            UUID storeId, String transactionId, LocalDateTime transactionDate);

    // ============== Platform Ücreti Kategorileri Sorguları ==============

    /**
     * Description alanına göre stopaj/platform ücreti toplamı.
     * Türkçe keyword matching kullanır (case-insensitive).
     */
    @Query("SELECT COALESCE(SUM(s.amount), 0) FROM TrendyolStoppage s " +
            "WHERE s.store.id = :storeId " +
            "AND s.transactionDate BETWEEN :startDate AND :endDate " +
            "AND LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    BigDecimal sumByDescriptionKeyword(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("keyword") String keyword);

    /**
     * Belirli keyword listesine UYMAYAN stopajların toplamı (Diğer Platform Ücretleri için).
     */
    @Query("SELECT COALESCE(SUM(s.amount), 0) FROM TrendyolStoppage s " +
            "WHERE s.store.id = :storeId " +
            "AND s.transactionDate BETWEEN :startDate AND :endDate " +
            "AND LOWER(s.description) NOT LIKE '%uluslararası%' " +
            "AND LOWER(s.description) NOT LIKE '%yurt dışı operasyon%' " +
            "AND LOWER(s.description) NOT LIKE '%termin%' " +
            "AND LOWER(s.description) NOT LIKE '%platform hizmet%' " +
            "AND LOWER(s.description) NOT LIKE '%fatura kontör%' " +
            "AND LOWER(s.description) NOT LIKE '%tedarik edememe%' " +
            "AND LOWER(s.description) NOT LIKE '%az-yurtdışı%' " +
            "AND LOWER(s.description) NOT LIKE '%az-platform%' " +
            "AND LOWER(s.description) NOT LIKE '%paketleme%' " +
            "AND LOWER(s.description) NOT LIKE '%depo hizmet%' " +
            "AND LOWER(s.description) NOT LIKE '%çağrı merkezi%' " +
            "AND LOWER(s.description) NOT LIKE '%fotoğraf%' " +
            "AND LOWER(s.description) NOT LIKE '%entegrasyon%' " +
            "AND LOWER(s.description) NOT LIKE '%depolama%' " +
            "AND LOWER(s.description) NOT LIKE '%stopaj%'")
    BigDecimal sumOtherPlatformFees(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Stopaj kayıtlarının toplamı (sadece stopaj olanlar).
     */
    @Query("SELECT COALESCE(SUM(s.amount), 0) FROM TrendyolStoppage s " +
            "WHERE s.store.id = :storeId " +
            "AND s.transactionDate BETWEEN :startDate AND :endDate " +
            "AND LOWER(s.description) LIKE '%stopaj%'")
    BigDecimal sumStoppageOnly(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
