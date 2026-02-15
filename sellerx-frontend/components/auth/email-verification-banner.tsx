"use client";

import { useState } from "react";
import { AlertTriangle, X, RefreshCw, CheckCircle2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useVerificationStatus, useResendVerification } from "@/hooks/queries/use-auth";

interface EmailVerificationBannerProps {
  userEmail?: string;
}

export function EmailVerificationBanner({ userEmail }: EmailVerificationBannerProps) {
  const [dismissed, setDismissed] = useState(false);
  const [resendSuccess, setResendSuccess] = useState(false);
  const [cooldown, setCooldown] = useState(0);

  const { data: status, isLoading } = useVerificationStatus();
  const resendVerification = useResendVerification();

  // Don't show if loading, dismissed, or already verified
  if (isLoading || dismissed || status?.verified) {
    return null;
  }

  const handleResend = () => {
    if (cooldown > 0) return;

    resendVerification.mutate(undefined, {
      onSuccess: (data) => {
        if (data.success) {
          setResendSuccess(true);
          setCooldown(60);
          const timer = setInterval(() => {
            setCooldown((prev) => {
              if (prev <= 1) {
                clearInterval(timer);
                setResendSuccess(false);
                return 0;
              }
              return prev - 1;
            });
          }, 1000);
        }
      },
    });
  };

  return (
    <div className="bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-lg p-4 mb-4">
      <div className="flex items-start gap-3">
        <AlertTriangle className="h-5 w-5 text-yellow-600 dark:text-yellow-500 flex-shrink-0 mt-0.5" />
        <div className="flex-1 min-w-0">
          <h3 className="font-medium text-yellow-800 dark:text-yellow-200">
            E-posta Doğrulaması Gerekli
          </h3>
          <p className="mt-1 text-sm text-yellow-700 dark:text-yellow-300">
            Hesabınızın güvenliği için lütfen e-posta adresinizi doğrulayın.
            {userEmail && (
              <span className="block mt-1 font-medium">{userEmail}</span>
            )}
          </p>
          <div className="mt-3 flex flex-wrap items-center gap-2">
            {resendSuccess ? (
              <div className="flex items-center gap-1 text-green-600 dark:text-green-400 text-sm">
                <CheckCircle2 className="h-4 w-4" />
                <span>E-posta gönderildi!</span>
              </div>
            ) : (
              <Button
                size="sm"
                variant="outline"
                onClick={handleResend}
                disabled={resendVerification.isPending || cooldown > 0}
                className="border-yellow-300 dark:border-yellow-700 hover:bg-yellow-100 dark:hover:bg-yellow-900/40"
              >
                {resendVerification.isPending ? (
                  <>
                    <RefreshCw className="mr-1 h-3 w-3 animate-spin" />
                    Gönderiliyor...
                  </>
                ) : cooldown > 0 ? (
                  `Tekrar gönder (${cooldown}s)`
                ) : (
                  "Doğrulama E-postası Gönder"
                )}
              </Button>
            )}
          </div>
        </div>
        <button
          onClick={() => setDismissed(true)}
          className="text-yellow-600 dark:text-yellow-500 hover:text-yellow-800 dark:hover:text-yellow-300 p-1"
          aria-label="Kapat"
        >
          <X className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}
