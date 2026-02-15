"use client";

import { useState } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import { Mail, RefreshCw, CheckCircle2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useResendVerification } from "@/hooks/queries/use-auth";
import { FadeIn } from "@/components/motion";

export default function VerificationPendingPage() {
  const searchParams = useSearchParams();
  const email = searchParams.get("email") || "";
  const [resendSuccess, setResendSuccess] = useState(false);
  const [cooldown, setCooldown] = useState(0);

  const resendVerification = useResendVerification();

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
    <div className="flex min-h-screen items-center justify-center bg-muted">
      <FadeIn direction="up" duration={0.4}>
        <div className="w-full max-w-md p-8 bg-card rounded-lg shadow-lg text-center">
        <div className="mx-auto w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center">
          <Mail className="h-8 w-8 text-primary" />
        </div>

        <h1 className="mt-6 text-2xl font-bold">E-postanızı Doğrulayın</h1>

        <p className="mt-4 text-muted-foreground">
          <span className="font-medium text-foreground">{email || "E-posta adresinize"}</span>
          {email ? " adresine" : ""} bir doğrulama e-postası gönderdik.
        </p>

        <p className="mt-2 text-sm text-muted-foreground">
          E-postadaki linke tıklayarak hesabınızı doğrulayın.
        </p>

        <div className="mt-8 p-4 bg-muted rounded-lg">
          <h3 className="font-medium text-sm">E-posta gelmedi mi?</h3>
          <ul className="mt-2 text-sm text-muted-foreground text-left space-y-1">
            <li>• Spam/gereksiz klasörünü kontrol edin</li>
            <li>• E-posta adresinin doğru olduğundan emin olun</li>
            <li>• Birkaç dakika bekleyin</li>
          </ul>
        </div>

        {resendSuccess && (
          <div className="mt-4 p-3 bg-green-50 dark:bg-green-900/20 rounded-lg flex items-center gap-2 justify-center text-green-700 dark:text-green-400">
            <CheckCircle2 className="h-4 w-4" />
            <span className="text-sm">Doğrulama e-postası tekrar gönderildi!</span>
          </div>
        )}

        <div className="mt-6 space-y-3">
          <Link href="/dashboard">
            <Button className="w-full">
              Dashboard'a Git
            </Button>
          </Link>

          <Button
            onClick={handleResend}
            disabled={resendVerification.isPending || cooldown > 0}
            variant="outline"
            className="w-full"
          >
            {resendVerification.isPending ? (
              <>
                <RefreshCw className="mr-2 h-4 w-4 animate-spin" />
                Gönderiliyor...
              </>
            ) : cooldown > 0 ? (
              `Tekrar gönder (${cooldown}s)`
            ) : (
              <>
                <RefreshCw className="mr-2 h-4 w-4" />
                E-postayı Tekrar Gönder
              </>
            )}
          </Button>
        </div>

        <p className="mt-6 text-xs text-muted-foreground">
          E-posta doğrulaması tamamlanmadan bazı özellikler kısıtlı olabilir.
        </p>
        </div>
      </FadeIn>
    </div>
  );
}
