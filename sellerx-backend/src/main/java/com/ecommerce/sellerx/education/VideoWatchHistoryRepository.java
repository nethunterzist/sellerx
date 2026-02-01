package com.ecommerce.sellerx.education;

import com.ecommerce.sellerx.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoWatchHistoryRepository extends JpaRepository<VideoWatchHistory, UUID> {
    
    // Kullanıcının izlediği tüm videoları getir
    List<VideoWatchHistory> findByUser(User user);
    
    // Video için tüm izlenme kayıtlarını getir
    List<VideoWatchHistory> findByVideo(EducationVideo video);
    
    // Kullanıcının belirli bir videoyu izleyip izlemediğini kontrol et
    boolean existsByUserAndVideo(User user, EducationVideo video);
    
    // Kullanıcının belirli bir videoyu izleme kaydını getir
    Optional<VideoWatchHistory> findByUserAndVideo(User user, EducationVideo video);
    
    // Kullanıcının izlediği video ID'lerini getir
    @Query("SELECT vwh.video.id FROM VideoWatchHistory vwh WHERE vwh.user = :user")
    List<UUID> findWatchedVideoIdsByUser(@Param("user") User user);
}
