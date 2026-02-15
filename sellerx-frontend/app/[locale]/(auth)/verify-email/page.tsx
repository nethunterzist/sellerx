"use client";

import { useEffect, useState } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import Link from "next/link";
import { Loader2, CheckCircle2, XCircle, AlertCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useVerifyEmail } from "@/hooks/queries/use-auth";
import { FadeIn } from "@/components/motion";

type VerificationState = "loading" | "success" | "error" | "already_verified" | "no_token";

export default function VerifyEmailPage() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const token = searchParams.get("token");
  const [state, setState] = useState<VerificationState>("loading");
  const [errorMessage, setErrorMessage] = useState<string>("");

  const verifyEmail = useVerifyEmail();

  useEffect(() => {
    if (!token) {
      setState("no_token");
      return;
    }

    verifyEmail.mutate(token, {
      onSuccess: (data) => {
        if (data.success) {
          if (data.alreadyVerified) {
            setState("already_verified");
          } else {
            setState("success");
          }
        } else {
          setState("error");
          setErrorMessage(data.message || "Doğrulama başarısız oldu");
        }
      },
      onError: (error: Error) => {
        setState("error");
        setErrorMessage(error.message || "Bir hata oluştu");
      },
    });
  }, [token]);

  if (state === "loading") {
    return (
      <div className="flex min-h-screen items-center justify-center bg-muted">
        <div className="w-full max-w-md p-8 bg-card rounded-lg shadow-lg text-center">
          <Loader2 className="mx-auto h-12 w-12 animate-spin text-primary" />
          <h1 className="mt-4 text-xl font-semibold">E-posta Doğrulanıyor</h1>
          <p className="mt-2 text-muted-foreground">
            Lütfen bekleyin...
          </p>
        </div>
      </div>
    );
  }

  if (state === "no_token") {
    return (
      <div className="flex min-h-screen items-center justify-center bg-muted">
        <FadeIn direction="up" duration={0.4}>
          <div className="w-full max-w-md p-8 bg-card rounded-lg shadow-lg text-center">
            <AlertCircle className="mx-auto h-12 w-12 text-yellow-500" />
            <h1 className="mt-4 text-xl font-semibold">Geçersiz Link</h1>
            <p className="mt-2 text-muted-foreground">
              E-posta doğrulama linki geçersiz veya eksik.
            </p>
            <div className="mt-6 space-y-3">
              <Link href="/sign-in">
                <Button className="w-full">Giriş Yap</Button>
              </Link>
            </div>
          </div>
        </FadeIn>
      </div>
    );
  }

  if (state === "success") {
    return (
      <div className="flex min-h-screen items-center justify-center bg-muted">
        <FadeIn direction="up" duration={0.4}>
          <div className="w-full max-w-md p-8 bg-card rounded-lg shadow-lg text-center">
            <CheckCircle2 className="mx-auto h-12 w-12 text-green-500" />
            <h1 className="mt-4 text-xl font-semibold">E-posta Doğrulandı!</h1>
            <p className="mt-2 text-muted-foreground">
              E-posta adresiniz başarıyla doğrulandı. Artık tüm özelliklere erişebilirsiniz.
            </p>
            <div className="mt-6">
              <Link href="/dashboard">
                <Button className="w-full">Dashboard'a Git</Button>
              </Link>
            </div>
          </div>
        </FadeIn>
      </div>
    );
  }

  if (state === "already_verified") {
    return (
      <div className="flex min-h-screen items-center justify-center bg-muted">
        <FadeIn direction="up" duration={0.4}>
          <div className="w-full max-w-md p-8 bg-card rounded-lg shadow-lg text-center">
            <CheckCircle2 className="mx-auto h-12 w-12 text-blue-500" />
            <h1 className="mt-4 text-xl font-semibold">Zaten Doğrulanmış</h1>
            <p className="mt-2 text-muted-foreground">
              E-posta adresiniz daha önce doğrulanmış.
            </p>
            <div className="mt-6">
              <Link href="/dashboard">
                <Button className="w-full">Dashboard'a Git</Button>
              </Link>
            </div>
          </div>
        </FadeIn>
      </div>
    );
  }

  // Error state
  return (
    <div className="flex min-h-screen items-center justify-center bg-muted">
      <FadeIn direction="up" duration={0.4}>
        <div className="w-full max-w-md p-8 bg-card rounded-lg shadow-lg text-center">
          <XCircle className="mx-auto h-12 w-12 text-red-500" />
          <h1 className="mt-4 text-xl font-semibold">Doğrulama Başarısız</h1>
          <p className="mt-2 text-muted-foreground">
            {errorMessage || "E-posta doğrulama linki geçersiz veya süresi dolmuş."}
          </p>
          <div className="mt-6 space-y-3">
            <Link href="/sign-in">
              <Button className="w-full">Giriş Yap</Button>
            </Link>
            <p className="text-sm text-muted-foreground">
              Giriş yaptıktan sonra yeni doğrulama e-postası talep edebilirsiniz.
            </p>
          </div>
        </div>
      </FadeIn>
    </div>
  );
}
