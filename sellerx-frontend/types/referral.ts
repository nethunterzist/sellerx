export type ReferralStatus = 'PENDING' | 'COMPLETED' | 'EXPIRED';

export interface ReferralDto {
  id: string;
  referredUserName: string;
  referredUserEmail: string;
  status: ReferralStatus;
  rewardDaysGranted: number;
  createdAt: string;
  rewardAppliedAt: string | null;
}

export interface ReferralStats {
  referralCode: string | null;
  referralLink: string | null;
  totalReferrals: number;
  completedReferrals: number;
  pendingReferrals: number;
  totalDaysEarned: number;
  maxBonusDaysRemaining: number;
  canRefer: boolean;
  referrals: ReferralDto[];
}
