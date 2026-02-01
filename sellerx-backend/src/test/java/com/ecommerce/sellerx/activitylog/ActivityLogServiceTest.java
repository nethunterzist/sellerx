package com.ecommerce.sellerx.activitylog;

import com.ecommerce.sellerx.common.BaseUnitTest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ActivityLogServiceTest extends BaseUnitTest {

    @Mock
    private ActivityLogRepository activityLogRepository;

    @Mock
    private HttpServletRequest httpServletRequest;

    private ActivityLogService activityLogService;

    private static final String CHROME_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    @BeforeEach
    void setUp() {
        activityLogService = new ActivityLogService(activityLogRepository);

        lenient().when(httpServletRequest.getHeader("User-Agent")).thenReturn(CHROME_USER_AGENT);
        lenient().when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        lenient().when(httpServletRequest.getHeader("X-Real-IP")).thenReturn(null);
        lenient().when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @Nested
    @DisplayName("logLogin")
    class LogLogin {

        @Test
        @DisplayName("should save login activity log with correct fields")
        void shouldSaveLoginLog() {
            when(activityLogRepository.save(any(ActivityLog.class))).thenAnswer(i -> i.getArgument(0));

            activityLogService.logLogin(1L, "test@test.com", httpServletRequest);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            verify(activityLogRepository).save(captor.capture());
            ActivityLog saved = captor.getValue();

            assertThat(saved.getUserId()).isEqualTo(1L);
            assertThat(saved.getEmail()).isEqualTo("test@test.com");
            assertThat(saved.getAction()).isEqualTo("login");
            assertThat(saved.getSuccess()).isTrue();
            assertThat(saved.getIpAddress()).isEqualTo("127.0.0.1");
            assertThat(saved.getBrowser()).isEqualTo("Google Chrome");
            assertThat(saved.getDevice()).isEqualTo("Desktop");
        }
    }

    @Nested
    @DisplayName("logFailedLogin")
    class LogFailedLogin {

        @Test
        @DisplayName("should save failed login with null userId")
        void shouldSaveFailedLoginLog() {
            when(activityLogRepository.save(any(ActivityLog.class))).thenAnswer(i -> i.getArgument(0));

            activityLogService.logFailedLogin("bad@test.com", httpServletRequest);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            verify(activityLogRepository).save(captor.capture());
            ActivityLog saved = captor.getValue();

            assertThat(saved.getUserId()).isNull();
            assertThat(saved.getEmail()).isEqualTo("bad@test.com");
            assertThat(saved.getAction()).isEqualTo("failed_login");
            assertThat(saved.getSuccess()).isFalse();
        }
    }

    @Nested
    @DisplayName("logLogout")
    class LogLogout {

        @Test
        @DisplayName("should save logout activity log")
        void shouldSaveLogoutLog() {
            when(activityLogRepository.save(any(ActivityLog.class))).thenAnswer(i -> i.getArgument(0));

            activityLogService.logLogout(1L, "test@test.com", httpServletRequest);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            verify(activityLogRepository).save(captor.capture());
            ActivityLog saved = captor.getValue();

            assertThat(saved.getAction()).isEqualTo("logout");
            assertThat(saved.getSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("IP address extraction")
    class IpAddressExtraction {

        @Test
        @DisplayName("should extract IP from X-Forwarded-For header")
        void shouldExtractFromXForwardedFor() {
            when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.50, 70.41.3.18");
            when(activityLogRepository.save(any(ActivityLog.class))).thenAnswer(i -> i.getArgument(0));

            activityLogService.logLogin(1L, "test@test.com", httpServletRequest);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            verify(activityLogRepository).save(captor.capture());
            assertThat(captor.getValue().getIpAddress()).isEqualTo("203.0.113.50");
        }

        @Test
        @DisplayName("should extract IP from X-Real-IP when X-Forwarded-For is absent")
        void shouldExtractFromXRealIp() {
            when(httpServletRequest.getHeader("X-Real-IP")).thenReturn("10.0.0.1");
            when(activityLogRepository.save(any(ActivityLog.class))).thenAnswer(i -> i.getArgument(0));

            activityLogService.logLogin(1L, "test@test.com", httpServletRequest);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            verify(activityLogRepository).save(captor.capture());
            assertThat(captor.getValue().getIpAddress()).isEqualTo("10.0.0.1");
        }
    }

    @Nested
    @DisplayName("Device and browser parsing")
    class DeviceAndBrowserParsing {

        @Test
        @DisplayName("should detect mobile device from user agent")
        void shouldDetectMobileDevice() {
            String mobileAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)";
            when(httpServletRequest.getHeader("User-Agent")).thenReturn(mobileAgent);
            when(activityLogRepository.save(any(ActivityLog.class))).thenAnswer(i -> i.getArgument(0));

            activityLogService.logLogin(1L, "test@test.com", httpServletRequest);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            verify(activityLogRepository).save(captor.capture());
            assertThat(captor.getValue().getDevice()).isEqualTo("Mobile");
        }

        @Test
        @DisplayName("should detect Firefox browser")
        void shouldDetectFirefox() {
            String firefoxAgent = "Mozilla/5.0 (X11; Linux x86_64; rv:121.0) Gecko/20100101 Firefox/121.0";
            when(httpServletRequest.getHeader("User-Agent")).thenReturn(firefoxAgent);
            when(activityLogRepository.save(any(ActivityLog.class))).thenAnswer(i -> i.getArgument(0));

            activityLogService.logLogin(1L, "test@test.com", httpServletRequest);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            verify(activityLogRepository).save(captor.capture());
            assertThat(captor.getValue().getBrowser()).isEqualTo("Mozilla Firefox");
        }
    }

    @Nested
    @DisplayName("getActivityLogs")
    class GetActivityLogs {

        @Test
        @DisplayName("should return mapped DTOs for user logs")
        void shouldReturnMappedDtos() {
            ActivityLog log = ActivityLog.builder()
                    .userId(1L)
                    .email("test@test.com")
                    .action("login")
                    .device("Desktop")
                    .browser("Google Chrome")
                    .ipAddress("127.0.0.1")
                    .success(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            log.setId(1L);

            when(activityLogRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(PageRequest.class)))
                    .thenReturn(List.of(log));

            List<ActivityLogDto> result = activityLogService.getActivityLogs(1L, 10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAction()).isEqualTo("login");
            assertThat(result.get(0).getDevice()).isEqualTo("Desktop");
            assertThat(result.get(0).getBrowser()).isEqualTo("Google Chrome");
            assertThat(result.get(0).getSuccess()).isTrue();
        }
    }
}
