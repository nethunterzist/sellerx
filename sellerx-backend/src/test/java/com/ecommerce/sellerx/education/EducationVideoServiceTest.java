package com.ecommerce.sellerx.education;

import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.notifications.NotificationService;
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
import static org.mockito.Mockito.*;

class EducationVideoServiceTest extends BaseUnitTest {

    @Mock
    private EducationVideoRepository videoRepository;

    @Mock
    private VideoWatchHistoryRepository watchHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    private EducationVideoService educationVideoService;

    private User testUser;
    private EducationVideo testVideo;
    private UUID videoId;

    @BeforeEach
    void setUp() {
        educationVideoService = new EducationVideoService(
                videoRepository, watchHistoryRepository, userRepository, notificationService
        );

        testUser = User.builder()
                .name("Test User")
                .email("test@test.com")
                .build();
        testUser.setId(1L);

        videoId = UUID.randomUUID();
        testVideo = EducationVideo.builder()
                .title("Getting Started with SellerX")
                .description("Learn the basics of SellerX")
                .category(VideoCategory.GETTING_STARTED)
                .duration("5:30")
                .videoUrl("https://youtube.com/embed/abc123")
                .thumbnailUrl("https://img.youtube.com/vi/abc123/0.jpg")
                .videoType(VideoType.YOUTUBE)
                .order(1)
                .isActive(true)
                .createdBy(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testVideo.setId(videoId);
    }

    @Nested
    @DisplayName("getAllVideos")
    class GetAllVideos {

        @Test
        @DisplayName("should return all active videos ordered by order")
        void shouldReturnAllActiveVideos() {
            when(videoRepository.findByIsActiveTrueOrderByOrderAsc()).thenReturn(List.of(testVideo));

            List<EducationVideoDto> result = educationVideoService.getAllVideos();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Getting Started with SellerX");
            assertThat(result.get(0).getCategory()).isEqualTo(VideoCategory.GETTING_STARTED);
            assertThat(result.get(0).getIsActive()).isTrue();
        }

        @Test
        @DisplayName("should return empty list when no active videos exist")
        void shouldReturnEmptyListWhenNoActiveVideos() {
            when(videoRepository.findByIsActiveTrueOrderByOrderAsc()).thenReturn(List.of());

            List<EducationVideoDto> result = educationVideoService.getAllVideos();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getVideosByCategory")
    class GetVideosByCategory {

        @Test
        @DisplayName("should return videos filtered by category")
        void shouldReturnVideosFilteredByCategory() {
            when(videoRepository.findByCategoryAndIsActiveTrueOrderByOrderAsc(VideoCategory.GETTING_STARTED))
                    .thenReturn(List.of(testVideo));

            List<EducationVideoDto> result = educationVideoService.getVideosByCategory(VideoCategory.GETTING_STARTED);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCategory()).isEqualTo(VideoCategory.GETTING_STARTED);
        }
    }

    @Nested
    @DisplayName("getVideoById")
    class GetVideoById {

        @Test
        @DisplayName("should return video when found")
        void shouldReturnVideoWhenFound() {
            when(videoRepository.findById(videoId)).thenReturn(Optional.of(testVideo));

            EducationVideoDto result = educationVideoService.getVideoById(videoId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(videoId);
            assertThat(result.getTitle()).isEqualTo("Getting Started with SellerX");
        }

        @Test
        @DisplayName("should throw when video not found")
        void shouldThrowWhenVideoNotFound() {
            UUID missingId = UUID.randomUUID();
            when(videoRepository.findById(missingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> educationVideoService.getVideoById(missingId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Video not found");
        }
    }

    @Nested
    @DisplayName("createVideo")
    class CreateVideo {

        @Test
        @DisplayName("should create video and send notification")
        void shouldCreateVideoAndSendNotification() {
            CreateVideoRequest request = new CreateVideoRequest();
            request.setTitle("New Video");
            request.setDescription("Description");
            request.setCategory(VideoCategory.PRODUCTS);
            request.setDuration("10:00");
            request.setVideoUrl("https://youtube.com/embed/xyz");
            request.setThumbnailUrl("https://img.youtube.com/vi/xyz/0.jpg");
            request.setVideoType(VideoType.YOUTUBE);
            request.setOrder(2);
            request.setIsActive(true);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(videoRepository.save(any(EducationVideo.class))).thenAnswer(invocation -> {
                EducationVideo v = invocation.getArgument(0);
                v.setId(UUID.randomUUID());
                v.setCreatedAt(LocalDateTime.now());
                v.setUpdatedAt(LocalDateTime.now());
                return v;
            });

            EducationVideoDto result = educationVideoService.createVideo(request, 1L);

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("New Video");
            assertThat(result.getCategory()).isEqualTo(VideoCategory.PRODUCTS);
            verify(videoRepository).save(any(EducationVideo.class));
            verify(notificationService).createVideoAddedNotification(any(EducationVideo.class));
        }

        @Test
        @DisplayName("should default isActive to true when null")
        void shouldDefaultIsActiveToTrueWhenNull() {
            CreateVideoRequest request = new CreateVideoRequest();
            request.setTitle("Video");
            request.setDescription("Desc");
            request.setCategory(VideoCategory.ORDERS);
            request.setDuration("3:00");
            request.setVideoUrl("https://youtube.com/embed/def");
            request.setVideoType(VideoType.YOUTUBE);
            request.setOrder(1);
            request.setIsActive(null);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(videoRepository.save(any(EducationVideo.class))).thenAnswer(invocation -> {
                EducationVideo v = invocation.getArgument(0);
                v.setId(UUID.randomUUID());
                v.setCreatedAt(LocalDateTime.now());
                v.setUpdatedAt(LocalDateTime.now());
                return v;
            });

            EducationVideoDto result = educationVideoService.createVideo(request, 1L);

            assertThat(result.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("should throw when user not found")
        void shouldThrowWhenUserNotFound() {
            CreateVideoRequest request = new CreateVideoRequest();
            request.setTitle("Video");
            request.setCategory(VideoCategory.ANALYTICS);
            request.setDuration("1:00");
            request.setVideoUrl("url");
            request.setVideoType(VideoType.UPLOADED);
            request.setOrder(1);

            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> educationVideoService.createVideo(request, 999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not found");
        }
    }

    @Nested
    @DisplayName("updateVideo")
    class UpdateVideo {

        @Test
        @DisplayName("should update only provided fields")
        void shouldUpdateOnlyProvidedFields() {
            UpdateVideoRequest request = new UpdateVideoRequest();
            request.setTitle("Updated Title");
            request.setOrder(5);

            when(videoRepository.findById(videoId)).thenReturn(Optional.of(testVideo));
            when(videoRepository.save(any(EducationVideo.class))).thenReturn(testVideo);

            EducationVideoDto result = educationVideoService.updateVideo(videoId, request);

            assertThat(testVideo.getTitle()).isEqualTo("Updated Title");
            assertThat(testVideo.getOrder()).isEqualTo(5);
            assertThat(testVideo.getDescription()).isEqualTo("Learn the basics of SellerX");
            verify(videoRepository).save(testVideo);
        }

        @Test
        @DisplayName("should throw when video not found for update")
        void shouldThrowWhenVideoNotFoundForUpdate() {
            UUID missingId = UUID.randomUUID();
            UpdateVideoRequest request = new UpdateVideoRequest();
            request.setTitle("New Title");

            when(videoRepository.findById(missingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> educationVideoService.updateVideo(missingId, request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Video not found");
        }
    }

    @Nested
    @DisplayName("deleteVideo")
    class DeleteVideo {

        @Test
        @DisplayName("should soft-delete by setting isActive to false")
        void shouldSoftDeleteBySettingIsActiveToFalse() {
            when(videoRepository.findById(videoId)).thenReturn(Optional.of(testVideo));

            educationVideoService.deleteVideo(videoId);

            assertThat(testVideo.getIsActive()).isFalse();
            verify(videoRepository).save(testVideo);
        }
    }

    @Nested
    @DisplayName("markAsWatched")
    class MarkAsWatched {

        @Test
        @DisplayName("should create new watch history when not previously watched")
        void shouldCreateNewWatchHistory() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(videoRepository.findById(videoId)).thenReturn(Optional.of(testVideo));
            when(watchHistoryRepository.findByUserAndVideo(testUser, testVideo)).thenReturn(Optional.empty());

            educationVideoService.markAsWatched(videoId, 1L);

            verify(watchHistoryRepository).save(any(VideoWatchHistory.class));
        }

        @Test
        @DisplayName("should update existing watch history when already watched")
        void shouldUpdateExistingWatchHistory() {
            VideoWatchHistory existing = VideoWatchHistory.builder()
                    .user(testUser)
                    .video(testVideo)
                    .watchedAt(LocalDateTime.now().minusDays(1))
                    .build();
            existing.setId(UUID.randomUUID());

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(videoRepository.findById(videoId)).thenReturn(Optional.of(testVideo));
            when(watchHistoryRepository.findByUserAndVideo(testUser, testVideo)).thenReturn(Optional.of(existing));

            educationVideoService.markAsWatched(videoId, 1L);

            assertThat(existing.getWatchedAt()).isAfter(LocalDateTime.now().minusMinutes(1));
            verify(watchHistoryRepository).save(existing);
        }
    }

    @Nested
    @DisplayName("getUserWatchStatus")
    class GetUserWatchStatus {

        @Test
        @DisplayName("should return list of watched video IDs")
        void shouldReturnListOfWatchedVideoIds() {
            List<UUID> watchedIds = List.of(videoId, UUID.randomUUID());
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(watchHistoryRepository.findWatchedVideoIdsByUser(testUser)).thenReturn(watchedIds);

            VideoWatchStatusDto result = educationVideoService.getUserWatchStatus(1L);

            assertThat(result.getWatchedVideoIds()).hasSize(2);
            assertThat(result.getWatchedVideoIds()).contains(videoId);
        }

        @Test
        @DisplayName("should throw when user not found")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> educationVideoService.getUserWatchStatus(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not found");
        }
    }
}
