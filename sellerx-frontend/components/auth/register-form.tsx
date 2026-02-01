"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useRegister } from "@/hooks/queries/use-auth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import { Eye, EyeOff, Gift, Loader2 } from "lucide-react";

export function RegisterForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const referralCode = searchParams.get("ref");
  const { mutate: register, isPending } = useRegister();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [acceptTerms, setAcceptTerms] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    if (!name || !email || !password || !confirmPassword) {
      setError("Lütfen tüm alanları doldurun");
      return;
    }

    if (password !== confirmPassword) {
      setError("Şifreler eşleşmiyor");
      return;
    }

    if (password.length < 6) {
      setError("Şifre en az 6 karakter olmalıdır");
      return;
    }

    if (!acceptTerms) {
      setError("Lütfen kullanım koşullarını kabul edin");
      return;
    }

    register(
      { name, email, password, ...(referralCode ? { referralCode } : {}) },
      {
        onSuccess: () => {
          router.push("/dashboard");
        },
        onError: (err: any) => {
          setError(err.message || "Kayıt başarısız. Lütfen tekrar deneyin.");
        },
      }
    );
  };

  return (
    <div className="w-full max-w-md mx-auto">
      {/* Logo */}
      <div className="text-center mb-8">
        <h1 className="text-3xl font-bold text-[#1D70F1]">sellerx</h1>
        <p className="text-gray-500 mt-2">
          Hesabınızı oluşturun
        </p>
      </div>

      {/* Referral Banner */}
      {referralCode && (
        <div className="mb-4 flex items-center gap-2 rounded-lg border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-700">
          <Gift className="h-4 w-4 shrink-0" />
          <span>Davet koduyla kayıt oluyorsunuz — <strong>30 gün ücretsiz deneme</strong> kazanacaksınız!</span>
        </div>
      )}

      {/* Form */}
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 rounded bg-red-50 border border-red-200 text-red-600 text-sm">
            {error}
          </div>
        )}

        <div className="space-y-2">
          <Label htmlFor="name">Ad Soyad</Label>
          <Input
            id="name"
            type="text"
            placeholder="Adınız Soyadınız"
            value={name}
            onChange={(e) => setName(e.target.value)}
            disabled={isPending}
            className="h-11"
          />
        </div>

        <div className="space-y-2">
          <Label htmlFor="email">E-posta</Label>
          <Input
            id="email"
            type="email"
            placeholder="ornek@email.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            disabled={isPending}
            className="h-11"
          />
        </div>

        <div className="space-y-2">
          <Label htmlFor="password">Şifre</Label>
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
          <Label htmlFor="confirmPassword">Şifre Tekrar</Label>
          <Input
            id="confirmPassword"
            type={showPassword ? "text" : "password"}
            placeholder="Şifrenizi tekrar girin"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            disabled={isPending}
            className="h-11"
          />
        </div>

        <div className="flex items-start space-x-2">
          <Checkbox
            id="terms"
            checked={acceptTerms}
            onCheckedChange={(checked) => setAcceptTerms(checked as boolean)}
            className="mt-1"
          />
          <label
            htmlFor="terms"
            className="text-sm text-gray-600 cursor-pointer"
          >
            <Link href="/terms" className="text-[#1D70F1] hover:underline">
              Kullanım Koşulları
            </Link>{" "}
            ve{" "}
            <Link href="/privacy" className="text-[#1D70F1] hover:underline">
              Gizlilik Politikası
            </Link>
            'nı kabul ediyorum
          </label>
        </div>

        <Button
          type="submit"
          disabled={isPending}
          className="w-full h-11 bg-[#1D70F1] hover:bg-[#1560d1] text-white font-medium"
        >
          {isPending ? (
            <>
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              Hesap oluşturuluyor...
            </>
          ) : (
            "Hesap Oluştur"
          )}
        </Button>
      </form>

      {/* Sign in link */}
      <div className="mt-6 text-center">
        <p className="text-sm text-gray-600">
          Zaten hesabınız var mı?{" "}
          <Link
            href="/sign-in"
            className="text-[#1D70F1] font-medium hover:underline"
          >
            Giriş yap
          </Link>
        </p>
      </div>

      {/* Footer */}
      <div className="mt-8 text-center">
        <p className="text-xs text-gray-400">
          Yardıma mı ihtiyacınız var?{" "}
          <a
            href="mailto:support@sellerx.com"
            className="text-[#1D70F1] hover:underline"
          >
            support@sellerx.com
          </a>
        </p>
      </div>
    </div>
  );
}
