// Auth işlemleri için - Next.js API routes kullan
export const authApi = {
  login: async (credentials: { email: string; password: string }) => {
    const res = await fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(credentials),
      credentials: "include",
    });

    if (!res.ok) {
      const error = await res.json();
      throw new Error(error.message || "Giriş başarısız");
    }

    return res.json();
  },

  register: async (data: { name: string; email: string; password: string; referralCode?: string }) => {
    const res = await fetch("/api/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    });

    if (!res.ok) {
      const error = await res.json();
      throw new Error(error.message || "Kayıt başarısız");
    }

    return res.json();
  },

  me: async () => {
    const headers: Record<string, string> = {};
    if (typeof window !== "undefined") {
      const impToken = sessionStorage.getItem("impersonation_token");
      if (impToken) {
        headers["X-Impersonation-Token"] = impToken;
      }
    }
    const res = await fetch("/api/auth/me", {
      credentials: "include",
      cache: "no-cache", // Prevent browser caching for auth checks
      headers,
    });

    if (!res.ok) {
      throw new Error("Kullanıcı bilgileri alınamadı");
    }

    return res.json();
  },

  refresh: async () => {
    const res = await fetch("/api/auth/refresh", {
      method: "POST",
      credentials: "include",
    });

    if (!res.ok) {
      throw new Error("Oturum yenilenirken hata oluştu");
    }

    return res.json();
  },

  logout: async () => {
    const res = await fetch("/api/auth/logout", {
      method: "POST",
      credentials: "include",
    });

    if (!res.ok) {
      throw new Error("Çıkış yapılırken hata oluştu");
    }

    return res.json();
  },

  forgotPassword: async (email: string) => {
    const res = await fetch("/api/auth/forgot-password", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email }),
    });

    if (!res.ok) {
      const error = await res.json();
      throw new Error(error.message || "Şifre sıfırlama isteği başarısız");
    }

    return res.json();
  },

  verifyResetToken: async (token: string) => {
    const res = await fetch(`/api/auth/verify-reset-token?token=${encodeURIComponent(token)}`);

    if (!res.ok) {
      const error = await res.json();
      throw new Error(error.message || "Token doğrulama başarısız");
    }

    return res.json();
  },

  resetPassword: async (data: { token: string; newPassword: string }) => {
    const res = await fetch("/api/auth/reset-password", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    });

    if (!res.ok) {
      const error = await res.json();
      throw new Error(error.message || "Şifre sıfırlama başarısız");
    }

    return res.json();
  },

  verifyEmail: async (token: string) => {
    const res = await fetch(`/api/auth/verify-email?token=${encodeURIComponent(token)}`);

    if (!res.ok) {
      const error = await res.json();
      throw new Error(error.message || "Email doğrulama başarısız");
    }

    return res.json();
  },

  resendVerification: async () => {
    const res = await fetch("/api/auth/resend-verification", {
      method: "POST",
      credentials: "include",
    });

    if (!res.ok) {
      const error = await res.json();
      throw new Error(error.message || "Email gönderilemedi");
    }

    return res.json();
  },

  verificationStatus: async () => {
    const res = await fetch("/api/auth/verification-status", {
      credentials: "include",
    });

    if (!res.ok) {
      const error = await res.json();
      throw new Error(error.message || "Durum kontrol edilemedi");
    }

    return res.json();
  },
};
