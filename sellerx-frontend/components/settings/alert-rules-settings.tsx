"use client";

import { useState } from "react";
import {
  useAlertRules,
  useAlertRuleCounts,
  useToggleAlertRule,
  useDeleteAlertRule,
} from "@/hooks/queries/use-alerts";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Switch } from "@/components/ui/switch";
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
import {
  Loader2,
  Plus,
  Bell,
  Pencil,
  Trash2,
  Mail,
  Smartphone,
  BellRing,
  Clock,
  Package,
  DollarSign,
  Tag,
  ShoppingCart,
  Settings,
  Zap,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { format } from "date-fns";
import { tr } from "date-fns/locale";
import {
  AlertRule,
  AlertType,
  ALERT_TYPE_LABELS,
  ALERT_CONDITION_LABELS,
} from "@/types/alert";
import { AlertRuleFormModal } from "@/components/alerts/alert-rule-form-modal";

const ALERT_TYPE_ICONS: Record<AlertType, React.ReactNode> = {
  STOCK: <Package className="h-4 w-4" />,
  PROFIT: <DollarSign className="h-4 w-4" />,
  PRICE: <Tag className="h-4 w-4" />,
  ORDER: <ShoppingCart className="h-4 w-4" />,
  SYSTEM: <Settings className="h-4 w-4" />,
};

const ALERT_TYPE_COLORS: Record<AlertType, string> = {
  STOCK: "bg-blue-100 dark:bg-blue-900/30 text-blue-600",
  PROFIT: "bg-green-100 dark:bg-green-900/30 text-green-600",
  PRICE: "bg-purple-100 dark:bg-purple-900/30 text-purple-600",
  ORDER: "bg-orange-100 dark:bg-orange-900/30 text-orange-600",
  SYSTEM: "bg-gray-100 dark:bg-gray-800 text-gray-600",
};

export function AlertRulesSettings() {
  const { data: rules, isLoading: rulesLoading } = useAlertRules();
  const { data: counts } = useAlertRuleCounts();
  const toggleMutation = useToggleAlertRule();
  const deleteMutation = useDeleteAlertRule();

  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [editingRule, setEditingRule] = useState<AlertRule | null>(null);
  const [deletingRuleId, setDeletingRuleId] = useState<string | null>(null);

  const handleToggle = async (ruleId: string) => {
    try {
      await toggleMutation.mutateAsync(ruleId);
    } catch (error) {
      console.error("Kural toggle hatası:", error);
    }
  };

  const handleDelete = async (ruleId: string) => {
    try {
      await deleteMutation.mutateAsync(ruleId);
      setDeletingRuleId(null);
    } catch (error) {
      console.error("Kural silme hatası:", error);
    }
  };

  const formatCondition = (rule: AlertRule): string => {
    const condition = ALERT_CONDITION_LABELS[rule.conditionType];
    if (rule.conditionType === "ZERO" || rule.conditionType === "CHANGED") {
      return condition;
    }
    return `${condition} ${rule.threshold}`;
  };

  const formatScope = (rule: AlertRule): string => {
    if (rule.productBarcode) {
      return `Ürün: ${rule.productBarcode}`;
    }
    if (rule.categoryName) {
      return `Kategori: ${rule.categoryName}`;
    }
    if (rule.storeName) {
      return `Mağaza: ${rule.storeName}`;
    }
    return "Tüm ürünler";
  };

  if (rulesLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-semibold text-foreground">Uyarı Kuralları</h2>
          <p className="text-sm text-muted-foreground mt-1">
            Stok, kar ve sipariş uyarıları için özel kurallar tanımlayın
          </p>
        </div>
        <Button
          onClick={() => setIsCreateModalOpen(true)}
          className="bg-[#1D70F1] hover:bg-[#1560d1]"
        >
          <Plus className="h-4 w-4 mr-2" />
          Yeni Kural
        </Button>
      </div>

      {/* Stats Cards */}
      {counts && (
        <div className="grid grid-cols-3 gap-4">
          <div className="bg-card rounded-xl border border-border p-4">
            <p className="text-2xl font-semibold text-foreground">{counts.total}</p>
            <p className="text-sm text-muted-foreground mt-1">Toplam Kural</p>
          </div>
          <div className="bg-green-50 dark:bg-green-900/20 rounded-xl border border-green-200 dark:border-green-800 p-4">
            <p className="text-2xl font-semibold text-green-600">{counts.active}</p>
            <p className="text-sm text-muted-foreground mt-1">Aktif</p>
          </div>
          <div className="bg-muted rounded-xl border border-border p-4">
            <p className="text-2xl font-semibold text-muted-foreground">{counts.inactive}</p>
            <p className="text-sm text-muted-foreground mt-1">Pasif</p>
          </div>
        </div>
      )}

      {/* Info Card */}
      <div className="bg-blue-50 dark:bg-blue-900/20 rounded-xl border border-blue-200 dark:border-blue-800 p-6">
        <div className="flex gap-4">
          <Zap className="h-5 w-5 text-blue-600 flex-shrink-0 mt-0.5" />
          <div>
            <p className="font-medium text-blue-800 dark:text-blue-200">Nasıl Çalışır?</p>
            <ul className="text-sm text-blue-700 dark:text-blue-300 mt-2 space-y-1">
              <li>• Ürün senkronizasyonu sonrası stok kuralları otomatik kontrol edilir</li>
              <li>• Koşul sağlandığında email ve/veya uygulama içi bildirim gönderilir</li>
              <li>• Cooldown süresi spam bildirimleri önler</li>
              <li>• Kuralları istediğiniz zaman aktif/pasif yapabilirsiniz</li>
            </ul>
          </div>
        </div>
      </div>

      {/* Rules List */}
      {rules && rules.length > 0 ? (
        <div className="space-y-4">
          {rules.map((rule) => (
            <div
              key={rule.id}
              className={cn(
                "bg-card rounded-xl border p-6 transition-colors",
                rule.active ? "border-border" : "border-dashed border-muted-foreground/30 opacity-60"
              )}
            >
              <div className="flex items-start justify-between">
                {/* Left: Rule Info */}
                <div className="flex items-start gap-4">
                  {/* Icon */}
                  <div
                    className={cn(
                      "h-10 w-10 rounded-lg flex items-center justify-center",
                      ALERT_TYPE_COLORS[rule.alertType]
                    )}
                  >
                    {ALERT_TYPE_ICONS[rule.alertType]}
                  </div>

                  {/* Details */}
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="font-medium text-foreground">{rule.name}</h3>
                      <Badge variant={rule.active ? "default" : "secondary"}>
                        {rule.active ? "Aktif" : "Pasif"}
                      </Badge>
                    </div>

                    <p className="text-sm text-muted-foreground mt-1">
                      {ALERT_TYPE_LABELS[rule.alertType]} • {formatCondition(rule)} • {formatScope(rule)}
                    </p>

                    {/* Notification Channels */}
                    <div className="flex items-center gap-4 mt-3">
                      <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                        <Mail className={cn("h-3.5 w-3.5", rule.emailEnabled && "text-green-600")} />
                        <span className={rule.emailEnabled ? "text-green-600" : ""}>
                          Email {rule.emailEnabled ? "✓" : "✗"}
                        </span>
                      </div>
                      <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                        <BellRing className={cn("h-3.5 w-3.5", rule.inAppEnabled && "text-green-600")} />
                        <span className={rule.inAppEnabled ? "text-green-600" : ""}>
                          Uygulama {rule.inAppEnabled ? "✓" : "✗"}
                        </span>
                      </div>
                      <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                        <Clock className="h-3.5 w-3.5" />
                        <span>{rule.cooldownMinutes} dk cooldown</span>
                      </div>
                    </div>

                    {/* Trigger Stats */}
                    {rule.triggerCount > 0 && (
                      <p className="text-xs text-muted-foreground mt-2">
                        {rule.triggerCount} kez tetiklendi
                        {rule.lastTriggeredAt && (
                          <> • Son: {format(new Date(rule.lastTriggeredAt), "dd MMM yyyy HH:mm", { locale: tr })}</>
                        )}
                      </p>
                    )}
                  </div>
                </div>

                {/* Right: Actions */}
                <div className="flex items-center gap-3">
                  {/* Toggle Switch */}
                  <Switch
                    checked={rule.active}
                    onCheckedChange={() => handleToggle(rule.id)}
                    disabled={toggleMutation.isPending}
                  />

                  {/* Edit Button */}
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={() => setEditingRule(rule)}
                  >
                    <Pencil className="h-4 w-4" />
                  </Button>

                  {/* Delete Button */}
                  <AlertDialog
                    open={deletingRuleId === rule.id}
                    onOpenChange={(open) => !open && setDeletingRuleId(null)}
                  >
                    <AlertDialogTrigger asChild>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="text-red-600 hover:text-red-700 hover:bg-red-50"
                        onClick={() => setDeletingRuleId(rule.id)}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </AlertDialogTrigger>
                    <AlertDialogContent>
                      <AlertDialogHeader>
                        <AlertDialogTitle>Kuralı Sil</AlertDialogTitle>
                        <AlertDialogDescription>
                          "{rule.name}" kuralını silmek istediğinize emin misiniz?
                          Bu işlem geri alınamaz.
                        </AlertDialogDescription>
                      </AlertDialogHeader>
                      <AlertDialogFooter>
                        <AlertDialogCancel>İptal</AlertDialogCancel>
                        <AlertDialogAction
                          onClick={() => handleDelete(rule.id)}
                          className="bg-red-600 hover:bg-red-700"
                          disabled={deleteMutation.isPending}
                        >
                          {deleteMutation.isPending ? (
                            <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                          ) : null}
                          Sil
                        </AlertDialogAction>
                      </AlertDialogFooter>
                    </AlertDialogContent>
                  </AlertDialog>
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : (
        /* Empty State */
        <div className="text-center py-12 bg-card rounded-xl border border-border">
          <Bell className="h-12 w-12 mx-auto text-muted-foreground mb-3" />
          <p className="text-foreground font-medium mb-1">Henüz uyarı kuralı yok</p>
          <p className="text-sm text-muted-foreground mb-4">
            Stok düşüklerinde veya kar marjı değişikliklerinde otomatik bildirim almak için kural oluşturun.
          </p>
          <Button
            onClick={() => setIsCreateModalOpen(true)}
            className="bg-[#1D70F1] hover:bg-[#1560d1]"
          >
            <Plus className="h-4 w-4 mr-2" />
            İlk Kuralınızı Oluşturun
          </Button>
        </div>
      )}

      {/* Create/Edit Modal */}
      <AlertRuleFormModal
        open={isCreateModalOpen || !!editingRule}
        onOpenChange={(open) => {
          if (!open) {
            setIsCreateModalOpen(false);
            setEditingRule(null);
          }
        }}
        rule={editingRule}
      />
    </div>
  );
}
