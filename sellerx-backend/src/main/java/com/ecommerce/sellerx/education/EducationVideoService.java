package com.ecommerce.sellerx.education;

import com.ecommerce.sellerx.notifications.NotificationService;
import com.ecommerce.sellerx.notifications.NotificationType;
import com.ecommerce.sellerx.users.User;
import com.ecommerce.sellerx.users.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class EducationVideoService {
    
    private final EducationVideoRepository videoRepository;
    private final VideoWatchHistoryRepository watchHistoryRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    
    public List<EducationVideoDto> getAllVideos() {
        return videoRepository.findByIsActiveTrueOrderByOrderAsc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    public List<EducationVideoDto> getVideosByCategory(VideoCategory category) {
        return videoRepository.findByCategoryAndIsActiveTrueOrderByOrderAsc(category)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    public EducationVideoDto getVideoById(UUID id) {
        EducationVideo video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        return toDto(video);
    }
    
    @Transactional
    public EducationVideoDto createVideo(CreateVideoRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        EducationVideo video = EducationVideo.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .duration(request.getDuration())
                .videoUrl(request.getVideoUrl())
                .thumbnailUrl(request.getThumbnailUrl())
                .videoType(request.getVideoType())
                .order(request.getOrder())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .createdBy(user)
                .build();
        
        video = videoRepository.save(video);
        
        // Tüm aktif kullanıcılara bildirim oluştur
        notificationService.createVideoAddedNotification(video);
        
        return toDto(video);
    }
    
    @Transactional
    public EducationVideoDto updateVideo(UUID id, UpdateVideoRequest request) {
        EducationVideo video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        
        if (request.getTitle() != null) {
            video.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            video.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            video.setCategory(request.getCategory());
        }
        if (request.getDuration() != null) {
            video.setDuration(request.getDuration());
        }
        if (request.getVideoUrl() != null) {
            video.setVideoUrl(request.getVideoUrl());
        }
        if (request.getThumbnailUrl() != null) {
            video.setThumbnailUrl(request.getThumbnailUrl());
        }
        if (request.getVideoType() != null) {
            video.setVideoType(request.getVideoType());
        }
        if (request.getOrder() != null) {
            video.setOrder(request.getOrder());
        }
        if (request.getIsActive() != null) {
            video.setIsActive(request.getIsActive());
        }
        
        video = videoRepository.save(video);
        return toDto(video);
    }
    
    @Transactional
    public void deleteVideo(UUID id) {
        EducationVideo video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        // Soft delete
        video.setIsActive(false);
        videoRepository.save(video);
    }
    
    @Transactional
    public void markAsWatched(UUID videoId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        EducationVideo video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        
        // Eğer zaten izlenmişse, güncelle
        VideoWatchHistory existing = watchHistoryRepository.findByUserAndVideo(user, video).orElse(null);
        if (existing != null) {
            existing.setWatchedAt(java.time.LocalDateTime.now());
            watchHistoryRepository.save(existing);
        } else {
            // Yeni izlenme kaydı oluştur
            VideoWatchHistory watchHistory = VideoWatchHistory.builder()
                    .user(user)
                    .video(video)
                    .watchedAt(java.time.LocalDateTime.now())
                    .build();
            watchHistoryRepository.save(watchHistory);
        }
    }
    
    public VideoWatchStatusDto getUserWatchStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<UUID> watchedVideoIds = watchHistoryRepository.findWatchedVideoIdsByUser(user);
        return VideoWatchStatusDto.builder()
                .watchedVideoIds(watchedVideoIds)
                .build();
    }
    
    private EducationVideoDto toDto(EducationVideo video) {
        return EducationVideoDto.builder()
                .id(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .category(video.getCategory())
                .duration(video.getDuration())
                .videoUrl(video.getVideoUrl())
                .thumbnailUrl(video.getThumbnailUrl())
                .videoType(video.getVideoType())
                .order(video.getOrder())
                .isActive(video.getIsActive())
                .createdAt(video.getCreatedAt())
                .updatedAt(video.getUpdatedAt())
                .build();
    }
}
