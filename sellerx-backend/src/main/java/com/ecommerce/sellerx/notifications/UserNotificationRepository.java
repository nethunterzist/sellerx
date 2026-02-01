package com.ecommerce.sellerx.notifications;

import com.ecommerce.sellerx.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {

    // Kullanıcının tüm bildirimlerini getir (yeni önce)
    List<UserNotification> findByUserOrderByCreatedAtDesc(User user);

    // Kullanıcının okunmamış bildirimlerini getir
    List<UserNotification> findByUserAndReadFalseOrderByCreatedAtDesc(User user);

    // Kullanıcının okunmamış bildirim sayısı
    long countByUserAndReadFalse(User user);

    // Bildirimi okundu işaretle
    @Modifying
    @Transactional
    @Query("UPDATE UserNotification n SET n.read = true WHERE n.id = :id AND n.user = :user")
    int markAsRead(@Param("id") UUID id, @Param("user") User user);

    // Kullanıcının tüm bildirimlerini okundu işaretle
    @Modifying
    @Transactional
    @Query("UPDATE UserNotification n SET n.read = true WHERE n.user = :user AND n.read = false")
    int markAllAsRead(@Param("user") User user);

    // Kullanıcının bildirimini getir
    Optional<UserNotification> findByIdAndUser(UUID id, User user);

    // --- Admin queries ---

    long countByReadFalse();

    @Query("SELECT n.type AS notificationType, COUNT(n) AS typeCount FROM UserNotification n GROUP BY n.type")
    List<NotificationTypeCountProjection> countByType();
}
