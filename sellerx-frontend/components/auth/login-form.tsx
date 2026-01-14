"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useLogin } from "@/hooks/queries/use-auth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import { Eye, EyeOff, Loader2 } from "lucide-react";

export function LoginForm() {
  const router = useRouter();
  const { mutate: login, isPending } = useLogin();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    if (!email || !password) {
      setError("Lütfen tüm alanları doldurun");
      return;
    }

    login(
      { email, password },
      {
        onSuccess: () => {
          router.push("/dashboard");
        },
        onError: (err: any) => {
          setError(err.message || "Geçersiz e-posta veya şifre");
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
          Trendyol & Hepsiburada Analitik
        </p>
      </div>

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

        <div className="space-y-2">
          <Label htmlFor="password">Şifre</Label>
          <div className="relative">
            <Input
              id="password"
              type={showPassword ? "text" : "password"}
              placeholder="Şifrenizi girin"
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

        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <Checkbox
              id="remember"
              checked={rememberMe}
              onCheckedChange={(checked) => setRememberMe(checked as boolean)}
            />
            <label
              htmlFor="remember"
              className="text-sm text-gray-600 cursor-pointer"
            >
              Oturumumu açık tut
            </label>
          </div>
          <Link
            href="/forgot-password"
            className="text-sm text-[#1D70F1] hover:underline"
          >
            Şifremi unuttum
          </Link>
        </div>

        <Button
          type="submit"
          disabled={isPending}
          className="w-full h-11 bg-[#1D70F1] hover:bg-[#1560d1] text-white font-medium"
        >
          {isPending ? (
            <>
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              Giriş yapılıyor...
            </>
          ) : (
            "Giriş Yap"
          )}
        </Button>
      </form>

      {/* Sign up link */}
      <div className="mt-6 text-center">
        <p className="text-sm text-gray-600">
          Hesabınız yok mu?{" "}
          <Link
            href="/register"
            className="text-[#1D70F1] font-medium hover:underline"
          >
            Kayıt ol
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
