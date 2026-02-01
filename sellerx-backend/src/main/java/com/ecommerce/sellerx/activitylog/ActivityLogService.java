package com.ecommerce.sellerx.activitylog;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityLogService {
    private final ActivityLogRepository activityLogRepository;

    /**
     * Log successful login
     */
    public void logLogin(Long userId, String email, HttpServletRequest request) {
        ActivityLog log = createLogEntry(userId, email, "login", true, request);
        activityLogRepository.save(log);
    }

    /**
     * Log failed login attempt
     */
    public void logFailedLogin(String email, HttpServletRequest request) {
        ActivityLog log = createLogEntry(null, email, "failed_login", false, request);
        activityLogRepository.save(log);
    }

    /**
     * Log logout
     */
    public void logLogout(Long userId, String email, HttpServletRequest request) {
        ActivityLog log = createLogEntry(userId, email, "logout", true, request);
        activityLogRepository.save(log);
    }

    /**
     * Get activity logs for a user
     */
    public List<ActivityLogDto> getActivityLogs(Long userId, int limit) {
        List<ActivityLog> logs = activityLogRepository.findByUserIdOrderByCreatedAtDesc(
                userId,
                PageRequest.of(0, limit)
        );
        return logs.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private ActivityLog createLogEntry(Long userId, String email, String action, boolean success, HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String ipAddress = getClientIpAddress(request);
        String device = parseDevice(userAgent);
        String browser = parseBrowser(userAgent);

        return ActivityLog.builder()
                .userId(userId)
                .email(email)
                .action(action)
                .device(device)
                .browser(browser)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .success(success)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private String parseDevice(String userAgent) {
        if (userAgent == null) return "Unknown";

        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("mobile") || userAgent.contains("android") || userAgent.contains("iphone")) {
            return "Mobile";
        } else if (userAgent.contains("tablet") || userAgent.contains("ipad")) {
            return "Tablet";
        }
        return "Desktop";
    }

    private String parseBrowser(String userAgent) {
        if (userAgent == null) return "Unknown";

        if (userAgent.contains("Edg/")) return "Microsoft Edge";
        if (userAgent.contains("Chrome/") && !userAgent.contains("Chromium/")) return "Google Chrome";
        if (userAgent.contains("Firefox/")) return "Mozilla Firefox";
        if (userAgent.contains("Safari/") && !userAgent.contains("Chrome/")) return "Safari";
        if (userAgent.contains("Opera/") || userAgent.contains("OPR/")) return "Opera";

        return "Unknown Browser";
    }

    private ActivityLogDto toDto(ActivityLog log) {
        return ActivityLogDto.builder()
                .id(log.getId())
                .action(log.getAction())
                .device(log.getDevice())
                .browser(log.getBrowser())
                .ipAddress(log.getIpAddress())
                .success(log.getSuccess())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
