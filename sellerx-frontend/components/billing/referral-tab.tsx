'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useTranslations } from 'next-intl';
import {
  Copy,
  Check,
  Gift,
  Users,
  Clock,
  CalendarPlus,
  Link as LinkIcon,
  Loader2,
  ArrowUpCircle,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { useReferralStats, useGenerateReferralCode } from '@/hooks/queries/use-referrals';
import type { ReferralStatus } from '@/types/referral';

function StatusBadge({ status }: { status: ReferralStatus }) {
  const t = useTranslations('referral');
  const styles: Record<ReferralStatus, string> = {
    PENDING: 'bg-yellow-100 text-yellow-800',
    COMPLETED: 'bg-green-100 text-green-800',
    EXPIRED: 'bg-gray-100 text-gray-600',
  };
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${styles[status]}`}>
      {t(`status.${status}`)}
    </span>
  );
}

export function ReferralTab() {
  const t = useTranslations('referral');
  const { data: stats, isLoading } = useReferralStats();
  const generateCode = useGenerateReferralCode();
  const [copiedField, setCopiedField] = useState<'code' | 'link' | null>(null);

  const copyToClipboard = async (text: string, field: 'code' | 'link') => {
    await navigator.clipboard.writeText(text);
    setCopiedField(field);
    setTimeout(() => setCopiedField(null), 2000);
  };

  const handleGenerateCode = () => {
    generateCode.mutate();
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  // User can't refer (FREE plan)
  if (stats && !stats.canRefer && !stats.referralCode) {
    return (
      <Card>
        <CardContent className="flex flex-col items-center justify-center py-12 text-center">
          <ArrowUpCircle className="h-12 w-12 text-muted-foreground mb-4" />
          <h3 className="text-lg font-medium mb-2">{t('upgradeRequired')}</h3>
          <p className="text-sm text-muted-foreground mb-4 max-w-md">
            {t('upgradeDescription')}
          </p>
          <Button asChild>
            <Link href="/billing/checkout">{t('upgradeCta')}</Link>
          </Button>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      {/* Referral Code Card */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Gift className="h-5 w-5 text-[#1D70F1]" />
            {t('title')}
          </CardTitle>
          <CardDescription>{t('subtitle')}</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {stats?.referralCode ? (
            <>
              {/* Code */}
              <div className="space-y-2">
                <label className="text-sm font-medium text-muted-foreground">{t('yourCode')}</label>
                <div className="flex items-center gap-2">
                  <div className="flex-1 rounded-lg border bg-muted/50 px-4 py-2.5 font-mono text-lg font-bold tracking-wider">
                    {stats.referralCode}
                  </div>
                  <Button
                    variant="outline"
                    size="icon"
                    onClick={() => copyToClipboard(stats.referralCode!, 'code')}
                  >
                    {copiedField === 'code' ? (
                      <Check className="h-4 w-4 text-green-600" />
                    ) : (
                      <Copy className="h-4 w-4" />
                    )}
                  </Button>
                </div>
              </div>

              {/* Link */}
              {stats.referralLink && (
                <div className="space-y-2">
                  <label className="text-sm font-medium text-muted-foreground">{t('shareLink')}</label>
                  <div className="flex items-center gap-2">
                    <div className="flex-1 truncate rounded-lg border bg-muted/50 px-4 py-2.5 text-sm">
                      {stats.referralLink}
                    </div>
                    <Button
                      variant="outline"
                      size="icon"
                      onClick={() => copyToClipboard(stats.referralLink!, 'link')}
                    >
                      {copiedField === 'link' ? (
                        <Check className="h-4 w-4 text-green-600" />
                      ) : (
                        <LinkIcon className="h-4 w-4" />
                      )}
                    </Button>
                  </div>
                </div>
              )}
            </>
          ) : (
            <div className="text-center py-4">
              <p className="text-sm text-muted-foreground mb-3">{t('noCodeYet')}</p>
              <Button onClick={handleGenerateCode} disabled={generateCode.isPending}>
                {generateCode.isPending ? (
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                ) : (
                  <Gift className="mr-2 h-4 w-4" />
                )}
                {t('generateCode')}
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Stats Cards */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center gap-3">
              <div className="rounded-lg bg-blue-100 p-2">
                <Users className="h-5 w-5 text-blue-700" />
              </div>
              <div>
                <p className="text-2xl font-bold">{stats?.totalReferrals ?? 0}</p>
                <p className="text-xs text-muted-foreground">{t('stats.totalReferrals')}</p>
              </div>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center gap-3">
              <div className="rounded-lg bg-green-100 p-2">
                <Check className="h-5 w-5 text-green-700" />
              </div>
              <div>
                <p className="text-2xl font-bold">{stats?.completedReferrals ?? 0}</p>
                <p className="text-xs text-muted-foreground">{t('stats.completed')}</p>
              </div>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center gap-3">
              <div className="rounded-lg bg-yellow-100 p-2">
                <Clock className="h-5 w-5 text-yellow-700" />
              </div>
              <div>
                <p className="text-2xl font-bold">{stats?.pendingReferrals ?? 0}</p>
                <p className="text-xs text-muted-foreground">{t('stats.pending')}</p>
              </div>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center gap-3">
              <div className="rounded-lg bg-purple-100 p-2">
                <CalendarPlus className="h-5 w-5 text-purple-700" />
              </div>
              <div>
                <p className="text-2xl font-bold">{stats?.totalDaysEarned ?? 0}</p>
                <p className="text-xs text-muted-foreground">{t('stats.daysEarned')}</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Max bonus remaining info */}
      {stats && stats.maxBonusDaysRemaining > 0 && (
        <p className="text-sm text-muted-foreground">
          {t('remaining', { days: stats.maxBonusDaysRemaining })}
        </p>
      )}

      {stats && stats.maxBonusDaysRemaining === 0 && (
        <p className="text-sm text-amber-600 font-medium">{t('maxReached')}</p>
      )}

      {/* How It Works */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">{t('howItWorks')}</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 sm:grid-cols-3">
            <div className="flex gap-3">
              <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[#1D70F1] text-white text-sm font-bold">
                1
              </div>
              <div>
                <p className="text-sm font-medium">{t('step1Title')}</p>
                <p className="text-xs text-muted-foreground">{t('step1Desc')}</p>
              </div>
            </div>
            <div className="flex gap-3">
              <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[#1D70F1] text-white text-sm font-bold">
                2
              </div>
              <div>
                <p className="text-sm font-medium">{t('step2Title')}</p>
                <p className="text-xs text-muted-foreground">{t('step2Desc')}</p>
              </div>
            </div>
            <div className="flex gap-3">
              <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[#1D70F1] text-white text-sm font-bold">
                3
              </div>
              <div>
                <p className="text-sm font-medium">{t('step3Title')}</p>
                <p className="text-xs text-muted-foreground">{t('step3Desc')}</p>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Referral History */}
      {stats?.referrals && stats.referrals.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">{t('historyTitle')}</CardTitle>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t('table.user')}</TableHead>
                  <TableHead>{t('table.status')}</TableHead>
                  <TableHead>{t('table.reward')}</TableHead>
                  <TableHead>{t('table.date')}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {stats.referrals.map((ref) => (
                  <TableRow key={ref.id}>
                    <TableCell>
                      <div>
                        <p className="font-medium text-sm">{ref.referredUserName}</p>
                        <p className="text-xs text-muted-foreground">{ref.referredUserEmail}</p>
                      </div>
                    </TableCell>
                    <TableCell>
                      <StatusBadge status={ref.status} />
                    </TableCell>
                    <TableCell>
                      {ref.rewardDaysGranted > 0
                        ? `+${ref.rewardDaysGranted} ${t('days')}`
                        : '-'}
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {new Date(ref.createdAt).toLocaleDateString('tr-TR')}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
