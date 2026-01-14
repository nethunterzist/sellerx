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

  register: async (data: { name: string; email: string; password: string }) => {
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
    const res = await fetch("/api/auth/me", {
      credentials: "include",
      cache: "no-cache", // Prevent browser caching for auth checks
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
};
