"use client";

import { useAuth } from "@/hooks/useAuth";

export default function ProfilePage() {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="p-8">
        <p>Yükleniyor...</p>
      </div>
    );
  }

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold mb-8">Profil</h1>

      <div className="bg-muted p-4 rounded-lg">
        <p><strong>Ad:</strong> {user?.name || "-"}</p>
        <p><strong>Email:</strong> {user?.email || "-"}</p>
        <p><strong>ID:</strong> {user?.id || "-"}</p>
      </div>

      <p className="mt-8 text-muted-foreground">
        Profil sayfası tasarımınızı buraya ekleyin.
      </p>
    </div>
  );
}
