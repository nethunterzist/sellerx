"use client";

import { useState } from "react";
import { useAuth } from "@/hooks/useAuth";
import { useChangePassword } from "@/hooks/queries/use-settings";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { SettingsSection, SettingsDangerZone } from "./settings-section";
import {
  Lock,
  LogOut,
  Trash2,
  Loader2,
  CheckCircle2,
  Eye,
  EyeOff,
  AlertTriangle,
  Shield,
  Smartphone,
  Key,
} from "lucide-react";

export function SecuritySettings() {
  const { logout } = useAuth();
  const changePasswordMutation = useChangePassword();

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPasswords, setShowPasswords] = useState(false);
  const [passwordError, setPasswordError] = useState("");
  const [passwordSuccess, setPasswordSuccess] = useState(false);

  const handlePasswordChange = async () => {
    setPasswordError("");
    setPasswordSuccess(false);

    if (!currentPassword || !newPassword || !confirmPassword) {
      setPasswordError("Lütfen tüm alanları doldurun");
      return;
    }

    if (newPassword.length < 6) {
      setPasswordError("Yeni şifre en az 6 karakter olmalıdır");
      return;
    }

    if (newPassword !== confirmPassword) {
      setPasswordError("Şifreler eşleşmiyor");
      return;
    }

    try {
      await changePasswordMutation.mutateAsync({
        currentPassword,
        newPassword,
        confirmPassword,
      });
      setPasswordSuccess(true);
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      setTimeout(() => setPasswordSuccess(false), 3000);
    } catch (error: any) {
      setPasswordError(error.message || "Şifre değiştirilemedi");
    }
  };

  const handleLogout = async () => {
    await logout();
  };

  return (
    <div className="space-y-6">
      {/* Password Change Section */}
      <SettingsSection
        title="Şifre Değiştir"
        description="Hesabınızın güvenliği için şifrenizi düzenli olarak değiştirin"
      >
        {passwordError && (
          <div className="p-3 rounded-lg bg-red-50 border border-red-200 text-red-600 text-sm flex items-center gap-2 mb-4">
            <AlertTriangle className="h-4 w-4" />
            {passwordError}
          </div>
        )}

        {passwordSuccess && (
          <div className="p-3 rounded-lg bg-green-50 border border-green-200 text-green-600 text-sm flex items-center gap-2 mb-4">
            <CheckCircle2 className="h-4 w-4" />
            Şifreniz başarıyla değiştirildi
          </div>
        )}

        <div className="space-y-4 max-w-md">
          <div className="space-y-2">
            <Label htmlFor="currentPassword">Mevcut Şifre</Label>
            <div className="relative">
              <Input
                id="currentPassword"
                type={showPasswords ? "text" : "password"}
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                placeholder="Mevcut şifreniz"
                className="pr-10"
              />
              <button
                type="button"
                onClick={() => setShowPasswords(!showPasswords)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
              >
                {showPasswords ? (
                  <EyeOff className="h-4 w-4" />
                ) : (
                  <Eye className="h-4 w-4" />
                )}
              </button>
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="newPassword">Yeni Şifre</Label>
            <Input
              id="newPassword"
              type={showPasswords ? "text" : "password"}
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              placeholder="En az 6 karakter"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="confirmPassword">Yeni Şifre (Tekrar)</Label>
            <Input
              id="confirmPassword"
              type={showPasswords ? "text" : "password"}
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="Yeni şifrenizi tekrar girin"
            />
          </div>
        </div>

        <div className="pt-6 mt-6 border-t border-border">
          <Button
            onClick={handlePasswordChange}
            disabled={changePasswordMutation.isPending}
            className="bg-[#1D70F1] hover:bg-[#1560d1]"
          >
            {changePasswordMutation.isPending ? (
              <>
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                Değiştiriliyor...
              </>
            ) : (
              <>
                <Lock className="h-4 w-4 mr-2" />
                Şifreyi Değiştir
              </>
            )}
          </Button>
        </div>
      </SettingsSection>

      {/* Two-Factor Authentication - Coming Soon */}
      <SettingsSection
        title="İki Faktörlü Doğrulama (2FA)"
        description="Hesabınıza ekstra bir güvenlik katmanı ekleyin"
      >
        <div className="space-y-4">
          <div className="flex items-center justify-between p-4 rounded-lg bg-muted border border-border">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-lg bg-muted flex items-center justify-center">
                <Smartphone className="h-5 w-5 text-muted-foreground" />
              </div>
              <div>
                <p className="font-medium text-foreground">SMS Doğrulama</p>
                <p className="text-sm text-muted-foreground">
                  Telefon numaranıza gönderilen kod ile giriş yapın
                </p>
              </div>
            </div>
            <Badge className="bg-muted text-muted-foreground border-border">
              Yakında
            </Badge>
          </div>

          <div className="flex items-center justify-between p-4 rounded-lg bg-muted border border-border">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-lg bg-muted flex items-center justify-center">
                <Key className="h-5 w-5 text-muted-foreground" />
              </div>
              <div>
                <p className="font-medium text-foreground">Authenticator App</p>
                <p className="text-sm text-muted-foreground">
                  Google Authenticator veya benzeri uygulamalar ile
                </p>
              </div>
            </div>
            <Badge className="bg-muted text-muted-foreground border-border">
              Yakında
            </Badge>
          </div>
        </div>

        <div className="mt-4 p-4 rounded-lg bg-blue-50 border border-blue-200">
          <div className="flex items-start gap-3">
            <Shield className="h-5 w-5 text-blue-600 mt-0.5" />
            <div>
              <p className="font-medium text-blue-900">
                İki faktörlü doğrulama neden önemli?
              </p>
              <p className="text-sm text-blue-700 mt-1">
                2FA, şifreniz ele geçirilse bile hesabınızın güvende kalmasını sağlar.
                Bu özellik yakında kullanılabilir olacak.
              </p>
            </div>
          </div>
        </div>
      </SettingsSection>

      {/* Danger Zone */}
      <SettingsDangerZone>
        <div className="flex items-center justify-between p-4 rounded-lg bg-muted">
          <div>
            <p className="font-medium text-foreground">Oturumu Kapat</p>
            <p className="text-sm text-muted-foreground">Tüm cihazlardan çıkış yapın</p>
          </div>
          <Button variant="outline" onClick={handleLogout}>
            <LogOut className="h-4 w-4 mr-2" />
            Çıkış Yap
          </Button>
        </div>

        <div className="flex items-center justify-between p-4 rounded-lg bg-red-50">
          <div>
            <p className="font-medium text-red-600">Hesabı Sil</p>
            <p className="text-sm text-red-500">
              Hesabınız ve tüm verileriniz kalıcı olarak silinecek
            </p>
          </div>
          <AlertDialog>
            <AlertDialogTrigger asChild>
              <Button variant="destructive">
                <Trash2 className="h-4 w-4 mr-2" />
                Hesabı Sil
              </Button>
            </AlertDialogTrigger>
            <AlertDialogContent>
              <AlertDialogHeader>
                <AlertDialogTitle>
                  Hesabınızı silmek istediğinizden emin misiniz?
                </AlertDialogTitle>
                <AlertDialogDescription>
                  Bu işlem geri alınamaz. Hesabınız ve tüm verileriniz
                  (mağazalar, ürünler, siparişler, raporlar) kalıcı olarak
                  silinecektir.
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel>İptal</AlertDialogCancel>
                <AlertDialogAction className="bg-red-600 hover:bg-red-700">
                  Evet, Hesabımı Sil
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
        </div>
      </SettingsDangerZone>
    </div>
  );
}
