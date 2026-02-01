"use client";

import { useState } from "react";
import {
  useAdminNotificationStats,
  useBroadcastNotification,
} from "@/hooks/queries/use-admin-referrals";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Bell,
  BellRing,
  Mail,
  Send,
  BarChart3,
  Loader2,
} from "lucide-react";

export default function AdminNotificationsPage() {
  const { data: stats, isLoading } = useAdminNotificationStats();
  const broadcastMutation = useBroadcastNotification();

  const [open, setOpen] = useState(false);
  const [type, setType] = useState("INFO");
  const [title, setTitle] = useState("");
  const [message, setMessage] = useState("");
  const [link, setLink] = useState("");

  const handleBroadcast = async () => {
    if (!title.trim() || !message.trim()) return;

    await broadcastMutation.mutateAsync({
      type,
      title: title.trim(),
      message: message.trim(),
      link: link.trim() || undefined,
    });

    setType("INFO");
    setTitle("");
    setMessage("");
    setLink("");
    setOpen(false);
  };

  const totalNotifications = stats?.totalNotifications ?? 0;
  const unreadNotifications = stats?.unreadNotifications ?? 0;
  const byType = stats?.byType || {};

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">Bildirimler</h1>
          <p className="text-sm text-slate-400 mt-1">
            Bildirim istatistikleri ve toplu bildirim gonderme
          </p>
        </div>
        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger asChild>
            <Button className="bg-blue-600 hover:bg-blue-700 text-white">
              <Send className="h-4 w-4 mr-2" />
              Toplu Bildirim Gonder
            </Button>
          </DialogTrigger>
          <DialogContent className="bg-slate-900 border-slate-700/50 text-white sm:max-w-[500px]">
            <DialogHeader>
              <DialogTitle className="text-white">
                Toplu Bildirim Gonder
              </DialogTitle>
              <DialogDescription className="text-slate-400">
                Tum kullanicilara bildirim gonderin.
              </DialogDescription>
            </DialogHeader>
            <div className="space-y-4 py-4">
              <div className="space-y-2">
                <label className="text-sm font-medium text-slate-300">
                  Bildirim Tipi
                </label>
                <Select value={type} onValueChange={setType}>
                  <SelectTrigger className="bg-slate-800 border-slate-700 text-white">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent className="bg-slate-800 border-slate-700">
                    <SelectItem value="INFO">Bilgi</SelectItem>
                    <SelectItem value="WARNING">Uyari</SelectItem>
                    <SelectItem value="SUCCESS">Basari</SelectItem>
                    <SelectItem value="ERROR">Hata</SelectItem>
                    <SelectItem value="UPDATE">Guncelleme</SelectItem>
                    <SelectItem value="PROMOTION">Promosyon</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium text-slate-300">
                  Baslik
                </label>
                <Input
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="Bildirim basligi..."
                  className="bg-slate-800 border-slate-700 text-white placeholder:text-slate-500"
                />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium text-slate-300">
                  Mesaj
                </label>
                <Textarea
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  placeholder="Bildirim mesaji..."
                  rows={4}
                  className="bg-slate-800 border-slate-700 text-white placeholder:text-slate-500 resize-none"
                />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium text-slate-300">
                  Link (opsiyonel)
                </label>
                <Input
                  value={link}
                  onChange={(e) => setLink(e.target.value)}
                  placeholder="https://..."
                  className="bg-slate-800 border-slate-700 text-white placeholder:text-slate-500"
                />
              </div>
            </div>
            <DialogFooter>
              <Button
                variant="outline"
                onClick={() => setOpen(false)}
                className="border-slate-700 text-slate-300 hover:bg-slate-800"
              >
                Iptal
              </Button>
              <Button
                onClick={handleBroadcast}
                disabled={
                  !title.trim() ||
                  !message.trim() ||
                  broadcastMutation.isPending
                }
                className="bg-blue-600 hover:bg-blue-700 text-white"
              >
                {broadcastMutation.isPending ? (
                  <>
                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                    Gonderiliyor...
                  </>
                ) : (
                  <>
                    <Send className="h-4 w-4 mr-2" />
                    Gonder
                  </>
                )}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>

      {/* Stat Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card className="bg-slate-900 border-slate-700/50">
          <CardContent className="pt-6">
            {isLoading ? (
              <Skeleton className="h-16 w-full bg-slate-800" />
            ) : (
              <div className="flex items-center gap-4">
                <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-blue-900/30 text-blue-400">
                  <Bell className="h-6 w-6" />
                </div>
                <div>
                  <p className="text-sm text-slate-400">Toplam Bildirim</p>
                  <p className="text-2xl font-bold text-white">
                    {totalNotifications}
                  </p>
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        <Card className="bg-slate-900 border-slate-700/50">
          <CardContent className="pt-6">
            {isLoading ? (
              <Skeleton className="h-16 w-full bg-slate-800" />
            ) : (
              <div className="flex items-center gap-4">
                <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-orange-900/30 text-orange-400">
                  <BellRing className="h-6 w-6" />
                </div>
                <div>
                  <p className="text-sm text-slate-400">Okunmamis</p>
                  <p className="text-2xl font-bold text-white">
                    {unreadNotifications}
                  </p>
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        <Card className="bg-slate-900 border-slate-700/50">
          <CardContent className="pt-6">
            {isLoading ? (
              <Skeleton className="h-16 w-full bg-slate-800" />
            ) : (
              <div className="flex items-center gap-4">
                <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-purple-900/30 text-purple-400">
                  <BarChart3 className="h-6 w-6" />
                </div>
                <div>
                  <p className="text-sm text-slate-400">Tipe Gore Dagilim</p>
                  <div className="flex flex-wrap gap-2 mt-1">
                    {Object.keys(byType).length > 0 ? (
                      Object.entries(byType).map(([key, value]) => (
                        <span
                          key={key}
                          className="text-xs bg-slate-800 text-slate-300 px-2 py-0.5 rounded"
                        >
                          {key}: {value as number}
                        </span>
                      ))
                    ) : (
                      <span className="text-sm text-slate-500">Veri yok</span>
                    )}
                  </div>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Broadcast Success Message */}
      {broadcastMutation.isSuccess && (
        <Card className="bg-green-950/50 border-green-700/50">
          <CardContent className="pt-6">
            <div className="flex items-center gap-3">
              <Mail className="h-5 w-5 text-green-400" />
              <p className="text-green-400">
                Toplu bildirim basariyla gonderildi!
              </p>
            </div>
          </CardContent>
        </Card>
      )}

      {broadcastMutation.isError && (
        <Card className="bg-red-950/50 border-red-700/50">
          <CardContent className="pt-6">
            <div className="flex items-center gap-3">
              <Mail className="h-5 w-5 text-red-400" />
              <p className="text-red-400">
                Bildirim gonderilirken bir hata olustu. Lutfen tekrar deneyin.
              </p>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
