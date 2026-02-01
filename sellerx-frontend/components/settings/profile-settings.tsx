"use client";

const isDev = process.env.NODE_ENV === "development";

import { useState, useEffect } from "react";
import { useAuth } from "@/hooks/useAuth";
import { useUpdateProfile } from "@/hooks/queries/use-settings";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { SettingsSection } from "./settings-section";
import { Loader2, CheckCircle2, Save } from "lucide-react";

export function ProfileSettings() {
  const { user } = useAuth();
  const updateProfileMutation = useUpdateProfile();

  const [name, setName] = useState(user?.name || "");
  const [profileSaved, setProfileSaved] = useState(false);

  useEffect(() => {
    if (user?.name) setName(user.name);
  }, [user]);

  const handleProfileSave = async () => {
    try {
      await updateProfileMutation.mutateAsync({ name });
      setProfileSaved(true);
      setTimeout(() => setProfileSaved(false), 3000);
    } catch (error) {
      if (isDev) console.error("Profil kaydedilemedi:", error);
    }
  };

  return (
    <SettingsSection
      title="Profil Bilgileri"
      description="Hesabınızla ilgili temel bilgilerinizi güncelleyin"
    >
      <div className="grid gap-6 sm:grid-cols-2">
        <div className="space-y-2">
          <Label htmlFor="name">Ad Soyad</Label>
          <Input
            id="name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Adınız Soyadınız"
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="email">E-posta</Label>
          <Input
            id="email"
            type="email"
            value={user?.email || ""}
            disabled
            className="bg-muted"
          />
          <p className="text-xs text-muted-foreground">E-posta adresi değiştirilemez</p>
        </div>
      </div>

      <div className="flex items-center gap-3 pt-6 mt-6 border-t border-border">
        <Button
          onClick={handleProfileSave}
          disabled={updateProfileMutation.isPending}
          className="bg-[#1D70F1] hover:bg-[#1560d1]"
        >
          {updateProfileMutation.isPending ? (
            <>
              <Loader2 className="h-4 w-4 mr-2 animate-spin" />
              Kaydediliyor...
            </>
          ) : profileSaved ? (
            <>
              <CheckCircle2 className="h-4 w-4 mr-2" />
              Kaydedildi
            </>
          ) : (
            <>
              <Save className="h-4 w-4 mr-2" />
              Değişiklikleri Kaydet
            </>
          )}
        </Button>
      </div>
    </SettingsSection>
  );
}
