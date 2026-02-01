package com.ecommerce.sellerx.admin;

import com.ecommerce.sellerx.admin.dto.AdminReferralDto;
import com.ecommerce.sellerx.admin.dto.AdminReferralStatsDto;
import com.ecommerce.sellerx.admin.dto.TopReferrerDto;
import com.ecommerce.sellerx.referral.Referral;
import com.ecommerce.sellerx.referral.ReferralRepository;
import com.ecommerce.sellerx.referral.ReferralStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminReferralService {

    private final ReferralRepository referralRepository;

    @Transactional(readOnly = true)
    public Page<AdminReferralDto> getReferrals(Pageable pageable) {
        return referralRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public AdminReferralStatsDto getReferralStats() {
        long total = referralRepository.count();
        long completed = referralRepository.countByStatus(ReferralStatus.COMPLETED);
        long pending = referralRepository.countByStatus(ReferralStatus.PENDING);
        long totalRewardDays = referralRepository.sumAllRewardDays();

        var topReferrersRaw = referralRepository.findTopReferrers(PageRequest.of(0, 10));
        List<TopReferrerDto> topReferrers = topReferrersRaw.stream()
                .map(row -> TopReferrerDto.builder()
                        .email(row.getEmail())
                        .referralCount(row.getReferralCount())
                        .totalRewardDays(row.getTotalRewardDays())
                        .build())
                .toList();

        return AdminReferralStatsDto.builder()
                .total(total)
                .completed(completed)
                .pending(pending)
                .totalRewardDays(totalRewardDays)
                .topReferrers(topReferrers)
                .build();
    }

    private AdminReferralDto toDto(Referral referral) {
        return AdminReferralDto.builder()
                .id(referral.getId())
                .referrerEmail(referral.getReferrerUser().getEmail())
                .referredEmail(referral.getReferredUser().getEmail())
                .code(referral.getReferralCode())
                .status(referral.getStatus().name())
                .rewardDays(referral.getRewardDaysGranted())
                .createdAt(referral.getCreatedAt())
                .build();
    }
}
