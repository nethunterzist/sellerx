"use client";

import { useAuth } from "@/hooks/useAuth";
import { Skeleton } from "@/components/ui/skeleton";
import { Card, CardContent } from "@/components/ui/card";

function ProfilePageSkeleton() {
  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <Skeleton className="h-8 w-48" />
      <Card>
        <CardContent className="p-6 space-y-4">
          <div className="space-y-2">
            <Skeleton className="h-4 w-20" />
            <Skeleton className="h-5 w-48" />
          </div>
          <div className="space-y-2">
            <Skeleton className="h-4 w-20" />
            <Skeleton className="h-5 w-64" />
          </div>
          <div className="space-y-2">
            <Skeleton className="h-4 w-20" />
            <Skeleton className="h-5 w-32" />
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

export default function ProfilePage() {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="p-8">
        <ProfilePageSkeleton />
      </div>
    );
  }

  return (
    <div className="p-8">
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
