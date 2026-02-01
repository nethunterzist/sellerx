package com.ecommerce.sellerx.education;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EducationVideoRepository extends JpaRepository<EducationVideo, UUID> {
    
    // Tüm aktif videoları order'a göre sıralı getir
    List<EducationVideo> findByIsActiveTrueOrderByOrderAsc();
    
    // Kategoriye göre aktif videoları getir
    List<EducationVideo> findByCategoryAndIsActiveTrueOrderByOrderAsc(VideoCategory category);
    
    // ID'ye göre video getir (aktif olup olmadığına bakmaksızın)
    Optional<EducationVideo> findById(UUID id);
    
    // Tüm videoları getir (admin için)
    List<EducationVideo> findAllByOrderByOrderAsc();
}
