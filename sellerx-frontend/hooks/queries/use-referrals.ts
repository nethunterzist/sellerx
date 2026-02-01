import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiRequest } from '@/lib/api/client';
import type { ReferralStats } from '@/types/referral';

export const referralKeys = {
  all: ['referrals'] as const,
  stats: () => [...referralKeys.all, 'stats'] as const,
};

export function useReferralStats() {
  return useQuery({
    queryKey: referralKeys.stats(),
    queryFn: () => apiRequest<ReferralStats>('/referrals/stats'),
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
}

export function useGenerateReferralCode() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () =>
      apiRequest<{ code: string }>('/referrals/code', { method: 'POST' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: referralKeys.stats() });
    },
  });
}

export function useValidateReferralCode(code: string | null) {
  return useQuery({
    queryKey: [...referralKeys.all, 'validate', code],
    queryFn: () =>
      apiRequest<{ valid: boolean }>(`/referrals/validate/${code}`),
    enabled: !!code && code.length >= 4,
    staleTime: 1000 * 60 * 10, // 10 minutes
  });
}
