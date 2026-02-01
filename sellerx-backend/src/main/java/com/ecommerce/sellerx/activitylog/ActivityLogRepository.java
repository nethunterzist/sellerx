package com.ecommerce.sellerx.activitylog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // All logs paginated (admin)
    Page<ActivityLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Filter by action
    Page<ActivityLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);

    // Filter by email (partial match)
    @Query("SELECT a FROM ActivityLog a WHERE LOWER(a.email) LIKE LOWER(CONCAT('%', :email, '%')) ORDER BY a.createdAt DESC")
    Page<ActivityLog> findByEmailContaining(@Param("email") String email, Pageable pageable);

    // Count failed logins since date
    @Query("SELECT COUNT(a) FROM ActivityLog a WHERE a.action = 'failed_login' AND a.createdAt > :since")
    long countFailedLoginsSince(@Param("since") LocalDateTime since);

    // Top failed login IPs
    @Query("SELECT a.ipAddress AS ipAddress, COUNT(a) AS failedCount FROM ActivityLog a WHERE a.action = 'failed_login' AND a.createdAt > :since GROUP BY a.ipAddress ORDER BY failedCount DESC")
    List<IpFailedCountProjection> topFailedLoginIpsSince(@Param("since") LocalDateTime since);

    // Accounts with most failed logins
    @Query("SELECT a.email AS email, COUNT(a) AS failedCount FROM ActivityLog a WHERE a.action = 'failed_login' AND a.createdAt > :since AND a.email IS NOT NULL GROUP BY a.email HAVING COUNT(a) >= :threshold ORDER BY failedCount DESC")
    List<EmailFailedCountProjection> suspiciousAccountsSince(@Param("since") LocalDateTime since, @Param("threshold") long threshold);

    // Count logins today
    @Query("SELECT COUNT(a) FROM ActivityLog a WHERE a.action = 'login' AND a.createdAt > :since")
    long countLoginsSince(@Param("since") LocalDateTime since);
}
