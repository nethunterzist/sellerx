package com.ecommerce.sellerx.admin;

import com.ecommerce.sellerx.admin.dto.AdminReferralDto;
import com.ecommerce.sellerx.admin.dto.AdminReferralStatsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/referrals")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminReferralController {

    private final AdminReferralService adminReferralService;

    /**
     * List all referrals (paginated, newest first)
     * GET /api/admin/referrals?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Page<AdminReferralDto>> getReferrals(
            @PageableDefault(size = 20) Pageable pageable) {
        if (pageable.getPageSize() > 100) {
            pageable = PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort());
        }
        log.info("Admin fetching referrals: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<AdminReferralDto> referrals = adminReferralService.getReferrals(pageable);
        return ResponseEntity.ok(referrals);
    }

    /**
     * Get referral statistics
     * GET /api/admin/referrals/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<AdminReferralStatsDto> getReferralStats() {
        log.info("Admin fetching referral stats");
        AdminReferralStatsDto stats = adminReferralService.getReferralStats();
        return ResponseEntity.ok(stats);
    }
}
