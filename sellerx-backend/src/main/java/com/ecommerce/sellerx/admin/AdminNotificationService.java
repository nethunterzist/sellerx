package com.ecommerce.sellerx.admin;

import com.ecommerce.sellerx.admin.dto.AdminBroadcastRequest;
import com.ecommerce.sellerx.admin.dto.AdminNotificationStatsDto;
import com.ecommerce.sellerx.notifications.NotificationType;
import com.ecommerce.sellerx.notifications.UserNotification;
import com.ecommerce.sellerx.notifications.UserNotificationRepository;
import com.ecommerce.sellerx.users.User;
import com.ecommerce.sellerx.users.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminNotificationService {

    private final UserNotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public AdminNotificationStatsDto getNotificationStats() {
        long total = notificationRepository.count();
        long totalUnread = notificationRepository.countByReadFalse();

        var typeCountsRaw = notificationRepository.countByType();
        Map<String, Long> byType = new LinkedHashMap<>();
        for (var row : typeCountsRaw) {
            byType.put(row.getNotificationType().name(), row.getTypeCount());
        }

        return AdminNotificationStatsDto.builder()
                .total(total)
                .totalUnread(totalUnread)
                .byType(byType)
                .build();
    }

    @Transactional
    public int broadcastNotification(AdminBroadcastRequest request) {
        NotificationType type;
        try {
            type = NotificationType.valueOf(request.getType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid notification type: " + request.getType()
                    + ". Valid types: " + java.util.Arrays.toString(NotificationType.values()));
        }

        List<User> allUsers = userRepository.findAll();

        for (User user : allUsers) {
            UserNotification notification = UserNotification.builder()
                    .user(user)
                    .type(type)
                    .title(request.getTitle())
                    .message(request.getMessage())
                    .link(request.getLink())
                    .read(false)
                    .build();
            notificationRepository.save(notification);
        }

        log.info("Admin broadcast notification sent: type={}, title='{}', recipients={}",
                type, request.getTitle(), allUsers.size());

        return allUsers.size();
    }
}
