package com.ecommerce.sellerx.admin;

import com.ecommerce.sellerx.activitylog.ActivityLog;
import com.ecommerce.sellerx.activitylog.ActivityLogRepository;
import com.ecommerce.sellerx.admin.dto.AdminActivityLogDto;
import com.ecommerce.sellerx.admin.dto.AdminSecuritySummaryDto;
import com.ecommerce.sellerx.admin.dto.SuspiciousAccountDto;
import com.ecommerce.sellerx.admin.dto.SuspiciousIpDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    /**
     * Get activity logs with optional filters for email and action.
     * If both filters are null, returns all logs paginated.
     */
    public Page<AdminActivityLogDto> getActivityLogs(String email, String action, Pageable pageable) {
        Page<ActivityLog> page;

        if (email != null && !email.isBlank()) {
            page = activityLogRepository.findByEmailContaining(email.trim(), pageable);
        } else if (action != null && !action.isBlank()) {
            page = activityLogRepository.findByActionOrderByCreatedAtDesc(action.trim(), pageable);
        } else {
            page = activityLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return page.map(this::toDto);
    }

    /**
     * Build a security summary with failed login counts, suspicious IPs, and suspicious accounts.
     */
    public AdminSecuritySummaryDto getSecuritySummary() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twentyFourHoursAgo = now.minusHours(24);
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        long failedLogins24h = activityLogRepository.countFailedLoginsSince(twentyFourHoursAgo);
        long failedLogins7d = activityLogRepository.countFailedLoginsSince(sevenDaysAgo);
        long totalLoginsToday = activityLogRepository.countLoginsSince(startOfToday);

        // IPs with failed logins in the last 7 days
        List<SuspiciousIpDto> suspiciousIps = activityLogRepository.topFailedLoginIpsSince(sevenDaysAgo)
                .stream()
                .limit(20)
                .map(row -> SuspiciousIpDto.builder()
                        .ipAddress(row.getIpAddress())
                        .failedCount(row.getFailedCount())
                        .build())
                .collect(Collectors.toList());

        // Accounts with 3+ failed logins in the last 7 days
        List<SuspiciousAccountDto> suspiciousAccounts = activityLogRepository
                .suspiciousAccountsSince(sevenDaysAgo, 3)
                .stream()
                .limit(20)
                .map(row -> SuspiciousAccountDto.builder()
                        .email(row.getEmail())
                        .failedCount(row.getFailedCount())
                        .build())
                .collect(Collectors.toList());

        return AdminSecuritySummaryDto.builder()
                .failedLogins24h(failedLogins24h)
                .failedLogins7d(failedLogins7d)
                .suspiciousIps(suspiciousIps)
                .suspiciousAccounts(suspiciousAccounts)
                .totalLoginsToday(totalLoginsToday)
                .build();
    }

    private AdminActivityLogDto toDto(ActivityLog log) {
        return AdminActivityLogDto.builder()
                .id(log.getId())
                .email(log.getEmail())
                .action(log.getAction())
                .ipAddress(log.getIpAddress())
                .device(log.getDevice())
                .browser(log.getBrowser())
                .success(log.getSuccess())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
