package com.ecommerce.sellerx.referral;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/referrals")
@RequiredArgsConstructor
@Slf4j
public class ReferralController {

    private final ReferralService referralService;

    /**
     * GET /api/referrals/stats - Get referral dashboard stats
     */
    @GetMapping("/stats")
    public ResponseEntity<ReferralStatsDto> getStats() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(referralService.getReferralStats(userId));
    }

    /**
     * POST /api/referrals/code - Generate or retrieve referral code
     */
    @PostMapping("/code")
    public ResponseEntity<Map<String, String>> generateCode() {
        Long userId = getCurrentUserId();
        String code = referralService.getOrCreateReferralCode(userId);
        return ResponseEntity.ok(Map.of("code", code));
    }

    /**
     * GET /api/referrals/validate/{code} - Validate referral code (public)
     */
    @GetMapping("/validate/{code}")
    public ResponseEntity<Map<String, Boolean>> validateCode(@PathVariable String code) {
        boolean valid = referralService.isValidReferralCode(code);
        return ResponseEntity.ok(Map.of("valid", valid));
    }

    private Long getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.valueOf(auth.getPrincipal().toString());
    }
}
