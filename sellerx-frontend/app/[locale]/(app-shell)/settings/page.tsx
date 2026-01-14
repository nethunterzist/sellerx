"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/hooks/useAuth";
import { useMyStores, useDeleteStore } from "@/hooks/queries/use-stores";
import {
  useUpdateProfile,
  useChangePassword,
  useUserPreferences,
  useUpdatePreferences,
} from "@/hooks/queries/use-settings";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
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
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  User,
  Lock,
  Bell,
  Palette,
  Store,
  LogOut,
  Trash2,
  Loader2,
  CheckCircle2,
  Eye,
  EyeOff,
  Save,
  AlertTriangle,
} from "lucide-react";
import { cn } from "@/lib/utils";

export default function SettingsPage() {
  const router = useRouter();
  const { user, logout } = useAuth();
  const { data: stores, isLoading: storesLoading } = useMyStores();
  const { data: preferences } = useUserPreferences();
  const updateProfileMutation = useUpdateProfile();
  const changePasswordMutation = useChangePassword();
  const updatePreferencesMutation = useUpdatePreferences();
  const deleteStoreMutation = useDeleteStore();

  // Profile form state
  const [name, setName] = useState(user?.name || "");
  const [phone, setPhone] = useState("");
  const [company, setCompany] = useState("");
  const [profileSaved, setProfileSaved] = useState(false);

  // Password form state
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPasswords, setShowPasswords] = useState(false);
  const [passwordError, setPasswordError] = useState("");
  const [passwordSuccess, setPasswordSuccess] = useState(false);

  // Notification preferences state
  const [emailNotifications, setEmailNotifications] = useState(
    preferences?.notifications?.email ?? true
  );
  const [pushNotifications, setPushNotifications] = useState(
    preferences?.notifications?.push ?? true
  );
  const [orderUpdates, setOrderUpdates] = useState(
    preferences?.notifications?.orderUpdates ?? true
  );
  const [stockAlerts, setStockAlerts] = useState(
    preferences?.notifications?.stockAlerts ?? true
  );
  const [weeklyReport, setWeeklyReport] = useState(
    preferences?.notifications?.weeklyReport ?? false
  );

  // Appearance preferences state
  const [theme, setTheme] = useState<"light" | "dark" | "system">(
    preferences?.theme ?? "light"
  );
  const [currency, setCurrency] = useState<"TRY" | "USD" | "EUR">(
    preferences?.currency ?? "TRY"
  );

  const handleProfileSave = async () => {
    try {
      await updateProfileMutation.mutateAsync({ name, phone, company });
      setProfileSaved(true);
      setTimeout(() => setProfileSaved(false), 3000);
    } catch (error) {
      console.error("Profil kaydedilemedi:", error);
    }
  };

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

  const handleNotificationsSave = async () => {
    try {
      await updatePreferencesMutation.mutateAsync({
        notifications: {
          email: emailNotifications,
          push: pushNotifications,
          orderUpdates,
          stockAlerts,
          weeklyReport,
        },
      });
    } catch (error) {
      console.error("Bildirim tercihleri kaydedilemedi:", error);
    }
  };

  const handleAppearanceSave = async () => {
    try {
      await updatePreferencesMutation.mutateAsync({
        theme,
        currency,
      });
    } catch (error) {
      console.error("Görünüm tercihleri kaydedilemedi:", error);
    }
  };

  const handleDeleteStore = (storeId: string) => {
    deleteStoreMutation.mutate(storeId);
  };

  const handleLogout = async () => {
    await logout();
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Ayarlar</h1>
        <p className="text-sm text-gray-500 mt-1">
          Hesap ayarlarınızı ve tercihlerinizi yönetin
        </p>
      </div>

      {/* Settings Tabs */}
      <Tabs defaultValue="profile" className="space-y-6">
        <TabsList className="grid w-full grid-cols-5 lg:w-auto lg:inline-grid">
          <TabsTrigger value="profile" className="gap-2">
            <User className="h-4 w-4" />
            <span className="hidden sm:inline">Profil</span>
          </TabsTrigger>
          <TabsTrigger value="security" className="gap-2">
            <Lock className="h-4 w-4" />
            <span className="hidden sm:inline">Güvenlik</span>
          </TabsTrigger>
          <TabsTrigger value="notifications" className="gap-2">
            <Bell className="h-4 w-4" />
            <span className="hidden sm:inline">Bildirimler</span>
          </TabsTrigger>
          <TabsTrigger value="appearance" className="gap-2">
            <Palette className="h-4 w-4" />
            <span className="hidden sm:inline">Görünüm</span>
          </TabsTrigger>
          <TabsTrigger value="stores" className="gap-2">
            <Store className="h-4 w-4" />
            <span className="hidden sm:inline">Mağazalar</span>
          </TabsTrigger>
        </TabsList>

        {/* Profile Tab */}
        <TabsContent value="profile">
          <Card>
            <CardHeader>
              <CardTitle>Profil Bilgileri</CardTitle>
              <CardDescription>
                Hesabınızla ilgili temel bilgilerinizi güncelleyin
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="grid gap-4 sm:grid-cols-2">
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
                    className="bg-gray-50"
                  />
                  <p className="text-xs text-gray-500">
                    E-posta adresi değiştirilemez
                  </p>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="phone">Telefon</Label>
                  <Input
                    id="phone"
                    type="tel"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    placeholder="+90 5XX XXX XX XX"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="company">Şirket Adı</Label>
                  <Input
                    id="company"
                    value={company}
                    onChange={(e) => setCompany(e.target.value)}
                    placeholder="Şirket adınız (opsiyonel)"
                  />
                </div>
              </div>

              <div className="flex items-center gap-3 pt-4">
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
            </CardContent>
          </Card>
        </TabsContent>

        {/* Security Tab */}
        <TabsContent value="security">
          <Card>
            <CardHeader>
              <CardTitle>Şifre Değiştir</CardTitle>
              <CardDescription>
                Hesabınızın güvenliği için şifrenizi düzenli olarak değiştirin
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              {passwordError && (
                <div className="p-3 rounded-lg bg-red-50 border border-red-200 text-red-600 text-sm flex items-center gap-2">
                  <AlertTriangle className="h-4 w-4" />
                  {passwordError}
                </div>
              )}

              {passwordSuccess && (
                <div className="p-3 rounded-lg bg-green-50 border border-green-200 text-green-600 text-sm flex items-center gap-2">
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
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
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

              <div className="pt-4">
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
            </CardContent>
          </Card>
        </TabsContent>

        {/* Notifications Tab */}
        <TabsContent value="notifications">
          <Card>
            <CardHeader>
              <CardTitle>Bildirim Tercihleri</CardTitle>
              <CardDescription>
                Hangi bildirimlerden haberdar olmak istediğinizi seçin
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="space-y-4">
                <div className="flex items-center justify-between py-3 border-b">
                  <div>
                    <p className="font-medium">E-posta Bildirimleri</p>
                    <p className="text-sm text-gray-500">
                      Önemli güncellemeler ve duyurular için e-posta alın
                    </p>
                  </div>
                  <Switch
                    checked={emailNotifications}
                    onCheckedChange={setEmailNotifications}
                  />
                </div>

                <div className="flex items-center justify-between py-3 border-b">
                  <div>
                    <p className="font-medium">Push Bildirimleri</p>
                    <p className="text-sm text-gray-500">
                      Tarayıcı üzerinden anlık bildirimler alın
                    </p>
                  </div>
                  <Switch
                    checked={pushNotifications}
                    onCheckedChange={setPushNotifications}
                  />
                </div>

                <div className="flex items-center justify-between py-3 border-b">
                  <div>
                    <p className="font-medium">Sipariş Güncellemeleri</p>
                    <p className="text-sm text-gray-500">
                      Yeni sipariş ve sipariş durum değişiklikleri
                    </p>
                  </div>
                  <Switch
                    checked={orderUpdates}
                    onCheckedChange={setOrderUpdates}
                  />
                </div>

                <div className="flex items-center justify-between py-3 border-b">
                  <div>
                    <p className="font-medium">Stok Uyarıları</p>
                    <p className="text-sm text-gray-500">
                      Düşük stok ve stok tükenmesi uyarıları
                    </p>
                  </div>
                  <Switch
                    checked={stockAlerts}
                    onCheckedChange={setStockAlerts}
                  />
                </div>

                <div className="flex items-center justify-between py-3">
                  <div>
                    <p className="font-medium">Haftalık Rapor</p>
                    <p className="text-sm text-gray-500">
                      Her hafta satış ve performans özeti
                    </p>
                  </div>
                  <Switch
                    checked={weeklyReport}
                    onCheckedChange={setWeeklyReport}
                  />
                </div>
              </div>

              <div className="pt-4">
                <Button
                  onClick={handleNotificationsSave}
                  disabled={updatePreferencesMutation.isPending}
                  className="bg-[#1D70F1] hover:bg-[#1560d1]"
                >
                  {updatePreferencesMutation.isPending ? (
                    <>
                      <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                      Kaydediliyor...
                    </>
                  ) : (
                    <>
                      <Save className="h-4 w-4 mr-2" />
                      Tercihleri Kaydet
                    </>
                  )}
                </Button>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        {/* Appearance Tab */}
        <TabsContent value="appearance">
          <Card>
            <CardHeader>
              <CardTitle>Görünüm Ayarları</CardTitle>
              <CardDescription>
                Uygulama görünümünü ve bölgesel tercihlerinizi özelleştirin
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="grid gap-6 sm:grid-cols-2">
                <div className="space-y-2">
                  <Label>Tema</Label>
                  <Select
                    value={theme}
                    onValueChange={(v) => setTheme(v as typeof theme)}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="light">Açık Tema</SelectItem>
                      <SelectItem value="dark">Koyu Tema</SelectItem>
                      <SelectItem value="system">Sistem Varsayılanı</SelectItem>
                    </SelectContent>
                  </Select>
                  <p className="text-xs text-gray-500">
                    Koyu tema yakında kullanılabilir olacak
                  </p>
                </div>

                <div className="space-y-2">
                  <Label>Para Birimi</Label>
                  <Select
                    value={currency}
                    onValueChange={(v) => setCurrency(v as typeof currency)}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="TRY">TRY - Türk Lirası</SelectItem>
                      <SelectItem value="USD">USD - Amerikan Doları</SelectItem>
                      <SelectItem value="EUR">EUR - Euro</SelectItem>
                    </SelectContent>
                  </Select>
                  <p className="text-xs text-gray-500">
                    Dashboard ve raporlarda gösterilecek para birimi
                  </p>
                </div>
              </div>

              <div className="pt-4">
                <Button
                  onClick={handleAppearanceSave}
                  disabled={updatePreferencesMutation.isPending}
                  className="bg-[#1D70F1] hover:bg-[#1560d1]"
                >
                  {updatePreferencesMutation.isPending ? (
                    <>
                      <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                      Kaydediliyor...
                    </>
                  ) : (
                    <>
                      <Save className="h-4 w-4 mr-2" />
                      Ayarları Kaydet
                    </>
                  )}
                </Button>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        {/* Stores Tab */}
        <TabsContent value="stores">
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle>Bağlı Mağazalar</CardTitle>
                  <CardDescription>
                    Hesabınıza bağlı pazaryeri mağazalarını yönetin
                  </CardDescription>
                </div>
                <Button
                  onClick={() => router.push("/new-store")}
                  className="bg-[#F27A1A] hover:bg-[#E06A0A]"
                >
                  Mağaza Ekle
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              {storesLoading ? (
                <div className="flex items-center justify-center py-8">
                  <Loader2 className="h-6 w-6 animate-spin text-gray-400" />
                </div>
              ) : stores && stores.length > 0 ? (
                <div className="space-y-3">
                  {stores.map((store: any) => (
                    <div
                      key={store.id}
                      className="flex items-center justify-between p-4 rounded-lg border border-gray-200 hover:border-gray-300 transition-colors"
                    >
                      <div className="flex items-center gap-3">
                        <div
                          className={cn(
                            "h-10 w-10 rounded-lg flex items-center justify-center text-sm font-bold text-white",
                            store.marketplace === "trendyol"
                              ? "bg-[#F27A1A]"
                              : "bg-[#FF6000]"
                          )}
                        >
                          {store.marketplace === "trendyol" ? "T" : "H"}
                        </div>
                        <div>
                          <p className="font-medium text-gray-900">
                            {store.storeName || store.store_name}
                          </p>
                          <p className="text-sm text-gray-500 capitalize">
                            {store.marketplace} • Seller ID: {store.sellerId}
                          </p>
                        </div>
                      </div>

                      <AlertDialog>
                        <AlertDialogTrigger asChild>
                          <Button
                            variant="ghost"
                            size="sm"
                            className="text-red-600 hover:text-red-700 hover:bg-red-50"
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </AlertDialogTrigger>
                        <AlertDialogContent>
                          <AlertDialogHeader>
                            <AlertDialogTitle>
                              Mağazayı Sil
                            </AlertDialogTitle>
                            <AlertDialogDescription>
                              <strong>{store.storeName}</strong> mağazasını silmek
                              istediğinizden emin misiniz? Bu işlem geri alınamaz ve
                              mağazaya ait tüm veriler silinecektir.
                            </AlertDialogDescription>
                          </AlertDialogHeader>
                          <AlertDialogFooter>
                            <AlertDialogCancel>İptal</AlertDialogCancel>
                            <AlertDialogAction
                              onClick={() => handleDeleteStore(store.id)}
                              className="bg-red-600 hover:bg-red-700"
                            >
                              Mağazayı Sil
                            </AlertDialogAction>
                          </AlertDialogFooter>
                        </AlertDialogContent>
                      </AlertDialog>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="text-center py-8">
                  <Store className="h-12 w-12 mx-auto text-gray-300 mb-3" />
                  <p className="text-gray-500 mb-1">Henüz mağaza yok</p>
                  <p className="text-sm text-gray-400">
                    İlk mağazanızı eklemek için yukarıdaki butonu kullanın
                  </p>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Account Actions */}
          <Card className="mt-6 border-red-200">
            <CardHeader>
              <CardTitle className="text-red-600">Tehlikeli Bölge</CardTitle>
              <CardDescription>
                Bu işlemler geri alınamaz, dikkatli olun
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center justify-between p-4 rounded-lg bg-gray-50">
                <div>
                  <p className="font-medium">Oturumu Kapat</p>
                  <p className="text-sm text-gray-500">
                    Tüm cihazlardan çıkış yapın
                  </p>
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
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}
