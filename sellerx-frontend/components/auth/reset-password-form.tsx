"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useVerifyResetToken, useResetPassword } from "@/hooks/queries/use-auth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Loader2, ArrowLeft, Eye, EyeOff, CheckCircle, AlertCircle } from "lucide-react";
import { FadeIn } from "@/components/motion";

export function ResetPasswordForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const token = searchParams.get("token");

  const { data: tokenData, isLoading: isValidating, error: tokenError } = useVerifyResetToken(token);
  const { mutate: resetPassword, isPending } = useResetPassword();

  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    if (!password) {
      setError("Lütfen yeni şifrenizi girin");
      return;
    }

    if (password.length < 6) {
      setError("Şifre en az 6 karakter olmalıdır");
      return;
    }

    if (password !== confirmPassword) {
      setError("Şifreler eşleşmiyor");
      return;
    }

    resetPassword(
      { token: token!, newPassword: password },
      {
        onSuccess: () => {
          setSuccess(true);
        },
        onError: (err: any) => {
          setError(err.message || "Şifre sıfırlama başarısız");
        },
      }
    );
  };

  // Success state
  if (success) {
    return (
      <FadeIn direction="up" duration={0.4}>
        <div className="w-full max-w-md mx-auto">
          <div className="text-center mb-8">
            <h1 className="text-3xl font-bold text-[#1D70F1]">sellerx</h1>
          </div>

          <div className="text-center space-y-4">
            <div className="mx-auto w-16 h-16 bg-green-100 rounded-full flex items-center justify-center">
              <CheckCircle className="h-8 w-8 text-green-600" />
            </div>
            <h2 className="text-xl font-semibold text-gray-900">
              Şifreniz Güncellendi
            </h2>
            <p className="text-gray-600">
              Şifreniz başarıyla değiştirildi. Artık yeni şifrenizle giriş yapabilirsiniz.
            </p>
          </div>

          <div className="mt-8">
            <Button
              onClick={() => router.push("/sign-in")}
              className="w-full h-11 bg-[#1D70F1] hover:bg-[#1560d1] text-white font-medium"
            >
              Giriş Yap
            </Button>
          </div>
        </div>
      </FadeIn>
    );
  }

  // Loading state
  if (isValidating) {
    return (
      <div className="w-full max-w-md mx-auto">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-[#1D70F1]">sellerx</h1>
        </div>
        <div className="text-center space-y-4">
          <Loader2 className="mx-auto h-8 w-8 animate-spin text-[#1D70F1]" />
          <p className="text-gray-600">Token doğrulanıyor...</p>
        </div>
      </div>
    );
  }

  // Invalid token state
  if (!token || tokenError || (tokenData && !tokenData.valid)) {
    return (
      <FadeIn direction="up" duration={0.4}>
        <div className="w-full max-w-md mx-auto">
          <div className="text-center mb-8">
            <h1 className="text-3xl font-bold text-[#1D70F1]">sellerx</h1>
          </div>

          <div className="text-center space-y-4">
            <div className="mx-auto w-16 h-16 bg-red-100 rounded-full flex items-center justify-center">
              <AlertCircle className="h-8 w-8 text-red-600" />
            </div>
            <h2 className="text-xl font-semibold text-gray-900">
              Geçersiz veya Süresi Dolmuş Bağlantı
            </h2>
            <p className="text-gray-600">
              Bu şifre sıfırlama bağlantısı geçersiz veya süresi dolmuş.
              Lütfen yeni bir şifre sıfırlama isteği oluşturun.
            </p>
          </div>

          <div className="mt-8 space-y-4">
            <Button
              onClick={() => router.push("/forgot-password")}
              className="w-full h-11 bg-[#1D70F1] hover:bg-[#1560d1] text-white font-medium"
            >
              Yeni Şifre Sıfırlama İsteği
            </Button>
            <div className="text-center">
              <Link
                href="/sign-in"
                className="inline-flex items-center text-[#1D70F1] hover:underline"
              >
                <ArrowLeft className="mr-2 h-4 w-4" />
                Giriş sayfasına dön
              </Link>
            </div>
          </div>
        </div>
      </FadeIn>
    );
  }

  // Reset form
  return (
    <FadeIn direction="up" duration={0.4}>
    <div className="w-full max-w-md mx-auto">
      <div className="text-center mb-8">
        <h1 className="text-3xl font-bold text-[#1D70F1]">sellerx</h1>
        <p className="text-gray-500 mt-2">Yeni Şifre Belirleyin</p>
      </div>

      {tokenData?.email && (
        <p className="text-center text-gray-600 mb-6">
          <strong>{tokenData.email}</strong> hesabı için yeni şifre belirleyin.
        </p>
      )}

      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 rounded bg-red-50 border border-red-200 text-red-600 text-sm">
            {error}
          </div>
        )}

        <div className="space-y-2">
          <Label htmlFor="password">Yeni Şifre</Label>
          <div className="relative">
            <Input
              id="password"
              type={showPassword ? "text" : "password"}
              placeholder="En az 6 karakter"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={isPending}
              className="h-11 pr-10"
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
            >
              {showPassword ? (
                <EyeOff className="h-5 w-5" />
              ) : (
                <Eye className="h-5 w-5" />
              )}
            </button>
          </div>
        </div>

        <div className="space-y-2">
          <Label htmlFor="confirmPassword">Şifreyi Tekrar Girin</Label>
          <div className="relative">
            <Input
              id="confirmPassword"
              type={showConfirmPassword ? "text" : "password"}
              placeholder="Şifrenizi tekrar girin"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              disabled={isPending}
              className="h-11 pr-10"
            />
            <button
              type="button"
              onClick={() => setShowConfirmPassword(!showConfirmPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
            >
              {showConfirmPassword ? (
                <EyeOff className="h-5 w-5" />
              ) : (
                <Eye className="h-5 w-5" />
              )}
            </button>
          </div>
        </div>

        <Button
          type="submit"
          disabled={isPending}
          className="w-full h-11 bg-[#1D70F1] hover:bg-[#1560d1] text-white font-medium"
        >
          {isPending ? (
            <>
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              Şifre Güncelleniyor...
            </>
          ) : (
            "Şifreyi Güncelle"
          )}
        </Button>
      </form>

      <div className="mt-6 text-center">
        <Link
          href="/sign-in"
          className="inline-flex items-center text-[#1D70F1] hover:underline"
        >
          <ArrowLeft className="mr-2 h-4 w-4" />
          Giriş sayfasına dön
        </Link>
      </div>
    </div>
    </FadeIn>
  );
}
