package com.ecommerce.sellerx.notifications;

import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.education.EducationVideo;
import com.ecommerce.sellerx.education.VideoCategory;
import com.ecommerce.sellerx.education.VideoType;
import com.ecommerce.sellerx.users.User;
import com.ecommerce.sellerx.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NotificationServiceTest extends BaseUnitTest {

    @Mock
    private UserNotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    private NotificationService notificationService;

    private User testUser;
    private User secondUser;
    private UserNotification testNotification;
    private UUID notificationId;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, userRepository);

        testUser = User.builder()
                .name("Test User")
                .email("test@test.com")
                .build();
        testUser.setId(1L);

        secondUser = User.builder()
                .name("Second User")
                .email("second@test.com")
                .build();
        secondUser.setId(2L);

        notificationId = UUID.randomUUID();
        testNotification = UserNotification.builder()
                .user(testUser)
                .type(NotificationType.VIDEO_ADDED)
                .title("New Video")
                .message("A new education video has been added")
                .link("/education?video=123")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
        testNotification.setId(notificationId);
    }

    @Nested
    @DisplayName("createNotification")
    class CreateNotification {

        @Test
        @DisplayName("should create notification for user")
        void shouldCreateNotificationForUser() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(notificationRepository.save(any(UserNotification.class))).thenAnswer(invocation -> {
                UserNotification n = invocation.getArgument(0);
                n.setId(UUID.randomUUID());
                return n;
            });

            UserNotification result = notificationService.createNotification(
                    1L, NotificationType.STOCK_ALERT, "Stock Alert", "Product X is low on stock", "/products"
            );

            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo(NotificationType.STOCK_ALERT);
            assertThat(result.getTitle()).isEqualTo("Stock Alert");
            assertThat(result.getMessage()).isEqualTo("Product X is low on stock");
            assertThat(result.getLink()).isEqualTo("/products");
            assertThat(result.getRead()).isFalse();
            verify(notificationRepository).save(any(UserNotification.class));
        }

        @Test
        @DisplayName("should throw when user not found")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.createNotification(
                    999L, NotificationType.SYSTEM, "Test", "Test", null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not found");
        }
    }

    @Nested
    @DisplayName("createVideoAddedNotification")
    class CreateVideoAddedNotification {

        @Test
        @DisplayName("should create notification for all users when video is added")
        void shouldCreateNotificationForAllUsers() {
            EducationVideo video = EducationVideo.builder()
                    .title("New Tutorial")
                    .description("Learn something new")
                    .category(VideoCategory.PRODUCTS)
                    .duration("5:00")
                    .videoUrl("https://youtube.com/embed/test")
                    .videoType(VideoType.YOUTUBE)
                    .order(1)
                    .isActive(true)
                    .build();
            video.setId(UUID.randomUUID());

            when(userRepository.findAll()).thenReturn(List.of(testUser, secondUser));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.findById(2L)).thenReturn(Optional.of(secondUser));
            when(notificationRepository.save(any(UserNotification.class))).thenAnswer(invocation -> {
                UserNotification n = invocation.getArgument(0);
                n.setId(UUID.randomUUID());
                return n;
            });

            notificationService.createVideoAddedNotification(video);

            verify(notificationRepository, times(2)).save(any(UserNotification.class));
        }

        @Test
        @DisplayName("should use default message when video description is null")
        void shouldUseDefaultMessageWhenDescriptionIsNull() {
            EducationVideo video = EducationVideo.builder()
                    .title("No Description Video")
                    .description(null)
                    .category(VideoCategory.SETTINGS)
                    .duration("3:00")
                    .videoUrl("url")
                    .videoType(VideoType.UPLOADED)
                    .order(1)
                    .isActive(true)
                    .build();
            video.setId(UUID.randomUUID());

            when(userRepository.findAll()).thenReturn(List.of(testUser));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(notificationRepository.save(any(UserNotification.class))).thenAnswer(invocation -> {
                UserNotification n = invocation.getArgument(0);
                n.setId(UUID.randomUUID());
                return n;
            });

            notificationService.createVideoAddedNotification(video);

            verify(notificationRepository).save(argThat(notification ->
                    notification.getMessage().equals("Yeni bir eğitim videosu eklendi.")));
        }
    }

    @Nested
    @DisplayName("getUserNotifications")
    class GetUserNotifications {

        @Test
        @DisplayName("should return notifications for user")
        void shouldReturnNotificationsForUser() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(notificationRepository.findByUserOrderByCreatedAtDesc(testUser))
                    .thenReturn(List.of(testNotification));

            List<NotificationDto> result = notificationService.getUserNotifications(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(notificationId);
            assertThat(result.get(0).getTitle()).isEqualTo("New Video");
            assertThat(result.get(0).getType()).isEqualTo(NotificationType.VIDEO_ADDED);
            assertThat(result.get(0).getRead()).isFalse();
        }

        @Test
        @DisplayName("should return empty list when no notifications")
        void shouldReturnEmptyListWhenNoNotifications() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(notificationRepository.findByUserOrderByCreatedAtDesc(testUser)).thenReturn(List.of());

            List<NotificationDto> result = notificationService.getUserNotifications(1L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("markAsRead")
    class MarkAsRead {

        @Test
        @DisplayName("should mark notification as read")
        void shouldMarkNotificationAsRead() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            notificationService.markAsRead(notificationId, 1L);

            verify(notificationRepository).markAsRead(notificationId, testUser);
        }

        @Test
        @DisplayName("should throw when user not found")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.markAsRead(notificationId, 999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not found");
        }
    }

    @Nested
    @DisplayName("getUnreadCount")
    class GetUnreadCount {

        @Test
        @DisplayName("should return unread notification count")
        void shouldReturnUnreadNotificationCount() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(notificationRepository.countByUserAndReadFalse(testUser)).thenReturn(5L);

            long result = notificationService.getUnreadCount(1L);

            assertThat(result).isEqualTo(5L);
        }

        @Test
        @DisplayName("should return zero when no unread notifications")
        void shouldReturnZeroWhenNoUnreadNotifications() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(notificationRepository.countByUserAndReadFalse(testUser)).thenReturn(0L);

            long result = notificationService.getUnreadCount(1L);

            assertThat(result).isEqualTo(0L);
        }
    }
}
