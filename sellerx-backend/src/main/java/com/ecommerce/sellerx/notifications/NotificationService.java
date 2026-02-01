package com.ecommerce.sellerx.notifications;

import com.ecommerce.sellerx.education.EducationVideo;
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
public class NotificationService {
    
    private final UserNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    
    @Transactional
    public UserNotification createNotification(Long userId, NotificationType type, String title, String message, String link) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UserNotification notification = UserNotification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .link(link)
                .read(false)
                .build();
        
        return notificationRepository.save(notification);
    }
    
    /**
     * Yeni video eklendiğinde tüm aktif kullanıcılara bildirim oluştur
     */
    @Transactional
    public void createVideoAddedNotification(EducationVideo video) {
        List<User> allUsers = userRepository.findAll();
        
        for (User user : allUsers) {
            String link = "/education?video=" + video.getId();
            createNotification(
                    user.getId(),
                    NotificationType.VIDEO_ADDED,
                    "Yeni Eğitim Videosu: " + video.getTitle(),
                    video.getDescription() != null ? video.getDescription() : "Yeni bir eğitim videosu eklendi.",
                    link
            );
        }
    }
    
    public List<NotificationDto> getUserNotifications(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return notificationRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void markAsRead(UUID notificationId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        notificationRepository.markAsRead(notificationId, user);
    }
    
    public long getUnreadCount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return notificationRepository.countByUserAndReadFalse(user);
    }
    
    private NotificationDto toDto(UserNotification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .link(notification.getLink())
                .read(notification.getRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
