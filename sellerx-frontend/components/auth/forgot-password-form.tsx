"use client";

import { useState } from "react";
import Link from "next/link";
import { useForgotPassword } from "@/hooks/queries/use-auth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Loader2, ArrowLeft, CheckCircle, Mail } from "lucide-react";
import { FadeIn } from "@/components/motion";

export function ForgotPasswordForm() {
  const { mutate: forgotPassword, isPending } = useForgotPassword();
  const [email, setEmail] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    if (!email) {
      setError("Lütfen e-posta adresinizi girin");
      return;
    }

    forgotPassword(email, {
      onSuccess: () => {
        setSuccess(true);
      },
      onError: (err: any) => {
        // Always show success to prevent email enumeration
        setSuccess(true);
      },
    });
  };

  if (success) {
    return (
      <FadeIn direction="up" duration={0.4}>
        <div className="w-full max-w-md mx-auto">
          {/* Logo */}
          <div className="text-center mb-8">
            <h1 className="text-3xl font-bold text-[#1D70F1]">sellerx</h1>
          </div>

          {/* Success Message */}
          <div className="text-center space-y-4">
            <div className="mx-auto w-16 h-16 bg-green-100 rounded-full flex items-center justify-center">
              <Mail className="h-8 w-8 text-green-600" />
            </div>
            <h2 className="text-xl font-semibold text-gray-900">
              E-posta Gönderildi
            </h2>
            <p className="text-gray-600">
              Şifre sıfırlama bağlantısı <strong>{email}</strong> adresine gönderildi.
              Lütfen gelen kutunuzu kontrol edin.
            </p>
            <p className="text-sm text-gray-500">
              E-posta gelmedi mi? Spam klasörünü kontrol edin veya birkaç dakika bekleyin.
            </p>
          </div>

          {/* Back to login */}
          <div className="mt-8 text-center">
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

  return (
    <FadeIn direction="up" duration={0.4}>
    <div className="w-full max-w-md mx-auto">
      {/* Logo */}
      <div className="text-center mb-8">
        <h1 className="text-3xl font-bold text-[#1D70F1]">sellerx</h1>
        <p className="text-gray-500 mt-2">Şifrenizi Sıfırlayın</p>
      </div>

      {/* Description */}
      <p className="text-center text-gray-600 mb-6">
        E-posta adresinizi girin, size şifre sıfırlama bağlantısı gönderelim.
      </p>

      {/* Form */}
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 rounded bg-red-50 border border-red-200 text-red-600 text-sm">
            {error}
          </div>
        )}

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

        <Button
          type="submit"
          disabled={isPending}
          className="w-full h-11 bg-[#1D70F1] hover:bg-[#1560d1] text-white font-medium"
        >
          {isPending ? (
            <>
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              Gönderiliyor...
            </>
          ) : (
            "Şifre Sıfırlama Bağlantısı Gönder"
          )}
        </Button>
      </form>

      {/* Back to login */}
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
